package com.pulse.budget.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests for own feature #1 — streaks, levels and badge unlock rules. */
class GamificationRulesTest {

    @Test
    fun `streak starts, continues, holds and resets correctly`() {
        assertEquals(1, nextStreak(null, 100, 0))   // first ever log
        assertEquals(5, nextStreak(100, 101, 4))    // next day
        assertEquals(4, nextStreak(101, 101, 4))    // same day
        assertEquals(1, nextStreak(100, 105, 9))    // missed days
    }

    @Test
    fun `level goes up every 1000 xp`() {
        assertEquals(1, levelForXp(0))
        assertEquals(1, levelForXp(999))
        assertEquals(2, levelForXp(1000))
        assertEquals(4, levelForXp(3500))
    }

    private fun stats(
        streak: Int = 0, count: Int = 0, days: Int = 0, spend: Double = 0.0,
        min: Double = 2000.0, max: Double = 10000.0, receipt: Boolean = false, noSpend: Int = 0
    ) = BadgeStats(streak, count, days, spend, min, max, receipt, noSpend)

    @Test
    fun `first expense unlocks Budget Starter only`() {
        val earned = qualifyingBadges(stats(streak = 1, count = 1, days = 1, spend = 50.0, noSpend = 6))
        assertEquals(setOf("budget_starter"), earned)
    }

    @Test
    fun `streak badges unlock at 7, 30 and 90 days`() {
        assertTrue("streak_7" in qualifyingBadges(stats(streak = 7, count = 1)))
        assertFalse("streak_30" in qualifyingBadges(stats(streak = 29, count = 1)))
        val ninety = qualifyingBadges(stats(streak = 90, count = 1))
        assertTrue(ninety.containsAll(listOf("streak_7", "streak_30", "legend_investor")))
    }

    @Test
    fun `goal keeper needs 5 expenses and spend between min and max`() {
        assertTrue("goal_keeper" in qualifyingBadges(stats(count = 5, spend = 3000.0)))
        assertFalse("goal_keeper" in qualifyingBadges(stats(count = 5, spend = 500.0)))
        assertFalse("goal_keeper" in qualifyingBadges(stats(count = 4, spend = 3000.0)))
    }

    @Test
    fun `receipt keeper and budget optimizer rules`() {
        assertTrue("receipt_keeper" in qualifyingBadges(stats(count = 1, receipt = true)))
        assertTrue("budget_optimizer" in qualifyingBadges(stats(count = 10, spend = 8000.0)))
        assertFalse("budget_optimizer" in qualifyingBadges(stats(count = 10, spend = 8001.0)))
    }

    @Test
    fun `badge progress shows how far along the user is`() {
        val p = badgeProgress("streak_7", stats(streak = 3, count = 1))!!
        assertEquals(3, p.current)
        assertEquals(7, p.target)
        assertTrue(p.hint.contains("4 more days"))
        assertFalse(p.isComplete)
    }

    @Test
    fun `badge progress is capped at the target and unknown ids return null`() {
        val p = badgeProgress("on_track_explorer", stats(days = 20, count = 20))!!
        assertEquals(14, p.current)
        assertTrue(p.isComplete)
        assertEquals(1f, p.fraction, 0.001f)
        assertEquals(null, badgeProgress("not_a_badge", stats()))
    }

    @Test
    fun `xp to next level`() {
        assertEquals(1000, xpToNextLevel(0))
        assertEquals(550, xpToNextLevel(450))
        assertEquals(1000, xpToNextLevel(2000))
    }
}
