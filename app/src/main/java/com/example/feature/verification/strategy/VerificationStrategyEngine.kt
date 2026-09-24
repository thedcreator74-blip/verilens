package com.example.feature.verification.strategy

import com.example.feature.verification.model.ClaimCategory

data class VerificationStrategy(
    val category: ClaimCategory,
    val primaryAuthoritativeDomains: List<String>,
    val secondaryNewsDomains: List<String>,
    val domainQuerySuffix: String,
    val requiredEvidenceTypes: List<String>,
    val defaultSearchQueries: (claim: String) -> List<String>
)

object VerificationStrategyEngine {

    /**
     * Detects category using keyword analysis across claim text.
     */
    fun detectCategory(claim: String): ClaimCategory {
        val lower = claim.lowercase()

        return when {
            containsAny(lower, "ministry", "government", "govt", "pib", "circular", "gazette", "parliament", "policy", "scheme", "cabinet", "election", "ration", "aadhaar", "yojana", "subsid", "pm-") ->
                ClaimCategory.GOVERNMENT

            containsAny(lower, "cure", "health", "hospital", "doctor", "virus", "vaccine", "cancer", "diabetes", "disease", "covid", "who", "cdc", "nih", "icmr", "medicine", "pill", "tea", "remedy", "infection") ->
                ClaimCategory.HEALTH

            containsAny(lower, "rbi", "sebi", "bank", "rupees", "dollar", "crypto", "bitcoin", "stock", "nse", "bse", "invest", "interest rate", "inflation", "tax", "loan", "fraud", "scam") ->
                ClaimCategory.FINANCE

            containsAny(lower, "ugc", "aicte", "university", "school", "exam", "neet", "jee", "cbse", "scholarship", "admit card", "college", "degree", "marksheet", "board exam") ->
                ClaimCategory.EDUCATION

            containsAny(lower, "ai", "gemini", "gpt", "apple", "google", "microsoft", "android", "software", "windows", "cyber", "hack", "update", "chip", "nvidia", "app", "algorithm") ->
                ClaimCategory.TECHNOLOGY

            containsAny(lower, "cyclone", "flood", "earthquake", "weather", "temperature", "rain", "monsoon", "meteorol", "tsunami", "forecast", "heatwave") ->
                ClaimCategory.WEATHER

            containsAny(lower, "cricket", "football", "world cup", "olympics", "fifa", "bcci", "icc", "match", "tournament", "medal", "ipl", "trophy") ->
                ClaimCategory.SPORTS

            containsAny(lower, "company", "ceo", "shares", "merger", "acquisition", "revenue", "profit", "layoff", "quarterly", "tata", "reliance", "tesla", "amazon") ->
                ClaimCategory.BUSINESS

            else -> ClaimCategory.GENERAL_NEWS
        }
    }

    /**
     * Provides specialized verification strategy based on category.
     */
    fun getStrategy(category: ClaimCategory): VerificationStrategy {
        return when (category) {
            ClaimCategory.GOVERNMENT -> VerificationStrategy(
                category = category,
                primaryAuthoritativeDomains = listOf("pib.gov.in", "gov.in", "nic.in", "india.gov.in"),
                secondaryNewsDomains = listOf("reuters.com", "apnews.com", "thehindu.com", "indianexpress.com", "bbc.com"),
                domainQuerySuffix = "site:gov.in OR site:pib.gov.in",
                requiredEvidenceTypes = listOf("Official Government Press Release", "Gazette Notification", "Authorized Spokesperson Statement"),
                defaultSearchQueries = { claim ->
                    val cleanWords = extractCoreKeywords(claim)
                    listOf(
                        "$cleanWords fact check PIB",
                        "$cleanWords site:pib.gov.in OR site:gov.in",
                        "$cleanWords government official notification"
                    )
                }
            )

            ClaimCategory.HEALTH -> VerificationStrategy(
                category = category,
                primaryAuthoritativeDomains = listOf("who.int", "cdc.gov", "nih.gov", "ncbi.nlm.nih.gov", "pubmed.ncbi.nlm.nih.gov", "icmr.gov.in"),
                secondaryNewsDomains = listOf("healthfeedback.org", "reuters.com", "apnews.com", "bmj.com", "thelancet.com"),
                domainQuerySuffix = "site:who.int OR site:nih.gov OR site:cdc.gov",
                requiredEvidenceTypes = listOf("Clinical Trial Data", "Peer-Reviewed Medical Journal", "WHO/CDC Advisory"),
                defaultSearchQueries = { claim ->
                    val cleanWords = extractCoreKeywords(claim)
                    listOf(
                        "$cleanWords fact check WHO medical",
                        "$cleanWords site:who.int OR site:cdc.gov OR site:nih.gov",
                        "$cleanWords clinical study evidence health"
                    )
                }
            )

            ClaimCategory.FINANCE -> VerificationStrategy(
                category = category,
                primaryAuthoritativeDomains = listOf("rbi.org.in", "sebi.gov.in", "nseindia.com", "bseindia.com", "sec.gov"),
                secondaryNewsDomains = listOf("bloomberg.com", "reuters.com", "financialtimes.com", "economictimes.indiatimes.com"),
                domainQuerySuffix = "site:rbi.org.in OR site:sebi.gov.in",
                requiredEvidenceTypes = listOf("Regulatory Notification", "Stock Exchange Filing", "Central Bank Circular"),
                defaultSearchQueries = { claim ->
                    val cleanWords = extractCoreKeywords(claim)
                    listOf(
                        "$cleanWords RBI circular SEBI notification",
                        "$cleanWords site:rbi.org.in OR site:sebi.gov.in",
                        "$cleanWords official financial clarification"
                    )
                }
            )

            ClaimCategory.EDUCATION -> VerificationStrategy(
                category = category,
                primaryAuthoritativeDomains = listOf("ugc.gov.in", "aicte-india.org", "education.gov.in", "nta.ac.in", "cbse.gov.in"),
                secondaryNewsDomains = listOf("ndtv.com/education", "indianexpress.com/education", "thehindu.com"),
                domainQuerySuffix = "site:ugc.gov.in OR site:aicte-india.org OR site:education.gov.in",
                requiredEvidenceTypes = listOf("University Grants Commission Circular", "Official Exam Board Notice", "Ministry Release"),
                defaultSearchQueries = { claim ->
                    val cleanWords = extractCoreKeywords(claim)
                    listOf(
                        "$cleanWords UGC official notice AICTE",
                        "$cleanWords site:education.gov.in OR site:ugc.gov.in",
                        "$cleanWords scholarship exam notification fact check"
                    )
                }
            )

            ClaimCategory.TECHNOLOGY -> VerificationStrategy(
                category = category,
                primaryAuthoritativeDomains = listOf("developer.android.com", "microsoft.com", "apple.com", "googleblog.com", "github.com"),
                secondaryNewsDomains = listOf("arstechnica.com", "theverge.com", "techcrunch.com", "wired.com", "reuters.com"),
                domainQuerySuffix = "site:arstechnica.com OR site:theverge.com",
                requiredEvidenceTypes = listOf("Official Release Notes", "Engineering Blog Post", "CVE Security Advisory"),
                defaultSearchQueries = { claim ->
                    val cleanWords = extractCoreKeywords(claim)
                    listOf(
                        "$cleanWords official documentation announcement",
                        "$cleanWords tech fact check security advisory",
                        "$cleanWords release notes update"
                    )
                }
            )

            ClaimCategory.WEATHER -> VerificationStrategy(
                category = category,
                primaryAuthoritativeDomains = listOf("mausam.imd.gov.in", "noaa.gov", "wmo.int", "weather.gov"),
                secondaryNewsDomains = listOf("reuters.com", "apnews.com", "bbc.com/weather"),
                domainQuerySuffix = "site:imd.gov.in OR site:noaa.gov",
                requiredEvidenceTypes = listOf("National Meteorological Bulletin", "Satellite Advisory", "Civil Defense Alert"),
                defaultSearchQueries = { claim ->
                    val cleanWords = extractCoreKeywords(claim)
                    listOf(
                        "$cleanWords meteorological department bulletin IMD NOAA",
                        "$cleanWords official weather alert warning fact check",
                        "$cleanWords site:imd.gov.in OR site:noaa.gov"
                    )
                }
            )

            ClaimCategory.SPORTS -> VerificationStrategy(
                category = category,
                primaryAuthoritativeDomains = listOf("icc-cricket.com", "fifa.com", "olympics.com", "bcci.tv"),
                secondaryNewsDomains = listOf("espncricinfo.com", "bbc.com/sport", "reuters.com/lifestyle/sports"),
                domainQuerySuffix = "site:icc-cricket.com OR site:fifa.com OR site:olympics.com",
                requiredEvidenceTypes = listOf("Official Federation Press Release", "Match Scorecard Archive", "Regulatory Ruling"),
                defaultSearchQueries = { claim ->
                    val cleanWords = extractCoreKeywords(claim)
                    listOf(
                        "$cleanWords official statement federation ICC FIFA",
                        "$cleanWords sports fact check match outcome",
                        "$cleanWords official announcement"
                    )
                }
            )

            ClaimCategory.BUSINESS -> VerificationStrategy(
                category = category,
                primaryAuthoritativeDomains = listOf("sec.gov", "mca.gov.in", "investor.gov"),
                secondaryNewsDomains = listOf("reuters.com", "bloomberg.com", "wsj.com", "ft.com"),
                domainQuerySuffix = "site:reuters.com OR site:bloomberg.com",
                requiredEvidenceTypes = listOf("SEC / Regulatory Filing", "Audited Financial Report", "Official Press Wire"),
                defaultSearchQueries = { claim ->
                    val cleanWords = extractCoreKeywords(claim)
                    listOf(
                        "$cleanWords official press release annual report",
                        "$cleanWords SEC filing company announcement",
                        "$cleanWords business fact check Reuters"
                    )
                }
            )

            ClaimCategory.GENERAL_NEWS -> VerificationStrategy(
                category = category,
                primaryAuthoritativeDomains = listOf("reuters.com", "apnews.com", "afp.com", "bbc.com", "altnews.in", "boomlive.in", "snopes.com", "politifact.com"),
                secondaryNewsDomains = listOf("thehindu.com", "indianexpress.com", "ndtv.com"),
                domainQuerySuffix = "site:reuters.com OR site:apnews.com OR site:snopes.com",
                requiredEvidenceTypes = listOf("Wire Service Dispatch", "IFCN Fact-Check Report", "Eyewitness Ground Investigation"),
                defaultSearchQueries = { claim ->
                    val cleanWords = extractCoreKeywords(claim)
                    listOf(
                        "$cleanWords fact check Reuters Snopes",
                        "$cleanWords fake debunk verified news",
                        "$cleanWords site:apnews.com OR site:reuters.com"
                    )
                }
            )
        }
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { text.contains(it) }
    }

    private fun extractCoreKeywords(claim: String): String {
        val stopWords = setOf("a", "an", "the", "in", "on", "at", "to", "for", "of", "with", "by", "from",
            "is", "are", "was", "were", "be", "been", "has", "have", "had", "this", "that", "these", "those")
        val words = claim.split(Regex("[\\s,;:.!?-]+"))
            .filter { it.length > 2 && !stopWords.contains(it.lowercase()) }
            .take(6)
        return words.joinToString(" ")
    }
}
