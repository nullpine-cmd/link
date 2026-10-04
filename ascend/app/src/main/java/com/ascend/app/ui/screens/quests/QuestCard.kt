package com.ascend.app.ui.screens.quests

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ascend.app.data.local.LogEntity
import com.ascend.app.data.local.QuestEntity
import com.ascend.app.data.local.hasTarget
import com.ascend.app.ui.components.DifficultyStars
import com.ascend.app.ui.components.EmojiTile
import com.ascend.app.ui.components.GlassCard
import com.ascend.app.ui.components.Pill
import com.ascend.app.ui.components.QuestCheck
import com.ascend.app.ui.components.StreakFlame
import com.ascend.app.ui.components.bounceClick
import com.ascend.app.ui.theme.AscendColors
import com.ascend.app.ui.theme.color
import com.ascend.app.ui.theme.icon
import com.ascend.app.ui.util.formatTime

data class QuestMenuAction(val label: String, val icon: ImageVector, val onClick: () -> Unit)

/** Карточка квеста: выполнение, мета-информация, серия и меню действий. */
@Composable
fun QuestCard(
    quest: QuestEntity,
    done: Boolean,
    canComplete: Boolean,
    onCheck: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    streak: Int = 0,
    log: LogEntity? = null,
    subtitle: String? = null,
    badge: String? = null,
    menu: List<QuestMenuAction> = emptyList(),
) {
    val color = quest.attribute.color
    val titleColor by animateColorAsState(
        if (done) AscendColors.TextSecondary else AscendColors.TextPrimary,
        label = "questTitle",
    )
    val contentAlpha by animateFloatAsState(if (done) 0.82f else 1f, label = "questAlpha")
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        accent = if (done) color else null,
        onClick = onClick,
        contentPadding = PaddingValues(start = 14.dp, end = 6.dp, top = 14.dp, bottom = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            QuestCheck(
                checked = done,
                color = color,
                enabled = canComplete && !done,
                onClick = { if (canComplete && !done) onCheck() },
            )
            Spacer(Modifier.width(12.dp))
            EmojiTile(quest.emoji, color, side = 42.dp)
            Spacer(Modifier.width(12.dp))
            Column(
                Modifier
                    .weight(1f)
                    .graphicsLayer { alpha = contentAlpha },
            ) {
                if (badge != null) {
                    Text(badge.uppercase(), style = MaterialTheme.typography.labelSmall, color = AscendColors.GoldDeep)
                    Spacer(Modifier.height(2.dp))
                }
                Text(
                    text = quest.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = titleColor,
                    textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(quest.attribute.icon, contentDescription = quest.attribute.title, tint = color, modifier = Modifier.size(14.dp))
                    DifficultyStars(quest.difficulty.stars, color = AscendColors.Gold, starSize = 9.dp)
                    if (quest.hasTarget) {
                        Text(
                            "${quest.targetAmount} ${quest.unit.orEmpty()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = AscendColors.TextSecondary,
                            maxLines = 1,
                        )
                    }
                    quest.timeMinutes?.let {
                        Text(formatTime(it), style = MaterialTheme.typography.labelMedium, color = AscendColors.TextSecondary)
                    }
                    if (streak > 1) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StreakFlame(diameter = 14.dp)
                            Text("$streak", style = MaterialTheme.typography.labelMedium, color = AscendColors.Ember)
                        }
                    }
                }
                if (subtitle != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AscendColors.TextMuted, maxLines = 1)
                }
            }
            AnimatedVisibility(
                visible = done && log != null,
                enter = scaleIn(spring(dampingRatio = 0.4f, stiffness = 420f)) + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(64.dp)) {
                    Pill("+${log?.xp ?: 0}", AscendColors.Cyan, filled = false)
                    if (log != null && log.amount > 1 && quest.hasTarget) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${log.amount} ${quest.unit.orEmpty()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (log.hitTarget) AscendColors.Success else AscendColors.TextMuted,
                        )
                    }
                }
            }
            if (menu.isNotEmpty()) {
                QuestMenu(menu)
            } else {
                Spacer(Modifier.width(8.dp))
            }
        }
    }
}

@Composable
private fun QuestMenu(actions: List<QuestMenuAction>) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Box(
            Modifier
                .size(36.dp)
                .bounceClick(pressedScale = 0.85f) { expanded = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "Действия", tint = AscendColors.TextMuted)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.label, color = AscendColors.TextPrimary) },
                    leadingIcon = { Icon(action.icon, contentDescription = null, tint = AscendColors.TextSecondary) },
                    onClick = {
                        expanded = false
                        action.onClick()
                    },
                )
            }
        }
    }
}
