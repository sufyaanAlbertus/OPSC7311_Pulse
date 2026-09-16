package com.pulse.budget.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pulse.budget.ui.components.label
import com.pulse.budget.ui.components.PulseTopBar
import com.pulse.budget.ui.components.SharpCard
import com.pulse.budget.ui.components.SharpProgressBar
import com.pulse.budget.ui.theme.MonoBody
import com.pulse.budget.ui.theme.MonoDisplay
import com.pulse.budget.ui.theme.MonoSmall
import com.pulse.budget.util.formatZar
import com.pulse.budget.viewmodel.ExpenseViewModel
import com.pulse.budget.viewmodel.PeriodOption

@Composable
fun CategoryBreakdownScreen(viewModel: ExpenseViewModel, isDark: Boolean, onToggleTheme: () -> Unit) {
    val categoryTotals by viewModel.periodCategoryTotals.collectAsState()
    val limits by viewModel.categoryLimits.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val total = categoryTotals.sumOf { it.total }

    Scaffold(topBar = { PulseTopBar(title = "Categories", isDark = isDark, onToggleTheme = onToggleTheme) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PeriodOption.entries.forEach { option ->
                    val isSelected = option == selectedPeriod
                    Text(
                        text = option.label.uppercase(),
                        style = MonoSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                            .clickable { viewModel.setPeriod(option) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            SharpCard {
                Text("Total Spend — ${selectedPeriod.label}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(formatZar(total), style = MonoDisplay)
            }
            Spacer(Modifier.height(16.dp))

            if (categoryTotals.isEmpty()) {
                Text(
                    "No spending for ${selectedPeriod.label.lowercase()}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn {
                    items(categoryTotals) { ct ->
                        val limit = limits.firstOrNull { it.category == ct.category }?.limit ?: 0.0
                        val progress = if (limit > 0) (ct.total / limit).toFloat() else 0f
                        val overLimit = limit > 0 && ct.total > limit

                        Column(modifier = Modifier.padding(vertical = 10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(ct.category.label(), style = MaterialTheme.typography.bodyMedium)
                                Text(formatZar(ct.total), style = MonoBody, color = if (overLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(Modifier.height(6.dp))
                            SharpProgressBar(progress = progress, overLimit = overLimit)
                            if (limit > 0) {
                                Spacer(Modifier.height(2.dp))
                                Text("${(progress * 100).toInt()}%", style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
