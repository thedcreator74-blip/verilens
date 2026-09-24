"""Configurable Trusted Source Registry.

Manages reputations, official status, trust scores, and domain groupings
for high-precision evidence retrieval across categories.
"""

import json
import os
from pathlib import Path
from typing import Dict, List, Optional, Any
from app.core.logging import logger


class DomainMetadata:
    def __init__(
        self,
        domain: str,
        name: str,
        reputation: str,
        trust_score: float,
        categories: List[str],
        is_official: bool = False
    ):
        self.domain = domain.lower()
        self.name = name
        self.reputation = reputation
        self.trust_score = float(trust_score)
        self.categories = categories
        self.is_official = is_official

    def to_dict(self) -> Dict[str, Any]:
        return {
            "domain": self.domain,
            "name": self.name,
            "reputation": self.reputation,
            "trust_score": self.trust_score,
            "categories": self.categories,
            "is_official": self.is_official
        }


class TrustedSourceRegistry:
    """Singleton registry for authoritative source validation."""

    _instance = None

    def __new__(cls, *args, **kwargs):
        if cls._instance is None:
            cls._instance = super(TrustedSourceRegistry, cls).__new__(cls)
            cls._instance._initialized = False
        return cls._instance

    def __init__(self, config_path: Optional[str] = None):
        if self._initialized:
            return

        self.domains: Dict[str, DomainMetadata] = {}
        self.category_mappings: Dict[str, Dict[str, List[str]]] = {}
        self.config_path = config_path or os.path.join(
            os.path.dirname(__file__), "trusted_sources.json"
        )
        self.load_registry()
        self._initialized = True

    def load_registry(self) -> None:
        """Loads domain definitions from JSON configuration."""
        try:
            path = Path(self.config_path)
            if not path.exists():
                logger.warning(f"Trusted source file {self.config_path} not found. Initializing empty registry.")
                return

            with open(path, "r", encoding="utf-8") as f:
                data = json.load(f)

            raw_domains = data.get("domains", {})
            for domain_str, meta in raw_domains.items():
                self.domains[domain_str.lower()] = DomainMetadata(
                    domain=domain_str,
                    name=meta.get("name", domain_str),
                    reputation=meta.get("reputation", "medium"),
                    trust_score=meta.get("trust_score", 60.0),
                    categories=meta.get("categories", []),
                    is_official=meta.get("is_official", False)
                )

            self.category_mappings = data.get("category_mappings", {})
            logger.info(f"Loaded {len(self.domains)} trusted domains across {len(self.category_mappings)} categories.")
        except Exception as e:
            logger.error(f"Failed loading trusted source registry: {e}", exc_info=True)

    def get_domain_info(self, domain: str) -> Optional[DomainMetadata]:
        """Retrieves metadata for a specific domain, supporting subdomains."""
        clean_domain = domain.lower().strip()
        if clean_domain in self.domains:
            return self.domains[clean_domain]

        # Check if parent domain exists (e.g., news.google.com -> google.com)
        parts = clean_domain.split(".")
        if len(parts) > 2:
            parent = ".".join(parts[-2:])
            if parent in self.domains:
                return self.domains[parent]

        # Official government domain heuristics (.gov, .gov.in, .nic.in, .edu)
        if clean_domain.endswith((".gov", ".gov.in", ".nic.in")):
            return DomainMetadata(
                domain=clean_domain,
                name=f"Official Government Portal ({clean_domain})",
                reputation="official",
                trust_score=95.0,
                categories=["Government"],
                is_official=True
            )

        if clean_domain.endswith((".edu", ".ac.in", ".edu.in")):
            return DomainMetadata(
                domain=clean_domain,
                name=f"Educational Institution ({clean_domain})",
                reputation="official",
                trust_score=92.0,
                categories=["Education"],
                is_official=True
            )

        return None

    def get_trust_score(self, domain: str) -> float:
        """Returns trust score between 0.0 and 100.0."""
        info = self.get_domain_info(domain)
        if info:
            return info.trust_score
        return 50.0  # Baseline neutral trust for uncatalogued domains

    def is_official_source(self, domain: str) -> bool:
        """Determines if domain is considered an official authoritative institution."""
        info = self.get_domain_info(domain)
        if info:
            return info.is_official
        return False

    def get_domains_for_category(self, category: str) -> Dict[str, List[str]]:
        """
        Returns priority_domains, trusted_domains, and fallback_domains for category.
        """
        mapping = self.category_mappings.get(category)
        if mapping:
            return mapping
        # Default fallback mapping
        return self.category_mappings.get("General", {
            "priority_domains": ["pib.gov.in", "reuters.com", "apnews.com"],
            "trusted_domains": ["bbc.com", "thehindu.com", "altnews.in"],
            "fallback_domains": ["boomlive.in"]
        })

    def register_domain(
        self,
        domain: str,
        name: str,
        reputation: str,
        trust_score: float,
        categories: List[str],
        is_official: bool = False
    ) -> None:
        """Dynamically registers or updates domain reputation."""
        self.domains[domain.lower()] = DomainMetadata(
            domain=domain,
            name=name,
            reputation=reputation,
            trust_score=trust_score,
            categories=categories,
            is_official=is_official
        )


registry = TrustedSourceRegistry()
