package com.ascend.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ascend.app.ui.theme.AscendColors
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope

/**
 * Кнопка выполнения: кольцо наливается цветом характеристики с упругим «пульсом»,
 * галочка прорисовывается штрихом, вокруг разлетаются искры.
 */
@Composable
fun QuestCheck(
    checked: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    diameter: Dp = 34.dp,
    enabled: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current
    val fill by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = 420f),
        label = "checkFill",
    )
    val stroke = remember { Animatable(if (checked) 1f else 0f) }
    val ripple = remember { Animatable(1f) }
    var burst by remember { mutableIntStateOf(0) }
    var previous by remember { mutableStateOf(checked) }

    LaunchedEffect(checked) {
        if (checked && !previous) {
            burst++
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            coroutineScope {
                launch {
                    ripple.snapTo(0f)
                    ripple.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
                }
                launch {
                    stroke.snapTo(0f)
                    stroke.animateTo(1f, tween(360, delayMillis = 80, easing = FastOutSlowInEasing))
                }
            }
        } else if (!checked) {
            stroke.snapTo(0f)
        } else {
            stroke.snapTo(1f)
        }
        previous = checked
    }

    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        ParticleBurst(
            trigger = burst,
            colors = listOf(color, Color.White, AscendColors.Gold),
            modifier = Modifier.matchParentSize(),
            count = 16,
            power = diameter * 1.4f,
        )
        Canvas(
            Modifier
                .matchParentSize()
                .bounceClick(enabled = enabled, pressedScale = 0.85f, onClick = onClick),
        ) {
            val r = size.minDimension / 2f
            val ringWidth = 2.2.dp.toPx()
            val rippleValue = ripple.value
            if (rippleValue < 1f) {
                drawCircle(
                    color = color.copy(alpha = 0.6f * (1f - rippleValue)),
                    radius = r * (1f + rippleValue * 0.9f),
                    style = Stroke(ringWidth * (1f - rippleValue) + 0.5f),
                )
            }
            drawCircle(
                color = (if (enabled) color else AscendColors.TextMuted).copy(alpha = 0.65f * (1f - fill).coerceIn(0f, 1f) + 0.1f),
                radius = r - ringWidth / 2f,
                style = Stroke(ringWidth),
            )
            if (fill > 0.001f) {
                drawCircle(
                    brush = Brush.radialGradient(listOf(color.copy(alpha = 0.55f), Color.Transparent), radius = r * 1.6f),
                    radius = r * 1.6f * fill.coerceIn(0f, 1.2f),
                )
                drawCircle(
                    brush = Brush.linearGradient(listOf(color, color.copy(alpha = 0.75f)), start = Offset.Zero, end = Offset(size.width, size.height)),
                    radius = r * fill,
                )
            }
            val progress = stroke.value
            if (progress > 0f) {
                val w = size.width
                val h = size.height
                val path = Path().apply {
                    moveTo(w * 0.28f, h * 0.52f)
                    lineTo(w * 0.44f, h * 0.67f)
                    lineTo(w * 0.73f, h * 0.36f)
                }
                val measure = PathMeasure()
                measure.setPath(path, false)
                val segment = Path()
                measure.getSegment(0f, measure.length * progress, segment, true)
                drawPath(
                    segment,
                    Color.White,
                    style = Stroke(2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
    }
}
