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

/**
 * This item without [target], or null when no result is left (the item should then be deleted).
 * If the removed result was the current one for its length, the newest remaining result of that
 * length takes its place; the entry's own length follows if its length no longer has a result.
 */
fun HistorySummary.withoutResult(target: HistoryLengthResult): HistorySummary? {
    val all = lengthResults.values + alternatives
    val index = all.indexOf(target)
    if (index < 0) return this
    val remaining = all.toMutableList().apply { removeAt(index) }
    if (remaining.isEmpty()) return null

    val current = linkedMapOf<SummaryLength, HistoryLengthResult>()
    remaining.groupBy { it.length }.forEach { (length, results) ->
        val kept = lengthResults[length]?.takeIf { it in results }
        current[length] = kept ?: results.maxByOrNull { it.createdOn } ?: results.last()
    }
    val newLength = if (current.containsKey(length)) length else remaining.maxByOrNull { it.createdOn }!!.length
    return copy(
        length = newLength,
        lengthResults = current,
        alternatives = remaining.filterNot { it in current.values },
    )
}

/** Videos and web pages can be fetched again; text has nothing to fetch and documents may be gone. */
fun HistorySummary.canBeUpdated(): Boolean =
    (type == SummaryType.VIDEO || type == SummaryType.ARTICLE) && !sourceLink.isNullOrBlank()
