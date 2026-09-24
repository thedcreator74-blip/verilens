"""Endpoints for AI Reasoning Engine analysis and credibility report generation."""

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from app.database.session import get_db
from app.schemas.reasoning import CredibilityReport, ReasoningInputPayload
from app.schemas.response import VerificationResponse
from app.services.pipeline import VerificationPipeline
from app.schemas.request import TextAnalysisRequest, LinkAnalysisRequest
from app.reasoning.engine import reasoning_engine
from app.core.logging import logger

router = APIRouter(prefix="/reasoning", tags=["AI Reasoning Engine"])


@router.post(
    "/analyze",
    response_model=CredibilityReport,
    status_code=status.HTTP_200_OK,
    summary="Generate Credibility Report from Evidence Package"
)
async def generate_reasoning_report(
    payload: ReasoningInputPayload
):
    """
    Directly analyzes an evidence package collected by the Verification Engine.
    The AI acts as an impartial investigator:
    - Never searches the internet.
    - Evaluates only the supplied evidence items and source domain metadata.
    - Detects conflicts, calculates empirical confidence, and outputs strict JSON.
    """
    return await reasoning_engine.analyze(
        claim=payload.claim,
        normalized_claim=payload.normalized_claim,
        category=payload.category,
        evidence=payload.evidence,
        sources=payload.sources,
        ocr_confidence=payload.ocr_confidence
    )


@router.post(
    "/full-investigation/text",
    response_model=CredibilityReport,
    status_code=status.HTTP_200_OK,
    summary="End-to-End Investigation: Extract Evidence then Run AI Reasoning"
)
async def full_investigation_text(
    payload: TextAnalysisRequest,
    db: AsyncSession = Depends(get_db)
):
    """
    Executes the Verification Pipeline to extract and rank evidence from trusted sources,
    then executes the AI Reasoning Engine to generate the full Credibility Report.
    """
    verification_response = await VerificationPipeline.process_text(text=payload.text, session=db)
    return await reasoning_engine.analyze_verification_response(verification_response)


@router.post(
    "/full-investigation/link",
    response_model=CredibilityReport,
    status_code=status.HTTP_200_OK,
    summary="End-to-End Investigation for News Article Link"
)
async def full_investigation_link(
    payload: LinkAnalysisRequest,
    db: AsyncSession = Depends(get_db)
):
    """
    Executes Verification Pipeline on a news link, then passes verified evidence to the AI Reasoning Engine.
    """
    verification_response = await VerificationPipeline.process_link(url=payload.url, session=db)
    return await reasoning_engine.analyze_verification_response(verification_response)
