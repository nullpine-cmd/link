package com.ascend.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionTest {

    @Test
    fun `new hero starts at level one with empty bar`() {
        val info = Progression.heroLevel(0)
        assertEquals(1, info.level)
        assertEquals(0L, info.xpInLevel)
        assertEquals(Progression.xpToNextLevel(1), info.xpForNext)
        assertEquals(0f, info.progress)
    }

    @Test
    fun `second level arrives after a handful of quests`() {
        val need = Progression.xpToNextLevel(1)
        assertEquals(92L, need)
        assertEquals(1, Progression.heroLevel(need - 1).level)
        assertEquals(2, Progression.heroLevel(need).level)
        assertEquals(0L, Progression.heroLevel(need).xpInLevel)
    }

    @Test
    fun `total xp for level matches walking the curve`() {
        for (level in 1..60) {
            val xp = Progression.totalXpForLevel(level)
            assertEquals(level, Progression.heroLevel(xp).level)
            if (level > 1) assertEquals(level - 1, Progression.heroLevel(xp - 1).level)
        }
    }

    @Test
    fun `level is capped and reports full progress`() {
        val info = Progression.heroLevel(Long.MAX_VALUE / 4)
        assertEquals(Progression.MAX_LEVEL, info.level)
        assertTrue(info.isMax)
        assertEquals(1f, info.progress)
    }

    @Test
    fun `negative xp is treated as zero`() {
        assertEquals(1, Progression.heroLevel(-50).level)
        assertEquals(1, Progression.attributeLevel(-1).level)
    }

    @Test
    fun `ranks follow levels`() {
        assertEquals(Rank.E, Rank.forLevel(1))
        assertEquals(Rank.E, Rank.forLevel(4))
        assertEquals(Rank.D, Rank.forLevel(5))
        assertEquals(Rank.C, Rank.forLevel(10))
        assertEquals(Rank.S, Rank.forLevel(30))
        assertEquals(Rank.SSS, Rank.forLevel(99))
        assertEquals(null, Rank.SSS.next)
        assertEquals(Rank.D, Rank.E.next)
    }
}
