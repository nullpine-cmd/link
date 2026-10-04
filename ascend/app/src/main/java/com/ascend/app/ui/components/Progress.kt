package com.ascend.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ascend.app.ui.theme.AscendColors
import kotlin.math.cos
import kotlin.math.sin

/** Полоса прогресса с пружинным заполнением, мерцающим бликом и светящимся «наконечником». */
@Composable
fun GlowProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    colors: List<Color> = AscendColors.Xp,
    height: Dp = 10.dp,
    trackColor: Color = Color.White.copy(alpha = 0.08f),
    shimmer: Boolean = true,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 45f),
        label = "barProgress",
    )
    val transition = rememberInfiniteTransition(label = "barShimmer")
    val shine by transition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(2_200, delayMillis = 900, easing = FastOutSlowInEasing)),
        label = "shine",
    )
    val brushColors = if (colors.size == 1) listOf(colors[0], colors[0]) else colors
    Canvas(modifier.fillMaxWidth().height(height)) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(trackColor, cornerRadius = radius)
        val width = size.width * animated
        if (width <= 0.5f) return@Canvas
        val filled = Path().apply {
            addRoundRect(RoundRect(0f, 0f, width.coerceAtLeast(size.height), size.height, radius))
        }
        clipPath(filled) {
            drawRect(Brush.horizontalGradient(brushColors, startX = 0f, endX = size.width.coerceAtLeast(1f)))
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent)),
                size = Size(size.width, size.height * 0.5f),
            )
            if (shimmer) {
                val x = shine * size.width
                drawRect(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.45f), Color.Transparent),
                        startX = x - size.height * 6f,
                        endX = x + size.height * 6f,
                    ),
                )
            }
        }
        val head = Offset(width.coerceAtLeast(size.height) - size.height / 2f, size.height / 2f)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(brushColors.last().copy(alpha = 0.7f), Color.Transparent),
                center = head,
                radius = size.height * 1.6f,
            ),
            radius = size.height * 1.6f,
            center = head,
        )
    }
}

/** Кольцо прогресса с градиентом и светящейся точкой на конце дуги. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    colors: List<Color> = AscendColors.Xp,
    stroke: Dp = 8.dp,
    trackColor: Color = Color.White.copy(alpha = 0.07f),
    content: @Composable BoxScope.() -> Unit = {},
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 50f),
        label = "ringProgress",
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokePx = stroke.toPx()
            val diameter = size.minDimension - strokePx
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            drawArc(trackColor, 0f, 360f, false, topLeft, arcSize, style = Stroke(strokePx))
            if (animated > 0.001f) {
                val sweepColors = colors + colors.first()
                rotate(-90f) {
                    drawArc(
                        brush = Brush.sweepGradient(sweepColors, center),
                        startAngle = 0f,
                        sweepAngle = 360f * animated,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(strokePx, cap = StrokeCap.Round),
                    )
                }
                val angle = Math.toRadians((360f * animated - 90f).toDouble())
                val r = diameter / 2f
                val head = Offset(center.x + r * cos(angle).toFloat(), center.y + r * sin(angle).toFloat())
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.9f), colors.last().copy(alpha = 0.4f), Color.Transparent),
                        center = head,
                        radius = strokePx * 1.4f,
                    ),
                    radius = strokePx * 1.4f,
                    center = head,
                )
            }
        }
        content()
    }
}

/** Число, у которого каждая цифра «прокручивается» как на одометре. */
@Composable
fun RollingNumber(
    value: Long,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    val text = value.toString()
    Row(modifier) {
        text.forEachIndexed { index, char ->
            key(text.length - index) {
                AnimatedContent(
                    targetState = char,
                    transitionSpec = {
                        val up = targetState > initialState
                        (
                            slideInVertically(spring(dampingRatio = 0.7f, stiffness = 300f)) { if (up) it else -it } +
                                fadeIn()
                            ) togetherWith (
                            slideOutVertically { if (up) -it else it } + fadeOut()
                            ) using SizeTransform(clip = false)
                    },
                    label = "digit",
                ) { digit ->
                    Text(digit.toString(), style = style, color = color)
                }
            }
        }
    }
}

/** Плавный счётчик: значение «набегает» к новому. */
@Composable
fun CountingText(
    value: Int,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    prefix: String = "",
    suffix: String = "",
    durationMillis: Int = 900,
) {
    val animated by animateIntAsState(value, tween(durationMillis, easing = FastOutSlowInEasing), label = "counting")
    Text("$prefix$animated$suffix", style = style, color = color, modifier = modifier)
}

/** Звёзды сложности. */
@Composable
fun DifficultyStars(stars: Int, color: Color, modifier: Modifier = Modifier, max: Int = 4, starSize: Dp = 10.dp) {
    Canvas(modifier.size(width = starSize * max + 2.dp * (max - 1), height = starSize)) {
        val s = size.height
        val gap = 2.dp.toPx()
        for (i in 0 until max) {
            val cx = i * (s + gap) + s / 2f
            val path = starPath(Offset(cx, s / 2f), s / 2f, s / 4.4f)
            drawPath(path, if (i < stars) color else Color.White.copy(alpha = 0.14f))
        }
    }
}

fun starPath(center: Offset, outer: Float, inner: Float, points: Int = 5): Path = Path().apply {
    val step = Math.PI / points
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) outer else inner
        val angle = -Math.PI / 2 + i * step
        val x = center.x + (r * cos(angle)).toFloat()
        val y = center.y + (r * sin(angle)).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}
