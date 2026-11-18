package io.github.utubesummary

import io.github.utubesummary.R
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import io.github.utubesummary.llm.AIProvider
import io.github.utubesummary.model.SummarizationPhase
import io.github.utubesummary.ui.component.LanguageSelector
import io.github.utubesummary.ui.component.LlmIndicator
import io.github.utubesummary.ui.component.instantDialogHeightFraction
import io.github.utubesummary.ui.component.ProfileSelector
import io.github.utubesummary.ui.component.resolveUserErrorMessage
import io.github.utubesummary.ui.component.summaryShareText
import io.github.utubesummary.ui.theme.UTubeSummaryTheme
import io.github.utubesummary.vm.SettingsUiState
import io.github.utubesummary.vm.SettingsViewModel
import io.github.utubesummary.vm.SummaryViewModel

@AndroidEntryPoint
class InstantSummaryActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val summaryViewModel: SummaryViewModel by viewModels()
    private val textToSummarizeStateFlow = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            val settings by settingsViewModel.settingsUiState.collectAsState()
            val textToSummarize by textToSummarizeStateFlow.collectAsState()

            LaunchedEffect(textToSummarize) {
                textToSummarize?.let {
                    if (it.isNotBlank()) {
                        summaryViewModel.summarize(it)
                    }
                }
            }

            UTubeSummaryTheme(
                darkTheme = when (settings.theme) {
                    1 -> true
                    2 -> false
                    else -> isSystemInDarkTheme()
                },
                dynamicColor = settings.dynamicColor
            ) {
                InstantSummaryDialog(
                    viewModel = summaryViewModel,
                    activeProvider = settings.activeProvider,
                    activeModel = settings.activeModel,
                    onDismiss = { finish() },
                    onOpenInApp = { openInApp() },
                    settings = settings,
                    // Changing a setting here summarizes the same text again with it.
                    onProfileChange = { profileId ->
                        settingsViewModel.selectProfile(profileId)
                        textToSummarize?.takeIf { it.isNotBlank() }?.let {
                            summaryViewModel.summarize(
                                it,
                                profileId = profileId,
                                language = settings.summaryLanguage,
                            )
                        }
                    },
                    onLanguageChange = { language ->
                        settingsViewModel.setSummaryLanguage(language)
                        textToSummarize?.takeIf { it.isNotBlank() }?.let {
                            summaryViewModel.summarize(
                                it,
                                profileId = settings.selectedProfileId,
                                language = language,
                            )
                        }
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val textToSummarize = when (intent?.action) {
            Intent.ACTION_PROCESS_TEXT ->
                intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()

            Intent.ACTION_SEND -> {
                val type = intent.type ?: ""
                if (type.startsWith("application/") || type.startsWith("image/")) {
                    val contentUri: Uri? =
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    contentUri?.toString()
                } else {
                    intent.getStringExtra(Intent.EXTRA_TEXT)
                }
            }

            // Never read the clipboard here: this activity is exported and must not
            // summarize clipboard contents on behalf of other apps.
            else -> null
        }

        if (textToSummarize.isNullOrBlank()) {
            finish()
            return
        }
        textToSummarizeStateFlow.value = textToSummarize
    }

    private fun openInApp() {
        val summaryState = summaryViewModel.summarizationState.value
        val summaryResult = summaryState.summaryResult
        val lengthResults = summaryState.lengthResults

        val targetIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (intent?.action == Intent.ACTION_SEND) {
                action = Intent.ACTION_SEND
                type = intent.type
                intent.extras?.let { putExtras(it) }
                intent.clipData?.let { clipData = it }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                action = Intent.ACTION_SEND
                type = "text/plain"
                val text = textToSummarizeStateFlow.value
                    ?: summaryResult?.sourceLink
                    ?: ""
                putExtra(Intent.EXTRA_TEXT, text)
            }

            if (summaryResult != null) {
                putExtra("auto_trigger", false)
                val effectiveLengthResults = if (lengthResults.isNotEmpty()) {
                    lengthResults + (summaryResult.length to summaryResult)
                } else {
                    mapOf(summaryResult.length to summaryResult)
                }
                // Only an id travels in the intent; the summaries stay in memory (see SummaryHandoff).
                putExtra(
                    "handoff_id",
                    SummaryHandoff.put(SummaryHandoff.Entry(summaryResult, effectiveLengthResults))
                )
            } else {
                putExtra("auto_trigger", true)
            }
        }
        startActivity(targetIntent)
        finish()
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InstantSummaryDialog(
    viewModel: SummaryViewModel,
    activeProvider: AIProvider? = null,
    activeModel: String? = null,
    onDismiss: () -> Unit,
    onOpenInApp: () -> Unit,
    settings: SettingsUiState? = null,
    onProfileChange: (String) -> Unit = {},
    onLanguageChange: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val summarizationState by viewModel.summarizationState.collectAsState()
    val isLoading = summarizationState.isLoading
    val summaryResult = summarizationState.summaryResult
    val error = summarizationState.error
    val clipboard = LocalClipboard.current
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize
    val configuration = LocalConfiguration.current
    // A landscape window is short, so the dialog may use more of it.
    val heightFraction = instantDialogHeightFraction(
        widthDp = configuration.screenWidthDp,
        heightDp = configuration.screenHeightDp
    )
    val maxHeight = with(density) { (containerSize.height * heightFraction).toDp() }
    val scope = rememberCoroutineScope()

    val effectiveProvider = summaryResult?.provider?.let { p ->
        AIProvider.entries.find { it.name == p }
    } ?: activeProvider
    val effectiveModel = summaryResult?.model ?: activeModel

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .padding(horizontal = 16.dp)
                    .heightIn(max = maxHeight),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Header: App icon / title + Dismiss Close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            LlmIndicator(
                                provider = effectiveProvider,
                                model = effectiveModel,
                                boxSize = 32.dp,
                                iconSize = 20.dp,
                                fontSize = 7.sp,
                            )
                            Text(
                                text = summaryResult?.title?.takeIf { it.isNotBlank() }
                                    ?: stringResource(R.string.instant_summarize),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.clear),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (settings != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (settings.profiles.size > 1) {
                                ProfileSelector(
                                    profiles = settings.profiles,
                                    selectedProfileId = settings.selectedProfileId,
                                    onProfileSelect = onProfileChange,
                                    enabled = !isLoading,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                            }
                            LanguageSelector(
                                selectedLanguage = settings.summaryLanguage,
                                onLanguageSelect = onLanguageChange,
                                enabled = !isLoading,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    when {
                        isLoading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 140.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LoadingIndicator(modifier = Modifier.size(56.dp))
                                    summarizationState.phase?.let { phase ->
                                        Text(
                                            text = stringResource(
                                                if (phase == SummarizationPhase.EXTRACTING) R.string.phase_extracting else R.string.phase_summarizing
                                            ),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        error != null -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.error_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = error.resolveUserErrorMessage(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = onOpenInApp,
                                        shape = CircleShape,
                                    ) {
                                        Text(
                                            text = stringResource(R.string.open_in_app),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        summaryResult != null -> {
                            // Scrollable Summary Content
                            Column(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = summaryResult.summary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Bottom Action Bar: Copy, Share, Open in App
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                clipboard.setClipEntry(
                                                    ClipData.newPlainText(
                                                        "User Input",
                                                        summaryResult.summary
                                                    ).toClipEntry()
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = stringResource(R.string.copied),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    summaryShareText(summaryResult)
                                                )
                                                type = "text/plain"
                                            }
                                            context.startActivity(
                                                Intent.createChooser(sendIntent, null)
                                            )
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = stringResource(R.string.share),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Button(
                                    onClick = onOpenInApp,
                                    shape = CircleShape,
                                ) {
                                    Text(
                                        text = stringResource(R.string.open_in_app),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}