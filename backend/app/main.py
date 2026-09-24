"""Main FastAPI Application Entrypoint."""

from contextlib import asynccontextmanager
from fastapi import FastAPI, Request, status
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from fastapi.exceptions import RequestValidationError
from app.config.settings import settings
from app.core.logging import setup_logging, logger
from app.core.exceptions import VeriLensException
from app.middleware.request_id import RequestLoggingMiddleware
from app.database.session import init_db
from app.api.v1.router import v1_router


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Lifecycle manager for startup and shutdown routines."""
    setup_logging(debug=settings.DEBUG)
    logger.info(f"Starting {settings.APP_NAME} v{settings.APP_VERSION} ({settings.ENVIRONMENT})...")
    await init_db()
    yield
    logger.info("Shutting down VeriLens AI verification engine...")


app = FastAPI(
    title=settings.APP_NAME,
    version=settings.APP_VERSION,
    description="Verification Engine Backend for VeriLens AI. Prepares verified evidence packages from screenshots, text, and news links.",
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc"
)

# CORS Middleware for Android Client & Web UI
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Request ID and latency tracking middleware
app.add_middleware(RequestLoggingMiddleware)


# Exception Handlers
@app.exception_handler(VeriLensException)
async def handle_verilens_exception(request: Request, exc: VeriLensException):
    return JSONResponse(
        status_code=exc.status_code,
        content={
            "error": exc.__class__.__name__,
            "message": exc.message,
            "details": exc.details
        }
    )


@app.exception_handler(RequestValidationError)
async def handle_validation_error(request: Request, exc: RequestValidationError):
    return JSONResponse(
        status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
        content={
            "error": "ValidationError",
            "message": "The request payload failed structural validation.",
            "details": exc.errors()
        }
    )


@app.exception_handler(Exception)
async def handle_generic_exception(request: Request, exc: Exception):
    logger.error(f"Unhandled server error: {exc}", exc_info=True)
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={
            "error": "InternalServerError",
            "message": "An unexpected error occurred while preparing evidence. Please check server logs."
        }
    )


# Mount API Routes
app.include_router(v1_router)


@app.get("/", tags=["Root"])
async def root():
    return {
        "app": settings.APP_NAME,
        "version": settings.APP_VERSION,
        "status": "online",
        "docs": "/docs",
        "endpoints": {
            "analyze_image": "/api/v1/analyze/image",
            "analyze_text": "/api/v1/analyze/text",
            "analyze_link": "/api/v1/analyze/link",
            "history": "/api/v1/history",
            "health": "/api/v1/health"
        }
    }
