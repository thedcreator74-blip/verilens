"""Gemini client service using standard Google Generative AI REST / SDK integration."""

import os
import json
import httpx
from typing import Optional, Dict, Any
from app.config.settings import settings
from app.core.logging import logger
from app.reasoning.prompts import SYSTEM_PROMPT


class GeminiClient:
    """Encapsulates secure API requests to Gemini 2.5/3.1 preview models."""

    # Default model according to the gemini-api skill rules
    DEFAULT_MODEL = "gemini-2.5-flash"
    API_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent"

    def __init__(self, api_key: Optional[str] = None, model: Optional[str] = None):
        self.api_key = api_key or os.environ.get("GEMINI_API_KEY", "") or getattr(settings, "GEMINI_API_KEY", "")
        self.model = model or getattr(settings, "GEMINI_MODEL", self.DEFAULT_MODEL)

    @property
    def is_configured(self) -> bool:
        return bool(self.api_key and self.api_key.strip())

    async def generate_investigation_report(
        self,
        prompt: str,
        system_instruction: str = SYSTEM_PROMPT,
        temperature: float = 0.2
    ) -> str:
        """
        Sends formatted evidence dossier to Gemini and enforces JSON response.
        """
        if not self.is_configured:
            logger.warning("GEMINI_API_KEY is not configured. Falling back to deterministic investigative reasoning.")
            return ""

        url = f"{self.API_URL_TEMPLATE.format(model=self.model)}?key={self.api_key}"

        payload = {
            "contents": [
                {
                    "role": "user",
                    "parts": [{"text": prompt}]
                }
            ],
            "systemInstruction": {
                "role": "system",
                "parts": [{"text": system_instruction}]
            },
            "generationConfig": {
                "temperature": temperature,
                "responseMimeType": "application/json",
                "maxOutputTokens": 2048,
            }
        }

        async with httpx.AsyncClient(timeout=15.0) as client:
            try:
                response = await client.post(
                    url,
                    json=payload,
                    headers={"Content-Type": "application/json"}
                )

                if response.status_code != 200:
                    logger.error(f"Gemini API returned status {response.status_code}: {response.text}")
                    return ""

                data = response.json()
                candidates = data.get("candidates", [])
                if candidates and "content" in candidates[0]:
                    parts = candidates[0]["content"].get("parts", [])
                    if parts and "text" in parts[0]:
                        return parts[0]["text"]

                logger.warning(f"Unexpected response structure from Gemini API: {data}")
                return ""

            except httpx.TimeoutException:
                logger.error("Gemini API request timed out after 15 seconds.")
                return ""
            except Exception as e:
                logger.error(f"Error executing Gemini API call: {e}", exc_info=True)
                return ""
