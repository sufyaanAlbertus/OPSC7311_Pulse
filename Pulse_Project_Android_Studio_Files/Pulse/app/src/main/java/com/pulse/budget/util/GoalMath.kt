package com.pulse.budget.util

import com.pulse.budget.data.model.CategoryGoal
import com.pulse.budget.data.model.Expense
import com.pulse.budget.data.model.SpendGroup

// Pure goal calculations behind the two new required visuals: 1.

/** Goals are entered as MONTHLY amounts; a "month" is treated as 30 days for scaling. */
const val DAYS_PER_GOAL_MONTH = 30.0

enum class GoalStatus { NO_GOAL, UNDER_MIN, ON_TRACK, OVER_MAX }

/** Scales a monthly goal to a different period length so the graph compares like with like. */
fun scaleMonthlyGoal(monthlyGoal: Double, periodDays: Long): Double {
    if (monthlyGoal <= 0.0 || periodDays <= 0L) return 0.0
    return monthlyGoal * periodDays / DAYS_PER_GOAL_MONTH
}

/** Where an amount sits relative to a min/max goal pair. */
fun goalStatus(spent: Double, minGoal: Double, maxGoal: Double): GoalStatus = when {
    minGoal <= 0.0 && maxGoal <= 0.0 -> GoalStatus.NO_GOAL
    maxGoal > 0.0 && spent > maxGoal -> GoalStatus.OVER_MAX
    spent < minGoal -> GoalStatus.UNDER_MIN
    else -> GoalStatus.ON_TRACK
}

/** Groups expenses by category (built-in categories and the user's own ones), largest first. */
fun groupSpend(expenses: List<Expense>): List<SpendGroup> =
    expenses.groupBy { it.groupKey }
        .map { (key, items) ->
            val first = items.first()
            SpendGroup(
                key = key,
                label = first.categoryLabel,
                category = first.category,
                isCustom = first.customCategoryName != null,
                total = items.sumOf { it.amount }
            )
        }
        .sortedByDescending { it.total }

/** One bar on the category graph. */
data class CategoryChartRow(
    val key: String,
    val label: String,
    val total: Double,
    val minGoal: Double,
    val maxGoal: Double,
    val status: GoalStatus
)

/** Builds the rows for the category graph. */
fun buildCategoryChartRows(
    groups: List<SpendGroup>,
    goals: List<CategoryGoal>,
    labelForKey: (String) -> String,
    periodDays: Long
): List<CategoryChartRow> {
    val goalsByKey = goals.associateBy { it.key }
    val spentRows = groups.map { g ->
        val goal = goalsByKey[g.key]
        val min = scaleMonthlyGoal(goal?.minGoal ?: 0.0, periodDays)
        val max = scaleMonthlyGoal(goal?.maxGoal ?: 0.0, periodDays)
        CategoryChartRow(g.key, g.label, g.total, min, max, goalStatus(g.total, min, max))
    }
    val spentKeys = groups.map { it.key }.toSet()
    val goalOnlyRows = goals
        .filter { it.key !in spentKeys && (it.minGoal > 0.0 || it.maxGoal > 0.0) }
        .map { goal ->
            val min = scaleMonthlyGoal(goal.minGoal, periodDays)
            val max = scaleMonthlyGoal(goal.maxGoal, periodDays)
            CategoryChartRow(goal.key, labelForKey(goal.key), 0.0, min, max, goalStatus(0.0, min, max))
        }
    return (spentRows + goalOnlyRows).sortedByDescending { maxOf(it.total, it.maxGoal) }
}

/** One day on the 30-day goal tracker. */
data class GoalTrackPoint(
    val epochDay: Long,
    val daySpend: Double,
    val cumulative: Double,
    val minPace: Double,
    val maxPace: Double
) {
    val status: GoalStatus get() = goalStatus(cumulative, minPace, maxPace)
}

/** Builds the cumulative spending line for [startDay]..[endDay] with the min/max goal pace. */
fun buildGoalTrack(
    dailyTotals: Map<Long, Double>,
    startDay: Long,
    endDay: Long,
    monthlyMin: Double,
    monthlyMax: Double
): List<GoalTrackPoint> {
    if (endDay < startDay) return emptyList()
    val days = (endDay - startDay + 1).toDouble()
    var running = 0.0
    return (startDay..endDay).mapIndexed { index, day ->
        val spend = dailyTotals[day] ?: 0.0
        running += spend
        val fraction = (index + 1) / days
        GoalTrackPoint(
            epochDay = day,
            daySpend = spend,
            cumulative = running,
            minPace = monthlyMin.coerceAtLeast(0.0) * fraction,
            maxPace = monthlyMax.coerceAtLeast(0.0) * fraction
        )
    }
}

/** Summary shown next to the tracker: a 0–100 score and a count of each kind of day. */
data class GoalTrackSummary(
    val scorePercent: Int,
    val daysOnTrack: Int,
    val daysUnder: Int,
    val daysOver: Int,
    val totalSpent: Double,
    val finalStatus: GoalStatus
)

fun summarizeGoalTrack(points: List<GoalTrackPoint>, monthlyMin: Double, monthlyMax: Double): GoalTrackSummary {
    if (points.isEmpty()) return GoalTrackSummary(0, 0, 0, 0, 0.0, GoalStatus.NO_GOAL)
    val onTrack = points.count { it.status == GoalStatus.ON_TRACK || it.status == GoalStatus.NO_GOAL }
    val under = points.count { it.status == GoalStatus.UNDER_MIN }
    val over = points.count { it.status == GoalStatus.OVER_MAX }
    val total = points.last().cumulative
    return GoalTrackSummary(
        scorePercent = (onTrack * 100) / points.size,
        daysOnTrack = onTrack,
        daysUnder = under,
        daysOver = over,
        totalSpent = total,
        finalStatus = goalStatus(total, monthlyMin, monthlyMax)
    )
}
