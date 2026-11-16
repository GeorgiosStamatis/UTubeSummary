package io.github.utubesummary.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import io.github.utubesummary.R
import io.github.utubesummary.domain.importer.InvalidBackupException
import io.github.utubesummary.domain.provider.FileTooLargeException
import io.github.utubesummary.domain.usecase.GetUserSettingsUseCase
import io.github.utubesummary.domain.usecase.ImportVideoSummariesUseCase
import io.github.utubesummary.domain.usecase.TestProviderConnectionUseCase
import io.github.utubesummary.domain.usecase.UpdateProviderConfigUseCase
import io.github.utubesummary.domain.usecase.UpdateUserPreferencesUseCase
import io.github.utubesummary.llm.AIProvider
import io.github.utubesummary.model.ProviderTestResult
import io.github.utubesummary.model.SummaryLength
import io.github.utubesummary.util.SecureStorageException
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    getUserSettingsUseCase: GetUserSettingsUseCase,
    private val updateUserPreferencesUseCase: UpdateUserPreferencesUseCase,
    private val updateProviderConfigUseCase: UpdateProviderConfigUseCase,
    private val testProviderConnectionUseCase: TestProviderConnectionUseCase,
    private val importVideoSummariesUseCase: ImportVideoSummariesUseCase,
) : ViewModel() {

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState

    /** Imports video summaries from the JSON backup at [uri], skipping videos already in history. */
    fun importVideoSummaries(uri: String) {
        if (_importState.value is ImportState.Importing) return
        _importState.value = ImportState.Importing
        viewModelScope.launch {
            importVideoSummariesUseCase(uri)
                .onSuccess { _importState.value = ImportState.Done(it) }
                .onFailure { error ->
                    val reason = when (error) {
                        is InvalidBackupException -> ImportFailure.INVALID_FILE
                        is FileTooLargeException -> ImportFailure.TOO_LARGE
                        else -> ImportFailure.UNREADABLE
                    }
                    _importState.value = ImportState.Failed(reason)
                }
        }
    }

    fun clearImportState() {
        _importState.value = ImportState.Idle
    }

    private val _userMessages = MutableSharedFlow<Int>(extraBufferCapacity = 1)

    /** One-shot messages for the UI (string resource ids), e.g. a failed secure save. */
    val userMessages: SharedFlow<Int> = _userMessages

    private fun launchSecure(block: suspend () -> Unit): Job = viewModelScope.launch {
        try {
            block()
        } catch (e: SecureStorageException) {
            _userMessages.tryEmit(R.string.secureStorageFailed)
        }
    }

    private val _providerTestState = MutableStateFlow<ProviderTestState>(ProviderTestState.Idle)
    val providerTestState: StateFlow<ProviderTestState> = _providerTestState
    private var providerTestJob: Job? = null

    /** Checks the URL and key without saving them. */
    fun testProviderConnection(provider: AIProvider, baseUrl: String, apiKey: String) {
        providerTestJob?.cancel()
        _providerTestState.value = ProviderTestState.Testing
        providerTestJob = launchSecure {
            val result = testProviderConnectionUseCase(provider, baseUrl, apiKey)
            if (result is ProviderTestResult.Success) {
                updateProviderConfigUseCase.applyFetchedModels(provider.name, result.models)
            }
            _providerTestState.value = ProviderTestState.Done(result)
        }
    }

    fun clearProviderTest() {
        providerTestJob?.cancel()
        _providerTestState.value = ProviderTestState.Idle
    }

    private val _summaryLength = MutableStateFlow<SummaryLength?>(null)
    private val _activeProvider = MutableStateFlow<AIProvider?>(null)
    private val _activeModel = MutableStateFlow<String?>(null)

    val settingsUiState: StateFlow<SettingsUiState> = combine(
        getUserSettingsUseCase(),
        _summaryLength,
        _activeProvider,
        _activeModel,
    ) { userSettings, immediateLength, immediateProvider, immediateModel ->
        val prefs = userSettings.preferences
        val providerConfigs = userSettings.providerConfigs
        val effectiveProvider = immediateProvider
            ?: prefs.activeProvider?.let { AIProvider.entries.find { p -> p.name == it } }
            ?: AIProvider.OPENAI
        val providerConfig = effectiveProvider.name.let { providerConfigs[it] }
        val storedLength = SummaryLength.entries.find { it.name == prefs.summaryLength }
            ?: SummaryLength.MEDIUM
        val effectiveLength = immediateLength ?: storedLength
        val effectiveModel = immediateModel?.takeIf { it.isNotBlank() }
            ?: providerConfig?.activeModel?.takeIf { it.isNotBlank() }
            ?: effectiveProvider.getEffectiveModel(providerConfig)

        SettingsUiState(
            useOriginalLanguage = prefs.useOriginalLanguage,
            dynamicColor = prefs.dynamicColor,
            showThumbnails = prefs.showThumbnails,
            theme = prefs.theme,
            apiKey = providerConfig?.apiKey?.takeIf { it.isNotBlank() },
            baseUrl = providerConfig?.baseUrl?.takeIf { it.isNotBlank() },
            activeProvider = effectiveProvider,
            providerConfigs = providerConfigs,
            activeModel = effectiveModel,
            showLength = prefs.showLength,
            summaryLength = effectiveLength,
            autoExtractUrl = prefs.autoExtractUrl,
            bilibiliSessData = prefs.bilibiliSessData,
            bilibiliSessDataExpires = prefs.bilibiliSessDataExpires,
            isAppendMode = prefs.isAppendMode,
            customBasePrompt = prefs.customBasePrompt,
            additionalSystemPrompt = prefs.additionalSystemPrompt,
            providerOrder = prefs.providerOrder,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    // Original Language in summary
    fun setUseOriginalLanguageValue(newValue: Boolean) =
        savePreference(updateUserPreferencesUseCase::setUseOriginalLanguage, newValue)

    // Video thumbnails in History
    fun setShowThumbnailsValue(newValue: Boolean) =
        savePreference(updateUserPreferencesUseCase::setShowThumbnails, newValue)

    // Dynamic color
    fun setDynamicColorValue(newValue: Boolean) =
        savePreference(updateUserPreferencesUseCase::setDynamicColor, newValue)

    // Theme for Dark, Light or System
    fun setTheme(newValue: Int) =
        savePreference(updateUserPreferencesUseCase::setTheme, newValue)

    // API Key
    fun setApiKeyValue(newValue: String, provider: String? = null) {
        launchSecure {
            val targetProvider = provider
                ?: _activeProvider.value?.name
                ?: settingsUiState.value.activeProvider?.name
            updateProviderConfigUseCase.updateApiKey(targetProvider, newValue)
        }
    }

    // API base url
    fun setBaseUrlValue(newValue: String, provider: String? = null) {
        launchSecure {
            val targetProvider = provider
                ?: _activeProvider.value?.name
                ?: settingsUiState.value.activeProvider?.name
            updateProviderConfigUseCase.updateBaseUrl(targetProvider, newValue)
        }
    }

    // AI provider
    fun setAIProviderValue(newValue: String) {
        val providerEnum = AIProvider.entries.find { it.name == newValue }
        _activeProvider.value = providerEnum
        _activeModel.value = null
        savePreference(updateUserPreferencesUseCase::setActiveProvider, newValue)
    }

    fun setProviderAndModel(provider: AIProvider, model: String) {
        _activeProvider.value = provider
        _activeModel.value = model
        savePreference(updateUserPreferencesUseCase::setActiveProvider, provider.name)
        launchSecure {
            updateModelForProvider(provider.name, model)
        }
    }

    fun setProviderOrder(order: List<String>) =
        savePreference(updateUserPreferencesUseCase::setProviderOrder, order)

    fun setProviderConfig(
        provider: String,
        baseUrl: String,
        apiKey: String,
        providerOrder: List<String>? = null,
    ) {
        val providerEnum = AIProvider.entries.find { it.name == provider }
        _activeProvider.value = providerEnum
        _activeModel.value = null
        launchSecure {
            updateProviderConfigUseCase.setProviderConfig(
                provider = provider,
                baseUrl = baseUrl,
                apiKey = apiKey,
                providerOrder = providerOrder,
            )
            // Best effort: refresh the model list from the server; failures are ignored.
            if (providerEnum != null && (baseUrl.isNotBlank() || !providerEnum.isMandatoryBaseUrl)) {
                val result = testProviderConnectionUseCase(providerEnum, baseUrl, apiKey)
                if (result is ProviderTestResult.Success) {
                    updateProviderConfigUseCase.applyFetchedModels(provider, result.models)
                }
            }
        }
    }

    // Model
    fun setModel(newValue: String, provider: AIProvider? = null) {
        _activeModel.value = newValue
        launchSecure {
            val targetProvider = provider
                ?: _activeProvider.value
                ?: settingsUiState.value.activeProvider
            updateModelForProvider(targetProvider?.name, newValue)
        }
    }

    fun setModelForProvider(provider: String, model: String) {
        val currentActive =
            _activeProvider.value?.name ?: settingsUiState.value.activeProvider?.name
        if (currentActive == provider) {
            _activeModel.value = model
        }
        launchSecure {
            updateModelForProvider(provider, model)
        }
    }

    private suspend fun updateModelForProvider(provider: String?, model: String) {
        updateProviderConfigUseCase.updateModelForProvider(provider, model)
    }

    fun setProviderModels(provider: String, models: List<String>, selectedModel: String? = null) {
        val currentActive =
            _activeProvider.value?.name ?: settingsUiState.value.activeProvider?.name
        val targetModel =
            selectedModel ?: models.firstOrNull() ?: ""
        if (currentActive == provider || currentActive == null) {
            if (targetModel.isNotBlank()) {
                _activeModel.value = targetModel
            }
            if (_activeProvider.value == null && settingsUiState.value.activeProvider == null) {
                _activeProvider.value = AIProvider.entries.find { it.name == provider }
            }
        }
        launchSecure {
            updateProviderConfigUseCase.setProviderModels(provider, models, selectedModel)
        }
    }

    fun resetProviderModelsToDefault(provider: String) {
        val currentActive =
            _activeProvider.value?.name ?: settingsUiState.value.activeProvider?.name
        if (currentActive == provider || currentActive == null) {
            val aiProvider = AIProvider.entries.find { it.name == provider }
            _activeModel.value = aiProvider?.defaultModelIds?.firstOrNull()
        }
        launchSecure {
            updateProviderConfigUseCase.resetProviderModelsToDefault(provider)
        }
    }

    // Show length
    fun setShowLengthValue(newValue: Boolean) =
        savePreference(updateUserPreferencesUseCase::setShowLength, newValue)

    // Summary Length
    fun setSummaryLength(newValue: SummaryLength) {
        _summaryLength.value = newValue
        savePreference(updateUserPreferencesUseCase::setSummaryLength, newValue.name)
    }

    // Auto extract url
    fun setAutoExtractUrlValue(newValue: Boolean) =
        savePreference(updateUserPreferencesUseCase::setAutoExtractUrl, newValue)

    // BiliBili SESSDATA
    fun setBilibiliSessData(data: String, expires: Long) {
        launchSecure {
            updateUserPreferencesUseCase.setBilibiliSessData(data, expires)
        }
    }

    fun clearBilibiliSessData() {
        launchSecure {
            updateUserPreferencesUseCase.clearBilibiliSessData()
        }
    }

    // Advanced Setup
    fun setIsAppendMode(newValue: Boolean) {
        launchSecure {
            updateUserPreferencesUseCase.setIsAppendMode(newValue)
        }
    }

    fun setCustomBasePrompt(newValue: String) =
        savePreference(updateUserPreferencesUseCase::setCustomBasePrompt, newValue)

    fun setAdditionalSystemPrompt(newValue: String) =
        savePreference(updateUserPreferencesUseCase::setAdditionalSystemPrompt, newValue)

    // --- Preference Handling Helpers ---
    private fun <T> savePreference(setter: suspend (T) -> Unit, value: T) {
        launchSecure {
            setter(value)
        }
    }
}
