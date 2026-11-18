package io.github.utubesummary.ui.component

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WindowLayoutTest {

    @Test
    fun `phone in portrait is not wide`() {
        assertFalse(isWideLayout(widthDp = 411, heightDp = 891))
    }

    @Test
    fun `phone in landscape below 600dp is not wide`() {
        assertFalse(isWideLayout(widthDp = 599, heightDp = 360))
    }

    @Test
    fun `phone in landscape from 600dp is wide`() {
        assertTrue(isWideLayout(widthDp = 800, heightDp = 360))
        assertTrue(isWideLayout(widthDp = 600, heightDp = 360))
    }

    @Test
    fun `small tablet in portrait is not wide`() {
        assertFalse(isWideLayout(widthDp = 600, heightDp = 960))
    }

    @Test
    fun `tablet in landscape is wide`() {
        assertTrue(isWideLayout(widthDp = 960, heightDp = 600))
        assertTrue(isWideLayout(widthDp = 1280, heightDp = 800))
    }

    @Test
    fun `width of 840dp is wide even in portrait`() {
        assertTrue(isWideLayout(widthDp = 840, heightDp = 1200))
        assertFalse(isWideLayout(widthDp = 839, heightDp = 1200))
    }

    @Test
    fun `instant dialog gets more height in landscape`() {
        assertEquals(0.9f, instantDialogHeightFraction(widthDp = 800, heightDp = 360))
        assertEquals(0.55f, instantDialogHeightFraction(widthDp = 411, heightDp = 891))
    }
}
