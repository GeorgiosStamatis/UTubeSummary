package io.github.utubesummary.model

/** How the History is ordered; pinned items always stay on top. */
enum class HistorySort(val code: Int) {
    NEWEST(0),
    TITLE(1),
    OLDEST(2),
}

/** What the History list shows besides the search text. */
data class HistoryFilter(
    val type: SummaryType? = null,
    /** Only items with a result made by this profile. */
    val profileId: String? = null,
    val pinnedOnly: Boolean = false,
    val sort: HistorySort = HistorySort.NEWEST,
) {
    val isActive: Boolean get() = type != null || profileId != null || pinnedOnly
}
