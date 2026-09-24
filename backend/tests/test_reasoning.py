"""Unit and integration tests for AI Reasoning Engine."""

import pytest
import json
from app.reasoning.confidence import ConfidenceCalculator, ConflictDetector
from app.reasoning.formatter import ReportFormatter
from app.reasoning.deterministic import DeterministicInvestigator
from app.reasoning.engine import ReasoningEngine
from app.schemas.reasoning import CredibilityReport


# Sample Fixtures
MOCK_OFFICIAL_EVIDENCE = [
    {
        "title": "Cabinet Approves Expansion of Solar Scheme",
        "publisher": "Press Information Bureau",
        "url": "https://pib.gov.in/PressReleasePage.aspx?PRID=199999",
        "snippet": "The Union Cabinet has officially approved the PM Surya Ghar Muft Bijli Yojana with total outlay of Rs 75,021 crore.",
        "main_content": "Detailed gazette approved rollout across 10 million households nationwide with 300 units free solar electricity.",
        "domain": "pib.gov.in",
        "relevance_score": 92.5,
        "is_official": True,
        "publish_date": "2025-02-15"
    },
    {
        "title": "Government Rolls Out PM Surya Ghar Rooftop Solar Initiative",
        "publisher": "The Hindu",
        "url": "https://thehindu.com/news/national/pm-surya-ghar-scheme/article.ece",
        "snippet": "Prime Minister announces rooftop solar subsidy scheme providing up to Rs 78,000 subsidy.",
        "main_content": "Eligible households can apply via the national portal.",
        "domain": "thehindu.com",
        "relevance_score": 88.0,
        "is_official": False,
        "publish_date": "2025-02-16"
    }
]

MOCK_OFFICIAL_SOURCES = [
    {"domain": "pib.gov.in", "name": "Press Information Bureau", "url": "https://pib.gov.in", "trust_score": 98.0, "is_official": True},
    {"domain": "thehindu.com", "name": "The Hindu", "url": "https://thehindu.com", "trust_score": 90.0, "is_official": False}
]

MOCK_MISLEADING_EVIDENCE = [
    {
        "title": "PIB Fact Check Clarifies Viral Free Laptop Message Is Fake",
        "publisher": "PIB Fact Check",
        "url": "https://pib.gov.in/FactCheck/laptop.aspx",
        "snippet": "A viral WhatsApp message claiming Government is distributing free laptops to all students under Free Laptop Scheme 2025 is fake. No such scheme has been approved.",
        "main_content": "PIB Fact Check explicitly debunks the viral notification.",
        "domain": "pib.gov.in",
        "relevance_score": 95.0,
        "is_official": True,
        "publish_date": "2025-01-10"
    },
    {
        "title": "Fact Check: Don't fall for fake student laptop scheme links",
        "publisher": "Boom Live",
        "url": "https://boomlive.in/fact-check/free-laptop-scheme-hoax",
        "snippet": "The circular circulating on social media promising free laptops is a fraudulent phishing scam.",
        "main_content": "Officials confirmed the Ministry of Education has issued no such notification.",
        "domain": "boomlive.in",
        "relevance_score": 89.0,
        "is_official": False,
        "publish_date": "2025-01-11"
    }
]

MOCK_CONFLICTING_EVIDENCE = [
    {
        "title": "Ministry Denies Rumours of Early Exam Postponement",
        "publisher": "Press Information Bureau",
        "url": "https://pib.gov.in/exams.aspx",
        "snippet": "PIB Fact Check clarifies that claims regarding the postponement of nationwide entrance exams are false claim and unverified rumour.",
        "domain": "pib.gov.in",
        "relevance_score": 90.0,
        "is_official": True
    },
    {
        "title": "Exam Body Announces Revised Examination Schedule",
        "publisher": "NDTV",
        "url": "https://ndtv.com/education/exam-postponed",
        "snippet": "Officials officially launched revised schedules for selected regional centers due to state holidays.",
        "domain": "ndtv.com",
        "relevance_score": 85.0,
        "is_official": False
    }
]


def test_conflict_detection():
    """Verifies that contradictory evidence triggers conflict detection."""
    has_conflict, explanation, positions = ConflictDetector.detect_conflicts(
        evidence_items=MOCK_CONFLICTING_EVIDENCE,
        sources=[{"domain": "pib.gov.in"}, {"domain": "ndtv.com"}]
    )
    assert has_conflict is True
    assert "conflicting" in explanation.lower() or "discrepancy" in explanation.lower()
    assert len(positions) >= 2


def test_confidence_calculator_high_corroboration():
    """Tests high confidence score when multiple official sources agree."""
    score, reasons = ConfidenceCalculator.calculate(
        evidence_items=MOCK_OFFICIAL_EVIDENCE,
        sources=MOCK_OFFICIAL_SOURCES,
        has_conflict=False,
        assessment="Likely Credible"
    )
    assert 75 <= score <= 99
    assert any("official" in r.lower() for r in reasons)


def test_confidence_calculator_zero_evidence():
    """Tests baseline score when zero evidence exists."""
    score, reasons = ConfidenceCalculator.calculate(
        evidence_items=[],
        sources=[],
        has_conflict=False,
        assessment="Insufficient Information"
    )
    assert score <= 30
    assert any("zero" in r.lower() or "lack" in r.lower() for r in reasons)


def test_report_formatter_strict_assessments():
    """Ensures forbidden words (Fake, Real, True, False) are mapped to allowed assessments."""
    raw_json = json.dumps({
        "claim": "Government gives free solar panels",
        "verified_information": "Official schemes exist with specific subsidy caps.",
        "assessment": "True",  # Forbidden raw term
        "confidence": 85,
        "reasoning": ["Matches gazette"],
        "evidence_summary": "Evaluated 2 sources",
        "recommendations": ["Check official site"],
        "sources": []
    })

    report = ReportFormatter.parse_and_validate(raw_json, fallback_claim="Sample", collected_sources=[])
    assert report.assessment == "Likely Credible"
    assert report.disclaimer == "This assessment is generated using AI based on available evidence and should not replace official verification."


def test_report_formatter_misleading_mapping():
    """Ensures 'Fake' raw output is mapped to 'Potentially Misleading'."""
    raw_json = json.dumps({
        "claim": "WhatsApp message about free laptops",
        "verified_information": "PIB has issued an explicit denial.",
        "assessment": "Fake",
        "confidence": 90,
        "reasoning": ["Debunked by PIB"],
        "evidence_summary": "Fact-checks available",
        "recommendations": ["Do not click phishing links"],
        "sources": []
    })

    report = ReportFormatter.parse_and_validate(raw_json, fallback_claim="Sample", collected_sources=[])
    assert report.assessment == "Potentially Misleading"


@pytest.mark.asyncio
async def test_deterministic_investigator_credible():
    """Tests deterministic investigator on corroborated claim."""
    engine = ReasoningEngine()
    report = await engine.analyze(
        claim="Cabinet approved PM Surya Ghar Muft Bijli Yojana",
        normalized_claim="PM Surya Ghar Muft Bijli Yojana solar scheme",
        category="Government",
        evidence=MOCK_OFFICIAL_EVIDENCE,
        sources=MOCK_OFFICIAL_SOURCES
    )

    assert isinstance(report, CredibilityReport)
    assert report.assessment == "Likely Credible"
    assert report.confidence >= 70
    assert len(report.reasoning) >= 1
    assert len(report.recommendations) >= 1
    assert len(report.sources) >= 1
    assert "official verification" in report.disclaimer


@pytest.mark.asyncio
async def test_deterministic_investigator_misleading():
    """Tests deterministic investigator on debunked / viral claim."""
    engine = ReasoningEngine()
    report = await engine.analyze(
        claim="Free laptop distributed to all students by education ministry",
        normalized_claim="Free laptop scheme distribution students",
        category="Government",
        evidence=MOCK_MISLEADING_EVIDENCE,
        sources=[{"domain": "pib.gov.in", "trust_score": 98.0, "is_official": True}]
    )

    assert report.assessment == "Potentially Misleading"
    assert report.confidence >= 70
    assert any("clarified" in r.lower() or "refuted" in r.lower() or "misleading" in r.lower() for r in report.reasoning)


@pytest.mark.asyncio
async def test_deterministic_investigator_zero_evidence():
    """Tests deterministic investigator behavior on unknown claims without evidence."""
    engine = ReasoningEngine()
    report = await engine.analyze(
        claim="Aliens landed at central station yesterday evening",
        normalized_claim="Aliens landed central station",
        category="General",
        evidence=[],
        sources=[]
    )

    assert report.assessment == "Insufficient Information"
    assert report.confidence <= 40
    assert len(report.sources) == 0
