"""
TalentMatch AI - Retrieval Benchmark & Evaluation Suite
Demonstrates retrieval accuracy, ranking calibration, and baseline performance metrics.
"""

import pytest
from app.matching.models import (
    CandidateSpec,
    JobRequisitionSpec,
    MatchRequest,
)
from app.matching.engine import matching_engine


def test_synthetic_talent_retrieval_benchmark():
    """
    Evaluates ranking fidelity across diverse candidate archetypes:
    - Candidate 1: Exact Fit Senior (Should rank #1, eligible)
    - Candidate 2: In-domain Junior (Eligible, lower experience score)
    - Candidate 3: Senior Python/Cloud (Missing mandatory Java & Spring Boot)
    - Candidate 4: Frontend Specialist (Missing backend requirements)
    - Candidate 5: Unrelated Domain / Noise (Near-zero relevance)
    """

    job = JobRequisitionSpec(
        id="bench-job-1",
        title="Senior Java & Distributed Systems Engineer",
        description="Seeking a senior backend engineer to architect distributed Spring Boot microservices with high throughput.",
        mandatory_skills=["Java", "Spring Boot"],
        preferred_skills=["Docker", "Kubernetes", "Amazon Web Services (AWS)"],
        min_experience_years=5.0,
        weight_skill_coverage=0.40,
        weight_experience=0.25,
        weight_project_relevance=0.10,
        weight_semantic_similarity=0.25,
        shortlist_count=5,
    )

    candidates = [
        CandidateSpec(
            id="exact-fit",
            name="Alice Exact",
            summary="Senior Java Engineer with 6 years building distributed Spring Boot microservices on AWS and Kubernetes.",
            skills=["Java", "Spring Boot", "Docker", "Kubernetes", "AWS"],
            total_experience_years=6.0,
            experience_text="6 years architecting high-volume microservices using Java 17, Spring Boot, Docker containers, and AWS EKS.",
            projects_text="High-throughput payment gateway built in Spring Boot and MySQL.",
        ),
        CandidateSpec(
            id="junior-fit",
            name="Bob Junior",
            summary="Junior Java developer with 1.5 years experience in Spring Boot.",
            skills=["Java", "Spring Boot", "Docker"],
            total_experience_years=1.5,
            experience_text="1.5 years developing REST endpoints in Spring Boot and Docker.",
            projects_text="Student portal in Spring Boot.",
        ),
        CandidateSpec(
            id="cross-domain-senior",
            name="Charlie Python",
            summary="Senior Python architect with 8 years building Django and FastAPI services on AWS.",
            skills=["Python", "Django", "FastAPI", "Docker", "AWS"],  # Missing Java, Spring Boot
            total_experience_years=8.0,
            experience_text="8 years designing cloud architecture in Python and AWS.",
            projects_text="Cloud monitoring dashboard in Python.",
        ),
        CandidateSpec(
            id="frontend-dev",
            name="Dana Frontend",
            summary="Senior Frontend Engineer with 5 years in React and TypeScript.",
            skills=["React", "TypeScript", "HTML5/CSS3"],
            total_experience_years=5.0,
            experience_text="5 years building responsive web apps with React and TypeScript.",
            projects_text="Interactive design system in React.",
        ),
        CandidateSpec(
            id="unrelated-noise",
            name="Edward Retail",
            summary="Experienced store manager with 8 years managing retail inventory and sales teams.",
            skills=[],
            total_experience_years=8.0,
            experience_text="8 years retail customer service and inventory replenishment.",
            projects_text="Retail sales optimization program.",
        ),
    ]

    request = MatchRequest(job=job, candidates=candidates)
    response = matching_engine.evaluate_match(request)

    assert response.total_candidates_evaluated == 5
    assert response.eligible_candidates_count == 2  # Only Alice and Bob have both Java and Spring Boot

    ranked_ids = [m.candidate_id for m in response.matches]

    # Verification 1: Alice Exact MUST be rank #1
    assert ranked_ids[0] == "exact-fit"
    alice_match = response.matches[0]
    assert alice_match.is_eligible is True
    assert alice_match.score >= 80.0
    assert alice_match.score_breakdown.experience_relevance == 100.0

    # Verification 2: Bob Junior MUST be rank #2 (eligible, but lower experience relevance)
    assert ranked_ids[1] == "junior-fit"
    bob_match = response.matches[1]
    assert bob_match.is_eligible is True
    assert bob_match.score_breakdown.experience_relevance == 30.0  # 1.5 / 5.0 * 100
    assert alice_match.score > bob_match.score

    # Verification 3: Charlie, Dana, Edward MUST NOT be eligible
    ineligible_matches = {m.candidate_id: m for m in response.matches[2:]}
    assert ineligible_matches["cross-domain-senior"].is_eligible is False
    assert ineligible_matches["frontend-dev"].is_eligible is False
    assert ineligible_matches["unrelated-noise"].is_eligible is False

    # Verification 4: Edward (Retail) must have the lowest score
    assert ranked_ids[-1] == "unrelated-noise"
    assert response.matches[-1].score < 25.0
