package com.ascend.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.app.ui.theme.AscendColors
import com.ascend.app.ui.theme.colors
import com.ascend.core.Rank
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Эмблема героя: вращающиеся рунные кольца, дышащее ядро ауры, кольцо опыта
 * и спутники-искры, число которых растёт с рангом.
 */
@Composable
fun HeroEmblem(
    avatar: String,
    aura: List<Color>,
    rank: Rank,
    progress: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = 200.dp,
    showProgress: Boolean = true,
    detailed: Boolean = true,
) {
    val transition = rememberInfiniteTransition(label = "emblem")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(40_000, easing = LinearEasing)),
        label = "spin",
    )
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath",
    )
    val orbit by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(10_000, easing = LinearEasing)),
        label = "orbit",
    )
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 40f),
        label = "emblemProgress",
    )
    val rankColors = rank.colors
    val primary = aura.first()
    val secondary = aura.getOrElse(1) { aura.first() }

    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val c = center

            // Аура
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        primary.copy(alpha = 0.42f + 0.18f * breath),
                        secondary.copy(alpha = 0.12f),
                        Color.Transparent,
                    ),
                    center = c,
                    radius = r,
                ),
                radius = r,
                center = c,
            )

            if (detailed) {
                // Внешнее рунное кольцо
                rotate(spin, c) {
                    val ringR = r * 0.88f
                    for (i in 0 until 60) {
                        val major = i % 5 == 0
                        val len = if (major) r * 0.075f else r * 0.035f
                        val a = (i * 6f) * (PI.toFloat() / 180f)
                        val start = Offset(c.x + cos(a) * (ringR - len), c.y + sin(a) * (ringR - len))
                        val end = Offset(c.x + cos(a) * ringR, c.y + sin(a) * ringR)
                        drawLine(
                            color = rankColors.first().copy(alpha = if (major) 0.75f else 0.28f),
                            start = start,
                            end = end,
                            strokeWidth = if (major) 2.dp.toPx() else 1.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }
                }
                // Среднее пунктирное кольцо с ромбами
                rotate(-spin * 1.7f, c) {
                    drawCircle(
                        color = secondary.copy(alpha = 0.45f),
                        radius = r * 0.76f,
                        center = c,
                        style = Stroke(
                            width = 1.4.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 7.dp.toPx())),
                        ),
                    )
                    for (i in 0 until 3) {
                        val a = (i * 120f) * (PI.toFloat() / 180f)
                        drawDiamond(
                            Offset(c.x + cos(a) * r * 0.76f, c.y + sin(a) * r * 0.76f),
                            r * 0.045f,
                            rankColors.last(),
                        )
                    }
                }
            }

            // Кольцо опыта
            if (showProgress) {
                val ringR = r * 0.65f
                val strokePx = (r * 0.06f).coerceAtLeast(3.dp.toPx())
                val topLeft = Offset(c.x - ringR, c.y - ringR)
                val arcSize = Size(ringR * 2, ringR * 2)
                drawCircle(Color.White.copy(alpha = 0.07f), ringR, c, style = Stroke(strokePx))
                if (animatedProgress > 0.002f) {
                    rotate(-90f, c) {
                        drawArc(
                            brush = Brush.sweepGradient(listOf(primary, secondary, primary), c),
                            startAngle = 0f,
                            sweepAngle = 360f * animatedProgress,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(strokePx, cap = StrokeCap.Round),
                        )
                    }
                    val a = (360f * animatedProgress - 90f) * (PI.toFloat() / 180f)
                    val head = Offset(c.x + cos(a) * ringR, c.y + sin(a) * ringR)
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color.White, secondary.copy(alpha = 0.5f), Color.Transparent),
                            center = head,
                            radius = strokePx * 1.8f,
                        ),
                        radius = strokePx * 1.8f,
                        center = head,
                    )
                }
            }

            // Ядро
            val coreR = r * 0.53f * (0.97f + 0.03f * breath)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        primary.copy(alpha = 0.95f),
                        secondary.copy(alpha = 0.55f),
                        AscendColors.Night,
                    ),
                    center = Offset(c.x - coreR * 0.25f, c.y - coreR * 0.3f),
                    radius = coreR * 1.5f,
                ),
                radius = coreR,
                center = c,
            )
            drawCircle(
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.04f)),
                    startY = c.y - coreR,
                    endY = c.y + coreR,
                ),
                radius = coreR,
                center = c,
                style = Stroke(1.2.dp.toPx()),
            )

            // Спутники-искры
            if (detailed) {
                val count = 3 + rank.ordinal
                for (i in 0 until count) {
                    val phase = i.toFloat() / count
                    val speed = 1f + (i % 3) * 0.35f
                    val a = ((orbit * speed + phase) * 2f * PI).toFloat()
                    val orbitR = r * (0.80f + 0.07f * sin(i * 1.7f))
                    val p = Offset(c.x + cos(a) * orbitR, c.y + sin(a) * orbitR * 0.92f)
                    val color = rankColors[i % rankColors.size]
                    drawCircle(
                        brush = Brush.radialGradient(listOf(color.copy(alpha = 0.55f), Color.Transparent), center = p, radius = r * 0.07f),
                        radius = r * 0.07f,
                        center = p,
                    )
                    drawCircle(Color.White.copy(alpha = 0.95f), radius = r * 0.014f + 0.6.dp.toPx(), center = p)
                }
            }
        }
        Text(
            text = avatar,
            fontSize = (diameter.value * 0.30f).sp,
            modifier = Modifier.graphicsLayer {
                translationY = -diameter.toPx() * 0.012f * (breath * 2f - 1f)
                val s = 1f + 0.03f * breath
                scaleX = s
                scaleY = s
            },
        )
    }
}

private fun DrawScope.drawDiamond(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + radius * 0.7f, center.y)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - radius * 0.7f, center.y)
        close()
    }
    drawCircle(
        brush = Brush.radialGradient(listOf(color.copy(alpha = 0.6f), Color.Transparent), center = center, radius = radius * 2.4f),
        radius = radius * 2.4f,
        center = center,
    )
    drawPath(path, color)
}
