"""
Tests for Multi-Factor Semantic Matching Engine
"""

import pytest
from app.matching.models import (
    CandidateSpec,
    JobRequisitionSpec,
    MatchRequest,
)
from app.matching.engine import matching_engine


@pytest.fixture
def sample_job():
    return JobRequisitionSpec(
        id="job-1",
        title="Senior Java Backend Engineer",
        description="We are looking for an experienced Java engineer to design scalable Spring Boot microservices.",
        mandatory_skills=["Java", "Spring Boot", "Docker"],
        preferred_skills=["Kubernetes", "AWS"],
        min_experience_years=4.0,
        weight_skill_coverage=0.40,
        weight_experience=0.20,
        weight_project_relevance=0.10,
        weight_semantic_similarity=0.30,
        shortlist_count=5,
        min_score_threshold=None,
    )


def test_mandatory_skill_eligibility(sample_job):
    qualified_candidate = CandidateSpec(
        id="cand-1",
        name="Alice Developer",
        summary="Experienced Java backend engineer specializing in Spring Boot and Docker deployments.",
        skills=["Java", "Spring Boot", "Docker", "Kubernetes"],
        total_experience_years=5.0,
        experience_text="5 years building Spring Boot services with Docker containers.",
    )

    unqualified_candidate = CandidateSpec(
        id="cand-2",
        name="Bob Pythonista",
        summary="Python developer experienced in Django and Flask.",
        skills=["Python", "Django", "Docker"],  # Missing Java and Spring Boot
        total_experience_years=5.0,
        experience_text="Built Python web services.",
    )

    req = MatchRequest(job=sample_job, candidates=[qualified_candidate, unqualified_candidate])
    response = matching_engine.evaluate_match(req)

    assert response.total_candidates_evaluated == 2
    assert response.eligible_candidates_count == 1

    # Qualified candidate should be eligible and have higher score
    alice_match = next(m for m in response.matches if m.candidate_id == "cand-1")
    assert alice_match.is_eligible is True
    assert len(alice_match.missing_mandatory_skills) == 0
    assert "Spring Boot" in alice_match.matched_skills
    assert alice_match.score >= 60.0

    # Bob should be missing mandatory skills
    bob_match = next(m for m in response.matches if m.candidate_id == "cand-2")
    assert bob_match.is_eligible is False
    assert "Java" in bob_match.missing_mandatory_skills or "Spring Boot" in bob_match.missing_mandatory_skills


def test_threshold_not_silently_lowered(sample_job):
    """
    Verify rule: 'Do not lower a score threshold silently when fewer candidates qualify than the requested shortlist count.'
    """
    sample_job.min_score_threshold = 80.0
    sample_job.shortlist_count = 10  # Recruiter requested 10 candidates

    # Only 1 candidate qualifies above 80
    strong_cand = CandidateSpec(
        id="cand-strong",
        name="Lead Dev",
        summary="Senior Java Spring Boot Docker AWS Architect",
        skills=["Java", "Spring Boot", "Docker", "Kubernetes", "AWS"],
        total_experience_years=8.0,
        experience_text="8 years architecting Spring Boot microservices with Docker on AWS.",
    )

    mediocre_cand = CandidateSpec(
        id="cand-mediocre",
        name="Junior Dev",
        summary="Junior developer with basic skills",
        skills=["Java"],
        total_experience_years=1.0,
        experience_text="1 year junior development.",
    )

    req = MatchRequest(job=sample_job, candidates=[strong_cand, mediocre_cand])
    response = matching_engine.evaluate_match(req)

    # Even though shortlist_count=10, only the candidate >= 80.0 is returned
    assert response.shortlisted_count == 1
    assert len(response.matches) == 1
    assert response.matches[0].candidate_id == "cand-strong"
    assert response.matches[0].score >= 80.0


def test_custom_weighting_impact():
    # Job heavily weighting experience over skill coverage
    exp_heavy_job = JobRequisitionSpec(
        id="job-exp",
        title="Engineering Manager",
        description="Engineering leader with 10+ years of tech experience.",
        mandatory_skills=[],
        preferred_skills=["Java"],
        min_experience_years=10.0,
        weight_skill_coverage=0.05,
        weight_experience=0.75,
        weight_project_relevance=0.05,
        weight_semantic_similarity=0.15,
        shortlist_count=5,
    )

    veteran = CandidateSpec(
        id="cand-vet",
        name="Senior Veteran",
        summary="12 years software engineering management.",
        skills=["Java"],
        total_experience_years=12.0,
        experience_text="12 years leading enterprise software teams.",
    )

    junior = CandidateSpec(
        id="cand-jun",
        name="Junior Tech",
        summary="1 year tech experience with Java.",
        skills=["Java"],
        total_experience_years=1.0,
        experience_text="1 year junior developer.",
    )

    req = MatchRequest(job=exp_heavy_job, candidates=[veteran, junior])
    response = matching_engine.evaluate_match(req)

    vet_match = next(m for m in response.matches if m.candidate_id == "cand-vet")
    jun_match = next(m for m in response.matches if m.candidate_id == "cand-jun")

    assert vet_match.score_breakdown.experience_relevance == 100.0
    assert jun_match.score_breakdown.experience_relevance == 10.0
    assert vet_match.score > jun_match.score + 50.0
