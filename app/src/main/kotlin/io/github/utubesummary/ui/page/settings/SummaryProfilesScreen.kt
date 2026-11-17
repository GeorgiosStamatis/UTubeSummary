package io.github.utubesummary.ui.page.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.utubesummary.R
import io.github.utubesummary.model.DEFAULT_PROFILE_ID
import io.github.utubesummary.model.SummaryProfile
import io.github.utubesummary.ui.component.displayName
import io.github.utubesummary.vm.SettingsViewModel

/** Marker for "creating a new profile" while editing. */
private const val NEW_PROFILE = "<new>"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryProfilesScreen(
    onBack: () -> Unit,
    settingsViewModel: SettingsViewModel,
) {
    val state by settingsViewModel.settingsUiState.collectAsState()
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    val editing = state.profiles.firstOrNull { it.id == editingId }

    BackHandler(enabled = editingId != null) { editingId = null }

    if (editingId != null) {
        ProfileEditor(
            profile = editing,
            onBack = { editingId = null },
            onSave = { name, prompt ->
                settingsViewModel.saveProfile(editing?.id, name, prompt)
                editingId = null
            },
            onDeleteOrReset = {
                editing?.let { settingsViewModel.deleteOrResetProfile(it.id) }
                editingId = null
            },
        )
        return
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.manage_profiles),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.profile_back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editingId = NEW_PROFILE }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.profile_new))
            }
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
                items(state.profiles, key = { it.id }) { profile ->
                    ProfileRow(
                        profile = profile,
                        selected = profile.id == state.selectedProfileId,
                        onClick = {
                            if (profile.id == DEFAULT_PROFILE_ID) {
                                settingsViewModel.selectProfile(profile.id)
                            } else {
                                editingId = profile.id
                            }
                        },
                        onSelect = { settingsViewModel.selectProfile(profile.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(
    profile: SummaryProfile,
    selected: Boolean,
    onClick: () -> Unit,
    onSelect: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(profile.displayName()) },
        supportingContent = {
            val text = when {
                profile.id == DEFAULT_PROFILE_ID -> stringResource(R.string.profile_default_hint)
                profile.isModified -> stringResource(R.string.profile_modified)
                else -> profile.basePrompt.lineSequence().firstOrNull().orEmpty()
            }
            Text(text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        trailingContent = {
            IconButton(onClick = onSelect) {
                if (selected) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileEditor(
    profile: SummaryProfile?,
    onBack: () -> Unit,
    onSave: (name: String, prompt: String) -> Unit,
    onDeleteOrReset: () -> Unit,
) {
    val displayName = profile?.displayName().orEmpty()
    var name by rememberSaveable(profile?.id) { mutableStateOf(displayName) }
    var prompt by rememberSaveable(profile?.id) { mutableStateOf(profile?.basePrompt.orEmpty()) }
    val canSave = name.isNotBlank() && prompt.isNotBlank()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(if (profile == null) R.string.profile_new else R.string.profile_edit),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.profile_back)
                        )
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.profile_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text(stringResource(R.string.profile_prompt)) },
                supportingText = { Text(stringResource(R.string.profile_prompt_hint)) },
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        // Keep following the localized name of a built-in profile unless renamed.
                        val unchangedBuiltInName = profile?.name == null && name == displayName
                        onSave(if (unchangedBuiltInName && profile != null) "" else name, prompt)
                    },
                    enabled = canSave
                ) {
                    Text(stringResource(R.string.profile_save))
                }
                if (profile != null) {
                    // Custom profiles are deleted; built-in ones can only be reset to the preset.
                    val isReset = profile.isBuiltIn
                    if (!isReset || profile.isModified) {
                        OutlinedButton(onClick = onDeleteOrReset) {
                            Text(stringResource(if (isReset) R.string.profile_reset else R.string.profile_delete))
                        }
                    }
                }
            }
        }
    }
}
