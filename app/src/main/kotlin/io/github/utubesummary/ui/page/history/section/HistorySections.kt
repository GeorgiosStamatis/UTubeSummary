package io.github.utubesummary.ui.page.history.section

/**
 * The section an entry is listed under. Pinned entries form their own section at the top; when
 * sorted by title there are no date headers (the key is empty).
 */
fun historySectionKey(
    pinned: Boolean,
    createdOn: Long,
    sortedByTitle: Boolean,
    pinnedTitle: String,
    dateTitle: (Long) -> String = ::getDateSectionTitle,
): String = when {
    pinned -> pinnedTitle
    sortedByTitle -> ""
    else -> dateTitle(createdOn)
}

/**
 * Where an entry sits inside its section, from the section keys of its neighbours (`null` at the
 * ends of the list). Used to round the corners of the first and last row of a group.
 */
fun groupPositionOf(previousKey: String?, key: String, nextKey: String?): GroupPosition {
    val first = previousKey != key
    val last = nextKey != key
    return when {
        first && last -> GroupPosition.SINGLE
        first -> GroupPosition.TOP
        last -> GroupPosition.BOTTOM
        else -> GroupPosition.MIDDLE
    }
}
