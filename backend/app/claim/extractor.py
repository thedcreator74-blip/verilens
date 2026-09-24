"""Claim Extraction Engine.

Filters out UI noise, buttons, advertisements, hashtags, social metadata,
and identifies the primary factual declarative claim.
"""

import re
from typing import List, Tuple, Dict, Any
from app.utils.text_cleaner import (
    normalize_whitespace,
    remove_emojis,
    extract_sentences
)

# Common UI Buttons & Call-to-Actions to discard
BUTTON_PATTERNS = [
    r"\bclick\s+(?:here|now|link|below)\b",
    r"\bread\s+(?:more|full\s+article|here)\b",
    r"\bsee\s+(?:more|details|attached)\b",
    r"\b(?:subscribe|follow|share|like|comment|retweet|repost)\b",
    r"\b(?:sign\s+in|sign\s+up|log\s+in|register|submit)\b",
    r"\b(?:download|install|open\s+app|swipe\s+up|tap\s+here)\b",
    r"\b(?:apply\s+now|claim\s+now|book\s+now|shop\s+now|buy\s+now)\b",
    r"\b(?:learn\s+more|check\s+eligibility|get\s+started)\b",
]

# Common Ad & Promotional Slogans
AD_PATTERNS = [
    r"\b(?:sponsored|advertisement|promoted|ad\s+content)\b",
    r"\b(?:limited\s+(?:time\s+)?offer|hurry\s+up|mega\s+sale)\b",
    r"\b(?:flat\s+\d+%\s+off|discount|cashback|win\s+prizes?)\b",
    r"\bexclusive\s+deal\b",
]

# Mobile Screenshot Status Bar & Navigation Junk
UI_CHROME_PATTERNS = [
    r"\b(?:lte|4g|5g|volte|wifi|bluetooth|am|pm)\b",
    r"\b\d{1,2}:\d{2}\s*(?:am|pm)?\b",
    r"\bbattery\s*(?:\d{1,3}%?)\b",
    r"\b(?:search|menu|home|back|settings|profile|notifications?)\b",
]

# Sensationalist Lead-in Prefixes to strip or extract from
LEAD_IN_PREFIXES = [
    r"^(?:breaking\s+news(?:\s*:)?\s*)",
    r"^(?:urgent(?:\s*:)?\s*)",
    r"^(?:viral(?:\s*:)?\s*)",
    r"^(?:shocking(?:\s*:)?\s*)",
    r"^(?:just\s+in(?:\s*:)?\s*)",
    r"^(?:must\s+watch(?:\s*:)?\s*)",
    r"^(?:important\s+alert(?:\s*:)?\s*)",
    r"^(?:fact\s+check(?:\s*:)?\s*)",
]

# Indicative claim action verbs & entity signals that indicate a factual proposition
FACTUAL_VERB_SIGNALS = [
    "announced", "approves", "approved", "launched", "launches", "passes", "passed",
    "bans", "banned", "decides", "decided", "releases", "released", "offers", "gives",
    "introduced", "introduces", "declares", "declared", "orders", "ordered", "mandates",
    "confirms", "confirmed", "denies", "denied", "arrests", "arrested", "increases",
    "decreases", "allocated", "disburses", "signs", "signed", "warns", "warned"
]


def is_noise_or_button(text: str) -> bool:
    """Checks if a short text line is a button, ad, or UI chrome."""
    lowered = text.lower().strip()
    if len(lowered) < 3:
        return True

    # Check button patterns
    for pat in BUTTON_PATTERNS:
        if re.search(pat, lowered, re.IGNORECASE):
            return True

    # Check ad patterns
    for pat in AD_PATTERNS:
        if re.search(pat, lowered, re.IGNORECASE):
            return True

    # Check UI chrome
    for pat in UI_CHROME_PATTERNS:
        if re.fullmatch(pat, lowered, re.IGNORECASE):
            return True

    # Check single word navigations
    if lowered in ["home", "menu", "next", "previous", "close", "ok", "cancel", "done", "skip"]:
        return True

    return False


def clean_line_noise(text: str) -> str:
    """Removes hashtags, @mentions, URLs, and extraneous social metadata."""
    # Remove URLs
    text = re.sub(r"https?://\S+|www\.\S+", "", text)
    # Remove @mentions
    text = re.sub(r"@\w+", "", text)
    # Remove #hashtags but keep words if it's CamelCase
    text = re.sub(r"#\w+", "", text)
    # Remove emojis
    text = remove_emojis(text)
    return normalize_whitespace(text)


class ClaimExtractor:
    """Extracts the primary verifiable factual claim and supporting context."""

    @classmethod
    def extract_claim(cls, raw_text: str) -> Tuple[str, List[str], float]:
        """
        Parses raw text (from OCR, user text, or scraped article) and extracts:
        Returns: (main_claim, supporting_sentences, confidence)
        """
        if not raw_text or not raw_text.strip():
            return "", [], 0.0

        # Step 1: Split into candidate sentences/lines
        lines = [clean_line_noise(line) for line in raw_text.split("\n")]
        candidates = []
        for line in lines:
            if line and not is_noise_or_button(line):
                # Split compound sentences if needed
                for s in extract_sentences(line):
                    if not is_noise_or_button(s) and len(s.split()) >= 3:
                        candidates.append(s)

        if not candidates:
            # Fallback to cleaned raw text if splitting was overly aggressive
            fallback = clean_line_noise(raw_text)
            if fallback and len(fallback.split()) >= 3:
                candidates = [fallback]
            else:
                return "", [], 0.0

        # Step 2: Score candidates to find the primary declarative claim
        scored_candidates: List[Tuple[str, float]] = []

        for candidate in candidates:
            score = 50.0  # Base confidence

            cleaned_candidate = candidate
            # Check for leading "BREAKING NEWS:" or similar prefixes
            has_lead_in = False
            for lead in LEAD_IN_PREFIXES:
                match = re.match(lead, cleaned_candidate, re.IGNORECASE)
                if match:
                    cleaned_candidate = cleaned_candidate[match.end():].strip()
                    score += 25.0
                    has_lead_in = True
                    break

            word_count = len(cleaned_candidate.split())

            # Ideal claim length: 5 to 30 words
            if 5 <= word_count <= 25:
                score += 20.0
            elif 26 <= word_count <= 40:
                score += 10.0
            elif word_count < 4:
                score -= 25.0

            # Contains digits or currency (e.g. ₹50,000, 2026, 10%)
            if re.search(r"[₹$€£]\s*[\d,]+|\b\d+(?:%|k|cr|lakh|crore|billion|million)?\b", cleaned_candidate, re.IGNORECASE):
                score += 15.0

            # Contains factual action verbs
            lowered = cleaned_candidate.lower()
            for verb in FACTUAL_VERB_SIGNALS:
                if f" {verb} " in f" {lowered} ":
                    score += 15.0
                    break

            # Penalize question forms (usually inquiries, not authoritative claims)
            if cleaned_candidate.endswith("?"):
                score -= 20.0

            scored_candidates.append((cleaned_candidate, min(score, 99.0)))

        # Sort descending by score
        scored_candidates.sort(key=lambda x: x[1], reverse=True)

        main_claim, confidence = scored_candidates[0]

        # Remaining candidates become supporting sentences
        supporting = [c for c, _ in scored_candidates[1:5] if c != main_claim]

        return main_claim, supporting, round(confidence, 2)
