package io.github.utubesummary.ui.page.history.section

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import io.github.utubesummary.R
import io.github.utubesummary.model.HistorySummary
import io.github.utubesummary.ui.component.ContentBadgeColors

private val ThumbnailWidth = 96.dp
private val ThumbnailHeight = 54.dp

/**
 * Small video thumbnail (16:9). It is requested at exactly its on-screen size, cached on disk
 * (capped, see App) and only loaded while the row is visible. While it loads, or if it can't
 * (offline, private or deleted video), the tonal YouTube badge is shown instead.
 */
@Composable
fun VideoThumbnail(
    summary: HistorySummary,
    url: String,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val request = ImageRequest.Builder(LocalContext.current)
        .data(url)
        .size(with(density) { ThumbnailWidth.roundToPx() }, with(density) { ThumbnailHeight.roundToPx() })
        .scale(Scale.FILL)
        .allowRgb565(true)
        .crossfade(150)
        .build()

    val shape = RoundedCornerShape(10.dp)
    val (container, content) = ContentBadgeColors.badgeColorsFor(summary)
    val fallback: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .size(ThumbnailWidth, ThumbnailHeight)
                .background(container),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.youtube),
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(24.dp)
            )
        }
    }

    SubcomposeAsyncImage(
        model = request,
        contentDescription = null,
        modifier = modifier
            .size(ThumbnailWidth, ThumbnailHeight)
            .clip(shape),
        contentScale = ContentScale.Crop,
        loading = { fallback() },
        error = { fallback() },
    )
}
