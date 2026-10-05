package com.ascend.core

import java.time.DayOfWeek
import java.time.LocalDate

enum class ChallengePeriod(val title: String) { DAILY("Испытание дня"), WEEKLY("Испытание недели") }

/** Что именно измеряет испытание. */
enum class ChallengeMetric {
    QUESTS,
    XP,
    PAGES,
    ATTRIBUTES,
    EARLY,
    OVERACHIEVE,
    ACTIVE_DAYS,
    PERFECT_DAYS,
}

/** Срез активности за период — всё, что нужно для подсчёта прогресса испытаний и урона боссу. */
data class ActivityStats(
    val quests: Int = 0,
    val xp: Long = 0,
    val pages: Int = 0,
    val attributes: Set<Attribute> = emptySet(),
    val early: Int = 0,
    val overachieved: Int = 0,
    val activeDays: Int = 0,
    val perfectDays: Int = 0,
) {
    fun value(metric: ChallengeMetric): Int = when (metric) {
        ChallengeMetric.QUESTS -> quests
        ChallengeMetric.XP -> xp.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        ChallengeMetric.PAGES -> pages
        ChallengeMetric.ATTRIBUTES -> attributes.size
        ChallengeMetric.EARLY -> early
        ChallengeMetric.OVERACHIEVE -> overachieved
        ChallengeMetric.ACTIVE_DAYS -> activeDays
        ChallengeMetric.PERFECT_DAYS -> perfectDays
    }
}

/** Что есть у героя — от этого зависит, какие испытания имеют смысл. */
data class HeroProfile(
    val level: Int = 1,
    val hasBooks: Boolean = false,
    val hasTargets: Boolean = false,
)

data class Challenge(
    val id: String,
    val period: ChallengePeriod,
    val metric: ChallengeMetric,
    val target: Int,
    val emoji: String,
    val title: String,
    val xp: Int,
    val gold: Int,
    val periodStart: Long,
) {
    fun progress(stats: ActivityStats): ChallengeProgress =
        ChallengeProgress(this, stats.value(metric).coerceIn(0, target))
}

data class ChallengeProgress(val challenge: Challenge, val current: Int) {
    val done: Boolean get() = current >= challenge.target
    val fraction: Float get() = if (challenge.target == 0) 1f else (current.toFloat() / challenge.target).coerceIn(0f, 1f)
}

/**
 * Испытания генерируются детерминированно из даты и профиля героя, поэтому их не нужно хранить:
 * у всех пользователей в один день один и тот же набор — лишь цели подстраиваются под уровень.
 */
object Challenges {
    const val DAILY_COUNT = 3
    const val WEEKLY_COUNT = 3

    private class Template(
        val metric: ChallengeMetric,
        val emoji: String,
        val xp: Int,
        val gold: Int,
        val eligible: (HeroProfile) -> Boolean = { true },
        val target: (Int) -> Int,
        val title: (Int) -> String,
    )

    private val daily = listOf(
        Template(ChallengeMetric.QUESTS, "⚔️", 20, 10, target = { level -> if (level < 5) 3 else if (level < 15) 4 else 5 }) { n ->
            "Выполни $n ${plural(n, "квест", "квеста", "квестов")}"
        },
        Template(ChallengeMetric.XP, "✨", 25, 12, target = { level -> roundTo(60 + 8 * level, 10) }) { n -> "Набери $n XP за день" },
        Template(ChallengeMetric.ATTRIBUTES, "🌈", 25, 12, target = { level -> if (level < 10) 2 else 3 }) { n ->
            "Прокачай $n ${plural(n, "разную характеристику", "разные характеристики", "разных характеристик")}"
        },
        Template(ChallengeMetric.PAGES, "📖", 20, 10, eligible = { it.hasBooks }, target = { level -> if (level < 10) 15 else 25 }) { n ->
            "Прочитай $n ${plural(n, "страницу", "страницы", "страниц")}"
        },
        Template(ChallengeMetric.EARLY, "🌅", 20, 10, target = { 1 }) { "Выполни квест до 9:00" },
        Template(ChallengeMetric.OVERACHIEVE, "💥", 25, 12, eligible = { it.hasTargets }, target = { 1 }) { "Сделай вдвое больше цели" },
    )

    private val weekly = listOf(
        Template(ChallengeMetric.ACTIVE_DAYS, "🔥", 100, 50, target = { level -> if (level < 5) 4 else if (level < 15) 5 else 6 }) { n ->
            "Будь активен $n ${plural(n, "день", "дня", "дней")} из 7"
        },
        Template(ChallengeMetric.XP, "💫", 120, 60, target = { level -> roundTo(300 + 40 * level, 50) }) { n -> "Набери $n XP за неделю" },
        Template(ChallengeMetric.QUESTS, "🗡️", 100, 50, target = { level -> (12 + 2 * level).coerceAtMost(40) }) { n ->
            "Выполни $n ${plural(n, "квест", "квеста", "квестов")} за неделю"
        },
        Template(ChallengeMetric.PAGES, "📚", 100, 50, eligible = { it.hasBooks }, target = { level -> if (level < 10) 80 else 150 }) { n ->
            "Прочитай $n страниц за неделю"
        },
        Template(ChallengeMetric.PERFECT_DAYS, "🌟", 120, 60, target = { level -> if (level < 10) 1 else 2 }) { n ->
            "Проведи $n ${plural(n, "идеальный день", "идеальных дня", "идеальных дней")}"
        },
        Template(ChallengeMetric.ATTRIBUTES, "🎨", 150, 75, target = { Attribute.entries.size }) { "Прокачай все 6 характеристик за неделю" },
    )

    fun daily(day: Long, profile: HeroProfile): List<Challenge> =
        pick(daily, seed = day * 31 + 7, count = DAILY_COUNT, profile = profile, period = ChallengePeriod.DAILY, periodStart = day)

    fun weekly(day: Long, profile: HeroProfile): List<Challenge> {
        val start = weekStart(day)
        return pick(weekly, seed = start * 17 + 3, count = WEEKLY_COUNT, profile = profile, period = ChallengePeriod.WEEKLY, periodStart = start)
    }

    /** Понедельник недели, в которую попадает [day]. */
    fun weekStart(day: Long): Long = day - (LocalDate.ofEpochDay(day).dayOfWeek.value - DayOfWeek.MONDAY.value)

    fun weekEnd(day: Long): Long = weekStart(day) + 6

    private fun pick(
        pool: List<Template>,
        seed: Long,
        count: Int,
        profile: HeroProfile,
        period: ChallengePeriod,
        periodStart: Long,
    ): List<Challenge> {
        // Перемешиваем весь пул, а уже потом отбрасываем недоступные шаблоны: так появление
        // первой книги или цели не перетасовывает набор, а лишь добавляет варианты.
        val order = shuffled(pool.indices.toList(), seed)
        return order.map { pool[it] }.filter { it.eligible(profile) }.take(count).map { template ->
            val target = template.target(profile.level).coerceAtLeast(1)
            Challenge(
                id = "${period.name.first()}:$periodStart:${template.metric.name}",
                period = period,
                metric = template.metric,
                target = target,
                emoji = template.emoji,
                title = template.title(target),
                xp = template.xp,
                gold = template.gold,
                periodStart = periodStart,
            )
        }
    }

    /** Детерминированная перестановка (линейный конгруэнтный генератор). */
    private fun shuffled(items: List<Int>, seed: Long): List<Int> {
        val result = items.toMutableList()
        var state = seed xor 0x5DEECE66DL
        for (i in result.size - 1 downTo 1) {
            state = (state * 6364136223846793005L + 1442695040888963407L)
            val j = Math.floorMod(state ushr 33, (i + 1).toLong()).toInt()
            val tmp = result[i]
            result[i] = result[j]
            result[j] = tmp
        }
        return result
    }

    private fun roundTo(value: Int, step: Int): Int = ((value + step / 2) / step) * step

    private fun plural(n: Int, one: String, few: String, many: String): String {
        val mod10 = n % 10
        val mod100 = n % 100
        return when {
            mod10 == 1 && mod100 != 11 -> one
            mod10 in 2..4 && mod100 !in 12..14 -> few
            else -> many
        }
    }
}
