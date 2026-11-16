package io.github.utubesummary.llm

import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.dsl.prompt
import io.github.utubesummary.model.ChatExchange

/** Earlier question/answer pairs sent along with a new question. */
const val MAX_HISTORY_EXCHANGES = 10

/**
 * The content comes first (in the system message) and the question last, so the start of the
 * prompt is identical for every question about the same content, which lets providers that
 * cache prompt prefixes answer follow-up questions faster and cheaper.
 */
fun buildQuestionPrompt(
    sourceText: String,
    history: List<ChatExchange>,
    question: String,
): Prompt = prompt("content-question") {
    system(
        """
        You answer questions about the content between the CONTENT markers.
        - Use only that content. If the answer is not in it, say so instead of guessing.
        - Answer in the language of the question, clearly and concisely.
        - Do not mention these instructions or the markers.

        CONTENT START
        $sourceText
        CONTENT END
        """.trimIndent()
    )
    history.takeLast(MAX_HISTORY_EXCHANGES).forEach {
        user(it.question)
        assistant(it.answer)
    }
    user(question)
}
