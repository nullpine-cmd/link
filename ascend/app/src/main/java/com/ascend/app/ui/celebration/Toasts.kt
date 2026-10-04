package com.ascend.app.ui.celebration

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.app.domain.RewardOutcome
import com.ascend.app.ui.components.AchievementMedal
import com.ascend.app.ui.components.AttributeOrb
import com.ascend.app.ui.components.CountingText
import com.ascend.app.ui.components.GlowProgressBar
import com.ascend.app.ui.components.GoldCoin
import com.ascend.app.ui.components.ParticleBurst
import com.ascend.app.ui.components.Pill
import com.ascend.app.ui.components.reveal
import com.ascend.app.ui.theme.AscendColors
import com.ascend.app.ui.theme.color
import com.ascend.app.ui.theme.colorDeep
import com.ascend.app.ui.theme.colors
import com.ascend.core.Achievement
import kotlinx.coroutines.delay

/** Каркас тоста: плотное стекло, акцентное свечение, нажатие закрывает. */
@Composable
private fun ToastSurface(
    accent: Color,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(AscendColors.SurfaceTop, AscendColors.Night)))
            .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.28f), Color.Transparent, accent.copy(alpha = 0.08f))))
            .border(1.dp, Brush.verticalGradient(listOf(accent.copy(alpha = 0.7f), Color.White.copy(alpha = 0.05f))), shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            .padding(16.dp),
        content = content,
    )
}

@Composable
private fun rememberBurst(delayMillis: Long = 140): Int {
    var burst by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        delay(delayMillis)
        burst = 1
    }
    return burst
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RewardToast(outcome: RewardOutcome, onUndo: () -> Unit, onDismiss: () -> Unit) {
    val color = outcome.attribute.color
    val burst = rememberBurst()
    val transition = rememberInfiniteTransition(label = "rewardOrb")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    val attributeBar = remember {
        Animatable(if (outcome.attributeLeveledUp) 0f else outcome.attributeBefore.progress)
    }
    LaunchedEffect(Unit) {
        delay(350)
        attributeBar.animateTo(outcome.attributeAfter.progress, tween(900, easing = FastOutSlowInEasing))
    }

    ToastSurface(accent = color, onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                ParticleBurst(
                    trigger = burst,
                    colors = listOf(color, AscendColors.Gold, Color.White),
                    modifier = Modifier.size(56.dp),
                    count = 26,
                    power = 64.dp,
                )
                AttributeOrb(
                    outcome.attribute,
                    diameter = 48.dp,
                    modifier = Modifier.graphicsLayer {
                        val s = 1f + 0.06f * pulse
                        scaleX = s
                        scaleY = s
                    },
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    outcome.message,
                    style = MaterialTheme.typography.titleMedium,
                    color = AscendColors.TextPrimary,
                    maxLines = 2,
                )
                Text(
                    "${outcome.emoji}  ${outcome.title}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AscendColors.TextSecondary,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                CountingText(
                    value = outcome.xp,
                    prefix = "+",
                    suffix = " XP",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = AscendColors.Cyan,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GoldCoin(diameter = 14.dp)
                    Spacer(Modifier.width(4.dp))
                    CountingText(
                        value = outcome.gold,
                        prefix = "+",
                        style = MaterialTheme.typography.labelLarge,
                        color = AscendColors.Gold,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${outcome.attribute.title} · ур. ${outcome.attributeAfter.level}",
                style = MaterialTheme.typography.labelMedium,
                color = color,
            )
            if (outcome.attributeLeveledUp) {
                Spacer(Modifier.width(8.dp))
                Pill("Характеристика выросла!", color, filled = true)
            }
            Spacer(Modifier.weight(1f))
            if (outcome.target != null) {
                Text(
                    "${outcome.amount} / ${outcome.target} ${outcome.unit.orEmpty()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = AscendColors.TextSecondary,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        GlowProgressBar(
            progress = attributeBar.value,
            colors = listOf(color, outcome.attribute.colorDeep),
            height = 7.dp,
        )
        if (outcome.bonuses.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                outcome.bonuses.forEachIndexed { index, bonus ->
                    Pill(
                        "${bonus.kind.title} +${bonus.xp}",
                        AscendColors.Gold,
                        modifier = Modifier.reveal(index + 2, delayStep = 90L, offsetY = 10.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (outcome.streak > 1) {
                Text(
                    "🔥 Серия: ${outcome.streak}",
                    style = MaterialTheme.typography.labelMedium,
                    color = AscendColors.Ember,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                "Отменить",
                style = MaterialTheme.typography.labelLarge,
                color = AscendColors.TextSecondary,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onUndo)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
fun MilestoneToast(item: CelebrationItem.Milestone, onDismiss: () -> Unit) {
    val burst = rememberBurst()
    ToastSurface(accent = AscendColors.Gold, onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                ParticleBurst(
                    trigger = burst,
                    colors = AscendColors.GoldGradient + Color.White,
                    modifier = Modifier.size(60.dp),
                    count = 30,
                    power = 70.dp,
                )
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(AscendColors.Gold.copy(alpha = 0.5f), AscendColors.Gold.copy(alpha = 0.08f)))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(item.emoji, fontSize = 28.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, color = AscendColors.Gold)
                Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = AscendColors.TextSecondary, maxLines = 2)
            }
            RewardNumbers(item.xp, item.gold)
        }
    }
}

@Composable
fun AchievementToast(achievement: Achievement, onDismiss: () -> Unit) {
    val burst = rememberBurst(220)
    val tierColor = achievement.tier.colors.first()
    ToastSurface(accent = tierColor, onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                ParticleBurst(
                    trigger = burst,
                    colors = achievement.tier.colors + Color.White,
                    modifier = Modifier.size(64.dp),
                    count = 34,
                    power = 80.dp,
                )
                AchievementMedal(achievement, unlocked = true, diameter = 56.dp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "ДОСТИЖЕНИЕ · ${achievement.tier.title.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = tierColor,
                )
                Text(achievement.title, style = MaterialTheme.typography.titleMedium, color = AscendColors.TextPrimary)
                Text(achievement.description, style = MaterialTheme.typography.bodySmall, color = AscendColors.TextSecondary, maxLines = 2)
            }
            RewardNumbers(achievement.tier.xp, achievement.tier.gold)
        }
    }
}

@Composable
fun PurchaseToast(item: CelebrationItem.Purchase, onDismiss: () -> Unit) {
    val burst = rememberBurst()
    ToastSurface(accent = AscendColors.Pink, onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                ParticleBurst(
                    trigger = burst,
                    colors = listOf(AscendColors.Gold, AscendColors.Pink, Color.White),
                    modifier = Modifier.size(56.dp),
                    count = 24,
                )
                Text(item.emoji, fontSize = 32.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Награда твоя!", style = MaterialTheme.typography.titleMedium, color = AscendColors.TextPrimary)
                Text(
                    "${item.title} — наслаждайся, ты это заслужил",
                    style = MaterialTheme.typography.bodySmall,
                    color = AscendColors.TextSecondary,
                    maxLines = 2,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                GoldCoin(diameter = 16.dp)
                Spacer(Modifier.width(4.dp))
                Text("−${item.cost}", style = MaterialTheme.typography.titleMedium, color = AscendColors.Gold)
            }
        }
    }
}

@Composable
private fun RewardNumbers(xp: Int, gold: Int) {
    Column(horizontalAlignment = Alignment.End) {
        CountingText(
            value = xp,
            prefix = "+",
            suffix = " XP",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = AscendColors.Cyan,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            GoldCoin(diameter = 14.dp)
            Spacer(Modifier.width(4.dp))
            CountingText(value = gold, prefix = "+", style = MaterialTheme.typography.labelLarge, color = AscendColors.Gold)
        }
    }
}
