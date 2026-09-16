package com.pulse.budget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pulse.budget.navigation.PulseDestination
import com.pulse.budget.navigation.bottomNavDestinations
import com.pulse.budget.ui.screens.AddExpenseScreen
import com.pulse.budget.ui.screens.BadgesScreen
import com.pulse.budget.ui.screens.CategoryBreakdownScreen
import com.pulse.budget.ui.screens.DashboardScreen
import com.pulse.budget.ui.screens.ExpenseListScreen
import com.pulse.budget.ui.screens.GoalsScreen
import com.pulse.budget.ui.screens.LoginScreen
import com.pulse.budget.ui.screens.ProfileScreen
import com.pulse.budget.ui.theme.PulseTheme
import com.pulse.budget.viewmodel.AuthViewModel
import com.pulse.budget.viewmodel.ExpenseViewModel
import com.pulse.budget.viewmodel.ThemeViewModel

class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()
    private val expenseViewModel: ExpenseViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDark by themeViewModel.isDarkTheme.collectAsState()
            PulseTheme(darkTheme = isDark) {
                PulseApp(
                    isDark = isDark,
                    onToggleTheme = { themeViewModel.toggleTheme() },
                    expenseViewModel = expenseViewModel,
                    authViewModel = authViewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulseApp(
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    expenseViewModel: ExpenseViewModel,
    authViewModel: AuthViewModel
) {
    val navController: NavHostController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val sessionUserId by authViewModel.sessionUserId.collectAsState()

    // Session-driven redirects: a restored session skips straight past Login, and a
    // logout() call (from the Profile screen) bounces the user back to Login from
    // wherever they were, clearing the back stack either way.
    LaunchedEffect(sessionUserId, currentRoute) {
        if (currentRoute == null) return@LaunchedEffect
        if (sessionUserId != null && currentRoute == PulseDestination.Login.route) {
            navController.navigate(PulseDestination.Dashboard.route) {
                popUpTo(PulseDestination.Login.route) { inclusive = true }
            }
        } else if (sessionUserId == null && currentRoute != PulseDestination.Login.route) {
            navController.navigate(PulseDestination.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val showBottomBar = currentRoute != PulseDestination.Login.route && currentRoute != PulseDestination.AddExpense.route

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    bottomNavDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(PulseDestination.Dashboard.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                            icon = { Icon(destination.icon(), contentDescription = destination.label) },
                            label = { Text(destination.label.uppercase(), style = MaterialTheme.typography.labelSmall) },
                            // Matches the app's electric-blue accent instead of Material3's
                            // default purple/pink selection color.
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = PulseDestination.Login.route,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
        ) {
            composable(PulseDestination.Login.route) {
                LoginScreen(
                    authViewModel = authViewModel,
                    onLoginSuccess = {
                        navController.navigate(PulseDestination.Dashboard.route) {
                            popUpTo(PulseDestination.Login.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(PulseDestination.Dashboard.route) {
                DashboardScreen(
                    viewModel = expenseViewModel,
                    authViewModel = authViewModel,
                    isDark = isDark,
                    onToggleTheme = onToggleTheme,
                    onAddExpense = { navController.navigate(PulseDestination.AddExpense.route) }
                )
            }
            composable(PulseDestination.AddExpense.route) {
                AddExpenseScreen(
                    viewModel = expenseViewModel,
                    isDark = isDark,
                    onToggleTheme = onToggleTheme,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(PulseDestination.ExpenseList.route) {
                ExpenseListScreen(viewModel = expenseViewModel, isDark = isDark, onToggleTheme = onToggleTheme)
            }
            composable(PulseDestination.Categories.route) {
                CategoryBreakdownScreen(viewModel = expenseViewModel, isDark = isDark, onToggleTheme = onToggleTheme)
            }
            composable(PulseDestination.Goals.route) {
                GoalsScreen(viewModel = expenseViewModel, isDark = isDark, onToggleTheme = onToggleTheme)
            }
            composable(PulseDestination.Badges.route) {
                BadgesScreen(viewModel = expenseViewModel, isDark = isDark, onToggleTheme = onToggleTheme)
            }
            composable(PulseDestination.Profile.route) {
                ProfileScreen(
                    authViewModel = authViewModel,
                    expenseViewModel = expenseViewModel,
                    isDark = isDark,
                    onToggleTheme = onToggleTheme
                )
            }
        }
    }
}

private fun PulseDestination.icon() = when (this) {
    PulseDestination.Dashboard -> Icons.Outlined.Home
    PulseDestination.ExpenseList -> Icons.Outlined.List
    PulseDestination.Categories -> Icons.Outlined.Category
    PulseDestination.Goals -> Icons.Outlined.TrackChanges
    PulseDestination.Badges -> Icons.Outlined.EmojiEvents
    PulseDestination.Profile -> Icons.Outlined.Person
    else -> Icons.Outlined.Home
}