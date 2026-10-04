package com.ascend.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.ascend.core.Attribute
import kotlinx.coroutines.flow.Flow

@Dao
interface HeroDao {
    @Query("SELECT * FROM hero WHERE id = 1")
    fun observe(): Flow<HeroEntity?>

    @Query("SELECT * FROM hero WHERE id = 1")
    suspend fun get(): HeroEntity?

    @Upsert
    suspend fun upsert(hero: HeroEntity)

    @Query("DELETE FROM hero")
    suspend fun clear()
}

@Dao
interface QuestDao {
    @Query("SELECT * FROM quests ORDER BY CASE WHEN timeMinutes IS NULL THEN 1 ELSE 0 END, timeMinutes, id")
    fun observeAll(): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quests ORDER BY CASE WHEN timeMinutes IS NULL THEN 1 ELSE 0 END, timeMinutes, id")
    suspend fun getAll(): List<QuestEntity>

    @Query("SELECT * FROM quests WHERE id = :id")
    suspend fun get(id: Long): QuestEntity?

    @Query("SELECT * FROM quests WHERE bookId = :bookId AND archived = 0")
    fun observeForBook(bookId: Long): Flow<List<QuestEntity>>

    @Insert
    suspend fun insert(quest: QuestEntity): Long

    @Update
    suspend fun update(quest: QuestEntity)

    @Query("DELETE FROM quests WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE quests SET bookId = NULL WHERE bookId = :bookId")
    suspend fun detachBook(bookId: Long)

    @Query("DELETE FROM quests")
    suspend fun clear()
}

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY CASE WHEN finishedAt IS NULL THEN 0 ELSE 1 END, createdAt DESC")
    fun observeAll(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    fun observe(id: Long): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun get(id: Long): BookEntity?

    @Insert
    suspend fun insert(book: BookEntity): Long

    @Update
    suspend fun update(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM books WHERE finishedAt IS NOT NULL")
    suspend fun finishedCount(): Long

    @Query("SELECT COUNT(*) FROM books WHERE finishedAt IS NOT NULL")
    fun observeFinishedCount(): Flow<Long>

    @Query("DELETE FROM books")
    suspend fun clear()
}

data class AttributeXp(val attribute: Attribute, val xp: Long)

data class DayActivity(val day: Long, val count: Int, val xp: Long)

data class LogCounters(
    val quests: Long,
    val pages: Long,
    val early: Long,
    val late: Long,
    val overachieved: Long,
    val comebacks: Long,
    val purchases: Long,
    val perfectDays: Long,
)

private const val COUNTERS_QUERY = """
    SELECT
        (SELECT COUNT(*) FROM activity_log WHERE kind = 'QUEST') AS quests,
        (SELECT COALESCE(SUM(amount), 0) FROM activity_log
            WHERE kind IN ('QUEST', 'READING') AND (bookId IS NOT NULL OR unit = 'стр.')) AS pages,
        (SELECT COUNT(*) FROM activity_log WHERE kind IN ('QUEST', 'READING') AND hour < 7) AS early,
        (SELECT COUNT(*) FROM activity_log WHERE kind IN ('QUEST', 'READING') AND hour >= 23) AS late,
        (SELECT COUNT(*) FROM activity_log WHERE overachieved = 1) AS overachieved,
        (SELECT COUNT(*) FROM activity_log WHERE comeback = 1) AS comebacks,
        (SELECT COUNT(*) FROM activity_log WHERE kind = 'PURCHASE') AS purchases,
        (SELECT COUNT(*) FROM activity_log WHERE kind = 'PERFECT_DAY') AS perfectDays
"""

private const val ATTRIBUTE_XP_QUERY = """
    SELECT attribute, SUM(xp) AS xp FROM activity_log
    WHERE attribute IS NOT NULL AND kind IN ('QUEST', 'READING', 'BOOK_FINISHED')
    GROUP BY attribute
"""

private const val ACTIVE_DAYS_QUERY =
    "SELECT DISTINCT day FROM activity_log WHERE kind IN ('QUEST', 'READING') ORDER BY day DESC"

@Dao
interface LogDao {
    @Insert
    suspend fun insert(log: LogEntity): Long

    @Query("SELECT * FROM activity_log WHERE id = :id")
    suspend fun get(id: Long): LogEntity?

    @Query("DELETE FROM activity_log WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM activity_log WHERE day = :day ORDER BY timestamp")
    fun observeDay(day: Long): Flow<List<LogEntity>>

    @Query("SELECT * FROM activity_log WHERE day = :day ORDER BY timestamp")
    suspend fun getDay(day: Long): List<LogEntity>

    @Query("SELECT * FROM activity_log WHERE bookId = :bookId AND kind IN ('QUEST', 'READING') ORDER BY timestamp DESC")
    fun observeForBook(bookId: Long): Flow<List<LogEntity>>

    @Query("SELECT * FROM activity_log ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<LogEntity>>

    @Query(ACTIVE_DAYS_QUERY)
    fun observeActiveDays(): Flow<List<Long>>

    @Query(ACTIVE_DAYS_QUERY)
    suspend fun activeDays(): List<Long>

    @Query("SELECT day FROM activity_log WHERE questId = :questId AND kind = 'QUEST' ORDER BY day")
    suspend fun questDays(questId: Long): List<Long>

    @Query("SELECT COALESCE(SUM(xp), 0) FROM activity_log")
    fun observeTotalXp(): Flow<Long>

    @Query("SELECT COALESCE(SUM(xp), 0) FROM activity_log")
    suspend fun totalXp(): Long

    @Query("SELECT COALESCE(SUM(gold), 0) FROM activity_log")
    fun observeGold(): Flow<Long>

    @Query("SELECT COALESCE(SUM(gold), 0) FROM activity_log")
    suspend fun gold(): Long

    @Query(ATTRIBUTE_XP_QUERY)
    fun observeAttributeXp(): Flow<List<AttributeXp>>

    @Query(ATTRIBUTE_XP_QUERY)
    suspend fun attributeXp(): List<AttributeXp>

    @Query(COUNTERS_QUERY)
    fun observeCounters(): Flow<LogCounters>

    @Query(COUNTERS_QUERY)
    suspend fun counters(): LogCounters

    @Query(
        """
        SELECT day, COUNT(*) AS count, COALESCE(SUM(xp), 0) AS xp FROM activity_log
        WHERE kind IN ('QUEST', 'READING') AND day BETWEEN :from AND :to
        GROUP BY day
        """,
    )
    fun observeActivity(from: Long, to: Long): Flow<List<DayActivity>>

    @Query("SELECT COUNT(*) FROM activity_log WHERE kind = 'PERFECT_DAY' AND day = :day")
    suspend fun perfectDayCount(day: Long): Int

    @Query("DELETE FROM activity_log WHERE kind = 'PERFECT_DAY' AND day = :day")
    suspend fun deletePerfectDay(day: Long)

    @Query("DELETE FROM activity_log WHERE kind = 'BOOK_FINISHED' AND bookId = :bookId")
    suspend fun deleteBookFinished(bookId: Long)

    @Query("UPDATE activity_log SET bookId = NULL WHERE bookId = :bookId")
    suspend fun detachBook(bookId: Long)

    @Query("DELETE FROM activity_log")
    suspend fun clear()
}

@Dao
interface ShopDao {
    @Query("SELECT * FROM shop_items ORDER BY cost, id")
    fun observeAll(): Flow<List<ShopItemEntity>>

    @Query("SELECT * FROM shop_items WHERE id = :id")
    suspend fun get(id: Long): ShopItemEntity?

    @Insert
    suspend fun insert(item: ShopItemEntity): Long

    @Update
    suspend fun update(item: ShopItemEntity)

    @Query("DELETE FROM shop_items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM shop_items")
    suspend fun count(): Int

    @Query("DELETE FROM shop_items")
    suspend fun clear()
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements ORDER BY unlockedAt DESC")
    fun observeAll(): Flow<List<AchievementEntity>>

    @Query("SELECT id FROM achievements")
    suspend fun ids(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: AchievementEntity)

    @Query("DELETE FROM achievements")
    suspend fun clear()
}
