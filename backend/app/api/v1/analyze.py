"""Analyze endpoints for image, text, and news links."""

from fastapi import APIRouter, Depends, File, UploadFile, status
from sqlalchemy.ext.asyncio import AsyncSession
from app.database.session import get_db
from app.schemas.request import TextAnalysisRequest, LinkAnalysisRequest
from app.schemas.response import VerificationResponse
from app.services.pipeline import VerificationPipeline
from app.core.exceptions import InvalidInputException
from app.config.settings import settings

router = APIRouter(prefix="/analyze", tags=["Evidence Preparation"])


@router.post(
    "/image",
    response_model=VerificationResponse,
    status_code=status.HTTP_200_OK,
    summary="Analyze Screenshot and Extract Verified Evidence"
)
async def analyze_image(
    file: UploadFile = File(..., description="Screenshot or image containing claim to verify"),
    db: AsyncSession = Depends(get_db)
):
    """
    Submits a screenshot for OCR processing, factual claim extraction,
    category classification, and multi-source evidence preparation.
    """
    if not file.content_type or file.content_type not in settings.ALLOWED_IMAGE_TYPES:
        raise InvalidInputException(
            f"Unsupported file type '{file.content_type}'. Allowed types: {', '.join(settings.ALLOWED_IMAGE_TYPES)}"
        )

    image_bytes = await file.read()
    max_bytes = settings.MAX_IMAGE_SIZE_MB * 1024 * 1024
    if len(image_bytes) > max_bytes:
        raise InvalidInputException(
            f"Uploaded file exceeds size limit of {settings.MAX_IMAGE_SIZE_MB}MB."
        )

    return await VerificationPipeline.process_image(image_bytes=image_bytes, session=db)


@router.post(
    "/text",
    response_model=VerificationResponse,
    status_code=status.HTTP_200_OK,
    summary="Analyze Text Statement and Extract Verified Evidence"
)
async def analyze_text(
    payload: TextAnalysisRequest,
    db: AsyncSession = Depends(get_db)
):
    """
    Submits raw text or viral message for claim normalization,
    category classification, and authoritative evidence retrieval.
    """
    return await VerificationPipeline.process_text(text=payload.text, session=db)


@router.post(
    "/link",
    response_model=VerificationResponse,
    status_code=status.HTTP_200_OK,
    summary="Analyze News Link and Extract Verified Evidence"
)
async def analyze_link(
    payload: LinkAnalysisRequest,
    db: AsyncSession = Depends(get_db)
):
    """
    Submits an article URL to parse headline, extract primary proposition,
    and cross-reference against trusted domain sources.
    """
    return await VerificationPipeline.process_link(url=payload.url, session=db)
