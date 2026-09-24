package com.example.feature.verification.claim

import java.util.regex.Pattern

object ClaimExtractor {

    // Regex patterns for common UI noise
    private val TIMESTAMP_PATTERN = Pattern.compile("^\\d{1,2}:\\d{2}(\\s?[AaPp][Mm])?$", Pattern.CASE_INSENSITIVE)
    private val BATTERY_OR_NETWORK_PATTERN = Pattern.compile("^(100%|\\d{1,2}%|LTE|5G|4G|3G|VoLTE|WiFi|Wi-Fi|No SIM|SOS)$", Pattern.CASE_INSENSITIVE)
    private val HASHTAG_PATTERN = Pattern.compile("#\\w+")
    private val URL_PATTERN = Pattern.compile("https?://\\S+|www\\.\\S+")

    private val NOISE_EXACT_WORDS = setOf(
        "breaking news", "breaking", "exclusive", "update", "urgent",
        "click here", "tap here", "read more", "swipe up", "see more",
        "like", "comment", "share", "retweet", "repost", "save", "bookmark",
        "follow", "following", "subscribe", "sponsored", "promoted", "ad",
        "advertisement", "views", "replies", "comments", "home", "search",
        "notifications", "messages", "profile", "settings", "menu", "back",
        "forward", "cancel", "ok", "submit", "login", "sign up", "download",
        "install", "today", "yesterday", "just now", "hours ago", "minutes ago",
        "trending", "foryou", "explore", "dm for credit", "link in bio", "dm"
    )

    /**
     * Extracts the single primary claim from raw OCR text and visual observations.
     * Filters out UI noise, status bar metrics, button labels, repeated hashtags, and ads.
     */
    fun extractPrimaryClaim(rawOcr: String, visualHeadline: String, visualClaim: String): String {
        // If Gemini Vision already isolated a clean claim that is not pure noise, use it as candidate
        if (visualClaim.isNotBlank() && !isNoiseLine(visualClaim) && visualClaim.length in 15..280) {
            return cleanClaimString(visualClaim)
        }
        if (visualHeadline.isNotBlank() && !isNoiseLine(visualHeadline) && visualHeadline.length in 15..280) {
            return cleanClaimString(visualHeadline)
        }

        // Otherwise filter lines from raw OCR
        val lines = rawOcr.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val candidateSentences = mutableListOf<String>()

        for (line in lines) {
            if (isNoiseLine(line)) continue

            // Strip hashtags and URLs
            var cleaned = HASHTAG_PATTERN.matcher(line).replaceAll("").trim()
            cleaned = URL_PATTERN.matcher(cleaned).replaceAll("").trim()
            cleaned = cleaned.replace(Regex("^(BREAKING NEWS|UPDATE|ALERT|URGENT)[:\\s-]*", RegexOption.IGNORE_CASE), "").trim()

            if (cleaned.length >= 15 && cleaned.any { it.isLetter() }) {
                candidateSentences.add(cleaned)
            }
        }

        if (candidateSentences.isEmpty()) {
            return cleanClaimString(rawOcr.take(150))
        }

        // Score sentences by factual proposition keywords (e.g. verbs, numbers, entities)
        val bestCandidate = candidateSentences.maxByOrNull { sentence ->
            scoreProposition(sentence)
        } ?: candidateSentences.first()

        return cleanClaimString(bestCandidate)
    }

    private fun isNoiseLine(line: String): Boolean {
        val trimmed = line.trim()
        val lower = trimmed.lowercase()

        if (trimmed.length < 3) return true
        if (TIMESTAMP_PATTERN.matcher(trimmed).matches()) return true
        if (BATTERY_OR_NETWORK_PATTERN.matcher(trimmed).matches()) return true
        if (NOISE_EXACT_WORDS.contains(lower)) return true

        // Social media counts (e.g., "12.4K likes", "340 comments", "1.2M views")
        if (lower.matches(Regex("^[\\d.,]+[kmb]?\\s*(likes|views|comments|shares|retweets|reposts|replies|followers|following)$"))) {
            return true
        }

        // Pure hashtags or emojis
        if (trimmed.all { it == '#' || it.isWhitespace() }) return true
        if (trimmed.all { !it.isLetterOrDigit() }) return true

        return false
    }

    private fun scoreProposition(sentence: String): Int {
        var score = sentence.length.coerceAtMost(100)
        val lower = sentence.lowercase()

        // Give priority to action verbs and announcement indicators
        val factMarkers = listOf("announced", "approved", "launched", "confirmed", "gives", "bans", "releases",
            "passed", "mandates", "reported", "warning", "vaccine", "government", "ministry", "hospital",
            "rupees", "dollar", "percent", "%", "₹", "$")

        for (marker in factMarkers) {
            if (lower.contains(marker)) {
                score += 25
            }
        }

        // Penalize clickbait phrases
        val clickbait = listOf("click here", "you won't believe", "shocking", "viral video", "link in bio")
        for (cb in clickbait) {
            if (lower.contains(cb)) {
                score -= 40
            }
        }

        return score
    }

    private fun cleanClaimString(claim: String): String {
        var result = claim.trim()
        // Remove leading noise tokens like "BREAKING NEWS:" or "ALERT -"
        result = result.replace(Regex("^(BREAKING NEWS|UPDATE|ALERT|URGENT)[:\\s-]*", RegexOption.IGNORE_CASE), "")
        // Remove trailing "Click Here" or "Read More"
        result = result.replace(Regex("(?i)(click here|read more|swipe up|link in bio)[.!]?$"), "")
        result = result.trim()

        // Capitalize first letter
        return if (result.isNotEmpty()) {
            result.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } else {
            "General Claim"
        }
    }
}
