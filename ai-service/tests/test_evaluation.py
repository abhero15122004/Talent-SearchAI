"""
TalentMatch AI — AI Evaluation & Benchmark Test Suite
Validates ranking metrics, baselines, extraction metrics, and experiment runner.
"""

import json
import os
from pathlib import Path
import pytest

# Ensure environment safety
os.environ["USE_TF"] = "0"
os.environ["TF_ENABLE_ONEDNN_OPTS"] = "0"
os.environ["HF_HUB_OFFLINE"] = "1"
os.environ["TRANSFORMERS_OFFLINE"] = "1"

from evaluation.metrics import (
    ndcg_at_k,
    precision_at_k,
    recall_at_k,
    reciprocal_rank,
    skill_extraction_metrics,
    aggregate_ranking_metrics,
    aggregate_extraction_metrics,
)
from evaluation.baselines import (
    TfidfRankingBaseline,
    DenseSemanticBaseline,
    HybridRankingBaseline,
    CrossEncoderReranker,
)
from evaluation.run_experiments import run_evaluation, load_dataset


# =========================================================================
# 1. Ranking & Extraction Metric Tests
# =========================================================================

def test_ndcg_at_k_perfect_ranking():
    # Ideal ranking: candidates predicted in exact ground truth relevance order
    y_true = [3.0, 2.0, 1.0, 0.0]
    y_pred = [0.95, 0.80, 0.50, 0.10]

    score = ndcg_at_k(y_true, y_pred, k=3)
    assert score == 1.0, f"Expected perfect NDCG@3 of 1.0, got {score}"


def test_ndcg_at_k_inverted_ranking():
    # Completely inverted ranking: worst candidate ranked first
    y_true = [3.0, 2.0, 1.0, 0.0]
    y_pred = [0.10, 0.20, 0.80, 0.99]

    score = ndcg_at_k(y_true, y_pred, k=3)
    assert score < 0.60, f"Expected degraded NDCG@3 for inverted ranking, got {score}"


def test_ndcg_at_k_degenerate_cases():
    # Degenerate case 1: empty lists
    assert ndcg_at_k([], [], k=3) == 0.0

    # Degenerate case 2: all items have 0 ground truth relevance
    assert ndcg_at_k([0.0, 0.0, 0.0], [0.9, 0.8, 0.7], k=3) == 1.0

    # Degenerate case 3: k is larger than candidate pool
    y_true = [3.0, 1.0]
    y_pred = [0.9, 0.2]
    assert ndcg_at_k(y_true, y_pred, k=10) == 1.0


def test_precision_and_recall_at_k():
    y_true = [3.0, 1.0, 2.0, 0.0]
    y_pred = [0.9, 0.8, 0.7, 0.1]
    # At k=2: top predicted are idx 0 (rel 3) and idx 1 (rel 1).
    # Relevant items (>= 2): idx 0 and idx 2 (total 2 relevant).
    # In top 2: only idx 0 is relevant.
    # P@2 = 1 / 2 = 0.5
    # R@2 = 1 / 2 = 0.5
    assert precision_at_k(y_true, y_pred, k=2, threshold=2.0) == 0.5
    assert recall_at_k(y_true, y_pred, k=2, threshold=2.0) == 0.5


def test_reciprocal_rank():
    # First relevant candidate (>= 2.0) is at rank 1
    assert reciprocal_rank([3.0, 2.0, 0.0], [0.9, 0.5, 0.1], threshold=2.0) == 1.0

    # First relevant candidate is at rank 2
    assert reciprocal_rank([1.0, 3.0, 0.0], [0.9, 0.8, 0.1], threshold=2.0) == 0.5

    # No relevant candidates
    assert reciprocal_rank([0.0, 1.0, 0.0], [0.9, 0.8, 0.1], threshold=2.0) == 0.0


def test_skill_extraction_metrics():
    # Exact match
    m = skill_extraction_metrics(["Java", "Spring Boot"], ["java", "spring boot"])
    assert m["precision"] == 1.0
    assert m["recall"] == 1.0
    assert m["f1"] == 1.0

    # Partial match: 1 TP, 1 FP (Docker), 1 FN (AWS)
    m2 = skill_extraction_metrics(["Java", "Docker"], ["Java", "AWS"])
    assert m2["precision"] == 0.5
    assert m2["recall"] == 0.5
    assert m2["f1"] == 0.5


# =========================================================================
# 2. Baseline Model Execution Tests
# =========================================================================

@pytest.fixture
def sample_job_and_candidates():
    job = {
        "id": "test-job-1",
        "title": "Java Backend Engineer",
        "description": "Building Spring Boot services with MySQL and Docker.",
        "mandatory_skills": ["Java", "Spring Boot"],
        "preferred_skills": ["Docker", "MySQL"],
        "min_experience_years": 3.0,
        "weights": {"skill_coverage": 0.4, "experience": 0.3, "project_relevance": 0.1, "semantic_similarity": 0.2},
    }
    candidates = [
        {
            "candidate_id": "c1",
            "name": "Alice Senior",
            "summary": "Senior Java developer with 5 years Spring Boot and Docker.",
            "skills": ["Java", "Spring Boot", "Docker", "MySQL"],
            "total_experience_years": 5.0,
            "experience_text": "5 years Java and Spring Boot development.",
            "projects_text": "Microservices platform in Spring Boot.",
            "raw_text": "Alice Senior. Java, Spring Boot, Docker.",
            "relevance_label": 3,
        },
        {
            "candidate_id": "c2",
            "name": "Bob Retail",
            "summary": "Store manager with customer service experience.",
            "skills": ["Sales", "Customer Service"],
            "total_experience_years": 4.0,
            "experience_text": "Retail store manager.",
            "projects_text": "",
            "raw_text": "Bob Retail. Customer service.",
            "relevance_label": 0,
        },
    ]
    return job, candidates


def test_tfidf_baseline(sample_job_and_candidates):
    job, candidates = sample_job_and_candidates
    model = TfidfRankingBaseline()
    scores, latency = model.rank_candidates(job, candidates)

    assert len(scores) == 2
    assert latency > 0.0
    # Alice should score higher than Bob in lexical overlap
    assert scores[0] > scores[1]


def test_dense_semantic_baseline(sample_job_and_candidates):
    job, candidates = sample_job_and_candidates
    model = DenseSemanticBaseline()
    scores, latency = model.rank_candidates(job, candidates)

    assert len(scores) == 2
    assert latency > 0.0
    assert scores[0] > scores[1]


def test_hybrid_ranking_baseline(sample_job_and_candidates):
    job, candidates = sample_job_and_candidates
    model = HybridRankingBaseline()
    scores, latency = model.rank_candidates(job, candidates)

    assert len(scores) == 2
    assert scores[0] > 70.0
    assert scores[1] < 40.0


def test_cross_encoder_reranker_fallback(sample_job_and_candidates):
    job, candidates = sample_job_and_candidates
    # Instantiate with fallback capability
    model = CrossEncoderReranker(top_n=2)
    scores, latency = model.rank_candidates(job, candidates)

    assert len(scores) == 2
    assert latency > 0.0
    assert scores[0] > scores[1]


# =========================================================================
# 3. Dataset Integrity & Schema Validation
# =========================================================================

def test_evaluation_dataset_integrity():
    dataset_path = Path(__file__).resolve().parent.parent / "evaluation" / "data" / "dataset_v1.json"
    assert dataset_path.exists(), "Evaluation dataset dataset_v1.json must exist"

    data = load_dataset(str(dataset_path))
    assert "dataset_version" in data
    assert "splits" in data
    assert "jobs" in data

    jobs = data["jobs"]
    assert len(jobs) >= 5, "Dataset must contain at least 5 job requisitions"

    total_candidates = 0
    for job in jobs:
        assert job.get("id"), "Job must have an ID"
        assert job.get("title"), "Job must have a title"
        assert len(job.get("mandatory_skills", [])) > 0, "Job must have mandatory skills"

        candidates = job.get("candidates", [])
        assert len(candidates) >= 4, f"Job {job['id']} must have at least 4 candidates"
        total_candidates += len(candidates)

        for cand in candidates:
            assert cand.get("candidate_id"), "Candidate must have ID"
            assert cand.get("relevance_label") in [0, 1, 2, 3], f"Invalid relevance label {cand.get('relevance_label')}"
            assert cand.get("relevance_label_human_reviewed") is True
            assert "annotated_skills" in cand

    assert total_candidates >= 25, f"Expected at least 25 candidates, found {total_candidates}"
