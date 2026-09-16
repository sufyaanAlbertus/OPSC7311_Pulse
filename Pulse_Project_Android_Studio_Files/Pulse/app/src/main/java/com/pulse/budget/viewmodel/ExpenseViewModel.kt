package com.pulse.budget.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.budget.PrefsRepository
import com.pulse.budget.data.local.AppDatabase
import com.pulse.budget.data.local.CategoryTotal
import com.pulse.budget.data.local.CategoryType
import com.pulse.budget.data.local.ExpenseEntity
import com.pulse.budget.data.local.RecurrenceType
import com.pulse.budget.data.repository.PulseRepository
import com.pulse.budget.util.isDayOverThreshold
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Bar values for the 7-day signal chart: date label to spend amount. */
data class DaySpend(val label: String, val amount: Float, val overLimit: Boolean)

/** Part 2 brief: "view the list ... during a user-selectable period" — applies to the Expense
 *  List and Category Breakdown screens (the Dashboard's "This Month" card stays fixed to the
 *  current month by design, since that's its whole purpose). */
enum class PeriodOption(val label: String) { WEEK("This Week"), MONTH("This Month") }

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repo = PulseRepository(db)
    private val prefs = PrefsRepository(application)

    /**
     * Drives every flow below. All Room queries and DataStore keys are scoped by userId, and
     * every StateFlow here re-derives from this whenever it changes (login, logout, or logging
     * into a different account) — so no screen can ever be left showing the previous account's
     * data. This is what was missing before: the old version built its flows once, at
     * ViewModel construction, with no notion of "which account" at all.
     */
    val sessionUserId: StateFlow<Long?> = prefs.sessionUserId.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    val streakDays: StateFlow<Int> = sessionUserId.flatMapLatest { id ->
        id?.let { prefs.streakDays(it) } ?: flowOf(0)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val xp: StateFlow<Int> = sessionUserId.flatMapLatest { id ->
        id?.let { prefs.xp(it) } ?: flowOf(0)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val monthlyBudget: StateFlow<Float> = sessionUserId.flatMapLatest { id ->
        id?.let { prefs.monthlyBudget(it) } ?: flowOf(10000f)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 10000f)

    val minMonthlyBudget: StateFlow<Float> = sessionUserId.flatMapLatest { id ->
        id?.let { prefs.minMonthlyBudget(it) } ?: flowOf(2000f)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 2000f)

    private fun monthRange(): Pair<Long, Long> {
        val today = LocalDate.now()
        val start = today.withDayOfMonth(1).toEpochDay()
        val end = today.withDayOfMonth(today.lengthOfMonth()).toEpochDay()
        return start to end
    }

    private fun sevenDayRange(): Pair<Long, Long> {
        val todayEpoch = LocalDate.now().toEpochDay()
        return (todayEpoch - 6) to todayEpoch
    }

    val monthTotal: StateFlow<Double> = sessionUserId.flatMapLatest { id ->
        if (id == null) flowOf(0.0) else {
            val (s, e) = monthRange()
            repo.totalInRange(id, s, e)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val monthExpenses: StateFlow<List<ExpenseEntity>> = sessionUserId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else {
            val (s, e) = monthRange()
            repo.expensesInRange(id, s, e)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val categoryTotals: StateFlow<List<CategoryTotal>> = sessionUserId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else {
            val (s, e) = monthRange()
            repo.categoryTotalsInRange(id, s, e)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val categoryLimits = sessionUserId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repo.categoryLimits(id)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // --- User-selectable period, for Expense List and Category Breakdown ---

    private val _selectedPeriod = MutableStateFlow(PeriodOption.MONTH)
    val selectedPeriod: StateFlow<PeriodOption> = _selectedPeriod.asStateFlow()

    fun setPeriod(period: PeriodOption) {
        Log.d("ExpenseViewModel", "Period changed to $period")
        _selectedPeriod.value = period
    }

    private fun rangeFor(period: PeriodOption): Pair<Long, Long> = when (period) {
        PeriodOption.WEEK -> sevenDayRange()
        PeriodOption.MONTH -> monthRange()
    }

    val periodExpenses: StateFlow<List<ExpenseEntity>> =
        combine(sessionUserId, _selectedPeriod) { id, period -> id to period }
            .flatMapLatest { (id, period) ->
                if (id == null) flowOf(emptyList()) else {
                    val (s, e) = rangeFor(period)
                    repo.expensesInRange(id, s, e)
                }
            }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val periodCategoryTotals: StateFlow<List<CategoryTotal>> =
        combine(sessionUserId, _selectedPeriod) { id, period -> id to period }
            .flatMapLatest { (id, period) ->
                if (id == null) flowOf(emptyList()) else {
                    val (s, e) = rangeFor(period)
                    repo.categoryTotalsInRange(id, s, e)
                }
            }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val customCategories = sessionUserId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repo.customCategories(id)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun addCustomCategory(name: String) {
        val userId = sessionUserId.value ?: return
        if (name.isBlank()) return
        viewModelScope.launch { repo.addCustomCategory(userId, name) }
    }

    val badges = sessionUserId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repo.badges(id)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Real seven-day signal, built from the daily totals query rather than placeholder data.
     * A day is flagged "over" when its spend exceeds 1.5x the average implied daily budget
     * (monthlyBudget / 30) — there's no per-day limit concept in the data model, so this is
     * the simplest meaningful threshold rather than an arbitrary fixed rand amount.
     */
    val sevenDaySignal: StateFlow<List<DaySpend>> =
        combine(sessionUserId, monthlyBudget) { id, budget -> id to budget }
            .flatMapLatest { (id, budget) ->
                if (id == null) {
                    flowOf(emptyList())
                } else {
                    val (s, e) = sevenDayRange()
                    repo.dailyTotalsInRange(id, s, e).map { totals ->
                        val totalsByDay = totals.associate { it.day to it.total }
                        (s..e).map { day ->
                            val amount = totalsByDay[day]?.toFloat() ?: 0f
                            val label = LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("dd"))
                            DaySpend(label = label, amount = amount, overLimit = isDayOverThreshold(amount, budget))
                        }
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        // Seed the badge catalog for whichever account is active. Runs again (as a harmless
        // no-op, thanks to OnConflictStrategy.IGNORE) every time the session user changes.
        viewModelScope.launch {
            sessionUserId.collect { id ->
                if (id != null) repo.seedBadgesIfEmpty(id)
            }
        }
    }

    fun addExpense(
        amount: Double,
        category: CategoryType,
        dateEpochDay: Long,
        startTime: String = "",
        endTime: String = "",
        description: String,
        receiptUri: String?,
        recurrence: RecurrenceType = RecurrenceType.NONE,
        customCategoryName: String? = null
    ) {
        val userId = sessionUserId.value ?: return
        Log.d("ExpenseViewModel", "addExpense called: R$amount / $category / day=$dateEpochDay")
        viewModelScope.launch {
            repo.addExpense(
                ExpenseEntity(
                    userId = userId,
                    amount = amount,
                    category = category,
                    dateEpochDay = dateEpochDay,
                    startTime = startTime,
                    endTime = endTime,
                    description = description,
                    receiptUri = receiptUri,
                    recurrence = recurrence,
                    customCategoryName = customCategoryName
                )
            )
            prefs.recordLogEvent(userId, dateEpochDay)

            val (weekStart, weekEnd) = sevenDayRange()
            val daysWithData = repo.daysWithExpenses(userId, weekStart, weekEnd).first()
            val noSpendDays = 7 - daysWithData.size

            repo.evaluateBadges(
                userId = userId,
                streakDays = streakDays.value,
                expenseCount = monthExpenses.value.size + 1,
                underBudgetDaysThisMonth = monthExpenses.value.map { it.dateEpochDay }.distinct().size,
                totalMonthSpend = monthTotal.value + amount,
                monthlyBudget = monthlyBudget.value.toDouble(),
                hasLoggedReceipt = receiptUri != null,
                noSpendDaysThisWeek = noSpendDays
            )
        }
    }

    fun setMonthlyBudget(amount: Float) {
        val userId = sessionUserId.value ?: return
        Log.d("ExpenseViewModel", "Monthly budget (max) set to R$amount")
        viewModelScope.launch { prefs.setMonthlyBudget(userId, amount) }
    }

    fun setMinMonthlyBudget(amount: Float) {
        val userId = sessionUserId.value ?: return
        Log.d("ExpenseViewModel", "Monthly budget (min) set to R$amount")
        viewModelScope.launch { prefs.setMinMonthlyBudget(userId, amount) }
    }

    fun setCategoryLimit(category: CategoryType, limit: Double) {
        val userId = sessionUserId.value ?: return
        viewModelScope.launch { repo.setCategoryLimit(userId, category, limit) }
    }
}