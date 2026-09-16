package com.pulse.budget.navigation

sealed class PulseDestination(val route: String, val label: String) {
    data object Login : PulseDestination("login", "Login")
    data object Dashboard : PulseDestination("dashboard", "Home")
    data object AddExpense : PulseDestination("add_expense", "Add")
    data object ExpenseList : PulseDestination("expenses", "Expenses")
    data object Categories : PulseDestination("categories", "Categories")
    data object Goals : PulseDestination("goals", "Goals")
    data object Badges : PulseDestination("badges", "Badges")
    data object Profile : PulseDestination("profile", "Profile")
}

val bottomNavDestinations = listOf(
    PulseDestination.Dashboard,
    PulseDestination.ExpenseList,
    PulseDestination.Categories,
    PulseDestination.Goals,
    PulseDestination.Badges,
    PulseDestination.Profile
)