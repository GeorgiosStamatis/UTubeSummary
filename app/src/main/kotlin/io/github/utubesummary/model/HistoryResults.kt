package io.github.utubesummary.model

/** Profile used by a stored result; entries from before profiles existed used the Default one. */
private fun HistoryLengthResult.variantProfile(): String = profileId ?: DEFAULT_PROFILE_ID

/** Two results of the same length are the same variant when profile and language are equal. */
fun isSameVariant(a: HistoryLengthResult, b: HistoryLengthResult): Boolean =
    a.length == b.length && a.variantProfile() == b.variantProfile() && a.language == b.language

/**
 * Adds [new] to the results of an item. It becomes the current result for its length; the result it
 * displaces is kept in the returned alternatives unless it is the same variant (same profile and
 * language), which [new] simply replaces.
 */
fun mergeHistoryResult(
    current: Map<SummaryLength, HistoryLengthResult>,
    alternatives: List<HistoryLengthResult>,
    new: HistoryLengthResult,
): Pair<Map<SummaryLength, HistoryLengthResult>, List<HistoryLengthResult>> {
    val old = current[new.length]
    val keptAlternatives = alternatives.filterNot { isSameVariant(it, new) }
    val newAlternatives = if (old != null && !isSameVariant(old, new)) keptAlternatives + old else keptAlternatives
    return (current + (new.length to new)) to newAlternatives
}

/**
 * Space-delimited profile ids of every result of an item (" default quick_brief "), so the History
 * can be filtered by profile with a simple LIKE query.
 */
fun HistorySummary.variantKeys(): String {
    val ids = (lengthResults.values + alternatives).map { it.variantProfile() }.distinct()
    return if (ids.isEmpty()) "" else ids.joinToString(separator = " ", prefix = " ", postfix = " ")
}
