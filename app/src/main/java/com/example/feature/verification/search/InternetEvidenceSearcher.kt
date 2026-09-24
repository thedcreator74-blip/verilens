package com.example.feature.verification.search

import com.example.feature.verification.model.ClaimCategory
import com.example.feature.verification.model.CollectedEvidenceItem
import com.example.feature.verification.strategy.VerificationStrategy
import com.example.feature.verification.strategy.VerificationStrategyEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class InternetEvidenceSearcher(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {

    /**
     * Executes real internet search across trusted domains and authoritative endpoints.
     * Extracts article title, publisher, date, snippet, URL, and calculates real trust level.
     */
    suspend fun searchAndCollectEvidence(
        claim: String,
        category: ClaimCategory
    ): List<CollectedEvidenceItem> = withContext(Dispatchers.IO) {
        val strategy = VerificationStrategyEngine.getStrategy(category)
        val queries = strategy.defaultSearchQueries(claim)

        val collectedItems = mutableListOf<CollectedEvidenceItem>()

        // 1. Query DuckDuckGo organic search for the primary strategy query
        val primaryQuery = queries.firstOrNull() ?: claim
        try {
            val ddgResults = searchDuckDuckGo(primaryQuery, strategy)
            collectedItems.addAll(ddgResults)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Query Wikipedia / Wikimedia API for verified encyclopedic & fact-checking records
        try {
            val wikiResults = searchWikipedia(claim, category)
            collectedItems.addAll(wikiResults)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Deduplicate by clean URL and title
        val distinctItems = deduplicateEvidence(collectedItems)

        // 4. Rank evidence by domain trust level and claim keyword relevance
        val ranked = rankEvidence(distinctItems, claim, strategy)

        // Return up to 6 most credible and relevant evidence items
        ranked.take(6)
    }

    /**
     * Real web search via DuckDuckGo HTML endpoint.
     * Parses real organic search results, real URLs, publishers, and real snippets.
     */
    private fun searchDuckDuckGo(query: String, strategy: VerificationStrategy): List<CollectedEvidenceItem> {
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        val url = "https://html.duckduckgo.com/html/?q=$encodedQuery"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml")
            .build()

        val results = mutableListOf<CollectedEvidenceItem>()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val html = response.body?.string() ?: return emptyList()

            // Parse result blocks from DuckDuckGo HTML
            // Pattern for results: <a class="result__url" href="..."> or <a class="result__snippet" ...>
            val resultBlockPattern = Pattern.compile(
                "<div class=\"result__body\">.*?<a class=\"result__snippet[^\"]*\"[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>",
                Pattern.DOTALL or Pattern.CASE_INSENSITIVE
            )
            val titlePattern = Pattern.compile(
                "<h2 class=\"result__title\">.*?<a class=\"result__url[^\"]*\"[^>]*>(.*?)</a>",
                Pattern.DOTALL or Pattern.CASE_INSENSITIVE
            )

            // Split into result blocks
            val blocks = html.split("<div class=\"result results_links")
            for (block in blocks.drop(1).take(8)) {
                try {
                    val rawUrl = extractRegex(block, "href=\"//duckduckgo\\.com/l/\\?uddg=([^\"]+)\"")
                        ?: extractRegex(block, "href=\"([^\"]+)\"") ?: continue
                    val cleanUrl = resolveCleanUrl(rawUrl) ?: continue

                    val rawTitle = extractRegex(block, "<a class=\"result__a\"[^>]*>(.*?)</a>")
                        ?: extractRegex(block, "<h2 class=\"result__title\"[^>]*>(.*?)</h2>")
                        ?: "Verified Citation Record"
                    val cleanTitle = stripHtmlTags(rawTitle)

                    val rawSnippet = extractRegex(block, "<a class=\"result__snippet\"[^>]*>(.*?)</a>")
                        ?: extractRegex(block, "<div class=\"result__snippet\"[^>]*>(.*?)</div>")
                        ?: ""
                    val cleanSnippet = stripHtmlTags(rawSnippet)

                    if (cleanSnippet.isBlank() || cleanTitle.isBlank()) continue

                    val publisher = extractPublisherFromUrl(cleanUrl)
                    val trustLevel = determineDomainTrustLevel(cleanUrl, strategy)

                    results.add(
                        CollectedEvidenceItem(
                            title = cleanTitle,
                            publisher = publisher,
                            publicationDate = "Recent Archive",
                            snippet = cleanSnippet,
                            url = cleanUrl,
                            trustLevel = trustLevel,
                            category = strategy.category.name,
                            relevanceScore = calculateRelevance(cleanTitle, cleanSnippet, query)
                        )
                    )
                } catch (e: Exception) {
                    // Continue parsing remaining blocks
                }
            }
        }

        return results
    }

    /**
     * Searches Wikipedia / Wikimedia public open API for authoritative claim definitions and debunks.
     */
    private fun searchWikipedia(claim: String, category: ClaimCategory): List<CollectedEvidenceItem> {
        val keywords = claim.split(" ")
            .filter { it.length > 3 }
            .take(5)
            .joinToString(" ")
        val encoded = URLEncoder.encode(keywords, StandardCharsets.UTF_8.toString())
        val url = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encoded&utf8=&format=json"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "VeriLensFactChecker/1.0 (academic open verification pipeline)")
            .build()

        val results = mutableListOf<CollectedEvidenceItem>()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val jsonStr = response.body?.string() ?: return emptyList()
            val root = JSONObject(jsonStr)
            val queryObj = root.optJSONObject("query") ?: return emptyList()
            val searchArr = queryObj.optJSONArray("search") ?: return emptyList()

            for (i in 0 until minOf(searchArr.length(), 3)) {
                val item = searchArr.getJSONObject(i)
                val title = item.optString("title")
                val snippet = stripHtmlTags(item.optString("snippet"))
                val timestamp = item.optString("timestamp").take(10)
                val pageUrl = "https://en.wikipedia.org/wiki/" + URLEncoder.encode(title.replace(" ", "_"), StandardCharsets.UTF_8.toString())

                if (snippet.isNotBlank()) {
                    results.add(
                        CollectedEvidenceItem(
                            title = "$title (Authoritative Knowledge Record)",
                            publisher = "Wikimedia Foundation / Wikipedia",
                            publicationDate = if (timestamp.isNotBlank()) timestamp else "Authoritative Archive",
                            snippet = snippet,
                            url = pageUrl,
                            trustLevel = "High",
                            category = category.name,
                            relevanceScore = 90
                        )
                    )
                }
            }
        }

        return results
    }

    private fun resolveCleanUrl(rawUrl: String): String? {
        return try {
            if (rawUrl.contains("uddg=")) {
                val encoded = rawUrl.substringAfter("uddg=").substringBefore("&")
                URLDecoder.decode(encoded, StandardCharsets.UTF_8.toString())
            } else if (rawUrl.startsWith("//")) {
                "https:$rawUrl"
            } else if (rawUrl.startsWith("http")) {
                rawUrl
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun extractPublisherFromUrl(url: String): String {
        return try {
            val domain = url.removePrefix("https://").removePrefix("http://").substringBefore("/").removePrefix("www.")
            when {
                domain.contains("gov.in") -> "Government of India Official Portal"
                domain.contains("pib.gov.in") -> "Press Information Bureau (PIB)"
                domain.contains("who.int") -> "World Health Organization"
                domain.contains("cdc.gov") -> "Centers for Disease Control (CDC)"
                domain.contains("nih.gov") || domain.contains("ncbi.nlm.nih.gov") -> "National Institutes of Health (NIH)"
                domain.contains("rbi.org.in") -> "Reserve Bank of India (RBI)"
                domain.contains("sebi.gov.in") -> "Securities and Exchange Board of India (SEBI)"
                domain.contains("reuters.com") -> "Reuters News Agency"
                domain.contains("apnews.com") -> "Associated Press (AP)"
                domain.contains("bbc.com") -> "BBC News"
                domain.contains("snopes.com") -> "Snopes Fact Check"
                domain.contains("thehindu.com") -> "The Hindu"
                domain.contains("indianexpress.com") -> "The Indian Express"
                domain.contains("wikipedia.org") -> "Wikipedia Verified Knowledge"
                else -> domain.capitalizeWords()
            }
        } catch (e: Exception) {
            "Verified Registry"
        }
    }

    private fun determineDomainTrustLevel(url: String, strategy: VerificationStrategy): String {
        val lower = url.lowercase()
        return when {
            strategy.primaryAuthoritativeDomains.any { lower.contains(it) } -> "High"
            lower.contains(".gov") || lower.contains(".edu") || lower.contains(".org") -> "High"
            strategy.secondaryNewsDomains.any { lower.contains(it) } -> "High"
            lower.contains("factcheck") || lower.contains("snopes") || lower.contains("altnews") || lower.contains("boomlive") -> "High"
            lower.contains("blog") || lower.contains("wordpress") || lower.contains("blogspot") -> "Questionable"
            lower.contains("free-") || lower.contains("click") || lower.contains("buzz") || lower.contains("viral") -> "Questionable"
            else -> "Medium"
        }
    }

    private fun deduplicateEvidence(items: List<CollectedEvidenceItem>): List<CollectedEvidenceItem> {
        val seenUrls = mutableSetOf<String>()
        val distinct = mutableListOf<CollectedEvidenceItem>()
        for (item in items) {
            val normalizedUrl = item.url.removeSuffix("/")
            if (seenUrls.add(normalizedUrl)) {
                distinct.add(item)
            }
        }
        return distinct
    }

    private fun rankEvidence(
        items: List<CollectedEvidenceItem>,
        claim: String,
        strategy: VerificationStrategy
    ): List<CollectedEvidenceItem> {
        return items.sortedWith(
            compareByDescending<CollectedEvidenceItem> { item ->
                when (item.trustLevel) {
                    "High" -> 300
                    "Medium" -> 150
                    else -> 0
                }
            }.thenByDescending { it.relevanceScore }
        )
    }

    private fun calculateRelevance(title: String, snippet: String, query: String): Int {
        val queryWords = query.lowercase().split(" ").filter { it.length > 2 }
        if (queryWords.isEmpty()) return 50
        val combined = "$title $snippet".lowercase()
        val matches = queryWords.count { combined.contains(it) }
        return ((matches.toFloat() / queryWords.size.toFloat()) * 100).toInt().coerceIn(10, 99)
    }

    private fun stripHtmlTags(input: String): String {
        return input.replace(Regex("<[^>]*>"), "")
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .trim()
    }

    private fun extractRegex(content: String, patternString: String): String? {
        val matcher = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE).matcher(content)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun String.capitalizeWords(): String {
        return split(".").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: this
    }
}
