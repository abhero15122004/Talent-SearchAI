"""
TalentMatch AI - Resume Extraction and Normalization Engine
"""

import re
from datetime import datetime
from typing import Dict, List, Optional, Set, Tuple
import spacy
from spacy.matcher import PhraseMatcher

from app.config import settings
from app.nlp.taxonomy import CanonicalSkill, skill_taxonomy
from app.nlp.models import (
    EducationItem,
    ExperienceItem,
    ProjectItem,
    ProvenanceMetadata,
    ResumeExtractionResponse,
    SkillExtraction,
)

# Email and Phone regex patterns
EMAIL_PATTERN = re.compile(r"[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+")
PHONE_PATTERN = re.compile(r"(?:\+?\d{1,3}[-.\s]?)?\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4}")

# Standard date ranges e.g. "2020 - 2023", "Jan 2019 - Present", "05/2018 - 08/2021"
DATE_RANGE_PATTERN = re.compile(
    r"(?:(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\.?\s+)?(\d{4}|\d{2}/\d{4})\s*(?:-|–|—|to)\s*(?:(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\.?\s+)?(\d{4}|\d{2}/\d{4}|[Pp]resent|[Cc]urrent)",
    re.IGNORECASE,
)

YEAR_PATTERN = re.compile(r"\b(19\d{2}|20\d{2})\b")

# Degree patterns
DEGREE_PATTERN = re.compile(
    r"\b(Bachelor(?:'s)?(?:\s+of\s+[A-Za-z]+)?|Master(?:'s)?(?:\s+of\s+[A-Za-z]+)?|Ph\.?D\.?|B\.?S\.?|M\.?S\.?|B\.?Tech|M\.?Tech|B\.?E\.?|M\.?E\.?|B\.?A\.?|M\.?B\.?A\.?|Associate(?:'s)?)\b",
    re.IGNORECASE,
)

# Section Headers
SECTION_HEADERS = {
    "summary": re.compile(r"^(?:summary|professional\s+summary|profile|about\s+me|career\s+objective)\b", re.IGNORECASE),
    "skills": re.compile(r"^(?:skills|technical\s+skills|core\s+competencies|technologies|tools\s*&\s*technologies|key\s+skills)\b", re.IGNORECASE),
    "experience": re.compile(r"^(?:experience|work\s+experience|employment\s+history|professional\s+experience|work\s+history)\b", re.IGNORECASE),
    "education": re.compile(r"^(?:education|academic\s+background|academics|qualifications)\b", re.IGNORECASE),
    "projects": re.compile(r"^(?:projects|personal\s+projects|key\s+projects|academic\s+projects)\b", re.IGNORECASE),
}


class ResumeExtractor:
    """
    Hybrid NLP and Rule-Based Resume Extraction Engine.
    Uses spaCy for linguistic parsing and sentence boundary detection, combined with
    a deterministic skill taxonomy and section segmenter.
    """

    def __init__(self, spacy_model: str = settings.SPACY_MODEL):
        self.spacy_model_name = spacy_model
        try:
            self.nlp = spacy.load(spacy_model)
        except Exception:
            # Fallback if specific model not loaded
            self.nlp = spacy.blank("en")
            if "sentencizer" not in self.nlp.pipe_names:
                self.nlp.add_pipe("sentencizer")

        self.matcher = PhraseMatcher(self.nlp.vocab, attr="LOWER")
        self._init_skill_matcher()

    def _init_skill_matcher(self):
        """Initializes PhraseMatcher with all taxonomy skill aliases."""
        for skill in skill_taxonomy.get_all_canonical_skills():
            patterns = [self.nlp.make_doc(skill.name)]
            for alias in skill.aliases:
                if alias.strip():
                    patterns.append(self.nlp.make_doc(alias.strip()))
            self.matcher.add(skill.id, patterns)

    def extract(self, raw_text: str, candidate_id: Optional[str] = None) -> ResumeExtractionResponse:
        """Main entrypoint for structured resume extraction."""
        uncertainty: List[str] = []
        if not raw_text or len(raw_text.strip()) == 0:
            return ResumeExtractionResponse(
                candidate_id=candidate_id,
                uncertainty_indicators=["Resume text is completely empty"],
                provenance=ProvenanceMetadata(
                    spacy_model=self.spacy_model_name,
                    taxonomy_version=skill_taxonomy.version,
                ),
            )

        text = raw_text.strip()
        if len(text) < 150:
            uncertainty.append("Resume text is unusually short (< 150 chars); extractions may be incomplete")

        # Linguistic processing
        doc = self.nlp(text)

        # 1. Contact information
        email = self._extract_email(text)
        phone = self._extract_phone(text)
        full_name = self._extract_name(doc, text)

        if not email:
            uncertainty.append("No contact email address identified")

        # 2. Section segmentation
        sections = self._segment_sections(text)

        # 3. Skills extraction and normalization with taxonomy
        skills = self._extract_skills(doc, sections)
        if not skills:
            uncertainty.append("No technical skills from taxonomy identified")

        # 4. Experience extraction
        experience, total_years = self._extract_experience(sections.get("experience", ""), doc)
        if not experience:
            uncertainty.append("No structured work experience records identified")

        # 5. Education extraction
        education = self._extract_education(sections.get("education", ""), doc)
        if not education:
            uncertainty.append("No structured education records identified")

        # 6. Projects extraction
        projects = self._extract_projects(sections.get("projects", ""), doc)

        # 7. Summary extraction
        summary = sections.get("summary")
        if not summary and len(text) > 200:
            # Use first paragraph as implicit summary if not segmented
            first_block = text.split("\n\n")[0].strip()
            if len(first_block) > 40 and not any(h.search(first_block) for h in SECTION_HEADERS.values()):
                summary = first_block

        return ResumeExtractionResponse(
            candidate_id=candidate_id,
            full_name=full_name,
            email=email,
            phone=phone,
            summary=summary,
            skills=skills,
            work_experience=experience,
            education=education,
            projects=projects,
            total_years_experience=total_years,
            provenance=ProvenanceMetadata(
                spacy_model=self.spacy_model_name,
                taxonomy_version=skill_taxonomy.version,
            ),
            uncertainty_indicators=uncertainty,
        )

    def _extract_email(self, text: str) -> Optional[str]:
        match = EMAIL_PATTERN.search(text)
        return match.group(0).lower() if match else None

    def _extract_phone(self, text: str) -> Optional[str]:
        match = PHONE_PATTERN.search(text)
        return match.group(0).strip() if match else None

    def _extract_name(self, doc, text: str) -> Optional[str]:
        """Extract candidate name from document header using NER or structural heuristics."""
        # 1. Try NER PERSON in first 400 characters
        for ent in doc.ents:
            if ent.label_ == "PERSON" and ent.start_char < 300:
                clean = ent.text.strip()
                if 2 <= len(clean.split()) <= 4 and not any(char.isdigit() for char in clean):
                    return clean

        # 2. Heuristic: First non-empty line with 2-3 capitalized words
        lines = [line.strip() for line in text.split("\n") if line.strip()]
        for line in lines[:4]:
            if "@" in line or any(char.isdigit() for char in line):
                continue
            words = line.split()
            if 2 <= len(words) <= 4 and all(w[0].isupper() for w in words if w.isalpha()):
                return line
        return None

    def _segment_sections(self, text: str) -> Dict[str, str]:
        """Segments text into labeled functional blocks based on section headers."""
        lines = text.split("\n")
        sections: Dict[str, List[str]] = {}
        current_section = "header"

        for line in lines:
            trimmed = line.strip()
            if not trimmed:
                continue

            # Check if this line is a section header
            matched_header = None
            for sec_name, pattern in SECTION_HEADERS.items():
                if pattern.search(trimmed):
                    matched_header = sec_name
                    break

            if matched_header:
                current_section = matched_header
                if current_section not in sections:
                    sections[current_section] = []
            else:
                sections.setdefault(current_section, []).append(trimmed)

        return {k: "\n".join(v) for k, v in sections.items()}

    def _extract_skills(self, doc, sections: Dict[str, str]) -> List[SkillExtraction]:
        """
        Extracts and normalizes skills using PhraseMatcher against taxonomy.
        Assigns provenance, category, confidence, and evidence snippet.
        """
        matches = self.matcher(doc)
        extracted: Dict[str, SkillExtraction] = {}
        skills_section_text = sections.get("skills", "").lower()

        for match_id, start, end in matches:
            skill_id = self.nlp.vocab.strings[match_id]
            span = doc[start:end]
            raw_text = span.text.strip()

            canonical = skill_taxonomy.normalize(raw_text)
            if not canonical:
                continue

            # Extract sentence-level context snippet
            sent = span.sent.text.strip() if span.sent else raw_text
            # Truncate snippet to 160 chars for concise readability
            if len(sent) > 160:
                sent = sent[:157] + "..."

            # Calculate confidence
            is_in_skills_section = canonical.name.lower() in skills_section_text or raw_text.lower() in skills_section_text
            confidence = 1.0 if is_in_skills_section else 0.92

            # Keep entry or update if higher confidence
            if canonical.id not in extracted or confidence > extracted[canonical.id].confidence:
                extracted[canonical.id] = SkillExtraction(
                    canonical_id=canonical.id,
                    canonical_name=canonical.name,
                    category=canonical.category,
                    raw_text=raw_text,
                    confidence=confidence,
                    evidence_snippet=sent,
                )

        # Sort by canonical name
        return sorted(list(extracted.values()), key=lambda s: s.canonical_name)

    def _extract_experience(self, exp_text: str, doc) -> Tuple[List[ExperienceItem], Optional[float]]:
        """Extracts work experience records and computes approximate total years."""
        if not exp_text:
            return [], None

        items: List[ExperienceItem] = []
        blocks = re.split(r"\n(?=[A-Z0-9][^\n]+(?:\d{4}|Present|Current))", exp_text)
        current_year = datetime.now().year
        total_months = 0

        for block in blocks:
            b_clean = block.strip()
            if not b_clean or len(b_clean) < 15:
                continue

            date_match = DATE_RANGE_PATTERN.search(b_clean)
            start_date, end_date = None, None
            years_duration = None

            if date_match:
                start_year_str = date_match.group(2)
                end_year_str = date_match.group(4)

                start_date = date_match.group(0).split("-")[0].strip()
                end_date = date_match.group(0).split("-")[-1].strip()

                try:
                    s_yr = int(start_year_str[-4:])
                    e_yr = current_year if "present" in end_year_str.lower() or "current" in end_year_str.lower() else int(end_year_str[-4:])
                    diff = max(0.5, float(e_yr - s_yr))
                    years_duration = round(diff, 1)
                    total_months += int(diff * 12)
                except Exception:
                    pass

            lines = [l.strip() for l in b_clean.split("\n") if l.strip()]
            header_line = lines[0] if lines else ""
            desc = "\n".join(lines[1:]) if len(lines) > 1 else header_line

            # Identify skills mentioned in this experience block
            block_doc = self.nlp(b_clean)
            block_matches = self.matcher(block_doc)
            block_skills: Set[str] = set()
            for m_id, s, e in block_matches:
                span_text = block_doc[s:e].text.strip()
                norm = skill_taxonomy.normalize(span_text)
                if norm:
                    block_skills.add(norm.name)

            items.append(
                ExperienceItem(
                    title=header_line[:80] if header_line else None,
                    company=None,
                    start_date=start_date,
                    end_date=end_date,
                    years=years_duration,
                    description=desc[:500] if desc else None,
                    skills_mentioned=sorted(list(block_skills)),
                )
            )

        total_years = round(total_months / 12.0, 1) if total_months > 0 else None
        return items, total_years

    def _extract_education(self, edu_text: str, doc) -> List[EducationItem]:
        """Extracts degrees, institutions, and graduation years."""
        if not edu_text:
            return []

        items: List[EducationItem] = []
        lines = [l.strip() for l in edu_text.split("\n") if l.strip()]

        for line in lines:
            degree_match = DEGREE_PATTERN.search(line)
            year_match = YEAR_PATTERN.search(line)

            if degree_match or year_match or "university" in line.lower() or "college" in line.lower() or "institute" in line.lower():
                grad_year = int(year_match.group(1)) if year_match else None
                degree = degree_match.group(0) if degree_match else None

                # Clean institution heuristic
                institution = line
                if degree:
                    institution = institution.replace(degree, "")
                if grad_year:
                    institution = institution.replace(str(grad_year), "")
                institution = institution.strip(" ,.-|")

                items.append(
                    EducationItem(
                        degree=degree,
                        institution=institution[:120] if institution else None,
                        field_of_study=None,
                        graduation_year=grad_year,
                    )
                )

        return items

    def _extract_projects(self, proj_text: str, doc) -> List[ProjectItem]:
        """Extracts project descriptions and mentioned technologies."""
        if not proj_text:
            return []

        items: List[ProjectItem] = []
        blocks = proj_text.split("\n\n")

        for block in blocks:
            b_clean = block.strip()
            if not b_clean or len(b_clean) < 10:
                continue

            lines = [l.strip() for l in b_clean.split("\n") if l.strip()]
            name = lines[0][:80]
            desc = "\n".join(lines[1:]) if len(lines) > 1 else lines[0]

            # Detect technologies in project description
            p_doc = self.nlp(b_clean)
            p_matches = self.matcher(p_doc)
            techs: Set[str] = set()
            for m_id, s, e in p_matches:
                t_span = p_doc[s:e].text.strip()
                c_skill = skill_taxonomy.normalize(t_span)
                if c_skill:
                    techs.add(c_skill.name)

            items.append(
                ProjectItem(
                    name=name,
                    description=desc[:500],
                    technologies=sorted(list(techs)),
                )
            )

        return items


# Global singleton instance
resume_extractor = ResumeExtractor()
