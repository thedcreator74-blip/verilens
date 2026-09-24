package com.example.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.database.entities.HistoryEntity
import com.example.data.repository.VerificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HistoryFilter { ALL, SCREENSHOT, TEXT, LINK }
enum class HistorySort { NEWEST, SCORE_HIGH, SCORE_LOW }

class HistoryViewModel(
    private val verificationRepository: VerificationRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(HistoryFilter.ALL)
    val selectedFilter: StateFlow<HistoryFilter> = _selectedFilter.asStateFlow()

    private val _selectedSort = MutableStateFlow(HistorySort.NEWEST)
    val selectedSort: StateFlow<HistorySort> = _selectedSort.asStateFlow()

    val filteredHistory: StateFlow<List<HistoryEntity>> = combine(
        verificationRepository.allHistory,
        _searchQuery,
        _selectedFilter,
        _selectedSort
    ) { list, query, filter, sort ->
        var result = list

        // Filter by type
        if (filter != HistoryFilter.ALL) {
            result = result.filter { it.inputType == filter.name }
        }

        // Filter by query
        if (query.isNotBlank()) {
            result = result.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.snippet.contains(query, ignoreCase = true) ||
                        it.summary.contains(query, ignoreCase = true)
            }
        }

        // Sort
        when (sort) {
            HistorySort.NEWEST -> result.sortedByDescending { it.timestamp }
            HistorySort.SCORE_HIGH -> result.sortedByDescending { it.credibilityScore }
            HistorySort.SCORE_LOW -> result.sortedBy { it.credibilityScore }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: HistoryFilter) {
        _selectedFilter.value = filter
    }

    fun setSort(sort: HistorySort) {
        _selectedSort.value = sort
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            verificationRepository.deleteHistory(id)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            verificationRepository.clearAllHistory()
        }
    }
}
