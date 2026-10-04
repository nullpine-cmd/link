package com.ascend.core

data class StreakInfo(val current: Int, val best: Int)

object Streaks {

    /**
     * Серия активных дней. Пропуск не наказывается — серия просто начинается заново,
     * а вчерашняя серия считается живой, пока сегодняшний день не закончился.
     */
    fun daily(activeDays: Collection<Long>, today: Long): StreakInfo {
        if (activeDays.isEmpty()) return StreakInfo(0, 0)
        val days = activeDays.toSortedSet()
        var best = 0
        var run = 0
        var previous: Long? = null
        for (day in days) {
            run = if (previous != null && day == previous + 1) run + 1 else 1
            best = maxOf(best, run)
            previous = day
        }
        val anchor = when {
            today in days -> today
            today - 1 in days -> today - 1
            else -> return StreakInfo(0, best)
        }
        var current = 0
        var day = anchor
        while (day in days) {
            current++
            day--
        }
        return StreakInfo(current, best)
    }

    /** Серия привычки по запланированным дням: учитываются только дни по расписанию. */
    fun habit(schedule: WeekSchedule, completedDays: Collection<Long>, today: Long): StreakInfo {
        if (completedDays.isEmpty() || schedule.isEmpty) return StreakInfo(0, 0)
        val done = completedDays.toSortedSet()
        var best = 0
        var run = 0
        var expectedPrevious: Long? = null
        for (day in done) {
            run = if (expectedPrevious != null && schedule.previousDueDay(day) == expectedPrevious) run + 1 else 1
            best = maxOf(best, run)
            expectedPrevious = day
        }
        val anchor = when {
            today in done -> today
            else -> schedule.previousDueDay(today)?.takeIf { it in done } ?: return StreakInfo(0, best)
        }
        var current = 0
        var day: Long? = anchor
        while (day != null && day in done) {
            current++
            day = schedule.previousDueDay(day)
        }
        return StreakInfo(current, best)
    }

    /** Отображаемая серия привычки: живая, если выполнена сегодня или в прошлый запланированный день. */
    fun visibleHabitStreak(schedule: WeekSchedule, streak: Int, lastCompletedDay: Long?, today: Long): Int = when {
        lastCompletedDay == null -> 0
        lastCompletedDay == today -> streak
        lastCompletedDay == schedule.previousDueDay(today) -> streak
        else -> 0
    }
}
