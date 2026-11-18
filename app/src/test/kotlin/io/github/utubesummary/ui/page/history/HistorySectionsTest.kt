package io.github.utubesummary.ui.page.history

import io.github.utubesummary.ui.page.history.section.GroupPosition
import io.github.utubesummary.ui.page.history.section.groupPositionOf
import io.github.utubesummary.ui.page.history.section.historySectionKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HistorySectionsTest {

    private val dateTitle = { _: Long -> "Today" }

    @Test
    fun `pinned entries form their own section`() {
        assertEquals("Pinned", historySectionKey(true, 1L, false, "Pinned", dateTitle))
        assertEquals("Pinned", historySectionKey(true, 1L, true, "Pinned", dateTitle))
    }

    @Test
    fun `others are grouped by date`() {
        assertEquals("Today", historySectionKey(false, 1L, false, "Pinned", dateTitle))
    }

    @Test
    fun `sorted by title there are no date headers`() {
        assertEquals("", historySectionKey(false, 1L, true, "Pinned", dateTitle))
    }

    @Test
    fun `a lone entry is single`() {
        assertEquals(GroupPosition.SINGLE, groupPositionOf(null, "Today", null))
        assertEquals(GroupPosition.SINGLE, groupPositionOf("Pinned", "Today", "Yesterday"))
    }

    @Test
    fun `entries of a group are top, middle and bottom`() {
        assertEquals(GroupPosition.TOP, groupPositionOf(null, "Today", "Today"))
        assertEquals(GroupPosition.MIDDLE, groupPositionOf("Today", "Today", "Today"))
        assertEquals(GroupPosition.BOTTOM, groupPositionOf("Today", "Today", null))
        assertEquals(GroupPosition.BOTTOM, groupPositionOf("Today", "Today", "Yesterday"))
    }

    @Test
    fun `a list without headers is one group`() {
        assertEquals(GroupPosition.TOP, groupPositionOf(null, "", ""))
        assertEquals(GroupPosition.MIDDLE, groupPositionOf("", "", ""))
    }
}
