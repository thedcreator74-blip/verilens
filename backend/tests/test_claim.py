"""Unit tests for Claim Extraction and Normalization."""

import pytest
from app.claim.extractor import ClaimExtractor
from app.claim.normalizer import ClaimNormalizer


def test_claim_extraction_filters_buttons_and_lead_in():
    ocr_input = (
        "BREAKING NEWS:\n"
        "Government gives ₹50,000 scholarship to college students\n"
        "Click Here to apply now!\n"
        "Limited time offer! Share with friends."
    )
    main_claim, supporting, confidence = ClaimExtractor.extract_claim(ocr_input)

    assert "Government gives ₹50,000 scholarship" in main_claim
    assert "Click Here" not in main_claim
    assert confidence >= 70.0


def test_claim_normalization_cleans_noise_and_emojis():
    raw_claim = "🚨🚨 BREAKING: The the government announced Rs. 50000 subsidy!!!!   "
    normalized = ClaimNormalizer.normalize(raw_claim)

    assert "🚨" not in normalized
    assert "The the" not in normalized
    assert "The government" in normalized
    assert "₹50000" in normalized
    assert normalized.endswith(".")
    assert "!!!!" not in normalized


def test_claim_normalization_removes_repeated_words():
    raw = "in in the recent election poll"
    normalized = ClaimNormalizer.normalize(raw)
    assert normalized.startswith("In the recent election poll")
