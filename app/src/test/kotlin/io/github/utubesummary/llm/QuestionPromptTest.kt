package io.github.utubesummary.llm

import io.github.utubesummary.model.ChatExchange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class QuestionPromptTest {

    @Test
    fun `content comes first and the question last`() {
        val prompt = buildQuestionPrompt(
            sourceText = "THE TRANSCRIPT",
            history = listOf(ChatExchange("q1", "a1")),
            question = "q2",
        )
        val messages = prompt.messages

        assertEquals(listOf("System", "User", "Assistant", "User"), messages.map { it::class.simpleName })
        assertTrue(messages.first().toString().contains("THE TRANSCRIPT"))
        assertTrue(messages[1].toString().contains("q1"))
        assertTrue(messages[2].toString().contains("a1"))
        assertTrue(messages.last().toString().contains("q2"))
    }

    @Test
    fun `history is capped to the most recent exchanges`() {
        val history = (1..15).map { ChatExchange("q$it", "a$it") }
        val messages = buildQuestionPrompt("text", history, "new").messages

        // system + 10 exchanges (2 messages each) + the new question
        assertEquals(1 + MAX_HISTORY_EXCHANGES * 2 + 1, messages.size)
        assertTrue(messages[1].toString().contains("q6"))
    }
}
