package io.github.utubesummary.ui.page.settings.dialog

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ModelSearchTest {

    private val models = listOf("gpt-4o", "gpt-4o-mini", "o3-mini", "Claude-Sonnet")

    @Test
    fun `a blank query lists every model`() {
        assertEquals(models, filterModels(models, ""))
        assertEquals(models, filterModels(models, "   "))
    }

    @Test
    fun `the search ignores case and surrounding spaces and keeps the order`() {
        assertEquals(listOf("gpt-4o", "gpt-4o-mini"), filterModels(models, " GPT "))
        assertEquals(listOf("o3-mini"), filterModels(models, "O3"))
        assertEquals(listOf("Claude-Sonnet"), filterModels(models, "sonnet"))
    }

    @Test
    fun `a query that matches nothing lists nothing`() {
        assertEquals(emptyList<String>(), filterModels(models, "llama"))
    }

    @Test
    fun `a typed name that is not a model can be used`() {
        assertEquals("gpt-5-new", typedModelToUse(models, "  gpt-5-new "))
    }

    @Test
    fun `a name that is only part of a model can still be used`() {
        assertEquals("gpt-4", typedModelToUse(models, "gpt-4"))
    }

    @Test
    fun `an exact model or a blank query offers nothing`() {
        assertNull(typedModelToUse(models, "gpt-4o"))
        assertNull(typedModelToUse(models, " gpt-4o "))
        assertNull(typedModelToUse(models, ""))
        assertNull(typedModelToUse(models, "   "))
    }
}
