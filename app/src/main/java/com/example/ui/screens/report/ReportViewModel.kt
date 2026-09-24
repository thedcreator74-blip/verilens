package com.example.ui.screens.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.VerificationRepository
import com.example.feature.verification.model.CollectedEvidenceItem
import com.example.feature.verification.model.Verdict
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReportViewModel(
    private val historyId: Long,
    private val verificationRepository: VerificationRepository
) : ViewModel() {

    private val _reportState = MutableStateFlow<ReportDetail?>(null)
    val reportState: StateFlow<ReportDetail?> = _reportState.asStateFlow()

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val evidenceListAdapter = moshi.adapter<List<CollectedEvidenceItem>>(
        Types.newParameterizedType(List::class.java, CollectedEvidenceItem::class.java)
    )
    private val stringListAdapter = moshi.adapter<List<String>>(
        Types.newParameterizedType(List::class.java, String::class.java)
    )

    init {
        loadReport()
    }

    private fun loadReport() {
        viewModelScope.launch {
            val entity = verificationRepository.getHistoryById(historyId)
            if (entity != null) {
                val sources = try {
                    evidenceListAdapter.fromJson(entity.sourcesJson) ?: emptyList()
                } catch (_: Exception) {
                    emptyList()
                }

                val recs = try {
                    stringListAdapter.fromJson(entity.recommendationsJson) ?: emptyList()
                } catch (_: Exception) {
                    emptyList()
                }

                _reportState.value = ReportDetail(
                    id = entity.id,
                    title = entity.title,
                    originalClaim = entity.originalClaim,
                    verifiedInformation = entity.verifiedInformation,
                    inputType = entity.inputType,
                    timestamp = entity.timestamp,
                    credibilityScore = entity.credibilityScore,
                    verdict = Verdict.fromString(entity.verdict),
                    summary = entity.summary,
                    reasoning = entity.reasoning,
                    category = entity.category,
                    sources = sources,
                    recommendations = recs,
                    disclaimer = entity.disclaimer
                )
            }
        }
    }
}
