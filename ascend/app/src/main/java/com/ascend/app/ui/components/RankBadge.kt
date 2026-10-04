package com.ascend.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.app.ui.theme.colors
import com.ascend.core.Rank
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Шестигранный знак ранга с бегущим бликом; у SSS — переливающаяся радуга. */
@Composable
fun RankBadge(rank: Rank, modifier: Modifier = Modifier, diameter: Dp = 44.dp) {
    val transition = rememberInfiniteTransition(label = "rankBadge")
    val sheen by transition.animateFloat(
        initialValue = -0.8f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(tween(2_600, delayMillis = 2_200, easing = LinearEasing)),
        label = "sheen",
    )
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(5_000, easing = LinearEasing)),
        label = "spin",
    )
    val colors = rank.colors
    val rainbow = rank == Rank.SSS
    Box(
        modifier = modifier
            .size(diameter)
            .drawWithCache {
                val mid = Offset(size.width / 2f, size.height / 2f)
                val outer = hexagonPath(mid, size.minDimension / 2f * 0.94f)
                val inner = hexagonPath(mid, size.minDimension / 2f * 0.72f)
                val fill = Brush.linearGradient(colors, start = Offset.Zero, end = Offset(size.width, size.height))
                onDrawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(listOf(colors.first().copy(alpha = 0.45f), Color.Transparent)),
                        radius = size.minDimension * 0.8f,
                    )
                    if (rainbow) {
                        clipPath(outer) {
                            rotate(spin) {
                                drawCircle(Brush.sweepGradient(colors), radius = size.maxDimension)
                            }
                        }
                    } else {
                        drawPath(outer, fill)
                    }
                    drawPath(inner, Color.Black.copy(alpha = 0.22f))
                    drawPath(inner, Color.White.copy(alpha = 0.25f), style = Stroke(1.dp.toPx()))
                    clipPath(outer) {
                        val x = sheen * size.width
                        drawRect(
                            Brush.linearGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.55f), Color.Transparent),
                                start = Offset(x - size.width * 0.35f, 0f),
                                end = Offset(x + size.width * 0.35f, size.height),
                            ),
                            size = Size(size.width, size.height),
                        )
                    }
                    drawPath(outer, Color.White.copy(alpha = 0.6f), style = Stroke(1.3.dp.toPx()))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = rank.letter,
            style = TextStyle(
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = (diameter.value * if (rank.letter.length > 2) 0.24f else if (rank.letter.length == 2) 0.3f else 0.38f).sp,
                letterSpacing = (-0.5).sp,
                shadow = Shadow(Color.Black.copy(alpha = 0.45f), Offset(0f, 2f), 6f),
            ),
        )
    }
}

fun hexagonPath(center: Offset, radius: Float): Path = Path().apply {
    for (i in 0 until 6) {
        val a = (-90f + 60f * i) * (PI.toFloat() / 180f)
        val x = center.x + cos(a) * radius
        val y = center.y + sin(a) * radius
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}
