package io.github.utubesummary.ui.page.home

import io.github.utubesummary.model.SummaryLength

/** What a result was made with, and what is selected now. `null` means "not known". */
data class ResultSettings(
    val hasResult: Boolean,
    val resultLength: SummaryLength?,
    val resultProfileId: String?,
    val resultLanguage: String?,
)

data class SelectedSettings(
    val showLength: Boolean,
    val length: SummaryLength,
    val profileId: String,
    val language: String,
)

/**
 * Whether the shown result no longer matches the selected length, profile or language, so the
 * summarize button should offer to run it again. A result restored from the History does not know
 * its profile or language (`null`), which never counts as a difference.
 */
fun isResultStale(result: ResultSettings, selected: SelectedSettings): Boolean {
    if (!result.hasResult) return false
    val lengthDiffers = selected.showLength &&
            result.resultLength != null && result.resultLength != selected.length
    val profileDiffers = result.resultProfileId != null && result.resultProfileId != selected.profileId
    val languageDiffers = result.resultLanguage != null && result.resultLanguage != selected.language
    return lengthDiffers || profileDiffers || languageDiffers
}
