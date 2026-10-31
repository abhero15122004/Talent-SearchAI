"""
TalentMatch AI - AI Service Internal Security
"""

from fastapi import Security, HTTPException, status
from fastapi.security import APIKeyHeader
from app.config import settings

API_KEY_HEADER = APIKeyHeader(name="X-Internal-Service-Key", auto_error=False)


async def verify_service_key(api_key: str = Security(API_KEY_HEADER)) -> str:
    """
    Enforces shared secret authentication between Spring Boot and the Python AI service.
    Rejects requests without or with invalid X-Internal-Service-Key.
    """
    if not api_key or api_key != settings.SERVICE_KEY:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or missing X-Internal-Service-Key header",
            headers={"WWW-Authenticate": "ApiKey"},
        )
    return api_key
