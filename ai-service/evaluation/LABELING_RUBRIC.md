# TalentMatch AI — Candidate Relevance Labeling Rubric & Evaluation Guide

**Version**: `1.0.0-eval`  
**Author**: Lead Software Architect & AI Engineering Team  
**Scope**: Offline Evaluation Dataset for Candidate-Job Matching & Ranking  

---

## 1. Ethical Guidelines & Data Usage Disclaimer

> [!IMPORTANT]
> **Synthetic & Anonymized Data Policy**  
> All candidate profiles and job requisitions in this evaluation benchmark are **synthetically generated or strictly anonymized**. No real personal identifiable information (PII), contact data, or protected demographic attributes are used.  
> 
> **Algorithmic Evaluation Disclaimer**  
> Performance metrics derived from this benchmark (NDCG@K, Precision@K, Recall@K, MRR) are used exclusively for **technical algorithmic calibration, retrieval comparison, and regression testing**. They **MUST NEVER** be presented as empirical proof or guarantees of real-world human job performance, hireability, or organizational recruitment outcomes.

---

## 2. Distinction: Human-Reviewed vs. Model-Generated Labels

To prevent circular evaluation and evaluation contamination:
1. **Human-Reviewed Ground Truth Labels (`relevance_label`)**:
   - Manually reviewed and calibrated by technical recruiting domain experts following this strict rubric.
   - Independent of model predictions, embedding scores, and TF-IDF weights.
   - Immutable across baseline comparisons.
2. **Model/Baseline Predictions (`model_score`)**:
   - Produced dynamically by the candidate ranking algorithms (TF-IDF, Pure Semantic Embedder, Phase 5 Hybrid, Cross-Encoder).
   - Ranked and compared against the human ground truth.

---

## 3. Four-Point Relevance Grading Rubric

Each candidate-job pair is assigned an integer label from **0 to 3**:

```
+-------+--------------------+---------------------------------------------------------------+
| Grade | Label              | Operational Definition                                        |
+-------+--------------------+---------------------------------------------------------------+
|   3   | Highly Relevant    | Exceeds/meets all mandatory skills, has target domain tenure,  |
|       |                    | verified project evidence, and multiple preferred skills.     |
+-------+--------------------+---------------------------------------------------------------+
|   2   | Relevant           | Meets all mandatory skills and target domain tenure; moderate  |
|       |                    | preferred skill coverage or relevant adjacent experience.     |
+-------+--------------------+---------------------------------------------------------------+
|   1   | Partially Relevant | Meets some adjacent skills or junior tenure, but misses key    |
|       |                    | mandatory requirements or lacks verified stack experience.    |
+-------+--------------------+---------------------------------------------------------------+
|   0   | Not Relevant       | Completely unrelated discipline, wrong technology family,      |
|       |                    | or superficial keyword stuffing without domain knowledge.     |
+-------+--------------------+---------------------------------------------------------------+
```

### Detailed Grading Criteria

#### Grade 3: Highly Relevant (Target Shortlist Candidate)
- **Mandatory Requirements**: 100% satisfied. The candidate demonstrates clear, verifiable professional or substantial project tenure in every mandatory skill.
- **Experience Tenure**: Meets or exceeds the job's minimum required experience years.
- **Preferred Requirements**: Demonstrates at least 50% of the preferred skills.
- **Project Evidence**: Real-world architectural, delivery, or code contributions in the primary stack.
- **Synonym Handling**: Recognized canonical competencies (e.g., `Spring Boot` when job specifies `Spring Framework` or vice versa).

#### Grade 2: Relevant (Qualified for Technical Screen)
- **Mandatory Requirements**: 100% satisfied. The candidate meets all mandatory competencies.
- **Experience Tenure**: Meets the job's minimum tenure requirement or is within 1 year with strong project evidence.
- **Preferred Requirements**: May possess few or no preferred skills, but core competence is solid.
- **Project Evidence**: Has working experience or academic/personal projects in the required domain.

#### Grade 1: Partially Relevant (Marginal / Non-Viable for Current Role)
- **Mandatory Requirements**: **Misses one or more mandatory requirements**, OR meets them with severely insufficient tenure (e.g., 6 months experience for a Senior 5+ year role).
- **Adjacent Transferability**: Candidate is strong in a neighboring stack (e.g., Senior Django/Python engineer applying for a Senior Spring/Java position).
- **Keyword Stuffing**: Candidate mentions the target keywords only in an educational list or seminar attendance with zero demonstrable production experience.
- *Rule*: Any candidate missing a mandatory job requirement CANNOT receive a score higher than 1.

#### Grade 0: Not Relevant (Immediate Rejection)
- **Domain Mismatch**: Candidate comes from an entirely non-technical or disjoint field (e.g., Retail Store Manager, Sales Representative, Accountant).
- **Tech Stack Disconnect**: Complete absence of overlapping software engineering competencies (e.g., iOS Swift mobile specialist applying for a Cloud DevOps Kubernetes role with no infrastructure experience).
- **Malformed / Empty**: Incomplete, corrupted, or non-informative resume text.

---

## 4. Specific Challenge Archetypes & Labeling Rules

To thoroughly evaluate model robustness against real-world recruitment challenges, the dataset includes specific edge-case archetypes:

### A. Synonym and Abbreviation Variations
- **Rule**: If a candidate uses valid industry synonyms (e.g., `K8s` for `Kubernetes`, `TS` for `TypeScript`, `Postgres` for `PostgreSQL`, `AWS` for `Amazon Web Services`), they **must be recognized as meeting the requirement**.
- **Rubric**: If all other criteria are met, Grade 2 or 3 applies. The baseline models must be evaluated on whether their taxonomy normalizes these correctly.

### B. Keyword Stuffing / Keyword False Positives
- **Rule**: Candidates who list target keywords without context (e.g. "Attended 1-day seminar on Java 17", or copy-pasting the job description in a footer) **must be penalized**.
- **Rubric**: Grade 0 or 1. Lexical search (TF-IDF) typically fails here by ranking these candidates near the top; the evaluation framework must catch this error.

### C. Semantic False Positives (Buzzword Overload)
- **Rule**: Candidates whose summaries are loaded with broad, high-level corporate architecture jargon ("orchestrated high-throughput scalable distributed cloud paradigms with visionary leadership") but contain **no concrete evidence of the mandatory tech stack**.
- **Rubric**: Grade 1 or 0. Dense embedding models (Sentence Transformers) often score these candidates artificially high due to conceptual similarity; the evaluation framework tracks these semantic false positives.

### D. Missing Mandatory Requirements
- **Rule**: A candidate with 10 years of general engineering experience who lacks a mandatory role requirement (e.g., no `Java` for a Java microservices requisition).
- **Rubric**: Must be graded Grade 1. Hybrid gating should down-rank them, whereas pure semantic or keyword models may mistakenly place them in the top 3.

---

## 5. Skill Extraction Evaluation Guidelines

In addition to ranking relevance, each candidate in the dataset has a curated ground-truth list of canonical skills (`annotated_skills`):
- **Precision**: $\frac{\text{True Positives}}{\text{True Positives} + \text{False Positives}}$ (avoiding hallucinated or irrelevant skills).
- **Recall**: $\frac{\text{True Positives}}{\text{True Positives} + \text{False Negatives}}$ (capturing all valid candidate skills).
- **F1 Score**: Harmonic mean of Precision and Recall.

---

## 6. Train / Validation / Test Splitting

To prevent cross-contamination and data leakage:
- **Grouping Rule**: Data is grouped strictly by **Job Requisition Identifier**.
- No candidate resumes or job descriptions in the test split are ever present in the training or parameter calibration splits.
- This tests how well the models generalize to **unseen job descriptions and novel candidate pools**.
