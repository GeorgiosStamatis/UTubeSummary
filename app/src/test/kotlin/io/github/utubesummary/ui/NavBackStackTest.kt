package io.github.utubesummary.ui

import androidx.navigation3.runtime.NavKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NavBackStackTest {

    private fun target(vararg stack: NavKey): List<NavKey> = backStackForStartAction(stack.toList())

    @Test
    fun `a stack that ends on Home stays as it is`() {
        assertEquals(listOf<NavKey>(Nav.Home), target(Nav.Home))
    }

    @Test
    fun `History above Home is dropped`() {
        assertEquals(listOf<NavKey>(Nav.Home), target(Nav.Home, Nav.History))
    }

    @Test
    fun `several screens above Home are all dropped`() {
        assertEquals(
            listOf<NavKey>(Nav.Home),
            target(Nav.Home, Nav.Settings(highlight = "ai"), Nav.AdvancedSummarySetup, Nav.SummaryProfiles)
        )
        assertEquals(listOf<NavKey>(Nav.Home), target(Nav.Home, Nav.History, Nav.Settings()))
    }

    @Test
    fun `only what is below Home is kept`() {
        assertEquals(listOf<NavKey>(Nav.Onboarding, Nav.Home), target(Nav.Onboarding, Nav.Home, Nav.History))
    }

    @Test
    fun `without Home nothing changes`() {
        assertEquals(listOf<NavKey>(Nav.Onboarding), target(Nav.Onboarding))
        assertEquals(listOf<NavKey>(Nav.Settings(), Nav.History), target(Nav.Settings(), Nav.History))
        assertEquals(emptyList<NavKey>(), backStackForStartAction(emptyList()))
    }
}
