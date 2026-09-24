"""Pytest configuration and fixtures for VeriLens AI tests."""

import io
import pytest
import pytest_asyncio
from PIL import Image, ImageDraw
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from app.database.session import Base
from app.models.history import VerificationHistory

TEST_DB_URL = "sqlite+aiosqlite:///:memory:"


@pytest_asyncio.fixture
async def async_engine():
    engine = create_async_engine(TEST_DB_URL, echo=False)
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    yield engine
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)
    await engine.dispose()


@pytest_asyncio.fixture
async def db_session(async_engine):
    session_factory = async_sessionmaker(bind=async_engine, class_=AsyncSession, expire_on_commit=False)
    async with session_factory() as session:
        yield session


@pytest.fixture
def sample_test_image_bytes() -> bytes:
    """Generates a small valid PNG image containing text-like visual blocks."""
    img = Image.new("RGB", (300, 150), color=(255, 255, 255))
    draw = ImageDraw.Draw(img)
    draw.rectangle([20, 20, 280, 60], fill=(0, 51, 102))
    draw.rectangle([20, 80, 200, 100], fill=(100, 100, 100))
    buffer = io.BytesIO()
    img.save(buffer, format="PNG")
    return buffer.getvalue()
