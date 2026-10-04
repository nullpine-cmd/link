package com.ascend.app.domain

import com.ascend.app.data.local.LogEntity
import com.ascend.app.data.local.QuestEntity
import com.ascend.core.Achievement
import com.ascend.core.Attribute
import com.ascend.core.Bonus
import com.ascend.core.HeroStats
import com.ascend.core.HeroTitle
import com.ascend.core.LevelInfo
import com.ascend.core.Rank
import com.ascend.core.StreakInfo

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
) {
    fun attribute(attribute: Attribute): LevelInfo = attributes.getValue(attribute)
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
) {
    val leveledUp: Boolean get() = levelAfter.level > levelBefore.level
    val rankBefore: Rank get() = Rank.forLevel(levelBefore.level)
    val rankAfter: Rank get() = Rank.forLevel(levelAfter.level)
    val rankUp: Boolean get() = rankAfter != rankBefore
    val attributeLeveledUp: Boolean get() = attributeAfter.level > attributeBefore.level
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

/** События для глобального слоя празднований. */
sealed interface Celebration {
    data class Completed(val outcome: RewardOutcome) : Celebration
    data class Purchased(val result: PurchaseResult.Success) : Celebration
}

data class HeroStatistics(
    val stats: HeroStats,
    val activeDays: Int,
    val totalXp: Long,
    val unlocked: Map<Achievement, Long>,
)
