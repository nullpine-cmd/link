package com.ascend.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowUp
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ascend.app.ui.theme.colors
import com.ascend.core.Achievement
import com.ascend.core.Metric
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val Metric.icon: ImageVector
    get() = when (this) {
        Metric.QUESTS -> Icons.Rounded.TaskAlt
        Metric.BEST_DAY_STREAK -> Icons.Rounded.LocalFireDepartment
        Metric.PAGES -> Icons.Rounded.AutoStories
        Metric.BOOKS -> Icons.Rounded.MenuBook
        Metric.LEVEL -> Icons.Rounded.KeyboardDoubleArrowUp
        Metric.MIN_ATTRIBUTE_LEVEL -> Icons.Rounded.Balance
        Metric.MAX_ATTRIBUTE_LEVEL -> Icons.Rounded.Star
        Metric.EARLY_COMPLETIONS -> Icons.Rounded.WbTwilight
        Metric.LATE_COMPLETIONS -> Icons.Rounded.Bedtime
        Metric.OVERACHIEVEMENTS -> Icons.Rounded.Bolt
        Metric.COMEBACKS -> Icons.Rounded.Autorenew
        Metric.PURCHASES -> Icons.Rounded.Redeem
        Metric.PERFECT_DAYS -> Icons.Rounded.AutoAwesome
        Metric.CHALLENGES -> Icons.Rounded.Flag
        Metric.BOSSES -> Icons.Rounded.Shield
        Metric.TALENTS -> Icons.Rounded.AutoFixHigh
        Metric.COMBO -> Icons.Rounded.Whatshot
    }

/** Медаль достижения: зубчатая звезда цвета ступени, блик и иконка. Закрытая — тусклая с замком. */
@Composable
fun AchievementMedal(
    achievement: Achievement,
    unlocked: Boolean,
    modifier: Modifier = Modifier,
    diameter: Dp = 64.dp,
) {
    val transition = rememberInfiniteTransition(label = "medal")
    val sheen by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(2_800, delayMillis = 2_600, easing = LinearEasing)),
        label = "sheen",
    )
    val tierColors = achievement.tier.colors
    val locked = listOf(Color(0xFF3A3657), Color(0xFF231F3D))
    val colors = if (unlocked) tierColors else locked
    Box(
        modifier = modifier
            .size(diameter)
            .drawWithCache {
                val mid = Offset(size.width / 2f, size.height / 2f)
                val r = size.minDimension / 2f
                val rosette = rosettePath(mid, r * 0.96f, r * 0.84f, 16)
                val fill = Brush.linearGradient(colors, start = Offset.Zero, end = Offset(size.width, size.height))
                onDrawBehind {
                    if (unlocked) {
                        drawCircle(
                            brush = Brush.radialGradient(listOf(tierColors.first().copy(alpha = 0.45f), Color.Transparent), center = mid, radius = r * 1.3f),
                            radius = r * 1.3f,
                            center = mid,
                        )
                    }
                    drawPath(rosette, fill)
                    drawCircle(Color.Black.copy(alpha = if (unlocked) 0.18f else 0.3f), r * 0.66f, mid)
                    drawCircle(Color.White.copy(alpha = if (unlocked) 0.45f else 0.08f), r * 0.66f, mid, style = Stroke(1.2.dp.toPx()))
                    if (unlocked) {
                        clipPath(rosette) {
                            val x = sheen * size.width
                            drawRect(
                                Brush.linearGradient(
                                    listOf(Color.Transparent, Color.White.copy(alpha = 0.5f), Color.Transparent),
                                    start = Offset(x - size.width * 0.3f, 0f),
                                    end = Offset(x + size.width * 0.3f, size.height),
                                ),
                            )
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (unlocked) achievement.metric.icon else Icons.Rounded.Lock,
            contentDescription = achievement.title,
            tint = if (unlocked) Color.White else Color.White.copy(alpha = 0.35f),
            modifier = Modifier.size(diameter * 0.38f),
        )
    }
}

private fun rosettePath(center: Offset, outer: Float, inner: Float, teeth: Int): Path = Path().apply {
    val total = teeth * 2
    for (i in 0 until total) {
        val r = if (i % 2 == 0) outer else inner
        val a = (-PI / 2 + i * PI / teeth).toFloat()
        val x = center.x + cos(a) * r
        val y = center.y + sin(a) * r
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}
