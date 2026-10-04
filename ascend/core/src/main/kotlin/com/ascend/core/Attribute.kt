package com.ascend.core

/**
 * Характеристики героя. Все пути развития равноценны: за одинаковую сложность
 * любая характеристика приносит одинаковый опыт — меняется лишь то, какая из них растёт.
 */
enum class Attribute(
    val title: String,
    val short: String,
    val description: String,
    val examples: String,
) {
    STRENGTH(
        title = "Сила",
        short = "СИЛ",
        description = "Тело, спорт и выносливость",
        examples = "Отжимания, бег, зал, растяжка",
    ),
    INTELLECT(
        title = "Интеллект",
        short = "ИНТ",
        description = "Знания, чтение и учёба",
        examples = "Книги, курсы, языки, конспекты",
    ),
    VITALITY(
        title = "Здоровье",
        short = "ЗДР",
        description = "Сон, питание и забота о себе",
        examples = "Вода, режим сна, прогулки, полезная еда",
    ),
    SPIRIT(
        title = "Дух",
        short = "ДУХ",
        description = "Осознанность, воля и спокойствие",
        examples = "Медитация, дневник, цифровой детокс",
    ),
    CHARISMA(
        title = "Харизма",
        short = "ХАР",
        description = "Люди, общение и отношения",
        examples = "Звонок близким, встреча, помощь другу",
    ),
    MASTERY(
        title = "Мастерство",
        short = "МАС",
        description = "Работа, проекты и творчество",
        examples = "Проект, музыка, рисование, код",
    ),
}
