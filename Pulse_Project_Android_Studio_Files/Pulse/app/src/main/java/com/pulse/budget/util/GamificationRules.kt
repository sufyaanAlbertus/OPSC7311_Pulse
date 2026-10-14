package com.pulse.budget.util

// Own feature #1: gamification (streaks, XP, levels, badges).

/** XP awarded every time an expense is logged. */
const val XP_PER_LOG = 15

/** XP needed per level on the Badges screen. */
const val XP_PER_LEVEL = 1000

/** Works out the new streak after logging on [todayEpochDay]. */
fun nextStreak(lastLogEpochDay: Long?, todayEpochDay: Long, currentStreak: Int): Int {
    if (lastLogEpochDay == null) return 1
    return when (todayEpochDay - lastLogEpochDay) {
        0L -> currentStreak.coerceAtLeast(1)
        1L -> currentStreak + 1
        else -> 1
    }
}

fun levelForXp(xp: Int): Int = (xp.coerceAtLeast(0) / XP_PER_LEVEL) + 1

/** Everything the badge rules need to know, gathered by ExpenseViewModel after each log. */
data class BadgeStats(
    val streakDays: Int,
    val expenseCountThisMonth: Int,
    val loggingDaysThisMonth: Int,
    val monthSpend: Double,
    val minMonthlyGoal: Double,
    val maxMonthlyGoal: Double,
    val hasLoggedReceipt: Boolean,
    val noSpendDaysThisWeek: Int
)

/** Returns the ids of every badge the user currently qualifies for (see BADGE_CATALOG). */
fun qualifyingBadges(s: BadgeStats): Set<String> {
    val earned = mutableSetOf<String>()
    if (s.expenseCountThisMonth >= 1) earned += "budget_starter"
    if (s.streakDays >= 7) earned += "streak_7"
    if (s.streakDays >= 30) earned += "streak_30"
    if (s.streakDays >= 90) earned += "legend_investor"
    if (s.hasLoggedReceipt) earned += "receipt_keeper"
    if (s.loggingDaysThisMonth >= 14) earned += "on_track_explorer"
    if (s.noSpendDaysThisWeek >= 2 && s.loggingDaysThisMonth >= 7) earned += "no_spend_master"
    if (s.expenseCountThisMonth >= 10 && s.maxMonthlyGoal > 0 && s.monthSpend <= s.maxMonthlyGoal * 0.8) {
        earned += "budget_optimizer"
    }
    if (s.expenseCountThisMonth >= 5 &&
        goalStatus(s.monthSpend, s.minMonthlyGoal, s.maxMonthlyGoal) == GoalStatus.ON_TRACK
    ) {
        earned += "goal_keeper"
    }
    return earned
}


data class BadgeProgress(
    val current: Int,
    val target: Int,
    /** Unit shown after the numbers, e.g. "days in a row". */
    val unit: String,
    /** What the user should do next, in plain words. */
    val hint: String
) {
    val fraction: Float get() = if (target <= 0) 0f else (current.toFloat() / target).coerceIn(0f, 1f)
    val isComplete: Boolean get() = current >= target
}

fun badgeProgress(badgeId: String, s: BadgeStats): BadgeProgress? = when (badgeId) {
    "budget_starter" -> BadgeProgress(
        minOf(s.expenseCountThisMonth, 1), 1, "expense logged",
        "Log any expense with the + Log Expense button."
    )
    "streak_7" -> streakProgress(s.streakDays, 7)
    "streak_30" -> streakProgress(s.streakDays, 30)
    "legend_investor" -> streakProgress(s.streakDays, 90)
    "receipt_keeper" -> BadgeProgress(
        if (s.hasLoggedReceipt) 1 else 0, 1, "receipt attached",
        "When adding an expense, tap the receipt box and choose a photo."
    )
    "on_track_explorer" -> BadgeProgress(
        minOf(s.loggingDaysThisMonth, 14), 14, "days logged this month",
        "Log at least one expense on ${remaining(s.loggingDaysThisMonth, 14)} more different days this month."
    )
    "no_spend_master" -> BadgeProgress(
        minOf(s.noSpendDaysThisWeek, 2), 2, "no-spend days this week",
        "Have 2 days this week with no spending, and log on 7+ days this month " +
                "(you're on ${minOf(s.loggingDaysThisMonth, 7)} / 7)."
    )
    "budget_optimizer" -> {
        val usedPercent = if (s.maxMonthlyGoal > 0) (s.monthSpend / s.maxMonthlyGoal * 100).toInt() else 0
        BadgeProgress(
            minOf(s.expenseCountThisMonth, 10), 10, "expenses this month",
            "Log 10 expenses this month while using 80% or less of your maximum goal " +
                    "(you've used $usedPercent%)."
        )
    }
    "goal_keeper" -> BadgeProgress(
        minOf(s.expenseCountThisMonth, 5), 5, "expenses this month",
        "Log 5 expenses and keep this month's total between your minimum and maximum goals."
    )
    else -> null
}

private fun streakProgress(streak: Int, target: Int) = BadgeProgress(
    minOf(streak, target), target, "days in a row",
    if (streak >= target) "Done!" else
        "Log at least one expense every day. ${remaining(streak, target)} more day${if (target - streak == 1) "" else "s"} to go."
)

private fun remaining(current: Int, target: Int) = (target - current).coerceAtLeast(0)

/** XP still needed to reach the next level. */
fun xpToNextLevel(xp: Int): Int = XP_PER_LEVEL - (xp.coerceAtLeast(0) % XP_PER_LEVEL)
