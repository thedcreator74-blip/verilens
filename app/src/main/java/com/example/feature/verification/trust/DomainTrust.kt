package com.example.feature.verification.trust

import com.squareup.moshi.JsonClass
import java.net.URI

@JsonClass(generateAdapter = true)
data class DomainTrust(
    val domain: String,
    val organization: String,
    val trustLevel: String, // Authoritative, Established, Medium, Low, Suspicious
    val isFactChecker: Boolean = false,
    val isGovernment: Boolean = false,
    val notes: String = ""
)

interface DomainTrustResolver {
    fun resolveTrust(urlOrDomain: String): DomainTrust
}

class DomainTrustResolverImpl : DomainTrustResolver {
    private val authoritativeDomains = mapOf(
        "reuters.com" to DomainTrust("reuters.com", "Reuters", "Authoritative", isFactChecker = true),
        "apnews.com" to DomainTrust("apnews.com", "Associated Press", "Authoritative", isFactChecker = true),
        "bbc.com" to DomainTrust("bbc.com", "BBC News", "Authoritative"),
        "bbc.co.uk" to DomainTrust("bbc.co.uk", "BBC News", "Authoritative"),
        "thehindu.com" to DomainTrust("thehindu.com", "The Hindu", "Authoritative"),
        "timesofindia.indiatimes.com" to DomainTrust("timesofindia.indiatimes.com", "Times of India", "Established"),
        "indianexpress.com" to DomainTrust("indianexpress.com", "The Indian Express", "Established"),
        "pib.gov.in" to DomainTrust("pib.gov.in", "PIB Fact Check / Govt of India", "Authoritative", isFactChecker = true, isGovernment = true),
        "altnews.in" to DomainTrust("altnews.in", "Alt News", "Authoritative", isFactChecker = true),
        "boomlive.in" to DomainTrust("boomlive.in", "BOOM Live", "Authoritative", isFactChecker = true),
        "snopes.com" to DomainTrust("snopes.com", "Snopes", "Authoritative", isFactChecker = true),
        "politifact.com" to DomainTrust("politifact.com", "PolitiFact", "Authoritative", isFactChecker = true),
        "who.int" to DomainTrust("who.int", "World Health Organization", "Authoritative", isGovernment = true),
        "cdc.gov" to DomainTrust("cdc.gov", "CDC", "Authoritative", isGovernment = true)
    )

    override fun resolveTrust(urlOrDomain: String): DomainTrust {
        val host = try {
            val formatted = if (!urlOrDomain.startsWith("http://") && !urlOrDomain.startsWith("https://")) {
                "https://$urlOrDomain"
            } else urlOrDomain
            URI(formatted).host?.lowercase()?.removePrefix("www.") ?: urlOrDomain.lowercase()
        } catch (_: Exception) {
            urlOrDomain.lowercase()
        }

        // Direct match
        authoritativeDomains[host]?.let { return it }

        // Partial suffix match (e.g., .gov or .edu)
        if (host.endsWith(".gov") || host.endsWith(".gov.in") || host.endsWith(".mil")) {
            return DomainTrust(host, "Government Authority", "Authoritative", isGovernment = true)
        }
        if (host.endsWith(".edu") || host.endsWith(".ac.in")) {
            return DomainTrust(host, "Educational Institution", "Established")
        }

        for ((domain, trust) in authoritativeDomains) {
            if (host.endsWith(domain)) {
                return trust
            }
        }

        return DomainTrust(host, host, "Medium", notes = "General web source")
    }
}
