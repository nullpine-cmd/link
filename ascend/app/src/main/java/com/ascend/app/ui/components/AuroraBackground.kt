package com.ascend.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.ascend.app.ui.theme.AscendColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private class Star(val x: Float, val y: Float, val radius: Float, val phase: Float, val speed: Float)

/**
 * Живой фон: медленно дрейфующее северное сияние и мерцающие звёзды.
 * Всё рисуется в фазе отрисовки — без рекомпозиций на каждом кадре.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    accent: Color = AscendColors.Violet,
    secondary: Color = AscendColors.Cyan,
    content: @Composable BoxScope.() -> Unit,
) {
    val accentColor by animateColorAsState(accent, label = "auroraAccent")
    val secondaryColor by animateColorAsState(secondary, label = "auroraSecondary")
    val transition = rememberInfiniteTransition(label = "aurora")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(28_000, easing = LinearEasing)),
        label = "drift",
    )
    val twinkle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6_000, easing = LinearEasing)),
        label = "twinkle",
    )
    val stars = remember {
        val random = Random(42)
        List(80) {
            Star(
                x = random.nextFloat(),
                y = random.nextFloat(),
                radius = 0.4f + random.nextFloat() * 1.2f,
                phase = random.nextFloat(),
                speed = 0.5f + random.nextFloat() * 1.5f,
            )
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AscendColors.Void)
            .drawBehind {
                drawAurora(drift, accentColor, secondaryColor)
                drawStars(stars, twinkle)
            },
        content = content,
    )
}

private fun DrawScope.drawAurora(drift: Float, accent: Color, secondary: Color) {
    val w = size.width
    val h = size.height
    val a = drift * 2f * PI.toFloat()
    blob(Offset(w * (0.18f + 0.16f * sin(a)), h * (0.10f + 0.05f * cos(a * 2f))), w * 0.95f, accent.copy(alpha = 0.26f))
    blob(Offset(w * (0.88f + 0.10f * cos(a)), h * (0.32f + 0.08f * sin(a))), w * 0.80f, secondary.copy(alpha = 0.13f))
    blob(Offset(w * (0.45f + 0.22f * sin(a + 1.7f)), h * (0.62f + 0.06f * cos(a + 0.4f))), w * 0.70f, AscendColors.Pink.copy(alpha = 0.07f))
    blob(Offset(w * (0.55f + 0.18f * cos(a + 2.4f)), h * (1.02f + 0.03f * sin(a))), w * 1.05f, accent.copy(alpha = 0.16f))
}

private fun DrawScope.blob(center: Offset, radius: Float, color: Color) {
    drawCircle(
        brush = Brush.radialGradient(listOf(color, color.copy(alpha = 0f)), center = center, radius = radius),
        radius = radius,
        center = center,
    )
}

private fun DrawScope.drawStars(stars: List<Star>, twinkle: Float) {
    val tau = 2f * PI.toFloat()
    val px = 1.dp.toPx()
    for (star in stars) {
        val wave = 0.5f + 0.5f * sin((twinkle * star.speed + star.phase) * tau)
        drawCircle(
            color = Color.White.copy(alpha = 0.08f + 0.55f * wave * wave),
            radius = star.radius * px,
            center = Offset(star.x * size.width, star.y * size.height),
        )
    }
}
