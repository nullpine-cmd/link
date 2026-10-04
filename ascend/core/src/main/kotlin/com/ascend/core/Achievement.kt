package com.ascend.core

enum class Tier(val title: String, val xp: Int, val gold: Int) {
    BRONZE("Бронза", 25, 15),
    SILVER("Серебро", 60, 40),
    GOLD("Золото", 150, 100),
    LEGENDARY("Легенда", 400, 250),
}

enum class Metric {
    QUESTS,
    BEST_DAY_STREAK,
    PAGES,
    BOOKS,
    LEVEL,
    MIN_ATTRIBUTE_LEVEL,
    MAX_ATTRIBUTE_LEVEL,
    EARLY_COMPLETIONS,
    LATE_COMPLETIONS,
    OVERACHIEVEMENTS,
    COMEBACKS,
    PURCHASES,
    PERFECT_DAYS,
}

enum class Achievement(
    val title: String,
    val description: String,
    val tier: Tier,
    val metric: Metric,
    val goal: Long,
) {
    FIRST_STEP("Первый шаг", "Выполни первый квест", Tier.BRONZE, Metric.QUESTS, 1),
    WARM_UP("Разогрев", "Выполни 10 квестов", Tier.BRONZE, Metric.QUESTS, 10),
    TIRELESS("Неутомимый", "Выполни 100 квестов", Tier.SILVER, Metric.QUESTS, 100),
    THOUSAND_DEEDS("Тысяча подвигов", "Выполни 1000 квестов", Tier.LEGENDARY, Metric.QUESTS, 1000),
    SPARK("Искра", "Будь активен 3 дня подряд", Tier.BRONZE, Metric.BEST_DAY_STREAK, 3),
    WEEK_OF_POWER("Неделя силы", "Будь активен 7 дней подряд", Tier.SILVER, Metric.BEST_DAY_STREAK, 7),
    IRON_WILL("Железная воля", "Будь активен 30 дней подряд", Tier.GOLD, Metric.BEST_DAY_STREAK, 30),
    UNSTOPPABLE("Неудержимый", "Будь активен 100 дней подряд", Tier.LEGENDARY, Metric.BEST_DAY_STREAK, 100),
    FIRST_PAGES("Первые страницы", "Прочитай 50 страниц", Tier.BRONZE, Metric.PAGES, 50),
    BOOKWORM("Книжный червь", "Прочитай 1000 страниц", Tier.SILVER, Metric.PAGES, 1000),
    KEEPER_OF_KNOWLEDGE("Хранитель знаний", "Прочитай 5000 страниц", Tier.GOLD, Metric.PAGES, 5000),
    LAST_PAGE("Последняя страница", "Дочитай первую книгу", Tier.SILVER, Metric.BOOKS, 1),
    BIBLIOPHILE("Библиофил", "Дочитай 10 книг", Tier.GOLD, Metric.BOOKS, 10),
    AWAKENING("Пробуждение", "Достигни 5 уровня", Tier.BRONZE, Metric.LEVEL, 5),
    ASCENT("Восхождение", "Достигни 10 уровня", Tier.SILVER, Metric.LEVEL, 10),
    PEAK_CONQUEROR("Покоритель вершин", "Достигни 25 уровня", Tier.GOLD, Metric.LEVEL, 25),
    LIVING_LEGEND("Живая легенда", "Достигни 50 уровня", Tier.LEGENDARY, Metric.LEVEL, 50),
    HARMONY("Гармония", "Подними все характеристики до 3 уровня", Tier.GOLD, Metric.MIN_ATTRIBUTE_LEVEL, 3),
    SPECIALIST("Специалист", "Подними любую характеристику до 10 уровня", Tier.SILVER, Metric.MAX_ATTRIBUTE_LEVEL, 10),
    EARLY_BIRD("Ранняя пташка", "Выполни квест до 7 утра", Tier.BRONZE, Metric.EARLY_COMPLETIONS, 1),
    NIGHT_OWL("Ночная сова", "Выполни квест после 23:00", Tier.BRONZE, Metric.LATE_COMPLETIONS, 1),
    BEYOND_LIMITS("Сверх нормы", "Сделай вдвое больше задуманного", Tier.SILVER, Metric.OVERACHIEVEMENTS, 1),
    RETURN_OF_THE_HERO("Возвращение героя", "Вернись к делам после перерыва", Tier.BRONZE, Metric.COMEBACKS, 1),
    TREAT_YOURSELF("Заслуженный отдых", "Купи первую награду в лавке", Tier.BRONZE, Metric.PURCHASES, 1),
    PERFECT_DAY("Идеальный день", "Выполни все квесты дня (от трёх)", Tier.SILVER, Metric.PERFECT_DAYS, 1),
    PERFECT_WEEK("Безупречность", "Проведи 7 идеальных дней", Tier.GOLD, Metric.PERFECT_DAYS, 7),
}

/** Срез статистики героя, по которому проверяются достижения. */
data class HeroStats(
    val questsCompleted: Long = 0,
    val bestDayStreak: Long = 0,
    val pagesRead: Long = 0,
    val booksFinished: Long = 0,
    val level: Int = 1,
    val attributeLevels: Map<Attribute, Int> = emptyMap(),
    val earlyCompletions: Long = 0,
    val lateCompletions: Long = 0,
    val overachievements: Long = 0,
    val comebacks: Long = 0,
    val purchases: Long = 0,
    val perfectDays: Long = 0,
) {
    fun value(metric: Metric): Long = when (metric) {
        Metric.QUESTS -> questsCompleted
        Metric.BEST_DAY_STREAK -> bestDayStreak
        Metric.PAGES -> pagesRead
        Metric.BOOKS -> booksFinished
        Metric.LEVEL -> level.toLong()
        Metric.MIN_ATTRIBUTE_LEVEL -> Attribute.entries.minOf { attributeLevels[it] ?: 1 }.toLong()
        Metric.MAX_ATTRIBUTE_LEVEL -> Attribute.entries.maxOf { attributeLevels[it] ?: 1 }.toLong()
        Metric.EARLY_COMPLETIONS -> earlyCompletions
        Metric.LATE_COMPLETIONS -> lateCompletions
        Metric.OVERACHIEVEMENTS -> overachievements
        Metric.COMEBACKS -> comebacks
        Metric.PURCHASES -> purchases
        Metric.PERFECT_DAYS -> perfectDays
    }
}

object AchievementRules {
    const val EARLY_HOUR = 7
    const val LATE_HOUR = 23

    fun progress(achievement: Achievement, stats: HeroStats): Float =
        (stats.value(achievement.metric).toFloat() / achievement.goal).coerceIn(0f, 1f)

    fun earned(stats: HeroStats): List<Achievement> =
        Achievement.entries.filter { stats.value(it.metric) >= it.goal }

    fun newlyEarned(stats: HeroStats, alreadyUnlocked: Set<Achievement>): List<Achievement> =
        earned(stats).filterNot { it in alreadyUnlocked }
}
