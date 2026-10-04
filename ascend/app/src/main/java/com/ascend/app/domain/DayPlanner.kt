package com.ascend.app.domain

import com.ascend.app.data.local.LogEntity
import com.ascend.app.data.local.LogKind
import com.ascend.app.data.local.QuestEntity
import com.ascend.app.data.local.isHabit
import com.ascend.app.data.local.schedule
import com.ascend.core.Streaks

/**
 * Собирает план дня. Прошлые дни показывают только сделанное — пропуски не выставляются
 * напоказ, приложение поощряет, а не упрекает. Невыполненные задачи прошлых дней
 * мягко переезжают на сегодня.
 */
object DayPlanner {

    fun build(quests: List<QuestEntity>, logsOfDay: List<LogEntity>, day: Long, today: Long): DayPlan {
        val completions = logsOfDay
            .filter { it.kind == LogKind.QUEST && it.questId != null }
            .associateBy { it.questId!! }

        val items = quests.mapNotNull { quest ->
            val log = completions[quest.id]
            val streak = if (quest.isHabit) {
                Streaks.visibleHabitStreak(quest.schedule, quest.streak, quest.lastCompletedDay, today)
            } else {
                0
            }
            val activeStatus = if (day == today) PlanStatus.PENDING else PlanStatus.PLANNED
            when {
                log != null -> PlanItem(quest, log, PlanStatus.DONE, overdue = false, visibleStreak = streak)
                day < today || quest.archived -> null
                quest.isHabit ->
                    if (quest.createdDay <= day && quest.schedule.isDue(day)) {
                        PlanItem(quest, null, activeStatus, overdue = false, visibleStreak = streak)
                    } else {
                        null
                    }
                quest.completedAt != null -> null
                quest.dueDay == day -> PlanItem(quest, null, activeStatus, overdue = false, visibleStreak = 0)
                day == today && quest.dueDay != null && quest.dueDay < today ->
                    PlanItem(quest, null, PlanStatus.PENDING, overdue = true, visibleStreak = 0)
                else -> null
            }
        }.sortedWith(
            compareBy<PlanItem> { it.status == PlanStatus.DONE }
                .thenBy { it.quest.timeMinutes ?: Int.MAX_VALUE }
                .thenBy { it.quest.id },
        )
        return DayPlan(day, today, items)
    }
}
