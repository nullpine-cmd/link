package com.ascend.app.domain

import com.ascend.app.data.local.LogEntity
import com.ascend.app.data.local.LogKind
import com.ascend.app.data.local.QuestEntity
import com.ascend.core.Attribute
import com.ascend.core.Difficulty
import com.ascend.core.QuestKind
import com.ascend.core.WeekSchedule
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DayPlannerTest {

    private val monday = LocalDate.of(2026, 10, 5).toEpochDay()

    private fun quest(
        id: Long,
        kind: QuestKind = QuestKind.HABIT,
        schedule: WeekSchedule = WeekSchedule.EVERY_DAY,
        dueDay: Long? = null,
        time: Int? = null,
        createdDay: Long = monday - 30,
        archived: Boolean = false,
    ) = QuestEntity(
        id = id,
        title = "Квест $id",
        emoji = "🎯",
        kind = kind,
        attribute = Attribute.SPIRIT,
        difficulty = Difficulty.EASY,
        scheduleMask = schedule.mask,
        dueDay = dueDay,
        timeMinutes = time,
        createdAt = 0,
        createdDay = createdDay,
        archived = archived,
    )

    private fun done(questId: Long, day: Long) = LogEntity(
        id = questId * 100,
        kind = LogKind.QUEST,
        questId = questId,
        title = "",
        emoji = "",
        day = day,
        timestamp = day * 1000,
        hour = 12,
        xp = 10,
    )

    @Test
    fun `habits follow their weekdays`() {
        val weekdays = quest(1, schedule = WeekSchedule.WEEKDAYS)
        val weekends = quest(2, schedule = WeekSchedule.WEEKENDS)
        val plan = DayPlanner.build(listOf(weekdays, weekends), emptyList(), monday, monday)
        assertEquals(listOf(1L), plan.items.map { it.quest.id })
        assertEquals(PlanStatus.PENDING, plan.items.single().status)
    }

    @Test
    fun `future days are planned and pending items come before done ones`() {
        val quests = listOf(quest(1, time = 9 * 60), quest(2, time = 7 * 60), quest(3))
        val plan = DayPlanner.build(quests, listOf(done(2, monday)), monday, monday)
        assertEquals(listOf(1L, 3L, 2L), plan.items.map { it.quest.id })
        assertEquals(1, plan.done)

        val tomorrow = DayPlanner.build(quests, emptyList(), monday + 1, monday)
        assertTrue(tomorrow.items.all { it.status == PlanStatus.PLANNED })
    }

    @Test
    fun `past days show only what was done`() {
        val quests = listOf(quest(1), quest(2))
        val plan = DayPlanner.build(quests, listOf(done(1, monday - 1)), monday - 1, monday)
        assertEquals(listOf(1L), plan.items.map { it.quest.id })
        assertEquals(PlanStatus.DONE, plan.items.single().status)
    }

    @Test
    fun `habits created later and archived ones are skipped`() {
        val fresh = quest(1, createdDay = monday + 2)
        val archived = quest(2, archived = true)
        assertTrue(DayPlanner.build(listOf(fresh, archived), emptyList(), monday, monday).items.isEmpty())
    }

    @Test
    fun `overdue tasks roll over only to today`() {
        val task = quest(1, kind = QuestKind.TASK, dueDay = monday - 2)
        val today = DayPlanner.build(listOf(task), emptyList(), monday, monday)
        assertTrue(today.items.single().overdue)
        assertTrue(DayPlanner.build(listOf(task), emptyList(), monday + 1, monday).items.isEmpty())
    }
}
