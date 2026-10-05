package com.ascend.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class UpgradeRulesTest {

    private val monday = LocalDate.of(2026, 10, 5).toEpochDay()
    private val minute = 60_000L

    // region Комбо

    @Test
    fun `combo chain counts only close completions`() {
        val now = 100 * minute
        assertEquals(0, Combo.step(emptyList(), now))
        assertEquals(1, Combo.step(listOf(now - 10 * minute), now))
        assertEquals(2, Combo.step(listOf(now - 10 * minute, now - 35 * minute), now))
        // Разрыв больше окна прерывает цепочку.
        assertEquals(1, Combo.step(listOf(now - 10 * minute, now - 50 * minute, now - 60 * minute), now))
        assertEquals(0, Combo.step(listOf(now - 31 * minute), now))
    }

    @Test
    fun `combo bonus stacks and is capped, talent raises the cap`() {
        val two = RewardEngine.calculate(RewardInput(Difficulty.NORMAL, amount = 1, comboStep = 1))
        assertEquals(listOf("Комбо ×2"), two.bonuses.map { it.label })
        assertEquals(22, two.totalXp)

        val capped = RewardEngine.calculate(RewardInput(Difficulty.NORMAL, amount = 1, comboStep = 9))
        assertEquals(Combo.BASE_CAP, capped.comboStep)
        assertEquals(30, capped.totalXp)

        val king = RewardEngine.calculate(RewardInput(Difficulty.NORMAL, amount = 1, comboStep = 9, talents = setOf(Talent.COMBO_KING)))
        assertEquals(Combo.KING_CAP, king.comboStep)
        assertEquals(36, king.totalXp)
    }

    // endregion

    // region Таланты

    @Test
    fun `time talents apply only in their hours`() {
        val morning = RewardEngine.calculate(RewardInput(Difficulty.NORMAL, amount = 1, hour = 8, talents = setOf(Talent.EARLY_BIRD, Talent.NIGHT_WATCH)))
        assertEquals(listOf(Talent.EARLY_BIRD.title), morning.bonuses.map { it.label })
        assertEquals(25, morning.totalXp)

        val noon = RewardEngine.calculate(RewardInput(Difficulty.NORMAL, amount = 1, hour = 13, talents = setOf(Talent.EARLY_BIRD, Talent.NIGHT_WATCH)))
        assertTrue(noon.bonuses.isEmpty())

        val night = RewardEngine.calculate(RewardInput(Difficulty.NORMAL, amount = 1, hour = 22, talents = setOf(Talent.NIGHT_WATCH)))
        assertEquals(25, night.totalXp)
    }

    @Test
    fun `talents reshape existing bonuses`() {
        val perfectionist = RewardEngine.calculate(RewardInput(Difficulty.NORMAL, amount = 20, target = 20, talents = setOf(Talent.PERFECTIONIST)))
        assertEquals(15, perfectionist.bonuses.single { it.kind == BonusKind.TARGET }.xp)

        val diligent = RewardEngine.calculate(RewardInput(Difficulty.NORMAL, amount = 30, target = 20, talents = setOf(Talent.DILIGENCE)))
        assertEquals(4, diligent.bonuses.single { it.kind == BonusKind.TALENT }.xp)
        val diligentCapped = RewardEngine.calculate(RewardInput(Difficulty.NORMAL, amount = 200, target = 20, talents = setOf(Talent.DILIGENCE)))
        assertEquals(10, diligentCapped.bonuses.single { it.kind == BonusKind.TALENT }.xp)

        val focused = RewardEngine.calculate(RewardInput(Difficulty.EASY, amount = 1, isFirstCompletionToday = true, talents = setOf(Talent.FOCUS_MASTER)))
        assertEquals(RewardEngine.FIRST_OF_DAY_XP_FOCUSED, focused.bonuses.single().xp)

        val secondWind = RewardEngine.calculate(RewardInput(Difficulty.EASY, amount = 1, daysSinceLastActive = 5, talents = setOf(Talent.SECOND_WIND)))
        assertEquals(RewardEngine.COMEBACK_XP * 2, secondWind.bonuses.single { it.kind == BonusKind.COMEBACK }.xp)

        val golden = RewardEngine.calculate(RewardInput(Difficulty.NORMAL, amount = 1, talents = setOf(Talent.GOLDEN_TOUCH)))
        assertEquals(10, golden.gold)
        assertEquals(8, RewardEngine.calculate(RewardInput(Difficulty.NORMAL, amount = 1)).gold)

        val sage = RewardEngine.calculate(RewardInput(Difficulty.NORMAL, amount = 1, isWeakestAttribute = true, talents = setOf(Talent.BALANCE_SAGE)))
        assertEquals(23, sage.totalXp)
    }

    @Test
    fun `iron streak extends the streak cap`() {
        val base = RewardInput(Difficulty.NORMAL, amount = 1, isHabit = true, streakBefore = 40, lastCompletedDay = 10, previousDueDay = 10)
        assertEquals(20, RewardEngine.calculate(base).bonuses.single { it.kind == BonusKind.STREAK }.xp)
        assertEquals(30, RewardEngine.calculate(base.copy(talents = setOf(Talent.IRON_STREAK))).bonuses.single { it.kind == BonusKind.STREAK }.xp)
    }

    @Test
    fun `talent purchase rules`() {
        assertEquals(TalentPurchase.Ok, TalentPurchase.check(Talent.EARLY_BIRD, emptySet(), level = 3, gold = 150))
        assertEquals(TalentPurchase.LevelTooLow(3), TalentPurchase.check(Talent.EARLY_BIRD, emptySet(), level = 2, gold = 999))
        assertEquals(TalentPurchase.NotEnoughGold(50), TalentPurchase.check(Talent.EARLY_BIRD, emptySet(), level = 5, gold = 100))
        assertEquals(TalentPurchase.AlreadyOwned, TalentPurchase.check(Talent.EARLY_BIRD, setOf(Talent.EARLY_BIRD), level = 9, gold = 999))
    }

    // endregion

    // region Испытания

    @Test
    fun `daily challenges are deterministic, distinct and level-scaled`() {
        val profile = HeroProfile(level = 1)
        val a = Challenges.daily(monday, profile)
        val b = Challenges.daily(monday, profile)
        assertEquals(a, b)
        assertEquals(Challenges.DAILY_COUNT, a.size)
        assertEquals(a.size, a.map { it.metric }.distinct().size)
        assertTrue(a.all { it.period == ChallengePeriod.DAILY && it.periodStart == monday })
        assertTrue(a.all { it.id.startsWith("D:$monday:") })

        val differentDays = (0L until 14L).map { Challenges.daily(monday + it, profile).map { c -> c.metric } }.distinct()
        assertTrue("наборы должны меняться день ото дня", differentDays.size > 3)

        val veteran = Challenges.daily(monday, HeroProfile(level = 20))
        val xpNovice = a.firstOrNull { it.metric == ChallengeMetric.XP }
        val xpVeteran = veteran.firstOrNull { it.metric == ChallengeMetric.XP }
        if (xpNovice != null && xpVeteran != null) assertTrue(xpVeteran.target > xpNovice.target)
    }

    @Test
    fun `challenges that need books or targets appear only when the hero has them`() {
        val plain = HeroProfile(level = 5, hasBooks = false, hasTargets = false)
        for (day in 0L until 60L) {
            val metrics = Challenges.daily(monday + day, plain).map { it.metric } + Challenges.weekly(monday + day, plain).map { it.metric }
            assertFalse(ChallengeMetric.PAGES in metrics)
            assertFalse(ChallengeMetric.OVERACHIEVE in metrics)
        }
        val reader = HeroProfile(level = 5, hasBooks = true, hasTargets = true)
        val all = (0L until 60L).flatMap { Challenges.daily(monday + it, reader).map { c -> c.metric } }
        assertTrue(ChallengeMetric.PAGES in all)
        assertTrue(ChallengeMetric.OVERACHIEVE in all)
    }

    @Test
    fun `weekly challenges share one set for the whole week`() {
        val profile = HeroProfile(level = 3)
        val week = (0L until 7L).map { Challenges.weekly(monday + it, profile) }.distinct()
        assertEquals(1, week.size)
        assertTrue(week.single().all { it.periodStart == monday && it.period == ChallengePeriod.WEEKLY })
        assertEquals(monday + 7, Challenges.weekStart(monday + 9))
        assertEquals(monday + 6, Challenges.weekEnd(monday + 2))
        assertEquals(monday, Challenges.weekStart(monday + 6))
    }

    @Test
    fun `progress is measured against the metric and clamped`() {
        val challenge = Challenge("x", ChallengePeriod.DAILY, ChallengeMetric.QUESTS, 3, "⚔️", "", 20, 10, monday)
        assertEquals(2, challenge.progress(ActivityStats(quests = 2)).current)
        assertFalse(challenge.progress(ActivityStats(quests = 2)).done)
        val over = challenge.progress(ActivityStats(quests = 7))
        assertEquals(3, over.current)
        assertTrue(over.done)
        assertEquals(1f, over.fraction)

        val attributes = Challenge("y", ChallengePeriod.WEEKLY, ChallengeMetric.ATTRIBUTES, 6, "🎨", "", 150, 75, monday)
        assertEquals(2, attributes.progress(ActivityStats(attributes = setOf(Attribute.SPIRIT, Attribute.STRENGTH))).current)
    }

    // endregion

    // region Босс недели

    @Test
    fun `boss is stable within a week and scales with level`() {
        val a = WeeklyBoss.forWeek(monday, 1)
        val b = WeeklyBoss.forWeek(monday + 6, 1)
        assertEquals(a, b)
        assertEquals(monday, a.weekStart)
        assertEquals("W:$monday", a.id)
        assertTrue(a.hp % 10 == 0)
        assertTrue(WeeklyBoss.forWeek(monday, 10).hp > a.hp)
        val next = WeeklyBoss.forWeek(monday + 7, 1)
        assertTrue(next.name != a.name)
    }

    @Test
    fun `fight tracks hp and defeat`() {
        val boss = WeeklyBoss.forWeek(monday, 1)
        val fight = WeeklyBoss.fight(boss, 100)
        assertEquals(boss.hp - 100, fight.hpLeft)
        assertFalse(fight.defeated)
        val win = WeeklyBoss.fight(boss, boss.hp.toLong() + 50)
        assertTrue(win.defeated)
        assertEquals(0, win.hpLeft)
        assertEquals(0f, win.fraction)
    }

    // endregion

    @Test
    fun `new achievements unlock from new metrics`() {
        val stats = HeroStats(challenges = 1, bosses = 1, talents = 5, bestCombo = 5)
        val earned = AchievementRules.earned(stats)
        assertTrue(Achievement.CHALLENGER in earned)
        assertTrue(Achievement.BOSS_HUNTER in earned)
        assertTrue(Achievement.TALENTED in earned)
        assertTrue(Achievement.COMBO_MASTER in earned)
        assertFalse(Achievement.DRAGON_SLAYER in earned)
    }
}
