package com.ascend.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ascend.app.ui.theme.AscendColors

/** Живое пламя серии: колышется и мерцает, а погасшее — тлеет серым. */
@Composable
fun StreakFlame(modifier: Modifier = Modifier, diameter: Dp = 28.dp, lit: Boolean = true) {
    val transition = rememberInfiniteTransition(label = "flame")
    val flicker by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(380, easing = LinearEasing), RepeatMode.Reverse),
        label = "flicker",
    )
    val sway by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sway",
    )
    val palette = if (lit) AscendColors.Fire else listOf(Color(0xFF9CA3AF), Color(0xFF6B7280), Color(0xFF4B5563))
    Canvas(modifier.size(diameter)) {
        val w = size.width
        val h = size.height
        val motion = if (lit) 1f else 0.15f
        if (lit) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(palette[1].copy(alpha = 0.32f + 0.18f * flicker), Color.Transparent),
                    center = Offset(w / 2f, h * 0.62f),
                    radius = w * 0.62f,
                ),
                radius = w * 0.62f,
                center = Offset(w / 2f, h * 0.62f),
            )
        }
        val stretch = 1f + 0.05f * flicker * motion
        val outer = flamePath(w, h, sway * 0.07f * motion, stretch)
        drawPath(outer, Brush.verticalGradient(listOf(palette[2], palette[1], palette[0]), startY = 0f, endY = h))
        scale(0.56f, 0.62f + 0.04f * flicker * motion, pivot = Offset(w / 2f, h * 0.96f)) {
            drawPath(
                flamePath(w, h, -sway * 0.05f * motion, 1f),
                Brush.verticalGradient(listOf(palette[0], Color.White.copy(alpha = if (lit) 0.95f else 0.4f)), startY = 0f, endY = h),
            )
        }
    }
}

private fun flamePath(w: Float, h: Float, sway: Float, stretch: Float): Path = Path().apply {
    val tipY = h * (0.04f + (1f - stretch) * 0.5f)
    moveTo(w * 0.5f, h * 0.97f)
    cubicTo(w * 0.10f, h * 0.93f, w * 0.06f, h * 0.56f, w * 0.30f, h * 0.36f)
    cubicTo(w * 0.33f, h * 0.50f, w * 0.40f, h * 0.55f, w * 0.45f, h * 0.55f)
    cubicTo(w * 0.38f, h * 0.30f, w * (0.50f + sway), h * 0.16f, w * (0.56f + sway), tipY)
    cubicTo(w * (0.80f + sway * 0.5f), h * 0.24f, w * 0.97f, h * 0.55f, w * 0.88f, h * 0.76f)
    cubicTo(w * 0.81f, h * 0.93f, w * 0.66f, h * 0.98f, w * 0.5f, h * 0.97f)
    close()
}
