package io.github.utubesummary.ui.page.settings.dialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import io.github.utubesummary.R
import io.github.utubesummary.llm.AIProvider
import io.github.utubesummary.model.ProviderConfig
import io.github.utubesummary.ui.component.ClickablePasteIcon
import io.github.utubesummary.ui.component.ReorderDragHandle
import io.github.utubesummary.ui.component.ReorderableItem
import io.github.utubesummary.ui.component.ReorderableListState
import io.github.utubesummary.ui.component.rememberReorderableListState
import io.github.utubesummary.ui.theme.UTubeSummaryTheme

@Composable
fun ModelSettingsDialog(
    onDismissRequest: () -> Unit,
    provider: AIProvider,
    initialModelId: String?,
    providerConfigs: Map<String, ProviderConfig>,
    onConfirm: (models: List<String>, selectedModel: String) -> Unit,
) {
    val initialEffectiveModels = remember(provider, providerConfigs) {
        val config = providerConfigs[provider.name]
        provider.getEffectiveModels(config)
    }
    val savedModelsList = remember(initialEffectiveModels) { initialEffectiveModels }
    var modelsList by remember(initialEffectiveModels) { mutableStateOf(initialEffectiveModels) }

    val savedSelectedModel = remember(initialModelId, initialEffectiveModels) {
        initialModelId?.takeIf { it in initialEffectiveModels }
            ?: initialEffectiveModels.firstOrNull()
            ?: ""
    }
    var selectedModel by remember(savedSelectedModel) { mutableStateOf(savedSelectedModel) }

    var query by remember { mutableStateOf("") }
    var editingModel by remember { mutableStateOf<String?>(null) }
    var editingModelText by remember { mutableStateOf("") }

    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val isChanged = modelsList != savedModelsList || selectedModel != savedSelectedModel
    val visibleModels = filterModels(modelsList, query)
    val typedModel = typedModelToUse(modelsList, query)
    val isFiltering = query.isNotBlank()
    val focusManager = LocalFocusManager.current
    val searchLabel = stringResource(R.string.searchModel)
    val noMatchText = stringResource(R.string.noModelsMatch)

    val reorderState = rememberReorderableListState(lazyListState) { fromIndex, toIndex ->
        // Indexes belong to the displayed list; they only match the full list when nothing is filtered.
        if (!isFiltering) {
            val updatedList = modelsList.toMutableList()
            val item = updatedList.removeAt(fromIndex)
            updatedList.add(toIndex, item)
            modelsList = updatedList
        }
    }

    val useTypedModel = { name: String ->
        if (name !in modelsList) {
            modelsList = listOf(name) + modelsList
        }
        selectedModel = name
        query = ""
        scope.launch {
            lazyListState.animateScrollToItem(0)
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = stringResource(id = R.string.manageModels) + " (${provider.id.display})",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.heightIn(max = 480.dp)) {
                // Search the provider's model list
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(searchLabel) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    shape = MaterialTheme.shapes.large,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        ClickablePasteIcon(
                            text = query,
                            onPaste = { query = it.trim() },
                            onClear = { query = "" }
                        )
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        visibleModels.singleOrNull()?.let { selectedModel = it }
                        focusManager.clearFocus()
                    })
                )

                typedModel?.let { name ->
                    Spacer(modifier = Modifier.height(8.dp))
                    UseTypedModelRow(name = name, onClick = { useTypedModel(name) })
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (visibleModels.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (modelsList.isEmpty()) {
                                stringResource(R.string.no_models_configured)
                            } else {
                                noMatchText
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(visibleModels, key = { _, item -> item }) { _, model ->
                            ModelListItem(
                                model = model,
                                isSelected = selectedModel == model,
                                isEditing = editingModel == model,
                                editingText = editingModelText,
                                reorderState = reorderState,
                                canReorder = !isFiltering,
                                onSelect = { selectedModel = model },
                                onStartEdit = {
                                    editingModel = model
                                    editingModelText = model
                                },
                                onEditingTextChange = { editingModelText = it },
                                onSaveEdit = { text ->
                                    val trimmed = text.trim()
                                    val index = modelsList.indexOf(model)
                                    if (trimmed.isNotBlank() && index >= 0) {
                                        val updatedList = modelsList.toMutableList()
                                        updatedList[index] = trimmed
                                        modelsList = updatedList
                                        if (selectedModel == model) {
                                            selectedModel = trimmed
                                        }
                                    }
                                    editingModel = null
                                },
                                onCancelEdit = { editingModel = null },
                                onDelete = {
                                    val index = modelsList.indexOf(model)
                                    if (index >= 0) {
                                        val updatedList = modelsList.toMutableList()
                                        updatedList.removeAt(index)
                                        modelsList = updatedList
                                        if (selectedModel == model) {
                                            selectedModel = updatedList.getOrNull(
                                                index.coerceAtMost(updatedList.size - 1)
                                            ) ?: ""
                                        }
                                    }
                                    if (editingModel == model) {
                                        editingModel = null
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(modelsList, selectedModel)
                },
                enabled = modelsList.isNotEmpty() && selectedModel.isNotBlank() && selectedModel in modelsList
            ) {
                Text(stringResource(id = R.string.ok))
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        modelsList = savedModelsList
                        selectedModel = savedSelectedModel
                        editingModel = null
                        query = ""
                    },
                    enabled = isChanged
                ) {
                    Text(stringResource(id = R.string.reset))
                }
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        },
    )
}

@Composable
private fun LazyItemScope.ModelListItem(
    model: String,
    isSelected: Boolean,
    isEditing: Boolean,
    editingText: String,
    reorderState: ReorderableListState,
    canReorder: Boolean,
    onSelect: () -> Unit,
    onStartEdit: () -> Unit,
    onEditingTextChange: (String) -> Unit,
    onSaveEdit: (String) -> Unit,
    onCancelEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    ReorderableItem(
        reorderState = reorderState,
        key = model,
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                }
            ),
            border = if (isSelected) {
                BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isEditing) {
                ModelEditRow(
                    text = editingText,
                    onTextChange = onEditingTextChange,
                    onSave = { onSaveEdit(editingText) },
                    onCancel = onCancelEdit
                )
            } else {
                ModelDisplayRow(
                    model = model,
                    isSelected = isSelected,
                    reorderState = reorderState,
                    canReorder = canReorder,
                    onSelect = onSelect,
                    onStartEdit = onStartEdit,
                    onDelete = onDelete
                )
            }
        }
    }
}

@Composable
private fun ModelEditRow(
    text: String,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            maxLines = 3,
            shape = MaterialTheme.shapes.medium
        )
        IconButton(
            onClick = onSave,
            enabled = text.isNotBlank(),
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = stringResource(R.string.save_edit),
                modifier = Modifier.size(20.dp)
            )
        }
        IconButton(
            onClick = onCancel,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = stringResource(R.string.cancel_edit),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ModelDisplayRow(
    model: String,
    isSelected: Boolean,
    reorderState: ReorderableListState,
    canReorder: Boolean,
    onSelect: () -> Unit,
    onStartEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .clickable(onClick = onSelect)
                .padding(
                    start = 8.dp,
                    end = 4.dp,
                    top = 6.dp,
                    bottom = 6.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = model,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp)
            )
            IconButton(
                onClick = onStartEdit,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = stringResource(R.string.editModel),
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = stringResource(R.string.deleteModel),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
            if (canReorder) {
                ReorderDragHandle(
                    reorderState = reorderState,
                    key = model,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Spacer(modifier = Modifier.size(28.dp))
            }
        }
    }
}

/** "Use <name>": picks a model the provider's list does not contain. */
@Composable
private fun UseTypedModelRow(name: String, onClick: () -> Unit) {
    val text = stringResource(R.string.useTypedModel, name)
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview
@Composable
private fun ModelSettingsDialogPreview() {
    UTubeSummaryTheme {
        ModelSettingsDialog(
            onDismissRequest = {},
            provider = AIProvider.OPENAI,
            initialModelId = AIProvider.OPENAI.models.first().id,
            providerConfigs = emptyMap(),
            onConfirm = { _, _ -> },
        )
    }
}
