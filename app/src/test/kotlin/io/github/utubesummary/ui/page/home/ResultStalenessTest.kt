package io.github.utubesummary.ui.page.home

import io.github.utubesummary.model.SummaryLength
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ResultStalenessTest {

    private val selected = SelectedSettings(
        showLength = true,
        length = SummaryLength.MEDIUM,
        profileId = "default",
        language = "content",
    )

    private val matching = ResultSettings(
        hasResult = true,
        resultLength = SummaryLength.MEDIUM,
        resultProfileId = "default",
        resultLanguage = "content",
    )

    @Test
    fun `a result made with the selected settings is current`() {
        assertFalse(isResultStale(matching, selected))
    }

    @Test
    fun `without a result nothing is stale`() {
        val none = ResultSettings(hasResult = false, resultLength = null, resultProfileId = null, resultLanguage = null)

        assertFalse(isResultStale(none, selected.copy(profileId = "other")))
    }

    @Test
    fun `another length makes the result stale only while lengths are shown`() {
        val other = selected.copy(length = SummaryLength.LONG)

        assertTrue(isResultStale(matching, other))
        assertFalse(isResultStale(matching, other.copy(showLength = false)))
    }

    @Test
    fun `another profile makes the result stale`() {
        assertTrue(isResultStale(matching, selected.copy(profileId = "quick_brief")))
    }

    @Test
    fun `another language makes the result stale`() {
        assertTrue(isResultStale(matching, selected.copy(language = "de")))
    }

    @Test
    fun `a result restored from the history does not know its profile or language`() {
        val restored = matching.copy(resultProfileId = null, resultLanguage = null)

        assertFalse(isResultStale(restored, selected.copy(profileId = "quick_brief", language = "de")))
    }

    @Test
    fun `a restored result still compares its length`() {
        val restored = matching.copy(resultProfileId = null, resultLanguage = null)

        assertTrue(isResultStale(restored, selected.copy(length = SummaryLength.SHORT)))
    }
}
