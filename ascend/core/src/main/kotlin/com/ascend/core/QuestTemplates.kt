package com.ascend.core

object Units {
    const val PAGES = "стр."
    const val TIMES = "раз"
    const val MINUTES = "мин"
    const val KILOMETERS = "км"
    const val GLASSES = "стак."
    const val STEPS = "шагов"

    val presets = listOf(PAGES, TIMES, MINUTES, KILOMETERS, GLASSES, STEPS)
}

enum class QuestKind(val title: String) {
    HABIT("Привычка"),
    TASK("Задача"),
}

data class QuestTemplate(
    val emoji: String,
    val title: String,
    val attribute: Attribute,
    val difficulty: Difficulty,
    val target: Int? = null,
    val unit: String? = null,
    val schedule: WeekSchedule = WeekSchedule.EVERY_DAY,
)

/** Готовые идеи квестов: ни один из них не обязателен — это лишь подсказки. */
object QuestTemplates {
    val all: List<QuestTemplate> = listOf(
        QuestTemplate("📖", "Читать книгу", Attribute.INTELLECT, Difficulty.NORMAL, 20, Units.PAGES),
        QuestTemplate("💪", "Отжимания", Attribute.STRENGTH, Difficulty.NORMAL, 30, Units.TIMES),
        QuestTemplate("🧘", "Медитация", Attribute.SPIRIT, Difficulty.EASY, 10, Units.MINUTES),
        QuestTemplate("💧", "Пить воду", Attribute.VITALITY, Difficulty.EASY, 8, Units.GLASSES),
        QuestTemplate("🚶", "Прогулка", Attribute.VITALITY, Difficulty.EASY, 30, Units.MINUTES),
        QuestTemplate("🏃", "Пробежка", Attribute.STRENGTH, Difficulty.HARD, 3, Units.KILOMETERS, WeekSchedule(0b001_0101)),
        QuestTemplate("✍️", "Вести дневник", Attribute.SPIRIT, Difficulty.EASY),
        QuestTemplate("📞", "Позвонить близким", Attribute.CHARISMA, Difficulty.EASY, schedule = WeekSchedule.WEEKENDS),
        QuestTemplate("🎸", "Практика на инструменте", Attribute.MASTERY, Difficulty.NORMAL, 20, Units.MINUTES),
        QuestTemplate("🌐", "Изучать язык", Attribute.INTELLECT, Difficulty.NORMAL, 15, Units.MINUTES),
        QuestTemplate("😴", "Лечь спать до 23:00", Attribute.VITALITY, Difficulty.NORMAL),
        QuestTemplate("💻", "Работа над проектом", Attribute.MASTERY, Difficulty.HARD, 60, Units.MINUTES, WeekSchedule.WEEKDAYS),
        QuestTemplate("🧹", "Навести порядок", Attribute.SPIRIT, Difficulty.EASY, 15, Units.MINUTES),
        QuestTemplate("🤸", "Растяжка", Attribute.STRENGTH, Difficulty.EASY, 10, Units.MINUTES),
    )

    val emojis: List<String> = listOf(
        "📖", "💪", "🧘", "💧", "🚶", "🏃", "✍️", "📞", "🎸", "🌐", "😴", "💻",
        "🧹", "🤸", "🏋️", "🚴", "🏊", "🥗", "🍎", "☀️", "🌙", "🎨", "🎯", "🧠",
        "💼", "💰", "🌱", "❤️", "🤝", "🎓", "📝", "🎧", "📷", "🧩", "🛠️", "⚡",
    )

    val avatars: List<String> = listOf(
        "🦊", "🐺", "🦁", "🐉", "🦉", "🐯", "🦅", "🐻", "🦄", "🐼", "🦈", "🐲",
    )
}
