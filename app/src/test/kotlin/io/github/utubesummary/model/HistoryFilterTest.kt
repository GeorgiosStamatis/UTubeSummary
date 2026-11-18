package io.github.utubesummary.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HistoryFilterTest {

    @Test
    fun `a default filter is not active and sorts newest first`() {
        val filter = HistoryFilter()

        assertFalse(filter.isActive)
        assertEquals(HistorySort.NEWEST, filter.sort)
    }

    @Test
    fun `type, profile and pinned each make the filter active`() {
        assertTrue(HistoryFilter(type = SummaryType.VIDEO).isActive)
        assertTrue(HistoryFilter(profileId = "quick_brief").isActive)
        assertTrue(HistoryFilter(pinnedOnly = true).isActive)
    }

    @Test
    fun `sorting alone is not a filter`() {
        assertFalse(HistoryFilter(sort = HistorySort.OLDEST).isActive)
        assertFalse(HistoryFilter(sort = HistorySort.TITLE).isActive)
    }

    @Test
    fun `sort codes are unique and stable`() {
        assertEquals(
            listOf(0, 1, 2),
            listOf(HistorySort.NEWEST, HistorySort.TITLE, HistorySort.OLDEST).map { it.code }
        )
        assertEquals(HistorySort.entries.size, HistorySort.entries.map { it.code }.toSet().size)
    }

    @Test
    fun `only short, medium and long can be chosen by the user`() {
        assertEquals(
            listOf(SummaryLength.SHORT, SummaryLength.MEDIUM, SummaryLength.LONG),
            SummaryLength.userSelectable
        )
        assertFalse(SummaryLength.NONE in SummaryLength.userSelectable)
    }
}
