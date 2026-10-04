package com.ascend.app.ui.screens.library

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascend.app.data.local.BookEntity
import com.ascend.app.data.local.isFinished
import com.ascend.app.data.local.isHabit
import com.ascend.app.data.local.pagesLeft
import com.ascend.app.data.local.progress
import com.ascend.app.data.local.schedule
import com.ascend.app.di.appViewModel
import com.ascend.app.ui.components.BookCover
import com.ascend.app.ui.components.CircleIconButton
import com.ascend.app.ui.components.DetailTopBar
import com.ascend.app.ui.components.EmptyState
import com.ascend.app.ui.components.GhostButton
import com.ascend.app.ui.components.GlassCard
import com.ascend.app.ui.components.GradientButton
import com.ascend.app.ui.components.Pill
import com.ascend.app.ui.components.ProgressRing
import com.ascend.app.ui.components.RollingNumber
import com.ascend.app.ui.components.SectionLabel
import com.ascend.app.ui.components.SelectChip
import com.ascend.app.ui.components.StreakFlame
import com.ascend.app.ui.components.reveal
import com.ascend.app.ui.screens.quests.LogProgressSheet
import com.ascend.app.ui.theme.AscendColors
import com.ascend.app.ui.theme.BookPalettes
import com.ascend.app.ui.theme.color
import com.ascend.app.ui.util.dateLong
import com.ascend.app.ui.util.daysWord
import com.ascend.app.ui.util.pagesWord
import com.ascend.app.ui.util.relativeDay
import com.ascend.core.Attribute
import com.ascend.core.Difficulty
import com.ascend.core.RewardEngine
import com.ascend.core.RewardInput
import com.ascend.core.Streaks
import com.ascend.core.Units
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BookDetailRoute(bookId: Long, onBack: () -> Unit) {
    val viewModel = appViewModel(key = "book-$bookId") {
        BookDetailViewModel(bookId, it.repository, it.celebrations, it.time)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var logging by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val book = state.book

    LaunchedEffect(state.loading, book) {
        if (!state.loading && book == null) onBack()
    }

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(
            title = "",
            onBack = onBack,
            modifier = Modifier.statusBarsPadding(),
            actions = {
                if (book != null) {
                    CircleIconButton(Icons.Rounded.Edit, "Изменить", { editing = true })
                    Spacer(Modifier.width(8.dp))
                    CircleIconButton(Icons.Rounded.DeleteOutline, "Удалить", { confirmDelete = true }, tint = AscendColors.Danger)
                }
            },
        )
        if (book != null) {
            BookDetailContent(
                state = state,
                book = book,
                onLog = { logging = true },
                onFinish = viewModel::finish,
                onCreateQuest = viewModel::createReadingQuest,
            )
        }
    }

    if (logging && book != null) {
        val quest = state.quests.firstOrNull { it.isHabit && it.schedule.isDue(state.today) && it.lastCompletedDay != state.today }
        LogProgressSheet(
            title = book.title,
            emoji = "📖",
            target = quest?.targetAmount ?: 20,
            unit = Units.PAGES,
            difficulty = quest?.difficulty ?: Difficulty.EASY,
            accent = Attribute.INTELLECT.color,
            book = book,
            previewXp = { pages ->
                if (quest != null) {
                    RewardEngine.calculate(
                        RewardInput(
                            difficulty = quest.difficulty,
                            amount = pages,
                            target = quest.targetAmount,
                            isHabit = true,
                            streakBefore = quest.streak,
                            lastCompletedDay = quest.lastCompletedDay,
                            previousDueDay = quest.schedule.previousDueDay(state.today),
                        ),
                    ).totalXp
                } else {
                    RewardEngine.readingSession(pages, false, null).totalXp
                }
            },
            onDismiss = { logging = false },
            onConfirm = viewModel::logReading,
        )
    }
    if (editing && book != null) {
        BookEditorSheet(initial = book, onDismiss = { editing = false })
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = AscendColors.SurfaceHigh,
            title = { Text("Убрать книгу с полки?") },
            text = { Text("Прочитанные страницы и опыт останутся с тобой. Связанные квесты продолжат работать без книги.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete()
                }) { Text("Убрать", color = AscendColors.Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun BookDetailContent(
    state: BookDetailUiState,
    book: BookEntity,
    onLog: () -> Unit,
    onFinish: () -> Unit,
    onCreateQuest: (Int) -> Unit,
) {
    val glow = BookPalettes.of(book.palette).first()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FloatingCover(book, glow)
        Spacer(Modifier.height(18.dp))
        Text(
            book.title,
            style = MaterialTheme.typography.headlineMedium,
            color = AscendColors.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.reveal(1),
        )
        if (book.author.isNotBlank()) {
            Text(
                book.author,
                style = MaterialTheme.typography.bodyLarge,
                color = AscendColors.TextSecondary,
                modifier = Modifier.reveal(2),
            )
        }
        val finishedAt = book.finishedAt
        if (book.isFinished && finishedAt != null) {
            Spacer(Modifier.height(10.dp))
            Pill("Прочитана · ${dateLong(dayOf(finishedAt))}", AscendColors.Success, icon = Icons.Rounded.EmojiEvents)
        }
        Spacer(Modifier.height(20.dp))

        GlassCard(Modifier.fillMaxWidth().reveal(3), accent = glow) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(
                    progress = book.progress,
                    modifier = Modifier.size(96.dp),
                    colors = listOf(AscendColors.Cyan, AscendColors.Violet, glow),
                    stroke = 9.dp,
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        RollingNumber(
                            value = (book.progress * 100).toLong(),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                            color = AscendColors.TextPrimary,
                        )
                        Text("%", style = MaterialTheme.typography.labelLarge, color = AscendColors.TextSecondary)
                    }
                }
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "стр. ${book.currentPage} из ${book.totalPages}",
                        style = MaterialTheme.typography.titleLarge,
                        color = AscendColors.TextPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (book.isFinished) "Книга покорена!" else "Осталось: ${pagesWord(book.pagesLeft)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AscendColors.TextSecondary,
                    )
                    val daily = state.quests.firstOrNull { it.isHabit }?.targetAmount
                    if (!book.isFinished && daily != null && daily > 0) {
                        val days = (book.pagesLeft + daily - 1) / daily
                        Text(
                            "≈ ${daysWord(days)} при $daily стр. в день",
                            style = MaterialTheme.typography.bodySmall,
                            color = AscendColors.TextMuted,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth().reveal(4), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GradientButton(
                text = "Записать чтение",
                icon = Icons.Rounded.Bolt,
                onClick = onLog,
                colors = listOf(AscendColors.Cyan, AscendColors.Indigo, AscendColors.Violet),
                modifier = Modifier.weight(1.4f),
            )
            if (!book.isFinished) {
                GhostButton(
                    text = "Дочитал!",
                    onClick = onFinish,
                    color = AscendColors.Gold,
                    height = 56.dp,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        val readingQuest = state.quests.firstOrNull { it.isHabit }
        if (readingQuest != null) {
            val streak = Streaks.visibleHabitStreak(readingQuest.schedule, readingQuest.streak, readingQuest.lastCompletedDay, state.today)
            GlassCard(Modifier.fillMaxWidth().reveal(5)) {
                SectionLabel("Квест чтения")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📖", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${readingQuest.targetAmount ?: 1} стр. · ${readingQuest.schedule.label().lowercase()}",
                            style = MaterialTheme.typography.titleMedium,
                            color = AscendColors.TextPrimary,
                        )
                        Text(
                            if (readingQuest.lastCompletedDay == state.today) "Сегодня уже выполнен ✓" else "Любое число страниц засчитается",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (readingQuest.lastCompletedDay == state.today) AscendColors.Success else AscendColors.TextMuted,
                        )
                    }
                    if (streak > 0) {
                        StreakFlame(diameter = 22.dp)
                        Text("$streak", style = MaterialTheme.typography.titleMedium, color = AscendColors.Ember)
                    }
                }
            }
        } else if (!book.isFinished) {
            var pages by remember { mutableIntStateOf(20) }
            GlassCard(Modifier.fillMaxWidth().reveal(5)) {
                SectionLabel("Квест чтения")
                Text(
                    "Поставь посильную цель на день. Даже одна прочитанная страница засчитается полностью",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AscendColors.TextSecondary,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 20, 40).forEach { value ->
                        SelectChip("$value стр.", selected = pages == value, color = AscendColors.Cyan, onClick = { pages = value })
                    }
                }
                Spacer(Modifier.height(12.dp))
                GhostButton(
                    "Создать ежедневный квест",
                    onClick = { onCreateQuest(pages) },
                    color = AscendColors.Cyan,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Column(Modifier.fillMaxWidth()) {
            SectionLabel("История чтения")
            if (state.logs.isEmpty()) {
                EmptyState("🕯️", "Пока без записей", "Запиши первое чтение — даже пару страниц")
            } else {
                GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                    state.logs.take(30).forEach { log ->
                        Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .drawBehind { drawCircle(AscendColors.Cyan) },
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                relativeDay(log.day, state.today),
                                style = MaterialTheme.typography.bodyMedium,
                                color = AscendColors.TextPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "+${log.amount} стр.",
                                style = MaterialTheme.typography.labelLarge,
                                color = AscendColors.TextSecondary,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("+${log.xp} XP", style = MaterialTheme.typography.labelLarge, color = AscendColors.Cyan)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingCover(book: BookEntity, glow: Color) {
    val transition = rememberInfiniteTransition(label = "cover")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6_000, easing = LinearEasing)),
        label = "phase",
    )
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath",
    )
    Box(
        Modifier
            .padding(top = 8.dp)
            .drawBehind {
                val c = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    brush = Brush.radialGradient(listOf(glow.copy(alpha = 0.45f + 0.15f * breath), Color.Transparent), center = c, radius = size.width),
                    radius = size.width,
                    center = c,
                )
                drawOval(
                    brush = Brush.radialGradient(
                        listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent),
                        center = Offset(c.x, size.height + 18.dp.toPx()),
                        radius = size.width * 0.5f,
                    ),
                    topLeft = Offset(c.x - size.width * 0.5f, size.height + 6.dp.toPx()),
                    size = Size(size.width, 24.dp.toPx()),
                )
            },
    ) {
        BookCover(
            title = book.title,
            author = book.author,
            palette = book.palette,
            modifier = Modifier
                .width(176.dp)
                .graphicsLayer {
                    val a = phase * 2f * PI.toFloat()
                    rotationY = sin(a) * 9f
                    rotationX = cos(a) * 3f
                    translationY = -breath * 8.dp.toPx()
                    cameraDistance = 14f * density
                },
        )
    }
}

private fun dayOf(millis: Long): Long =
    java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toEpochDay()
