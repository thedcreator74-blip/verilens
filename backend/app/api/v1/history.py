"""History query and management endpoints."""

from fastapi import APIRouter, Depends, Query, status
from sqlalchemy.ext.asyncio import AsyncSession
from typing import Optional
from app.database.session import get_db
from app.schemas.response import HistoryListResponse, VerificationResponse
from app.services.history_service import HistoryService

router = APIRouter(prefix="/history", tags=["History & Audit"])


@router.get(
    "",
    response_model=HistoryListResponse,
    status_code=status.HTTP_200_OK,
    summary="List Verification History"
)
async def list_history(
    skip: int = Query(0, ge=0, description="Offset index"),
    limit: int = Query(20, ge=1, le=100, description="Number of items to retrieve"),
    category: Optional[str] = Query(None, description="Optional category filter"),
    db: AsyncSession = Depends(get_db)
):
    """Retrieves paginated verification history."""
    return await HistoryService.get_history_list(
        session=db,
        skip=skip,
        limit=limit,
        category=category
    )


@router.get(
    "/{id}",
    response_model=VerificationResponse,
    status_code=status.HTTP_200_OK,
    summary="Get Verification Evidence Details by ID"
)
async def get_history_item(
    id: int,
    db: AsyncSession = Depends(get_db)
):
    """Retrieves the full evidence and sources package for a historical verification."""
    record = await HistoryService.get_by_id(session=db, history_id=id)
    return VerificationResponse(
        id=record.id,
        input_type=record.input_type,
        claim=record.extracted_claim,
        normalized_claim=record.normalized_claim,
        category=record.category,
        confidence=record.category_confidence,
        verification_strategy=record.verification_strategy,
        sources=record.sources or [],
        evidence=record.evidence or [],
        metrics=record.metrics or {}
    )


@router.delete(
    "/{id}",
    status_code=status.HTTP_200_OK,
    summary="Delete Verification Record"
)
async def delete_history_item(
    id: int,
    db: AsyncSession = Depends(get_db)
):
    """Permanently deletes a verification history entry."""
    await HistoryService.delete_by_id(session=db, history_id=id)
    return {"status": "success", "message": f"Verification entry #{id} deleted successfully."}
