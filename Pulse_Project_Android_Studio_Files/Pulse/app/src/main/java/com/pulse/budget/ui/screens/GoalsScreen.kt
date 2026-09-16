package com.pulse.budget.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pulse.budget.data.local.CategoryType
import com.pulse.budget.ui.components.CategoryIcon
import com.pulse.budget.ui.components.label
import com.pulse.budget.ui.components.PulsePrimaryButton
import com.pulse.budget.ui.components.PulseTextField
import com.pulse.budget.ui.components.PulseTopBar
import com.pulse.budget.ui.components.SharpCard
import com.pulse.budget.ui.components.SharpProgressBar
import com.pulse.budget.ui.theme.MonoBody
import com.pulse.budget.ui.theme.MonoDisplay
import com.pulse.budget.ui.theme.MonoSmall
import com.pulse.budget.util.formatZar
import com.pulse.budget.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(viewModel: ExpenseViewModel, isDark: Boolean, onToggleTheme: () -> Unit) {
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()
    val minMonthlyBudget by viewModel.minMonthlyBudget.collectAsState()
    val categoryTotals by viewModel.categoryTotals.collectAsState()
    val limits by viewModel.categoryLimits.collectAsState()
    var editingBudget by remember { mutableStateOf(false) }
    var budgetInput by remember { mutableStateOf(monthlyBudget.toString()) }
    var editingCategory by remember { mutableStateOf<CategoryType?>(null) }

    // Local, uncommitted slider state — only pushed to the ViewModel on release, so dragging
    // doesn't spam DataStore writes on every pixel of movement.
    var sliderRange by remember(minMonthlyBudget, monthlyBudget) {
        mutableStateOf(minMonthlyBudget..monthlyBudget)
    }

    Scaffold(topBar = { PulseTopBar(title = "Goals & Limits", isDark = isDark, onToggleTheme = onToggleTheme) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(16.dp)
        ) {
            SharpCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Monthly Budget (Max)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (editingBudget) {
                            Spacer(Modifier.height(6.dp))
                            PulseTextField(
                                value = budgetInput,
                                onValueChange = { budgetInput = it },
                                label = "Amount (R)",
                                isMonospace = true
                            )
                        } else {
                            Text(formatZar(monthlyBudget), style = MonoDisplay)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        if (editingBudget) {
                            viewModel.setMonthlyBudget(budgetInput.toFloatOrNull() ?: monthlyBudget)
                        } else {
                            budgetInput = monthlyBudget.toString()
                        }
                        editingBudget = !editingBudget
                    }) { Text(if (editingBudget) "Save" else "Edit") }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    "Min ${formatZar(sliderRange.start)}  ·  Max ${formatZar(sliderRange.endInclusive)}",
                    style = MonoBody,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Part 2 brief asks for both a minimum and a maximum monthly goal, and for a
                // SeekBar-equivalent control — a RangeSlider covers both requirements at once.
                RangeSlider(
                    value = sliderRange,
                    onValueChange = { sliderRange = it },
                    onValueChangeFinished = {
                        viewModel.setMinMonthlyBudget(sliderRange.start)
                        viewModel.setMonthlyBudget(sliderRange.endInclusive)
                    },
                    valueRange = 0f..30000f,
                    steps = 59
                )
            }
            Spacer(Modifier.height(20.dp))
            Text("Category Limits", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))

            LazyColumn {
                items(CategoryType.entries) { category ->
                    val limit = limits.firstOrNull { it.category == category }?.limit ?: 0.0
                    val spent = categoryTotals.firstOrNull { it.category == category }?.total ?: 0.0
                    val progress = if (limit > 0) (spent / limit).toFloat() else 0f

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .clickable { editingCategory = category },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryIcon(category = category, size = 36.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(category.label(), style = MaterialTheme.typography.bodyMedium)
                                Text(formatZar(limit), style = MonoBody)
                            }
                            Spacer(Modifier.height(4.dp))
                            SharpProgressBar(progress = progress, overLimit = limit > 0 && spent > limit)
                        }
                    }
                }
            }
        }
    }

    // Tap-to-edit dialog for a single category's limit.
    val category = editingCategory
    if (category != null) {
        var limitInput by remember(category) {
            mutableStateOf((limits.firstOrNull { it.category == category }?.limit ?: 0.0).toString())
        }
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = { Text(category.label() + " limit") },
            text = {
                PulseTextField(
                    value = limitInput,
                    onValueChange = { limitInput = it },
                    label = "Limit (R)",
                    isMonospace = true
                )
            },
            confirmButton = {
                PulsePrimaryButton(
                    text = "Save",
                    onClick = {
                        viewModel.setCategoryLimit(category, limitInput.toDoubleOrNull() ?: 0.0)
                        editingCategory = null
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { editingCategory = null }) { Text("Cancel") }
            }
        )
    }
}
