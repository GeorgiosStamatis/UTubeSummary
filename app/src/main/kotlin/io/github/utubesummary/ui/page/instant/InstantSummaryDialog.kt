package io.github.utubesummary.ui.page.instant

import android.content.ClipData
import android.content.Intent
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.utubesummary.R
import io.github.utubesummary.llm.AIProvider
import io.github.utubesummary.model.SummarizationPhase
import io.github.utubesummary.ui.component.LanguageSelector
import io.github.utubesummary.ui.component.LlmIndicator
import io.github.utubesummary.ui.component.ProfileSelector
import io.github.utubesummary.ui.component.instantDialogHeightFraction
import io.github.utubesummary.ui.component.resolveUserErrorMessage
import io.github.utubesummary.ui.component.summaryShareText
import io.github.utubesummary.vm.SettingsUiState
import io.github.utubesummary.vm.SummaryViewModel
import kotlinx.coroutines.launch

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
