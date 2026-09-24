package com.example.feature.chat.repository

import com.example.feature.chat.ChatMessage
import com.example.feature.chat.MessageSender
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChatRepository {
    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = MessageSender.ASSISTANT,
                text = "Hello! I'm VeriLens Assistant. Ask me anything about misleading claims, news fact-checking, or how to verify suspicious content before sharing."
            )
        )
    )
    val messages: Flow<List<ChatMessage>> = _messages.asStateFlow()

    suspend fun sendMessage(userText: String) {
        val current = _messages.value.toMutableList()
        current.add(ChatMessage(sender = MessageSender.USER, text = userText))
        _messages.value = current

        // Assistant response
        val responseText = generateAssistantResponse(userText)
        current.add(ChatMessage(sender = MessageSender.ASSISTANT, text = responseText))
        _messages.value = current.toList()
    }

    private fun generateAssistantResponse(query: String): String {
        val lower = query.lowercase()
        return when {
            lower.contains("how to verify") || lower.contains("check") ->
                "To verify news or images:\n1. Capture or upload a screenshot/photo with VeriLens.\n2. Cross-reference claims with primary fact-checkers like Reuters or PIB.\n3. Check original publication dates and reverse image search."
            lower.contains("scam") || lower.contains("otp") || lower.contains("phishing") ->
                "Scam Prevention Rule: Never share OTPs, passwords, or scan unknown QR codes that promise refunds. Official organizations will never demand urgent payments through personal UPI handles."
            else ->
                "I've analyzed your question against verified fact-checking standards. To get an in-depth cross-examination of a specific claim or headline, try using the Camera or Screenshot verification in the Home tab!"
        }
    }
}
