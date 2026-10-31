"""
TalentMatch AI - Multi-Factor Semantic Matching & Retrieval Engine
"""

from typing import Dict, List, Optional, Set, Tuple
import numpy as np

from app.nlp.taxonomy import skill_taxonomy
from app.matching.embedder import embedder
from app.matching.models import (
    CandidateMatchResult,
    CandidateSpec,
    JobRequisitionSpec,
    MatchRequest,
    MatchResponse,
    ScoreBreakdown,
)


class MatchingEngine:
    """
    Transparent, explainable talent retrieval and ranking engine.
    Combines dense neural embeddings (all-MiniLM-L6-v2) with strict eligibility rules,
    normalized skill coverage, and job-configured weighting.
    """

    def evaluate_match(self, request: MatchRequest) -> MatchResponse:
        job = request.job
        candidates = request.candidates

        if not candidates:
            return MatchResponse(
                job_id=job.id,
                total_candidates_evaluated=0,
                eligible_candidates_count=0,
                shortlisted_count=0,
                matches=[],
                model_metadata={
                    "embedding_model": embedder.model_name,
                    "dimension": "384",
                    "taxonomy_version": skill_taxonomy.version,
                },
            )

        # 1. Prepare job profile and embedding
        job_text = f"{job.title}\n{job.description}"
        if job.mandatory_skills:
            job_text += f"\nMandatory Skills: {', '.join(job.mandatory_skills)}"
        if job.preferred_skills:
            job_text += f"\nPreferred Skills: {', '.join(job.preferred_skills)}"

        job_embedding = embedder.encode(job_text)

        # Canonicalize job skills
        job_mandatory_canonical = self._canonicalize_skill_list(job.mandatory_skills)
        job_preferred_canonical = self._canonicalize_skill_list(job.preferred_skills)

        # Normalize weights
        w_skill = max(0.0, job.weight_skill_coverage)
        w_exp = max(0.0, job.weight_experience)
        w_proj = max(0.0, job.weight_project_relevance)
        w_semantic = max(0.0, job.weight_semantic_similarity)
        total_w = w_skill + w_exp + w_proj + w_semantic
        if total_w > 0:
            w_skill /= total_w
            w_exp /= total_w
            w_proj /= total_w
            w_semantic /= total_w
        else:
            w_skill, w_exp, w_proj, w_semantic = 0.35, 0.20, 0.15, 0.30

        evaluated_matches: List[CandidateMatchResult] = []

        for candidate in candidates:
            match_result = self._evaluate_single_candidate(
                job=job,
                job_embedding=job_embedding,
                job_mandatory=job_mandatory_canonical,
                job_preferred=job_preferred_canonical,
                weights=(w_skill, w_exp, w_proj, w_semantic),
                candidate=candidate,
            )
            evaluated_matches.append(match_result)

        # Filter and rank candidates
        # Rule: Never lower min_score_threshold silently when fewer candidates qualify than shortlist_count
        eligible_count = sum(1 for m in evaluated_matches if m.is_eligible)

        # Sort: eligible candidates first, then by score descending
        evaluated_matches.sort(key=lambda m: (1 if m.is_eligible else 0, m.score), reverse=True)

        # Apply score threshold if configured
        shortlisted: List[CandidateMatchResult] = []
        for match in evaluated_matches:
            if job.min_score_threshold is not None:
                if match.score < job.min_score_threshold:
                    continue
            shortlisted.append(match)
            if len(shortlisted) >= job.shortlist_count:
                break

        return MatchResponse(
            job_id=job.id,
            total_candidates_evaluated=len(candidates),
            eligible_candidates_count=eligible_count,
            shortlisted_count=len(shortlisted),
            matches=shortlisted,
            model_metadata={
                "embedding_model": embedder.model_name,
                "dimension": "384",
                "taxonomy_version": skill_taxonomy.version,
            },
        )

    def _canonicalize_skill_list(self, skills: List[str]) -> Dict[str, str]:
        """Maps raw skill names to canonical_id -> canonical_name."""
        canonical_map: Dict[str, str] = {}
        for s in skills:
            if not s:
                continue
            norm = skill_taxonomy.normalize(s)
            if norm:
                canonical_map[norm.id] = norm.name
            else:
                # Retain unrecognized skills as lowercase keys
                key = s.strip().lower()
                canonical_map[key] = s.strip()
        return canonical_map

    def _evaluate_single_candidate(
        self,
        job: JobRequisitionSpec,
        job_embedding: np.ndarray,
        job_mandatory: Dict[str, str],
        job_preferred: Dict[str, str],
        weights: Tuple[float, float, float, float],
        candidate: CandidateSpec,
    ) -> CandidateMatchResult:
        w_skill, w_exp, w_proj, w_semantic = weights
        uncertainties: List[str] = []

        # 1. Candidate skills normalization
        cand_canonical = self._canonicalize_skill_list(candidate.skills)

        # Check candidate raw text or experience text for additional taxonomy skills
        evidence_source = f"{candidate.raw_text or ''}\n{candidate.experience_text or ''}\n{candidate.summary or ''}"
        for s_id, s_name in job_mandatory.items():
            if s_id not in cand_canonical and s_name.lower() in evidence_source.lower():
                cand_canonical[s_id] = s_name
        for s_id, s_name in job_preferred.items():
            if s_id not in cand_canonical and s_name.lower() in evidence_source.lower():
                cand_canonical[s_id] = s_name

        # 2. Mandatory skills eligibility
        matched_mandatory: List[str] = []
        missing_mandatory: List[str] = []
        for s_id, s_name in job_mandatory.items():
            if s_id in cand_canonical:
                matched_mandatory.append(s_name)
            else:
                missing_mandatory.append(s_name)

        is_eligible = len(missing_mandatory) == 0
        if not is_eligible:
            uncertainties.append(f"Missing mandatory requirements: {', '.join(missing_mandatory)}")

        # 3. Preferred skills match
        matched_preferred: List[str] = []
        for s_id, s_name in job_preferred.items():
            if s_id in cand_canonical:
                matched_preferred.append(s_name)

        all_matched_skills = sorted(list(set(matched_mandatory + matched_preferred)))

        # 4. Normalized Skill Coverage Score (0 - 100)
        mand_ratio = (len(matched_mandatory) / len(job_mandatory)) if job_mandatory else 1.0
        pref_ratio = (len(matched_preferred) / len(job_preferred)) if job_preferred else 1.0

        if job_mandatory and job_preferred:
            coverage_score = (0.7 * mand_ratio + 0.3 * pref_ratio) * 100.0
        elif job_mandatory:
            coverage_score = mand_ratio * 100.0
        elif job_preferred:
            coverage_score = pref_ratio * 100.0
        else:
            coverage_score = 100.0

        # 5. Experience Relevance Score (0 - 100)
        if job.min_experience_years is not None and job.min_experience_years > 0:
            if candidate.total_experience_years is not None:
                exp_ratio = min(1.0, candidate.total_experience_years / job.min_experience_years)
                exp_score = exp_ratio * 100.0
            else:
                exp_score = 0.0
                uncertainties.append("Total years of experience duration not verifiable")
        else:
            exp_score = 100.0

        # 6. Semantic Similarity Score (0 - 100)
        cand_text = candidate.summary or ""
        if candidate.experience_text:
            cand_text += f"\n{candidate.experience_text}"
        if not cand_text and candidate.raw_text:
            cand_text = candidate.raw_text[:2000]

        if not cand_text.strip():
            semantic_score = 0.0
            uncertainties.append("No descriptive candidate text available for semantic analysis")
        else:
            if candidate.embedding and len(candidate.embedding) == 384:
                cand_vec = np.array(candidate.embedding, dtype=np.float32)
            else:
                cand_vec = embedder.encode(cand_text)

            cos_sim = embedder.cosine_similarity(job_embedding, cand_vec)
            # Rescale cosine similarity for all-MiniLM-L6-v2 (typically 0.2 to 0.8+) into [0, 100]
            semantic_score = round(max(0.0, min(100.0, cos_sim * 100.0)), 2)

        # 7. Project Relevance Score (0 - 100)
        if candidate.projects_text and candidate.projects_text.strip():
            proj_vec = embedder.encode(candidate.projects_text)
            proj_sim = embedder.cosine_similarity(job_embedding, proj_vec)
            proj_score = round(max(0.0, min(100.0, proj_sim * 100.0)), 2)
        else:
            # Fallback to general semantic score with an explicit note
            proj_score = round(semantic_score * 0.85, 2)
            uncertainties.append("No explicit projects portfolio submitted")

        # 8. Multi-Factor Weighted Final Score
        raw_final = (
            w_skill * coverage_score
            + w_exp * exp_score
            + w_proj * proj_score
            + w_semantic * semantic_score
        )

        # If mandatory requirements are missing, apply eligibility gating while keeping breakdown explainable
        final_score = round(raw_final if is_eligible else raw_final * 0.5, 2)

        # 9. Extract evidence snippets for matched skills
        evidence_snippets = self._extract_evidence_snippets(all_matched_skills, evidence_source)

        return CandidateMatchResult(
            candidate_id=candidate.id,
            candidate_name=candidate.name,
            score=final_score,
            score_breakdown=ScoreBreakdown(
                semantic_similarity=round(semantic_score, 2),
                skill_coverage=round(coverage_score, 2),
                experience_relevance=round(exp_score, 2),
                project_relevance=round(proj_score, 2),
                final_score=final_score,
            ),
            is_eligible=is_eligible,
            matched_skills=all_matched_skills,
            missing_mandatory_skills=missing_mandatory,
            matched_preferred_skills=matched_preferred,
            evidence_snippets=evidence_snippets,
            uncertainty_indicators=uncertainties,
        )

    def _extract_evidence_snippets(self, skills: List[str], text: str) -> Dict[str, str]:
        """Extracts context sentences where matched skills appear in candidate text."""
        snippets: Dict[str, str] = {}
        if not text or not skills:
            return snippets

        lines = [line.strip() for line in text.replace("\r", "\n").split("\n") if line.strip()]

        for skill in skills[:10]:  # limit to top 10 skills for crisp responses
            s_lower = skill.lower()
            for line in lines:
                if s_lower in line.lower():
                    clean_line = line.replace("\u00a0", " ").replace("\xc2\xa0", " ").replace("Â", "").strip()
                    if len(clean_line) > 160:
                        clean_line = clean_line[:160] + "..."
                    snippets[skill] = clean_line
                    break

        return snippets


# Global singleton instance
matching_engine = MatchingEngine()
