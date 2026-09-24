"""Evidence, source, and metric schemas."""

from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any


class SourceItem(BaseModel):
    domain: str = Field(..., description="Root domain of the source (e.g. pib.gov.in)")
    name: str = Field(..., description="Human-readable publisher or organization name")
    url: str = Field(..., description="Full URL of the source page")
    trust_score: float = Field(..., description="Calculated reputation score from 0.0 to 100.0")
    is_official: bool = Field(False, description="True if domain is an official government/institutional entity")
    category: Optional[str] = Field(None, description="Primary domain topic category")


class EvidenceItem(BaseModel):
    title: str = Field(..., description="Document or page title")
    url: str = Field(..., description="Source web URL")
    publisher: str = Field(..., description="Publisher name or domain")
    publish_date: Optional[str] = Field(None, description="Extracted ISO publication date if detected")
    snippet: str = Field(..., description="Relevant contextual excerpt surrounding claim keywords")
    main_content: Optional[str] = Field(None, description="Cleaned core text of the article")
    language: str = Field("en", description="Detected language code")
    domain: str = Field(..., description="Host domain name")
    relevance_score: float = Field(..., description="Multi-criteria ranking score from 0.0 to 100.0")
    is_official: bool = Field(False, description="Whether this is an authoritative source")


class PipelineMetrics(BaseModel):
    ocr_time_ms: float = 0.0
    claim_extraction_time_ms: float = 0.0
    search_time_ms: float = 0.0
    collector_time_ms: float = 0.0
    ranking_time_ms: float = 0.0
    total_time_ms: float = 0.0
