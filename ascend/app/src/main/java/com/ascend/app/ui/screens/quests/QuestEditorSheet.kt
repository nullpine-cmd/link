package com.ascend.app.ui.screens.quests

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascend.app.data.local.BookEntity
import com.ascend.app.di.appViewModel
import com.ascend.app.domain.QuestDraft
import com.ascend.app.ui.components.AscendSheet
import com.ascend.app.ui.components.AscendTextField
import com.ascend.app.ui.components.CircleIconButton
import com.ascend.app.ui.components.DifficultyStars
import com.ascend.app.ui.components.EmojiTile
import com.ascend.app.ui.components.GhostButton
import com.ascend.app.ui.components.GradientButton
import com.ascend.app.ui.components.SectionLabel
import com.ascend.app.ui.components.SegmentedControl
import com.ascend.app.ui.components.SelectChip
import com.ascend.app.ui.components.bounceClick
import com.ascend.app.ui.theme.AscendColors
import com.ascend.app.ui.theme.color
import com.ascend.app.ui.theme.icon
import com.ascend.app.ui.util.dateShort
import com.ascend.app.ui.util.formatTime
import com.ascend.core.Attribute
import com.ascend.core.Difficulty
import com.ascend.core.QuestKind
import com.ascend.core.QuestTemplate
import com.ascend.core.QuestTemplates
import com.ascend.core.Units
import com.ascend.core.WeekSchedule
import java.time.DayOfWeek

private class EditorState(initial: QuestDraft) {
    val id = initial.id
    var title by mutableStateOf(initial.title)
    var emoji by mutableStateOf(initial.emoji)
    var kind by mutableStateOf(initial.kind)
    var attribute by mutableStateOf(initial.attribute)
    var difficulty by mutableStateOf(initial.difficulty)
    var hasTarget by mutableStateOf(initial.target != null)
    var target by mutableStateOf(initial.target?.toString() ?: "")
    var unit by mutableStateOf(initial.unit ?: Units.TIMES)
    var schedule by mutableStateOf(WeekSchedule(initial.scheduleMask))
    var dueDay by mutableStateOf(initial.dueDay)
    var time by mutableStateOf(initial.timeMinutes)
    var bookId by mutableStateOf(initial.bookId)

    val targetValue: Int? get() = if (hasTarget) target.toIntOrNull()?.takeIf { it > 0 } else null
    val valid: Boolean
        get() = title.isNotBlank() &&
            (kind == QuestKind.TASK || !schedule.isEmpty) &&
            (!hasTarget || targetValue != null)

    fun apply(template: QuestTemplate) {
        title = template.title
        emoji = template.emoji
        attribute = template.attribute
        difficulty = template.difficulty
        hasTarget = template.target != null
        target = template.target?.toString() ?: ""
        unit = template.unit ?: Units.TIMES
        schedule = template.schedule
        kind = QuestKind.HABIT
    }

    fun linkBook(book: BookEntity?) {
        bookId = book?.id
        if (book != null) {
            unit = Units.PAGES
            if (!hasTarget || target.isBlank()) {
                hasTarget = true
                target = "20"
            }
            attribute = Attribute.INTELLECT
        }
    }

    fun toDraft(today: Long) = QuestDraft(
        id = id,
        title = title,
        emoji = emoji,
        kind = kind,
        attribute = attribute,
        difficulty = difficulty,
        target = targetValue,
        unit = if (hasTarget) unit.ifBlank { Units.TIMES } else null,
        scheduleMask = schedule.mask,
        dueDay = if (kind == QuestKind.TASK) dueDay ?: today else null,
        timeMinutes = time,
        bookId = bookId,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuestEditorSheet(request: QuestEditorRequest, onDismiss: () -> Unit) {
    val viewModel = appViewModel(key = "questEditor") { QuestEditorViewModel(it.repository, it.time) }
    val books by viewModel.books.collectAsStateWithLifecycle()
    val state = remember(request) { EditorState(viewModel.initialDraft(request)) }
    val editing = request is QuestEditorRequest.Edit
    val archived = (request as? QuestEditorRequest.Edit)?.quest?.archived == true
    val today = viewModel.today

    var showEmojis by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val accent by animateColorAsState(state.attribute.color, label = "editorAccent")

    AscendSheet(onDismiss = onDismiss) { hide ->
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (editing) "Изменить квест" else "Новый квест",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AscendColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                CircleIconButton(Icons.Rounded.Close, "Закрыть", hide, diameter = 40.dp)
            }
            Spacer(Modifier.height(16.dp))

            if (!editing) {
                SectionLabel("Быстрый старт")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(QuestTemplates.all) { template ->
                        SelectChip(
                            text = template.title,
                            leading = template.emoji,
                            selected = state.title == template.title,
                            color = template.attribute.color,
                            onClick = { state.apply(template) },
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
            }

            SectionLabel("Название")
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiTile(
                    state.emoji,
                    accent,
                    side = 54.dp,
                    modifier = Modifier.bounceClick { showEmojis = !showEmojis },
                )
                Spacer(Modifier.width(12.dp))
                AscendTextField(
                    value = state.title,
                    onValueChange = { state.title = it },
                    placeholder = "Например: читать 20 страниц",
                    accent = accent,
                    modifier = Modifier.weight(1f),
                )
            }
            AnimatedVisibility(
                visible = showEmojis,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                FlowRow(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    QuestTemplates.emojis.forEach { emoji ->
                        val selected = emoji == state.emoji
                        Box(
                            Modifier
                                .size(44.dp)
                                .bounceClick {
                                    state.emoji = emoji
                                    showEmojis = false
                                }
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) accent.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(emoji, fontSize = 22.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            SectionLabel("Тип")
            SegmentedControl(
                options = QuestKind.entries,
                selected = state.kind,
                label = { if (it == QuestKind.HABIT) "🔁  Привычка" else "✅  Разовая задача" },
                onSelect = { state.kind = it },
                color = accent,
            )
            Spacer(Modifier.height(20.dp))

            SectionLabel("Что прокачивает")
            Attribute.entries.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    row.forEach { attribute ->
                        AttributeChoice(
                            attribute = attribute,
                            selected = attribute == state.attribute,
                            onClick = { state.attribute = attribute },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            Text(
                "Все пути равноценны: награда зависит только от сложности, а не от выбора характеристики",
                style = MaterialTheme.typography.bodySmall,
                color = AscendColors.TextMuted,
            )
            Spacer(Modifier.height(20.dp))

            SectionLabel("Сложность")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Difficulty.entries.forEach { difficulty ->
                    DifficultyChoice(
                        difficulty = difficulty,
                        selected = difficulty == state.difficulty,
                        accent = accent,
                        onClick = { state.difficulty = difficulty },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Цель с количеством", style = MaterialTheme.typography.titleMedium, color = AscendColors.TextPrimary)
                    Text(
                        "Любой результат засчитается — цель лишь даёт бонус",
                        style = MaterialTheme.typography.bodySmall,
                        color = AscendColors.TextMuted,
                    )
                }
                Switch(
                    checked = state.hasTarget,
                    onCheckedChange = {
                        state.hasTarget = it
                        if (it && state.target.isBlank()) state.target = "10"
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = accent,
                        uncheckedThumbColor = AscendColors.TextMuted,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.06f),
                        uncheckedBorderColor = Color.White.copy(alpha = 0.12f),
                    ),
                )
            }
            AnimatedVisibility(
                visible = state.hasTarget,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(Modifier.padding(top = 12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AscendTextField(
                            value = state.target,
                            onValueChange = { value -> state.target = value.filter { it.isDigit() }.take(6) },
                            placeholder = "Сколько",
                            accent = accent,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        AscendTextField(
                            value = state.unit,
                            onValueChange = { state.unit = it.take(12) },
                            placeholder = "Единица",
                            accent = accent,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(Units.presets) { unit ->
                            SelectChip(unit, selected = state.unit == unit, color = accent, onClick = { state.unit = unit })
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            if (state.kind == QuestKind.HABIT) {
                SectionLabel("Повторять")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "Каждый день" to WeekSchedule.EVERY_DAY,
                        "Будни" to WeekSchedule.WEEKDAYS,
                        "Выходные" to WeekSchedule.WEEKENDS,
                    ).forEach { (label, preset) ->
                        SelectChip(label, selected = state.schedule == preset, color = accent, onClick = { state.schedule = preset })
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DayOfWeek.entries.forEach { day ->
                        DayToggle(
                            label = WeekSchedule.shortName(day),
                            selected = day in state.schedule,
                            accent = accent,
                            onClick = { state.schedule = state.schedule.toggle(day) },
                        )
                    }
                }
                if (state.schedule.isEmpty) {
                    Text(
                        "Выбери хотя бы один день",
                        style = MaterialTheme.typography.bodySmall,
                        color = AscendColors.Danger,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            } else {
                SectionLabel("Когда")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectChip("Сегодня", selected = state.dueDay == today, color = accent, onClick = { state.dueDay = today })
                    SelectChip("Завтра", selected = state.dueDay == today + 1, color = accent, onClick = { state.dueDay = today + 1 })
                    val custom = state.dueDay?.takeIf { it != today && it != today + 1 }
                    SelectChip(
                        text = custom?.let { dateShort(it) } ?: "Дата…",
                        icon = Icons.Rounded.CalendarMonth,
                        selected = custom != null,
                        color = accent,
                        onClick = { pickDate = true },
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            SectionLabel("Время (необязательно)")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SelectChip(
                    text = state.time?.let { formatTime(it) } ?: "В любое время",
                    icon = Icons.Rounded.Schedule,
                    selected = state.time != null,
                    color = accent,
                    onClick = { pickTime = true },
                )
                if (state.time != null) {
                    SelectChip("Убрать", selected = false, onClick = { state.time = null })
                }
            }

            val linkable = books.isNotEmpty() && (state.attribute == Attribute.INTELLECT || state.bookId != null)
            if (linkable) {
                Spacer(Modifier.height(20.dp))
                SectionLabel("Книга (прогресс пойдёт в библиотеку)")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        SelectChip("Без книги", selected = state.bookId == null, color = accent, onClick = { state.linkBook(null) })
                    }
                    items(books, key = { it.id }) { book ->
                        SelectChip(
                            text = book.title.take(22),
                            leading = "📖",
                            selected = state.bookId == book.id,
                            color = accent,
                            onClick = { state.linkBook(book) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            GradientButton(
                text = if (editing) "Сохранить" else "Создать квест",
                icon = Icons.Rounded.Check,
                enabled = state.valid,
                colors = listOf(accent, AscendColors.Violet, AscendColors.Cyan),
                onClick = {
                    viewModel.save(state.toDraft(today))
                    hide()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            if (editing) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (state.kind == QuestKind.HABIT || archived) {
                        GhostButton(
                            text = if (archived) "Вернуть" else "В архив",
                            icon = if (archived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                            onClick = {
                                viewModel.setArchived(state.id, !archived)
                                hide()
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    GhostButton(
                        text = "Удалить",
                        icon = Icons.Rounded.DeleteOutline,
                        color = AscendColors.Danger,
                        onClick = { confirmDelete = true },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (pickDate) {
            val pickerState = rememberDatePickerState(
                initialSelectedDateMillis = (state.dueDay ?: today) * MILLIS_PER_DAY,
            )
            DatePickerDialog(
                onDismissRequest = { pickDate = false },
                confirmButton = {
                    TextButton(onClick = {
                        pickerState.selectedDateMillis?.let { state.dueDay = it / MILLIS_PER_DAY }
                        pickDate = false
                    }) { Text("Готово") }
                },
                dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Отмена") } },
            ) {
                DatePicker(state = pickerState)
            }
        }

        if (pickTime) {
            val initial = state.time ?: (9 * 60)
            val timeState = rememberTimePickerState(initialHour = initial / 60, initialMinute = initial % 60, is24Hour = true)
            BasicAlertDialog(onDismissRequest = { pickTime = false }) {
                Column(
                    Modifier
                        .clip(RoundedCornerShape(28.dp))
                        .background(AscendColors.SurfaceHigh)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Во сколько?", style = MaterialTheme.typography.titleLarge, color = AscendColors.TextPrimary)
                    Spacer(Modifier.height(16.dp))
                    TimePicker(state = timeState)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { pickTime = false }) { Text("Отмена") }
                        TextButton(onClick = {
                            state.time = timeState.hour * 60 + timeState.minute
                            pickTime = false
                        }) { Text("Готово") }
                    }
                }
            }
        }

        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                containerColor = AscendColors.SurfaceHigh,
                title = { Text("Удалить квест?") },
                text = { Text("Заработанный опыт останется с тобой — история не стирается.") },
                confirmButton = {
                    TextButton(onClick = {
                        confirmDelete = false
                        viewModel.delete(state.id)
                        hide()
                    }) { Text("Удалить", color = AscendColors.Danger) }
                },
                dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } },
            )
        }
    }
}

private const val MILLIS_PER_DAY = 86_400_000L

@Composable
private fun AttributeChoice(
    attribute: Attribute,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = attribute.color
    val background by animateColorAsState(
        if (selected) color.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.04f),
        label = "attrBg",
    )
    val border by animateColorAsState(
        if (selected) color else Color.White.copy(alpha = 0.08f),
        label = "attrBorder",
    )
    Column(
        modifier = modifier
            .bounceClick(onClick = onClick)
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(18.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(color.copy(alpha = if (selected) 0.6f else 0.25f), Color.Transparent))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(attribute.icon, contentDescription = null, tint = if (selected) Color.White else color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(attribute.title, style = MaterialTheme.typography.labelLarge, color = AscendColors.TextPrimary, maxLines = 1)
        Text(
            attribute.description,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
            color = AscendColors.TextMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DifficultyChoice(
    difficulty: Difficulty,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background by animateColorAsState(
        if (selected) accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.04f),
        label = "diffBg",
    )
    val border by animateColorAsState(if (selected) accent else Color.White.copy(alpha = 0.08f), label = "diffBorder")
    Column(
        modifier = modifier
            .bounceClick(onClick = onClick)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(16.dp))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DifficultyStars(difficulty.stars, AscendColors.Gold, starSize = 9.dp)
        Spacer(Modifier.height(6.dp))
        Text(difficulty.title, style = MaterialTheme.typography.labelMedium, color = AscendColors.TextPrimary, maxLines = 1)
        Text("${difficulty.baseXp} XP", style = MaterialTheme.typography.labelSmall, color = AscendColors.Cyan)
    }
}

@Composable
private fun DayToggle(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    val background by animateColorAsState(if (selected) accent else Color.White.copy(alpha = 0.05f), label = "dayBg")
    val content by animateColorAsState(if (selected) Color.White else AscendColors.TextSecondary, label = "dayText")
    Box(
        Modifier
            .size(40.dp)
            .bounceClick(pressedScale = 0.85f, onClick = onClick)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = content)
    }
}
