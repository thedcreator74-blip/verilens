"""API response models."""

from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from app.schemas.evidence import SourceItem, EvidenceItem, PipelineMetrics


class VerificationResponse(BaseModel):
    id: Optional[int] = Field(None, description="Unique verification history record ID")
    input_type: str = Field(..., description="IMAGE, TEXT, or LINK")
    claim: str = Field(..., description="Extracted primary declarative factual claim")
    normalized_claim: str = Field(..., description="Normalized, noise-free claim statement")
    category: str = Field(..., description="Detected topical category")
    confidence: float = Field(..., description="Category classification confidence (0-100)")
    verification_strategy: str = Field(..., description="Search strategy applied")
    sources: List[SourceItem] = Field(default_factory=list, description="List of evaluated source domains")
    evidence: List[EvidenceItem] = Field(default_factory=list, description="Ranked top verifiable evidence items")
    metrics: PipelineMetrics = Field(default_factory=PipelineMetrics, description="Execution timings across pipeline steps")


class HistoryItemSummary(BaseModel):
    id: int
    input_type: str
    claim: str
    normalized_claim: str
    category: str
    confidence: float
    verification_strategy: str
    evidence_count: int
    sources_count: int
    created_at: str


class HistoryListResponse(BaseModel):
    total: int
    items: List[HistoryItemSummary]
    skip: int
    limit: int


class HealthResponse(BaseModel):
    status: str = "healthy"
    app: str
    version: str
    timestamp: str
    services: Dict[str, Any]
