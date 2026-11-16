package io.github.utubesummary.llm

import ai.koog.prompt.message.Message
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

        assertTrue(messages.first() is Message.System)
        assertTrue(messages.first().content.contains("THE TRANSCRIPT"))
        assertEquals(listOf("q1", "a1", "q2"), messages.drop(1).map { it.content })
        assertTrue(messages.last() is Message.User)
    }

    @Test
    fun `history is capped to the most recent exchanges`() {
        val history = (1..15).map { ChatExchange("q$it", "a$it") }
        val messages = buildQuestionPrompt("text", history, "new").messages

        // system + 10 exchanges (2 messages each) + the new question
        assertEquals(1 + MAX_HISTORY_EXCHANGES * 2 + 1, messages.size)
        assertEquals("q6", messages[1].content)
    }
}
