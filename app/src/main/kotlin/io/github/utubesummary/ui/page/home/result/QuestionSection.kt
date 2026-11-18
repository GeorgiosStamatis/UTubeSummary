package io.github.utubesummary.ui.page.home.result

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.utubesummary.R
import io.github.utubesummary.ui.component.ErrorMessage
import io.github.utubesummary.vm.ChatState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val KEYBOARD_ANIMATION_MS = 300L

/** Follow-up questions about the content that was just summarized. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
fun QuestionSection(
    chatState: ChatState,
    onAsk: (String) -> Unit,
    onCopyAnswer: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var question by rememberSaveable { mutableStateOf("") }
    // Scroll the field into view once the keyboard has opened, so it is never hidden behind it.
    val bringIntoView = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    val send = {
        if (question.isNotBlank() && !chatState.isAsking) {
            onAsk(question)
            question = ""
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.askTitle),
            style = MaterialTheme.typography.titleMedium,
        )

        chatState.turns.forEach { turn ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                SelectionContainer {
                    Text(
                        text = turn.question,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            turn.answer?.let { answer ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(start = 12.dp, top = 12.dp, end = 4.dp, bottom = 4.dp)) {
                        SelectionContainer {
                            Text(text = answer, style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = { onCopyAnswer(answer) }) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = stringResource(R.string.askCopyAnswer)
                                )
                            }
                        }
                    }
                }
            }

            turn.error?.let { ErrorMessage(error = it) }
        }

        if (chatState.isAsking) {
            LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(bringIntoView)
                .onFocusChanged { focus ->
                    if (focus.isFocused) {
                        scope.launch {
                            delay(KEYBOARD_ANIMATION_MS)
                            bringIntoView.bringIntoView()
                        }
                    }
                },
            shape = MaterialTheme.shapes.extraLarge,
            placeholder = { Text(stringResource(R.string.askHint)) },
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { send() }),
            trailingIcon = {
                IconButton(
                    onClick = send,
                    enabled = question.isNotBlank() && !chatState.isAsking
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = stringResource(R.string.askSend),
                        modifier = Modifier.padding(0.dp)
                    )
                }
            }
        )
    }
}
