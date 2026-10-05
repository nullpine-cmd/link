package com.ascend.core

/** Ветви талантов — для группировки на экране. */
enum class TalentBranch(val title: String, val description: String) {
    RHYTHM("Ритм", "Время, серии и комбо"),
    FORTUNE("Фортуна", "Золото и бонусы за возвращение"),
    MASTERY("Мастерство", "Цели, усердие и равновесие"),
}

/**
 * Таланты — пассивные усиления, которые покупаются за золото и открываются с уровнем.
 * Ни один талант не делает какой-то путь обязательным: это выбор игрока, а не требование.
 */
enum class Talent(
    val branch: TalentBranch,
    val emoji: String,
    val title: String,
    val description: String,
    val cost: Int,
    val minLevel: Int,
) {
    EARLY_BIRD(TalentBranch.RHYTHM, "🌅", "Утренняя звезда", "+25% базового опыта за квесты до 10:00", 150, 3),
    NIGHT_WATCH(TalentBranch.RHYTHM, "🌙", "Ночной дозор", "+25% базового опыта за квесты после 21:00", 150, 3),
    IRON_STREAK(TalentBranch.RHYTHM, "⛓️", "Железная серия", "Бонус за серию растёт до 30 дней вместо 20", 300, 6),
    COMBO_KING(TalentBranch.RHYTHM, "⚡", "Король комбо", "Комбо накапливается до ×8 вместо ×5", 300, 6),
    FOCUS_MASTER(TalentBranch.FORTUNE, "🎯", "Первый шаг", "Бонус за первый квест дня: 15 XP вместо 5", 180, 3),
    SECOND_WIND(TalentBranch.FORTUNE, "🌬️", "Второе дыхание", "Бонус за возвращение после перерыва удвоен", 200, 4),
    GOLDEN_TOUCH(TalentBranch.FORTUNE, "🪙", "Золотое чутьё", "Золота за опыт на четверть больше", 250, 5),
    PERFECTIONIST(TalentBranch.MASTERY, "💎", "Перфекционист", "Бонус за достигнутую цель: +75% вместо +50%", 350, 7),
    DILIGENCE(TalentBranch.MASTERY, "📈", "Усердие", "+2 XP за каждые 25% сверх цели (до +10)", 200, 4),
    BALANCE_SAGE(TalentBranch.MASTERY, "⚖️", "Мудрец равновесия", "+15% базового опыта за самую слабую характеристику", 400, 8),
    ;

    companion object {
        fun byName(name: String): Talent? = entries.firstOrNull { it.name == name }
    }
}

/** Решение о покупке таланта. */
sealed interface TalentPurchase {
    data object Ok : TalentPurchase
    data object AlreadyOwned : TalentPurchase
    data class LevelTooLow(val required: Int) : TalentPurchase
    data class NotEnoughGold(val missing: Long) : TalentPurchase

    companion object {
        fun check(talent: Talent, owned: Set<Talent>, level: Int, gold: Long): TalentPurchase = when {
            talent in owned -> AlreadyOwned
            level < talent.minLevel -> LevelTooLow(talent.minLevel)
            gold < talent.cost -> NotEnoughGold(talent.cost - gold)
            else -> Ok
        }
    }
}
