package com.ascend.core

import kotlin.math.roundToInt

enum class BonusKind(val title: String) {
    TARGET("Цель достигнута"),
    OVERACHIEVE("Перевыполнение"),
    STREAK("Серия"),
    FIRST_OF_DAY("Первый шаг дня"),
    COMEBACK("Возвращение героя"),
    COMBO("Комбо"),
    TALENT("Талант"),
}

data class Bonus(val kind: BonusKind, val xp: Int, val label: String = kind.title)

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
    /** Час выполнения (0–23) — для талантов, зависящих от времени. */
    val hour: Int = 12,
    /** Сколько выполнений уже стоит в цепочке комбо перед этим (0 — цепочки нет). */
    val comboStep: Int = 0,
    val talents: Set<Talent> = emptySet(),
    /** Прокачивается ли сейчас самая слабая характеристика героя. */
    val isWeakestAttribute: Boolean = false,
)

data class Reward(
    val baseXp: Int,
    val bonuses: List<Bonus>,
    val gold: Int,
    val newStreak: Int,
    val hitTarget: Boolean,
    val overachieved: Boolean,
    val comeback: Boolean,
    val comboStep: Int = 0,
) {
    val totalXp: Int get() = baseXp + bonuses.sumOf { it.xp }
}

/**
 * Сердце мотивации: награда никогда не отнимается, а неполный результат не обесценивается.
 * Прочитал одну страницу из двадцати — получаешь полную базовую награду, цель лишь даёт бонус.
 */
object RewardEngine {
    const val FIRST_OF_DAY_XP = 5
    const val FIRST_OF_DAY_XP_FOCUSED = 15
    const val COMEBACK_XP = 20
    const val COMEBACK_AFTER_DAYS = 3L
    const val MAX_STREAK_STEPS = 20
    const val MAX_STREAK_STEPS_IRON = 30
    const val GOLD_RATE = 0.4
    const val GOLD_RATE_GOLDEN = 0.5
    const val EARLY_BIRD_BEFORE_HOUR = 10
    const val NIGHT_WATCH_FROM_HOUR = 21

    fun calculate(input: RewardInput): Reward {
        require(input.amount > 0) { "Засчитывается любой прогресс больше нуля" }
        val talents = input.talents
        val base = input.difficulty.baseXp
        val bonuses = mutableListOf<Bonus>()

        val target = input.target?.takeIf { it > 0 }
        val hitTarget = target == null || input.amount >= target
        val overachieved = target != null && input.amount >= target * 2
        if (target != null && hitTarget) {
            val targetBonus = if (Talent.PERFECTIONIST in talents) base * 3 / 4 else base / 2
            bonuses += Bonus(BonusKind.TARGET, targetBonus)
        }
        if (overachieved) bonuses += Bonus(BonusKind.OVERACHIEVE, base / 4)
        if (target != null && hitTarget && Talent.DILIGENCE in talents) {
            val quarters = ((input.amount - target) * 4 / target).coerceIn(0, 5)
            if (quarters > 0) bonuses += Bonus(BonusKind.TALENT, quarters * 2, Talent.DILIGENCE.title)
        }

        val newStreak = when {
            !input.isHabit -> 0
            input.previousDueDay != null && input.lastCompletedDay == input.previousDueDay -> input.streakBefore + 1
            else -> 1
        }
        val streakCap = if (Talent.IRON_STREAK in talents) MAX_STREAK_STEPS_IRON else MAX_STREAK_STEPS
        val streakSteps = (newStreak - 1).coerceIn(0, streakCap)
        if (streakSteps > 0) {
            bonuses += Bonus(BonusKind.STREAK, (base * streakSteps * 0.05).roundToInt().coerceAtLeast(1))
        }

        val comboSteps = input.comboStep.coerceIn(0, Combo.cap(talents))
        if (comboSteps > 0) {
            bonuses += Bonus(BonusKind.COMBO, (base * comboSteps * Combo.STEP_RATE).roundToInt().coerceAtLeast(1), "Комбо ×${comboSteps + 1}")
        }

        if (input.isFirstCompletionToday) {
            bonuses += Bonus(BonusKind.FIRST_OF_DAY, if (Talent.FOCUS_MASTER in talents) FIRST_OF_DAY_XP_FOCUSED else FIRST_OF_DAY_XP)
        }

        val comeback = (input.daysSinceLastActive ?: 0L) >= COMEBACK_AFTER_DAYS
        if (comeback) bonuses += Bonus(BonusKind.COMEBACK, if (Talent.SECOND_WIND in talents) COMEBACK_XP * 2 else COMEBACK_XP)

        if (Talent.EARLY_BIRD in talents && input.hour < EARLY_BIRD_BEFORE_HOUR) {
            bonuses += Bonus(BonusKind.TALENT, (base / 4).coerceAtLeast(1), Talent.EARLY_BIRD.title)
        }
        if (Talent.NIGHT_WATCH in talents && input.hour >= NIGHT_WATCH_FROM_HOUR) {
            bonuses += Bonus(BonusKind.TALENT, (base / 4).coerceAtLeast(1), Talent.NIGHT_WATCH.title)
        }
        if (Talent.BALANCE_SAGE in talents && input.isWeakestAttribute) {
            bonuses += Bonus(BonusKind.TALENT, (base * 0.15).roundToInt().coerceAtLeast(1), Talent.BALANCE_SAGE.title)
        }

        val total = base + bonuses.sumOf { it.xp }
        return Reward(
            baseXp = base,
            bonuses = bonuses,
            gold = goldFor(total, talents),
            newStreak = newStreak,
            hitTarget = hitTarget,
            overachieved = overachieved,
            comeback = comeback,
            comboStep = comboSteps,
        )
    }

    /** Свободная запись чтения из библиотеки: скромная награда, растущая с числом страниц. */
    fun readingSession(
        pages: Int,
        isFirstCompletionToday: Boolean,
        daysSinceLastActive: Long?,
        talents: Set<Talent> = emptySet(),
        hour: Int = 12,
        comboStep: Int = 0,
    ): Reward {
        require(pages > 0) { "Засчитывается любой прогресс больше нуля" }
        val base = Difficulty.EASY.baseXp + pages.coerceAtMost(50) / 5
        val comboSteps = comboStep.coerceIn(0, Combo.cap(talents))
        val bonuses = buildList {
            if (comboSteps > 0) add(Bonus(BonusKind.COMBO, (base * comboSteps * Combo.STEP_RATE).roundToInt().coerceAtLeast(1), "Комбо ×${comboSteps + 1}"))
            if (isFirstCompletionToday) add(Bonus(BonusKind.FIRST_OF_DAY, if (Talent.FOCUS_MASTER in talents) FIRST_OF_DAY_XP_FOCUSED else FIRST_OF_DAY_XP))
            if ((daysSinceLastActive ?: 0L) >= COMEBACK_AFTER_DAYS) add(Bonus(BonusKind.COMEBACK, if (Talent.SECOND_WIND in talents) COMEBACK_XP * 2 else COMEBACK_XP))
            if (Talent.EARLY_BIRD in talents && hour < EARLY_BIRD_BEFORE_HOUR) add(Bonus(BonusKind.TALENT, (base / 4).coerceAtLeast(1), Talent.EARLY_BIRD.title))
            if (Talent.NIGHT_WATCH in talents && hour >= NIGHT_WATCH_FROM_HOUR) add(Bonus(BonusKind.TALENT, (base / 4).coerceAtLeast(1), Talent.NIGHT_WATCH.title))
        }
        val total = base + bonuses.sumOf { it.xp }
        return Reward(
            baseXp = base,
            bonuses = bonuses,
            gold = goldFor(total, talents),
            newStreak = 0,
            hitTarget = true,
            overachieved = false,
            comeback = bonuses.any { it.kind == BonusKind.COMEBACK },
            comboStep = comboSteps,
        )
    }

    fun goldFor(xp: Int, talents: Set<Talent> = emptySet()): Int {
        val rate = if (Talent.GOLDEN_TOUCH in talents) GOLD_RATE_GOLDEN else GOLD_RATE
        return (xp * rate).roundToInt().coerceAtLeast(1)
    }
}

/** Особые награды за события. */
object Milestones {
    const val BOOK_FINISHED_XP = 100
    const val BOOK_FINISHED_GOLD = 50
    const val PERFECT_DAY_XP = 15
    const val PERFECT_DAY_GOLD = 10
    const val PERFECT_DAY_MIN_QUESTS = 3
}
