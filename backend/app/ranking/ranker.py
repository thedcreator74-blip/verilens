"""Evidence Ranking and Deduplication Engine.

Applies multi-criteria weighted scoring:
- Domain Trust Score (35%)
- Keyword / Content Similarity (30%)
- Official Source Status (20%)
- Freshness / Recency (10%)
- Content Completeness (5%)
Removes duplicate and near-duplicate evidence items.
"""

import re
import datetime
from typing import List, Tuple
from app.core.logging import logger
from app.registry.registry import registry
from app.schemas.evidence import EvidenceItem, SourceItem
from app.utils.text_cleaner import calculate_jaccard_similarity, extract_domain


class EvidenceRanker:
    """Ranks and filters collected evidence into an optimal verification package."""

    @classmethod
    def rank_and_deduplicate(
        cls,
        evidence_list: List[EvidenceItem],
        claim: str,
        top_k: int = 6
    ) -> Tuple[List[EvidenceItem], List[SourceItem]]:
        """
        Calculates scores, removes duplicates, and packages the top evidence and evaluated sources.
        """
        if not evidence_list:
            return [], []

        # 1. Deduplication by URL and high content similarity
        unique_items: List[EvidenceItem] = []
        seen_urls = set()

        for item in evidence_list:
            norm_url = item.url.split("#")[0].rstrip("/")
            if norm_url in seen_urls:
                continue
            seen_urls.add(norm_url)

            # Check for near duplicate snippet against already accepted items
            is_duplicate = False
            for accepted in unique_items:
                sim = calculate_jaccard_similarity(item.snippet, accepted.snippet)
                if sim > 0.85:
                    is_duplicate = True
                    break

            if not is_duplicate:
                unique_items.append(item)

        # 2. Score each item
        scored_items: List[Tuple[EvidenceItem, float]] = []

        for item in unique_items:
            score = cls._calculate_evidence_score(item, claim)
            item.relevance_score = round(score, 1)
            scored_items.append((item, score))

        # Sort descending by final score
        scored_items.sort(key=lambda x: x[1], reverse=True)

        top_evidence = [item for item, _ in scored_items[:top_k]]

        # 3. Build SourceItem summary list
        source_map = {}
        for ev in top_evidence:
            domain = ev.domain or extract_domain(ev.url)
            if domain not in source_map:
                meta = registry.get_domain_info(domain)
                trust = meta.trust_score if meta else registry.get_trust_score(domain)
                name = meta.name if meta else ev.publisher
                is_off = meta.is_official if meta else ev.is_official

                source_map[domain] = SourceItem(
                    domain=domain,
                    name=name,
                    url=ev.url,
                    trust_score=trust,
                    is_official=is_off,
                    category=meta.categories[0] if meta and meta.categories else None
                )

        sources = list(source_map.values())
        return top_evidence, sources

    @classmethod
    def _calculate_evidence_score(cls, item: EvidenceItem, claim: str) -> float:
        """Computes weighted multi-criteria relevance score (0.0 to 100.0)."""
        domain = item.domain or extract_domain(item.url)
        domain_trust = registry.get_trust_score(domain)

        # Factor 1: Domain Trust (35%)
        # domain_trust is 0 to 100
        score_domain = domain_trust * 0.35

        # Factor 2: Keyword / Semantic Similarity (30%)
        combined_text = f"{item.title} {item.snippet}"
        sim = calculate_jaccard_similarity(claim, combined_text)
        # Boost similarity scale (0.0 - 1.0 mapped to 0 - 100)
        score_similarity = min(sim * 150.0, 100.0) * 0.30

        # Factor 3: Official Source Priority (20%)
        is_official = item.is_official or registry.is_official_source(domain)
        score_official = (100.0 if is_official else 40.0) * 0.20

        # Factor 4: Freshness (10%)
        score_freshness = cls._calculate_freshness_score(item.publish_date) * 0.10

        # Factor 5: Completeness (5%)
        completeness = 100.0 if len(item.main_content or "") > 200 else 50.0
        score_completeness = completeness * 0.05

        total_score = score_domain + score_similarity + score_official + score_freshness + score_completeness
        return min(max(total_score, 0.0), 100.0)

    @classmethod
    def _calculate_freshness_score(cls, date_str: str = None) -> float:
        """Scores publication recency."""
        if not date_str:
            return 60.0  # Neutral default for undated pages

        try:
            # Try parsing ISO year
            year_match = re.search(r"\b(202[0-6]|201[8-9])\b", date_str)
            if year_match:
                year = int(year_match.group(1))
                if year >= 2025:
                    return 95.0
                elif year == 2024:
                    return 85.0
                elif year == 2023:
                    return 75.0
                else:
                    return 60.0
        except Exception:
            pass

        return 60.0
