package com.ascend.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeroProgressTest {

    @Test
    fun `title emerges from what the hero actually does`() {
        assertEquals(HeroTitle.AWAKENED, HeroTitle.forAttributes(emptyMap()))
        assertEquals(
            HeroTitle.SAGE,
            HeroTitle.forAttributes(mapOf(Attribute.INTELLECT to 500L, Attribute.STRENGTH to 100L)),
        )
        assertEquals(
            HeroTitle.WARRIOR,
            HeroTitle.forAttributes(mapOf(Attribute.STRENGTH to 300L)),
        )
    }

    @Test
    fun `balanced growth gives the harmony title`() {
        val balanced = Attribute.entries.associateWith { 100L } + (Attribute.MASTERY to 120L)
        assertEquals(HeroTitle.HARMONY, HeroTitle.forAttributes(balanced))
    }

    @Test
    fun `achievements unlock from stats`() {
        val stats = HeroStats(questsCompleted = 12, pagesRead = 60, level = 5)
        val earned = AchievementRules.earned(stats)
        assertTrue(Achievement.FIRST_STEP in earned)
        assertTrue(Achievement.WARM_UP in earned)
        assertTrue(Achievement.FIRST_PAGES in earned)
        assertTrue(Achievement.AWAKENING in earned)
        assertTrue(Achievement.TIRELESS !in earned)
    }

    @Test
    fun `newly earned skips already unlocked`() {
        val stats = HeroStats(questsCompleted = 10)
        val fresh = AchievementRules.newlyEarned(stats, setOf(Achievement.FIRST_STEP))
        assertEquals(listOf(Achievement.WARM_UP), fresh)
    }

    @Test
    fun `harmony needs every attribute`() {
        val almost = Attribute.entries.associateWith { 3 } + (Attribute.CHARISMA to 2)
        assertTrue(Achievement.HARMONY !in AchievementRules.earned(HeroStats(attributeLevels = almost)))
        val all = Attribute.entries.associateWith { 3 }
        assertTrue(Achievement.HARMONY in AchievementRules.earned(HeroStats(attributeLevels = all)))
    }

    @Test
    fun `progress is clamped`() {
        assertEquals(0.5f, AchievementRules.progress(Achievement.WARM_UP, HeroStats(questsCompleted = 5)))
        assertEquals(1f, AchievementRules.progress(Achievement.WARM_UP, HeroStats(questsCompleted = 50)))
    }

    @Test
    fun `quote of the day is stable and in range`() {
        val q1 = Motivation.quoteOfDay(20_000)
        assertEquals(q1, Motivation.quoteOfDay(20_000))
        for (day in -100L..400L) Motivation.quoteOfDay(day)
    }
}
