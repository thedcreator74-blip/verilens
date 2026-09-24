package com.example.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.database.entities.HistoryEntity
import com.example.data.repository.VerificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val verificationRepository: VerificationRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val filteredHistory: StateFlow<List<HistoryEntity>> = combine(
        verificationRepository.getAllHistory(),
        _searchQuery
    ) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            list.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.originalClaim.contains(query, ignoreCase = true) ||
                        it.verdict.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            verificationRepository.clearHistory()
        }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            verificationRepository.deleteHistory(id)
        }
    }
}
