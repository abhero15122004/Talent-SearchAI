# TalentMatch AI - Python NLP & Semantic Matching Service

The **TalentMatch AI Service** provides natural language processing, deterministic skill normalization, and dense neural semantic retrieval for the TalentMatch recruitment platform.

---

## 1. Architecture & Capabilities

```
+-------------------------------------------------------------+
|                      Spring Boot 3.x                        |
|   (ResumeFileProcessor, MatchingService, JobController)     |
+------------------------------+------------------------------+
                               |
                   HTTP POST / JSON (with Auth)
                Header: X-Internal-Service-Key
                               |
                               v
+-------------------------------------------------------------+
|                  Python AI Service (FastAPI)                |
|                                                             |
|  +--------------------------+  +-------------------------+  |
|  |     Resume Extraction    |  |    Semantic Retrieval   |  |
|  |  - spaCy (en_core_web_sm)|  |  - all-MiniLM-L6-v2     |  |
|  |  - Rule-based Segmenter  |  |  - 384-dim Embeddings   |  |
|  |  - Section Parsers       |  |  - Cosine Similarity    |  |
|  +------------+-------------+  +------------+------------+  |
|               |                             |               |
|               +--------------+--------------+               |
|                              v                              |
|               +-----------------------------+               |
|               | Skill Taxonomy Engine v1.0.0|               |
|               |  - Canonical mapping        |               |
|               |  - Synonym normalization    |               |
|               |  - Category indexing        |               |
|               +-----------------------------+               |
+-------------------------------------------------------------+
```

### Core Components:
1. **Versioned Skill Taxonomy (v1.0.0)**:
   - Maintains canonical definitions, categories, and alias synonyms (e.g. `Spring Framework`, `springboot`, `spring` -> `Spring Boot`; `k8s`, `kube` -> `Kubernetes`; `ts` -> `TypeScript`).
   - Normalizes text casing, trailing punctuation, and sub-tokens while preserving original extracted text and evidence context.
2. **Structured Resume Extractor**:
   - Parses contact details (email, phone, name), work history with dates and duration calculation, education with degree detection, and project descriptions.
   - Generates provenance metadata (`extractor_version`, `taxonomy_version`, `spacy_model`, `processed_at`) and explicitly flags missing/uncertain fields (`uncertainty_indicators`).
3. **Dense Neural Embedder**:
   - Generates unit-normalized 384-dimensional embeddings using Sentence Transformers `all-MiniLM-L6-v2`.
   - Computes cosine similarity between job descriptions and candidate profiles.
4. **Explainable Multi-Factor Matching Engine**:
   - Enforces **Mandatory Skill Eligibility Gating**: Candidates lacking mandatory job requirements are explicitly identified and penalized.
   - Computes **Normalized Skill Coverage** across mandatory and preferred requirements.
   - Computes **Experience Relevance** against job minimum experience thresholds.
   - Computes **Project Relevance** and **Semantic Similarity**.
   - Respects **Job-Specific Weights** configured per requisition.
   - Enforces `min_score_threshold` without silently lowering thresholds if fewer candidates qualify than the target shortlist count.

---

## 2. Internal API Specification

All mutation and retrieval endpoints require the `X-Internal-Service-Key` header matching `AI_SERVICE_KEY`.

### Endpoints:
- `GET /internal/v1/health`:
  - Public health check reporting service status, models loaded, and taxonomy version.
- `POST /internal/v1/resumes/extract`:
  - Request: `{"raw_text": "...", "candidate_id": "optional-id"}`
  - Response: Structured JSON with contact details, canonical skills, work experience, education, projects, provenance, and uncertainty indicators.
- `POST /internal/v1/embeddings`:
  - Request: `{"texts": ["text 1", "text 2"]}`
  - Response: 384-dimensional vector arrays.
- `POST /internal/v1/matches/retrieve`:
  - Request: `MatchRequest` (Job spec with weights + List of candidate specs).
  - Response: Ranked candidates with `final_score`, multi-factor `score_breakdown`, `is_eligible`, `matched_skills`, `missing_mandatory_skills`, and contextual `evidence_snippets`.

---

## 3. Synthetic Benchmark & Retrieval Evaluation

The test suite includes an automated benchmark (`tests/test_benchmarks.py`) evaluating diverse candidate archetypes against a realistic Senior Java requisition:
- **Archetype 1 (Exact Fit Senior)**: 6 years Java, Spring Boot, Docker, Kubernetes, AWS.
  - Result: Rank #1, `is_eligible: true`, Score: > 80%, Experience Relevance: 100%.
- **Archetype 2 (Junior in Domain)**: 1.5 years Java, Spring Boot, Docker.
  - Result: Rank #2, `is_eligible: true`, Experience Relevance: 30%, Lower overall score.
- **Archetype 3 (Cross-domain Senior)**: 8 years Python/Django/AWS.
  - Result: `is_eligible: false` (Missing mandatory Java & Spring Boot), Score penalized.
- **Archetype 4 (Frontend Specialist)**: 5 years React/TypeScript.
  - Result: `is_eligible: false` (Missing mandatory backend skills).
- **Archetype 5 (Unrelated Domain)**: 8 years retail management.
  - Result: `is_eligible: false`, Bottom rank (< 25% score).

---

## 4. Local Execution & Testing

### Virtual Environment Interpreter:
The project is configured with a dedicated virtual environment at `.venv/`:
- **PowerShell / Windows**:
  ```powershell
  Set-Location CRM/ai-service
  .\.venv\Scripts\Activate.ps1
  ```
- **Direct execution with venv python**:
  ```powershell
  .\.venv\Scripts\python.exe -m pytest -v
  ```

### Running Tests:
```powershell
Set-Location CRM/ai-service
.\.venv\Scripts\python.exe -m pytest -v
```

### Starting the AI Service:
```powershell
Set-Location CRM/ai-service
.\.venv\Scripts\python.exe -m uvicorn app.main:app --host 0.0.0.0 --port 8000
```
