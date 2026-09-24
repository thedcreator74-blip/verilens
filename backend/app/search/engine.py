"""Evidence Search Engine.

Performs domain-prioritized, multi-query asynchronous search
supporting exact matching, phrase search, and progressive fallback broadening.
"""

import asyncio
import re
import time
import urllib.parse
from dataclasses import dataclass
from typing import List, Dict, Any, Optional
import httpx
from bs4 import BeautifulSoup
from app.config.settings import settings
from app.core.logging import logger
from app.verification.strategy import VerificationStrategy
from app.utils.text_cleaner import extract_domain


@dataclass
class SearchResult:
    title: str
    url: str
    snippet: str
    domain: str
    is_priority: bool = False


class SearchEngine:
    """Multi-provider asynchronous evidence search engine."""

    USER_AGENT = (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    )

    @classmethod
    async def search_evidence(
        cls,
        claim: str,
        strategy: VerificationStrategy,
        max_results: int = 8
    ) -> List[SearchResult]:
        """
        Executes multi-tier search:
        Tier 1: Priority domain queries (e.g. site:pib.gov.in <claim>)
        Tier 2: General phrase query
        Tier 3: Broadened keyword query if Tier 1+2 yield < 3 results
        """
        start_time = time.perf_counter()
        results: List[SearchResult] = []
        seen_urls = set()

        async with httpx.AsyncClient(
            timeout=settings.SEARCH_TIMEOUT_SECONDS,
            headers={"User-Agent": cls.USER_AGENT},
            follow_redirects=True
        ) as client:
            # Construct search queries
            queries = []

            # Tier 1: Priority Domain search
            if strategy.priority_domains:
                top_domain = strategy.priority_domains[0]
                queries.append((f"site:{top_domain} {claim}", True))

            # Tier 2: Standard phrase query
            queries.append((claim, False))

            # Execute search tasks concurrently
            tasks = [cls._execute_query(client, q, is_prio) for q, is_prio in queries]
            batch_results = await asyncio.gather(*tasks, return_exceptions=True)

            for batch in batch_results:
                if isinstance(batch, list):
                    for item in batch:
                        if item.url not in seen_urls:
                            seen_urls.add(item.url)
                            results.append(item)

            # Tier 3: Fallback broadening if under-represented
            if len(results) < 3:
                # Extract 4-6 most significant words (length >= 4, not stop words)
                stop_words = {"the", "and", "for", "with", "that", "this", "from", "have", "gives", "about"}
                words = [w for w in re.findall(r"\b\w{4,}\b", claim.lower()) if w not in stop_words]
                broad_query = " ".join(words[:5])
                if broad_query and broad_query != claim.lower():
                    logger.info(f"Broadening search query to: '{broad_query}'")
                    fallback_batch = await cls._execute_query(client, broad_query, False)
                    for item in fallback_batch:
                        if item.url not in seen_urls:
                            seen_urls.add(item.url)
                            results.append(item)

        elapsed_ms = (time.perf_counter() - start_time) * 1000
        logger.info(f"Search engine completed: retrieved {len(results)} items in {elapsed_ms:.2f}ms")
        return results[:max_results]

    @classmethod
    async def _execute_query(
        cls,
        client: httpx.AsyncClient,
        query: str,
        is_priority: bool
    ) -> List[SearchResult]:
        """Runs search against available backends (DuckDuckGo or Google CSE)."""
        # If Google CSE is configured, use it
        if settings.GOOGLE_CUSTOM_SEARCH_API_KEY and settings.GOOGLE_CSE_ID:
            try:
                return await cls._search_google_cse(client, query, is_priority)
            except Exception as e:
                logger.warning(f"Google CSE failed: {e}. Falling back to DuckDuckGo.")

        # Default: DuckDuckGo HTML / Lite search
        try:
            return await cls._search_duckduckgo(client, query, is_priority)
        except Exception as e:
            logger.warning(f"DuckDuckGo search error for '{query}': {e}")
            return []

    @classmethod
    async def _search_duckduckgo(
        cls,
        client: httpx.AsyncClient,
        query: str,
        is_priority: bool
    ) -> List[SearchResult]:
        """Scrapes DuckDuckGo HTML search results without API keys."""
        url = "https://html.duckduckgo.com/html/"
        data = {"q": query}
        response = await client.post(url, data=data)
        if response.status_code != 200:
            return []

        soup = BeautifulSoup(response.text, "html.parser")
        results = []

        for result_div in soup.find_all("div", class_="result"):
            title_tag = result_div.find("a", class_="result__a")
            snippet_tag = result_div.find("a", class_="result__snippet")

            if not title_tag:
                continue

            raw_url = title_tag.get("href", "")
            # DuckDuckGo wraps URLs in /l/?kh=-1&uddg=<encoded_url>
            if "uddg=" in raw_url:
                parsed = urllib.parse.parse_qs(urllib.parse.urlparse(raw_url).query)
                clean_url = parsed.get("uddg", [raw_url])[0]
            else:
                clean_url = raw_url

            title = title_tag.get_text(strip=True)
            snippet = snippet_tag.get_text(strip=True) if snippet_tag else ""
            domain = extract_domain(clean_url)

            if domain and clean_url.startswith("http"):
                results.append(SearchResult(
                    title=title,
                    url=clean_url,
                    snippet=snippet,
                    domain=domain,
                    is_priority=is_priority
                ))

            if len(results) >= 6:
                break

        return results

    @classmethod
    async def _search_google_cse(
        cls,
        client: httpx.AsyncClient,
        query: str,
        is_priority: bool
    ) -> List[SearchResult]:
        """Queries Google Custom Search JSON API."""
        url = "https://www.googleapis.com/customsearch/v1"
        params = {
            "key": settings.GOOGLE_CUSTOM_SEARCH_API_KEY,
            "cx": settings.GOOGLE_CSE_ID,
            "q": query,
            "num": 5
        }
        resp = await client.get(url, params=params)
        if resp.status_code != 200:
            return []

        data = resp.json()
        items = data.get("items", [])
        results = []
        for item in items:
            link = item.get("link", "")
            title = item.get("title", "")
            snippet = item.get("snippet", "")
            domain = extract_domain(link)
            if domain and link.startswith("http"):
                results.append(SearchResult(
                    title=title,
                    url=link,
                    snippet=snippet,
                    domain=domain,
                    is_priority=is_priority
                ))
        return results
