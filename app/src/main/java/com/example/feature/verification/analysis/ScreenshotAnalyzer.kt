package com.example.feature.verification.analysis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.BuildConfig
import com.example.feature.chat.network.GeminiCandidate
import com.example.feature.chat.network.GeminiContent
import com.example.feature.chat.network.GeminiGenerateRequest
import com.example.feature.chat.network.GeminiInlineData
import com.example.feature.chat.network.GeminiPart
import com.example.feature.chat.network.GeminiRestService
import com.example.feature.verification.model.ExtractedVisualAnalysis
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class ScreenshotAnalyzer(
    private val context: Context
) {
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    private val geminiService: GeminiRestService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiRestService::class.java)
    }

    /**
     * Preprocesses the bitmap: scales if width or height > 1600 to prevent OOM
     * and compresses to JPEG bytes.
     */
    fun preprocessBitmap(bitmap: Bitmap): Pair<Bitmap, ByteArray> {
        val maxDim = 1600
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        val scaledBitmap = if (originalWidth > maxDim || originalHeight > maxDim) {
            val ratio = originalWidth.toFloat() / originalHeight.toFloat()
            val (targetWidth, targetHeight) = if (ratio > 1f) {
                maxDim to (maxDim / ratio).toInt()
            } else {
                (maxDim * ratio).toInt() to maxDim
            }
            Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val jpegBytes = outputStream.toByteArray()
        return scaledBitmap to jpegBytes
    }

    fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Runs ML Kit Text Recognition on-device.
     */
    suspend fun extractOcrText(bitmap: Bitmap): String = withContext(Dispatchers.Default) {
        suspendCancellableCoroutine { continuation ->
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            textRecognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    continuation.resume(visionText.text)
                }
                .addOnFailureListener { e ->
                    // Return empty string on failure rather than crashing
                    continuation.resume("")
                }
        }
    }

    /**
     * Sends screenshot to Gemini Vision to identify:
     * Headline, Logos, Layout, Watermarks, Visible claims, Manipulation indicators.
     */
    suspend fun analyzeWithGeminiVision(jpegBytes: ByteArray, ocrText: String): ExtractedVisualAnalysis =
        withContext(Dispatchers.IO) {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext fallbackLocalVisualAnalysis(ocrText)
            }

            val base64Image = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)

            val prompt = """
                Analyze this screenshot strictly for fact-checking and news verification.
                Visible OCR text:
                $ocrText

                Identify:
                1. Main headline or primary title.
                2. Visible logos, branding, or watermarks.
                3. Visual layout type (e.g. Breaking News Broadcast, Social Media Post, Chat Screenshot, Govt Circular).
                4. Manipulation indicators (e.g. misaligned text, mismatched font weight, artificial timestamp overlay, compression noise).
                5. The primary claim or proposition that should be fact-checked.
                6. Short context notes.

                Respond ONLY in strict JSON format:
                {
                  "headline": "extracted headline",
                  "logos": ["logo1", "logo2"],
                  "layout": "Social Media / News Banner / Chat",
                  "manipulationIndicators": ["none" or list of signs],
                  "primaryClaim": "concise factual claim sentence",
                  "contextNotes": "brief context"
                }
            """.trimIndent()

            try {
                val request = GeminiGenerateRequest(
                    contents = listOf(
                        GeminiContent(
                            parts = listOf(
                                GeminiPart(text = prompt),
                                GeminiPart(
                                    inlineData = GeminiInlineData(
                                        mimeType = "image/jpeg",
                                        data = base64Image
                                    )
                                )
                            )
                        )
                    )
                )

                val response = geminiService.generateContent(apiKey, request)
                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                parseVisualAnalysisJson(rawText, ocrText)
            } catch (e: Exception) {
                fallbackLocalVisualAnalysis(ocrText)
            }
        }

    private fun parseVisualAnalysisJson(raw: String, fallbackOcr: String): ExtractedVisualAnalysis {
        return try {
            val cleaned = raw.replace("```json", "").replace("```", "").trim()
            val adapter = moshi.adapter(ExtractedVisualAnalysis::class.java)
            val parsed = adapter.fromJson(cleaned)
            if (parsed != null && parsed.primaryClaim.isNotBlank()) {
                parsed
            } else {
                fallbackLocalVisualAnalysis(fallbackOcr)
            }
        } catch (e: Exception) {
            fallbackLocalVisualAnalysis(fallbackOcr)
        }
    }

    private fun fallbackLocalVisualAnalysis(ocrText: String): ExtractedVisualAnalysis {
        val lines = ocrText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val headline = lines.firstOrNull { it.length in 10..90 } ?: (lines.firstOrNull() ?: "Visual Claim")
        val isSocial = ocrText.contains("Like", true) || ocrText.contains("Share", true) || ocrText.contains("Retweet", true)
        val isNews = ocrText.contains("Breaking", true) || ocrText.contains("News", true) || ocrText.contains("Report", true)
        val layout = if (isSocial) "Social Media Screenshot" else if (isNews) "News Broadcast / Digital Banner" else "Document / Message Screen"

        val logos = mutableListOf<String>()
        if (ocrText.contains("Twitter", true) || ocrText.contains("X.com", true)) logos.add("Twitter / X")
        if (ocrText.contains("WhatsApp", true)) logos.add("WhatsApp")
        if (ocrText.contains("Facebook", true)) logos.add("Facebook")
        if (ocrText.contains("Instagram", true)) logos.add("Instagram")
        if (ocrText.contains("Government", true) || ocrText.contains("Ministry", true)) logos.add("Official Agency Emblem")

        return ExtractedVisualAnalysis(
            headline = headline,
            logos = logos,
            layout = layout,
            manipulationIndicators = emptyList(),
            primaryClaim = headline,
            contextNotes = "Extracted directly from high-fidelity on-device OCR."
        )
    }
}
