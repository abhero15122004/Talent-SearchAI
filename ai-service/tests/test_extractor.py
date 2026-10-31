"""
Tests for Structured Resume Extraction, Section Parsing, and Normalization
"""

import pytest
from app.nlp.extractor import resume_extractor


def test_standard_resume_extraction():
    sample_resume = """
    Sarah Connor
    sarah.connor@example.com
    (555) 123-4567

    Professional Summary
    Senior Full Stack Engineer with 6+ years of experience designing scalable microservices.

    Technical Skills
    Java, Spring Framework, TypeScript, React, Docker, Kubernetes, MySQL, AWS

    Work Experience
    Lead Software Engineer | Acme Corp (2020 - Present)
    - Architected distributed backend services in Spring Boot and MySQL
    - Deployed containerized applications to AWS EKS using Docker and Kubernetes

    Software Engineer | TechStart (2018 - 2020)
    - Developed user interfaces using React and TypeScript
    - Built RESTful APIs in Python and FastAPI

    Education
    Bachelor of Science in Computer Science | Stanford University, 2018

    Projects
    Talent Cloud Platform
    - Real-time recruitment platform built with Spring Boot and React
    """

    res = resume_extractor.extract(sample_resume, candidate_id="cand-101")

    assert res.candidate_id == "cand-101"
    assert res.email == "sarah.connor@example.com"
    assert res.phone == "(555) 123-4567"
    assert "Sarah Connor" in (res.full_name or "")
    assert res.summary is not None
    assert "Senior Full Stack Engineer" in res.summary

    # Skills verification
    extracted_canonical = [s.canonical_name for s in res.skills]
    assert "Spring Boot" in extracted_canonical
    assert "Docker" in extracted_canonical
    assert "Kubernetes" in extracted_canonical
    assert "React" in extracted_canonical
    assert "MySQL" in extracted_canonical
    assert "Java" in extracted_canonical

    # Confidence and evidence snippets
    for skill in res.skills:
        assert skill.confidence >= 0.8
        assert skill.category is not None

    # Experience duration
    assert res.total_years_experience is not None
    assert res.total_years_experience >= 4.0
    assert len(res.work_experience) >= 2

    # Education
    assert len(res.education) >= 1
    assert any("Stanford" in (e.institution or "") for e in res.education)

    # Provenance
    assert res.provenance.taxonomy_version == "1.0.0"
    assert res.provenance.spacy_model is not None


def test_empty_resume_handling():
    res = resume_extractor.extract("", candidate_id="empty-1")
    assert res.candidate_id == "empty-1"
    assert res.email is None
    assert len(res.skills) == 0
    assert len(res.uncertainty_indicators) > 0
    assert any("empty" in u.lower() for u in res.uncertainty_indicators)


def test_noisy_and_abbreviated_resume():
    noisy_text = """
    Alex Mercer
    Contact: alex99@dev.io
    Core tools: k8s, py3, ts, gcp, psql.
    Past work:
    Dev at CloudInc from 2021 to 2023. Worked on k8s clusters and psql databases.
    """
    res = resume_extractor.extract(noisy_text)
    assert res.email == "alex99@dev.io"
    skill_names = [s.canonical_name for s in res.skills]

    # Verify abbreviations correctly mapped
    assert "Kubernetes" in skill_names
    assert "Python" in skill_names
    assert "TypeScript" in skill_names
    assert "PostgreSQL" in skill_names
    assert "Google Cloud Platform (GCP)" in skill_names


def test_missing_sections_sets_uncertainty():
    sparse_text = "John Smith\nEmail: jsmith@domain.com\nJust a simple note without any work experience or education."
    res = resume_extractor.extract(sparse_text)
    assert len(res.work_experience) == 0
    assert len(res.education) == 0
    assert any("work experience" in u.lower() for u in res.uncertainty_indicators)
    assert any("education" in u.lower() for u in res.uncertainty_indicators)
