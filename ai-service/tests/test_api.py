"""
Tests for AI Service FastAPI Endpoints and Authentication
"""

import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.config import settings

client = TestClient(app)

TEST_AUTH_HEADER = {"X-Internal-Service-Key": settings.SERVICE_KEY}


def test_health_endpoint():
    response = client.get("/internal/v1/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "UP"
    assert data["service"] == "talentmatch-ai-service"
    assert data["taxonomy_version"] == "1.0.0"
    assert int(data["canonical_skills_loaded"]) > 20


def test_unauthorized_access_rejected():
    # Without header
    res1 = client.post("/internal/v1/resumes/extract", json={"raw_text": "Sample text"})
    assert res1.status_code == 401

    # With invalid secret
    res2 = client.post(
        "/internal/v1/resumes/extract",
        json={"raw_text": "Sample text"},
        headers={"X-Internal-Service-Key": "wrong-secret-key"},
    )
    assert res2.status_code == 401


def test_resume_extract_endpoint():
    payload = {
        "candidate_id": "cand-test-1",
        "raw_text": "Jane Doe\njane@test.com\n\nExperience\nSoftware Engineer 2021-2023 at Corp.\nBuilt microservices with Spring Boot and Docker.\n\nSkills\nJava, Spring Boot, Docker",
    }
    response = client.post(
        "/internal/v1/resumes/extract",
        json=payload,
        headers=TEST_AUTH_HEADER,
    )
    assert response.status_code == 200
    data = response.json()
    assert data["candidate_id"] == "cand-test-1"
    assert data["email"] == "jane@test.com"
    skill_names = [s["canonical_name"] for s in data["skills"]]
    assert "Spring Boot" in skill_names
    assert "Docker" in skill_names


def test_embeddings_endpoint():
    payload = {"texts": ["Java backend engineer", "React frontend developer"]}
    response = client.post(
        "/internal/v1/embeddings",
        json=payload,
        headers=TEST_AUTH_HEADER,
    )
    assert response.status_code == 200
    data = response.json()
    assert data["dimension"] == 384
    assert len(data["embeddings"]) == 2
    assert len(data["embeddings"][0]) == 384


def test_matches_retrieve_endpoint():
    payload = {
        "job": {
            "id": "job-api-1",
            "title": "Cloud Architect",
            "description": "Architect cloud systems using AWS and Kubernetes.",
            "mandatory_skills": ["Amazon Web Services (AWS)"],
            "preferred_skills": ["Kubernetes"],
            "min_experience_years": 3.0,
            "weight_skill_coverage": 0.4,
            "weight_experience": 0.2,
            "weight_project_relevance": 0.1,
            "weight_semantic_similarity": 0.3,
            "shortlist_count": 3,
        },
        "candidates": [
            {
                "id": "cand-api-1",
                "name": "Cloud Pro",
                "summary": "Cloud engineer with 5 years AWS and Kubernetes experience.",
                "skills": ["AWS", "Kubernetes"],
                "total_experience_years": 5.0,
                "experience_text": "5 years AWS and Kubernetes.",
            }
        ],
    }
    response = client.post(
        "/internal/v1/matches/retrieve",
        json=payload,
        headers=TEST_AUTH_HEADER,
    )
    assert response.status_code == 200
    data = response.json()
    assert data["job_id"] == "job-api-1"
    assert data["total_candidates_evaluated"] == 1
    assert data["shortlisted_count"] == 1
    assert data["matches"][0]["candidate_id"] == "cand-api-1"
    assert data["matches"][0]["is_eligible"] is True
