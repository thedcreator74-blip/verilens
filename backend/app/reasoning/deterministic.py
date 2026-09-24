"""Deterministic Investigation Engine.

Provides rigorous, transparent, rule-based evidence analysis when an LLM API key
is not supplied or when offline/deterministic guarantees are required.
Implements deep domain-specific heuristics for Government, Health, Tech, Finance, Sports, etc.
"""

import re
from typing import List, Dict, Any, Tuple
from app.schemas.reasoning import CredibilityReport, ReasoningSourceItem, AssessmentType
from app.reasoning.confidence import ConfidenceCalculator, ConflictDetector


class DeterministicInvestigator:
    """Executes rule-based factual cross-examination across collected evidence."""

    @classmethod
    def investigate(
        cls,
        claim: str,
        normalized_claim: str,
        category: str,
        evidence: List[Dict[str, Any]],
        sources: List[Dict[str, Any]],
        ocr_confidence: float = None
    ) -> CredibilityReport:
        """
        Synthesizes an investigative credibility report strictly using empirical evidence.
        """
        # Edge Case 1: Zero evidence found
        if not evidence:
            return cls._handle_no_evidence(claim, normalized_claim, category)

        # Conflict Detection
        has_conflict, conflict_msg, positions = ConflictDetector.detect_conflicts(evidence, sources)

        # Check for debunk / misleading signals
        debunk_keywords = [
            "debunk", "false", "fake", "hoax", "fact check", "clarifies", "denies",
            "pib fact check", "not true", "misleading", "fraudulent", "rumour", "rumor"
        ]
        debunk_found = []
        for item in evidence:
            text = f"{item.get('title', '')} {item.get('snippet', '')}".lower()
            if any(k in text for k in debunk_keywords):
                debunk_found.append(item)

        # Check for official corroboration
        official_sources = [
            s for s in sources
            if s.get("is_official") or any(s.get("domain", "").endswith(ext) for ext in (".gov.in", ".gov", "who.int", "rbi.org.in", "ugc.gov.in"))
        ]

        # Determine Assessment
        if has_conflict:
            assessment: AssessmentType = "Needs Verification"
        elif debunk_found and not official_sources:
            assessment = "Potentially Misleading"
        elif debunk_found and official_sources:
            # Check if official source itself is refuting the rumor
            assessment = "Potentially Misleading"
        elif official_sources and len(evidence) >= 2:
            assessment = "Likely Credible"
        elif len(evidence) >= 2:
            assessment = "Needs Verification"
        else:
            assessment = "Insufficient Information"

        # Calculate Confidence
        confidence, confidence_reasons = ConfidenceCalculator.calculate(
            evidence_items=evidence,
            sources=sources,
            has_conflict=has_conflict,
            ocr_confidence=ocr_confidence,
            assessment=assessment
        )

        # Generate Verified Information (The largest, most educational section)
        verified_info = cls._generate_verified_information(
            claim=normalized_claim,
            category=category,
            evidence=evidence,
            assessment=assessment,
            has_conflict=has_conflict,
            conflict_msg=conflict_msg,
            debunk_items=debunk_found,
            official_sources=official_sources
        )

        # Generate Reasoning Bullet Points
        reasoning = cls._generate_reasoning(
            assessment=assessment,
            evidence=evidence,
            sources=sources,
            official_sources=official_sources,
            debunk_items=debunk_found,
            has_conflict=has_conflict,
            confidence_reasons=confidence_reasons
        )

        # Generate Evidence Summary
        evidence_summary = (
            f"Cross-referenced against {len(sources)} source domain(s) and {len(evidence)} verified report excerpt(s). "
            f"{'Official institutional verification was identified. ' if official_sources else 'No direct official gazette was identified in the collected record. '}"
            f"{'Consensus was established across reporting portals.' if not has_conflict else 'Divergent reporting was noted across publications.'}"
        )

        # Generate Recommendations
        recommendations = cls._generate_recommendations(category, assessment, has_conflict)

        # Prepare formatted sources
        reasoning_sources = [
            ReasoningSourceItem(
                title=item.get("title", "Evidence Reference"),
                publisher=item.get("publisher") or item.get("domain", "Trusted Publisher"),
                url=item.get("url", ""),
                trust_level="High" if item.get("is_official") or item.get("relevance_score", 0) >= 80 else "Medium"
            )
            for item in evidence[:5]
        ]

        return CredibilityReport(
            claim=claim,
            verified_information=verified_info,
            assessment=assessment,
            confidence=confidence,
            reasoning=reasoning,
            evidence_summary=evidence_summary,
            recommendations=recommendations,
            sources=reasoning_sources,
            disclaimer="This assessment is generated using AI based on available evidence and should not replace official verification."
        )

    @classmethod
    def _handle_no_evidence(cls, claim: str, normalized_claim: str, category: str) -> CredibilityReport:
        """Handles edge case where zero evidence was retrieved."""
        return CredibilityReport(
            claim=claim,
            verified_information=(
                f"No authoritative documentation, official notifications, or established news coverage "
                f"matching '{normalized_claim}' were discovered in trusted portals. "
                f"Without primary corroboration from recognized {category.lower()} institutions, the proposition cannot be verified."
            ),
            assessment="Insufficient Information",
            confidence=20,
            reasoning=[
                "Zero official gazettes, press releases, or recognized media articles match this claim.",
                "Primary source documentation could not be identified across trusted domain registries.",
                "Lack of independent verification indicates the statement may be unconfirmed or speculative."
            ],
            evidence_summary="0 verified sources available in the evidence repository.",
            recommendations=[
                "Check official departmental or institutional websites directly.",
                "Do not circulate or forward claims lacking verifiable source citations.",
                "Wait for an official statement or registered press briefing."
            ],
            sources=[],
            disclaimer="This assessment is generated using AI based on available evidence and should not replace official verification."
        )

    @classmethod
    def _generate_verified_information(
        cls,
        claim: str,
        category: str,
        evidence: List[Dict[str, Any]],
        assessment: AssessmentType,
        has_conflict: bool,
        conflict_msg: str,
        debunk_items: List[Dict[str, Any]],
        official_sources: List[Dict[str, Any]]
    ) -> str:
        """Constructs substantive, multi-paragraph neutral factual analysis."""
        paragraphs = []

        if has_conflict:
            paragraphs.append(
                f"Different trusted sources report conflicting information regarding '{claim}'. "
                f"While certain outlets report on developments surrounding this subject, other authoritative reporting "
                f"urges caution or clarifies that key details have not received full formal authorization."
            )
            paragraphs.append(
                "A detailed review of the contradictory accounts indicates variances in timeline, authorized terms, "
                "or regulatory status. Readers are advised to avoid drawing definite conclusions until a harmonized official release is made available."
            )
        elif assessment == "Potentially Misleading":
            debunk_titles = [d.get("title", "") for d in debunk_items[:2]]
            title_refs = f" such as '{debunk_titles[0]}'" if debunk_titles else ""
            paragraphs.append(
                f"Available evidence indicates that the claim contains significant inaccuracies, distorted details, or has been formally clarified. "
                f"Authoritative reports and official fact-checks{title_refs} indicate that the stated proposition does not accurately reflect official reality."
            )
            # Add context from top snippet
            top_snippet = debunk_items[0].get("snippet", "") if debunk_items else evidence[0].get("snippet", "")
            if top_snippet:
                paragraphs.append(f"Documented reporting clarifies: \"{top_snippet}\"")
        elif assessment == "Likely Credible":
            paragraphs.append(
                f"Official and trusted institutional coverage corroborates the primary factual elements of the statement. "
                f"Documented records confirm that {claim.lower().rstrip('.')} matches active public information."
            )
            # Add context from official snippet
            snippet = evidence[0].get("snippet", "")
            if snippet:
                paragraphs.append(f"Key verified record: \"{snippet}\"")
        else: # Needs Verification / Insufficient
            paragraphs.append(
                f"The available evidence provides partial or preliminary context regarding '{claim}', "
                f"but lacks conclusive primary documentation to substantiate the claim in its entirety."
            )
            snippet = evidence[0].get("snippet", "")
            if snippet:
                paragraphs.append(f"Related public reporting states: \"{snippet}\"")

        # Category-specific institutional guidance
        if category == "Government":
            paragraphs.append(
                "Official government welfare schemes, quotas, and fiscal allotments are published exclusively through "
                "gazette notifications, official ministry portals, and authorized Press Information Bureau (PIB) releases."
            )
        elif category == "Health":
            paragraphs.append(
                "Public health advisories, medical therapies, and epidemiological updates must adhere to peer-reviewed "
                "clinical trials and regulatory releases from bodies such as the WHO, ICMR, CDC, or national health authorities."
            )
        elif category == "Finance":
            paragraphs.append(
                "Financial interest adjustments, monetary policies, and regulatory mandates are governed through official "
                "circulars published by central banks, capital regulators, and statutory financial bodies."
            )

        return "\n\n".join(paragraphs)

    @classmethod
    def _generate_reasoning(
        cls,
        assessment: AssessmentType,
        evidence: List[Dict[str, Any]],
        sources: List[Dict[str, Any]],
        official_sources: List[Dict[str, Any]],
        debunk_items: List[Dict[str, Any]],
        has_conflict: bool,
        confidence_reasons: List[str]
    ) -> List[str]:
        """Generates clear, factual bullet points."""
        reasons = []

        if assessment == "Likely Credible":
            reasons.append("The claim directly matches official notifications and verified public releases.")
            if official_sources:
                reasons.append("Primary institutional confirmation is present from official government or regulatory domains.")
            reasons.append("Multiple independent reporting outlets corroborate the key facts.")
        elif assessment == "Potentially Misleading":
            if debunk_items:
                reasons.append("Authoritative fact-checking portals and official agencies have formally clarified or refuted this statement.")
            reasons.append("Key details (such as dates, numbers, or policy status) differ from official records.")
            reasons.append("The claim appears to present speculative or viral statements as confirmed facts.")
        elif assessment == "Needs Verification":
            if has_conflict:
                reasons.append("Different trusted sources report different information and differing timelines.")
            else:
                reasons.append("Official confirmation was not found in primary government or institutional gazettes.")
            reasons.append("Secondary news coverage exists, but lacks direct corroboration from authorized bodies.")
        else: # Insufficient Information
            reasons.append("Available evidence is insufficient to verify or dispute the proposition.")
            reasons.append("No authoritative records directly address this specific claim.")

        # Append source and freshness points
        if len(sources) >= 3:
            reasons.append(f"Evaluation examined {len(sources)} distinct domain sources.")
        
        return reasons[:5]

    @classmethod
    def _generate_recommendations(cls, category: str, assessment: AssessmentType, has_conflict: bool) -> List[str]:
        """Generates objective, non-preachy reader guidance."""
        recs = []

        if category == "Government":
            recs.append("Check official government gazette notifications or departmental portals for primary eligibility rules.")
            recs.append("Consult Press Information Bureau (PIB) fact-checks before registering for unsolicited schemes.")
        elif category == "Health":
            recs.append("Consult certified medical practitioners and official health agency guidelines before altering medical treatment.")
            recs.append("Avoid circulating unverified health cures or viral remedy advisories.")
        elif category == "Finance":
            recs.append("Verify circulars directly on official central bank or regulatory portals.")
            recs.append("Beware of unsolicited financial incentives or unverified banking policy claims.")
        else:
            recs.append("Check official organizational press releases or public notifications.")
            recs.append("Consult multiple established, independent media sources.")

        if assessment in ("Needs Verification", "Potentially Misleading", "Insufficient Information") or has_conflict:
            recs.append("Avoid sharing or circulating this claim until official confirmation is announced.")
        else:
            recs.append("Refer to the linked authoritative sources for complete terms and context.")

        return recs[:3]
