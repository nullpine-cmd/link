package com.ascend.app.domain

import com.ascend.app.data.local.LogEntity
import com.ascend.app.data.local.QuestEntity
import com.ascend.core.Achievement
import com.ascend.core.Attribute
import com.ascend.core.Bonus
import com.ascend.core.Boss
import com.ascend.core.BossFight
import com.ascend.core.Challenge
import com.ascend.core.ChallengeProgress
import com.ascend.core.HeroStats
import com.ascend.core.HeroTitle
import com.ascend.core.LevelInfo
import com.ascend.core.Rank
import com.ascend.core.StreakInfo
import com.ascend.core.Talent

data class HeroState(
    val name: String,
    val avatar: String,
    val aura: Int,
    val level: LevelInfo,
    val rank: Rank,
    val gold: Long,
    val attributeXp: Map<Attribute, Long>,
    val attributes: Map<Attribute, LevelInfo>,
    val title: HeroTitle,
    val streak: StreakInfo,
    val focus: String?,
    val createdAt: Long,
    val reminderEnabled: Boolean,
    val reminderMinutes: Int,
    val soundEnabled: Boolean = true,
    val reduceMotion: Boolean = false,
    val talents: Set<Talent> = emptySet(),
) {
    fun attribute(attribute: Attribute): LevelInfo = attributes.getValue(attribute)

    /** Самая слабая характеристика — ту, что прокачана меньше всех (при равенстве — первая по порядку). */
    val weakestAttribute: Attribute get() = Attribute.entries.minBy { attributeXp[it] ?: 0L }
}

enum class PlanStatus { DONE, PENDING, PLANNED }

data class PlanItem(
    val quest: QuestEntity,
    val log: LogEntity?,
    val status: PlanStatus,
    val overdue: Boolean,
    val visibleStreak: Int,
)

data class DayPlan(
    val day: Long,
    val today: Long,
    val items: List<PlanItem>,
) {
    val done: Int get() = items.count { it.status == PlanStatus.DONE }
    val total: Int get() = items.size
    val isToday: Boolean get() = day == today
    val isPast: Boolean get() = day < today
    val isFuture: Boolean get() = day > today
    val progress: Float get() = if (total == 0) 0f else done.toFloat() / total
}

/** Доска испытаний: прогресс по дневным и недельным, плюс уже полученные награды. */
data class ChallengeBoard(
    val daily: List<ChallengeProgress>,
    val weekly: List<ChallengeProgress>,
    val claimed: Set<String>,
    val weekStart: Long,
) {
    fun isClaimed(challenge: Challenge): Boolean = challenge.id in claimed
    val dailyDone: Int get() = daily.count { isClaimed(it.challenge) }
    val weeklyDone: Int get() = weekly.count { isClaimed(it.challenge) }

    companion object {
        val EMPTY = ChallengeBoard(emptyList(), emptyList(), emptySet(), 0)
    }
}

/** Босс недели и ход сражения. */
data class BossState(
    val fight: BossFight,
    val defeatedAt: Long?,
    val daysLeft: Int,
) {
    val boss: Boss get() = fight.boss
    val defeated: Boolean get() = defeatedAt != null || fight.defeated
}

/** Активная сессия фокуса в терминах интерфейса. */
data class FocusSession(
    val questId: Long?,
    val title: String,
    val emoji: String,
    val startedAt: Long,
    val targetMinutes: Int,
    val pausedAt: Long?,
    val pausedTotal: Long,
) {
    val paused: Boolean get() = pausedAt != null

    fun elapsedMillis(now: Long): Long {
        val end = pausedAt ?: now
        return (end - startedAt - pausedTotal).coerceAtLeast(0)
    }

    fun elapsedMinutes(now: Long): Int = (elapsedMillis(now) / 60_000).toInt()

    fun remainingMillis(now: Long): Long = (targetMinutes * 60_000L - elapsedMillis(now)).coerceAtLeast(0)

    fun progress(now: Long): Float =
        if (targetMinutes <= 0) 1f else (elapsedMillis(now).toFloat() / (targetMinutes * 60_000L)).coerceIn(0f, 1f)
}

/** Итог выполнения: всё, что нужно для праздничной анимации. */
data class RewardOutcome(
    val logId: Long,
    val title: String,
    val emoji: String,
    val attribute: Attribute,
    val xp: Int,
    val gold: Int,
    val baseXp: Int,
    val bonuses: List<Bonus>,
    val amount: Int,
    val unit: String?,
    val target: Int?,
    val streak: Int,
    val message: String,
    val levelBefore: LevelInfo,
    val levelAfter: LevelInfo,
    val attributeBefore: LevelInfo,
    val attributeAfter: LevelInfo,
    val achievements: List<Achievement>,
    val finishedBook: String?,
    val perfectDay: Boolean,
    val comboStep: Int = 0,
    val challenges: List<Challenge> = emptyList(),
    val bossDefeated: Boss? = null,
    val bossHit: BossFight? = null,
) {
    val leveledUp: Boolean get() = levelAfter.level > levelBefore.level
    val rankBefore: Rank get() = Rank.forLevel(levelBefore.level)
    val rankAfter: Rank get() = Rank.forLevel(levelAfter.level)
    val rankUp: Boolean get() = rankAfter != rankBefore
    val attributeLeveledUp: Boolean get() = attributeAfter.level > attributeBefore.level
    /** Комбо для показа: ×2 начинается со второго выполнения подряд. */
    val comboMultiplier: Int get() = comboStep + 1
}

sealed interface PurchaseResult {
    data class Success(
        val title: String,
        val emoji: String,
        val cost: Int,
        val achievements: List<Achievement>,
        val levelBefore: LevelInfo,
        val levelAfter: LevelInfo,
    ) : PurchaseResult

    data class NotEnoughGold(val missing: Long) : PurchaseResult
    data object NotFound : PurchaseResult
}

sealed interface TalentResult {
    data class Unlocked(val talent: Talent, val achievements: List<Achievement>) : TalentResult
    data class Refused(val reason: com.ascend.core.TalentPurchase) : TalentResult
}

/** События для глобального слоя празднований. */
sealed interface Celebration {
    data class Completed(val outcome: RewardOutcome) : Celebration
    data class Purchased(val result: PurchaseResult.Success) : Celebration
    data class TalentUnlocked(val result: TalentResult.Unlocked) : Celebration
}

data class HeroStatistics(
    val stats: HeroStats,
    val activeDays: Int,
    val totalXp: Long,
    val unlocked: Map<Achievement, Long>,
)

/** Итоги недели для летописи. */
data class WeekRecap(
    val weekStart: Long,
    val xp: Long,
    val quests: Int,
    val activeDays: Int,
    val pages: Int,
    val bestDay: Long?,
    val bestDayXp: Long,
    val topAttribute: Attribute?,
    val challengesDone: Int,
    val bossDefeated: Boolean,
)
