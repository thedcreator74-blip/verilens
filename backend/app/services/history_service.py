"""History persistence and retrieval service."""

from typing import List, Optional, Tuple
from sqlalchemy import select, delete, desc, func
from sqlalchemy.ext.asyncio import AsyncSession
from app.models.history import VerificationHistory
from app.schemas.response import HistoryItemSummary, HistoryListResponse
from app.core.exceptions import ResourceNotFoundException
from app.core.logging import logger


class HistoryService:
    @staticmethod
    async def save_verification(
        session: AsyncSession,
        input_type: str,
        raw_input: str,
        extracted_claim: str,
        normalized_claim: str,
        category: str,
        confidence: float,
        verification_strategy: str,
        sources: list,
        evidence: list,
        metrics: dict
    ) -> VerificationHistory:
        """Stores a completed verification run in SQLite."""
        history_entry = VerificationHistory(
            input_type=input_type,
            raw_input=raw_input,
            extracted_claim=extracted_claim,
            normalized_claim=normalized_claim,
            category=category,
            category_confidence=confidence,
            verification_strategy=verification_strategy,
            sources=[s.dict() if hasattr(s, "dict") else s for s in sources],
            evidence=[e.dict() if hasattr(e, "dict") else e for e in evidence],
            metrics=metrics if isinstance(metrics, dict) else metrics.dict()
        )
        session.add(history_entry)
        await session.commit()
        await session.refresh(history_entry)
        logger.info(f"Saved verification history record #{history_entry.id}")
        return history_entry

    @staticmethod
    async def get_history_list(
        session: AsyncSession,
        skip: int = 0,
        limit: int = 20,
        category: Optional[str] = None
    ) -> HistoryListResponse:
        """Retrieves paginated verification history summaries."""
        query = select(VerificationHistory)
        count_query = select(func.count(VerificationHistory.id))

        if category:
            query = query.where(VerificationHistory.category == category)
            count_query = count_query.where(VerificationHistory.category == category)

        total_res = await session.execute(count_query)
        total = total_res.scalar() or 0

        query = query.order_by(desc(VerificationHistory.created_at)).offset(skip).limit(limit)
        result = await session.execute(query)
        rows = result.scalars().all()

        items = [
            HistoryItemSummary(
                id=row.id,
                input_type=row.input_type,
                claim=row.extracted_claim,
                normalized_claim=row.normalized_claim,
                category=row.category,
                confidence=row.category_confidence,
                verification_strategy=row.verification_strategy,
                evidence_count=len(row.evidence) if isinstance(row.evidence, list) else 0,
                sources_count=len(row.sources) if isinstance(row.sources, list) else 0,
                created_at=row.created_at.isoformat() if row.created_at else ""
            )
            for row in rows
        ]

        return HistoryListResponse(total=total, items=items, skip=skip, limit=limit)

    @staticmethod
    async def get_by_id(session: AsyncSession, history_id: int) -> VerificationHistory:
        """Retrieves full verification record by ID."""
        result = await session.execute(select(VerificationHistory).where(VerificationHistory.id == history_id))
        record = result.scalar_one_or_none()
        if not record:
            raise ResourceNotFoundException(f"Verification history item #{history_id} not found.")
        return record

    @staticmethod
    async def delete_by_id(session: AsyncSession, history_id: int) -> bool:
        """Deletes a verification record by ID."""
        record = await HistoryService.get_by_id(session, history_id)
        await session.delete(record)
        await session.commit()
        logger.info(f"Deleted verification record #{history_id}")
        return True
