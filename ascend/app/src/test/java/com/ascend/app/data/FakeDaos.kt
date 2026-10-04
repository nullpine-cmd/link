package com.ascend.app.data

import com.ascend.app.data.local.AchievementDao
import com.ascend.app.data.local.AchievementEntity
import com.ascend.app.data.local.AttributeXp
import com.ascend.app.data.local.BookDao
import com.ascend.app.data.local.BookEntity
import com.ascend.app.data.local.DayActivity
import com.ascend.app.data.local.HeroDao
import com.ascend.app.data.local.HeroEntity
import com.ascend.app.data.local.LogCounters
import com.ascend.app.data.local.LogDao
import com.ascend.app.data.local.LogEntity
import com.ascend.app.data.local.LogKind
import com.ascend.app.data.local.QuestDao
import com.ascend.app.data.local.QuestEntity
import com.ascend.app.data.local.ShopDao
import com.ascend.app.data.local.ShopItemEntity
import com.ascend.core.Units
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Хранилище в памяти, повторяющее семантику SQL-запросов из DAO. */
class FakeStore {
    val hero = MutableStateFlow<HeroEntity?>(null)
    val quests = MutableStateFlow<List<QuestEntity>>(emptyList())
    val books = MutableStateFlow<List<BookEntity>>(emptyList())
    val logs = MutableStateFlow<List<LogEntity>>(emptyList())
    val shop = MutableStateFlow<List<ShopItemEntity>>(emptyList())
    val achievements = MutableStateFlow<List<AchievementEntity>>(emptyList())
    private var nextId = 1L

    fun id(): Long = nextId++
}

class FakeTime(var date: LocalDate = LocalDate.of(2026, 10, 5), var hourOfDay: Int = 12) : TimeProvider {
    override fun now(): Long = date.atTime(hourOfDay, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
    override fun zone(): ZoneId = ZoneOffset.UTC

    fun nextDay(days: Long = 1) {
        date = date.plusDays(days)
    }

    fun epochOf(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
}

object DirectTransactions : TransactionRunner {
    override suspend fun <R> transaction(block: suspend () -> R): R = block()
}

class FakeHeroDao(private val store: FakeStore) : HeroDao {
    override fun observe(): Flow<HeroEntity?> = store.hero
    override suspend fun get(): HeroEntity? = store.hero.value
    override suspend fun upsert(hero: HeroEntity) {
        store.hero.value = hero
    }
    override suspend fun clear() {
        store.hero.value = null
    }
}

class FakeQuestDao(private val store: FakeStore) : QuestDao {
    private fun sorted(list: List<QuestEntity>) =
        list.sortedWith(compareBy<QuestEntity>({ it.timeMinutes == null }, { it.timeMinutes ?: 0 }, { it.id }))

    override fun observeAll(): Flow<List<QuestEntity>> = store.quests.map(::sorted)
    override suspend fun getAll(): List<QuestEntity> = sorted(store.quests.value)
    override suspend fun get(id: Long): QuestEntity? = store.quests.value.firstOrNull { it.id == id }
    override fun observeForBook(bookId: Long): Flow<List<QuestEntity>> =
        store.quests.map { list -> list.filter { it.bookId == bookId && !it.archived } }

    override suspend fun insert(quest: QuestEntity): Long {
        val id = store.id()
        store.quests.value = store.quests.value + quest.copy(id = id)
        return id
    }

    override suspend fun update(quest: QuestEntity) {
        store.quests.value = store.quests.value.map { if (it.id == quest.id) quest else it }
    }

    override suspend fun delete(id: Long) {
        store.quests.value = store.quests.value.filterNot { it.id == id }
    }

    override suspend fun detachBook(bookId: Long) {
        store.quests.value = store.quests.value.map { if (it.bookId == bookId) it.copy(bookId = null) else it }
    }

    override suspend fun clear() {
        store.quests.value = emptyList()
    }
}

class FakeBookDao(private val store: FakeStore) : BookDao {
    override fun observeAll(): Flow<List<BookEntity>> = store.books
    override fun observe(id: Long): Flow<BookEntity?> = store.books.map { list -> list.firstOrNull { it.id == id } }
    override suspend fun get(id: Long): BookEntity? = store.books.value.firstOrNull { it.id == id }

    override suspend fun insert(book: BookEntity): Long {
        val id = store.id()
        store.books.value = store.books.value + book.copy(id = id)
        return id
    }

    override suspend fun update(book: BookEntity) {
        store.books.value = store.books.value.map { if (it.id == book.id) book else it }
    }

    override suspend fun delete(id: Long) {
        store.books.value = store.books.value.filterNot { it.id == id }
    }

    override suspend fun finishedCount(): Long = store.books.value.count { it.finishedAt != null }.toLong()
    override fun observeFinishedCount(): Flow<Long> = store.books.map { list -> list.count { it.finishedAt != null }.toLong() }
    override suspend fun clear() {
        store.books.value = emptyList()
    }
}

class FakeLogDao(private val store: FakeStore) : LogDao {
    private val activeKinds = setOf(LogKind.QUEST, LogKind.READING)

    override suspend fun insert(log: LogEntity): Long {
        val id = store.id()
        store.logs.value = store.logs.value + log.copy(id = id)
        return id
    }

    override suspend fun get(id: Long): LogEntity? = store.logs.value.firstOrNull { it.id == id }
    override suspend fun delete(id: Long) {
        store.logs.value = store.logs.value.filterNot { it.id == id }
    }

    override fun observeDay(day: Long): Flow<List<LogEntity>> = store.logs.map { day(it, day) }
    override suspend fun getDay(day: Long): List<LogEntity> = day(store.logs.value, day)
    private fun day(list: List<LogEntity>, day: Long) = list.filter { it.day == day }.sortedBy { it.timestamp }

    override fun observeForBook(bookId: Long): Flow<List<LogEntity>> = store.logs.map { list ->
        list.filter { it.bookId == bookId && it.kind in activeKinds }.sortedByDescending { it.timestamp }
    }

    override fun observeRecent(limit: Int): Flow<List<LogEntity>> =
        store.logs.map { list -> list.sortedByDescending { it.timestamp }.take(limit) }

    override fun observeActiveDays(): Flow<List<Long>> = store.logs.map(::activeDays)
    override suspend fun activeDays(): List<Long> = activeDays(store.logs.value)
    private fun activeDays(list: List<LogEntity>) =
        list.filter { it.kind in activeKinds }.map { it.day }.distinct().sortedDescending()

    override suspend fun questDays(questId: Long): List<Long> =
        store.logs.value.filter { it.questId == questId && it.kind == LogKind.QUEST }.map { it.day }.sorted()

    override fun observeTotalXp(): Flow<Long> = store.logs.map { list -> list.sumOf { it.xp.toLong() } }
    override suspend fun totalXp(): Long = store.logs.value.sumOf { it.xp.toLong() }
    override fun observeGold(): Flow<Long> = store.logs.map { list -> list.sumOf { it.gold.toLong() } }
    override suspend fun gold(): Long = store.logs.value.sumOf { it.gold.toLong() }

    override fun observeAttributeXp(): Flow<List<AttributeXp>> = store.logs.map(::attributeXp)
    override suspend fun attributeXp(): List<AttributeXp> = attributeXp(store.logs.value)
    private fun attributeXp(list: List<LogEntity>) = list
        .filter { it.attribute != null && it.kind in setOf(LogKind.QUEST, LogKind.READING, LogKind.BOOK_FINISHED) }
        .groupBy { it.attribute!! }
        .map { (attribute, logs) -> AttributeXp(attribute, logs.sumOf { it.xp.toLong() }) }

    override fun observeCounters(): Flow<LogCounters> = store.logs.map(::counters)
    override suspend fun counters(): LogCounters = counters(store.logs.value)
    private fun counters(list: List<LogEntity>) = LogCounters(
        quests = list.count { it.kind == LogKind.QUEST }.toLong(),
        pages = list.filter { it.kind in activeKinds && (it.bookId != null || it.unit == Units.PAGES) }.sumOf { it.amount.toLong() },
        early = list.count { it.kind in activeKinds && it.hour < 7 }.toLong(),
        late = list.count { it.kind in activeKinds && it.hour >= 23 }.toLong(),
        overachieved = list.count { it.overachieved }.toLong(),
        comebacks = list.count { it.comeback }.toLong(),
        purchases = list.count { it.kind == LogKind.PURCHASE }.toLong(),
        perfectDays = list.count { it.kind == LogKind.PERFECT_DAY }.toLong(),
    )

    override fun observeActivity(from: Long, to: Long): Flow<List<DayActivity>> = store.logs.map { list ->
        list.filter { it.kind in activeKinds && it.day in from..to }
            .groupBy { it.day }
            .map { (day, logs) -> DayActivity(day, logs.size, logs.sumOf { it.xp.toLong() }) }
    }

    override suspend fun perfectDayCount(day: Long): Int =
        store.logs.value.count { it.kind == LogKind.PERFECT_DAY && it.day == day }

    override suspend fun deletePerfectDay(day: Long) {
        store.logs.value = store.logs.value.filterNot { it.kind == LogKind.PERFECT_DAY && it.day == day }
    }

    override suspend fun deleteBookFinished(bookId: Long) {
        store.logs.value = store.logs.value.filterNot { it.kind == LogKind.BOOK_FINISHED && it.bookId == bookId }
    }

    override suspend fun detachBook(bookId: Long) {
        store.logs.value = store.logs.value.map { if (it.bookId == bookId) it.copy(bookId = null) else it }
    }

    override suspend fun clear() {
        store.logs.value = emptyList()
    }
}

class FakeShopDao(private val store: FakeStore) : ShopDao {
    override fun observeAll(): Flow<List<ShopItemEntity>> = store.shop.map { list -> list.sortedWith(compareBy({ it.cost }, { it.id })) }
    override suspend fun get(id: Long): ShopItemEntity? = store.shop.value.firstOrNull { it.id == id }
    override suspend fun insert(item: ShopItemEntity): Long {
        val id = store.id()
        store.shop.value = store.shop.value + item.copy(id = id)
        return id
    }

    override suspend fun update(item: ShopItemEntity) {
        store.shop.value = store.shop.value.map { if (it.id == item.id) item else it }
    }

    override suspend fun delete(id: Long) {
        store.shop.value = store.shop.value.filterNot { it.id == id }
    }

    override suspend fun count(): Int = store.shop.value.size
    override suspend fun clear() {
        store.shop.value = emptyList()
    }
}

class FakeAchievementDao(private val store: FakeStore) : AchievementDao {
    override fun observeAll(): Flow<List<AchievementEntity>> = store.achievements
    override suspend fun ids(): List<String> = store.achievements.value.map { it.id }
    override suspend fun insert(entity: AchievementEntity) {
        if (store.achievements.value.none { it.id == entity.id }) {
            store.achievements.value = store.achievements.value + entity
        }
    }

    override suspend fun clear() {
        store.achievements.value = emptyList()
    }
}

fun fakeRepository(store: FakeStore = FakeStore(), time: FakeTime = FakeTime()): GameRepository = GameRepository(
    heroDao = FakeHeroDao(store),
    questDao = FakeQuestDao(store),
    bookDao = FakeBookDao(store),
    logDao = FakeLogDao(store),
    shopDao = FakeShopDao(store),
    achievementDao = FakeAchievementDao(store),
    tx = DirectTransactions,
    time = time,
)
