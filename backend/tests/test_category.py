"""Unit tests for Category Classification."""

import pytest
from app.category.classifier import CategoryClassifier


def test_classify_government_claim():
    claim = "Ministry of Finance issued official notification for student scholarship scheme."
    category, confidence = CategoryClassifier.classify(claim)
    assert category == "Government"
    assert confidence >= 75.0


def test_classify_health_claim():
    claim = "WHO and CDC released emergency advisory on new vaccine symptoms and virus prevention."
    category, confidence = CategoryClassifier.classify(claim)
    assert category == "Health"
    assert confidence >= 80.0


def test_classify_finance_claim():
    claim = "RBI governor declared change in repo interest rate and bank inflation guidelines."
    category, confidence = CategoryClassifier.classify(claim)
    assert category == "Finance"
    assert confidence >= 75.0


def test_classify_technology_claim():
    claim = "OpenAI and Google reveal new AI semiconductor chip architecture."
    category, confidence = CategoryClassifier.classify(claim)
    assert category == "Technology"
    assert confidence >= 70.0


def test_classify_education_claim():
    claim = "UGC and AICTE announce new engineering syllabus and college exam guidelines."
    category, confidence = CategoryClassifier.classify(claim)
    assert category == "Education"
    assert confidence >= 75.0


def test_classify_general_fallback():
    claim = "A tree fell on the sidewalk near the corner shop yesterday morning."
    category, confidence = CategoryClassifier.classify(claim)
    assert category == "General"
