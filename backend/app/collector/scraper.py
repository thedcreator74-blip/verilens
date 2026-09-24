"""Evidence Collector & Content Extractor.

Fetches web pages asynchronously, extracts primary article body with Trafilatura/BeautifulSoup,
parses publication dates and metadata, and computes contextual snippets.
"""

import asyncio
import re
import time
from typing import List, Optional
import httpx
import trafilatura
from bs4 import BeautifulSoup
from app.config.settings import settings
from app.core.logging import logger
from app.registry.registry import registry
from app.schemas.evidence import EvidenceItem, SourceItem
from app.search.engine import SearchResult
from app.utils.text_cleaner import normalize_whitespace, extract_domain


class EvidenceCollector:
    """Asynchronously retrieves and extracts full text evidence from search result links."""

    USER_AGENT = (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    )

    @classmethod
    async def collect_evidence(
        cls,
        search_results: List[SearchResult],
        claim: str
    ) -> List[EvidenceItem]:
        """Collects and extracts evidence for all valid search results concurrently."""
        start_time = time.perf_counter()
        semaphore = asyncio.Semaphore(settings.MAX_CONCURRENT_SCRAPES)

        async with httpx.AsyncClient(
            timeout=settings.EVIDENCE_SCRAPE_TIMEOUT_SECONDS,
            headers={"User-Agent": cls.USER_AGENT},
            follow_redirects=True,
            verify=False  # Avoid SSL failures on public institutional portals
        ) as client:
            tasks = [
                cls._fetch_and_extract(client, item, claim, semaphore)
                for item in search_results
            ]
            results = await asyncio.gather(*tasks, return_exceptions=True)

        valid_evidence: List[EvidenceItem] = []
        for r in results:
            if isinstance(r, EvidenceItem):
                valid_evidence.append(r)

        elapsed_ms = (time.perf_counter() - start_time) * 1000
        logger.info(f"Evidence collector gathered {len(valid_evidence)} articles in {elapsed_ms:.2f}ms")
        return valid_evidence

    @classmethod
    async def _fetch_and_extract(
        cls,
        client: httpx.AsyncClient,
        item: SearchResult,
        claim: str,
        semaphore: asyncio.Semaphore
    ) -> Optional[EvidenceItem]:
        """Fetches a single page and parses content with Trafilatura and BS4."""
        domain = item.domain or extract_domain(item.url)
        domain_info = registry.get_domain_info(domain)
        publisher = domain_info.name if domain_info else domain
        is_official = domain_info.is_official if domain_info else registry.is_official_source(domain)

        async with semaphore:
            try:
                resp = await client.get(item.url)
                if resp.status_code != 200:
                    # Return lightweight evidence from search snippet
                    return EvidenceItem(
                        title=item.title,
                        url=item.url,
                        publisher=publisher,
                        publish_date=None,
                        snippet=item.snippet,
                        main_content=item.snippet,
                        language="en",
                        domain=domain,
                        relevance_score=50.0,
                        is_official=is_official
                    )

                html_content = resp.text

                # 1. Trafilatura Extraction (Optimal for article bodies)
                extracted_text = trafilatura.extract(
                    html_content,
                    include_comments=False,
                    include_tables=False,
                    no_fallback=False
                )

                # 2. BeautifulSoup Metadata Extraction (Title, Date, Fallback Text)
                soup = BeautifulSoup(html_content, "html.parser")

                # Extract publish date
                publish_date = cls._extract_date(soup)

                # Extract title if missing
                title = item.title
                if not title or len(title) < 5:
                    title_tag = soup.find("title")
                    if title_tag:
                        title = normalize_whitespace(title_tag.get_text())

                # Fallback text if Trafilatura was empty
                if not extracted_text:
                    paragraphs = [p.get_text() for p in soup.find_all("p")]
                    extracted_text = " ".join(paragraphs)

                clean_content = normalize_whitespace(extracted_text or item.snippet)

                # Contextual snippet generation
                snippet = cls._generate_contextual_snippet(clean_content, claim, item.snippet)

                return EvidenceItem(
                    title=title,
                    url=item.url,
                    publisher=publisher,
                    publish_date=publish_date,
                    snippet=snippet,
                    main_content=clean_content[:2000],  # Truncate to reasonable store length
                    language="en",
                    domain=domain,
                    relevance_score=0.0,  # Computed in Ranker
                    is_official=is_official
                )

            except Exception as e:
                logger.warning(f"Failed scraping {item.url}: {e}")
                # Fallback to search result snippet
                return EvidenceItem(
                    title=item.title,
                    url=item.url,
                    publisher=publisher,
                    publish_date=None,
                    snippet=item.snippet,
                    main_content=item.snippet,
                    language="en",
                    domain=domain,
                    relevance_score=40.0,
                    is_official=is_official
                )

    @classmethod
    def _extract_date(cls, soup: BeautifulSoup) -> Optional[str]:
        """Extracts publication date from standard metadata tags."""
        date_selectors = [
            ("meta", {"property": "article:published_time"}),
            ("meta", {"name": "publish-date"}),
            ("meta", {"name": "pubdate"}),
            ("meta", {"property": "og:pubdate"}),
            ("time", {}),
        ]
        for tag, attrs in date_selectors:
            el = soup.find(tag, attrs)
            if el:
                val = el.get("content") or el.get("datetime") or el.get_text(strip=True)
                if val and re.search(r"\d{4}", val):
                    return val[:19]
        return None

    @classmethod
    def _generate_contextual_snippet(cls, content: str, claim: str, fallback_snippet: str) -> str:
        """Extracts a 250-character window around key terms in the claim."""
        if not content:
            return fallback_snippet

        # Find first matching word of length >= 4
        words = [w for w in re.findall(r"\b\w{4,}\b", claim.lower()) if w not in {"with", "that", "from"}]
        for word in words:
            match = re.search(r"\b" + re.escape(word) + r"\b", content, re.IGNORECASE)
            if match:
                start = max(0, match.start() - 100)
                end = min(len(content), match.end() + 180)
                snippet = content[start:end].strip()
                if start > 0:
                    snippet = "..." + snippet
                if end < len(content):
                    snippet = snippet + "..."
                return normalize_whitespace(snippet)

        return fallback_snippet or content[:250]
