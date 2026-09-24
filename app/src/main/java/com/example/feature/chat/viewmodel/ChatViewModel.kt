package com.example.feature.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feature.chat.engine.SuggestedQuestionEngine
import com.example.feature.chat.model.ChatMessage
import com.example.feature.chat.model.ChatSource
import com.example.feature.chat.model.MessageSender
import com.example.feature.chat.model.VerificationContext
import com.example.feature.chat.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatUiState(
    val context: VerificationContext? = null,
    val messages: List<ChatMessage> = emptyList(),
    val suggestedQuestions: List<String> = emptyList(),
    val inputText: String = "",
    val isAnalyzing: Boolean = false,
    val errorMessage: String? = null
)

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val initialReportId: Long? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var activeStreamJob: Job? = null

    init {
        loadVerificationContext(initialReportId)
    }

    fun loadVerificationContext(preferredId: Long?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true) }
            val context = chatRepository.getLatestVerificationContext(preferredId)
            val suggested = SuggestedQuestionEngine.getSuggestedQuestions(context)

            val welcomeMessage = if (context != null) {
                ChatMessage(
                    id = "welcome_${System.currentTimeMillis()}",
                    sender = MessageSender.ASSISTANT,
                    text = "Welcome to **Ask VeriLens**. I have loaded your verification report for **\"${context.originalClaim}\"** (${context.verdict}, ${context.confidence}% Credibility).\n\nAsk me any question regarding the evidence, sources checked, why this confidence score was determined, or how to verify manually.",
                    sources = context.trustedSources,
                    isStreaming = false
                )
            } else {
                ChatMessage(
                    id = "welcome_general",
                    sender = MessageSender.ASSISTANT,
                    text = "Welcome to **Ask VeriLens**. I am your Evidence & Media Literacy Assistant. I can help you interpret verification reports, examine trusted public sources, spot manipulated media, or learn how to manually cross-reference claims.",
                    isStreaming = false
                )
            }

            _uiState.update {
                it.copy(
                    context = context,
                    suggestedQuestions = suggested,
                    messages = listOf(welcomeMessage),
                    isAnalyzing = false
                )
            }
        }
    }

    fun onInputTextChanged(newText: String) {
        _uiState.update { it.copy(inputText = newText) }
    }

    fun sendMessage(query: String? = null) {
        val textToSend = (query ?: _uiState.value.inputText).trim()
        if (textToSend.isBlank() || _uiState.value.isAnalyzing) return

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = MessageSender.USER,
            text = textToSend
        )

        val updatedMessages = _uiState.value.messages + userMessage
        _uiState.update {
            it.copy(
                inputText = "",
                messages = updatedMessages,
                isAnalyzing = true,
                errorMessage = null
            )
        }

        activeStreamJob?.cancel()
        activeStreamJob = viewModelScope.launch {
            try {
                chatRepository.askQuestion(
                    query = textToSend,
                    context = _uiState.value.context,
                    history = updatedMessages
                ).collect { assistantChunk ->
                    _uiState.update { current ->
                        val existingIndex = current.messages.indexOfFirst { it.id == assistantChunk.id }
                        val newMessages = if (existingIndex >= 0) {
                            current.messages.toMutableList().apply {
                                set(existingIndex, assistantChunk)
                            }
                        } else {
                            current.messages + assistantChunk
                        }
                        current.copy(
                            messages = newMessages,
                            isAnalyzing = assistantChunk.isStreaming
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        errorMessage = "Unable to complete request. Please try again."
                    )
                }
            } finally {
                _uiState.update { it.copy(isAnalyzing = false) }
            }
        }
    }

    fun clearConversation() {
        val context = _uiState.value.context
        val resetMessage = ChatMessage(
            id = "reset_${System.currentTimeMillis()}",
            sender = MessageSender.ASSISTANT,
            text = "Conversation cleared. Feel free to ask another question about ${if (context != null) "\"${context.originalClaim}\"" else "verification and media literacy"}.",
            sources = context?.trustedSources ?: emptyList()
        )
        _uiState.update {
            it.copy(
                messages = listOf(resetMessage),
                errorMessage = null
            )
        }
    }
}
