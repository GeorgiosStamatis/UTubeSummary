package io.github.utubesummary.model

/** What the History list shows besides the search text. */
data class HistoryFilter(
    val type: SummaryType? = null,
    /** Only items with a result made by this profile. */
    val profileId: String? = null,
    val pinnedOnly: Boolean = false,
) {
    val isActive: Boolean get() = type != null || profileId != null || pinnedOnly
}
