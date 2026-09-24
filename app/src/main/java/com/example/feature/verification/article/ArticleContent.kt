package com.example.feature.verification.article

import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ArticleContent(
    val url: String,
    val title: String,
    val body: String,
    val author: String? = null,
    val publishedDate: String? = null,
    val isExtractedSuccessfully: Boolean = true
)

interface ArticleContentExtractor {
    suspend fun extract(url: String): ArticleContent
}

class ArticleContentExtractorImpl(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) : ArticleContentExtractor {

    override suspend fun extract(url: String): ArticleContent = withContext(Dispatchers.IO) {
        try {
            val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else url

            val request = Request.Builder()
                .url(formattedUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: ""

            // Simple title extraction
            val titleMatcher = Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE).find(html)
            val title = titleMatcher?.groupValues?.get(1)?.trim() ?: "Web Article"

            // Simple text extraction (strip tags)
            val cleanBody = html
                .replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
                .replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
                .replace(Regex("<[^>]+>"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
                .take(3000)

            ArticleContent(
                url = url,
                title = title,
                body = cleanBody,
                isExtractedSuccessfully = cleanBody.isNotBlank()
            )
        } catch (_: Exception) {
            ArticleContent(
                url = url,
                title = "External Web Source",
                body = "Unable to preview full article content. Proceeding with search metadata.",
                isExtractedSuccessfully = false
            )
        }
    }
}
