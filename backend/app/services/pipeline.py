"""Full Verification Pipeline Service.

Executes the end-to-end evidence preparation process without AI hallucination:
Validation -> OCR -> Claim Extraction -> Normalization -> Category Classification
-> Verification Strategy -> Trusted Search -> Evidence Collection -> Ranking -> Persistence.
"""

import time
from typing import Optional
from sqlalchemy.ext.asyncio import AsyncSession
import httpx
import trafilatura
from bs4 import BeautifulSoup
from app.config.settings import settings
from app.core.exceptions import InvalidInputException, ClaimExtractionException
from app.core.logging import logger
from app.ocr.engine import OCREngine
from app.claim.extractor import ClaimExtractor
from app.claim.normalizer import ClaimNormalizer
from app.category.classifier import CategoryClassifier
from app.verification.strategy import StrategyEngine
from app.search.engine import SearchEngine
from app.collector.scraper import EvidenceCollector
from app.ranking.ranker import EvidenceRanker
from app.schemas.response import VerificationResponse
from app.schemas.evidence import PipelineMetrics
from app.services.history_service import HistoryService
from app.utils.text_cleaner import normalize_whitespace
from app.utils.url_validator import validate_public_url


class VerificationPipeline:
    """Core evidence preparation engine."""

    @classmethod
    async def process_image(
        cls,
        image_bytes: bytes,
        session: Optional[AsyncSession] = None
    ) -> VerificationResponse:
        """Processes screenshot or photo submission through full verification pipeline."""
        start_pipeline = time.perf_counter()
        metrics = PipelineMetrics()

        # Step 1 & 2: Input Validation & OCR Processing
        extracted_text, ocr_conf, ocr_time = OCREngine.extract_text_from_bytes(image_bytes)
        metrics.ocr_time_ms = ocr_time

        if not extracted_text or len(extracted_text.strip()) < 5:
            raise InvalidInputException("No readable text could be recognized from the provided image.")

        # Step 3 to 10: Continue common verification steps
        return await cls._execute_pipeline(
            input_type="IMAGE",
            raw_input=extracted_text,
            text_for_claim=extracted_text,
            metrics=metrics,
            start_pipeline=start_pipeline,
            session=session
        )

    @classmethod
    async def process_text(
        cls,
        text: str,
        session: Optional[AsyncSession] = None
    ) -> VerificationResponse:
        """Processes raw text or social message submission."""
        start_pipeline = time.perf_counter()
        metrics = PipelineMetrics()

        cleaned_text = normalize_whitespace(text)
        if len(cleaned_text) < 3:
            raise InvalidInputException("Submitted text is too short for claim extraction.")

        return await cls._execute_pipeline(
            input_type="TEXT",
            raw_input=cleaned_text,
            text_for_claim=cleaned_text,
            metrics=metrics,
            start_pipeline=start_pipeline,
            session=session
        )

    @classmethod
    async def process_link(
        cls,
        url: str,
        session: Optional[AsyncSession] = None
    ) -> VerificationResponse:
        """Processes submitted news article URL."""
        start_pipeline = time.perf_counter()
        metrics = PipelineMetrics()

        # Validate URL and prevent SSRF
        valid_url = validate_public_url(url)

        # Scrape target URL to obtain title and headline/article content
        try:
            async with httpx.AsyncClient(
                timeout=settings.EVIDENCE_SCRAPE_TIMEOUT_SECONDS,
                headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"},
                follow_redirects=True,
                verify=False
            ) as client:
                resp = await client.get(valid_url)
                if resp.status_code >= 400:
                    raise InvalidInputException(f"Target URL returned HTTP status {resp.status_code}")
                html = resp.text
        except Exception as e:
            raise InvalidInputException(f"Could not retrieve contents from URL: {str(e)}")

        soup = BeautifulSoup(html, "html.parser")
        title = ""
        title_tag = soup.find("title")
        if title_tag:
            title = normalize_whitespace(title_tag.get_text())

        # Extract main text
        extracted_content = trafilatura.extract(html) or ""
        text_for_claim = f"{title}\n{extracted_content[:1500]}"

        return await cls._execute_pipeline(
            input_type="LINK",
            raw_input=valid_url,
            text_for_claim=text_for_claim,
            metrics=metrics,
            start_pipeline=start_pipeline,
            session=session
        )

    @classmethod
    async def _execute_pipeline(
        cls,
        input_type: str,
        raw_input: str,
        text_for_claim: str,
        metrics: PipelineMetrics,
        start_pipeline: float,
        session: Optional[AsyncSession] = None
    ) -> VerificationResponse:
        """Executes verification stages from claim extraction to ranking and persistence."""
        # Step 3: Claim Extraction Engine
        t_start = time.perf_counter()
        raw_claim, supporting, extract_conf = ClaimExtractor.extract_claim(text_for_claim)
        metrics.claim_extraction_time_ms = round((time.perf_counter() - t_start) * 1000, 2)

        if not raw_claim:
            raise ClaimExtractionException("Could not extract a verifiable factual claim from the input.")

        # Step 4: Claim Normalization Engine
        normalized_claim = ClaimNormalizer.normalize(raw_claim)
        if not normalized_claim:
            normalized_claim = raw_claim

        # Step 5: Category Classification Engine
        category, cat_confidence = CategoryClassifier.classify(normalized_claim)

        # Step 6: Verification Strategy Engine
        strategy = StrategyEngine.get_strategy(category, normalized_claim)

        # Step 7 & 8: Search Engine with Trusted Registry Lookup
        t_start = time.perf_counter()
        search_results = await SearchEngine.search_evidence(
            claim=normalized_claim,
            strategy=strategy,
            max_results=settings.SEARCH_MAX_RESULTS
        )
        metrics.search_time_ms = round((time.perf_counter() - t_start) * 1000, 2)

        # Step 9: Evidence Collector
        t_start = time.perf_counter()
        raw_evidence = await EvidenceCollector.collect_evidence(
            search_results=search_results,
            claim=normalized_claim
        )
        metrics.collector_time_ms = round((time.perf_counter() - t_start) * 1000, 2)

        # Step 10: Evidence Ranking Engine & Deduplication
        t_start = time.perf_counter()
        ranked_evidence, sources = EvidenceRanker.rank_and_deduplicate(
            evidence_list=raw_evidence,
            claim=normalized_claim,
            top_k=6
        )
        metrics.ranking_time_ms = round((time.perf_counter() - t_start) * 1000, 2)

        metrics.total_time_ms = round((time.perf_counter() - start_pipeline) * 1000, 2)

        # Step 11: History Persistence (if DB session provided)
        history_id = None
        if session is not None:
            try:
                record = await HistoryService.save_verification(
                    session=session,
                    input_type=input_type,
                    raw_input=raw_input[:2000],
                    extracted_claim=raw_claim,
                    normalized_claim=normalized_claim,
                    category=category,
                    confidence=cat_confidence,
                    verification_strategy=strategy.strategy_name,
                    sources=sources,
                    evidence=ranked_evidence,
                    metrics=metrics
                )
                history_id = record.id
            except Exception as e:
                logger.error(f"Failed persisting verification history: {e}", exc_info=True)

        return VerificationResponse(
            id=history_id,
            input_type=input_type,
            claim=raw_claim,
            normalized_claim=normalized_claim,
            category=category,
            confidence=cat_confidence,
            verification_strategy=strategy.strategy_name,
            sources=sources,
            evidence=ranked_evidence,
            metrics=metrics
        )
