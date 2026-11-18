package io.github.utubesummary.ui.component

import java.util.Locale

/**
 * Validates that an author string is genuine and not an accidental paragraph, summary text, or placeholder.
 */
internal fun isKnownAuthor(author: String?): Boolean {
    if (author.isNullOrBlank()) return false
    val trimmed = author.trim()
    if (trimmed.length > 60 || trimmed.contains('\n') || trimmed.contains('\r')) return false
    if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith(
            "https://",
            ignoreCase = true
        )
    ) return false
    val lower = trimmed.lowercase(Locale.ROOT)
    return lower !in setOf(
        "unknown",
        "unknown author",
        "n/a",
        "none",
        "null",
        "article",
        "author",
        "creator",
        "speaker"
    )
}

/**
 * Determines whether the overview block should be displayed.
 * Prevents redundant display when overview is identical to or contained in the summary body.
 */
internal fun shouldShowOverview(overview: String?, summary: String): Boolean {
    if (overview.isNullOrBlank()) return false
    val o = overview.trim()
    val s = summary.trim()
    if (o.isEmpty() || s.isEmpty()) return false
    if (s.equals(o, ignoreCase = true)) return false
    if (s.startsWith(o, ignoreCase = true)) return false
    if (s.contains(o, ignoreCase = true)) return false
    if (s.length < o.length * 1.25) return false
    return true
}
