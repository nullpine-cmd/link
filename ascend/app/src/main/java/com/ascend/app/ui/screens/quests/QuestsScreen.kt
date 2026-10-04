package com.ascend.app.ui.screens.quests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascend.app.data.local.QuestEntity
import com.ascend.app.data.local.hasTarget
import com.ascend.app.data.local.isHabit
import com.ascend.app.data.local.schedule
import com.ascend.app.di.appViewModel
import com.ascend.app.ui.components.BottomBarSpace
import com.ascend.app.ui.components.EmptyState
import com.ascend.app.ui.components.GhostButton
import com.ascend.app.ui.components.SectionLabel
import com.ascend.app.ui.components.SegmentedControl
import com.ascend.app.ui.components.reveal
import com.ascend.app.ui.theme.AscendColors
import com.ascend.app.ui.util.formatTime
import com.ascend.app.ui.util.plural
import com.ascend.app.ui.util.relativeDay
import com.ascend.core.QuestKind
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

private enum class QuestTab(val title: String) { HABITS("Привычки"), TASKS("Задачи"), ARCHIVE("Архив") }

@Composable
fun QuestsRoute(onOpenEditor: (QuestEditorRequest) -> Unit) {
    val viewModel = appViewModel { QuestsViewModel(it.repository, it.celebrations, it.time) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    QuestsScreen(
        state = state,
        onComplete = viewModel::complete,
        onArchive = viewModel::setArchived,
        onDelete = viewModel::delete,
        onReschedule = viewModel::reschedule,
        onOpenEditor = onOpenEditor,
    )
}

@Composable
fun QuestsScreen(
    state: QuestsUiState,
    onComplete: (Long, Int) -> Unit,
    onArchive: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
    onReschedule: (Long, Long) -> Unit,
    onOpenEditor: (QuestEditorRequest) -> Unit,
) {
    val tabs = QuestTab.entries
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()
    var progressFor by remember { mutableStateOf<QuestEntity?>(null) }
    val check: (QuestEntity) -> Unit = { quest ->
        if (quest.hasTarget) progressFor = quest else onComplete(quest.id, 1)
    }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp),
        ) {
            Text("Квесты", style = MaterialTheme.typography.headlineLarge, color = AscendColors.TextPrimary)
            Text(
                "${state.habits.size} ${plural(state.habits.size, "привычка", "привычки", "привычек")} · " +
                    "${state.activeTasks} ${plural(state.activeTasks, "задача", "задачи", "задач")} в работе",
                style = MaterialTheme.typography.bodyMedium,
                color = AscendColors.TextSecondary,
            )
            Spacer(Modifier.height(16.dp))
            SegmentedControl(
                options = tabs,
                selected = tabs[pagerState.targetPage],
                label = { it.title },
                onSelect = { tab -> scope.launch { pagerState.animateScrollToPage(tab.ordinal) } },
            )
            Spacer(Modifier.height(8.dp))
        }
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            val offset = (pagerState.currentPage - page + pagerState.currentPageOffsetFraction).absoluteValue
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val fraction = offset.coerceIn(0f, 1f)
                        alpha = lerp(1f, 0.4f, fraction)
                        val scale = lerp(1f, 0.92f, fraction)
                        scaleX = scale
                        scaleY = scale
                    },
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = BottomBarSpace + 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (tabs[page]) {
                    QuestTab.HABITS -> habits(state, check, onArchive, onOpenEditor)
                    QuestTab.TASKS -> tasks(state, check, onReschedule, onOpenEditor)
                    QuestTab.ARCHIVE -> archive(state, onArchive, onDelete, onOpenEditor)
                }
            }
        }
    }

    progressFor?.let { quest ->
        QuestProgressSheet(
            quest = quest,
            book = quest.bookId?.let { state.books[it] },
            today = state.today,
            onDismiss = { progressFor = null },
            onConfirm = { amount -> onComplete(quest.id, amount) },
        )
    }
}

private fun LazyListScope.habits(
    state: QuestsUiState,
    onCheck: (QuestEntity) -> Unit,
    onArchive: (Long, Boolean) -> Unit,
    onOpenEditor: (QuestEditorRequest) -> Unit,
) {
    if (state.habits.isEmpty() && !state.loading) {
        item(key = "empty") {
            EmptyState(
                "🔁",
                "Пока нет привычек",
                "Привычка — это квест, который повторяется. Пропуски не наказываются, а серии приносят бонусы",
            )
        }
    }
    items(state.habits, key = { it.quest.id }) { row ->
        val quest = row.quest
        QuestCard(
            quest = quest,
            done = row.doneToday,
            canComplete = row.dueToday && !row.doneToday,
            streak = row.streak,
            subtitle = buildString {
                append(quest.schedule.label())
                if (!row.dueToday) append(" · сегодня отдых")
                if (quest.totalCompletions > 0) append(" · выполнено ${quest.totalCompletions}")
            },
            onCheck = { onCheck(quest) },
            onClick = { onOpenEditor(QuestEditorRequest.Edit(quest)) },
            menu = listOf(
                QuestMenuAction("Изменить", Icons.Rounded.Edit) { onOpenEditor(QuestEditorRequest.Edit(quest)) },
                QuestMenuAction("В архив", Icons.Rounded.Archive) { onArchive(quest.id, true) },
            ),
            modifier = Modifier.animateItem(),
        )
    }
    item(key = "add-habit") {
        GhostButton(
            "Новая привычка",
            icon = Icons.Rounded.Add,
            onClick = { onOpenEditor(QuestEditorRequest.New(kind = QuestKind.HABIT)) },
            modifier = Modifier.fillMaxWidth().animateItem(),
        )
    }
}

private fun LazyListScope.tasks(
    state: QuestsUiState,
    onCheck: (QuestEntity) -> Unit,
    onReschedule: (Long, Long) -> Unit,
    onOpenEditor: (QuestEditorRequest) -> Unit,
) {
    if (state.taskGroups.isEmpty() && !state.loading) {
        item(key = "empty") {
            EmptyState(
                "✅",
                "Нет задач",
                "Разовые дела на сегодня, завтра или любой день. Невыполненные мягко переезжают на сегодня",
            )
        }
    }
    state.taskGroups.forEach { group ->
        item(key = "header-${group.title}") {
            SectionLabel(group.title, Modifier.padding(top = 8.dp).animateItem(), color = if (group.done) AscendColors.Success else AscendColors.TextMuted)
        }
        items(group.tasks, key = { it.id }) { quest ->
            val day = quest.dueDay ?: state.today
            QuestCard(
                quest = quest,
                done = group.done,
                canComplete = !group.done,
                subtitle = buildString {
                    append(relativeDay(day, state.today))
                    quest.timeMinutes?.let { append(" · ${formatTime(it)}") }
                },
                onCheck = { onCheck(quest) },
                onClick = { onOpenEditor(QuestEditorRequest.Edit(quest)) },
                menu = if (group.done) {
                    emptyList()
                } else {
                    listOf(
                        QuestMenuAction("На сегодня", Icons.Rounded.EventRepeat) { onReschedule(quest.id, state.today) },
                        QuestMenuAction("На завтра", Icons.Rounded.EventRepeat) { onReschedule(quest.id, state.today + 1) },
                    )
                },
                modifier = Modifier.animateItem(),
            )
        }
    }
    item(key = "add-task") {
        GhostButton(
            "Новая задача",
            icon = Icons.Rounded.Add,
            onClick = { onOpenEditor(QuestEditorRequest.New(dueDay = state.today, kind = QuestKind.TASK)) },
            modifier = Modifier.fillMaxWidth().animateItem(),
        )
    }
}

private fun LazyListScope.archive(
    state: QuestsUiState,
    onArchive: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
    onOpenEditor: (QuestEditorRequest) -> Unit,
) {
    if (state.archived.isEmpty() && !state.loading) {
        item(key = "empty") {
            EmptyState("🗄️", "Архив пуст", "Сюда попадают привычки на паузе и давно выполненные задачи. Весь опыт остаётся с тобой")
        }
    }
    items(state.archived, key = { it.id }) { quest ->
        QuestCard(
            quest = quest,
            done = !quest.isHabit,
            canComplete = false,
            subtitle = if (quest.isHabit) "На паузе · выполнено ${quest.totalCompletions}" else "Выполнено",
            onCheck = {},
            onClick = { onOpenEditor(QuestEditorRequest.Edit(quest)) },
            menu = buildList {
                if (quest.archived) add(QuestMenuAction("Вернуть", Icons.Rounded.Unarchive) { onArchive(quest.id, false) })
                add(QuestMenuAction("Удалить", Icons.Rounded.DeleteOutline) { onDelete(quest.id) })
            },
            modifier = Modifier.reveal(0).animateItem(),
        )
    }
}
