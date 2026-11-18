package io.github.utubesummary.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

private const val WIDE_MIN_WIDTH_DP = 840
private const val LANDSCAPE_WIDE_MIN_WIDTH_DP = 600
private const val INSTANT_DIALOG_HEIGHT_PORTRAIT = 0.55f
private const val INSTANT_DIALOG_HEIGHT_LANDSCAPE = 0.9f

/**
 * Whether the window is wide enough for a two-pane layout: a tablet-sized width, or a landscape
 * window that is at least 600 dp wide.
 */
fun isWideLayout(widthDp: Int, heightDp: Int): Boolean =
    widthDp >= WIDE_MIN_WIDTH_DP || (widthDp > heightDp && widthDp >= LANDSCAPE_WIDE_MIN_WIDTH_DP)

/** Share of the window height the instant summary dialog may use; landscape windows are short. */
fun instantDialogHeightFraction(widthDp: Int, heightDp: Int): Float =
    if (widthDp > heightDp) INSTANT_DIALOG_HEIGHT_LANDSCAPE else INSTANT_DIALOG_HEIGHT_PORTRAIT

/** [isWideLayout] for the current window; recomposes when the window is rotated or resized. */
@Composable
fun rememberIsWideLayout(): Boolean {
    val configuration = LocalConfiguration.current
    return isWideLayout(configuration.screenWidthDp, configuration.screenHeightDp)
}
