"""
TalentMatch AI - AI Service Internal API Endpoints
"""

import logging
from typing import Dict
from fastapi import APIRouter, Depends, HTTPException, status

from app.config import settings
from app.security import verify_service_key
from app.nlp.taxonomy import skill_taxonomy
from app.nlp.models import ResumeExtractionRequest, ResumeExtractionResponse
from app.nlp.extractor import resume_extractor
from app.matching.models import (
    EmbeddingRequest,
    EmbeddingResponse,
    MatchRequest,
    MatchResponse,
)
from app.matching.embedder import embedder
from app.matching.engine import matching_engine

logger = logging.getLogger("talentmatch.ai")

router = APIRouter(prefix="/internal/v1")


@router.get("/health", tags=["Health"])
async def health_check() -> Dict[str, str]:
    """
    Health check endpoint reporting service, model, and taxonomy status.
    """
    return {
        "status": "UP",
        "service": "talentmatch-ai-service",
        "version": settings.SERVICE_VERSION,
        "spacy_model": settings.SPACY_MODEL,
        "embedding_model": settings.MODEL_NAME,
        "taxonomy_version": skill_taxonomy.version,
        "canonical_skills_loaded": str(skill_taxonomy.skill_count),
    }


@router.post(
    "/resumes/extract",
    response_model=ResumeExtractionResponse,
    tags=["Resume Extraction"],
    dependencies=[Depends(verify_service_key)],
)
async def extract_resume(request: ResumeExtractionRequest) -> ResumeExtractionResponse:
    """
    Extracts structured entities, normalized skills, work history, education,
    and provenance metadata from raw resume text.
    """
    if len(request.raw_text) > settings.MAX_INPUT_LENGTH:
        raise HTTPException(
            status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
            detail=f"Resume text exceeds maximum limit of {settings.MAX_INPUT_LENGTH} characters",
        )

    try:
        response = resume_extractor.extract(request.raw_text, candidate_id=request.candidate_id)
        return response
    except Exception as e:
        logger.error(f"Error extracting resume: {e}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"NLP extraction failed: {str(e)}",
        )


@router.post(
    "/embeddings",
    response_model=EmbeddingResponse,
    tags=["Embeddings"],
    dependencies=[Depends(verify_service_key)],
)
async def generate_embeddings(request: EmbeddingRequest) -> EmbeddingResponse:
    """
    Generates dense 384-dimensional unit-normalized Sentence Transformer embeddings for a list of texts.
    """
    try:
        embeddings = embedder.encode(request.texts, normalize=True)
        return EmbeddingResponse(
            model=embedder.model_name,
            dimension=384,
            embeddings=embeddings.tolist(),
        )
    except Exception as e:
        logger.error(f"Error generating embeddings: {e}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Embedding generation failed: {str(e)}",
        )


@router.post(
    "/matches/retrieve",
    response_model=MatchResponse,
    tags=["Matching"],
    dependencies=[Depends(verify_service_key)],
)
async def retrieve_matches(request: MatchRequest) -> MatchResponse:
    """
    Executes multi-factor semantic retrieval, mandatory eligibility filtering,
    skill coverage calculation, and explainable candidate scoring.
    """
    try:
        return matching_engine.evaluate_match(request)
    except Exception as e:
        logger.error(f"Error retrieving candidate matches: {e}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Matching retrieval failed: {str(e)}",
        )
