package com.ascend.core

/** Ранги героя — вехи на пути, открываются с уровнем. */
enum class Rank(
    val letter: String,
    val title: String,
    val minLevel: Int,
    val motto: String,
) {
    E("E", "Новичок", 1, "Каждый путь начинается с первого шага"),
    D("D", "Искатель", 5, "Ты нашёл свою дорогу — продолжай идти"),
    C("C", "Странник", 10, "Привычки становятся твоей силой"),
    B("B", "Воитель", 15, "Дисциплина стала частью тебя"),
    A("A", "Рыцарь", 22, "Твоя воля крепче стали"),
    S("S", "Мастер", 30, "Немногие заходят так далеко"),
    SS("SS", "Герой", 40, "О твоём пути слагают истории"),
    SSS("SSS", "Легенда", 50, "Ты стал тем, кем мечтал быть"),
    ;

    val next: Rank? get() = entries.getOrNull(ordinal + 1)

    companion object {
        fun forLevel(level: Int): Rank = entries.last { level >= it.minLevel }
    }
}
