package io.github.utubesummary.vm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import io.github.utubesummary.domain.repository.UserPreferencesRepository
import io.github.utubesummary.domain.usecase.GetOnboardingStatusUseCase
import io.github.utubesummary.domain.usecase.SetOnboardingStatusUseCase
import io.github.utubesummary.model.SummaryLength
import io.github.utubesummary.model.SummaryOutput
import io.github.utubesummary.model.ProfileEntry
import io.github.utubesummary.model.UserPreferences
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import io.github.utubesummary.testutil.FakeUserPreferencesRepository

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakePrefsRepo: FakeUserPreferencesRepository
    private lateinit var getOnboardingStatusUseCase: GetOnboardingStatusUseCase
    private lateinit var setOnboardingStatusUseCase: SetOnboardingStatusUseCase

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakePrefsRepo = FakeUserPreferencesRepository()
        getOnboardingStatusUseCase = GetOnboardingStatusUseCase(fakePrefsRepo)
        setOnboardingStatusUseCase = SetOnboardingStatusUseCase(fakePrefsRepo)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test isOnboarded reflects onboarding state`() = runTest(testDispatcher) {
        fakePrefsRepo.prefs.value = UserPreferences(isOnboarded = false)
        val viewModel = AppViewModel(getOnboardingStatusUseCase, setOnboardingStatusUseCase)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.isOnboarded.collect {}
        }

        assertEquals(false, viewModel.isOnboarded.value)

        fakePrefsRepo.prefs.value = UserPreferences(isOnboarded = true)
        assertEquals(true, viewModel.isOnboarded.value)
    }

    @Test
    fun `test setIsOnboarded updates repository`() = runTest(testDispatcher) {
        fakePrefsRepo.prefs.value = UserPreferences(isOnboarded = false)
        val viewModel = AppViewModel(getOnboardingStatusUseCase, setOnboardingStatusUseCase)

        viewModel.setIsOnboarded(true)
        assertTrue(fakePrefsRepo.prefs.value.isOnboarded)
    }

    @Test
    fun `test appStartAction event dispatch and reset`() {
        val viewModel = AppViewModel(getOnboardingStatusUseCase, setOnboardingStatusUseCase)

        viewModel.onEvent(AppStartAction(content = "https://example.com", autoTrigger = true))
        assertEquals("https://example.com", viewModel.appStartAction.value.content)
        assertTrue(viewModel.appStartAction.value.autoTrigger)

        viewModel.onStartActionHandled()
        assertNull(viewModel.appStartAction.value.content)
        assertFalse(viewModel.appStartAction.value.autoTrigger)
    }

    @Test
    fun `test appStartAction with initialSummary dispatch and reset`() {
        val viewModel = AppViewModel(getOnboardingStatusUseCase, setOnboardingStatusUseCase)
        val initialSummary = SummaryOutput(
            title = "Title",
            author = "Author",
            summary = "Summary",
            sourceLink = "https://example.com",
            isYoutubeLink = false,
            isBiliBiliLink = false,
            length = SummaryLength.MEDIUM,
        )

        viewModel.onEvent(
            AppStartAction(
                content = "https://example.com",
                autoTrigger = false,
                initialSummary = initialSummary,
            )
        )
        assertEquals("https://example.com", viewModel.appStartAction.value.content)
        assertFalse(viewModel.appStartAction.value.autoTrigger)
        assertEquals(initialSummary, viewModel.appStartAction.value.initialSummary)

        viewModel.onStartActionHandled()
        assertNull(viewModel.appStartAction.value.content)
        assertFalse(viewModel.appStartAction.value.autoTrigger)
        assertNull(viewModel.appStartAction.value.initialSummary)
        assertTrue(viewModel.appStartAction.value.lengthResults.isEmpty())
    }
}
