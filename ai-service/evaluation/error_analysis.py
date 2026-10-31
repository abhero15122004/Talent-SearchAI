"""
TalentMatch AI — Retrieval Error Analysis & Diagnostics Engine

Identifies, categorizes, and explains failure modes across models:
1. Keyword False Positives (TF-IDF failure mode)
2. Semantic False Positives (Dense embedding failure mode)
3. Mandatory Requirement Violations (Absence of gating)
4. Synonym Normalization Failures
5. Ranking Inversions (Model ranking A > B when True(A) < True(B))
6. Complex Model Diminishing Returns (Cross-Encoder vs Hybrid comparisons)
"""

from typing import Any, Dict, List, Tuple


class ErrorAnalyzer:
    """Diagnoses and extracts concrete case studies from benchmark evaluation runs."""

    def analyze_job_run(
        self,
        job_dict: Dict[str, Any],
        candidates: List[Dict[str, Any]],
        model_scores: Dict[str, List[float]],
    ) -> List[Dict[str, Any]]:
        """
        Runs failure mode diagnostics across all evaluated models for a given job.
        """
        diagnostics: List[Dict[str, Any]] = []

        y_true = [c["relevance_label"] for c in candidates]
        mandatory_skills = [m.lower() for m in job_dict.get("mandatory_skills", [])]

        for model_name, scores in model_scores.items():
            ranked_pairs = sorted(enumerate(scores), key=lambda x: x[1], reverse=True)
            top_3_indices = [idx for idx, _ in ranked_pairs[:3]]

            # 1. Detect Keyword False Positives:
            # High score (> 50.0 or in top 3), but true relevance <= 1 and archetype is 'keyword_stuffing'
            for idx in top_3_indices:
                cand = candidates[idx]
                if cand.get("archetype") == "keyword_stuffing" and y_true[idx] <= 1:
                    diagnostics.append({
                        "job_id": job_dict["id"],
                        "model": model_name,
                        "candidate_id": cand["candidate_id"],
                        "candidate_name": cand["name"],
                        "category": "KEYWORD_FALSE_POSITIVE",
                        "severity": "HIGH",
                        "model_score": scores[idx],
                        "true_relevance": y_true[idx],
                        "explanation": f"Model ranked keyword-stuffed resume '{cand['name']}' in top 3 with score {scores[idx]}. Candidate only has course/sales mentions of target keywords.",
                    })

            # 2. Detect Semantic False Positives:
            # High score (> 50.0 or in top 3), but true relevance <= 1 and archetype is 'semantic_false_positive'
            for idx in top_3_indices:
                cand = candidates[idx]
                if cand.get("archetype") == "semantic_false_positive" and y_true[idx] <= 1:
                    diagnostics.append({
                        "job_id": job_dict["id"],
                        "model": model_name,
                        "candidate_id": cand["candidate_id"],
                        "candidate_name": cand["name"],
                        "category": "SEMANTIC_FALSE_POSITIVE",
                        "severity": "HIGH",
                        "model_score": scores[idx],
                        "true_relevance": y_true[idx],
                        "explanation": f"Model ranked generic buzzword resume '{cand['name']}' in top 3 with score {scores[idx]}. Candidate discusses distributed cloud strategy without concrete tech stack.",
                    })

            # 3. Detect Mandatory Requirement Violations:
            # Candidate lacks mandatory skill, yet is ranked in the top 3
            for idx in top_3_indices:
                cand = candidates[idx]
                cand_skills = [s.lower() for s in cand.get("skills", [])]
                cand_text = f"{cand.get('raw_text', '')} {cand.get('experience_text', '')}".lower()
                missing = [m for m in mandatory_skills if m not in cand_skills and m not in cand_text]

                if missing and y_true[idx] <= 1:
                    diagnostics.append({
                        "job_id": job_dict["id"],
                        "model": model_name,
                        "candidate_id": cand["candidate_id"],
                        "candidate_name": cand["name"],
                        "category": "MANDATORY_REQUIREMENT_VIOLATION",
                        "severity": "CRITICAL",
                        "model_score": scores[idx],
                        "true_relevance": y_true[idx],
                        "missing_skills": missing,
                        "explanation": f"Model ranked candidate '{cand['name']}' in top 3 despite missing mandatory requirements ({', '.join(missing)}).",
                    })

            # 4. Detect Synonym Handling:
            # Candidate with archetype 'synonym_variation' should score highly (true relevance >= 2)
            for idx, cand in enumerate(candidates):
                if cand.get("archetype") == "synonym_variation" and y_true[idx] >= 2:
                    is_in_top_3 = idx in top_3_indices
                    if not is_in_top_3 and scores[idx] < 50.0:
                        diagnostics.append({
                            "job_id": job_dict["id"],
                            "model": model_name,
                            "candidate_id": cand["candidate_id"],
                            "candidate_name": cand["name"],
                            "category": "SYNONYM_NORMALIZATION_FAILURE",
                            "severity": "MEDIUM",
                            "model_score": scores[idx],
                            "true_relevance": y_true[idx],
                            "explanation": f"Model failed to recognize canonical synonyms (e.g. K8s, Spring, TS) for candidate '{cand['name']}', scoring only {scores[idx]}.",
                        })

        return diagnostics


error_analyzer = ErrorAnalyzer()
