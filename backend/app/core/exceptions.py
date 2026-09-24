"""Custom application exceptions."""

from typing import Any, Optional


class VeriLensException(Exception):
    """Base exception for all domain errors."""
    def __init__(self, message: str, status_code: int = 400, details: Optional[Any] = None):
        super().__init__(message)
        self.message = message
        self.status_code = status_code
        self.details = details


class InvalidInputException(VeriLensException):
    """Raised when user input fails validation."""
    def __init__(self, message: str, details: Optional[Any] = None):
        super().__init__(message=message, status_code=422, details=details)


class OCRException(VeriLensException):
    """Raised when OCR processing encounters a fatal error."""
    def __init__(self, message: str, details: Optional[Any] = None):
        super().__init__(message=message, status_code=422, details=details)


class ClaimExtractionException(VeriLensException):
    """Raised when a verifiable claim cannot be found."""
    def __init__(self, message: str, details: Optional[Any] = None):
        super().__init__(message=message, status_code=422, details=details)


class SearchException(VeriLensException):
    """Raised when external evidence search fails completely."""
    def __init__(self, message: str, details: Optional[Any] = None):
        super().__init__(message=message, status_code=502, details=details)


class LinkFetchException(VeriLensException):
    """Raised when user-provided link cannot be retrieved."""
    def __init__(self, message: str, details: Optional[Any] = None):
        super().__init__(message=message, status_code=400, details=details)


class ResourceNotFoundException(VeriLensException):
    """Raised when a requested resource or history item is not found."""
    def __init__(self, message: str):
        super().__init__(message=message, status_code=404)
