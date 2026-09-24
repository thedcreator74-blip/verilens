"""Application configuration using Pydantic Settings."""

from pydantic_settings import BaseSettings, SettingsConfigDict
from typing import List
import os


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore"
    )

    APP_NAME: str = "VeriLens AI Backend"
    APP_VERSION: str = "1.0.0"
    ENVIRONMENT: str = "development"
    DEBUG: bool = True
    PORT: int = 8000
    HOST: str = "0.0.0.0"

    # Database
    DATABASE_URL: str = "sqlite+aiosqlite:///./verilens.db"
    ECHO_SQL: bool = False

    # Input Limits
    MAX_IMAGE_SIZE_MB: int = 10
    MAX_TEXT_LENGTH: int = 10000
    ALLOWED_IMAGE_TYPES: List[str] = [
        "image/jpeg",
        "image/png",
        "image/webp",
        "image/bmp"
    ]

    # Verification & Search Settings
    SEARCH_MAX_RESULTS: int = 8
    SEARCH_TIMEOUT_SECONDS: float = 5.0
    EVIDENCE_SCRAPE_TIMEOUT_SECONDS: float = 4.0
    MAX_CONCURRENT_SCRAPES: int = 5
    ENABLE_TAMIL_OCR: bool = False

    # AI Reasoning & Gemini API
    GEMINI_API_KEY: str = ""
    GEMINI_MODEL: str = "gemini-2.5-flash"

    # Optional External Search API Keys (Falls back to direct DuckDuckGo HTML/Instant search)
    GOOGLE_CUSTOM_SEARCH_API_KEY: str = ""
    GOOGLE_CSE_ID: str = ""
    BING_SEARCH_API_KEY: str = ""


settings = Settings()
