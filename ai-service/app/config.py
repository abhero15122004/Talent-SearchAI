"""
TalentMatch AI - AI Service Configuration
"""

from pydantic_settings import BaseSettings
from pydantic import Field


class Settings(BaseSettings):
    SERVICE_KEY: str = Field(default="tm-internal-secret-key-prod-2026", env="AI_SERVICE_KEY")
    HOST: str = Field(default="0.0.0.0", env="AI_SERVICE_HOST")
    PORT: int = Field(default=8000, env="AI_SERVICE_PORT")
    MODEL_NAME: str = Field(default="all-MiniLM-L6-v2", env="AI_EMBEDDING_MODEL")
    SPACY_MODEL: str = Field(default="en_core_web_sm", env="AI_SPACY_MODEL")
    MAX_INPUT_LENGTH: int = Field(default=100_000, env="AI_MAX_INPUT_LENGTH")
    TAXONOMY_VERSION: str = "1.0.0"
    SERVICE_VERSION: str = "1.0.0"

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


settings = Settings()
