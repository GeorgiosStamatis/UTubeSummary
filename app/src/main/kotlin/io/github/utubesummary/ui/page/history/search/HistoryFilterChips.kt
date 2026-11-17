package io.github.utubesummary.ui.page.history.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.utubesummary.R
import io.github.utubesummary.model.HistorySort
import io.github.utubesummary.model.SummaryProfile
import io.github.utubesummary.model.SummaryType
import io.github.utubesummary.ui.component.displayName
import io.github.utubesummary.ui.component.icon

/**
 * Filter chips for the History: pinned items, the profile that made a summary, and the type.
 */
@Composable
fun HistoryFilterChips(
    selectedFilter: SummaryType?,
    onFilterChanged: (SummaryType) -> Unit,
    modifier: Modifier = Modifier,
    pinnedOnly: Boolean = false,
    onPinnedOnlyChanged: (Boolean) -> Unit = {},
    profiles: List<SummaryProfile> = emptyList(),
    selectedProfileId: String? = null,
    onProfileFilterChanged: (String?) -> Unit = {},
    sort: HistorySort = HistorySort.NEWEST,
    onSortChanged: (HistorySort) -> Unit = {},
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "pinned") {
            FilterChip(
                selected = pinnedOnly,
                onClick = { onPinnedOnlyChanged(!pinnedOnly) },
                label = { Text(stringResource(R.string.history_pinned)) },
                leadingIcon = {
                    Icon(
                        imageVector = if (pinnedOnly) Icons.Default.Check else Icons.Outlined.PushPin,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }

        if (profiles.size > 1) {
            item(key = "profile") {
                ProfileFilterChip(
                    profiles = profiles,
                    selectedProfileId = selectedProfileId,
                    onProfileFilterChanged = onProfileFilterChanged,
                )
            }
        }

        item(key = "sort") {
            SortChip(sort = sort, onSortChanged = onSortChanged)
        }

        items(SummaryType.entries) { filter ->
            val selected = selectedFilter == filter
            FilterChip(
                selected = selected,
                onClick = { onFilterChanged(filter) },
                label = {
                    Text(
                        text = filter.name.lowercase()
                            .replaceFirstChar { it.titlecase() }
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (selected) Icons.Default.Check else filter.icon,
                        contentDescription = filter.name
                    )
                }
            )
        }
    }
}

@Composable
private fun ProfileFilterChip(
    profiles: List<SummaryProfile>,
    selectedProfileId: String?,
    onProfileFilterChanged: (String?) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selected = profiles.firstOrNull { it.id == selectedProfileId }

    Box {
        FilterChip(
            selected = selected != null,
            onClick = { expanded = true },
            label = {
                Text(
                    text = selected?.displayName() ?: stringResource(R.string.summary_profile),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            trailingIcon = {
                Icon(Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.history_all_profiles)) },
                onClick = {
                    expanded = false
                    onProfileFilterChanged(null)
                }
            )
            HorizontalDivider()
            profiles.forEach { profile ->
                DropdownMenuItem(
                    text = { Text(profile.displayName()) },
                    onClick = {
                        expanded = false
                        // The view model toggles: choosing the active profile again clears it.
                        onProfileFilterChanged(profile.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun sortLabel(sort: HistorySort): String = stringResource(
    when (sort) {
        HistorySort.NEWEST -> R.string.history_sort_newest
        HistorySort.OLDEST -> R.string.history_sort_oldest
        HistorySort.TITLE -> R.string.history_sort_title
    }
)

@Composable
private fun SortChip(
    sort: HistorySort,
    onSortChanged: (HistorySort) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box {
        FilterChip(
            selected = sort != HistorySort.NEWEST,
            onClick = { expanded = true },
            label = {
                Text(
                    text = sortLabel(sort),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            trailingIcon = {
                Icon(Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            HistorySort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(sortLabel(option)) },
                    onClick = {
                        expanded = false
                        onSortChanged(option)
                    }
                )
            }
        }
    }
}
