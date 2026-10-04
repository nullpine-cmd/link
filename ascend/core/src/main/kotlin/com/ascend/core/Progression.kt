package com.ascend.core

/** Положение внутри уровня: сколько опыта набрано и сколько нужно до следующего. */
data class LevelInfo(
    val level: Int,
    val xpInLevel: Long,
    val xpForNext: Long,
    val totalXp: Long,
) {
    val isMax: Boolean get() = xpForNext == 0L
    val progress: Float get() = if (isMax) 1f else (xpInLevel.toFloat() / xpForNext).coerceIn(0f, 1f)
    val xpToNext: Long get() = (xpForNext - xpInLevel).coerceAtLeast(0)
}

/**
 * Кривые опыта. Первые уровни приходят быстро (2-й — за 3–4 квеста),
 * дальше путь растягивается, чтобы высокие ранги оставались достижением.
 */
object Progression {
    const val MAX_LEVEL = 100
    const val MAX_ATTRIBUTE_LEVEL = 99

    fun xpToNextLevel(level: Int): Long = 60L + 30L * level + 2L * level * level

    fun attributeXpToNext(level: Int): Long = 40L + 20L * level

    fun heroLevel(totalXp: Long): LevelInfo = walk(totalXp, MAX_LEVEL, ::xpToNextLevel)

    fun attributeLevel(xp: Long): LevelInfo = walk(xp, MAX_ATTRIBUTE_LEVEL, ::attributeXpToNext)

    /** Суммарный опыт, необходимый для достижения [level]. */
    fun totalXpForLevel(level: Int): Long = (1 until level.coerceIn(1, MAX_LEVEL)).sumOf(::xpToNextLevel)

    private inline fun walk(xp: Long, maxLevel: Int, cost: (Int) -> Long): LevelInfo {
        val total = xp.coerceAtLeast(0)
        var level = 1
        var remaining = total
        while (level < maxLevel) {
            val need = cost(level)
            if (remaining < need) break
            remaining -= need
            level++
        }
        val forNext = if (level >= maxLevel) 0L else cost(level)
        return LevelInfo(level, if (forNext == 0L) 0L else remaining, forNext, total)
    }
}
