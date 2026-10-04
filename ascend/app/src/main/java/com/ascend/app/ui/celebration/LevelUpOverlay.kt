package com.ascend.app.ui.celebration

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.app.ui.components.ConfettiRain
import com.ascend.app.ui.components.GradientButton
import com.ascend.app.ui.components.RankBadge
import com.ascend.app.ui.theme.AscendColors
import com.ascend.app.ui.theme.RuneLabel
import com.ascend.app.ui.theme.colors
import com.ascend.core.Rank
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Церемония нового уровня: вращающиеся лучи славы, ударные волны, дождь конфетти,
 * число уровня влетает с пружиной, при смене ранга — торжественно появляется новый знак.
 */
@Composable
fun LevelUpOverlay(item: CelebrationItem.LevelUp, onContinue: () -> Unit) {
    val rank = Rank.forLevel(item.after.level)
    val rankUp = rank != Rank.forLevel(item.before.level)
    val accent = if (rankUp) rank.colors.first() else AscendColors.Gold
    val haptics = LocalHapticFeedback.current

    val numberScale = remember { Animatable(0.2f) }
    val numberAlpha = remember { Animatable(0f) }
    val spacing = remember { Animatable(0f) }
    val badge = remember { Animatable(0f) }
    val details = remember { Animatable(0f) }
    var confetti by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        launch { spacing.animateTo(1f, tween(1_400, easing = FastOutSlowInEasing)) }
        delay(160)
        confetti = 1
        launch { numberAlpha.animateTo(1f, tween(260)) }
        launch { numberScale.animateTo(1f, spring(dampingRatio = 0.38f, stiffness = 170f)) }
        delay(450)
        if (rankUp) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            launch { badge.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 120f)) }
            delay(300)
        }
        details.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
    }

    BackHandler(onBack = onContinue)

    val transition = rememberInfiniteTransition(label = "levelUp")
    val rays by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(26_000, easing = LinearEasing)),
        label = "rays",
    )
    val wave by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_400, easing = LinearEasing)),
        label = "wave",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AscendColors.Void.copy(alpha = 0.9f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .drawBehind {
                val c = Offset(size.width / 2f, size.height * 0.36f)
                val length = size.maxDimension
                drawCircle(
                    brush = Brush.radialGradient(listOf(accent.copy(alpha = 0.35f), Color.Transparent), center = c, radius = size.width * 0.8f),
                    radius = size.width * 0.8f,
                    center = c,
                )
                rotate(rays, c) {
                    for (i in 0 until 14) {
                        val start = (i * 360f / 14f) * (PI.toFloat() / 180f)
                        val half = 5f * (PI.toFloat() / 180f)
                        val ray = Path().apply {
                            moveTo(c.x, c.y)
                            lineTo(c.x + cos(start - half) * length, c.y + sin(start - half) * length)
                            lineTo(c.x + cos(start + half) * length, c.y + sin(start + half) * length)
                            close()
                        }
                        drawPath(
                            ray,
                            Brush.radialGradient(listOf(accent.copy(alpha = 0.20f), Color.Transparent), center = c, radius = length * 0.6f),
                        )
                    }
                }
                for (k in 0 until 3) {
                    val phase = (wave + k / 3f) % 1f
                    drawCircle(
                        color = accent.copy(alpha = 0.35f * (1f - phase)),
                        radius = size.width * 0.12f + phase * size.width * 0.6f,
                        center = c,
                        style = Stroke((3f * (1f - phase) + 0.5f) * density),
                    )
                }
            },
    ) {
        ConfettiRain(trigger = confetti)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.7f))
            Text(
                "НОВЫЙ УРОВЕНЬ",
                style = RuneLabel.copy(fontSize = 14.sp, letterSpacing = (2f + 8f * spacing.value).sp),
                color = accent,
                modifier = Modifier.graphicsLayer { alpha = spacing.value },
            )
            Text(
                text = item.after.level.toString(),
                style = TextStyle(
                    brush = Brush.verticalGradient(
                        if (rankUp) listOf(Color.White) + rank.colors else listOf(Color.White) + AscendColors.GoldGradient,
                    ),
                    fontSize = 132.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-4).sp,
                    shadow = Shadow(accent.copy(alpha = 0.6f), Offset(0f, 0f), 40f),
                ),
                modifier = Modifier.graphicsLayer {
                    scaleX = numberScale.value
                    scaleY = numberScale.value
                    alpha = numberAlpha.value
                },
            )
            if (rankUp) {
                RankBadge(
                    rank = rank,
                    diameter = 96.dp,
                    modifier = Modifier.graphicsLayer {
                        val b = badge.value
                        scaleX = 0.3f + 0.7f * b
                        scaleY = 0.3f + 0.7f * b
                        rotationZ = (1f - b) * -180f
                        alpha = b.coerceIn(0f, 1f)
                    },
                )
                Spacer(Modifier.height(14.dp))
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = details.value
                    translationY = (1f - details.value) * 24.dp.toPx()
                },
            ) {
                Text(
                    if (rankUp) "Ранг ${rank.letter} · ${rank.title}" else "Ты стал сильнее",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AscendColors.TextPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (rankUp) rank.motto else nextRankHint(item.after.level),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AscendColors.TextSecondary,
                    textAlign = TextAlign.Center,
                )
                item.outcome?.let { outcome ->
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "+${outcome.xp} XP · ${outcome.emoji} ${outcome.title}",
                        style = MaterialTheme.typography.labelLarge,
                        color = AscendColors.Cyan,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            GradientButton(
                text = "Продолжить путь",
                onClick = onContinue,
                colors = if (rankUp) rank.colors.take(2) + AscendColors.Violet else AscendColors.Xp,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = details.value },
            )
            Spacer(Modifier.height(48.dp))
        }
    }
}

private fun nextRankHint(level: Int): String {
    val next = Rank.forLevel(level).next ?: return "Ты на вершине. Легенда продолжается!"
    val left = next.minLevel - level
    return "До ранга ${next.letter} «${next.title}» — ${levelsWord(left)}"
}

private fun levelsWord(n: Int): String {
    val mod10 = n % 10
    val mod100 = n % 100
    val word = when {
        mod10 == 1 && mod100 != 11 -> "уровень"
        mod10 in 2..4 && mod100 !in 12..14 -> "уровня"
        else -> "уровней"
    }
    return "$n $word"
}
