"""Unit tests for Trusted Source Registry."""

import pytest
from app.registry.registry import TrustedSourceRegistry, registry


def test_registry_loaded_domains():
    assert len(registry.domains) > 10
    pib = registry.get_domain_info("pib.gov.in")
    assert pib is not None
    assert pib.is_official is True
    assert pib.trust_score >= 95.0


def test_registry_official_subdomain_and_heuristic():
    info = registry.get_domain_info("department.gov.in")
    assert info is not None
    assert info.is_official is True
    assert info.trust_score >= 90.0


def test_registry_category_domains():
    gov_domains = registry.get_domains_for_category("Government")
    assert "pib.gov.in" in gov_domains["priority_domains"]
    assert "reuters.com" in gov_domains["trusted_domains"]


def test_dynamic_domain_registration():
    registry.register_domain(
        domain="custom-factcheck.org",
        name="Custom Fact Check Portal",
        reputation="high",
        trust_score=88.5,
        categories=["General"],
        is_official=False
    )
    score = registry.get_trust_score("custom-factcheck.org")
    assert score == 88.5
