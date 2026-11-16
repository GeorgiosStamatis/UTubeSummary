package io.github.utubesummary.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val LogoIndigo = Color(0xFF4A3FD6)
private val LogoAmber = Color(0xFFFFC73A)

// The mark is drawn in the 108-unit space of the launcher icon; the visible tile is the
// central 72 units (18..90).
private const val TILE_START = 18f
private const val TILE_SIZE = 72f

/**
 * The app mark: a white play button above three amber lines of text, on an indigo tile.
 * Brand colors are fixed (not themed). With [isAnimated] the mark gently pulses.
 */
@Composable
fun LogoIcon(modifier: Modifier = Modifier, size: Dp, isAnimated: Boolean = false) {
    val transition = rememberInfiniteTransition(label = "logo pulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (isAnimated) 1.06f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(pulse)
            .clip(RoundedCornerShape(percent = 28))
            .background(LogoIndigo)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val unit = this.size.width / TILE_SIZE
            fun point(x: Float, y: Float) = Offset((x - TILE_START) * unit, (y - TILE_START) * unit)

            val play = Path().apply {
                val a = point(44f, 29f)
                moveTo(a.x, a.y)
                val b = point(44f, 54f)
                lineTo(b.x, b.y)
                val c = point(66f, 41.5f)
                lineTo(c.x, c.y)
                close()
            }
            drawPath(play, Color.White, style = Fill)
            drawPath(
                play,
                Color.White,
                style = Stroke(width = 6f * unit, join = StrokeJoin.Round)
            )

            listOf(
                Triple(65f, 38f, 70f),
                Triple(72.5f, 38f, 70f),
                Triple(80f, 38f, 58f),
            ).forEach { (y, x1, x2) ->
                drawLine(
                    color = LogoAmber,
                    start = point(x1, y),
                    end = point(x2, y),
                    strokeWidth = 5f * unit,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Preview
@Composable
fun LogoIconPreview() {
    LogoIcon(size = 250.dp)
}
