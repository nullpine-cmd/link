package com.ascend.app.ui.screens.quests

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.app.data.local.BookEntity
import com.ascend.app.data.local.QuestEntity
import com.ascend.app.data.local.isHabit
import com.ascend.app.data.local.schedule
import com.ascend.app.ui.components.AscendSheet
import com.ascend.app.ui.components.CircleIconButton
import com.ascend.app.ui.components.CountingText
import com.ascend.app.ui.components.EmojiTile
import com.ascend.app.ui.components.GlassCard
import com.ascend.app.ui.components.GlowProgressBar
import com.ascend.app.ui.components.GradientButton
import com.ascend.app.ui.components.RollingNumber
import com.ascend.app.ui.components.SelectChip
import com.ascend.app.ui.theme.AscendColors
import com.ascend.app.ui.theme.color
import com.ascend.core.Difficulty
import com.ascend.core.RewardEngine
import com.ascend.core.RewardInput
import kotlin.math.roundToInt

/**
 * Запись прогресса. Главный принцип — любое количество засчитывается:
 * одна страница вместо двадцати всё равно приносит полную базовую награду.
 */
@Composable
fun LogProgressSheet(
    title: String,
    emoji: String,
    target: Int?,
    unit: String?,
    difficulty: Difficulty,
    accent: Color,
    book: BookEntity?,
    previewXp: (Int) -> Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val goal = target?.takeIf { it > 0 }
    var amount by rememberSaveable { mutableIntStateOf(goal ?: 10) }
    val sliderMax = ((goal ?: 20) * 2).coerceAtLeast(10)
    val unitLabel = unit.orEmpty()

    AscendSheet(onDismiss = onDismiss) { hide ->
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiTile(emoji, accent, side = 48.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Сколько сегодня?", style = MaterialTheme.typography.headlineSmall, color = AscendColors.TextPrimary)
                    Text(title, style = MaterialTheme.typography.bodyMedium, color = AscendColors.TextSecondary, maxLines = 1)
                }
            }
            Spacer(Modifier.height(22.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                CircleIconButton(Icons.Rounded.Remove, "Меньше", onClick = { amount = (amount - 1).coerceAtLeast(0) }, diameter = 52.dp)
                Spacer(Modifier.width(22.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(150.dp)) {
                    RollingNumber(
                        value = amount.toLong(),
                        style = MaterialTheme.typography.displayLarge,
                        color = AscendColors.TextPrimary,
                    )
                    Text(unitLabel, style = MaterialTheme.typography.labelLarge, color = AscendColors.TextSecondary)
                }
                Spacer(Modifier.width(22.dp))
                CircleIconButton(Icons.Rounded.Add, "Больше", onClick = { amount += 1 }, diameter = 52.dp, tint = accent)
            }
            Spacer(Modifier.height(10.dp))
            Slider(
                value = amount.coerceAtMost(sliderMax).toFloat(),
                onValueChange = { amount = it.roundToInt() },
                valueRange = 0f..sliderMax.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    inactiveTrackColor = accent.copy(alpha = 0.18f),
                ),
            )
            val quick = buildList {
                add(1)
                if (goal != null) {
                    if (goal >= 4) add(goal / 2)
                    add(goal)
                    add((goal * 1.5f).roundToInt())
                    add(goal * 2)
                } else {
                    add(5)
                    add(10)
                    add(20)
                }
            }.distinct().filter { it > 0 }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quick) { value ->
                    SelectChip(
                        text = when (value) {
                            goal -> "Цель · $value"
                            else -> "$value"
                        },
                        selected = amount == value,
                        onClick = { amount = value },
                        color = accent,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            GlassCard(accent = accent, contentPadding = PaddingValues(14.dp)) {
                Text(
                    "✨ Любое количество засчитается — награда за шаг вперёд гарантирована",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AscendColors.TextPrimary,
                )
                if (goal != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "🎯 Цель $goal $unitLabel даёт бонус +50%, вдвое больше — ещё +25%",
                        style = MaterialTheme.typography.bodySmall,
                        color = AscendColors.TextSecondary,
                    )
                }
                AnimatedVisibility(
                    visible = goal != null && amount >= goal,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Text(
                        if (goal != null && amount >= goal * 2) "⚡ Перевыполнение! Легендарное усердие" else "🏆 Цель достигнута — бонус твой",
                        style = MaterialTheme.typography.labelLarge,
                        color = AscendColors.Gold,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            if (book != null) {
                Spacer(Modifier.height(12.dp))
                val newPage = (book.currentPage + amount).coerceAtMost(book.totalPages)
                Text(
                    "📖 ${book.title}: стр. ${book.currentPage} → $newPage из ${book.totalPages}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AscendColors.TextSecondary,
                )
                Spacer(Modifier.height(6.dp))
                GlowProgressBar(
                    progress = newPage.toFloat() / book.totalPages.coerceAtLeast(1),
                    colors = listOf(AscendColors.Cyan, AscendColors.Violet),
                    height = 8.dp,
                )
                if (newPage >= book.totalPages && book.finishedAt == null) {
                    Text(
                        "🏆 Книга будет дочитана! +100 XP",
                        style = MaterialTheme.typography.labelLarge,
                        color = AscendColors.Gold,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
                Text("Награда:", style = MaterialTheme.typography.bodyMedium, color = AscendColors.TextSecondary)
                Spacer(Modifier.width(8.dp))
                CountingText(
                    value = if (amount > 0) previewXp(amount) else 0,
                    prefix = "+",
                    suffix = " XP",
                    durationMillis = 350,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, fontSize = 20.sp),
                    color = AscendColors.Cyan,
                )
                Spacer(Modifier.weight(1f))
                Text(difficulty.title, style = MaterialTheme.typography.labelMedium, color = AscendColors.TextMuted)
            }
            GradientButton(
                text = if (amount > 0) "Засчитать" else "Нужен хотя бы 1 шаг",
                icon = Icons.Rounded.Bolt,
                enabled = amount > 0,
                colors = listOf(accent, accent.copy(alpha = 0.85f), AscendColors.Violet),
                onClick = {
                    val value = amount
                    hide()
                    onConfirm(value)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Шторка для конкретного квеста — с предпросмотром награды по правилам движка. */
@Composable
fun QuestProgressSheet(
    quest: QuestEntity,
    book: BookEntity?,
    today: Long,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    LogProgressSheet(
        title = quest.title,
        emoji = quest.emoji,
        target = quest.targetAmount,
        unit = quest.unit,
        difficulty = quest.difficulty,
        accent = quest.attribute.color,
        book = book,
        previewXp = { amount ->
            RewardEngine.calculate(
                RewardInput(
                    difficulty = quest.difficulty,
                    amount = amount,
                    target = quest.targetAmount,
                    isHabit = quest.isHabit,
                    streakBefore = quest.streak,
                    lastCompletedDay = quest.lastCompletedDay,
                    previousDueDay = if (quest.isHabit) quest.schedule.previousDueDay(today) else null,
                ),
            ).totalXp
        },
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}
