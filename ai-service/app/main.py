"""
TalentMatch AI - NLP & Semantic Matching FastAPI Service
"""

import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI, Request, status
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.api.routes import router as api_router
from app.matching.embedder import embedder
from app.nlp.extractor import resume_extractor

# Structured logging configuration
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] [%(name)s]: %(message)s",
)
logger = logging.getLogger("talentmatch.ai")


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Service lifespan handler: Preloads models into memory on startup
    to ensure predictable response latencies on first incoming requests.
    """
    logger.info("Initializing TalentMatch AI Service...")
    logger.info(f"Loading spaCy model: {settings.SPACY_MODEL}...")
    _ = resume_extractor.nlp
    logger.info(f"Loading Sentence Transformers model: {settings.MODEL_NAME}...")
    _ = embedder.model
    logger.info("TalentMatch AI Service is ready to serve requests.")
    yield
    logger.info("Shutting down TalentMatch AI Service.")


app = FastAPI(
    title="TalentMatch AI NLP & Semantic Matching Service",
    description="Internal NLP extraction, taxonomy normalization, and semantic candidate retrieval service",
    version=settings.SERVICE_VERSION,
    lifespan=lifespan,
)

# Standard CORS Middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Attach API endpoints
app.include_router(api_router)


@app.exception_handler(Exception)
async def generic_exception_handler(request: Request, exc: Exception):
    logger.error(f"Unhandled exception on {request.method} {request.url.path}: {exc}", exc_info=True)
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={"detail": "An internal error occurred in the AI processing engine."},
    )


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host=settings.HOST, port=settings.PORT, reload=False)
