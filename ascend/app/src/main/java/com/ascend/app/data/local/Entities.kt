package com.ascend.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ascend.core.Attribute
import com.ascend.core.Difficulty
import com.ascend.core.QuestKind
import com.ascend.core.WeekSchedule

const val HERO_ID = 1

@Entity(tableName = "hero")
data class HeroEntity(
    @PrimaryKey val id: Int = HERO_ID,
    val name: String,
    val avatar: String,
    val aura: Int,
    val createdAt: Long,
    val focusText: String? = null,
    val focusDay: Long? = null,
    val reminderEnabled: Boolean = false,
    val reminderMinutes: Int = 20 * 60,
)

@Entity(tableName = "quests", indices = [Index("bookId")])
data class QuestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val emoji: String,
    val kind: QuestKind,
    val attribute: Attribute,
    val difficulty: Difficulty,
    val targetAmount: Int? = null,
    val unit: String? = null,
    val scheduleMask: Int = WeekSchedule.EVERY_DAY.mask,
    val dueDay: Long? = null,
    val timeMinutes: Int? = null,
    val bookId: Long? = null,
    val createdAt: Long,
    val createdDay: Long,
    val archived: Boolean = false,
    val completedAt: Long? = null,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val lastCompletedDay: Long? = null,
    val totalCompletions: Int = 0,
)

val QuestEntity.schedule: WeekSchedule get() = WeekSchedule(scheduleMask)
val QuestEntity.isHabit: Boolean get() = kind == QuestKind.HABIT
val QuestEntity.hasTarget: Boolean get() = (targetAmount ?: 0) > 0

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String,
    val totalPages: Int,
    val currentPage: Int = 0,
    val palette: Int,
    val createdAt: Long,
    val startedAt: Long? = null,
    val finishedAt: Long? = null,
)

val BookEntity.progress: Float
    get() = if (totalPages <= 0) 0f else (currentPage.toFloat() / totalPages).coerceIn(0f, 1f)
val BookEntity.isFinished: Boolean get() = finishedAt != null
val BookEntity.pagesLeft: Int get() = (totalPages - currentPage).coerceAtLeast(0)

/** Журнал — единственный источник правды об опыте и золоте: всё остальное выводится из него. */
enum class LogKind { QUEST, READING, BOOK_FINISHED, PERFECT_DAY, ACHIEVEMENT, PURCHASE }

@Entity(
    tableName = "activity_log",
    indices = [Index("day"), Index("questId"), Index("bookId")],
)
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: LogKind,
    val questId: Long? = null,
    val bookId: Long? = null,
    val shopItemId: Long? = null,
    val title: String,
    val emoji: String,
    val attribute: Attribute? = null,
    val unit: String? = null,
    val day: Long,
    val timestamp: Long,
    val hour: Int,
    val amount: Int = 0,
    val xp: Int = 0,
    val gold: Int = 0,
    val hitTarget: Boolean = false,
    val overachieved: Boolean = false,
    val comeback: Boolean = false,
    val bookPageBefore: Int? = null,
)

@Entity(tableName = "shop_items")
data class ShopItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val emoji: String,
    val cost: Int,
    val timesBought: Int = 0,
    val createdAt: Long,
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val unlockedAt: Long,
)
