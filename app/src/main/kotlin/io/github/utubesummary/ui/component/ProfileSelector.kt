package io.github.utubesummary.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.utubesummary.R
import io.github.utubesummary.llm.CRITICAL_ANALYST_PROFILE_ID
import io.github.utubesummary.llm.CURIOUS_LEARNER_PROFILE_ID
import io.github.utubesummary.llm.QUICK_BRIEF_PROFILE_ID
import io.github.utubesummary.llm.TECHNICAL_CONTENT_EVALUATOR_PROFILE_ID
import io.github.utubesummary.llm.TECHNICAL_ANALYSIS_PROFILE_ID
import io.github.utubesummary.model.DEFAULT_PROFILE_ID
import io.github.utubesummary.model.SummaryProfile

/** Display name of a profile: its custom/renamed name, else the localized built-in name. */
@Composable
fun SummaryProfile.displayName(): String {
    name?.takeIf { it.isNotBlank() }?.let { return it }
    val resId = when (id) {
        DEFAULT_PROFILE_ID -> R.string.profile_default
        TECHNICAL_ANALYSIS_PROFILE_ID -> R.string.profile_technical_analysis
        CRITICAL_ANALYST_PROFILE_ID -> R.string.profile_critical_analyst
        CURIOUS_LEARNER_PROFILE_ID -> R.string.profile_curious_learner
        TECHNICAL_CONTENT_EVALUATOR_PROFILE_ID -> R.string.profile_technical_content_evaluator
        QUICK_BRIEF_PROFILE_ID -> R.string.profile_quick_brief
        else -> R.string.profile_unnamed
    }
    return stringResource(resId)
}

/** Compact dropdown that picks the summarization profile. */
@Composable
fun ProfileSelector(
    profiles: List<SummaryProfile>,
    selectedProfileId: String,
    onProfileSelect: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selected = profiles.firstOrNull { it.id == selectedProfileId } ?: profiles.firstOrNull() ?: return

    Box(modifier = modifier) {
        AssistChip(
            onClick = { expanded = true },
            enabled = enabled,
            label = { Text("${stringResource(R.string.summary_profile)}: ${selected.displayName()}") },
            trailingIcon = {
                Icon(Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
            },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            profiles.forEach { profile ->
                DropdownMenuItem(
                    text = { Text(profile.displayName()) },
                    leadingIcon = {
                        if (profile.id == selected.id) {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    },
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        expanded = false
                        onProfileSelect(profile.id)
                    },
                )
            }
        }
    }
}
