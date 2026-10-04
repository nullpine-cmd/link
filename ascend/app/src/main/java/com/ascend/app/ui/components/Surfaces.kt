package com.ascend.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.app.ui.theme.AscendColors

/** Стеклянная карточка: полупрозрачный градиент, тонкая светящаяся кромка, акцентный оттенок. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    accent: Color? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val clickModifier = if (onClick != null) Modifier.bounceClick(pressedScale = 0.97f, onClick = onClick) else Modifier
    Column(
        modifier = modifier
            .then(clickModifier)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.075f), Color.White.copy(alpha = 0.028f)),
                ),
            )
            .then(
                if (accent != null) {
                    Modifier.background(
                        Brush.linearGradient(
                            listOf(accent.copy(alpha = 0.20f), accent.copy(alpha = 0.03f), Color.Transparent),
                        ),
                    )
                } else {
                    Modifier
                },
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        (accent ?: Color.White).copy(alpha = if (accent != null) 0.38f else 0.16f),
                        Color.White.copy(alpha = 0.04f),
                    ),
                ),
                shape = shape,
            )
            .padding(contentPadding),
        content = content,
    )
}

/** Главная кнопка: переливающийся градиент, мягкое свечение снизу и бегущий блик. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: List<Color> = AscendColors.Xp,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    height: Dp = 56.dp,
) {
    val transition = rememberInfiniteTransition(label = "buttonSheen")
    val sheen by transition.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(2_400, delayMillis = 1_400, easing = FastOutSlowInEasing)),
        label = "sheen",
    )
    val alpha by animateFloatAsState(if (enabled) 1f else 0.38f, label = "buttonAlpha")
    Box(
        modifier = modifier
            .height(height)
            .graphicsLayer { this.alpha = alpha }
            .bounceClick(enabled = enabled, onClick = onClick)
            .drawBehind {
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(colors.first().copy(alpha = 0.55f), Color.Transparent),
                        center = Offset(size.width / 2f, size.height * 0.85f),
                        radius = size.width * 0.55f,
                    ),
                    topLeft = Offset(-size.width * 0.05f, size.height * 0.15f),
                    size = Size(size.width * 1.1f, size.height * 1.35f),
                )
            }
            .clip(CircleShape)
            .background(Brush.horizontalGradient(colors))
            .drawWithContent {
                drawContent()
                val x = sheen * size.width
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.32f), Color.Transparent),
                        start = Offset(x - size.height * 1.4f, 0f),
                        end = Offset(x + size.height * 1.4f, size.height),
                    ),
                )
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)),
                    size = Size(size.width, size.height * 0.5f),
                )
            }
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
                color = Color.White,
                maxLines = 1,
            )
        }
    }
}

/** Вторичная кнопка-контур. */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = AscendColors.TextPrimary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 52.dp,
) {
    Row(
        modifier = modifier
            .height(height)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .bounceClick(enabled = enabled, onClick = onClick)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, color.copy(alpha = 0.35f), CircleShape)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = color, maxLines = 1)
    }
}

/** Выбираемый чип с плавной сменой цвета. */
@Composable
fun SelectChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = AscendColors.Violet,
    icon: ImageVector? = null,
    leading: String? = null,
) {
    val background by animateColorAsState(
        if (selected) color.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.05f),
        label = "chipBg",
    )
    val border by animateColorAsState(
        if (selected) color.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.10f),
        label = "chipBorder",
    )
    val content by animateColorAsState(
        if (selected) Color.White else AscendColors.TextSecondary,
        label = "chipContent",
    )
    Row(
        modifier = modifier
            .bounceClick(onClick = onClick)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (selected) color else content, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        if (leading != null) {
            Text(leading, fontSize = 16.sp)
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1)
    }
}

/** Поле ввода в стилистике приложения. */
@Composable
fun AscendTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    maxLength: Int = 80,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    accent: Color = AscendColors.Violet,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        if (focused) accent.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.10f),
        label = "fieldBorder",
    )
    val shape = RoundedCornerShape(18.dp)
    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.take(maxLength)) },
        modifier = modifier.onFocusChanged { focused = it.isFocused },
        singleLine = singleLine,
        textStyle = textStyle.copy(color = AscendColors.TextPrimary),
        cursorBrush = SolidColor(accent),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .clip(shape)
                    .background(Color.White.copy(alpha = if (focused) 0.08f else 0.05f))
                    .border(1.dp, borderColor, shape)
                    .padding(horizontal = 16.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leading != null) {
                    leading()
                    Spacer(Modifier.width(12.dp))
                }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            style = textStyle,
                            color = AscendColors.TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    inner()
                }
                if (trailing != null) {
                    Spacer(Modifier.width(12.dp))
                    trailing()
                }
            }
        },
    )
}

/** Мелкая подпись-«руна» над секциями. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = AscendColors.TextMuted) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier.padding(bottom = 10.dp),
    )
}

@Composable
fun Pill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    filled: Boolean = false,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = if (filled) 0.9f else 0.14f))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (filled) Color.White else color, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (filled) Color.White else color,
            maxLines = 1,
        )
    }
}
