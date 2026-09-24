"""Unit and integration tests for FastAPI REST Endpoints."""

import pytest
import pytest_asyncio
from httpx import AsyncClient, ASGITransport
from app.main import app
from app.database.session import get_db, Base
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession


TEST_DATABASE_URL = "sqlite+aiosqlite:///:memory:"


@pytest_asyncio.fixture
async def test_app_client():
    engine = create_async_engine(TEST_DATABASE_URL, echo=False)
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

    session_factory = async_sessionmaker(bind=engine, class_=AsyncSession, expire_on_commit=False)

    async def override_get_db():
        async with session_factory() as session:
            yield session

    app.dependency_overrides[get_db] = override_get_db

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        yield client

    app.dependency_overrides.clear()
    await engine.dispose()


@pytest.mark.asyncio
async def test_health_endpoint(test_app_client):
    response = await test_app_client.get("/api/v1/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] in ("healthy", "degraded")
    assert "services" in data


@pytest.mark.asyncio
async def test_analyze_text_invalid_input(test_app_client):
    response = await test_app_client.post("/api/v1/analyze/text", json={"text": "hi"})
    assert response.status_code == 422


@pytest.mark.asyncio
async def test_analyze_link_ssrf_blocked(test_app_client):
    response = await test_app_client.post(
        "/api/v1/analyze/link",
        json={"url": "http://127.0.0.1:8080/admin"}
    )
    assert response.status_code == 422


@pytest.mark.asyncio
async def test_history_crud(test_app_client):
    # Fetch empty history
    response = await test_app_client.get("/api/v1/history")
    assert response.status_code == 200
    data = response.json()
    assert data["total"] == 0
    assert data["items"] == []
