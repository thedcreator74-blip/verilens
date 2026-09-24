"""Verification Strategy Engine.

Formulates targeted, category-aware retrieval strategies.
Determines search depth, official domain priorities, and progressive fallback rules.
"""

from dataclasses import dataclass, field
from typing import List, Dict, Any
from app.registry.registry import registry


@dataclass
class VerificationStrategy:
    strategy_name: str
    category: str
    priority_domains: List[str]
    trusted_domains: List[str]
    fallback_domains: List[str]
    search_depth: int = 2
    max_sources: int = 8
    exact_match_required: bool = False
    query_templates: List[str] = field(default_factory=list)

    def to_dict(self) -> Dict[str, Any]:
        return {
            "strategy_name": self.strategy_name,
            "category": self.category,
            "priority_domains": self.priority_domains,
            "trusted_domains": self.trusted_domains,
            "fallback_domains": self.fallback_domains,
            "search_depth": self.search_depth,
            "max_sources": self.max_sources,
        }


class StrategyEngine:
    """Computes the optimal verification trajectory based on category classification."""

    @classmethod
    def get_strategy(cls, category: str, normalized_claim: str) -> VerificationStrategy:
        """Constructs an evidence verification strategy plan."""
        domains = registry.get_domains_for_category(category)
        priority_domains = domains.get("priority_domains", [])
        trusted_domains = domains.get("trusted_domains", [])
        fallback_domains = domains.get("fallback_domains", [])

        strategy_name = f"{category} Strategy"

        if category == "Government":
            strategy_name = "Government Official Gazettes & Notifications Strategy"
            search_depth = 3
            max_sources = 8
        elif category == "Health":
            strategy_name = "Medical Health Organizations & Journals Strategy"
            search_depth = 3
            max_sources = 8
        elif category == "Finance":
            strategy_name = "Financial Regulators & Market Filings Strategy"
            search_depth = 2
            max_sources = 6
        elif category == "Technology":
            strategy_name = "Tech Documentation & Authoritative Review Strategy"
            search_depth = 2
            max_sources = 6
        elif category == "Education":
            strategy_name = "Educational Councils & Institutional Portals Strategy"
            search_depth = 2
            max_sources = 6
        else:
            strategy_name = f"{category} Verification Strategy"
            search_depth = 2
            max_sources = 6

        # Formulate search queries:
        # 1. Exact or keyword phrase scoped to top priority domain
        # 2. General phrase query across trusted news
        # 3. Broadened query
        query_templates = []
        if priority_domains:
            primary_site = priority_domains[0]
            query_templates.append(f"site:{primary_site} {normalized_claim}")
        query_templates.append(normalized_claim)

        return VerificationStrategy(
            strategy_name=strategy_name,
            category=category,
            priority_domains=priority_domains,
            trusted_domains=trusted_domains,
            fallback_domains=fallback_domains,
            search_depth=search_depth,
            max_sources=max_sources,
            exact_match_required=False,
            query_templates=query_templates
        )
