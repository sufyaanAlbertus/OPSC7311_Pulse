package com.pulse.budget.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pulse.budget.data.model.Expense
import com.pulse.budget.data.model.RecurrenceType
import com.pulse.budget.ui.components.Base64Image
import com.pulse.budget.ui.components.CategoryIcon
import com.pulse.budget.ui.components.PeriodSelector
import com.pulse.budget.ui.components.PulseTopBar
import com.pulse.budget.ui.theme.MonoBody
import com.pulse.budget.ui.theme.MonoSmall
import com.pulse.budget.util.formatZar
import com.pulse.budget.viewmodel.ExpenseViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DAY_HEADER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy")

@Composable
fun ExpenseListScreen(viewModel: ExpenseViewModel, isDark: Boolean, onToggleTheme: () -> Unit) {
    val expenses by viewModel.periodExpenses.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val periodLabel by viewModel.periodLabel.collectAsState()
    val grouped = remember(expenses) { expenses.groupBy { it.dateEpochDay }.toSortedMap(compareByDescending { it }) }
    var openExpense by remember { mutableStateOf<Expense?>(null) }

    Scaffold(topBar = { PulseTopBar(title = "Expenses", isDark = isDark, onToggleTheme = onToggleTheme) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            PeriodSelector(
                selected = selectedPeriod,
                periodLabel = periodLabel,
                onSelect = viewModel::setPeriod,
                onCustomRange = viewModel::setCustomRange
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${expenses.size} expenses", style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Total ${formatZar(expenses.sumOf { it.amount })}", style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (expenses.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "No expenses logged for this period.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    grouped.forEach { (day, dayExpenses) ->
                        item(key = "header_$day") {
                            Spacer(Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(LocalDate.ofEpochDay(day).format(DAY_HEADER).uppercase(), style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatZar(dayExpenses.sumOf { it.amount }), style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                        items(dayExpenses, key = { it.id }) { expense ->
                            ExpenseRow(expense = expense, onClick = { openExpense = expense })
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }

    openExpense?.let { expense ->
        ExpenseDetailDialog(
            expense = expense,
            viewModel = viewModel,
            onDismiss = { openExpense = null },
            onDelete = {
                viewModel.deleteExpense(expense.id)
                openExpense = null
            }
        )
    }
}

@Composable
private fun ExpenseRow(expense: Expense, onClick: () -> Unit) {
    val timeRange = when {
        expense.startTime.isNotBlank() && expense.endTime.isNotBlank() -> "${expense.startTime}–${expense.endTime}"
        expense.startTime.isNotBlank() -> expense.startTime
        else -> null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            CategoryIcon(category = expense.category, size = 36.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(expense.categoryLabel, style = MaterialTheme.typography.bodyMedium)
                if (expense.description.isNotBlank()) {
                    Text(
                        expense.description,
                        style = MonoSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val tags = listOfNotNull(
                    timeRange,
                    when (expense.recurrence) {
                        RecurrenceType.WEEKLY -> "Repeats weekly"
                        RecurrenceType.MONTHLY -> "Repeats monthly"
                        RecurrenceType.NONE -> null
                    },
                    if (expense.isAutoGenerated) "Auto-added" else null
                )
                if (tags.isNotEmpty()) {
                    Text(tags.joinToString(" · "), style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(formatZar(expense.amount), style = MonoBody)
            if (expense.recurrence != RecurrenceType.NONE || expense.isAutoGenerated) {
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Outlined.Autorenew, contentDescription = "Recurring", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
            if (expense.hasReceipt) {
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Outlined.AttachFile, contentDescription = "Receipt attached", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/** Expense details + receipt photo (downloaded from Firestore on demand) + delete. */
@Composable
private fun ExpenseDetailDialog(
    expense: Expense,
    viewModel: ExpenseViewModel,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }

    // null = still loading; "" = no receipt / couldn't load.
    val receipt by produceState<String?>(initialValue = if (expense.hasReceipt) null else "", expense.id) {
        if (expense.hasReceipt) value = viewModel.loadReceipt(expense.id) ?: ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(0.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(expense.categoryLabel) },
        text = {
            Column {
                Text(formatZar(expense.amount), style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(6.dp))
                Text(LocalDate.ofEpochDay(expense.dateEpochDay).format(DAY_HEADER), style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (expense.startTime.isNotBlank()) {
                    Text(
                        "Time: ${expense.startTime}${if (expense.endTime.isNotBlank()) " – ${expense.endTime}" else ""}",
                        style = MonoSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (expense.description.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(expense.description, style = MaterialTheme.typography.bodyMedium)
                }
                if (expense.isRecurringTemplate) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Repeats ${expense.recurrence.name.lowercase()}",
                        style = MonoSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (expense.hasReceipt) {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 320.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (val r = receipt) {
                            null -> CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            "" -> Text("Receipt couldn't be loaded.", style = MonoSmall, color = MaterialTheme.colorScheme.error)
                            else -> Base64Image(
                                base64 = r,
                                contentDescription = "Receipt photo",
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
                if (confirmDelete) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Delete this expense?",
                        style = MonoSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = MaterialTheme.colorScheme.primary) }
        },
        dismissButton = {
            TextButton(onClick = { if (confirmDelete) onDelete() else confirmDelete = true }) {
                Text(if (confirmDelete) "Confirm delete" else "Delete", color = MaterialTheme.colorScheme.error)
            }
        }
    )
}
