"""Unit tests for Evidence Ranking and Deduplication."""

import pytest
from app.ranking.ranker import EvidenceRanker
from app.schemas.evidence import EvidenceItem


def test_ranking_prefers_official_trusted_source():
    claim = "Government announced scholarship scheme."

    official_item = EvidenceItem(
        title="Official PIB Release: Scholarship for students",
        url="https://pib.gov.in/release/123",
        publisher="Press Information Bureau",
        publish_date="2026-02-15",
        snippet="Government has officially announced ₹50,000 scholarship scheme.",
        main_content="Full article body detailing scholarship disbursed...",
        language="en",
        domain="pib.gov.in",
        relevance_score=0.0,
        is_official=True
    )

    unverified_blog = EvidenceItem(
        title="Scholarship rumors online",
        url="https://random-rumor-blog.xyz/post/1",
        publisher="Random Blog",
        publish_date="2022-01-01",
        snippet="People are talking about maybe a scholarship exists.",
        main_content="Some short blog text...",
        language="en",
        domain="random-rumor-blog.xyz",
        relevance_score=0.0,
        is_official=False
    )

    ranked, sources = EvidenceRanker.rank_and_deduplicate(
        evidence_list=[unverified_blog, official_item],
        claim=claim
    )

    assert len(ranked) == 2
    # Official PIB should be ranked #1
    assert ranked[0].domain == "pib.gov.in"
    assert ranked[0].relevance_score > ranked[1].relevance_score
    assert len(sources) == 2


def test_deduplication_removes_identical_urls():
    claim = "Government announcement."

    item1 = EvidenceItem(
        title="Release 1",
        url="https://pib.gov.in/release/123#section1",
        publisher="PIB",
        publish_date="2026-02-15",
        snippet="Government has announced new scheme.",
        language="en",
        domain="pib.gov.in",
        relevance_score=0.0,
        is_official=True
    )

    item2 = EvidenceItem(
        title="Release 1 Duplicate",
        url="https://pib.gov.in/release/123",
        publisher="PIB",
        publish_date="2026-02-15",
        snippet="Government has announced new scheme.",
        language="en",
        domain="pib.gov.in",
        relevance_score=0.0,
        is_official=True
    )

    ranked, sources = EvidenceRanker.rank_and_deduplicate(
        evidence_list=[item1, item2],
        claim=claim
    )

    assert len(ranked) == 1
