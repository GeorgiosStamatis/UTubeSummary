package io.github.utubesummary.ui.page.settings

import io.github.utubesummary.ui.page.settings.search.SearchableSetting
import io.github.utubesummary.ui.page.settings.search.SettingsSearchState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SettingsSearchStateTest {

    private fun setting(id: String, title: String, subtitle: String? = null, group: String = "General") =
        SearchableSetting(id = id, title = title, subtitle = subtitle, group = group, iconBadge = {})

    private val items = listOf(
        setting("theme", "Theme", "Light, dark or system", group = "Display"),
        setting("thumbs", "Show thumbnails", "Small YouTube images in the History", group = "Display"),
        setting("provider", "AI provider", "OpenAI, Gemini, Claude", group = "AI"),
        setting("import", "Import video summaries", group = "Data"),
    )

    @Test
    fun `without a query nothing is searching and nothing is listed`() {
        val state = SettingsSearchState(allItems = items)

        assertFalse(state.isSearching)
        assertEquals(emptyList<SearchableSetting>(), state.filteredItems)
    }

    @Test
    fun `a blank query is not a search`() {
        val state = SettingsSearchState(initialQuery = "   ", allItems = items)

        assertFalse(state.isSearching)
        assertEquals(emptyList<SearchableSetting>(), state.filteredItems)
    }

    @Test
    fun `the title is matched ignoring case`() {
        val state = SettingsSearchState(initialQuery = "THEME", allItems = items)

        assertTrue(state.isSearching)
        assertEquals(listOf("theme"), state.filteredItems.map { it.id })
    }

    @Test
    fun `the subtitle and the group are matched too`() {
        val state = SettingsSearchState(allItems = items)

        state.onQueryChange("gemini")
        assertEquals(listOf("provider"), state.filteredItems.map { it.id })

        state.onQueryChange("display")
        assertEquals(listOf("theme", "thumbs"), state.filteredItems.map { it.id })
    }

    @Test
    fun `surrounding spaces are ignored`() {
        val state = SettingsSearchState(initialQuery = "  import ", allItems = items)

        assertEquals(listOf("import"), state.filteredItems.map { it.id })
    }

    @Test
    fun `an unknown word finds nothing but still counts as searching`() {
        val state = SettingsSearchState(initialQuery = "zzz", allItems = items)

        assertTrue(state.isSearching)
        assertEquals(emptyList<SearchableSetting>(), state.filteredItems)
    }

    @Test
    fun `clearing the query ends the search`() {
        val state = SettingsSearchState(initialQuery = "theme", allItems = items)

        state.onClear()

        assertEquals("", state.query)
        assertFalse(state.isSearching)
    }
}
