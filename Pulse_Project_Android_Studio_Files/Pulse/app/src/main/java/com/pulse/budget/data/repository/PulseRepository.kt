package com.pulse.budget.data.repository

import android.util.Log
import com.pulse.budget.data.local.AppDatabase
import com.pulse.budget.data.local.BadgeEntity
import com.pulse.budget.data.local.CategoryLimitEntity
import com.pulse.budget.data.local.CategoryType
import com.pulse.budget.data.local.CustomCategoryEntity
import com.pulse.budget.data.local.ExpenseEntity

private const val TAG = "PulseRepository"

/** Static badge catalog seeded into Room per-account on first login (see seedBadgesIfEmpty).
 *  userId here is just a placeholder — seedBadgesIfEmpty() always copies each entry with the
 *  real account id before inserting. */
val BADGE_CATALOG = listOf(
    BadgeEntity(userId = 0L, id = "budget_starter", title = "Budget Starter"),
    BadgeEntity(userId = 0L, id = "streak_7", title = "7-Day Streak"),
    BadgeEntity(userId = 0L, id = "on_track_explorer", title = "On-Track Explorer"),
    BadgeEntity(userId = 0L, id = "receipt_keeper", title = "Receipt Keeper"),
    BadgeEntity(userId = 0L, id = "streak_30", title = "30-Day Streak"),
    BadgeEntity(userId = 0L, id = "no_spend_master", title = "No-Spend Master"),
    BadgeEntity(userId = 0L, id = "budget_optimizer", title = "Budget Optimizer"),
    BadgeEntity(userId = 0L, id = "legend_investor", title = "Legend Investor")
)

class PulseRepository(private val db: AppDatabase) {
    private val expenseDao = db.expenseDao()
    private val limitDao = db.categoryLimitDao()
    private val badgeDao = db.badgeDao()
    private val customCategoryDao = db.customCategoryDao()

    suspend fun addExpense(expense: ExpenseEntity): Long {
        val id = expenseDao.insert(expense)
        Log.d(TAG, "Expense saved: id=$id userId=${expense.userId} amount=${expense.amount} category=${expense.category} " +
                "customCategory=${expense.customCategoryName} recurrence=${expense.recurrence}")
        return id
    }

    fun expensesInRange(userId: Long, startDay: Long, endDay: Long) = expenseDao.getExpensesInRange(userId, startDay, endDay)
    fun totalInRange(userId: Long, startDay: Long, endDay: Long) = expenseDao.getTotalInRange(userId, startDay, endDay)
    fun categoryTotalsInRange(userId: Long, startDay: Long, endDay: Long) = expenseDao.getCategoryTotalsInRange(userId, startDay, endDay)
    fun totalForDay(userId: Long, day: Long) = expenseDao.getTotalForDay(userId, day)
    fun dailyTotalsInRange(userId: Long, startDay: Long, endDay: Long) = expenseDao.getDailyTotalsInRange(userId, startDay, endDay)
    fun daysWithExpenses(userId: Long, startDay: Long, endDay: Long) = expenseDao.getDaysWithExpenses(userId, startDay, endDay)

    suspend fun setCategoryLimit(userId: Long, category: CategoryType, limit: Double) {
        Log.d(TAG, "Category limit set: userId=$userId $category -> R$limit")
        limitDao.upsert(CategoryLimitEntity(userId = userId, category = category, limit = limit))
    }

    fun categoryLimits(userId: Long) = limitDao.getAll(userId)

    /** Seeds the badge catalog for this account the first time it logs in. Safe to call on
     *  every login: insertAll uses OnConflictStrategy.IGNORE, so an account that already has
     *  its rows just no-ops here. */
    suspend fun seedBadgesIfEmpty(userId: Long) {
        badgeDao.insertAll(BADGE_CATALOG.map { it.copy(userId = userId) })
    }

    fun badges(userId: Long) = badgeDao.getAll(userId)

    // --- User-created categories (Part 2 brief: "the user must be able to create categories") ---

    suspend fun addCustomCategory(userId: Long, name: String): Long {
        val id = customCategoryDao.insert(CustomCategoryEntity(userId = userId, name = name.trim()))
        Log.d(TAG, "Custom category created: userId=$userId '$name' -> id=$id")
        return id
    }

    fun customCategories(userId: Long) = customCategoryDao.getAll(userId)

    /**
     * Evaluates and unlocks badges after an expense is logged. Call from the
     * ViewModel right after addExpense(). streakDays and xp come from PrefsRepository,
     * expenseCount/underBudgetDays/hasReceipt come from the caller's current state.
     */
    suspend fun evaluateBadges(
        userId: Long,
        streakDays: Int,
        expenseCount: Int,
        underBudgetDaysThisMonth: Int,
        totalMonthSpend: Double,
        monthlyBudget: Double,
        hasLoggedReceipt: Boolean,
        noSpendDaysThisWeek: Int
    ) {
        suspend fun unlock(id: String) {
            badgeDao.getById(userId, id)?.let { badge ->
                if (!badge.earned) {
                    Log.d(TAG, "Badge unlocked: userId=$userId $id")
                    badgeDao.update(badge.copy(earned = true))
                }
            }
        }
        if (expenseCount >= 1) unlock("budget_starter")
        if (streakDays >= 7) unlock("streak_7")
        if (streakDays >= 30) unlock("streak_30")
        if (hasLoggedReceipt) unlock("receipt_keeper")
        if (underBudgetDaysThisMonth >= 14) unlock("on_track_explorer")
        if (noSpendDaysThisWeek >= 2) unlock("no_spend_master")
        if (totalMonthSpend <= monthlyBudget * 0.8) unlock("budget_optimizer")
        if (streakDays >= 90) unlock("legend_investor")
    }
}