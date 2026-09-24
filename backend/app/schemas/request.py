"""Request validation schemas for VeriLens AI endpoints."""

from pydantic import BaseModel, Field, HttpUrl, field_validator
import re


class TextAnalysisRequest(BaseModel):
    text: str = Field(
        ...,
        min_length=3,
        max_length=10000,
        description="The raw claim text or news statement to verify."
    )

    @field_validator("text")
    def validate_non_empty_text(cls, v: str) -> str:
        cleaned = v.strip()
        if len(cleaned) < 3:
            raise ValueError("Input text must contain at least 3 non-whitespace characters.")
        return cleaned


class LinkAnalysisRequest(BaseModel):
    url: str = Field(
        ...,
        description="A publicly accessible HTTP/HTTPS URL of a news article or announcement."
    )

    @field_validator("url")
    def validate_url_scheme_and_domain(cls, v: str) -> str:
        v = v.strip()
        if not re.match(r"^https?://[a-zA-Z0-9\-._~:/?#\[\]@!$&'()*+,;=%]+$", v):
            raise ValueError("Invalid URL format. Must start with http:// or https://")
        
        # SSRF Prevention: reject localhost and link-local addresses
        lower_v = v.lower()
        blocked_hosts = [
            "localhost", "127.0.0.1", "0.0.0.0", "169.254.169.254", 
            "[::1]", "metadata.google.internal"
        ]
        for host in blocked_hosts:
            if f"://{host}" in lower_v or f"@{host}" in lower_v:
                raise ValueError(f"Access to host '{host}' is strictly prohibited.")
        return v
