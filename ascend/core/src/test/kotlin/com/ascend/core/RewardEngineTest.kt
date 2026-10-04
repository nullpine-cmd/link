package com.ascend.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardEngineTest {

    @Test
    fun `one page out of twenty still gives the full base reward`() {
        val reward = RewardEngine.calculate(RewardInput(Difficulty.NORMAL, amount = 1, target = 20))
        assertEquals(Difficulty.NORMAL.baseXp, reward.baseXp)
        assertEquals(Difficulty.NORMAL.baseXp, reward.totalXp)
        assertFalse(reward.hitTarget)
        assertTrue(reward.gold > 0)
    }

    @Test
    fun `reaching the target adds a bonus, doubling it adds another`() {
        val hit = RewardEngine.calculate(RewardInput(Difficulty.NORMAL, amount = 20, target = 20))
        assertTrue(hit.hitTarget)
        assertEquals(listOf(BonusKind.TARGET), hit.bonuses.map { it.kind })
        assertEquals(30, hit.totalXp)

        val double = RewardEngine.calculate(RewardInput(Difficulty.NORMAL, amount = 40, target = 20))
        assertTrue(double.overachieved)
        assertEquals(listOf(BonusKind.TARGET, BonusKind.OVERACHIEVE), double.bonuses.map { it.kind })
        assertEquals(35, double.totalXp)
    }

    @Test
    fun `every attribute is rewarded equally`() {
        // Награда зависит только от сложности и прогресса, а не от выбранной характеристики:
        // отжимания и чтение одинаково ценны.
        val pushUps = RewardEngine.calculate(RewardInput(Difficulty.NORMAL, amount = 30, target = 30))
        val reading = RewardEngine.calculate(RewardInput(Difficulty.NORMAL, amount = 20, target = 20))
        assertEquals(pushUps.totalXp, reading.totalXp)
        assertEquals(pushUps.gold, reading.gold)
    }

    @Test
    fun `habit streak continues from previous scheduled day`() {
        val today = 20_000L
        val reward = RewardEngine.calculate(
            RewardInput(
                difficulty = Difficulty.NORMAL,
                amount = 1,
                isHabit = true,
                streakBefore = 4,
                lastCompletedDay = today - 1,
                previousDueDay = today - 1,
            ),
        )
        assertEquals(5, reward.newStreak)
        val streakBonus = reward.bonuses.single { it.kind == BonusKind.STREAK }
        assertEquals(4, streakBonus.xp) // 20 * 4 * 5%
    }

    @Test
    fun `missed day restarts streak without any penalty`() {
        val today = 20_000L
        val reward = RewardEngine.calculate(
            RewardInput(
                difficulty = Difficulty.NORMAL,
                amount = 1,
                isHabit = true,
                streakBefore = 10,
                lastCompletedDay = today - 3,
                previousDueDay = today - 1,
            ),
        )
        assertEquals(1, reward.newStreak)
        assertEquals(Difficulty.NORMAL.baseXp, reward.totalXp)
    }

    @Test
    fun `comeback after a break is celebrated`() {
        val reward = RewardEngine.calculate(
            RewardInput(Difficulty.EASY, amount = 1, isFirstCompletionToday = true, daysSinceLastActive = 5),
        )
        assertTrue(reward.comeback)
        assertEquals(
            listOf(BonusKind.FIRST_OF_DAY, BonusKind.COMEBACK),
            reward.bonuses.map { it.kind },
        )
        assertEquals(10 + RewardEngine.FIRST_OF_DAY_XP + RewardEngine.COMEBACK_XP, reward.totalXp)
    }

    @Test
    fun `tasks have no streak`() {
        val reward = RewardEngine.calculate(RewardInput(Difficulty.HARD, amount = 1))
        assertEquals(0, reward.newStreak)
        assertEquals(35, reward.totalXp)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zero progress is not a completion`() {
        RewardEngine.calculate(RewardInput(Difficulty.EASY, amount = 0))
    }

    @Test
    fun `reading session grows with pages but stays modest`() {
        assertEquals(10, RewardEngine.readingSession(1, false, null).totalXp)
        assertEquals(14, RewardEngine.readingSession(20, false, null).totalXp)
        assertEquals(20, RewardEngine.readingSession(500, false, null).totalXp)
    }
}
