"""Text cleaning, normalization, and tokenization utilities."""

import re
import unicodedata
from urllib.parse import urlparse
from typing import List, Set


# Regex pattern to match emojis and pictographs
EMOJI_PATTERN = re.compile(
    "["
    "\U0001F600-\U0001F64F"  # emoticons
    "\U0001F300-\U0001F5FF"  # symbols & pictographs
    "\U0001F680-\U0001F6FF"  # transport & map symbols
    "\U0001F1E0-\U0001F1FF"  # flags (iOS)
    "\U00002702-\U000027B0"
    "\U000024C2-\U0001F251"
    "\U0001F900-\U0001F9FF"  # supplemental symbols
    "\U0001FA70-\U0001FAFF"
    "]+",
    flags=re.UNICODE
)


def remove_emojis(text: str) -> str:
    """Strip emojis and pictorial characters from string."""
    return EMOJI_PATTERN.sub("", text)


def normalize_whitespace(text: str) -> str:
    """Collapses duplicate spaces, carriage returns, tabs, and zero-width spaces."""
    # Remove zero-width spaces & soft hyphens
    text = re.sub(r"[\u200B-\u200D\uFEFF\u00AD]", "", text)
    # Replace non-breaking spaces
    text = text.replace("\u00A0", " ")
    # Replace line breaks and tabs with single space
    text = re.sub(r"[\r\n\t]+", " ", text)
    # Collapse multiple spaces
    text = re.sub(r"\s{2,}", " ", text)
    return text.strip()


def remove_repeated_words(text: str) -> str:
    """Removes immediately repeated duplicate words (e.g. 'the the' -> 'the')."""
    return re.sub(r"\b(\w+)(?:\s+\1\b)+", r"\1", text, flags=re.IGNORECASE)


def extract_domain(url: str) -> str:
    """Extracts a clean, lowercase domain name from any valid URL."""
    try:
        parsed = urlparse(url)
        netloc = parsed.netloc.lower().split(":")[0]
        # Remove common 'www.' prefix
        if netloc.startswith("www."):
            netloc = netloc[4:]
        return netloc
    except Exception:
        return ""


def tokenize_words(text: str) -> List[str]:
    """Extracts lowercase alphabetic and numeric words."""
    cleaned = remove_emojis(text).lower()
    return re.findall(r"\b[a-zA-Z0-9_\u0B80-\u0BFF]{2,}\b", cleaned)


def calculate_jaccard_similarity(text1: str, text2: str) -> float:
    """Calculates Jaccard token similarity between two text strings."""
    tokens1 = set(tokenize_words(text1))
    tokens2 = set(tokenize_words(text2))
    if not tokens1 or not tokens2:
        return 0.0
    intersection = tokens1.intersection(tokens2)
    union = tokens1.union(tokens2)
    return float(len(intersection)) / float(len(union))


def extract_sentences(text: str) -> List[str]:
    """Splits text into discrete, cleaned sentence units."""
    # Normalize paragraph breaks
    text = text.replace("\r", "\n")
    raw_sentences = re.split(r"(?<=[.!?])\s+|\n+", text)
    cleaned = []
    for s in raw_sentences:
        item = normalize_whitespace(s)
        if len(item) > 10:
            cleaned.append(item)
    return cleaned
