package io.github.utubesummary.ui.page.settings

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.rounded.Tune
import io.github.utubesummary.model.DEFAULT_PROFILE_ID
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ShortText
import androidx.compose.material.icons.rounded.PostAdd
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.utubesummary.R
import io.github.utubesummary.llm.defaultSystemPromptPlaceholder
import io.github.utubesummary.llm.generateFinalPromptString
import io.github.utubesummary.vm.SettingsViewModel
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSummarySetupScreen(
    onBack: () -> Unit,
    settingsViewModel: SettingsViewModel,
    onOpenProfiles: () -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val state by settingsViewModel.settingsUiState.collectAsState()
    // Edit locally and persist in the background: binding the text field straight to the
    // DataStore-backed state makes typed characters lag/vanish and the cursor jump.
    var basePrompt by rememberPersistedText(
        saved = state.customBasePrompt,
        onSave = settingsViewModel::setCustomBasePrompt
    )
    var additionalPrompt by rememberPersistedText(
        saved = state.additionalSystemPrompt,
        onSave = settingsViewModel::setAdditionalSystemPrompt
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.advancedSummarySetup),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.go_back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp),
                contentPadding = innerPadding
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onOpenProfiles),
                            leadingContent = {
                                Icon(
                                    Icons.Rounded.Tune,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            supportingContent = { Text(stringResource(id = R.string.manage_profiles_description)) },
                        ) { Text(stringResource(id = R.string.manage_profiles)) }

                        Text(
                            text = stringResource(id = R.string.basePrompt),
                            style = MaterialTheme.typography.titleMedium
                        )
                        OutlinedTextField(
                            value = if (state.isAppendMode) defaultSystemPromptPlaceholder else basePrompt,
                            onValueChange = { basePrompt = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 240.dp),
                            shape = MaterialTheme.shapes.large,
                            readOnly = state.isAppendMode,
                            enabled = !state.isAppendMode
                        )

                        AnimatedVisibility(visible = !state.isAppendMode) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                            ) {
                                FilledTonalButton(
                                    onClick = { basePrompt = "" },
                                    enabled = basePrompt.isNotEmpty()
                                ) {
                                    Text(stringResource(R.string.clear))
                                }
                                FilledTonalButton(
                                    onClick = { basePrompt = defaultSystemPromptPlaceholder },
                                    enabled = basePrompt != defaultSystemPromptPlaceholder
                                ) {
                                    Text(stringResource(R.string.reset_to_default))
                                }
                            }
                        }

                        HorizontalDivider()

                        Text(
                            text = stringResource(id = R.string.additionalConfigurations),
                            style = MaterialTheme.typography.titleMedium
                        )

                        ListItem(
                            modifier = Modifier.fillMaxWidth(),
                            leadingContent = {
                                Icon(
                                    Icons.Rounded.PostAdd,
                                    contentDescription = "Append Custom Prompt",
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            supportingContent = { Text(stringResource(id = R.string.appendCustomPromptDescription)) },
                            trailingContent = {
                                Switch(
                                    checked = state.isAppendMode,
                                    onCheckedChange = { settingsViewModel.setIsAppendMode(it) }
                                )
                            }
                        ) { Text(stringResource(id = R.string.appendCustomPrompt)) }

                        AnimatedVisibility(visible = state.isAppendMode) {
                            OutlinedTextField(
                                value = additionalPrompt,
                                onValueChange = { additionalPrompt = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 80.dp, max = 160.dp),
                                shape = MaterialTheme.shapes.large,
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.additional_instructions_hint),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            )
                        }

                        ListItem(
                            modifier = Modifier.fillMaxWidth(),
                            leadingContent = {
                                Icon(
                                    Icons.Rounded.Translate,
                                    contentDescription = "Language Settings",
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = state.useOriginalLanguage,
                                    onCheckedChange = {
                                        settingsViewModel.setUseOriginalLanguageValue(
                                            it
                                        )
                                    }
                                )
                            },
                            supportingContent = { Text(stringResource(id = R.string.useOriginalLanguageDescription)) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        ) { Text(stringResource(id = R.string.useOriginalLanguage)) }

                        ListItem(
                            modifier = Modifier.fillMaxWidth(),
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            supportingContent = { Text(stringResource(id = R.string.useLengthOptionsDescription)) },
                            leadingContent = {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ShortText,
                                    contentDescription = "Length Options",
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = state.showLength,
                                    onCheckedChange = { settingsViewModel.setShowLengthValue(it) }
                                )
                            }
                        ) { Text(stringResource(id = R.string.useLengthOptions)) }

                        HorizontalDivider()

                        Text(
                            text = stringResource(id = R.string.finalPromptPreview),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        val appLanguage =
                            LocalLocale.current.platformLocale.getDisplayLanguage(Locale.ENGLISH)
                        val finalPromptPreview = generateFinalPromptString(
                            length = state.summaryLength,
                            showLength = state.showLength,
                            useContentLanguage = state.useOriginalLanguage,
                            appLanguage = appLanguage,
                            isAppendMode = state.isAppendMode,
                            customBasePrompt = basePrompt,
                            additionalSystemPrompt = additionalPrompt,
                            profileBasePrompt = state.profiles
                                .firstOrNull { it.id == state.selectedProfileId }
                                ?.takeIf { it.id != DEFAULT_PROFILE_ID }
                                ?.basePrompt,
                        )

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = finalPromptPreview,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}



private const val PERSIST_DELAY_MS = 400L

/**
 * Text state for an editable field backed by an asynchronously persisted value.
 *
 * The returned state updates synchronously with every keystroke and is saved after a short
 * pause (and when leaving the screen). A new [saved] value is adopted only if it was not
 * produced by this state's own writes, e.g. the initial load or a reset.
 */
@Composable
private fun rememberPersistedText(
    saved: String,
    onSave: (String) -> Unit,
): MutableState<String> {
    val text = remember { mutableStateOf(saved) }
    var lastSent by remember { mutableStateOf(saved) }
    val currentOnSave by rememberUpdatedState(onSave)

    LaunchedEffect(saved) {
        if (saved != lastSent) {
            lastSent = saved
            text.value = saved
        }
    }
    LaunchedEffect(text.value) {
        if (text.value != lastSent) {
            delay(PERSIST_DELAY_MS)
            lastSent = text.value
            currentOnSave(text.value)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            if (text.value != lastSent) {
                lastSent = text.value
                currentOnSave(text.value)
            }
        }
    }
    return text
}
