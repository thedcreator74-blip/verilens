"""Confidence calculation and conflict detection algorithms for AI Reasoning."""

import re
from typing import List, Dict, Any, Tuple
from app.core.logging import logger


class ConflictDetector:
    """Detects discrepancies and disagreements across collected evidence sources."""

    @staticmethod
    def detect_conflicts(
        evidence_items: List[Dict[str, Any]],
        sources: List[Dict[str, Any]]
    ) -> Tuple[bool, str, List[Dict[str, str]]]:
        """
        Analyzes evidence snippets and titles to check if trusted sources report contradictory facts.
        Returns:
            - has_conflict (bool)
            - conflict_explanation (str)
            - conflicting_positions (list of {publisher, position})
        """
        if not evidence_items or len(evidence_items) < 2:
            return False, "", []

        # Indicators of contradiction/debunking/denial
        denial_keywords = [
            "debunked", "false claim", "fake notification", "pib fact check",
            "clarifies", "denies", "did not announce", "no such scheme", "rumour",
            "rumor", "hoax", "fact check", "misleading claim", "untrue", "refutes"
        ]

        affirming_keywords = [
            "officially launched", "approved by cabinet", "announced by minister",
            "roll out", "eligibility criteria", "notification issued", "press release"
        ]

        denial_sources = []
        affirming_sources = []

        for item in evidence_items:
            combined = f"{item.get('title', '')} {item.get('snippet', '')} {item.get('main_content', '')}".lower()
            publisher = item.get("publisher") or item.get("domain", "Unknown Source")
            
            is_denial = any(k in combined for k in denial_keywords)
            is_affirming = any(k in combined for k in affirming_keywords)

            if is_denial and not is_affirming:
                denial_sources.append({
                    "publisher": publisher,
                    "position": f"Reports or confirms that the claim is unverified, false, or denied ({item.get('title', '')})"
                })
            elif is_affirming and not is_denial:
                affirming_sources.append({
                    "publisher": publisher,
                    "position": f"Reports or discusses active implementation or announcement ({item.get('title', '')})"
                })

        if denial_sources and affirming_sources:
            explanation = (
                f"Different trusted sources report conflicting information. "
                f"{len(denial_sources)} source(s) clarify or refute the claim, while "
                f"{len(affirming_sources)} source(s) discuss active coverage."
            )
            all_positions = denial_sources + affirming_sources
            return True, explanation, all_positions

        return False, "", []


class ConfidenceCalculator:
    """
    Computes a transparent empirical confidence score (0-100) based on:
    1. Number of trusted sources (weight 25%)
    2. Agreement among sources (weight 30%)
    3. Official source availability (weight 25%)
    4. Evidence freshness (weight 10%)
    5. Evidence completeness (weight 10%)
    """

    @classmethod
    def calculate(
        cls,
        evidence_items: List[Dict[str, Any]],
        sources: List[Dict[str, Any]],
        has_conflict: bool = False,
        ocr_confidence: float = None,
        assessment: str = "Needs Verification"
    ) -> Tuple[int, List[str]]:
        """
        Calculates confidence percentage and returns itemized justifications.
        """
        factors_reasoning = []

        # Edge Case: Zero evidence collected
        if not evidence_items:
            reasons = [
                "Zero authoritative evidence sources were retrieved for this claim.",
                "Confidence is set to minimal due to complete lack of verifiable data."
            ]
            return 15, reasons

        # 1. Number of Trusted Sources (Max 25 pts)
        num_sources = len(sources) if sources else len(evidence_items)
        if num_sources >= 4:
            source_pts = 25
            factors_reasoning.append(f"Strong source coverage with {num_sources} verified sources evaluated.")
        elif num_sources >= 2:
            source_pts = 18
            factors_reasoning.append(f"Moderate source coverage with {num_sources} sources.")
        elif num_sources == 1:
            source_pts = 10
            factors_reasoning.append("Limited coverage with only 1 primary source available.")
        else:
            source_pts = 5
            factors_reasoning.append("Minimal source coverage.")

        # 2. Agreement Among Sources (Max 30 pts)
        if has_conflict:
            agreement_pts = 8
            factors_reasoning.append("Significant discrepancy or conflicting viewpoints detected among sources.")
        else:
            agreement_pts = 30
            factors_reasoning.append("High consensus and consistent alignment across all reporting sources.")

        # 3. Official Source Availability (Max 25 pts)
        has_official = any(
            s.get("is_official") is True or
            s.get("trust_score", 0) >= 95.0 or
            any(s.get("domain", "").endswith(ext) for ext in (".gov.in", ".gov", ".nic.in", "who.int", "rbi.org.in"))
            for s in (sources or evidence_items)
        )
        if has_official:
            official_pts = 25
            factors_reasoning.append("Direct confirmation or coverage from official government/institutional authority present.")
        else:
            # Check highest trust score in sources
            max_trust = max([s.get("trust_score", 50.0) for s in sources], default=50.0)
            if max_trust >= 85.0:
                official_pts = 16
                factors_reasoning.append("High-credibility mainstream news sources present, though primary official gazette was not found.")
            else:
                official_pts = 8
                factors_reasoning.append("No primary official institutional portal was identified in the evidence.")

        # 4. Freshness (Max 10 pts)
        has_recent = any(
            re.search(r"\b(202[4-6])\b", str(item.get("publish_date", ""))) or
            re.search(r"\b(202[4-6])\b", str(item.get("snippet", "")))
            for item in evidence_items
        )
        if has_recent:
            freshness_pts = 10
            factors_reasoning.append("Evidence contains recent and up-to-date reporting.")
        else:
            freshness_pts = 5
            factors_reasoning.append("Evidence date is undated or references older reporting periods.")

        # 5. Completeness (Max 10 pts)
        avg_len = sum(len(item.get("snippet", "") + (item.get("main_content", "") or "")) for item in evidence_items) / max(len(evidence_items), 1)
        if avg_len > 250:
            completeness_pts = 10
            factors_reasoning.append("Rich contextual excerpts and detailed article content available for cross-examination.")
        else:
            completeness_pts = 5
            factors_reasoning.append("Brief snippet excerpts with limited full context.")

        raw_score = source_pts + agreement_pts + official_pts + freshness_pts + completeness_pts

        # Adjust score based on assessment category
        if assessment == "Insufficient Information":
            raw_score = min(raw_score, 45)
        elif assessment == "Needs Verification":
            raw_score = min(max(raw_score, 50), 75)
        elif assessment == "Potentially Misleading":
            raw_score = min(max(raw_score, 70), 92)
        elif assessment == "Likely Credible":
            raw_score = min(max(raw_score, 75), 98)

        # Apply slight penalty if OCR confidence was reported as low (< 0.6)
        if ocr_confidence is not None and 0.0 < ocr_confidence < 0.6:
            raw_score = max(int(raw_score * 0.9), 20)
            factors_reasoning.append(f"Confidence moderated slightly due to low screenshot OCR confidence ({ocr_confidence:.2f}).")

        final_score = int(min(max(raw_score, 10), 99))
        return final_score, factors_reasoning
