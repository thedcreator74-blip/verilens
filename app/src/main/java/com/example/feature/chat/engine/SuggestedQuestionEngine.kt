package com.example.feature.chat.engine

import com.example.feature.chat.model.VerificationContext

/**
 * Generates context-sensitive suggested question chips for Ask VeriLens.
 */
object SuggestedQuestionEngine {

    val defaultEducationQuestions = listOf(
        "Explain this report",
        "Summarize evidence",
        "Show official sources",
        "Why this confidence score?",
        "Translate report",
        "Compare sources",
        "Teach me media literacy",
        "How to verify manually"
    )

    fun getSuggestedQuestions(context: VerificationContext?): List<String> {
        if (context == null) {
            return listOf(
                "How does VeriLens verify claims?",
                "Teach me media literacy",
                "How fake screenshots are created",
                "Difference between opinion and fact",
                "How to verify news yourself",
                "Why official sources matter"
            )
        }

        val dynamicList = mutableListOf<String>()
        dynamicList.add("Explain this report")
        dynamicList.add("Summarize the evidence")
        dynamicList.add("Why is confidence only ${context.confidence}%?")
        dynamicList.add("Which official sources were checked?")

        if (context.verdict.equals("MISLEADING", ignoreCase = true)) {
            dynamicList.add("What makes this claim misleading?")
            dynamicList.add("What information is still missing?")
        } else if (context.verdict.equals("QUESTIONABLE", ignoreCase = true)) {
            dynamicList.add("Why is this marked Questionable?")
            dynamicList.add("How do I verify this manually?")
        } else {
            dynamicList.add("Explain like I'm 12")
            dynamicList.add("Can I trust this website?")
        }

        dynamicList.add("Teach me media literacy")
        dynamicList.add("How do I verify this manually?")
        return dynamicList.distinct()
    }
}
