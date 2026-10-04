package com.ascend.core

import kotlin.math.roundToInt

enum class BonusKind(val title: String) {
    TARGET("Цель достигнута"),
    OVERACHIEVE("Перевыполнение"),
    STREAK("Серия"),
    FIRST_OF_DAY("Первый шаг дня"),
    COMEBACK("Возвращение героя"),
}

data class Bonus(val kind: BonusKind, val xp: Int)

data class RewardInput(
    val difficulty: Difficulty,
    /** Сколько сделано. Любое значение больше нуля засчитывается полностью. */
    val amount: Int,
    /** Цель на день; null — квест без количества. */
    val target: Int? = null,
    val isHabit: Boolean = false,
    val streakBefore: Int = 0,
    val lastCompletedDay: Long? = null,
    /** Предыдущий запланированный день привычки — нужен, чтобы понять, продолжается ли серия. */
    val previousDueDay: Long? = null,
    val isFirstCompletionToday: Boolean = false,
    /** Сколько дней прошло с последней активности; null — первая активность вообще. */
    val daysSinceLastActive: Long? = null,
)

data class Reward(
    val baseXp: Int,
    val bonuses: List<Bonus>,
    val gold: Int,
    val newStreak: Int,
    val hitTarget: Boolean,
    val overachieved: Boolean,
    val comeback: Boolean,
) {
    val totalXp: Int get() = baseXp + bonuses.sumOf { it.xp }
}

/**
 * Сердце мотивации: награда никогда не отнимается, а неполный результат не обесценивается.
 * Прочитал одну страницу из двадцати — получаешь полную базовую награду, цель лишь даёт бонус.
 */
object RewardEngine {
    const val FIRST_OF_DAY_XP = 5
    const val COMEBACK_XP = 20
    const val COMEBACK_AFTER_DAYS = 3L
    const val MAX_STREAK_STEPS = 20
    const val GOLD_RATE = 0.4

    fun calculate(input: RewardInput): Reward {
        require(input.amount > 0) { "Засчитывается любой прогресс больше нуля" }
        val base = input.difficulty.baseXp
        val bonuses = mutableListOf<Bonus>()

        val target = input.target?.takeIf { it > 0 }
        val hitTarget = target == null || input.amount >= target
        val overachieved = target != null && input.amount >= target * 2
        if (target != null && hitTarget) bonuses += Bonus(BonusKind.TARGET, base / 2)
        if (overachieved) bonuses += Bonus(BonusKind.OVERACHIEVE, base / 4)

        val newStreak = when {
            !input.isHabit -> 0
            input.previousDueDay != null && input.lastCompletedDay == input.previousDueDay -> input.streakBefore + 1
            else -> 1
        }
        val streakSteps = (newStreak - 1).coerceIn(0, MAX_STREAK_STEPS)
        if (streakSteps > 0) {
            bonuses += Bonus(BonusKind.STREAK, (base * streakSteps * 0.05).roundToInt().coerceAtLeast(1))
        }

        if (input.isFirstCompletionToday) bonuses += Bonus(BonusKind.FIRST_OF_DAY, FIRST_OF_DAY_XP)

        val comeback = (input.daysSinceLastActive ?: 0L) >= COMEBACK_AFTER_DAYS
        if (comeback) bonuses += Bonus(BonusKind.COMEBACK, COMEBACK_XP)

        val total = base + bonuses.sumOf { it.xp }
        return Reward(
            baseXp = base,
            bonuses = bonuses,
            gold = goldFor(total),
            newStreak = newStreak,
            hitTarget = hitTarget,
            overachieved = overachieved,
            comeback = comeback,
        )
    }

    /** Свободная запись чтения из библиотеки: скромная награда, растущая с числом страниц. */
    fun readingSession(pages: Int, isFirstCompletionToday: Boolean, daysSinceLastActive: Long?): Reward {
        require(pages > 0) { "Засчитывается любой прогресс больше нуля" }
        val base = Difficulty.EASY.baseXp + pages.coerceAtMost(50) / 5
        val bonuses = buildList {
            if (isFirstCompletionToday) add(Bonus(BonusKind.FIRST_OF_DAY, FIRST_OF_DAY_XP))
            if ((daysSinceLastActive ?: 0L) >= COMEBACK_AFTER_DAYS) add(Bonus(BonusKind.COMEBACK, COMEBACK_XP))
        }
        val total = base + bonuses.sumOf { it.xp }
        return Reward(
            baseXp = base,
            bonuses = bonuses,
            gold = goldFor(total),
            newStreak = 0,
            hitTarget = true,
            overachieved = false,
            comeback = bonuses.any { it.kind == BonusKind.COMEBACK },
        )
    }

    fun goldFor(xp: Int): Int = (xp * GOLD_RATE).roundToInt().coerceAtLeast(1)
}

/** Особые награды за события. */
object Milestones {
    const val BOOK_FINISHED_XP = 100
    const val BOOK_FINISHED_GOLD = 50
    const val PERFECT_DAY_XP = 15
    const val PERFECT_DAY_GOLD = 10
    const val PERFECT_DAY_MIN_QUESTS = 3
}
