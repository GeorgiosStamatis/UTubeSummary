package io.github.utubesummary.llm

import io.github.utubesummary.llm.tools.extractAuthorFromUrl
import io.github.utubesummary.llm.tools.extractTitleFromUrl
import io.github.utubesummary.llm.tools.isUnknownAuthor
import io.github.utubesummary.llm.tools.isValidTitle

private const val TEXT_INPUT_TITLE = "Text Input"

/** The title and author shown for a summary; both are empty when nothing usable is known. */
data class TitleAndAuthor(val title: String, val author: String)

/**
 * Chooses the title and author of a summary. The model's answer wins when it is usable, then what
 * the extraction tool found, then a guess from the URL. Placeholders such as "Text Input",
 * "Just a moment..." or "Unknown" are never shown.
 */
fun resolveTitleAndAuthor(
    structuredTitle: String?,
    structuredAuthor: String?,
    extractedTitle: String,
    extractedAuthor: String,
    sourceLink: String?,
): TitleAndAuthor {
    val title = structuredTitle?.takeIf { isValidTitle(it) }
        ?: extractedTitle.takeIf { isValidTitle(it) && !it.equals(TEXT_INPUT_TITLE, ignoreCase = true) }
        ?: sourceLink?.let { extractTitleFromUrl(it) }?.takeIf { isValidTitle(it) }
        ?: extractedTitle.takeIf { it.isNotBlank() && !it.equals(TEXT_INPUT_TITLE, ignoreCase = true) }
        ?: ""

    val rawAuthor = structuredAuthor?.takeIf { !isUnknownAuthor(it) }
        ?: extractedAuthor.takeIf { !isUnknownAuthor(it) }
        ?: sourceLink?.let { extractAuthorFromUrl(it) }?.takeIf { !isUnknownAuthor(it) }
    val author = if (isUnknownAuthor(rawAuthor)) "" else rawAuthor?.trim().orEmpty()

    return TitleAndAuthor(title, author)
}
