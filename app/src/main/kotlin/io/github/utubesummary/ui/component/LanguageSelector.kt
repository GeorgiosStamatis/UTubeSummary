package io.github.utubesummary.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.utubesummary.R
import io.github.utubesummary.model.SUMMARY_LANGUAGE_APP
import io.github.utubesummary.model.SUMMARY_LANGUAGE_CONTENT
import io.github.utubesummary.model.summaryLanguageTags
import java.util.Locale

/** Display name of a summary language setting: "Video language", "App language" or the language's own name. */
@Composable
fun summaryLanguageLabel(language: String): String = when (language) {
    SUMMARY_LANGUAGE_CONTENT -> stringResource(R.string.language_video)
    SUMMARY_LANGUAGE_APP -> stringResource(R.string.language_app)
    else -> languageOwnName(language)
}

/** A language written in itself, e.g. "français" for "fr". */
fun languageOwnName(tag: String): String {
    val locale = Locale.forLanguageTag(tag)
    return locale.getDisplayLanguage(locale).replaceFirstChar { it.titlecase(locale) }
}

/** Compact dropdown that picks the language of the summary. */
@Composable
fun LanguageSelector(
    selectedLanguage: String,
    onLanguageSelect: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    var expanded by rememberSaveable { mutableStateOf(false) }

    @Composable
    fun Item(value: String) {
        DropdownMenuItem(
            text = { Text(summaryLanguageLabel(value)) },
            leadingIcon = {
                if (value == selectedLanguage) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            },
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                expanded = false
                onLanguageSelect(value)
            },
        )
    }

    Box(modifier = modifier) {
        AssistChip(
            onClick = { expanded = true },
            enabled = enabled,
            label = {
                Text(
                    text = "${stringResource(R.string.summary_language)}: ${summaryLanguageLabel(selectedLanguage)}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            trailingIcon = {
                Icon(Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
            },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Item(SUMMARY_LANGUAGE_CONTENT)
            Item(SUMMARY_LANGUAGE_APP)
            HorizontalDivider()
            summaryLanguageTags.forEach { Item(it) }
        }
    }
}
