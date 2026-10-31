"""
TalentMatch AI - NLP & Resume Extraction Schemas
"""

from typing import List, Optional
from datetime import datetime
from pydantic import BaseModel, Field


class SkillExtraction(BaseModel):
    canonical_id: str
    canonical_name: str
    category: str
    raw_text: str
    confidence: float = Field(ge=0.0, le=1.0)
    evidence_snippet: Optional[str] = None


class ExperienceItem(BaseModel):
    title: Optional[str] = None
    company: Optional[str] = None
    start_date: Optional[str] = None
    end_date: Optional[str] = None
    years: Optional[float] = None
    description: Optional[str] = None
    skills_mentioned: List[str] = Field(default_factory=list)


class EducationItem(BaseModel):
    degree: Optional[str] = None
    institution: Optional[str] = None
    field_of_study: Optional[str] = None
    graduation_year: Optional[int] = None


class ProjectItem(BaseModel):
    name: Optional[str] = None
    description: Optional[str] = None
    technologies: List[str] = Field(default_factory=list)


class ProvenanceMetadata(BaseModel):
    extractor_version: str = "1.0.0"
    taxonomy_version: str = "1.0.0"
    spacy_model: str = "en_core_web_sm"
    processed_at: str = Field(default_factory=lambda: datetime.utcnow().isoformat() + "Z")


class ResumeExtractionRequest(BaseModel):
    raw_text: str = Field(..., min_length=1, max_length=100_000)
    candidate_id: Optional[str] = None


class ResumeExtractionResponse(BaseModel):
    candidate_id: Optional[str] = None
    full_name: Optional[str] = None
    email: Optional[str] = None
    phone: Optional[str] = None
    summary: Optional[str] = None
    skills: List[SkillExtraction] = Field(default_factory=list)
    work_experience: List[ExperienceItem] = Field(default_factory=list)
    education: List[EducationItem] = Field(default_factory=list)
    projects: List[ProjectItem] = Field(default_factory=list)
    total_years_experience: Optional[float] = None
    provenance: ProvenanceMetadata = Field(default_factory=ProvenanceMetadata)
    uncertainty_indicators: List[str] = Field(default_factory=list)
