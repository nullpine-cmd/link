package com.ascend.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class ScheduleAndStreaksTest {

    private val monday = LocalDate.of(2026, 10, 5).toEpochDay()

    @Test
    fun `labels describe common schedules`() {
        assertEquals("Каждый день", WeekSchedule.EVERY_DAY.label())
        assertEquals("По будням", WeekSchedule.WEEKDAYS.label())
        assertEquals("По выходным", WeekSchedule.WEEKENDS.label())
        assertEquals("Пн, Ср, Пт", WeekSchedule(0b001_0101).label())
    }

    @Test
    fun `due days follow the mask`() {
        val weekdays = WeekSchedule.WEEKDAYS
        assertTrue(weekdays.isDue(monday))
        assertFalse(weekdays.isDue(monday + 5)) // суббота
        assertEquals(monday - 3, weekdays.previousDueDay(monday)) // пятница
        assertEquals(null, WeekSchedule(0).previousDueDay(monday))
    }

    @Test
    fun `toggle flips a single day`() {
        val schedule = WeekSchedule(0).toggle(DayOfWeek.WEDNESDAY)
        assertTrue(DayOfWeek.WEDNESDAY in schedule)
        assertEquals(1, schedule.daysPerWeek)
        assertTrue(schedule.toggle(DayOfWeek.WEDNESDAY).isEmpty)
    }

    @Test
    fun `daily streak stays alive until the day is over`() {
        val today = monday + 10
        val days = listOf(today - 3, today - 2, today - 1)
        assertEquals(StreakInfo(3, 3), Streaks.daily(days, today))
        assertEquals(StreakInfo(4, 4), Streaks.daily(days + today, today))
        assertEquals(StreakInfo(0, 3), Streaks.daily(days, today + 1))
    }

    @Test
    fun `best daily streak survives a break`() {
        val days = listOf(1L, 2L, 3L, 4L, 5L, 10L, 11L)
        assertEquals(StreakInfo(2, 5), Streaks.daily(days, 11))
    }

    @Test
    fun `habit streak only counts scheduled days`() {
        val mwf = WeekSchedule(0b001_0101)
        // Пн, Ср, Пт, Пн — четыре подряд по расписанию, хотя прошла неделя.
        val done = listOf(monday, monday + 2, monday + 4, monday + 7)
        assertEquals(StreakInfo(4, 4), Streaks.habit(mwf, done, monday + 8))
        // В среду ещё не выполнено, но серия живая до конца дня.
        assertEquals(4, Streaks.visibleHabitStreak(mwf, 4, monday + 7, monday + 9))
        // Пропущена среда — к пятнице серия обнулилась (без штрафов).
        assertEquals(0, Streaks.visibleHabitStreak(mwf, 4, monday + 7, monday + 11))
    }

    @Test
    fun `habit streak restarts after a missed scheduled day`() {
        val daily = WeekSchedule.EVERY_DAY
        val done = listOf(monday, monday + 1, monday + 3, monday + 4)
        assertEquals(StreakInfo(2, 2), Streaks.habit(daily, done, monday + 4))
    }
}
