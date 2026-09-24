package com.example.feature.verification.claim

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NormalizedClaim(
    val primaryClaim: String,
    val coreSubject: String = "",
    val keyEntities: List<String> = emptyList(),
    val claimType: String = "FACTUAL", // FACTUAL, OPINION, PREDICTION, SATIRE, SCAM
    val searchQueries: List<String> = emptyList(),
    val isCheckable: Boolean = true
)

class ClaimNormalizer {
    fun normalize(rawText: String, extractedUrl: String? = null): NormalizedClaim {
        val clean = rawText.trim().replace("\n", " ").take(500)
        if (clean.isBlank()) {
            return NormalizedClaim(
                primaryClaim = "No readable claim detected.",
                isCheckable = false
            )
        }

        // Basic heuristic extraction
        val words = clean.split(" ").filter { it.isNotBlank() }
        val queries = mutableListOf<String>()
        queries.add(clean.take(120))
        if (extractedUrl != null) {
            queries.add("site:$extractedUrl")
        }

        return NormalizedClaim(
            primaryClaim = clean,
            coreSubject = words.take(5).joinToString(" "),
            keyEntities = words.filter { it.length > 4 && it[0].isUpperCase() }.distinct().take(4),
            claimType = if (clean.contains("urgent", ignoreCase = true) || clean.contains("send money", ignoreCase = true) || clean.contains("won prize", ignoreCase = true) || clean.contains("click here", ignoreCase = true)) "SCAM" else "FACTUAL",
            searchQueries = queries,
            isCheckable = words.size >= 2
        )
    }
}
