package com.ascend.app.ui.util

import com.ascend.core.WeekSchedule
import java.time.LocalDate

private val monthsGenitive = listOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря",
)

private val monthsShort = listOf(
    "янв", "фев", "мар", "апр", "мая", "июн",
    "июл", "авг", "сен", "окт", "ноя", "дек",
)

private val weekdaysFull = listOf(
    "понедельник", "вторник", "среда", "четверг", "пятница", "суббота", "воскресенье",
)

fun weekdayShort(day: Long): String = WeekSchedule.shortName(LocalDate.ofEpochDay(day).dayOfWeek)

fun weekdayFull(day: Long): String = weekdaysFull[LocalDate.ofEpochDay(day).dayOfWeek.value - 1]

fun dateLong(day: Long): String {
    val date = LocalDate.ofEpochDay(day)
    return "${date.dayOfMonth} ${monthsGenitive[date.monthValue - 1]}"
}

fun dateShort(day: Long): String {
    val date = LocalDate.ofEpochDay(day)
    return "${date.dayOfMonth} ${monthsShort[date.monthValue - 1]}"
}

/** «Сегодня», «Завтра», «Вчера» или «12 октября». */
fun relativeDay(day: Long, today: Long): String = when (day - today) {
    0L -> "Сегодня"
    1L -> "Завтра"
    -1L -> "Вчера"
    2L -> "Послезавтра"
    else -> dateLong(day)
}

fun formatTime(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)

/** Русская множественная форма: plural(5, "день", "дня", "дней") → «дней». */
fun plural(n: Long, one: String, few: String, many: String): String {
    val mod10 = n % 10
    val mod100 = n % 100
    return when {
        mod10 == 1L && mod100 != 11L -> one
        mod10 in 2L..4L && mod100 !in 12L..14L -> few
        else -> many
    }
}

fun plural(n: Int, one: String, few: String, many: String): String = plural(n.toLong(), one, few, many)

fun daysWord(n: Int): String = "$n ${plural(n, "день", "дня", "дней")}"

fun pagesWord(n: Int): String = "$n ${plural(n, "страница", "страницы", "страниц")}"

fun questsWord(n: Int): String = "$n ${plural(n, "квест", "квеста", "квестов")}"
