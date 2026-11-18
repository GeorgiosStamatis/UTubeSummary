package io.github.utubesummary.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.utubesummary.R
import io.github.utubesummary.llm.AIProvider
import io.github.utubesummary.model.SummaryLength
import io.github.utubesummary.model.SummaryOutput

private const val MAX_LINES_WHEN_COLLAPSE = 7

/**
 * Material 3 Expressive Summary Card.
 * Adheres to M3 Expressive visual design:
 * - 28dp extra-large corner radius with tonal surface container
 * - 8dp spacing system with clear typography hierarchy
 * - Deduplicated overview and strict author validation
 * - Expressive spring physics on expand/collapse and TTS playback controls
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SummaryCard(
    modifier: Modifier = Modifier,
    summary: SummaryOutput,
    cardColors: CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ),
    isExpandedByDefault: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onShowSnackbar: (String) -> Unit,
    isPlaying: Boolean = false,
    onPlayRequest: () -> Unit = {},
) {
    var isExpanded by remember { mutableStateOf(isExpandedByDefault) }
    var isTextOverflowing by remember { mutableStateOf(false) }

    val hasTitle = summary.title.isNotBlank()
    val knownAuthor = summary.author.takeIf { isKnownAuthor(it) }
    val hasMeta =
        knownAuthor != null || summary.isYoutubeLink || summary.isBiliBiliLink || summary.provider != null
    val showOverview = remember(summary.overview, summary.summary) {
        shouldShowOverview(summary.overview, summary.summary)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = { if (isTextOverflowing) isExpanded = !isExpanded },
                        onLongClick = onLongClick
                    )
                } else {
                    Modifier.clickable { if (isTextOverflowing) isExpanded = !isExpanded }
                }
            ),
        shape = MaterialTheme.shapes.extraLarge,
        colors = cardColors,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
                .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title
            if (hasTitle) {
                Text(
                    text = summary.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 28.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Meta row: Author, Source Icon, Provider/Model Indicator
            if (hasMeta) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        if (knownAuthor != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = knownAuthor,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (summary.isYoutubeLink) {
                            Icon(
                                painter = painterResource(id = R.drawable.youtube),
                                contentDescription = "YouTube",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        } else if (summary.isBiliBiliLink) {
                            Icon(
                                painter = painterResource(id = R.drawable.bilibili),
                                contentDescription = "BiliBili",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    val aiProvider = summary.provider?.let { providerName ->
                        AIProvider.entries.find { it.name.equals(providerName, ignoreCase = true) }
                    }
                    if (aiProvider != null) {
                        LlmIndicator(
                            provider = aiProvider,
                            model = summary.model,
                            style = LlmIndicatorStyle.PILL,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            // Topic Tags
            if (summary.tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    summary.tags.forEach { tag ->
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Expressive Overview Box (only if distinct and non-duplicated)
            if (showOverview) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 3.dp, height = 14.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                            Text(
                                text = stringResource(R.string.overview),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = summary.overview!!.trim(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // Key Points
            if (summary.keyPoints.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Key Takeaways",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    summary.keyPoints.forEach { point ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 7.dp)
                                    .size(6.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                            Text(
                                text = point,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            // Summary Body
            if (summary.summary.isNotBlank()) {
                val bodyText = remember(summary.summary) { markdownToAnnotatedString(summary.summary.trim()) }
                Text(
                    text = bodyText,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (isExpanded) Int.MAX_VALUE else MAX_LINES_WHEN_COLLAPSE,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { textLayoutResult ->
                        isTextOverflowing =
                            textLayoutResult.lineCount > MAX_LINES_WHEN_COLLAPSE || textLayoutResult.hasVisualOverflow
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedVisibility(
                    visible = isTextOverflowing,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    TextButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = if (isExpanded) "Show Less" else "Show More",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        SummaryActionButtons(
            summary = summary,
            onShowSnackbar = onShowSnackbar,
            isPlaying = isPlaying,
            onPlayRequest = onPlayRequest
        )
    }
}

@Preview
@Composable
fun SummaryCardPreview() {
    val summary = SummaryOutput(
        title = "Nvidia Acquires Hugging Face: The End of Frontier AI Monopolies?",
        author = "Tech Reporter",
        overview = "Nvidia combines leading hardware with the primary open-source model hub in a blockbuster deal.",
        keyPoints = listOf(
            "Acquisition provides enterprise turnkey hardware-software stack.",
            "Challenges proprietary API moats from OpenAI and Anthropic."
        ),
        tags = listOf("AI", "OpenSource", "Nvidia", "HuggingFace"),
        summary = "Nvidia's acquisition of the open-source AI platform Hugging Face for $12.9 billion combines the leading GPU maker with a central repository for machine learning models. The integration enables enterprises to deploy private hardware and software alternatives to closed frontier labs.",
        isYoutubeLink = true,
        length = SummaryLength.SHORT,
        sourceLink = "https://youtube.com/watch?v=123",
        isBiliBiliLink = false,
        provider = AIProvider.GEMINI.name,
        model = "gemini-2.0-flash"
    )
    SummaryCard(
        summary = summary,
        onShowSnackbar = {}
    )
}