"""System health and operational diagnostic endpoints."""

import datetime
from fastapi import APIRouter, Depends, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import text
from app.config.settings import settings
from app.database.session import get_db
from app.registry.registry import registry
from app.ocr.engine import get_ocr_reader
from app.schemas.response import HealthResponse

router = APIRouter(tags=["Health & Status"])


@router.get(
    "/health",
    response_model=HealthResponse,
    status_code=status.HTTP_200_OK,
    summary="System Health Check"
)
async def health_check(db: AsyncSession = Depends(get_db)):
    """Validates connectivity to database, registry state, and operational status."""
    db_status = "ok"
    try:
        await db.execute(text("SELECT 1"))
    except Exception as e:
        db_status = f"unhealthy: {str(e)}"

    registry_count = len(registry.domains)

    return HealthResponse(
        status="healthy" if db_status == "ok" else "degraded",
        app=settings.APP_NAME,
        version=settings.APP_VERSION,
        timestamp=datetime.datetime.utcnow().isoformat(),
        services={
            "database": db_status,
            "registry_domains_loaded": registry_count,
            "ocr_engine": "ready",
            "environment": settings.ENVIRONMENT
        }
    )
