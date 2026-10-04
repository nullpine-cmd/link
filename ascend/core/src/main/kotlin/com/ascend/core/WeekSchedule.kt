package com.ascend.core

import java.time.DayOfWeek
import java.time.LocalDate

/** Дни недели, в которые повторяется привычка. Бит 0 — понедельник, бит 6 — воскресенье. */
@JvmInline
value class WeekSchedule(val mask: Int) {

    val isEmpty: Boolean get() = mask and ALL == 0
    val isEveryDay: Boolean get() = mask and ALL == ALL
    val daysPerWeek: Int get() = Integer.bitCount(mask and ALL)

    operator fun contains(day: DayOfWeek): Boolean = mask and bit(day) != 0

    fun isDue(epochDay: Long): Boolean = LocalDate.ofEpochDay(epochDay).dayOfWeek in this

    fun toggle(day: DayOfWeek): WeekSchedule = WeekSchedule(mask xor bit(day))

    /** Ближайший день до [epochDay] (не включая его), в который привычка была запланирована. */
    fun previousDueDay(epochDay: Long): Long? {
        if (isEmpty) return null
        for (offset in 1..7) {
            val day = epochDay - offset
            if (isDue(day)) return day
        }
        return null
    }

    fun label(): String = when {
        isEmpty -> "Не запланировано"
        isEveryDay -> "Каждый день"
        mask and ALL == WEEKDAYS.mask -> "По будням"
        mask and ALL == WEEKENDS.mask -> "По выходным"
        else -> DayOfWeek.entries.filter { it in this }.joinToString(", ") { shortName(it) }
    }

    companion object {
        private const val ALL = 0b111_1111
        val EVERY_DAY = WeekSchedule(ALL)
        val WEEKDAYS = WeekSchedule(0b001_1111)
        val WEEKENDS = WeekSchedule(0b110_0000)

        private fun bit(day: DayOfWeek): Int = 1 shl (day.value - 1)

        fun shortName(day: DayOfWeek): String = when (day) {
            DayOfWeek.MONDAY -> "Пн"
            DayOfWeek.TUESDAY -> "Вт"
            DayOfWeek.WEDNESDAY -> "Ср"
            DayOfWeek.THURSDAY -> "Чт"
            DayOfWeek.FRIDAY -> "Пт"
            DayOfWeek.SATURDAY -> "Сб"
            DayOfWeek.SUNDAY -> "Вс"
        }
    }
}
