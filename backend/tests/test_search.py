"""Unit tests for Verification Strategy and Search Query generation."""

import pytest
from app.verification.strategy import StrategyEngine
from app.search.engine import SearchEngine, SearchResult


def test_strategy_engine_government():
    strategy = StrategyEngine.get_strategy("Government", "Government announces ₹50,000 scholarship.")
    assert "Government" in strategy.strategy_name
    assert "pib.gov.in" in strategy.priority_domains
    assert strategy.search_depth >= 2
    assert len(strategy.query_templates) >= 2


def test_strategy_engine_health():
    strategy = StrategyEngine.get_strategy("Health", "New COVID vaccine approved by regulators.")
    assert "who.int" in strategy.priority_domains or "icmr.gov.in" in strategy.priority_domains


def test_search_result_dataclass():
    res = SearchResult(
        title="Official PIB Release",
        url="https://pib.gov.in/PressReleasePage.aspx?PRID=12345",
        snippet="Ministry of Education announces scholarship funds for undergraduate students.",
        domain="pib.gov.in",
        is_priority=True
    )
    assert res.domain == "pib.gov.in"
    assert res.is_priority is True
