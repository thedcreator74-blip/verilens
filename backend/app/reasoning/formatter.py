"""Formatter and validator for AI reasoning outputs."""

import json
import re
from typing import Dict, Any, List
from app.schemas.reasoning import CredibilityReport, ReasoningSourceItem, AssessmentType
from app.core.exceptions import VeriLensException


ALLOWED_ASSESSMENTS = [
    "Likely Credible",
    "Needs Verification",
    "Potentially Misleading",
    "Insufficient Information"
]

MANDATORY_DISCLAIMER = (
    "This assessment is generated using AI based on available evidence and should not replace official verification."
)


class ReportFormatter:
    """Formats and validates raw LLM JSON outputs into strict CredibilityReport instances."""

    @classmethod
    def clean_json_text(cls, raw_text: str) -> str:
        """Strips markdown ```json ``` markers and surrounding whitespace."""
        text = raw_text.strip()
        # Strip ```json ... ``` code blocks
        text = re.sub(r"^```(?:json)?\s*", "", text, flags=re.IGNORECASE)
        text = re.sub(r"\s*```$", "", text)
        return text.strip()

    @classmethod
    def parse_and_validate(
        cls,
        raw_llm_response: str,
        fallback_claim: str,
        collected_sources: List[Dict[str, Any]]
    ) -> CredibilityReport:
        """
        Parses raw text from Gemini, enforces schema compliance,
        normalizes assessments, and sanitizes sources.
        """
        cleaned_json = cls.clean_json_text(raw_llm_response)
        
        try:
            data = json.loads(cleaned_json)
        except Exception as e:
            # Fallback parsing if LLM returned malformed JSON
            return cls._generate_structural_fallback(
                claim=fallback_claim,
                reason="The reasoning engine encountered an issue parsing the model response.",
                collected_sources=collected_sources
            )

        # 1. Normalize and enforce Assessment
        raw_assessment = str(data.get("assessment", "")).strip()
        assessment: AssessmentType = cls._normalize_assessment(raw_assessment)

        # 2. Enforce confidence bounded between 0 and 100
        raw_confidence = data.get("confidence", 60)
        try:
            confidence = int(float(raw_confidence))
            confidence = min(max(confidence, 0), 100)
        except (ValueError, TypeError):
            confidence = 60

        # 3. Ensure verified_information is substantive
        verified_info = data.get("verified_information", "").strip()
        if not verified_info:
            verified_info = (
                "Based on the available evidence, no comprehensive factual record directly corroborates or refutes this claim. "
                "Consult official public releases for primary confirmation."
            )

        # 4. Normalize reasoning list
        reasoning = data.get("reasoning", [])
        if isinstance(reasoning, str):
            reasoning = [reasoning]
        elif not isinstance(reasoning, list) or not reasoning:
            reasoning = ["Analysis conducted across retrieved evidence sources."]
        reasoning = [str(r).strip() for r in reasoning if str(r).strip()]

        # 5. Normalize recommendations
        recommendations = data.get("recommendations", [])
        if isinstance(recommendations, str):
            recommendations = [recommendations]
        elif not isinstance(recommendations, list) or not recommendations:
            recommendations = ["Check official notices before acting on or sharing this claim."]
        recommendations = [str(r).strip() for r in recommendations if str(r).strip()]

        # 6. Format sources list
        raw_sources = data.get("sources", [])
        formatted_sources: List[ReasoningSourceItem] = []
        if isinstance(raw_sources, list) and raw_sources:
            for s in raw_sources:
                if isinstance(s, dict):
                    formatted_sources.append(
                        ReasoningSourceItem(
                            title=s.get("title") or "Referenced Evidence Article",
                            publisher=s.get("publisher") or s.get("domain", "Verified Source"),
                            url=s.get("url") or "",
                            trust_level=cls._normalize_trust_level(s.get("trust_level", "High"))
                        )
                    )
        
        # If model didn't return sources, populate from collected_sources
        if not formatted_sources and collected_sources:
            for s in collected_sources[:4]:
                formatted_sources.append(
                    ReasoningSourceItem(
                        title=s.get("name", s.get("domain", "Trusted Source")),
                        publisher=s.get("name", s.get("domain", "Official Publisher")),
                        url=s.get("url", ""),
                        trust_level="High" if s.get("trust_score", 60) >= 85 else "Medium"
                    )
                )

        evidence_summary = data.get("evidence_summary", "").strip()
        if not evidence_summary:
            evidence_summary = f"Evaluated {len(formatted_sources)} source(s) against the submitted statement."

        return CredibilityReport(
            claim=data.get("claim", fallback_claim),
            verified_information=verified_info,
            assessment=assessment,
            confidence=confidence,
            reasoning=reasoning,
            evidence_summary=evidence_summary,
            recommendations=recommendations,
            sources=formatted_sources,
            disclaimer=MANDATORY_DISCLAIMER
        )

    @classmethod
    def _normalize_assessment(cls, raw: str) -> AssessmentType:
        """Enforces strictly allowed assessment values."""
        clean = raw.lower()
        if "likely credible" in clean or "credible" in clean or "verified" in clean or "true" in clean:
            return "Likely Credible"
        elif "misleading" in clean or "false" in clean or "debunked" in clean or "fake" in clean:
            return "Potentially Misleading"
        elif "insufficient" in clean or "scarce" in clean or "no evidence" in clean:
            return "Insufficient Information"
        else:
            return "Needs Verification"

    @classmethod
    def _normalize_trust_level(cls, raw: str) -> str:
        clean = str(raw).lower()
        if "high" in clean:
            return "High"
        elif "low" in clean:
            return "Low"
        return "Medium"

    @classmethod
    def _generate_structural_fallback(
        cls,
        claim: str,
        reason: str,
        collected_sources: List[Dict[str, Any]]
    ) -> CredibilityReport:
        """Constructs a deterministic safe fallback when model output is unparseable."""
        sources = [
            ReasoningSourceItem(
                title=s.get("name", s.get("domain", "Source")),
                publisher=s.get("name", "Publisher"),
                url=s.get("url", ""),
                trust_level="High" if s.get("trust_score", 60) >= 80 else "Medium"
            )
            for s in collected_sources[:3]
        ]

        return CredibilityReport(
            claim=claim,
            verified_information=(
                "The system reviewed available evidence records for this claim. "
                "Current records require manual corroboration against primary institutional portals to ensure accuracy."
            ),
            assessment="Needs Verification",
            confidence=50,
            reasoning=[
                "Evidence was collected from trusted portals but requires additional primary verification.",
                reason
            ],
            evidence_summary=f"Examined {len(sources)} initial evidence references.",
            recommendations=[
                "Verify the claim directly on official government or departmental portals.",
                "Refrain from circulating until primary confirmation is available."
            ],
            sources=sources,
            disclaimer=MANDATORY_DISCLAIMER
        )
