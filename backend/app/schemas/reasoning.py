"""Pydantic schemas for AI Reasoning Engine analysis and credibility report."""

from typing import List, Literal, Optional
from pydantic import BaseModel, Field


AssessmentType = Literal[
    "Likely Credible",
    "Needs Verification",
    "Potentially Misleading",
    "Insufficient Information"
]

TrustLevelType = Literal["High", "Medium", "Low"]


class ReasoningSourceItem(BaseModel):
    title: str = Field(..., description="Headline or page title of the source")
    publisher: str = Field(..., description="Name of publisher or organization")
    url: str = Field(..., description="Direct URL of the verified evidence")
    trust_level: TrustLevelType = Field("High", description="Trust level: High, Medium, or Low")


class CredibilityReport(BaseModel):
    claim: str = Field(..., description="The factual claim being evaluated")
    verified_information: str = Field(
        ...,
        description="Comprehensive, neutral summary of verified factual reality based strictly on collected evidence"
    )
    assessment: AssessmentType = Field(
        ...,
        description="Strict assessment: 'Likely Credible', 'Needs Verification', 'Potentially Misleading', or 'Insufficient Information'"
    )
    confidence: int = Field(
        ...,
        ge=0,
        le=100,
        description="Calculated confidence percentage (0-100) based on source trust, agreement, and completeness"
    )
    reasoning: List[str] = Field(
        ...,
        min_length=1,
        description="Clear, bulleted points explaining why the assessment was made based strictly on evidence"
    )
    evidence_summary: str = Field(
        ...,
        description="Concise synthesis of evidence analyzed, agreement level, and source coverage"
    )
    recommendations: List[str] = Field(
        ...,
        min_length=1,
        description="Helpful, non-judgmental guidance for readers and verification next steps"
    )
    sources: List[ReasoningSourceItem] = Field(
        default_factory=list,
        description="Authoritative sources analyzed in the assessment"
    )
    disclaimer: str = Field(
        default="This assessment is generated using AI based on available evidence and should not replace official verification.",
        description="Mandatory system disclaimer"
    )


class ReasoningInputPayload(BaseModel):
    claim: str = Field(..., description="Original claim text")
    normalized_claim: str = Field(..., description="Normalized claim statement")
    category: str = Field("General", description="Topical classification")
    evidence: List[dict] = Field(default_factory=list, description="List of collected evidence items")
    sources: List[dict] = Field(default_factory=list, description="List of evaluated sources")
    ocr_confidence: Optional[float] = Field(None, description="OCR confidence if from screenshot")
