package com.pulse.budget.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pulse.budget.data.local.ExpenseEntity
import com.pulse.budget.ui.components.CategoryIcon
import com.pulse.budget.ui.components.label
import com.pulse.budget.ui.components.PulseTopBar
import com.pulse.budget.ui.theme.MonoBody
import com.pulse.budget.ui.theme.MonoSmall
import com.pulse.budget.util.formatZar
import com.pulse.budget.viewmodel.ExpenseViewModel
import com.pulse.budget.viewmodel.PeriodOption
import java.time.LocalDate

@Composable
fun ExpenseListScreen(viewModel: ExpenseViewModel, isDark: Boolean, onToggleTheme: () -> Unit) {
    val expenses by viewModel.periodExpenses.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val grouped = expenses.groupBy { it.dateEpochDay }.toSortedMap(compareByDescending { it })

    Scaffold(topBar = { PulseTopBar(title = "Expenses", isDark = isDark, onToggleTheme = onToggleTheme) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            PeriodSelector(selected = selectedPeriod, onSelect = viewModel::setPeriod)
            Spacer(Modifier.height(8.dp))

            if (expenses.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "No expenses logged for ${selectedPeriod.label.lowercase()}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    grouped.forEach { (day, dayExpenses) ->
                        item {
                            Spacer(Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(LocalDate.ofEpochDay(day).toString(), style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatZar(dayExpenses.sumOf { it.amount }), style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                        items(dayExpenses) { expense -> ExpenseRow(expense) }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

/** Part 2 brief: "view the list ... during a user-selectable period." */
@Composable
private fun PeriodSelector(selected: PeriodOption, onSelect: (PeriodOption) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PeriodOption.entries.forEach { option ->
            val isSelected = option == selected
            Text(
                text = option.label.uppercase(),
                style = MonoSmall,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(0.dp)
                    )
                    .clickable { onSelect(option) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun ExpenseRow(expense: ExpenseEntity) {
    val context = LocalContext.current
    val displayLabel = expense.customCategoryName ?: expense.category.label()
    val timeRange = if (expense.startTime.isNotBlank() || expense.endTime.isNotBlank()) {
        "${expense.startTime}–${expense.endTime}"
    } else null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .let { base ->
                if (expense.receiptUri != null) {
                    base.clickable {
                        runCatching {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(Uri.parse(expense.receiptUri), "image/*")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(intent)
                        }
                    }
                } else base
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryIcon(category = expense.category, size = 36.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(displayLabel, style = MaterialTheme.typography.bodyMedium)
                if (expense.description.isNotBlank()) {
                    Text(expense.description, style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (timeRange != null) {
                    Text(timeRange, style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(formatZar(expense.amount), style = MonoBody)
            if (expense.receiptUri != null) {
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Outlined.AttachFile, contentDescription = "Receipt attached — tap to view", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.height(16.dp))
            }
        }
    }
}
