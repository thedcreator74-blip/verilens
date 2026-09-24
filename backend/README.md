# VeriLens AI - Evidence Preparation & Verification Engine Backend

Production-grade FastAPI verification engine for VeriLens AI. Prepares verified evidence packages from user-submitted screenshots, raw text, and news links.

## Architecture Highlights
- **10-Step Verification Pipeline**: Input Validation -> OCR Processing -> Claim Extraction -> Claim Normalization -> Category Classification -> Verification Strategy -> Trusted Registry Lookup -> Multi-Source Evidence Search -> Evidence Collection -> Multi-Criteria Evidence Ranking.
- **Zero Hallucination / No AI Assumptions**: Prepares empirical, verifiable, multi-source evidence without synthesizing unverified summaries or credibility scores.
- **AI Reasoning Engine (Investigator Analyst Layer)**:
  - Consumes verified evidence packages without searching the web.
  - Acts as an impartial investigator with neutral, objective reasoning.
  - Strictly outputs one of 4 allowed assessments: `Likely Credible`, `Needs Verification`, `Potentially Misleading`, or `Insufficient Information` (never `Fake`, `Real`, `True`, or `False`).
  - Empirical 5-factor confidence calculation (Source count, Consensus, Official presence, Freshness, Completeness).
  - Cross-source conflict detection ensuring disagreements are transparently highlighted.
  - Seamless integration with Gemini 2.5/3.1 preview models + high-fidelity deterministic investigation fallback.
- **Async Concurrency**: Non-blocking asynchronous web search and content scraping with polite timeouts and rate-limiting.
- **Configurable Trusted Domain Registry**: Categorized registry scoring domain reputation across Government, Health, Finance, Tech, Education, and Fact Checkers.

## AI Reasoning API Endpoints
- `POST /api/v1/reasoning/analyze`: Analyzes an evidence package payload and returns the structured `CredibilityReport`.
- `POST /api/v1/reasoning/full-investigation/text`: End-to-end verification and reasoning for raw text.
- `POST /api/v1/reasoning/full-investigation/link`: End-to-end verification and reasoning for news article links.
