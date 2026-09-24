"""API v1 Router Aggregator."""

from fastapi import APIRouter
from app.api.v1.analyze import router as analyze_router
from app.api.v1.history import router as history_router
from app.api.v1.health import router as health_router
from app.api.v1.reasoning import router as reasoning_router

v1_router = APIRouter(prefix="/api/v1")

v1_router.include_router(analyze_router)
v1_router.include_router(history_router)
v1_router.include_router(health_router)
v1_router.include_router(reasoning_router)
