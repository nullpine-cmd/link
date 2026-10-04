package com.ascend.app.data

import com.ascend.app.data.local.LogKind
import com.ascend.app.domain.BookDraft
import com.ascend.app.domain.PurchaseResult
import com.ascend.app.domain.QuestDraft
import com.ascend.core.Achievement
import com.ascend.core.Attribute
import com.ascend.core.BonusKind
import com.ascend.core.Difficulty
import com.ascend.core.QuestKind
import com.ascend.core.Units
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRepositoryTest {

    private val store = FakeStore()
    private val time = FakeTime()
    private val repository = fakeRepository(store, time)

    private fun habit(
        title: String = "Читать",
        attribute: Attribute = Attribute.INTELLECT,
        difficulty: Difficulty = Difficulty.NORMAL,
        target: Int? = null,
        unit: String? = null,
        bookId: Long? = null,
    ) = QuestDraft(
        title = title,
        emoji = "📖",
        kind = QuestKind.HABIT,
        attribute = attribute,
        difficulty = difficulty,
        target = target,
        unit = unit,
        bookId = bookId,
    )

    private fun runTest(block: suspend () -> Unit) = runBlocking {
        repository.createHero("Тестовый герой", "🦊", 0)
        block()
    }

    @Test
    fun `one page out of twenty brings the full base reward`() = runTest {
        val id = repository.saveQuest(habit(target = 20, unit = Units.PAGES))
        val outcome = repository.completeQuest(id, 1)!!

        assertEquals(20, outcome.baseXp)
        assertEquals(listOf(BonusKind.FIRST_OF_DAY), outcome.bonuses.map { it.kind })
        assertEquals(25, outcome.xp)
        assertEquals(listOf(Achievement.FIRST_STEP), outcome.achievements)

        val hero = repository.heroState.first()!!
        assertEquals(25L, hero.attributeXp[Attribute.INTELLECT])
        assertEquals(10L + Achievement.FIRST_STEP.tier.gold, hero.gold)
        assertEquals(1, hero.streak.current)
    }

    @Test
    fun `push-ups and reading are rewarded equally`() {
        val strength = runBlocking {
            val repo = fakeRepository()
            repo.createHero("А", "🐺", 0)
            val id = repo.saveQuest(habit("Отжимания", Attribute.STRENGTH, target = 30, unit = Units.TIMES))
            repo.completeQuest(id, 30)!!
        }
        val reading = runBlocking {
            val repo = fakeRepository()
            repo.createHero("Б", "🦉", 0)
            val id = repo.saveQuest(habit("Чтение", Attribute.INTELLECT, target = 20, unit = Units.PAGES))
            repo.completeQuest(id, 20)!!
        }
        assertEquals(strength.xp, reading.xp)
        assertEquals(strength.gold, reading.gold)
        assertEquals(Attribute.STRENGTH, strength.attribute)
        assertEquals(Attribute.INTELLECT, reading.attribute)
    }

    @Test
    fun `habit is completed once per day`() = runTest {
        val id = repository.saveQuest(habit())
        assertNotNull(repository.completeQuest(id, 1))
        assertNull(repository.completeQuest(id, 1))
        assertEquals(1, store.logs.value.count { it.kind == LogKind.QUEST })
        time.nextDay()
        assertNotNull(repository.completeQuest(id, 1))
    }

    @Test
    fun `undo restores quest, book and experience`() = runTest {
        val bookId = repository.saveBook(BookDraft(title = "Книга", author = "Автор", totalPages = 100, currentPage = 10, palette = 0))
        val questId = repository.saveQuest(habit(target = 20, unit = Units.PAGES, bookId = bookId))
        val outcome = repository.completeQuest(questId, 20)!!
        assertEquals(30, store.books.value.single().currentPage)

        repository.undo(outcome.logId)

        assertEquals(10, store.books.value.single().currentPage)
        val quest = repository.quest(questId)!!
        assertNull(quest.lastCompletedDay)
        assertEquals(0, quest.totalCompletions)
        assertEquals(0, store.logs.value.count { it.kind == LogKind.QUEST })
        assertNotNull(repository.completeQuest(questId, 5))
    }

    @Test
    fun `streak grows day by day and a comeback is celebrated, not punished`() = runTest {
        val id = repository.saveQuest(habit(difficulty = Difficulty.EASY))
        assertEquals(15, repository.completeQuest(id, 1)!!.xp)

        time.nextDay()
        val second = repository.completeQuest(id, 1)!!
        assertEquals(2, second.streak)
        assertTrue(second.bonuses.any { it.kind == BonusKind.STREAK })

        time.nextDay(4)
        val comeback = repository.completeQuest(id, 1)!!
        assertEquals(1, comeback.streak)
        assertTrue(comeback.bonuses.any { it.kind == BonusKind.COMEBACK })
        assertTrue(Achievement.RETURN_OF_THE_HERO in comeback.achievements)
        assertTrue(comeback.xp > Difficulty.EASY.baseXp)
    }

    @Test
    fun `reading the last pages finishes the book`() = runTest {
        val bookId = repository.saveBook(
            BookDraft(title = "Мастер и Маргарита", author = "Булгаков", totalPages = 30, currentPage = 0, palette = 2, dailyPages = 20),
        )
        val quest = store.quests.value.single()
        assertEquals(bookId, quest.bookId)

        val outcome = repository.logReading(bookId, 30)!!
        assertEquals(quest.title, outcome.title)
        assertTrue(outcome.bonuses.any { it.kind == BonusKind.TARGET })
        assertEquals("Мастер и Маргарита", outcome.finishedBook)
        assertTrue(Achievement.LAST_PAGE in outcome.achievements)
        assertNotNull(store.books.value.single().finishedAt)
        assertEquals(1, store.logs.value.count { it.kind == LogKind.BOOK_FINISHED })

        val extra = repository.logReading(bookId, 5)!!
        assertEquals("Чтение: Мастер и Маргарита", extra.title)
        assertEquals(1, store.logs.value.count { it.kind == LogKind.READING })
    }

    @Test
    fun `completing every quest of the day makes a perfect day`() = runTest {
        val today = time.today()
        val ids = (1..3).map { index ->
            repository.saveQuest(
                QuestDraft(
                    title = "Задача $index",
                    emoji = "✅",
                    kind = QuestKind.TASK,
                    attribute = Attribute.MASTERY,
                    difficulty = Difficulty.EASY,
                    dueDay = today,
                ),
            )
        }
        assertFalse(repository.completeQuest(ids[0], 1)!!.perfectDay)
        assertFalse(repository.completeQuest(ids[1], 1)!!.perfectDay)
        val last = repository.completeQuest(ids[2], 1)!!
        assertTrue(last.perfectDay)
        assertTrue(Achievement.PERFECT_DAY in last.achievements)
        assertNull(repository.completeQuest(ids[2], 1))
    }

    @Test
    fun `shop spends earned gold`() = runTest {
        repository.saveShopItem(0, "Кофе", "☕", 10)
        val coffee = store.shop.value.first { it.title == "Кофе" }
        val tooExpensive = store.shop.value.maxBy { it.cost }

        val id = repository.saveQuest(habit(difficulty = Difficulty.EASY))
        repository.completeQuest(id, 1)

        assertTrue(repository.buy(tooExpensive.id) is PurchaseResult.NotEnoughGold)
        val result = repository.buy(coffee.id)
        assertTrue(result is PurchaseResult.Success)
        assertTrue(Achievement.TREAT_YOURSELF in (result as PurchaseResult.Success).achievements)
        val gold = repository.heroState.first()!!.gold
        assertEquals(6L + Achievement.FIRST_STEP.tier.gold - 10 + Achievement.TREAT_YOURSELF.tier.gold, gold)
    }

    @Test
    fun `overdue tasks move to today and past misses stay hidden`() = runTest {
        val today = time.today()
        repository.saveQuest(
            QuestDraft(
                title = "Позвонить",
                emoji = "📞",
                kind = QuestKind.TASK,
                attribute = Attribute.CHARISMA,
                difficulty = Difficulty.EASY,
                dueDay = today - 1,
            ),
        )
        val todayPlan = repository.dayPlan(today).first()
        assertTrue(todayPlan.items.single().overdue)
        val yesterday = repository.dayPlan(today - 1).first()
        assertTrue(yesterday.items.isEmpty())
    }

    @Test
    fun `archived habits leave the plan, deleting keeps experience`() = runTest {
        val id = repository.saveQuest(habit())
        repository.completeQuest(id, 1)
        val xpBefore = repository.heroState.first()!!.level.totalXp
        time.nextDay()
        repository.setArchived(id, true)
        assertTrue(repository.dayPlan(time.today()).first().items.isEmpty())
        repository.deleteQuest(id)
        assertEquals(xpBefore, repository.heroState.first()!!.level.totalXp)
    }

    @Test
    fun `reset returns to a fresh start`() = runTest {
        val id = repository.saveQuest(habit())
        repository.completeQuest(id, 1)
        repository.resetProgress()
        assertFalse(repository.hasHero.first())
        assertTrue(store.logs.value.isEmpty())
    }
}
