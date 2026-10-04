package com.ascend.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.app.ui.theme.AscendColors
import com.ascend.app.ui.theme.color
import com.ascend.app.ui.theme.icon
import com.ascend.core.Attribute

/** Монета с вращением «ребром» — символ золота. */
@Composable
fun GoldCoin(modifier: Modifier = Modifier, diameter: Dp = 18.dp, spinning: Boolean = false) {
    val transition = rememberInfiniteTransition(label = "coin")
    val turn by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3_200, easing = LinearEasing)),
        label = "turn",
    )
    Canvas(
        modifier
            .size(diameter)
            .graphicsLayer {
                if (spinning) {
                    rotationY = turn
                    cameraDistance = 12f * density
                }
            },
    ) {
        val r = size.minDimension / 2f
        drawCircle(Brush.linearGradient(AscendColors.GoldGradient, start = Offset.Zero, end = Offset(size.width, size.height)), r)
        drawCircle(Color(0xFFB45309).copy(alpha = 0.6f), r * 0.72f, style = Stroke(r * 0.12f))
        drawCircle(Color.White.copy(alpha = 0.55f), r * 0.22f, Offset(center.x - r * 0.3f, center.y - r * 0.3f))
    }
}

@Composable
fun GoldPill(gold: Long, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(AscendColors.Gold.copy(alpha = 0.12f))
            .border(1.dp, AscendColors.Gold.copy(alpha = 0.35f), CircleShape)
            .padding(start = 6.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GoldCoin(diameter = 18.dp, spinning = true)
        Spacer(Modifier.width(6.dp))
        RollingNumber(
            value = gold,
            style = MaterialTheme.typography.labelLarge,
            color = AscendColors.Gold,
        )
    }
}

@Composable
fun AttributeTag(attribute: Attribute, modifier: Modifier = Modifier, showTitle: Boolean = true) {
    Pill(
        text = if (showTitle) attribute.title else attribute.short,
        color = attribute.color,
        icon = attribute.icon,
        modifier = modifier,
    )
}

/** Иконка характеристики в светящемся круге. */
@Composable
fun AttributeOrb(attribute: Attribute, modifier: Modifier = Modifier, diameter: Dp = 40.dp) {
    Box(
        modifier = modifier
            .size(diameter)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(attribute.color.copy(alpha = 0.45f), attribute.color.copy(alpha = 0.10f)),
                ),
            )
            .border(1.dp, attribute.color.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(attribute.icon, contentDescription = attribute.title, tint = Color.White, modifier = Modifier.size(diameter * 0.5f))
    }
}

/** Эмодзи в мягком квадрате с оттенком характеристики. */
@Composable
fun EmojiTile(emoji: String, tint: Color, modifier: Modifier = Modifier, side: Dp = 44.dp) {
    Box(
        modifier = modifier
            .size(side)
            .clip(RoundedCornerShape(side * 0.32f))
            .background(Brush.linearGradient(listOf(tint.copy(alpha = 0.30f), tint.copy(alpha = 0.08f))))
            .border(1.dp, tint.copy(alpha = 0.30f), RoundedCornerShape(side * 0.32f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = (side.value * 0.48f).sp)
    }
}

@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = AscendColors.TextPrimary,
    diameter: Dp = 44.dp,
) {
    Box(
        modifier = modifier
            .size(diameter)
            .bounceClick(pressedScale = 0.88f, onClick = onClick)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(diameter * 0.5f))
    }
}

/** Заголовок экрана второго уровня с кнопкой «назад». */
@Composable
fun DetailTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", onBack)
        Spacer(Modifier.width(14.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            color = AscendColors.TextPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        actions()
    }
}

/** Пустое состояние с пульсирующим символом и подсказкой. */
@Composable
fun EmptyState(
    emoji: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val transition = rememberInfiniteTransition(label = "empty")
    val float by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3_000, easing = LinearEasing)),
        label = "float",
    )
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(96.dp)
                .graphicsLayer {
                    translationY = kotlin.math.sin(float * 2f * Math.PI.toFloat()) * 6.dp.toPx()
                }
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(AscendColors.Violet.copy(alpha = 0.35f), Color.Transparent))),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 44.sp)
        }
        Spacer(Modifier.height(14.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = AscendColors.TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = AscendColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(18.dp))
            action()
        }
    }
}

/** Плитка статистики. */
@Composable
fun StatTile(
    value: Long,
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier, contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(10.dp))
        RollingNumber(
            value = value,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
            color = AscendColors.TextPrimary,
        )
        Text(label, style = MaterialTheme.typography.bodySmall, color = AscendColors.TextSecondary, maxLines = 1)
    }
}
