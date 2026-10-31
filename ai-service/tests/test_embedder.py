"""
Tests for Sentence Transformers Embeddings and Vector Similarity
"""

import numpy as np
from app.matching.embedder import embedder


def test_embedder_dimension():
    text = "Java Spring Boot microservices and REST APIs"
    vector = embedder.encode(text)
    assert isinstance(vector, np.ndarray)
    assert vector.shape == (384,)
    # Unit normalization check
    norm = np.linalg.norm(vector)
    assert np.isclose(norm, 1.0, atol=1e-3)


def test_batch_encoding():
    texts = [
        "React and TypeScript frontend engineer",
        "Python FastAPI and data engineering",
    ]
    matrix = embedder.encode(texts)
    assert matrix.shape == (2, 384)


def test_semantic_similarity_relative_ordering():
    job_req = "Senior Backend Engineer with Java, Spring Boot, and PostgreSQL microservices."
    relevant_cand = "Java backend developer with extensive experience building Spring Boot microservices and SQL queries."
    irrelevant_cand = "Professional chef specializing in French culinary techniques and kitchen staff management."

    job_vec = embedder.encode(job_req)
    rel_vec = embedder.encode(relevant_cand)
    irrel_vec = embedder.encode(irrelevant_cand)

    sim_relevant = embedder.cosine_similarity(job_vec, rel_vec)
    sim_irrelevant = embedder.cosine_similarity(job_vec, irrel_vec)

    assert sim_relevant > 0.65
    assert sim_irrelevant < 0.25
    assert sim_relevant > (sim_irrelevant + 0.4)
