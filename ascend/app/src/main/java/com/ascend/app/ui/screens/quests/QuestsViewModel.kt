package com.ascend.app.ui.screens.quests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.app.data.GameRepository
import com.ascend.app.data.TimeProvider
import com.ascend.app.data.local.BookEntity
import com.ascend.app.data.local.QuestEntity
import com.ascend.app.data.local.isHabit
import com.ascend.app.data.local.schedule
import com.ascend.app.di.CelebrationBus
import com.ascend.app.domain.Celebration
import com.ascend.core.Streaks
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HabitRow(
    val quest: QuestEntity,
    val dueToday: Boolean,
    val doneToday: Boolean,
    val streak: Int,
)

data class TaskGroup(val title: String, val tasks: List<QuestEntity>, val done: Boolean = false)

data class QuestsUiState(
    val loading: Boolean = true,
    val habits: List<HabitRow> = emptyList(),
    val taskGroups: List<TaskGroup> = emptyList(),
    val archived: List<QuestEntity> = emptyList(),
    val activeTasks: Int = 0,
    val books: Map<Long, BookEntity> = emptyMap(),
    val today: Long,
)

class QuestsViewModel(
    private val repository: GameRepository,
    private val celebrations: CelebrationBus,
    time: TimeProvider,
) : ViewModel() {

    val state: StateFlow<QuestsUiState> = combine(
        repository.quests,
        repository.today,
        repository.books,
    ) { quests, today, books -> build(quests, today, books.associateBy { it.id }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuestsUiState(today = time.today()))

    fun complete(questId: Long, amount: Int) {
        viewModelScope.launch {
            repository.completeQuest(questId, amount)?.let { celebrations.emit(Celebration.Completed(it)) }
        }
    }

    fun setArchived(questId: Long, archived: Boolean) {
        viewModelScope.launch { repository.setArchived(questId, archived) }
    }

    fun delete(questId: Long) {
        viewModelScope.launch { repository.deleteQuest(questId) }
    }

    fun reschedule(questId: Long, day: Long) {
        viewModelScope.launch { repository.rescheduleTask(questId, day) }
    }

    private fun build(quests: List<QuestEntity>, today: Long, books: Map<Long, BookEntity>): QuestsUiState {
        val active = quests.filterNot { it.archived }
        val habits = active.filter { it.isHabit }
            .map { quest ->
                HabitRow(
                    quest = quest,
                    dueToday = quest.schedule.isDue(today),
                    doneToday = quest.lastCompletedDay == today,
                    streak = Streaks.visibleHabitStreak(quest.schedule, quest.streak, quest.lastCompletedDay, today),
                )
            }
            .sortedWith(compareBy<HabitRow>({ !(it.dueToday && !it.doneToday) }, { !it.dueToday }, { it.quest.timeMinutes ?: Int.MAX_VALUE }))

        val tasks = active.filterNot { it.isHabit }
        val pending = tasks.filter { it.completedAt == null }
        val groups = buildList {
            fun group(title: String, items: List<QuestEntity>, done: Boolean = false) {
                if (items.isNotEmpty()) add(TaskGroup(title, items, done))
            }
            val byTime = compareBy<QuestEntity>({ it.dueDay }, { it.timeMinutes ?: Int.MAX_VALUE }, { it.id })
            group("С прошлых дней", pending.filter { (it.dueDay ?: today) < today }.sortedWith(byTime))
            group("Сегодня", pending.filter { it.dueDay == today }.sortedWith(byTime))
            group("Завтра", pending.filter { it.dueDay == today + 1 }.sortedWith(byTime))
            group("Позже", pending.filter { (it.dueDay ?: today) > today + 1 }.sortedWith(byTime))
            group(
                "Выполнено",
                tasks.filter { it.completedAt != null && (it.lastCompletedDay ?: 0) >= today - 7 }
                    .sortedByDescending { it.completedAt },
                done = true,
            )
        }
        val archived = (quests.filter { it.archived } + tasks.filter { it.completedAt != null && (it.lastCompletedDay ?: 0) < today - 7 })
            .sortedByDescending { it.createdAt }

        return QuestsUiState(
            loading = false,
            habits = habits,
            taskGroups = groups,
            archived = archived,
            activeTasks = pending.size,
            books = books,
            today = today,
        )
    }
}
