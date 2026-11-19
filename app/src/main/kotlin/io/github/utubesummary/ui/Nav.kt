package io.github.utubesummary.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Nav : NavKey {
    @Serializable
    data object Home : Nav

    @Serializable
    data object Onboarding : Nav

    @Serializable
    data object History : Nav

    @Serializable
    data class Settings(val highlight: String? = null) : Nav

    @Serializable
    data object AdvancedSummarySetup : Nav

    @Serializable
    data object SummaryProfiles : Nav
}

/**
 * The back stack to show when a new link or summary is shared into the app: Home, where the input,
 * profile and language can be chosen. Everything above Home (History, Settings, ...) is dropped.
 * Without Home in the stack (during the first-run tutorial) nothing changes: Home reads the pending
 * link as soon as the tutorial is done.
 */
fun backStackForStartAction(stack: List<NavKey>): List<NavKey> {
    val homeIndex = stack.indexOf(Nav.Home)
    return if (homeIndex < 0) stack else stack.take(homeIndex + 1)
}
