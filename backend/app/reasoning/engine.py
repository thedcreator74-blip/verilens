"""AI Reasoning Engine orchestrator for VeriLens AI.

Coordinates the Gemini LLM investigator, deterministic fallback engine,
confidence calculation, conflict detection, and strict JSON output formatting.
"""

import json
from typing import List, Dict, Any, Optional
from app.core.logging import logger
from app.schemas.response import VerificationResponse
from app.schemas.reasoning import CredibilityReport, ReasoningInputPayload
from app.reasoning.gemini_service import GeminiClient
from app.reasoning.prompts import SYSTEM_PROMPT, INVESTIGATOR_ANALYSIS_PROMPT
from app.reasoning.confidence import ConfidenceCalculator, ConflictDetector
from app.reasoning.formatter import ReportFormatter
from app.reasoning.deterministic import DeterministicInvestigator


class ReasoningEngine:
    """
    Lead Investigator Layer.
    Consumes evidence packages collected by the Verification Engine and synthesizes
    an objective, transparent credibility report.
    """

    def __init__(self, gemini_client: Optional[GeminiClient] = None):
        self.gemini_client = gemini_client or GeminiClient()

    async def analyze(
        self,
        claim: str,
        normalized_claim: str,
        category: str = "General",
        evidence: Optional[List[Dict[str, Any]]] = None,
        sources: Optional[List[Dict[str, Any]]] = None,
        ocr_confidence: Optional[float] = None
    ) -> CredibilityReport:
        """
        Main entrypoint for evidence reasoning.
        """
        evidence_list = evidence or []
        sources_list = sources or []

        logger.info(f"ReasoningEngine analyzing claim: '{claim}' with {len(evidence_list)} evidence items.")

        # Step 1: Detect conflicts upfront
        has_conflict, conflict_msg, positions = ConflictDetector.detect_conflicts(evidence_list, sources_list)
        conflict_report = conflict_msg if has_conflict else "No direct factual contradictions detected across sources."

        # Step 2: Format prompt for Gemini if client is configured
        if self.gemini_client.is_configured:
            prompt = self._build_prompt(
                claim=claim,
                normalized_claim=normalized_claim,
                category=category,
                evidence=evidence_list,
                sources=sources_list,
                conflict_report=conflict_report,
                ocr_confidence=ocr_confidence
            )

            try:
                raw_llm_output = await self.gemini_client.generate_investigation_report(
                    prompt=prompt,
                    system_instruction=SYSTEM_PROMPT,
                    temperature=0.2
                )

                if raw_llm_output and raw_llm_output.strip():
                    logger.info("Successfully received LLM reasoning response from Gemini.")
                    parsed_report = ReportFormatter.parse_and_validate(
                        raw_llm_response=raw_llm_output,
                        fallback_claim=claim,
                        collected_sources=sources_list
                    )

                    # Re-verify and enforce algorithmic confidence bounds and conflict rules
                    calc_confidence, confidence_reasons = ConfidenceCalculator.calculate(
                        evidence_items=evidence_list,
                        sources=sources_list,
                        has_conflict=has_conflict,
                        ocr_confidence=ocr_confidence,
                        assessment=parsed_report.assessment
                    )
                    
                    # Blend LLM confidence with empirical calculation
                    final_confidence = int((parsed_report.confidence * 0.4) + (calc_confidence * 0.6))
                    parsed_report.confidence = min(max(final_confidence, 10), 99)

                    # Enforce conflict rule: If conflict detected, ensure assessment reflects it
                    if has_conflict and parsed_report.assessment == "Likely Credible":
                        parsed_report.assessment = "Needs Verification"
                        parsed_report.reasoning.insert(
                            0,
                            "Different trusted sources report conflicting information on this topic."
                        )

                    return parsed_report

            except Exception as e:
                logger.error(f"Error during Gemini LLM analysis: {e}. Falling back to deterministic engine.", exc_info=True)

        # Step 3: Deterministic Rule-Based Investigation Engine (Fallback or primary when offline/no API key)
        logger.info("Executing Deterministic Investigator analysis.")
        return DeterministicInvestigator.investigate(
            claim=claim,
            normalized_claim=normalized_claim,
            category=category,
            evidence=evidence_list,
            sources=sources_list,
            ocr_confidence=ocr_confidence
        )

    async def analyze_verification_response(
        self,
        verification_response: VerificationResponse,
        ocr_confidence: Optional[float] = None
    ) -> CredibilityReport:
        """Helper to directly analyze a VerificationResponse from the verification pipeline."""
        evidence_dicts = [e.dict() if hasattr(e, "dict") else e.model_dump() for e in verification_response.evidence]
        source_dicts = [s.dict() if hasattr(s, "dict") else s.model_dump() for s in verification_response.sources]

        return await self.analyze(
            claim=verification_response.claim,
            normalized_claim=verification_response.normalized_claim,
            category=verification_response.category,
            evidence=evidence_dicts,
            sources=source_dicts,
            ocr_confidence=ocr_confidence
        )

    def _build_prompt(
        self,
        claim: str,
        normalized_claim: str,
        category: str,
        evidence: List[Dict[str, Any]],
        sources: List[Dict[str, Any]],
        conflict_report: str,
        ocr_confidence: Optional[float]
    ) -> str:
        """Constructs rich dossier text for the investigator LLM."""
        sources_text = ""
        for idx, s in enumerate(sources, 1):
            sources_text += (
                f"{idx}. Domain: {s.get('domain')} | Trust Score: {s.get('trust_score')}/100 | "
                f"Official: {s.get('is_official')} | URL: {s.get('url')}\n"
            )
        if not sources_text:
            sources_text = "No domain evaluation records available."

        evidence_text = ""
        for idx, e in enumerate(evidence, 1):
            evidence_text += (
                f"--- EVIDENCE ITEM {idx} ---\n"
                f"Title: {e.get('title')}\n"
                f"Publisher: {e.get('publisher')} ({e.get('domain')})\n"
                f"URL: {e.get('url')}\n"
                f"Official Source: {e.get('is_official')}\n"
                f"Date: {e.get('publish_date') or 'Undated'}\n"
                f"Relevance Score: {e.get('relevance_score')}/100\n"
                f"Excerpt: {e.get('snippet')}\n"
            )
            if e.get("main_content"):
                evidence_text += f"Key Context: {e.get('main_content')[:400]}...\n"
            evidence_text += "\n"
        if not evidence_text:
            evidence_text = "No evidence excerpts were retrieved from external searches."

        input_meta = f"Category: {category} | Input OCR Confidence: {f'{ocr_confidence:.2f}' if ocr_confidence else 'N/A'}"

        return INVESTIGATOR_ANALYSIS_PROMPT.format(
            claim=claim,
            normalized_claim=normalized_claim,
            category=category,
            sources_formatted=sources_text,
            evidence_formatted=evidence_text,
            conflict_report=conflict_report,
            input_metadata=input_meta
        )


# Global singleton instance
reasoning_engine = ReasoningEngine()
