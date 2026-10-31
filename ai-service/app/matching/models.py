"""
TalentMatch AI - Semantic Matching & Embeddings Schemas
"""

from typing import Dict, List, Optional
from pydantic import BaseModel, Field


class EmbeddingRequest(BaseModel):
    texts: List[str] = Field(..., min_items=1, max_items=100)


class EmbeddingResponse(BaseModel):
    model: str
    dimension: int
    embeddings: List[List[float]]


class JobRequisitionSpec(BaseModel):
    id: str
    title: str
    description: str
    mandatory_skills: List[str] = Field(default_factory=list)
    preferred_skills: List[str] = Field(default_factory=list)
    min_experience_years: Optional[float] = None
    weight_skill_coverage: float = Field(default=0.35, ge=0.0, le=1.0)
    weight_experience: float = Field(default=0.20, ge=0.0, le=1.0)
    weight_project_relevance: float = Field(default=0.15, ge=0.0, le=1.0)
    weight_semantic_similarity: float = Field(default=0.30, ge=0.0, le=1.0)
    shortlist_count: int = Field(default=10, ge=1)
    min_score_threshold: Optional[float] = Field(default=None, ge=0.0, le=100.0)


class CandidateSpec(BaseModel):
    id: str
    name: Optional[str] = None
    summary: Optional[str] = None
    skills: List[str] = Field(default_factory=list)
    total_experience_years: Optional[float] = None
    experience_text: Optional[str] = None
    projects_text: Optional[str] = None
    raw_text: Optional[str] = None
    embedding: Optional[List[float]] = None


class ScoreBreakdown(BaseModel):
    semantic_similarity: float = Field(ge=0.0, le=100.0)
    skill_coverage: float = Field(ge=0.0, le=100.0)
    experience_relevance: float = Field(ge=0.0, le=100.0)
    project_relevance: float = Field(ge=0.0, le=100.0)
    final_score: float = Field(ge=0.0, le=100.0)


class CandidateMatchResult(BaseModel):
    candidate_id: str
    candidate_name: Optional[str] = None
    score: float = Field(ge=0.0, le=100.0)
    score_breakdown: ScoreBreakdown
    is_eligible: bool
    matched_skills: List[str] = Field(default_factory=list)
    missing_mandatory_skills: List[str] = Field(default_factory=list)
    matched_preferred_skills: List[str] = Field(default_factory=list)
    evidence_snippets: Dict[str, str] = Field(default_factory=dict)
    uncertainty_indicators: List[str] = Field(default_factory=list)


class MatchRequest(BaseModel):
    job: JobRequisitionSpec
    candidates: List[CandidateSpec] = Field(default_factory=list)


class MatchResponse(BaseModel):
    job_id: str
    total_candidates_evaluated: int
    eligible_candidates_count: int
    shortlisted_count: int
    matches: List[CandidateMatchResult] = Field(default_factory=list)
    model_metadata: Dict[str, str] = Field(default_factory=dict)
