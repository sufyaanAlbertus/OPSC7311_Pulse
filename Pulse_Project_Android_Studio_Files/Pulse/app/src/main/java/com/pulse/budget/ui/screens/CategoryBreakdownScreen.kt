package com.pulse.budget.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pulse.budget.ui.components.CategorySpendChart
import com.pulse.budget.ui.components.PeriodSelector
import com.pulse.budget.ui.components.PulseTopBar
import com.pulse.budget.ui.components.SharpCard
import com.pulse.budget.ui.theme.MonoDisplay
import com.pulse.budget.util.formatZar
import com.pulse.budget.viewmodel.ExpenseViewModel

/** Graph of the amount spent per category over a user-selectable period. */
@Composable
fun CategoryBreakdownScreen(viewModel: ExpenseViewModel, isDark: Boolean, onToggleTheme: () -> Unit) {
    val rows by viewModel.categoryChartRows.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val periodLabel by viewModel.periodLabel.collectAsState()
    val total = rows.sumOf { it.total }

    Scaffold(topBar = { PulseTopBar(title = "Categories", isDark = isDark, onToggleTheme = onToggleTheme) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            PeriodSelector(
                selected = selectedPeriod,
                periodLabel = periodLabel,
                onSelect = viewModel::setPeriod,
                onCustomRange = viewModel::setCustomRange
            )
            Spacer(Modifier.height(12.dp))

            SharpCard {
                Text("Total Spend — ${selectedPeriod.label}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(formatZar(total), style = MonoDisplay)
            }
            Spacer(Modifier.height(16.dp))

            SharpCard {
                Text("Spending per category", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(12.dp))
                if (rows.isEmpty()) {
                    Text(
                        "No spending in this period.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    CategorySpendChart(rows = rows)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
