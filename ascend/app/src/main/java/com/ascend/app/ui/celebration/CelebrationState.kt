package com.ascend.app.ui.celebration

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ascend.app.domain.Celebration
import com.ascend.app.domain.RewardOutcome
import com.ascend.core.Achievement
import com.ascend.core.LevelInfo
import com.ascend.core.Milestones

sealed interface CelebrationItem {
    val id: Long
    val durationMillis: Long

    data class Reward(override val id: Long, val outcome: RewardOutcome) : CelebrationItem {
        override val durationMillis: Long get() = 4_200
    }

    data class LevelUp(
        override val id: Long,
        val before: LevelInfo,
        val after: LevelInfo,
        val outcome: RewardOutcome?,
    ) : CelebrationItem {
        override val durationMillis: Long get() = Long.MAX_VALUE
    }

    data class Milestone(
        override val id: Long,
        val emoji: String,
        val title: String,
        val subtitle: String,
        val xp: Int,
        val gold: Int,
    ) : CelebrationItem {
        override val durationMillis: Long get() = 3_400
    }

    data class AchievementUnlocked(override val id: Long, val achievement: Achievement) : CelebrationItem {
        override val durationMillis: Long get() = 3_600
    }

    data class Purchase(override val id: Long, val emoji: String, val title: String, val cost: Int) : CelebrationItem {
        override val durationMillis: Long get() = 2_800
    }
}

/** Очередь празднований: тосты и церемонии показываются по одному, не перекрывая друг друга. */
@Stable
class CelebrationState {
    private val queue = ArrayDeque<CelebrationItem>()
    private var nextId = 1L

    var current: CelebrationItem? by mutableStateOf(null)
        private set

    fun enqueue(celebration: Celebration) {
        queue.addAll(itemsFor(celebration))
        if (current == null) advance()
    }

    fun dismiss(id: Long) {
        if (current?.id == id) advance()
    }

    private fun advance() {
        current = queue.removeFirstOrNull()
    }

    private fun itemsFor(celebration: Celebration): List<CelebrationItem> = buildList {
        when (celebration) {
            is Celebration.Completed -> {
                val outcome = celebration.outcome
                add(
                    if (outcome.leveledUp) {
                        CelebrationItem.LevelUp(nextId++, outcome.levelBefore, outcome.levelAfter, outcome)
                    } else {
                        CelebrationItem.Reward(nextId++, outcome)
                    },
                )
                outcome.finishedBook?.let { title ->
                    add(
                        CelebrationItem.Milestone(
                            nextId++,
                            "🏆",
                            "Книга прочитана!",
                            "«$title» покорена. Знание — сила!",
                            Milestones.BOOK_FINISHED_XP,
                            Milestones.BOOK_FINISHED_GOLD,
                        ),
                    )
                }
                if (outcome.perfectDay) {
                    add(
                        CelebrationItem.Milestone(
                            nextId++,
                            "🌟",
                            "Идеальный день!",
                            "Все квесты дня выполнены",
                            Milestones.PERFECT_DAY_XP,
                            Milestones.PERFECT_DAY_GOLD,
                        ),
                    )
                }
                outcome.achievements.forEach { add(CelebrationItem.AchievementUnlocked(nextId++, it)) }
            }
            is Celebration.Purchased -> {
                val result = celebration.result
                add(CelebrationItem.Purchase(nextId++, result.emoji, result.title, result.cost))
                if (result.levelAfter.level > result.levelBefore.level) {
                    add(CelebrationItem.LevelUp(nextId++, result.levelBefore, result.levelAfter, null))
                }
                result.achievements.forEach { add(CelebrationItem.AchievementUnlocked(nextId++, it)) }
            }
        }
    }
}
