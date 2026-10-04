package com.ascend.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ascend.app.ui.theme.AscendColors

enum class MainTab(val label: String, val icon: ImageVector, val route: String) {
    TODAY("День", Icons.Rounded.WbSunny, "today"),
    QUESTS("Квесты", Icons.Rounded.TaskAlt, "quests"),
    LIBRARY("Книги", Icons.Rounded.AutoStories, "library"),
    HERO("Герой", Icons.Rounded.Shield, "hero"),
}

/**
 * Парящая стеклянная панель навигации: подсветка вкладки перетекает с пружиной,
 * в центре — светящаяся кнопка нового квеста.
 */
@Composable
fun AscendBottomBar(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val slots = listOf(MainTab.TODAY, MainTab.QUESTS, null, MainTab.LIBRARY, MainTab.HERO)
    val selectedSlot = slots.indexOf(selected)
    val shape = RoundedCornerShape(30.dp)
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(70.dp)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(AscendColors.SurfaceHigh.copy(alpha = 0.92f), AscendColors.Night.copy(alpha = 0.96f)),
                    ),
                )
                .border(
                    1.dp,
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.03f))),
                    shape,
                ),
        ) {
            val slotWidth = maxWidth / slots.size
            val indicatorX by animateDpAsState(
                targetValue = slotWidth * selectedSlot + (slotWidth - 56.dp) / 2,
                animationSpec = spring(dampingRatio = 0.68f, stiffness = 380f),
                label = "indicator",
            )
            Box(
                Modifier
                    .offset(x = indicatorX, y = 9.dp)
                    .size(width = 56.dp, height = 52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(AscendColors.Violet.copy(alpha = 0.42f), AscendColors.Indigo.copy(alpha = 0.12f)),
                        ),
                    )
                    .border(1.dp, AscendColors.VioletLight.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
            )
            Row(Modifier.fillMaxWidth().fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                slots.forEach { tab ->
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        if (tab == null) {
                            AddOrb(onAdd)
                        } else {
                            TabItem(tab, tab == selected) { onSelect(tab) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabItem(tab: MainTab, selected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.35f, stiffness = 500f),
        label = "tabScale",
    )
    val tint by animateColorAsState(if (selected) Color.White else AscendColors.TextMuted, label = "tabTint")
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .bounceClick(pressedScale = 0.86f, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            tab.icon,
            contentDescription = tab.label,
            tint = tint,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        )
        Spacer(Modifier.height(3.dp))
        Text(tab.label, style = MaterialTheme.typography.labelSmall, color = tint, maxLines = 1)
    }
}

@Composable
private fun AddOrb(onAdd: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "addOrb")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4_000, easing = LinearEasing)),
        label = "spin",
    )
    Box(
        Modifier
            .size(54.dp)
            .bounceClick(pressedScale = 0.85f, onClick = onAdd)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(listOf(AscendColors.Violet.copy(alpha = 0.6f), Color.Transparent)),
                    radius = size.minDimension * 0.75f,
                )
                rotate(spin) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            listOf(AscendColors.Cyan, Color.Transparent, AscendColors.Pink, Color.Transparent, AscendColors.Cyan),
                        ),
                        radius = size.minDimension / 2f,
                        style = Stroke(2.dp.toPx()),
                    )
                }
            }
            .padding(4.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(AscendColors.Violet, AscendColors.Indigo, AscendColors.Cyan))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Add, contentDescription = "Новый квест", tint = Color.White, modifier = Modifier.size(28.dp))
    }
}

/** Высота панели вместе с отступами — без учёта системной навигации. */
private val BottomBarSpace = 92.dp

/** Отступ снизу для прокручиваемого содержимого, чтобы последний элемент не прятался под панелью. */
@Composable
fun bottomBarContentPadding(): Dp =
    BottomBarSpace + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
