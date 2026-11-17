package io.github.utubesummary.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale

class SummaryLanguageTest {

    @Test
    fun `before a choice the old original language switch decides`() {
        assertEquals(
            SUMMARY_LANGUAGE_CONTENT,
            effectiveSummaryLanguage(UserPreferences(useOriginalLanguage = true)),
        )
        assertEquals(
            SUMMARY_LANGUAGE_APP,
            effectiveSummaryLanguage(UserPreferences(useOriginalLanguage = false)),
        )
    }

    @Test
    fun `a stored choice wins over the old switch`() {
        val prefs = UserPreferences(useOriginalLanguage = true, summaryLanguage = "fr")

        assertEquals("fr", effectiveSummaryLanguage(prefs))
    }

    @Test
    fun `video language has no locale and the others map to one`() {
        val app = Locale.GERMAN

        assertNull(summaryPromptLocale(SUMMARY_LANGUAGE_CONTENT, app))
        assertEquals(app, summaryPromptLocale(SUMMARY_LANGUAGE_APP, app))
        assertEquals("French", summaryPromptLocale("fr", app)!!.getDisplayLanguage(Locale.ENGLISH))
    }

    @Test
    fun `every offered language tag resolves to a real language`() {
        assertTrue(summaryLanguageTags.all {
            Locale.forLanguageTag(it).getDisplayLanguage(Locale.ENGLISH).isNotBlank()
        })
    }
}
