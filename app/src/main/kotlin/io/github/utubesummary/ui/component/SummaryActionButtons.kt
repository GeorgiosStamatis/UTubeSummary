package io.github.utubesummary.ui.component

import android.content.ClipData
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.PauseCircleFilled
import androidx.compose.material.icons.outlined.PlayCircleFilled
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import io.github.utubesummary.R
import io.github.utubesummary.model.SummaryOutput
import io.github.utubesummary.util.getLanguageCode
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "TTS"

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun SummaryActionButtons(
    summary: SummaryOutput,
    onShowSnackbar: (String) -> Unit,
    isPlaying: Boolean,
    onPlayRequest: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val haptics = LocalHapticFeedback.current
    val markdownSaved = stringResource(R.string.markdown_saved)
    val markdownSaveFailed = stringResource(R.string.markdown_save_failed)
    // Lets the user choose where to save the summary as a Markdown file.
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri, "wt")?.use {
                            it.write(summaryShareText(summary).toByteArray(Charsets.UTF_8))
                        } ?: error("Cannot open $uri")
                    }.isSuccess
                }
                onShowSnackbar(if (saved) markdownSaved else markdownSaveFailed)
            }
        }
    }
    // What the speech engine reads: the summary without Markdown symbols.
    val summaryText = remember(summary.summary) { markdownToPlainText(summary.summary) }
    val failedToPlay = stringResource(R.string.tts_failed_to_play)
    val ttsInitFailed = stringResource(R.string.tts_init_failed)
    val ttsLanguageUnsupported = stringResource(R.string.tts_language_not_supported)

    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    var isPaused by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableIntStateOf(0) }
    var resumeOffset by remember { mutableIntStateOf(0) }
    // Long summaries are spoken in chunks; each utterance id maps to its start in summaryText.
    val utteranceOffsets = remember { ConcurrentHashMap<String, Int>() }
    val lastUtteranceId = remember { AtomicReference("") }
    val copied = stringResource(id = R.string.copied)
    var playbackSpeed by remember { mutableStateOf(PlaybackSpeed.NORMAL) }

    val updatedOnPlayRequest by rememberUpdatedState(onPlayRequest)
    val updatedIsPlaying by rememberUpdatedState(isPlaying)

    val utteranceProgressListener =
        remember {
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String) {}

                override fun onDone(utteranceId: String) {
                    if (utteranceId != lastUtteranceId.get()) return
                    scope.launch {
                        if (updatedIsPlaying) {
                            updatedOnPlayRequest()
                        }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String) {
                    scope.launch { onShowSnackbar(failedToPlay) }
                }

                override fun onRangeStart(
                    utteranceId: String,
                    start: Int,
                    end: Int,
                    frame: Int,
                ) {
                    currentPosition = (utteranceOffsets[utteranceId] ?: 0) + end
                }
            }
        }

    // Speaks summaryText from [offset], split below the engine's maximum input length.
    fun speakFrom(offset: Int, rate: Float) {
        val engine = tts ?: return
        engine.setSpeechRate(rate)
        utteranceOffsets.clear()
        val maxLength = TextToSpeech.getMaxSpeechInputLength().coerceAtLeast(500)
        var queueMode = TextToSpeech.QUEUE_FLUSH
        for (chunk in speechChunks(summaryText, offset, maxLength)) {
            val id = UUID.randomUUID().toString()
            utteranceOffsets[id] = chunk.start
            lastUtteranceId.set(id)
            engine.speak(chunk.text, queueMode, null, id)
            queueMode = TextToSpeech.QUEUE_ADD
        }
    }

    DisposableEffect(Unit) {
        var textToSpeech: TextToSpeech? = null
        val onInitListener = TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                Log.d(TAG, "TTS engine initialized.")
                textToSpeech?.setOnUtteranceProgressListener(utteranceProgressListener)
                tts = textToSpeech
            } else {
                Log.e(TAG, "TTS engine init error.")
                onShowSnackbar(ttsInitFailed)
            }
        }
        textToSpeech = TextToSpeech(context, onInitListener)

        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    LaunchedEffect(isPlaying) {
        if (tts == null) return@LaunchedEffect

        if (isPlaying) {
            scope.launch {
                val langCode = getLanguageCode(context, summaryText)
                val locale = langCode?.let { Locale.forLanguageTag(it) } ?: Locale.getDefault()
                val result = tts?.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "Language '$locale' is not supported. Falling back to default.")
                    val defaultResult = tts?.setLanguage(Locale.getDefault())
                    if (defaultResult == TextToSpeech.LANG_MISSING_DATA || defaultResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(TAG, "Default language is also not supported.")
                        onShowSnackbar(ttsLanguageUnsupported)
                    } else {
                        Log.d(TAG, "Language set to default: ${Locale.getDefault()}")
                    }
                } else {
                    Log.d(TAG, "Language set successfully to: $locale")
                }

                resumeOffset = 0
                speakFrom(0, playbackSpeed.rate)
            }
        } else {
            tts?.stop()
            isPaused = false
            currentPosition = 0
            resumeOffset = 0
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // TTS Play / Stop
        FilledTonalIconButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                onPlayRequest()
            },
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (isPlaying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.size(38.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.AutoMirrored.Outlined.VolumeUp,
                contentDescription = if (isPlaying) "Stop" else "Read",
                modifier = Modifier.size(20.dp)
            )
        }

        // Animated In-Flight TTS Controls (Pause & Speed)
        AnimatedVisibility(
            visible = isPlaying,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(start = 6.dp)
            ) {
                IconButton(
                    onClick = {
                        if (isPaused) {
                            resumeOffset = currentPosition
                            speakFrom(currentPosition, playbackSpeed.rate)
                            isPaused = false
                        } else {
                            tts?.stop()
                            isPaused = true
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Outlined.PlayCircleFilled else Icons.Outlined.PauseCircleFilled,
                        contentDescription = if (isPaused) stringResource(R.string.continue_playback) else stringResource(R.string.pause_playback),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                AssistChip(
                    onClick = {
                        val newSpeed = playbackSpeed.next()
                        playbackSpeed = newSpeed
                        if (!isPaused) {
                            tts?.stop()
                            resumeOffset = currentPosition
                            speakFrom(currentPosition, newSpeed.rate)
                        }
                    },
                    label = {
                        Text(
                            text = playbackSpeed.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    shape = CircleShape,
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(
                            alpha = 0.7f
                        ),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.height(30.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Original Source Link
        summary.sourceLink?.takeIf { it.isNotBlank() }?.let { link ->
            IconButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, link.toUri())
                    context.startActivity(intent)
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Link,
                    contentDescription = stringResource(R.string.open_original_link),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Copy Button
        IconButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                scope.launch {
                    clipboard.setClipEntry(
                        ClipData.newPlainText("Summary", summaryText).toClipEntry()
                    )
                    onShowSnackbar(copied)
                }
            },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = stringResource(R.string.copy_summary),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        // Share Button
        IconButton(
            onClick = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, summaryShareText(summary))
                    summary.title.takeIf { it.isNotBlank() }?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
                }
                val chooserIntent = Intent.createChooser(shareIntent, null)
                context.startActivity(chooserIntent)
            },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = stringResource(R.string.share),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        // Save as a Markdown file
        IconButton(
            onClick = { saveLauncher.launch(suggestedMarkdownFileName(summary.title)) },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.SaveAlt,
                contentDescription = stringResource(R.string.save_as_markdown),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
