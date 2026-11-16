package io.github.utubesummary.ui.page.settings.dialog

import io.github.utubesummary.model.ProviderTestResult
import io.github.utubesummary.vm.ProviderTestState
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProviderDialogButtonsTest {

    private fun buttons(
        providerChanged: Boolean = false,
        configChanged: Boolean = false,
        orderChanged: Boolean = false,
        formValid: Boolean = true,
        testState: ProviderTestState = ProviderTestState.Idle,
    ) = providerDialogButtons(providerChanged, configChanged, orderChanged, formValid, testState)

    private val success = ProviderTestState.Done(ProviderTestResult.Success(keyChecked = true))

    @Test
    fun `untouched form allows test but not ok`() {
        val b = buttons()
        assertFalse(b.canSubmit)
        assertTrue(b.canTest)
    }

    @Test
    fun `after a successful test ok is enabled and test is disabled`() {
        val b = buttons(testState = success)
        assertTrue(b.canSubmit)
        assertFalse(b.canTest)
    }

    @Test
    fun `after a failed test ok stays disabled and test can be retried`() {
        val b = buttons(testState = ProviderTestState.Done(ProviderTestResult.Unreachable))
        assertFalse(b.canSubmit)
        assertTrue(b.canTest)
    }

    @Test
    fun `while testing test is disabled`() {
        assertFalse(buttons(testState = ProviderTestState.Testing).canTest)
    }

    @Test
    fun `edits enable ok when the form is valid`() {
        assertTrue(buttons(configChanged = true).canSubmit)
        assertTrue(buttons(providerChanged = true).canSubmit)
        assertFalse(buttons(configChanged = true, formValid = false).canSubmit)
    }

    @Test
    fun `reordering alone enables ok`() {
        assertTrue(buttons(orderChanged = true).canSubmit)
    }

    @Test
    fun `invalid form disables test`() {
        assertFalse(buttons(formValid = false).canTest)
    }
}
