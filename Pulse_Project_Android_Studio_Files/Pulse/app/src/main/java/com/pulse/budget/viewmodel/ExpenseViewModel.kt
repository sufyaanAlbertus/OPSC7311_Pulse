package com.pulse.budget.viewmodel

/*
 * References:
 *  - Android Developers (2025) StateFlow and SharedFlow. https://developer.android.com/kotlin/flow/stateflow-and-sharedflow
 *  - Anthropic (2026) Claude [Large language model] — used as a coding assistant; see AI declaration in README.
 */

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.budget.data.model.Badge
import com.pulse.budget.data.model.CategoryGoal
import com.pulse.budget.data.model.CategoryType
import com.pulse.budget.data.model.CustomCategory
import com.pulse.budget.data.model.DEFAULT_MAX_MONTHLY_GOAL
import com.pulse.budget.data.model.DEFAULT_MIN_MONTHLY_GOAL
import com.pulse.budget.data.model.Expense
import com.pulse.budget.data.model.RecurrenceType
import com.pulse.budget.data.model.SpendGroup
import com.pulse.budget.data.model.UserProfile
import com.pulse.budget.data.repository.AuthRepository
import com.pulse.budget.data.repository.PulseRepository
import com.pulse.budget.util.BadgeStats
import com.pulse.budget.util.CategoryChartRow
import com.pulse.budget.util.GoalTrackPoint
import com.pulse.budget.util.GoalTrackSummary
import com.pulse.budget.util.ImageUtils
import com.pulse.budget.util.buildCategoryChartRows
import com.pulse.budget.util.buildGoalTrack
import com.pulse.budget.util.groupSpend
import com.pulse.budget.util.isDayOverThreshold
import com.pulse.budget.util.qualifyingBadges
import com.pulse.budget.util.summarizeGoalTrack
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val TAG = "ExpenseViewModel"

/** Bar values for the 7-day signal chart: date label to spend amount. */
data class DaySpend(val label: String, val amount: Float, val overLimit: Boolean)

/** User-selectable period for the Expense List and the Categories graph. */
enum class PeriodOption(val label: String) {
    WEEK("Last 7 Days"),
    MONTH("This Month"),
    THREE_MONTHS("Last 3 Months"),
    CUSTOM("Custom")
}

/** An inclusive range of epoch days. */
data class DayRange(val startDay: Long, val endDay: Long) {
    val days: Long get() = endDay - startDay + 1
}

private val SHORT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")

/** Expenses, goals, badges and stats for the signed-in user, read from Cloud Firestore via PulseRepository. */
@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = PulseRepository()

    val sessionUserId: StateFlow<String?> = AuthRepository.authState().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AuthRepository().currentUid
    )

    /** One-off messages for the snackbar (saved, deleted, badge unlocked, errors). */
    private val _userMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val userMessages: SharedFlow<String> = _userMessages.asSharedFlow()

    private fun message(text: String) {
        _userMessages.tryEmit(text)
    }

    // Profile: goals, streak, XP (users/{uid})

    private val profile: StateFlow<UserProfile?> = sessionUserId.flatMapLatest { uid ->
        if (uid == null) flowOf(null) else repo.userProfile(uid)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val streakDays: StateFlow<Int> = profile.map { it?.streakDays ?: 0 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val xp: StateFlow<Int> = profile.map { it?.xp ?: 0 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    /** Maximum monthly spending goal. */
    val monthlyBudget: StateFlow<Float> = profile.map { (it?.maxMonthlyGoal ?: DEFAULT_MAX_MONTHLY_GOAL).toFloat() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, DEFAULT_MAX_MONTHLY_GOAL.toFloat())

    /** Minimum monthly spending goal. */
    val minMonthlyBudget: StateFlow<Float> = profile.map { (it?.minMonthlyGoal ?: DEFAULT_MIN_MONTHLY_GOAL).toFloat() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, DEFAULT_MIN_MONTHLY_GOAL.toFloat())

    // Fixed date windows

    private fun monthRange(): DayRange {
        val today = LocalDate.now()
        return DayRange(today.withDayOfMonth(1).toEpochDay(), today.withDayOfMonth(today.lengthOfMonth()).toEpochDay())
    }

    /** The "past month" window used by the goal tracker: the last 30 days including today. */
    private fun last30Range(): DayRange {
        val today = LocalDate.now().toEpochDay()
        return DayRange(today - 29, today)
    }

    // This calendar month (Dashboard, Goals, Profile)

    val monthExpenses: StateFlow<List<Expense>> = sessionUserId.flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList()) else monthRange().let { repo.expensesInRange(uid, it.startDay, it.endDay) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val monthTotal: StateFlow<Double> = monthExpenses.map { list -> list.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val monthSpendGroups: StateFlow<List<SpendGroup>> = monthExpenses.map { groupSpend(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Category goals, custom categories, badges

    val categoryGoals: StateFlow<List<CategoryGoal>> = sessionUserId.flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList()) else repo.categoryGoals(uid)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val customCategories: StateFlow<List<CustomCategory>> = sessionUserId.flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList()) else repo.customCategories(uid)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val badges: StateFlow<List<Badge>> = sessionUserId.flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList()) else repo.badges(uid)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Turns a goal key back into a readable name (used for goal-only rows on the graph). */
    fun labelForKey(key: String): String =
        CategoryType.entries.firstOrNull { it.name == key }?.label
            ?: customCategories.value.firstOrNull { it.groupKey == key }?.name
            ?: key.removePrefix("custom_").replaceFirstChar { it.uppercase() }

    // User-selectable period (Expense List + Categories graph)

    private val _selectedPeriod = MutableStateFlow(PeriodOption.MONTH)
    val selectedPeriod: StateFlow<PeriodOption> = _selectedPeriod.asStateFlow()

    private val _customRange = MutableStateFlow<DayRange?>(null)

    fun setPeriod(period: PeriodOption) {
        Log.d(TAG, "Period changed to $period")
        _selectedPeriod.value = period
    }

    /** Called by the date-range picker. */
    fun setCustomRange(startDay: Long, endDay: Long) {
        val range = if (startDay <= endDay) DayRange(startDay, endDay) else DayRange(endDay, startDay)
        Log.d(TAG, "Custom period set: ${LocalDate.ofEpochDay(range.startDay)} to ${LocalDate.ofEpochDay(range.endDay)}")
        _customRange.value = range
        _selectedPeriod.value = PeriodOption.CUSTOM
    }

    val periodRange: StateFlow<DayRange> = combine(_selectedPeriod, _customRange) { period, custom ->
        val today = LocalDate.now()
        when (period) {
            PeriodOption.WEEK -> DayRange(today.toEpochDay() - 6, today.toEpochDay())
            PeriodOption.MONTH -> monthRange()
            PeriodOption.THREE_MONTHS -> DayRange(today.minusMonths(3).plusDays(1).toEpochDay(), today.toEpochDay())
            PeriodOption.CUSTOM -> custom ?: monthRange()
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, monthRange())

    /** E.g. "1 Oct – 31 Oct" — shown under the period chips. */
    val periodLabel: StateFlow<String> = periodRange.map {
        "${LocalDate.ofEpochDay(it.startDay).format(SHORT_DATE)} – ${LocalDate.ofEpochDay(it.endDay).format(SHORT_DATE)} (${it.days} days)"
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val periodExpenses: StateFlow<List<Expense>> =
        combine(sessionUserId, periodRange) { uid, range -> uid to range }
            .distinctUntilChanged()
            .flatMapLatest { (uid, range) ->
                if (uid == null) flowOf(emptyList()) else repo.expensesInRange(uid, range.startDay, range.endDay)
            }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Rows for the "amount spent per category" graph over the selected period. */
    val categoryChartRows: StateFlow<List<CategoryChartRow>> =
        combine(periodExpenses, categoryGoals, periodRange, customCategories) { expenses, goals, range, _ ->
            buildCategoryChartRows(groupSpend(expenses), goals, ::labelForKey, range.days)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Past 30 days: goal tracker + 7-day signal

    private val last30Expenses: StateFlow<List<Expense>> = sessionUserId.flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList()) else last30Range().let { repo.expensesInRange(uid, it.startDay, it.endDay) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val last30DailyTotals: StateFlow<Map<Long, Double>> = last30Expenses.map { list ->
        list.groupBy { it.dateEpochDay }.mapValues { (_, items) -> items.sumOf { it.amount } }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /** How well the user stayed between their minimum and maximum goals over the past month. */
    val goalTrack: StateFlow<List<GoalTrackPoint>> =
        combine(last30DailyTotals, minMonthlyBudget, monthlyBudget) { totals, min, max ->
            val range = last30Range()
            buildGoalTrack(totals, range.startDay, range.endDay, min.toDouble(), max.toDouble())
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val goalSummary: StateFlow<GoalTrackSummary> =
        combine(goalTrack, minMonthlyBudget, monthlyBudget) { points, min, max ->
            summarizeGoalTrack(points, min.toDouble(), max.toDouble())
        }.stateIn(viewModelScope, SharingStarted.Eagerly, summarizeGoalTrack(emptyList(), 0.0, 0.0))

    /** 7-day signal on the Dashboard (built from the same 30-day data, no extra query). */
    val sevenDaySignal: StateFlow<List<DaySpend>> =
        combine(last30DailyTotals, monthlyBudget) { totals, budget ->
            val today = LocalDate.now().toEpochDay()
            (today - 6..today).map { day ->
                val amount = totals[day]?.toFloat() ?: 0f
                DaySpend(
                    label = LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("dd")),
                    amount = amount,
                    overLimit = isDayOverThreshold(amount, budget)
                )
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Badge progress

    private fun computeBadgeStats(
        streak: Int,
        month: List<Expense>,
        last30: List<Expense>,
        min: Float,
        max: Float
    ): BadgeStats {
        val today = LocalDate.now().toEpochDay()
        val daysLoggedThisWeek = last30.filter { it.dateEpochDay in (today - 6)..today }
            .map { it.dateEpochDay }.distinct().size
        return BadgeStats(
            streakDays = streak,
            expenseCountThisMonth = month.size,
            loggingDaysThisMonth = month.map { it.dateEpochDay }.distinct().size,
            monthSpend = month.sumOf { it.amount },
            minMonthlyGoal = min.toDouble(),
            maxMonthlyGoal = max.toDouble(),
            hasLoggedReceipt = month.any { it.hasReceipt },
            noSpendDaysThisWeek = 7 - daysLoggedThisWeek
        )
    }

    /** Live stats used by the Badges screen to show "3 / 7 days" style progress for each badge. */
    val badgeStats: StateFlow<BadgeStats> =
        combine(streakDays, monthExpenses, last30Expenses, minMonthlyBudget, monthlyBudget) { streak, month, last30, min, max ->
            computeBadgeStats(streak, month, last30, min, max)
        }.stateIn(
            viewModelScope, SharingStarted.Eagerly,
            BadgeStats(0, 0, 0, 0.0, DEFAULT_MIN_MONTHLY_GOAL, DEFAULT_MAX_MONTHLY_GOAL, false, 7)
        )

    // Session start-up work

    init {
        viewModelScope.launch {
            sessionUserId.collect { uid ->
                if (uid == null) return@collect
                try {
                    repo.seedBadgesIfMissing(uid)
                    // Own feature #2: create any recurring expenses that came due since last time.
                    val created = repo.generateRecurringExpenses(uid, LocalDate.now())
                    if (created > 0) message("Added $created recurring expense${if (created == 1) "" else "s"}")
                } catch (e: Exception) {
                    Log.e(TAG, "Start-up sync failed (offline?)", e)
                }
            }
        }
    }

    // Writes

    fun addExpense(
        amount: Double,
        category: CategoryType,
        dateEpochDay: Long,
        startTime: String = "",
        endTime: String = "",
        description: String,
        receiptUri: Uri?,
        recurrence: RecurrenceType = RecurrenceType.NONE,
        customCategoryName: String? = null
    ) {
        val uid = sessionUserId.value ?: return
        Log.d(TAG, "addExpense: R$amount / $category / ${customCategoryName ?: "-"} / day=$dateEpochDay")
        viewModelScope.launch {
            try {
                val receiptBase64 = receiptUri?.let {
                    ImageUtils.uriToCompressedBase64(getApplication(), it, ImageUtils.RECEIPT_MAX_DIMENSION)
                }
                if (receiptUri != null && receiptBase64 == null) message("Couldn't read that photo — saved without a receipt")

                repo.addExpense(
                    uid,
                    Expense(
                        amount = amount,
                        category = category,
                        customCategoryName = customCategoryName,
                        dateEpochDay = dateEpochDay,
                        startTime = startTime,
                        endTime = endTime,
                        description = description.trim(),
                        recurrence = recurrence,
                        // A template counts as "generated up to" its own date.
                        lastGeneratedEpochDay = if (recurrence != RecurrenceType.NONE) dateEpochDay else null
                    ),
                    receiptBase64
                )
                message("Expense saved to the cloud")

                val today = LocalDate.now().toEpochDay()
                repo.recordLogEvent(uid, today)
                // A recurring expense logged with a past date may already have copies due.
                if (recurrence != RecurrenceType.NONE) repo.generateRecurringExpenses(uid, LocalDate.now())
                evaluateBadges(uid, today, justAddedReceipt = receiptBase64 != null)
            } catch (e: Exception) {
                Log.e(TAG, "addExpense failed", e)
                message("Couldn't save expense: ${e.localizedMessage ?: "unknown error"}")
            }
        }
    }

    fun deleteExpense(expenseId: String) {
        val uid = sessionUserId.value ?: return
        viewModelScope.launch {
            try {
                repo.deleteExpense(uid, expenseId)
                message("Expense deleted")
            } catch (e: Exception) {
                Log.e(TAG, "deleteExpense failed", e)
                message("Couldn't delete: ${e.localizedMessage}")
            }
        }
    }

    /** Loads a receipt photo from Firestore (Base64 JPEG), or null. */
    suspend fun loadReceipt(expenseId: String): String? {
        val uid = sessionUserId.value ?: return null
        return try {
            repo.getReceipt(uid, expenseId)
        } catch (e: Exception) {
            Log.e(TAG, "loadReceipt failed", e)
            null
        }
    }

    /** Badge rules live in util/GamificationRules.kt (own feature #1). */
    private suspend fun evaluateBadges(uid: String, todayEpochDay: Long, justAddedReceipt: Boolean) {
        val stats = computeBadgeStats(
            streakDays.value, monthExpenses.value, last30Expenses.value,
            minMonthlyBudget.value, monthlyBudget.value
        ).let { if (justAddedReceipt) it.copy(hasLoggedReceipt = true) else it }
        val unlocked = repo.unlockBadges(uid, qualifyingBadges(stats), todayEpochDay)
        unlocked.forEach { message("Badge unlocked: $it!") }
    }

    /** Saves the overall monthly minimum and maximum goals (validated by the Goals screen). */
    fun setMonthlyGoals(min: Float, max: Float) {
        val uid = sessionUserId.value ?: return
        Log.d(TAG, "Monthly goals set: min=R$min max=R$max")
        viewModelScope.launch {
            try {
                repo.setMonthlyGoals(uid, min.toDouble(), max.toDouble())
            } catch (e: Exception) {
                Log.e(TAG, "setMonthlyGoals failed", e)
                message("Couldn't save goals: ${e.localizedMessage}")
            }
        }
    }

    /** Saves a category's minimum and maximum monthly goal. */
    fun setCategoryGoal(key: String, min: Double, max: Double) {
        val uid = sessionUserId.value ?: return
        viewModelScope.launch {
            try {
                repo.setCategoryGoal(uid, key, min, max)
                message("Goal saved for ${labelForKey(key)}")
            } catch (e: Exception) {
                Log.e(TAG, "setCategoryGoal failed", e)
                message("Couldn't save goal: ${e.localizedMessage}")
            }
        }
    }

    fun addCustomCategory(name: String) {
        val uid = sessionUserId.value ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                repo.addCustomCategory(uid, name.trim().take(40)) // 40 = limit in firestore.rules
            } catch (e: Exception) {
                Log.e(TAG, "addCustomCategory failed", e)
                message("Couldn't add category: ${e.localizedMessage}")
            }
        }
    }
}
