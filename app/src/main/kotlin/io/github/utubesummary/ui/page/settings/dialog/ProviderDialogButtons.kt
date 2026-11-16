package io.github.utubesummary.ui.page.settings.dialog

import io.github.utubesummary.model.ProviderTestResult
import io.github.utubesummary.vm.ProviderTestState

/** Which of the AI provider dialog's action buttons are enabled. */
internal data class ProviderDialogButtons(val canSubmit: Boolean, val canTest: Boolean)

internal fun providerDialogButtons(
    providerChanged: Boolean,
    configChanged: Boolean,
    orderChanged: Boolean,
    formValid: Boolean,
    testState: ProviderTestState,
): ProviderDialogButtons {
    // A successful Test validates the current inputs: OK is allowed, Test is not needed again.
    val testPassed = (testState as? ProviderTestState.Done)?.result is ProviderTestResult.Success
    val canSubmit =
        if (providerChanged || configChanged) formValid else (orderChanged || testPassed)
    val canTest = formValid && testState !is ProviderTestState.Testing && !testPassed
    return ProviderDialogButtons(canSubmit = canSubmit, canTest = canTest)
}
