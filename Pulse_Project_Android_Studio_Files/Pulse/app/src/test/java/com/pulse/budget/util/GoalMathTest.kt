package com.pulse.budget.util

import com.pulse.budget.data.model.CategoryGoal
import com.pulse.budget.data.model.CategoryType
import com.pulse.budget.data.model.Expense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests for the maths behind the category graph and the 30-day goal tracker. */
class GoalMathTest {

    private val delta = 0.001

    @Test
    fun `monthly goal is scaled to the length of the period`() {
        assertEquals(700.0, scaleMonthlyGoal(3000.0, 7), delta)
        assertEquals(3000.0, scaleMonthlyGoal(3000.0, 30), delta)
        assertEquals(9000.0, scaleMonthlyGoal(3000.0, 90), delta)
    }

    @Test
    fun `zero or negative goal or period scales to zero`() {
        assertEquals(0.0, scaleMonthlyGoal(0.0, 30), delta)
        assertEquals(0.0, scaleMonthlyGoal(1000.0, 0), delta)
    }

    @Test
    fun `goal status covers every case`() {
        assertEquals(GoalStatus.NO_GOAL, goalStatus(500.0, 0.0, 0.0))
        assertEquals(GoalStatus.UNDER_MIN, goalStatus(100.0, 200.0, 1000.0))
        assertEquals(GoalStatus.ON_TRACK, goalStatus(500.0, 200.0, 1000.0))
        assertEquals(GoalStatus.ON_TRACK, goalStatus(1000.0, 200.0, 1000.0)) // exactly at max is fine
        assertEquals(GoalStatus.OVER_MAX, goalStatus(1000.01, 200.0, 1000.0))
        assertEquals(GoalStatus.ON_TRACK, goalStatus(5000.0, 200.0, 0.0))     // no max set
    }

    @Test
    fun `groupSpend keeps built-in and custom categories separate`() {
        val expenses = listOf(
            Expense(amount = 100.0, category = CategoryType.GROCERIES),
            Expense(amount = 50.0, category = CategoryType.GROCERIES),
            Expense(amount = 80.0, category = CategoryType.OTHER, customCategoryName = "Gym"),
            Expense(amount = 20.0, category = CategoryType.OTHER)
        )
        val groups = groupSpend(expenses)
        assertEquals(3, groups.size)
        assertEquals("Groceries", groups[0].label)
        assertEquals(150.0, groups[0].total, delta)
        assertEquals("Gym", groups[1].label)
        assertTrue(groups[1].isCustom)
    }

    @Test
    fun `chart rows include goal-only categories and scale goals`() {
        val groups = groupSpend(listOf(Expense(amount = 300.0, category = CategoryType.TRANSPORT)))
        val goals = listOf(
            CategoryGoal(key = "TRANSPORT", minGoal = 300.0, maxGoal = 600.0),
            CategoryGoal(key = "HEALTH", minGoal = 150.0, maxGoal = 0.0)
        )
        val rows = buildCategoryChartRows(groups, goals, { it }, periodDays = 15)
        assertEquals(2, rows.size)
        val transport = rows.first { it.key == "TRANSPORT" }
        assertEquals(150.0, transport.minGoal, delta)
        assertEquals(300.0, transport.maxGoal, delta)
        assertEquals(GoalStatus.ON_TRACK, transport.status)
        val health = rows.first { it.key == "HEALTH" }
        assertEquals(0.0, health.total, delta)
        assertEquals(GoalStatus.UNDER_MIN, health.status)
    }

    @Test
    fun `goal track builds a running total and pace band`() {
        val track = buildGoalTrack(mapOf(0L to 100.0, 2L to 50.0), startDay = 0, endDay = 3, monthlyMin = 400.0, monthlyMax = 800.0)
        assertEquals(4, track.size)
        assertEquals(listOf(100.0, 100.0, 150.0, 150.0), track.map { it.cumulative })
        assertEquals(100.0, track[0].minPace, delta) // 400 * 1/4
        assertEquals(800.0, track[3].maxPace, delta)
    }

    @Test
    fun `summary score counts days inside the band`() {
        val points = listOf(
            GoalTrackPoint(0, 0.0, 50.0, 10.0, 100.0),   // on track
            GoalTrackPoint(1, 0.0, 5.0, 20.0, 200.0),    // under
            GoalTrackPoint(2, 0.0, 400.0, 30.0, 300.0),  // over
            GoalTrackPoint(3, 0.0, 200.0, 40.0, 400.0)   // on track
        )
        val summary = summarizeGoalTrack(points, monthlyMin = 40.0, monthlyMax = 400.0)
        assertEquals(50, summary.scorePercent)
        assertEquals(2, summary.daysOnTrack)
        assertEquals(1, summary.daysUnder)
        assertEquals(1, summary.daysOver)
        assertEquals(GoalStatus.ON_TRACK, summary.finalStatus)
    }

    @Test
    fun `empty track gives an empty summary`() {
        val summary = summarizeGoalTrack(emptyList(), 100.0, 200.0)
        assertEquals(0, summary.scorePercent)
        assertEquals(GoalStatus.NO_GOAL, summary.finalStatus)
    }
}
