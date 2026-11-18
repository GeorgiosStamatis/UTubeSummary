package io.github.utubesummary

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import io.github.utubesummary.ui.page.instant.InstantSummaryDialog
import io.github.utubesummary.ui.theme.UTubeSummaryTheme
import io.github.utubesummary.vm.SettingsViewModel
import io.github.utubesummary.vm.SummaryViewModel
import kotlinx.coroutines.flow.MutableStateFlow

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

            Intent.ACTION_SEND -> sharedContentOf(
                mimeType = intent.type,
                text = { intent.getStringExtra(Intent.EXTRA_TEXT) },
                streamUri = {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)?.toString()
                },
            )

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
