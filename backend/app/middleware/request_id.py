"""Request ID and timing middleware."""

import time
import uuid
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import Response
from app.core.logging import logger, request_id_ctx


class RequestLoggingMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next) -> Response:
        req_id = request.headers.get("X-Request-ID", str(uuid.uuid4()))
        token = request_id_ctx.set(req_id)
        start_time = time.perf_counter()

        logger.info(f"Incoming request: {request.method} {request.url.path}")

        try:
            response = await call_next(request)
            process_time = (time.perf_counter() - start_time) * 1000
            response.headers["X-Request-ID"] = req_id
            response.headers["X-Process-Time-Ms"] = f"{process_time:.2f}"
            logger.info(
                f"Completed request: {request.method} {request.url.path} "
                f"status={response.status_code} in {process_time:.2f}ms"
            )
            return response
        except Exception as exc:
            process_time = (time.perf_counter() - start_time) * 1000
            logger.error(
                f"Failed request: {request.method} {request.url.path} "
                f"in {process_time:.2f}ms with error: {str(exc)}",
                exc_info=True
            )
            raise exc
        finally:
            request_id_ctx.reset(token)
