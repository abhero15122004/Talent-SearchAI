"""
Tests for Versioned Skill Taxonomy and Normalization
"""

import pytest
from app.nlp.taxonomy import skill_taxonomy, CanonicalSkill


def test_taxonomy_initialization():
    assert skill_taxonomy.version == "1.0.0"
    assert skill_taxonomy.skill_count > 25
    skills = skill_taxonomy.get_all_canonical_skills()
    assert any(s.name == "Spring Boot" for s in skills)
    assert any(s.name == "Kubernetes" for s in skills)


@pytest.mark.parametrize(
    "raw_term,expected_canonical_name",
    [
        ("spring framework", "Spring Boot"),
        ("Spring Boot", "Spring Boot"),
        ("springboot", "Spring Boot"),
        ("spring", "Spring Boot"),
        ("k8s", "Kubernetes"),
        ("kubernetes", "Kubernetes"),
        ("kube", "Kubernetes"),
        ("golang", "Go"),
        ("react.js", "React"),
        ("REACT", "React"),
        ("TypeScript", "TypeScript"),
        ("ts", "TypeScript"),
        ("amazon web services", "Amazon Web Services (AWS)"),
        ("aws", "Amazon Web Services (AWS)"),
        ("postgres", "PostgreSQL"),
        ("postgresql", "PostgreSQL"),
        ("psql", "PostgreSQL"),
        ("rest api", "REST APIs"),
        ("restful apis", "REST APIs"),
    ],
)
def test_skill_synonym_normalization(raw_term, expected_canonical_name):
    canonical = skill_taxonomy.normalize(raw_term)
    assert canonical is not None, f"Failed to normalize {raw_term}"
    assert canonical.name == expected_canonical_name


def test_punctuation_and_whitespace_handling():
    assert skill_taxonomy.normalize("  Docker  ").name == "Docker"
    assert skill_taxonomy.normalize("React,").name == "React"
    assert skill_taxonomy.normalize("Python;").name == "Python"
    assert skill_taxonomy.normalize("Java.").name == "Java"


def test_unrecognized_skill_returns_none():
    assert skill_taxonomy.normalize("Underwater Basket Weaving") is None
    assert skill_taxonomy.normalize("RandomNonExistentSkill123") is None
    assert skill_taxonomy.normalize("") is None
    assert skill_taxonomy.normalize(None) is None
