"""Claim Normalization Engine.

Converts extracted claims into clean, grammatically sound, canonical statements
optimized for search query generation and information retrieval.
"""

import re
from app.utils.text_cleaner import (
    remove_emojis,
    normalize_whitespace,
    remove_repeated_words
)


class ClaimNormalizer:
    """Normalizes raw claim statements."""

    @classmethod
    def normalize(cls, claim: str) -> str:
        """
        Normalizes punctuation, removes duplicates, cleans formatting,
        and standardizes sentence structure.
        """
        if not claim or not claim.strip():
            return ""

        # Step 1: Strip emojis and pictographs
        text = remove_emojis(claim)

        # Step 2: Remove repeated words ("the the", "in in")
        text = remove_repeated_words(text)

        # Step 3: Normalize currency formatting (Rs. / INR / ₹)
        text = re.sub(r"\b(?:Rs\.?|INR)\s*(\d+)", r"₹\1", text, flags=re.IGNORECASE)
        # Ensure no space between currency symbol and number (e.g. "₹ 50,000" -> "₹50,000")
        text = re.sub(r"([₹$€£])\s+(\d)", r"\1\2", text)

        # Step 4: Remove repetitive excessive punctuation (e.g. "!!!!", "????", "..")
        text = re.sub(r"!{2,}", "!", text)
        text = re.sub(r"\?{2,}", "?", text)
        text = re.sub(r"\.{2,}", ".", text)
        text = re.sub(r"[,;]{2,}", ",", text)

        # Step 5: Remove leading/trailing quotation marks, hyphens, colons
        text = re.sub(r"^[\s\"'‘“:;\-–—]+", "", text)
        text = re.sub(r"[\s\"'’”\-–—]+$", "", text)

        # Step 6: Normalize spacing around punctuation
        text = re.sub(r"\s+([.,!?;:])", r"\1", text)

        # Step 7: Collapse whitespace
        text = normalize_whitespace(text)

        if not text:
            return ""

        # Step 8: Ensure clean sentence capitalization
        text = text[0].upper() + text[1:]

        # Step 9: Ensure clean terminal punctuation
        if not text.endswith((".", "!", "?")):
            text = text + "."

        return text
