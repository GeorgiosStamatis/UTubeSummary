package io.github.utubesummary.ui.component

import io.github.utubesummary.model.SummaryOutput

/**
 * The text that is shared for a summary: Markdown with the title, author, source link, overview,
 * key points, the summary itself and tags. Empty parts are left out.
 */
fun summaryShareText(summary: SummaryOutput): String = buildString {
    if (summary.title.isNotBlank()) appendLine("# ${summary.title.trim()}")

    val byline = listOfNotNull(
        summary.author.takeIf { it.isNotBlank() }?.let { "By ${it.trim()}" },
        summary.sourceLink?.takeIf { it.isNotBlank() }?.let { "Source: ${it.trim()}" },
    )
    if (byline.isNotEmpty()) {
        appendLine()
        appendLine(byline.joinToString(" · "))
    }

    summary.overview?.takeIf { it.isNotBlank() }?.let {
        appendLine()
        appendLine("> ${it.trim()}")
    }

    if (summary.keyPoints.isNotEmpty()) {
        appendLine()
        summary.keyPoints.forEach { appendLine("- ${it.trim()}") }
    }

    if (summary.summary.isNotBlank()) {
        appendLine()
        appendLine(summary.summary.trim())
    }

    if (summary.tags.isNotEmpty()) {
        appendLine()
        appendLine(summary.tags.joinToString(" ") { "#" + it.trim().replace(' ', '_') })
    }
}.trim()
