package io.github.utubesummary.ui.page.settings

import android.content.ClipData
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import io.github.utubesummary.ui.page.settings.section.DataSettingsGroup
import io.github.utubesummary.ui.page.settings.dialog.ImportSummariesDialog
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import io.github.utubesummary.BuildConfig
import io.github.utubesummary.R
import io.github.utubesummary.llm.AIProvider
import io.github.utubesummary.ui.Nav
import io.github.utubesummary.ui.page.settings.dialog.AIProviderSettingsDialog
import io.github.utubesummary.ui.page.settings.dialog.ModelSettingsDialog
import io.github.utubesummary.ui.page.settings.dialog.ThemeSettingsDialog
import io.github.utubesummary.ui.page.settings.search.SettingsEmptySearchResults
import io.github.utubesummary.ui.page.settings.search.SettingsSearchBar
import io.github.utubesummary.ui.page.settings.search.rememberSettingsSearchState
import io.github.utubesummary.ui.page.settings.section.AISettingsGroup
import io.github.utubesummary.ui.page.settings.section.DisplaySettingsGroup
import io.github.utubesummary.ui.page.settings.section.GroupPosition
import io.github.utubesummary.ui.page.settings.section.HelpSettingsGroup
import io.github.utubesummary.ui.page.settings.section.ServicesSettingsGroup
import io.github.utubesummary.ui.page.settings.section.SettingItem
import io.github.utubesummary.ui.page.settings.section.SettingsFooter
import io.github.utubesummary.ui.page.settings.section.SettingsGroup
import io.github.utubesummary.ui.theme.UTubeSummaryTheme
import io.github.utubesummary.vm.SettingsUiState
import io.github.utubesummary.vm.SettingsViewModel
import io.github.utubesummary.ui.component.ContentMaxWidth

private sealed interface DialogState {
    data object None : DialogState
    data object Theme : DialogState
    data object Provider : DialogState
    data class Model(val provider: AIProvider? = null) : DialogState
}

data class SettingsActions(
    val onThemeChange: (Int) -> Unit,
    val onApiKeyChange: (String) -> Unit,
    val onProviderChange: (String) -> Unit,
    val onModelChange: (String) -> Unit,
    val onProviderConfigChange: (String, String, String, List<String>?) -> Unit,
    val onSaveProviderModels: (String, List<String>, String) -> Unit,
    val onResetProviderModels: (String) -> Unit,
    val onUseOriginalLanguageChange: (Boolean) -> Unit,
    val onDynamicColorChange: (Boolean) -> Unit,
    val onShowThumbnailsChange: (Boolean) -> Unit,
    val onShowLengthChange: (Boolean) -> Unit,
    val onAutoExtractUrlChange: (Boolean) -> Unit,
    val onBilibiliSessDataChange: (String, Long) -> Unit,
    val onBilibiliSessDataClear: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onNav: (Nav) -> Unit = {},
    settingsViewModel: SettingsViewModel,
    highlightSection: String?,
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val state by settingsViewModel.settingsUiState.collectAsState()

    val importState by settingsViewModel.importState.collectAsState()
    ImportSummariesDialog(state = importState, onDismiss = settingsViewModel::clearImportState)
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) settingsViewModel.importVideoSummaries(uri.toString())
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) settingsViewModel.exportHistory(uri.toString())
    }

    val screenContext = LocalContext.current
    LaunchedEffect(settingsViewModel) {
        settingsViewModel.userMessages.collect { messageRes ->
            Toast.makeText(screenContext, messageRes, Toast.LENGTH_LONG).show()
        }
    }

    val actions = SettingsActions(
        onThemeChange = settingsViewModel::setTheme,
        onApiKeyChange = settingsViewModel::setApiKeyValue,
        onProviderChange = settingsViewModel::setAIProviderValue,
        onModelChange = settingsViewModel::setModel,
        onProviderConfigChange = settingsViewModel::setProviderConfig,
        onSaveProviderModels = settingsViewModel::setProviderModels,
        onResetProviderModels = settingsViewModel::resetProviderModelsToDefault,
        onUseOriginalLanguageChange = settingsViewModel::setUseOriginalLanguageValue,
        onDynamicColorChange = settingsViewModel::setDynamicColorValue,
        onShowThumbnailsChange = settingsViewModel::setShowThumbnailsValue,
        onShowLengthChange = settingsViewModel::setShowLengthValue,
        onAutoExtractUrlChange = settingsViewModel::setAutoExtractUrlValue,
        onBilibiliSessDataChange = settingsViewModel::setBilibiliSessData,
        onBilibiliSessDataClear = settingsViewModel::clearBilibiliSessData
    )

    var dialogState by remember { mutableStateOf<DialogState>(DialogState.None) }
    var showBiliBiliLoginSheet by remember { mutableStateOf(value = false) }
    var showClearBilibiliSessDataDialog by remember { mutableStateOf(value = false) }

    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    val scope = rememberCoroutineScope()

    if (showClearBilibiliSessDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearBilibiliSessDataDialog = false },
            title = { Text(stringResource(R.string.clear_bilibili_login)) },
            text = { Text(stringResource(R.string.clear_bilibili_login_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        actions.onBilibiliSessDataClear()
                        showClearBilibiliSessDataDialog = false
                    }
                ) { Text(stringResource(R.string.clear)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearBilibiliSessDataDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showBiliBiliLoginSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBiliBiliLoginSheet = false },
            sheetState = sheetState,
            modifier = Modifier.fillMaxHeight()
        ) {
            fun hide() {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        showBiliBiliLoginSheet = false
                    }
                }
            }
            BiliBiliLoginSheetContent(
                onDismiss = { hide() }
            ) { sessData, expires ->
                actions.onBilibiliSessDataChange(sessData, expires)
                hide()
            }
        }
    }

    when (val currentDialogState = dialogState) {
        DialogState.None -> {}
        DialogState.Theme -> {
            ThemeSettingsDialog(
                onDismissRequest = { dialogState = DialogState.None },
                currentTheme = state.theme,
                onThemeChange = actions.onThemeChange,
            )
        }

        DialogState.Provider -> {
            val testState by settingsViewModel.providerTestState.collectAsState()
            AIProviderSettingsDialog(
                testState = testState,
                onTest = settingsViewModel::testProviderConnection,
                onTestReset = settingsViewModel::clearProviderTest,
                initialProvider = state.activeProvider ?: AIProvider.OPENAI,
                providerConfigs = state.providerConfigs,
                providerOrder = state.providerOrder,
                onDismissRequest = { dialogState = DialogState.None },
                onConfirm = { provider, baseUrl, apiKey, order ->
                    actions.onProviderConfigChange(provider.name, baseUrl, apiKey, order)
                },
                onNext = { provider, baseUrl, apiKey, order ->
                    actions.onProviderConfigChange(provider.name, baseUrl, apiKey, order)
                    dialogState = DialogState.Model(provider)
                }
            )
        }

        is DialogState.Model -> {
            val targetProvider =
                currentDialogState.provider ?: state.activeProvider ?: AIProvider.OPENAI
            val targetModelId = state.providerConfigs[targetProvider.name]?.activeModel
                ?: if (targetProvider == state.activeProvider) state.activeModel else null
            ModelSettingsDialog(
                onDismissRequest = { dialogState = DialogState.None },
                provider = targetProvider,
                initialModelId = targetModelId,
                providerConfigs = state.providerConfigs,
                onConfirm = { models, selectedModel ->
                    actions.onSaveProviderModels(targetProvider.name, models, selectedModel)
                    dialogState = DialogState.None
                },
            )
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.settings),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.go_back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
    ) { innerPadding ->
        SettingsContent(
            innerPadding = innerPadding,
            state = state,
            actions = actions,
            onNav = { onNav(it) },
            onShowThemeDialog = { dialogState = DialogState.Theme },
            onShowAIProviderDialog = { dialogState = DialogState.Provider },
            onShowModelDialog = { dialogState = DialogState.Model() },
            onShowBiliBiliLoginSheet = { showBiliBiliLoginSheet = true },
            onShowClearBilibiliSessDataDialog = { showClearBilibiliSessDataDialog = true },
            onImportSummaries = { importLauncher.launch(arrayOf("application/json", "*/*")) },
            onExportHistory = { exportLauncher.launch("utubesummary-history.json") },
            highlightSection = highlightSection
        )
    }
}

@Composable
private fun SettingsContent(
    innerPadding: PaddingValues,
    state: SettingsUiState,
    actions: SettingsActions,
    onNav: (Nav) -> Unit,
    onShowThemeDialog: () -> Unit,
    onShowAIProviderDialog: () -> Unit,
    onShowModelDialog: () -> Unit,
    onShowBiliBiliLoginSheet: () -> Unit,
    onShowClearBilibiliSessDataDialog: () -> Unit,
    onImportSummaries: () -> Unit,
    onExportHistory: () -> Unit = {},
    highlightSection: String?,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val searchState = rememberSettingsSearchState(
        state = state,
        actions = actions,
        onNav = onNav,
        onShowThemeDialog = onShowThemeDialog,
        onShowAIProviderDialog = onShowAIProviderDialog,
        onShowModelDialog = onShowModelDialog,
        onShowBiliBiliLoginSheet = onShowBiliBiliLoginSheet,
        onShowClearBilibiliSessDataDialog = onShowClearBilibiliSessDataDialog,
        onImportSummaries = onImportSummaries
    )

    LaunchedEffect(highlightSection) {
        if (highlightSection == "ai") {
            lazyListState.animateScrollToItem(index = 2)
        } else if (highlightSection == "3rd-party-service") {
            lazyListState.animateScrollToItem(index = 3)
        }
    }

    // Centred and capped so the list does not stretch across a tablet.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = ContentMaxWidth)
            .padding(horizontal = 16.dp)
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Android 16 Search Bar
            item(key = "search_bar") {
                SettingsSearchBar(
                    query = searchState.query,
                    onQueryChange = searchState::onQueryChange,
                    onClear = searchState::onClear
                )
            }

            if (searchState.isSearching) {
                if (searchState.filteredItems.isEmpty()) {
                    item(key = "empty_search") {
                        SettingsEmptySearchResults(query = searchState.query)
                    }
                } else {
                    item(key = "search_results") {
                        SettingsGroup {
                            searchState.filteredItems.forEachIndexed { index, item ->
                                val position = when {
                                    searchState.filteredItems.size == 1 -> GroupPosition.SINGLE
                                    index == 0 -> GroupPosition.TOP
                                    index == searchState.filteredItems.lastIndex -> GroupPosition.BOTTOM
                                    else -> GroupPosition.MIDDLE
                                }
                                SettingItem(
                                    title = item.title,
                                    subtitle = item.subtitle,
                                    iconBadge = item.iconBadge,
                                    position = position,
                                    trailingContent = item.trailingContent,
                                    onClick = item.onClick,
                                    onLongClick = item.onLongClick,
                                    enabled = item.enabled
                                )
                            }
                        }
                    }
                }
            } else {
                // Group 1: Display & Interface
                item(key = "group_display") {
                    DisplaySettingsGroup(
                        state = state,
                        actions = actions,
                        onShowThemeDialog = onShowThemeDialog
                    )
                }

                // Group 2: AI & Models
                item(key = "group_ai") {
                    AISettingsGroup(
                        state = state,
                        highlighted = highlightSection == "ai",
                        onShowAIProviderDialog = onShowAIProviderDialog,
                        onShowModelDialog = onShowModelDialog,
                        onNav = onNav
                    )
                }

                // Group 3: Services & Tools
                item(key = "group_services") {
                    ServicesSettingsGroup(
                        state = state,
                        actions = actions,
                        highlighted = highlightSection == "3rd-party-service",
                        onShowBiliBiliLoginSheet = onShowBiliBiliLoginSheet,
                        onShowClearBilibiliSessDataDialog = onShowClearBilibiliSessDataDialog
                    )
                }

                // History data: import
                item(key = "group_data") {
                    DataSettingsGroup(onImportSummaries = onImportSummaries, onExportHistory = onExportHistory)
                }

                // Group 4: Help & Community
                item(key = "group_help") {
                    HelpSettingsGroup(
                        onNav = onNav
                    )
                }

                // Footer (App Version & Info)
                item(key = "footer") {
                    val appInfo =
                        "${BuildConfig.VERSION_NAME} - ${BuildConfig.VERSION_CODE} (${BuildConfig.FLAVOR})"
                    val copiedText = stringResource(id = R.string.copied)

                    SettingsFooter(
                        appInfo = appInfo,
                        onCopyAppInfo = {
                            scope.launch {
                                clipboard.setClipEntry(
                                    ClipData.newPlainText("App info", appInfo).toClipEntry()
                                )
                                Toast.makeText(context, copiedText, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScrollContentPreview() {
    UTubeSummaryTheme {
        val state = SettingsUiState(
            theme = 0,
            apiKey = "test_key",
            baseUrl = "",
            activeProvider = AIProvider.OPENAI,
            useOriginalLanguage = false,
            dynamicColor = true,
            showLength = true,
            autoExtractUrl = true
        )
        val actions = SettingsActions(
            onThemeChange = {},
            onApiKeyChange = {},
            onProviderChange = {},
            onModelChange = {},
            onProviderConfigChange = { _, _, _, _ -> },
            onSaveProviderModels = { _, _, _ -> },
            onResetProviderModels = {},
            onUseOriginalLanguageChange = {},
            onDynamicColorChange = {},
            onShowThumbnailsChange = {},
            onShowLengthChange = {},
            onAutoExtractUrlChange = {},
            onBilibiliSessDataChange = { _, _ -> },
            onBilibiliSessDataClear = {}
        )
        Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { innerPadding ->
            SettingsContent(
                innerPadding = innerPadding,
                state = state,
                actions = actions,
                onShowThemeDialog = {},
                onShowAIProviderDialog = {},
                onShowModelDialog = {},
                highlightSection = null,
                onNav = {},
                onShowBiliBiliLoginSheet = {},
                onShowClearBilibiliSessDataDialog = {},
                onImportSummaries = {}
            )
        }
    }
}
