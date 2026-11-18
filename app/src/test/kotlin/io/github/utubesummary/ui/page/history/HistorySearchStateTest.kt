package io.github.utubesummary.ui.page.history

import io.github.utubesummary.model.HistorySort
import io.github.utubesummary.model.SummaryType
import io.github.utubesummary.ui.page.history.search.HistorySearchState
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HistorySearchStateTest {

    private fun state(
        query: String = "",
        type: SummaryType? = null,
        profileId: String? = null,
        pinnedOnly: Boolean = false,
    ) = HistorySearchState(
        query = query,
        selectedFilter = type,
        onQueryChange = {},
        onClear = {},
        onFilterChanged = {},
        selectedProfileId = profileId,
        pinnedOnly = pinnedOnly,
    )

    @Test
    fun `an empty state is not searching`() {
        assertFalse(state().isSearching)
        assertFalse(state(query = "   ").isSearching)
    }

    @Test
    fun `text, type, profile and pinned each count as searching`() {
        assertTrue(state(query = "kotlin").isSearching)
        assertTrue(state(type = SummaryType.VIDEO).isSearching)
        assertTrue(state(profileId = "quick_brief").isSearching)
        assertTrue(state(pinnedOnly = true).isSearching)
    }

    @Test
    fun `the sort order alone is not a search`() {
        val sorted = HistorySearchState(
            query = "",
            selectedFilter = null,
            onQueryChange = {},
            onClear = {},
            onFilterChanged = {},
            sort = HistorySort.TITLE,
        )

        assertFalse(sorted.isSearching)
    }
}
