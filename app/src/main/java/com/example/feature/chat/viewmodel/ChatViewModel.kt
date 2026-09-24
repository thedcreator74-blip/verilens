package com.example.feature.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feature.chat.ChatMessage
import com.example.feature.chat.repository.ChatRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(private val chatRepository: ChatRepository) : ViewModel() {
    val messages: StateFlow<List<ChatMessage>> = chatRepository.messages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            chatRepository.sendMessage(text)
        }
    }
}
