package com.ascend.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Пружинное «вдавливание» при нажатии. */
fun Modifier.pressScale(interactionSource: InteractionSource, pressedScale: Float = 0.95f): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 700f),
        label = "pressScale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Клик без ряби, но с упругим откликом — основной тактильный язык интерфейса. */
fun Modifier.bounceClick(
    enabled: Boolean = true,
    pressedScale: Float = 0.95f,
    onClick: () -> Unit,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    this
        .pressScale(source, pressedScale)
        .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
}

/**
 * Каскадное появление: элемент всплывает снизу с лёгким масштабом.
 * Проигрывается один раз за жизнь элемента (переживает прокрутку и смену вкладок).
 */
fun Modifier.reveal(index: Int = 0, delayStep: Long = 55L, offsetY: Dp = 28.dp): Modifier = composed {
    var played by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (played) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!played) {
            delay(index.coerceIn(0, 10) * delayStep)
            progress.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 210f))
            played = true
        }
    }
    graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * offsetY.toPx()
        val s = 0.94f + 0.06f * p
        scaleX = s
        scaleY = s
    }
}
