package io.github.utubesummary.ui.page.settings.dialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.utubesummary.R
import io.github.utubesummary.llm.AIProvider
import io.github.utubesummary.model.ProviderConfig
import io.github.utubesummary.vm.ProviderTestState
import io.github.utubesummary.model.ProviderTestResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import io.github.utubesummary.ui.component.ClickablePasteIcon
import io.github.utubesummary.ui.component.ReorderDragHandle
import io.github.utubesummary.ui.component.ReorderableItem
import io.github.utubesummary.ui.component.ReorderableListState
import io.github.utubesummary.ui.component.rememberReorderableListState
import io.github.utubesummary.ui.theme.UTubeSummaryTheme

@Composable
fun AIProviderSettingsDialog(
    onDismissRequest: () -> Unit,
    initialProvider: AIProvider,
    providerConfigs: Map<String, ProviderConfig>,
    providerOrder: List<String> = emptyList(),
    onConfirm: (provider: AIProvider, baseUrl: String, apiKey: String, providerOrder: List<String>) -> Unit,
    onNext: (provider: AIProvider, baseUrl: String, apiKey: String, providerOrder: List<String>) -> Unit,
    testState: ProviderTestState = ProviderTestState.Idle,
    onTest: (provider: AIProvider, baseUrl: String, apiKey: String) -> Unit = { _, _, _ -> },
    onTestReset: () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val apiKeyFocusRequester = remember { FocusRequester() }
    var selectedProvider by remember { mutableStateOf(initialProvider) }

    val initialEffectiveProviders = remember(providerOrder) {
        AIProvider.getEffectiveProviders(providerOrder)
    }
    var providersList by remember(initialEffectiveProviders) {
        mutableStateOf(initialEffectiveProviders)
    }

    // Built-in providers show their official URL (editable); it is saved as "default".
    val locale = LocalConfiguration.current.locales[0]
    val defaultBaseUrl = selectedProvider.defaultBaseUrl(locale).orEmpty()
    val initialBaseUrl = providerConfigs[selectedProvider.name]?.baseUrl
        ?.takeIf { it.isNotBlank() } ?: defaultBaseUrl

    var baseUrlTextFieldValue by remember(selectedProvider) {
        mutableStateOf(initialBaseUrl)
    }
    var apiKeyTextFieldValue by remember(selectedProvider) {
        mutableStateOf(providerConfigs[selectedProvider.name]?.apiKey ?: "")
    }

    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableListState(lazyListState) { fromIndex, toIndex ->
        val updatedList = providersList.toMutableList()
        val item = updatedList.removeAt(fromIndex)
        updatedList.add(toIndex, item)
        providersList = updatedList
    }

    val formValid = when {
        selectedProvider.isMandatoryBaseUrl -> baseUrlTextFieldValue.isNotBlank()
        selectedProvider.isRequiredApiKey -> apiKeyTextFieldValue.isNotBlank()
        else -> true
    }
    val providerChanged = selectedProvider != initialProvider
    val orderChanged = providersList != initialEffectiveProviders
    val configChanged =
        baseUrlTextFieldValue != initialBaseUrl ||
                apiKeyTextFieldValue != (providerConfigs[selectedProvider.name]?.apiKey ?: "")

    // A successful Test validates the current inputs: allow OK, and don't offer Test again.
    val testPassed = (testState as? ProviderTestState.Done)?.result is ProviderTestResult.Success
    val canSubmit = if (providerChanged || configChanged) formValid else (orderChanged || testPassed)

    // A previous test result no longer applies once the inputs change or the dialog closes.
    LaunchedEffect(selectedProvider, baseUrlTextFieldValue, apiKeyTextFieldValue) {
        onTestReset()
    }
    DisposableEffect(Unit) { onDispose { onTestReset() } }

    val submit = {
        val orderNames = providersList.map { it.name }
        if (providerChanged && formValid) {
            onNext(selectedProvider, baseUrlTextFieldValue, apiKeyTextFieldValue, orderNames)
        } else if (providerChanged) {
            onConfirm(
                initialProvider,
                providerConfigs[initialProvider.name]?.baseUrl ?: "",
                providerConfigs[initialProvider.name]?.apiKey ?: "",
                orderNames
            )
            onDismissRequest()
        } else {
            onConfirm(selectedProvider, baseUrlTextFieldValue, apiKeyTextFieldValue, orderNames)
            onDismissRequest()
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(id = R.string.setAIProvider)) },
        text = {
            Column(modifier = Modifier.heightIn(max = 480.dp)) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(providersList, key = { _, item -> item.name }) { _, provider ->
                        AIProviderListItem(
                            provider = provider,
                            isSelected = selectedProvider == provider,
                            reorderState = reorderState,
                            onSelect = { selectedProvider = provider }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = baseUrlTextFieldValue,
                    onValueChange = { baseUrlTextFieldValue = it },
                    label = { Text(if (selectedProvider.isMandatoryBaseUrl) "* Base Url" else "Custom URL") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = (defaultBaseUrl.ifBlank { null } ?: selectedProvider.baseUrlPlaceholder)?.let { hint ->
                        @Composable { Text(hint, modifier = Modifier.alpha(0.5f)) }
                    },
                    leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = "Base Url") },
                    trailingIcon = {
                        ClickablePasteIcon(
                            text = baseUrlTextFieldValue,
                            onPaste = { baseUrlTextFieldValue = it.trim() },
                            onClear = { baseUrlTextFieldValue = "" }
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { apiKeyFocusRequester.requestFocus() }
                    ),
                )

                Spacer(modifier = Modifier.height(9.dp))

                if (selectedProvider.showsApiKeyField) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(apiKeyFocusRequester),
                        value = apiKeyTextFieldValue,
                        onValueChange = { apiKeyTextFieldValue = it },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.VpnKey,
                                contentDescription = "API Key"
                            )
                        },
                        label = {
                            Text(
                                if (selectedProvider.isRequiredApiKey) {
                                    "* " + stringResource(R.string.setApiKey)
                                } else {
                                    stringResource(
                                        R.string.apiKeyOptional,
                                        stringResource(R.string.setApiKey)
                                    )
                                }
                            )
                        },
                        shape = MaterialTheme.shapes.large,
                        trailingIcon = {
                            ClickablePasteIcon(
                                text = apiKeyTextFieldValue,
                                onPaste = { apiKeyTextFieldValue = it.trim() },
                                onClear = { apiKeyTextFieldValue = "" }
                            )
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                if (canSubmit) {
                                    submit()
                                }
                            }
                        )
                    )
                }

                ProviderTestResultText(testState)
            }
        },
        confirmButton = {
            TextButton(
                onClick = submit,
                enabled = canSubmit
            ) {
                Text(stringResource(id = if (selectedProvider != initialProvider && formValid) R.string.next else R.string.ok))
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        onTest(selectedProvider, baseUrlTextFieldValue, apiKeyTextFieldValue)
                    },
                    enabled = formValid && testState !is ProviderTestState.Testing && !testPassed
                ) {
                    Text(stringResource(id = R.string.testConnection))
                }
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        },
    )
}

@Composable
private fun ProviderTestResultText(state: ProviderTestState) {
    val (text, isError) = when (state) {
        ProviderTestState.Idle -> return
        ProviderTestState.Testing -> stringResource(R.string.testingConnection) to false
        is ProviderTestState.Done -> when (val result = state.result) {
            is ProviderTestResult.Success -> {
                val base = stringResource(
                    if (result.keyChecked) R.string.testSuccessKey else R.string.testSuccess
                )
                if (result.models.isEmpty()) {
                    base to false
                } else {
                    "$base " + stringResource(R.string.testModelsLoaded, result.models.size) to false
                }
            }

            is ProviderTestResult.AuthFailed ->
                stringResource(R.string.testAuthFailed, result.statusCode) to true

            ProviderTestResult.EndpointNotFound ->
                stringResource(R.string.testEndpointNotFound) to true

            is ProviderTestResult.HttpError ->
                stringResource(R.string.testHttpError, result.statusCode) to true

            ProviderTestResult.Unreachable -> stringResource(R.string.testUnreachable) to true
        }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun LazyItemScope.AIProviderListItem(
    provider: AIProvider,
    isSelected: Boolean,
    reorderState: ReorderableListState,
    onSelect: () -> Unit,
) {
    ReorderableItem(
        reorderState = reorderState,
        key = provider.name,
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
                BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .clickable(onClick = onSelect)
                        .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = onSelect,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = ImageVector.vectorResource(id = provider.icon),
                        tint = if (isSelected && !provider.isMonochromeIcon) Color.Unspecified else LocalContentColor.current,
                        contentDescription = "${provider.id.display} icon",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = provider.id.display,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    ReorderDragHandle(
                        reorderState = reorderState,
                        key = provider.name,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun AIProviderSettingsDialogPreview() {
    UTubeSummaryTheme {
        AIProviderSettingsDialog(
            onDismissRequest = {},
            initialProvider = AIProvider.OPENAI,
            providerConfigs = emptyMap(),
            providerOrder = emptyList(),
            onConfirm = { _, _, _, _ -> },
            onNext = { _, _, _, _ -> },
        )
    }
}
