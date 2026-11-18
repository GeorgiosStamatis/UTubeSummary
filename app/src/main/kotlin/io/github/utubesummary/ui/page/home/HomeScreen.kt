package io.github.utubesummary.ui.page.home

import android.content.ClipData
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.toClipEntry
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.github.utubesummary.exception.SummaryException
import io.github.utubesummary.llm.tools.getFileName
import io.github.utubesummary.model.SummaryLength
import io.github.utubesummary.ui.Nav
import io.github.utubesummary.ui.component.rememberIsWideLayout
import io.github.utubesummary.ui.page.home.action.HomeFloatingActionButtons
import io.github.utubesummary.ui.page.home.appbar.HomeTopAppBar
import io.github.utubesummary.ui.page.home.sheet.ProviderModelBottomSheet
import io.github.utubesummary.vm.AppViewModel
import io.github.utubesummary.vm.SettingsViewModel
import io.github.utubesummary.vm.SummaryViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNav: (dest: Nav) -> Unit = {},
    appViewModel: AppViewModel,
    settingsViewModel: SettingsViewModel,
    summaryViewModel: SummaryViewModel = hiltViewModel<SummaryViewModel>(),
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val haptics = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    var urlOrText by rememberSaveable { mutableStateOf("") }
    // Saved so a picked file and a camera photo survive Android ending the app's process.
    var documentFilename by rememberSaveable { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var cameraImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var showProviderModelSheet by rememberSaveable { mutableStateOf(false) }
    var isScrollingDown by rememberSaveable { mutableStateOf(false) }

    val settings by settingsViewModel.settingsUiState.collectAsState()
    val summarizationState by summaryViewModel.summarizationState.collectAsState()
    val chatState by summaryViewModel.chatState.collectAsState()
    val appStartAction by appViewModel.appStartAction.collectAsState()

    fun summarize() {
        focusManager.clearFocus()
        isScrollingDown = false
        summaryViewModel.summarize(
            urlOrText,
            profileId = settings.selectedProfileId,
            language = settings.summaryLanguage,
        )
    }

    fun clearInput() {
        urlOrText = ""
        summaryViewModel.clearCurrentSummary()
        focusRequester.requestFocus()
        documentFilename = null
        isScrollingDown = false
    }

    val filePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            // Keep read access after the process ends; not every provider allows it.
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            scope.launch {
                summaryViewModel.clearCurrentSummary()
                documentFilename = getFileName(context, uri)
                urlOrText = uri.toString()
            }
        }

    val imagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                summaryViewModel.clearCurrentSummary()
                documentFilename = getFileName(context, uri)
                urlOrText = uri.toString()
            }
        }

    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                cameraImageUri?.let { uri ->
                    scope.launch {
                        summaryViewModel.clearCurrentSummary()
                        documentFilename = getFileName(context, uri)
                        urlOrText = uri.toString()
                    }
                }
            }
        }

    LaunchedEffect(appStartAction) {
        if (appStartAction.content != null || appStartAction.initialSummary != null) {
            val initialSummary = appStartAction.initialSummary
            val content = appStartAction.content ?: initialSummary?.sourceLink.orEmpty()
            urlOrText = content
            if (content.startsWith("content://") || content.startsWith("file://")) {
                documentFilename =
                    runCatching { getFileName(context, Uri.parse(content)) }.getOrNull()
            } else {
                documentFilename = null
            }

            if (initialSummary != null) {
                if (initialSummary.length != SummaryLength.NONE) {
                    settingsViewModel.setSummaryLength(initialSummary.length)
                }
                summaryViewModel.setExistingSummary(
                    output = initialSummary,
                    input = content,
                    lengthResults = appStartAction.lengthResults
                )
            } else {
                summaryViewModel.clearCurrentSummary()
                if (appStartAction.autoTrigger) {
                    summarize()
                }
            }
            appViewModel.onStartActionHandled()
        }
    }

    LaunchedEffect(summarizationState.summaryResult) {
        isPlaying = false
    }

    LaunchedEffect(summarizationState.error) {
        isPlaying = false
        summarizationState.error?.let { error ->
            if (error is SummaryException.BiliBiliLoginRequiredException) {
                onNav(Nav.Settings(highlight = "3rd-party-service"))
                summaryViewModel.clearCurrentSummary()
            }
        }
    }

    LaunchedEffect(settings.summaryLength, settings.showLength) {
        if (settings.showLength && settings.summaryLength != SummaryLength.NONE) {
            summaryViewModel.switchLength(settings.summaryLength)
        }
    }

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val snackBarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    val fabNestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -5f) {
                    isScrollingDown = true
                } else if (available.y > 5f) {
                    isScrollingDown = false
                }
                return Offset.Zero
            }
        }
    }

    // On wide screens both columns scroll on their own, so the button stays visible.
    val isWide = rememberIsWideLayout()
    val fabVisible by remember(isWide) {
        derivedStateOf {
            isWide || !isScrollingDown || !listState.canScrollBackward
        }
    }

    val hasInput = remember(urlOrText, documentFilename) {
        documentFilename != null || urlOrText.isNotBlank()
    }
    val hasResult = summarizationState.summaryResult?.summary?.isNotEmpty() == true
    // A result from a known, different length, profile or language is stale. Unknown (restored
    // from history) is not.
    val isDirty = isResultStale(
        result = ResultSettings(
            hasResult = summarizationState.summaryResult != null,
            resultLength = summarizationState.summaryResult?.length,
            resultProfileId = summarizationState.profileId,
            resultLanguage = summarizationState.language,
        ),
        selected = SelectedSettings(
            showLength = settings.showLength,
            length = settings.summaryLength,
            profileId = settings.selectedProfileId,
            language = settings.summaryLanguage,
        ),
    )

    val actions = HomeActions(
        onSummarize = {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            summarize()
        },
        onClearInput = { clearInput() },
        onUrlChange = { urlOrText = it },
        onLengthSelect = { length ->
            settingsViewModel.setSummaryLength(length)
            summaryViewModel.switchLength(length)
        },
        onProfileSelect = settingsViewModel::selectProfile,
        onLanguageSelect = settingsViewModel::setSummaryLanguage,
        onCancelSummarize = summaryViewModel::cancelSummarize,
        onPasteFromClipboard = {
            clearInput()
            scope.launch {
                clipboard.getClipEntry()?.let { clipEntry ->
                    urlOrText = clipEntry.clipData.getItemAt(0).text.toString()
                }
            }
        },
        onPlaySummary = { isPlaying = !isPlaying },
        onCopySummary = { text ->
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            scope.launch {
                clipboard.setClipEntry(
                    ClipData.newPlainText("User Input", text).toClipEntry()
                )
            }
        },
        onShowSnackBar = { message ->
            scope.launch { snackBarHostState.showSnackbar(message) }
        },
        onLaunchFilePicker = { filePickerLauncher.launch(MimeTypes.allSupported) },
        onLaunchImagePicker = { imagePickerLauncher.launch("image/*") },
        onLaunchCamera = {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "new_image.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )
            uri?.let {
                cameraImageUri = it
                cameraLauncher.launch(it)
            }
        },
        onSelectProvider = { settingsViewModel.setAIProviderValue(it.name) },
        onSelectModel = { settingsViewModel.setModel(it) },
        onSelectProviderAndModel = { provider, model ->
            settingsViewModel.setProviderAndModel(provider, model)
        },
        onShowProviderModelSheet = { showProviderModelSheet = true },
        onDismissProviderModelSheet = { showProviderModelSheet = false },
        onAskQuestion = summaryViewModel::askQuestion,
    )

    if (showProviderModelSheet) {
        ProviderModelBottomSheet(
            settings = settings,
            onProviderSelect = actions.onSelectProvider,
            onModelSelect = actions.onSelectModel,
            onDismiss = actions.onDismissProviderModelSheet,
            onGoToSettings = { onNav(Nav.Settings(highlight = "ai")) }
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .nestedScroll(fabNestedScrollConnection),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            HomeTopAppBar(
                scrollBehavior = scrollBehavior,
                settings = settings,
                onNav = onNav,
                onIndicatorClick = actions.onShowProviderModelSheet,
                onConfirmSwitch = actions.onSelectProviderAndModel
            )
        },
        snackbarHost = { SnackbarHost(snackBarHostState) },
        floatingActionButton = {
            HomeFloatingActionButtons(
                fabVisible = fabVisible,
                hasInput = hasInput,
                onPaste = actions.onPasteFromClipboard,
                onSummarize = actions.onSummarize,
                isLoading = summarizationState.isLoading,
                hasResult = hasResult,
                isDirty = isDirty,
                onShowSnackBar = actions.onShowSnackBar,
                onLaunchFilePicker = actions.onLaunchFilePicker,
                onLaunchImagePicker = actions.onLaunchImagePicker,
                onLaunchCamera = actions.onLaunchCamera
            )
        }
    ) { innerPadding ->
        HomeContent(
            innerPadding = innerPadding,
            listState = listState,
            urlOrText = urlOrText,
            documentFilename = documentFilename,
            focusRequester = focusRequester,
            settings = settings,
            summarizationState = summarizationState,
            isPlaying = isPlaying,
            actions = actions,
            chatState = chatState,
        )
    }
}
