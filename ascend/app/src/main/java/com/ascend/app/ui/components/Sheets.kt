package com.ascend.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ascend.app.ui.theme.AscendColors
import kotlinx.coroutines.launch

/**
 * Нижняя шторка в стиле приложения. В содержимое передаётся `hide` —
 * плавно закрывает шторку и лишь затем вызывает [onDismiss].
 */
@Composable
fun AscendSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.(hide: () -> Unit) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val hide: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AscendColors.Night,
        contentColor = AscendColors.TextPrimary,
        scrimColor = Color.Black.copy(alpha = 0.62f),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .size(width = 44.dp, height = 5.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(AscendColors.Xp)),
            )
        },
    ) {
        content(hide)
    }
}

/** Сегментированный переключатель с перетекающим индикатором. */
@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = AscendColors.Violet,
) {
    val shape = RoundedCornerShape(18.dp)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .padding(4.dp),
    ) {
        val segment = maxWidth / options.size
        val index = options.indexOf(selected).coerceAtLeast(0)
        val offset by animateDpAsState(
            targetValue = segment * index,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = 420f),
            label = "segment",
        )
        Box(
            Modifier
                .offset(x = offset)
                .width(segment)
                .fillMaxHeight()
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.55f), color.copy(alpha = 0.30f))))
                .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEach { option ->
                val active = option == selected
                val textColor by animateColorAsState(
                    if (active) Color.White else AscendColors.TextSecondary,
                    label = "segmentText",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .bounceClick(pressedScale = 0.94f) { onSelect(option) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label(option), style = MaterialTheme.typography.labelLarge, color = textColor, maxLines = 1)
                }
            }
        }
    }
}
