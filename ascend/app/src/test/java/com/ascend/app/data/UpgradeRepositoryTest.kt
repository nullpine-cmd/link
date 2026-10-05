package com.ascend.app.data

import com.ascend.app.data.local.LogKind
import com.ascend.app.domain.BookDraft
import com.ascend.app.domain.QuestDraft
import com.ascend.app.domain.TalentResult
import com.ascend.core.Achievement
import com.ascend.core.Attribute
import com.ascend.core.BonusKind
import com.ascend.core.ChallengeMetric
import com.ascend.core.Challenges
import com.ascend.core.Difficulty
import com.ascend.core.QuestKind
import com.ascend.core.Talent
import com.ascend.core.TalentPurchase
import com.ascend.core.Units
import com.ascend.core.WeeklyBoss
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class UpgradeRepositoryTest {

    private val store = FakeStore()
    private val time = FakeTime(date = LocalDate.of(2026, 10, 5), hourOfDay = 12) // понедельник
    private val repository = fakeRepository(store, time)

    private fun task(title: String, difficulty: Difficulty = Difficulty.EASY, attribute: Attribute = Attribute.MASTERY, target: Int? = null, unit: String? = null) =
        QuestDraft(title = title, emoji = "✅", kind = QuestKind.TASK, attribute = attribute, difficulty = difficulty, target = target, unit = unit)

    private fun runTest(block: suspend () -> Unit) = runBlocking {
        repository.createHero("Герой", "🦊", 0)
        block()
    }

    private suspend fun completeNewTask(difficulty: Difficulty = Difficulty.EASY, attribute: Attribute = Attribute.MASTERY) =
        repository.completeQuest(repository.saveQuest(task("Дело ${store.logs.value.size}", difficulty, attribute)), 1)!!

    // region Комбо

    @Test
    fun `completions within half an hour build a combo`() = runTest {
        val first = completeNewTask()
        assertEquals(0, first.comboStep)
        time.advanceMinutes(10)
        val second = completeNewTask()
        assertEquals(1, second.comboStep)
        assertTrue(second.bonuses.any { it.kind == BonusKind.COMBO })
        time.advanceMinutes(45)
        val third = completeNewTask()
        assertEquals(0, third.comboStep)
        assertEquals(2L, repository.statistics.first().stats.bestCombo)
    }

    // endregion

    // region Таланты

    @Test
    fun `talent needs level and gold, then changes rewards`() = runTest {
        val refused = repository.unlockTalent(Talent.EARLY_BIRD)
        assertTrue(refused is TalentResult.Refused && refused.reason is TalentPurchase.LevelTooLow)

        // Поднимаем уровень и копим золото эпическими задачами.
        repeat(12) {
            completeNewTask(Difficulty.EPIC)
            time.advanceMinutes(60)
        }
        val hero = repository.heroState.first()!!
        assertTrue(hero.level.level >= 3)
        assertTrue(hero.gold >= Talent.EARLY_BIRD.cost)

        val unlocked = repository.unlockTalent(Talent.EARLY_BIRD)
        assertTrue(unlocked is TalentResult.Unlocked)
        assertTrue(Achievement.APPRENTICE in (unlocked as TalentResult.Unlocked).achievements)
        assertEquals(setOf(Talent.EARLY_BIRD), repository.heroState.first()!!.talents)
        assertEquals(hero.gold - Talent.EARLY_BIRD.cost + Achievement.APPRENTICE.tier.gold, repository.heroState.first()!!.gold)
        assertTrue(repository.unlockTalent(Talent.EARLY_BIRD) is TalentResult.Refused)

        time.hourOfDay = 8
        time.advanceMinutes(60)
        val morning = completeNewTask()
        assertTrue(morning.bonuses.any { it.kind == BonusKind.TALENT && it.label == Talent.EARLY_BIRD.title })
    }

    // endregion

    // region Испытания

    @Test
    fun `a challenge is rewarded exactly once when its target is reached`() = runTest {
        val board = repository.challengeBoard.first()
        assertEquals(Challenges.DAILY_COUNT, board.daily.size)
        assertEquals(Challenges.WEEKLY_COUNT, board.weekly.size)
        // Любое испытание, которое продвигается обычными выполнениями разных характеристик.
        val reachable = setOf(ChallengeMetric.QUESTS, ChallengeMetric.XP, ChallengeMetric.ATTRIBUTES)
        val target = (board.daily + board.weekly).first { it.challenge.metric in reachable }.challenge

        var rewardedAt = -1
        var attempts = 0
        while (rewardedAt < 0 && attempts < 80) {
            val attribute = Attribute.entries[attempts % Attribute.entries.size]
            val outcome = completeNewTask(Difficulty.EPIC, attribute)
            attempts++
            if (outcome.challenges.any { it.id == target.id }) rewardedAt = attempts
            time.advanceMinutes(90)
        }
        assertTrue("испытание должно быть засчитано", rewardedAt > 0)
        assertEquals(1, store.logs.value.count { it.kind == LogKind.CHALLENGE && it.refId == target.id })
        assertTrue(repository.challengeBoard.first().isClaimed(target))

        // Дальнейшие выполнения не выдают награду повторно.
        completeNewTask(Difficulty.EPIC)
        assertEquals(1, store.logs.value.count { it.kind == LogKind.CHALLENGE && it.refId == target.id })
        assertTrue(repository.statistics.first().stats.challenges >= 1)
        assertTrue(Achievement.CHALLENGER.name in store.achievements.value.map { it.id })
    }

    @Test
    fun `challenge set does not change when a book appears mid-day`() = runTest {
        val before = repository.challengeBoard.first().daily.map { it.challenge.id }
        repository.saveBook(BookDraft(title = "Книга", author = "", totalPages = 100, currentPage = 0, palette = 0))
        val after = repository.challengeBoard.first().daily.map { it.challenge.id }
        // Старые испытания остаются на месте (новые могут лишь добавиться в пределах трёх).
        assertTrue(after.containsAll(before.take(after.indexOfFirst { it !in before }.let { if (it < 0) after.size else it })))
        assertEquals(Challenges.DAILY_COUNT, after.size)
    }

    // endregion

    // region Босс недели

    @Test
    fun `boss takes damage from xp and is defeated once`() = runTest {
        val initial = repository.bossState.first()
        assertEquals(WeeklyBoss.forWeek(time.today(), 1), initial.boss)
        assertEquals(initial.boss.hp, initial.fight.hpLeft)
        assertFalse(initial.defeated)

        var defeatedBy: Int? = null
        var attempts = 0
        while (defeatedBy == null && attempts < 60) {
            val outcome = completeNewTask(Difficulty.EPIC)
            attempts++
            if (outcome.bossDefeated != null) defeatedBy = attempts
            time.advanceMinutes(120)
        }
        assertNotNull("босс должен быть повержен", defeatedBy)
        val state = repository.bossState.first()
        assertTrue(state.defeated)
        assertEquals(0, state.fight.hpLeft)
        assertEquals(1, store.logs.value.count { it.kind == LogKind.BOSS })
        assertTrue(Achievement.BOSS_HUNTER.name in store.achievements.value.map { it.id })

        completeNewTask(Difficulty.EPIC)
        assertEquals(1, store.logs.value.count { it.kind == LogKind.BOSS })

        // Новая неделя — новый босс с полным здоровьем.
        time.nextDay(7)
        val next = repository.bossState.first()
        assertFalse(next.defeated)
        assertTrue(next.boss.name != state.boss.name)
        assertEquals(next.boss.hp, next.fight.hpLeft)
    }

    @Test
    fun `boss strength is frozen at the level the hero had on monday`() = runTest {
        val monday = repository.bossState.first().boss
        repeat(10) {
            completeNewTask(Difficulty.EPIC)
            time.advanceMinutes(60)
        }
        assertTrue(repository.heroState.first()!!.level.level > 1)
        assertEquals(monday.hp, repository.bossState.first().boss.hp)
    }

    // endregion

    // region Фокус

    @Test
    fun `focus session survives, pauses and completes the quest in minutes`() = runTest {
        val id = repository.saveQuest(
            QuestDraft(title = "Медитация", emoji = "🧘", kind = QuestKind.HABIT, attribute = Attribute.SPIRIT, difficulty = Difficulty.EASY, target = 10, unit = Units.MINUTES),
        )
        assertTrue(repository.startFocus(id, 10))
        val session = repository.focusSession.first()!!
        assertEquals(id, session.questId)
        time.advanceMinutes(4)
        repository.pauseFocus()
        time.advanceMinutes(30)
        repository.resumeFocus()
        time.advanceMinutes(8)
        assertEquals(12, repository.focusSession.first()!!.elapsedMinutes(time.now()))

        val outcome = repository.finishFocus()!!
        assertEquals(12, outcome.amount)
        assertTrue(outcome.bonuses.any { it.kind == BonusKind.TARGET })
        assertNull(repository.focusSession.first())

        // Повторная сессия по уже выполненной привычке закрывается без награды.
        assertTrue(repository.startFocus(id, 5))
        assertNull(repository.finishFocus())
        assertNull(repository.focusSession.first())
    }

    @Test
    fun `deleting the quest cancels its focus session`() = runTest {
        val id = repository.saveQuest(task("Проект", target = 60, unit = Units.MINUTES))
        repository.startFocus(id, 25)
        repository.deleteQuest(id)
        assertNull(repository.focusSession.first())
    }

    // endregion

    // region Резервная копия

    @Test
    fun `backup round-trips through json`() = runTest {
        val bookId = repository.saveBook(BookDraft(title = "Книга", author = "Автор", totalPages = 120, currentPage = 10, palette = 3, dailyPages = 20))
        repository.logReading(bookId, 20)
        completeNewTask(Difficulty.HARD)
        val snapshot = repository.snapshot()
        val json = BackupCodec.encode(snapshot)
        val decoded = BackupCodec.decode(json)
        assertEquals(snapshot.copy(exportedAt = decoded.exportedAt), decoded)

        val heroBefore = repository.heroState.first()!!
        repository.resetProgress()
        assertFalse(repository.hasHero.first())
        repository.restore(decoded)
        val heroAfter = repository.heroState.first()!!
        assertEquals(heroBefore, heroAfter)
        assertEquals(30, store.books.value.single().currentPage)
        assertEquals(snapshot.logs.size, store.logs.value.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `foreign json is rejected`() {
        BackupCodec.decode("""{"hello":"world"}""")
    }

    // endregion

    @Test
    fun `settings toggles persist on the hero`() = runTest {
        repository.setSound(false)
        repository.setReduceMotion(true)
        val hero = repository.heroState.first()!!
        assertFalse(hero.soundEnabled)
        assertTrue(hero.reduceMotion)
    }

    @Test
    fun `week recap summarises the log`() = runTest {
        completeNewTask(Difficulty.NORMAL, Attribute.STRENGTH)
        time.nextDay()
        completeNewTask(Difficulty.NORMAL, Attribute.STRENGTH)
        completeNewTask(Difficulty.EASY, Attribute.INTELLECT)
        val recap = repository.weekRecap(Challenges.weekStart(time.today())).first()
        assertEquals(3, recap.quests)
        assertEquals(2, recap.activeDays)
        assertEquals(Attribute.STRENGTH, recap.topAttribute)
        assertEquals(time.today(), recap.bestDay)
        assertTrue(recap.xp > 0)
    }
}
