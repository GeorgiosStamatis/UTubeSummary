package io.github.utubesummary

import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.navigation3.runtime.rememberNavBackStack
import dagger.hilt.android.AndroidEntryPoint
import io.github.utubesummary.ui.AppNavigation
import io.github.utubesummary.ui.Nav
import io.github.utubesummary.ui.theme.UTubeSummaryTheme
import io.github.utubesummary.vm.AppStartAction
import io.github.utubesummary.vm.AppViewModel
import io.github.utubesummary.vm.SettingsViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        // Check if a link has been shared
        handleIntent(intent)

        setContent {
            val settingsState by settingsViewModel.settingsUiState.collectAsState()
            val isOnboarded by appViewModel.isOnboarded.collectAsState()

            UTubeSummaryTheme(
                darkTheme = when (settingsState.theme) {
                    1 -> true
                    2 -> false
                    else -> isSystemInDarkTheme()
                },
                dynamicColor = settingsState.dynamicColor
            ) {
                if (isOnboarded == null) {
                    return@UTubeSummaryTheme
                }
                val startDestination = if (isOnboarded == true) Nav.Home else Nav.Onboarding
                val backStack = rememberNavBackStack(startDestination)
                AppNavigation(
                    backStack = backStack,
                    appViewModel = appViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val type = intent.type
                val autoTrigger = intent.getBooleanExtra(EXTRA_AUTO_TRIGGER, false)
                val handoffId = intent.getStringExtra(EXTRA_HANDOFF_ID)
                val handoff = handoffId?.let { SummaryHandoff.take(it) }
                val initialSummary = handoff?.summary
                val lengthResults = handoff?.lengthResults ?: emptyMap()

                // If the summary is gone (the app process was restarted in between), summarize again.
                val startsSummary = shouldAutoTrigger(initialSummary != null, autoTrigger, handoffId)

                val content = sharedContentOf(
                    mimeType = type,
                    text = { intent.getStringExtra(Intent.EXTRA_TEXT) },
                    streamUri = {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)?.toString()
                    },
                )
                // A file share without a file link has nothing to summarize.
                if (content != null || !isFileShare(type)) {
                    appViewModel.onEvent(
                        AppStartAction(
                            content = content,
                            autoTrigger = startsSummary,
                            initialSummary = initialSummary,
                            lengthResults = lengthResults,
                        )
                    )
                }

                // Clear intent action so recreating activity won't re-trigger
                intent.action = null
            }

            Intent.ACTION_VIEW -> {
                if (intent.data?.host != "clipboard") return
                // To avoid re-triggering on configuration change, we clear the data.
                intent.data = null
                // Postpone clipboard access until the window has focus.
                // Fill the input only; the user must tap summarize to send anything.
                window.decorView.post {
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.primaryClip?.getItemAt(0)?.text?.let {
                        appViewModel.onEvent(AppStartAction(content = it.toString()))
                    }
                }
            }
        }
    }
}