package com.ascend.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ascend.app.ui.theme.AscendColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

private class Spark(
    val angle: Float,
    val speed: Float,
    val radiusDp: Float,
    val color: Color,
    val spin: Float,
    val shard: Boolean,
)

/**
 * Взрыв искр из центра. Срабатывает при каждом изменении [trigger] (> 0).
 * Частицы не хранят состояние — их положение вычисляется из времени.
 */
@Composable
fun ParticleBurst(
    trigger: Int,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    count: Int = 22,
    power: Dp = 70.dp,
    durationMillis: Int = 850,
    gravity: Float = 0.45f,
) {
    val progress = remember { Animatable(1f) }
    val sparks = remember(trigger) {
        val random = Random(trigger * 7919 + 13)
        List(count) {
            Spark(
                angle = random.nextFloat() * 2f * PI.toFloat(),
                speed = 0.45f + random.nextFloat() * 0.75f,
                radiusDp = 1.6f + random.nextFloat() * 2.6f,
                color = colors[random.nextInt(colors.size)],
                spin = (random.nextFloat() - 0.5f) * 4f,
                shard = random.nextFloat() > 0.55f,
            )
        }
    }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(durationMillis, easing = LinearEasing))
        }
    }
    Canvas(modifier) {
        val t = progress.value
        if (t >= 1f) return@Canvas
        val distance = power.toPx()
        val eased = 1f - (1f - t).pow(3)
        val fade = (1f - t).coerceIn(0f, 1f)
        for (spark in sparks) {
            val travel = distance * spark.speed * eased
            val x = center.x + cos(spark.angle) * travel
            val y = center.y + sin(spark.angle) * travel + gravity * distance * t * t
            val p = Offset(x, y)
            val radius = spark.radiusDp.dp.toPx() * (1f - 0.5f * t)
            drawCircle(
                brush = Brush.radialGradient(listOf(spark.color.copy(alpha = 0.45f * fade), Color.Transparent), center = p, radius = radius * 3f),
                radius = radius * 3f,
                center = p,
            )
            if (spark.shard) {
                rotate(spark.spin * 360f * t, pivot = p) {
                    drawRect(
                        color = spark.color.copy(alpha = fade),
                        topLeft = Offset(x - radius * 1.6f, y - radius * 0.5f),
                        size = Size(radius * 3.2f, radius),
                    )
                }
            } else {
                drawCircle(spark.color.copy(alpha = fade), radius = radius, center = p)
            }
        }
    }
}

private class ConfettiPiece(
    val x: Float,
    val delay: Float,
    val speed: Float,
    val phase: Float,
    val swayDp: Float,
    val color: Color,
    val widthDp: Float,
    val heightDp: Float,
    val spin: Float,
)

/** Дождь конфетти с покачиванием и «переворотами» в 3D. */
@Composable
fun ConfettiRain(
    trigger: Int,
    modifier: Modifier = Modifier,
    count: Int = 150,
    durationMillis: Int = 4_200,
    colors: List<Color> = listOf(
        AscendColors.Gold,
        AscendColors.Violet,
        AscendColors.Cyan,
        AscendColors.Pink,
        AscendColors.Success,
        Color.White,
    ),
) {
    val progress = remember { Animatable(1f) }
    val pieces = remember(trigger) {
        val random = Random(trigger * 31 + 7)
        List(count) {
            ConfettiPiece(
                x = random.nextFloat(),
                delay = random.nextFloat() * 0.35f,
                speed = 0.75f + random.nextFloat() * 0.6f,
                phase = random.nextFloat() * 2f * PI.toFloat(),
                swayDp = 8f + random.nextFloat() * 28f,
                color = colors[random.nextInt(colors.size)],
                widthDp = 6f + random.nextFloat() * 5f,
                heightDp = 3f + random.nextFloat() * 4f,
                spin = (random.nextFloat() - 0.5f) * 3f,
            )
        }
    }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(durationMillis, easing = LinearEasing))
        }
    }
    Canvas(modifier.fillMaxSize()) {
        val t = progress.value
        if (t >= 1f) return@Canvas
        val margin = 24.dp.toPx()
        for (piece in pieces) {
            val local = ((t - piece.delay) / (1f - piece.delay)).coerceIn(0f, 1f)
            if (local <= 0f) continue
            val y = -margin + (size.height + margin * 2f) * local * piece.speed
            if (y > size.height + margin) continue
            val x = piece.x * size.width + sin(piece.phase + local * 9f) * piece.swayDp.dp.toPx()
            val p = Offset(x, y)
            val w = piece.widthDp.dp.toPx()
            val h = piece.heightDp.dp.toPx()
            val alpha = if (local > 0.85f) (1f - local) / 0.15f else 1f
            val flip = cos(piece.phase + local * 14f)
            rotate(piece.spin * 540f * local, pivot = p) {
                scale(scaleX = flip, scaleY = 1f, pivot = p) {
                    drawRect(
                        color = piece.color.copy(alpha = alpha.coerceIn(0f, 1f)),
                        topLeft = Offset(x - w / 2f, y - h / 2f),
                        size = Size(w, h),
                    )
                }
            }
        }
    }
}
