package com.pulse.budget.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulse.budget.data.model.CategoryType
import com.pulse.budget.ui.components.CategoryIcon
import com.pulse.budget.ui.components.GoalScoreRing
import com.pulse.budget.ui.components.GoalTrackerChart
import com.pulse.budget.ui.components.PulsePrimaryButton
import com.pulse.budget.ui.components.PulseTextField
import com.pulse.budget.ui.components.PulseTopBar
import com.pulse.budget.ui.components.SharpCard
import com.pulse.budget.ui.components.SharpProgressBar
import com.pulse.budget.ui.components.statusColor
import com.pulse.budget.ui.components.statusLabel
import com.pulse.budget.ui.theme.MonoBody
import com.pulse.budget.ui.theme.MonoSmall
import com.pulse.budget.ui.theme.OnTrackGreen
import com.pulse.budget.util.GoalStatus
import com.pulse.budget.util.GoalTrackSummary
import com.pulse.budget.util.InputValidation
import com.pulse.budget.util.formatZar
import com.pulse.budget.util.goalStatus
import com.pulse.budget.viewmodel.ExpenseViewModel
import kotlin.math.roundToInt

private data class GoalRowItem(val key: String, val label: String, val category: CategoryType)

private val TitleStyle: TextStyle @Composable get() = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)

/** Monthly min/max goals, the 30-day goal tracker and per-category goals. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(viewModel: ExpenseViewModel, isDark: Boolean, onToggleTheme: () -> Unit) {
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()
    val minMonthlyBudget by viewModel.minMonthlyBudget.collectAsState()
    val spendGroups by viewModel.monthSpendGroups.collectAsState()
    val categoryGoals by viewModel.categoryGoals.collectAsState()
    val customCategories by viewModel.customCategories.collectAsState()
    val goalTrack by viewModel.goalTrack.collectAsState()
    val summary by viewModel.goalSummary.collectAsState()

    var editingRow by remember { mutableStateOf<GoalRowItem?>(null) }

    // Unsaved copies of the goals, saved only when the user taps Save.
    var minInput by remember { mutableStateOf("") }
    var maxInput by remember { mutableStateOf("") }
    var sliderRange by remember { mutableStateOf(0f..0f) }
    var monthlyError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(minMonthlyBudget, monthlyBudget) {
        minInput = minMonthlyBudget.roundToInt().toString()
        maxInput = monthlyBudget.roundToInt().toString()
        sliderRange = minMonthlyBudget..monthlyBudget
    }
    val sliderMax = maxOf(30000f, monthlyBudget * 1.5f)
    val typedMin = InputValidation.parseGoal(minInput)
    val typedMax = InputValidation.parseGoal(maxInput)
    val hasUnsavedChanges = typedMin?.toFloat() != minMonthlyBudget || typedMax?.toFloat() != monthlyBudget

    val rows = remember(customCategories) {
        CategoryType.entries.map { GoalRowItem(it.name, it.label, it) } +
                customCategories.map { GoalRowItem(it.groupKey, it.name, CategoryType.OTHER) }
    }

    Scaffold(topBar = { PulseTopBar(title = "Goals", isDark = isDark, onToggleTheme = onToggleTheme) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Monthly spending range
            item {
                Spacer(Modifier.height(12.dp))
                SharpCard {
                    Text("Monthly budget", style = TitleStyle)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PulseTextField(
                            value = minInput,
                            onValueChange = { minInput = it; monthlyError = null },
                            label = "Minimum (R)",
                            isMonospace = true,
                            isNumber = true,
                            modifier = Modifier.weight(1f)
                        )
                        PulseTextField(
                            value = maxInput,
                            onValueChange = { maxInput = it; monthlyError = null },
                            label = "Maximum (R)",
                            isMonospace = true,
                            isNumber = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    RangeSlider(
                        value = sliderRange,
                        onValueChange = {
                            val snapped = ((it.start / 100f).roundToInt() * 100f)..((it.endInclusive / 100f).roundToInt() * 100f)
                            sliderRange = snapped
                            minInput = snapped.start.roundToInt().toString()
                            maxInput = snapped.endInclusive.roundToInt().toString()
                            monthlyError = null
                        },
                        valueRange = 0f..sliderMax
                    )
                    monthlyError?.let {
                        Text(it, style = MonoSmall, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                    }
                    if (hasUnsavedChanges) {
                        PulsePrimaryButton(
                            text = "Save",
                            onClick = {
                                val error = InputValidation.validateGoalRange(typedMin, typedMax)
                                    ?: if ((typedMax ?: 0.0) <= 0.0) "Maximum must be more than R0." else null
                                if (error != null) monthlyError = error
                                else viewModel.setMonthlyGoals(typedMin!!.toFloat(), typedMax!!.toFloat())
                            }
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = OnTrackGreen, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Saved", style = MonoSmall, color = OnTrackGreen)
                        }
                    }
                }
            }

            // Last 30 days
            item {
                Spacer(Modifier.height(16.dp))
                GoalTrackerCard(
                    summary = summary,
                    min = minMonthlyBudget.toDouble(),
                    max = monthlyBudget.toDouble(),
                    chart = { GoalTrackerChart(points = goalTrack) }
                )
            }

            // Category goals
            item {
                Spacer(Modifier.height(20.dp))
                Text("Category goals", style = TitleStyle)
                Spacer(Modifier.height(8.dp))
            }

            items(rows, key = { it.key }) { row ->
                val goal = categoryGoals.firstOrNull { it.key == row.key }
                val min = goal?.minGoal ?: 0.0
                val max = goal?.maxGoal ?: 0.0
                val spent = spendGroups.firstOrNull { it.key == row.key }?.total ?: 0.0
                val status = goalStatus(spent, min, max)
                val progress = if (max > 0) (spent / max).toFloat() else 0f

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editingRow = row }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryIcon(category = row.category, size = 36.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(row.label, style = MaterialTheme.typography.bodyMedium)
                            Text(formatZar(spent), style = MonoBody)
                        }
                        Spacer(Modifier.height(4.dp))
                        SharpProgressBar(progress = progress, overLimit = status == GoalStatus.OVER_MAX)
                        Spacer(Modifier.height(2.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                if (goal == null) "Set goal" else "${formatZar(min)} – ${formatZar(max)}",
                                style = MonoSmall,
                                color = if (goal == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (goal != null) {
                                Text(statusLabel(status).uppercase(), style = MonoSmall, color = statusColor(status))
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    val row = editingRow
    if (row != null) {
        val existing = categoryGoals.firstOrNull { it.key == row.key }
        var minText by remember(row) { mutableStateOf(existing?.minGoal?.takeIf { it > 0 }?.roundToInt()?.toString() ?: "") }
        var maxText by remember(row) { mutableStateOf(existing?.maxGoal?.takeIf { it > 0 }?.roundToInt()?.toString() ?: "") }
        var error by remember(row) { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { editingRow = null },
            shape = RoundedCornerShape(0.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(row.label) },
            text = {
                Column {
                    PulseTextField(value = minText, onValueChange = { minText = it; error = null }, label = "Minimum (R)", isMonospace = true, isNumber = true)
                    Spacer(Modifier.height(8.dp))
                    PulseTextField(value = maxText, onValueChange = { maxText = it; error = null }, label = "Maximum (R)", isMonospace = true, isNumber = true)
                    error?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, style = MonoSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val min = InputValidation.parseGoal(minText)
                    val max = InputValidation.parseGoal(maxText)
                    val validation = InputValidation.validateGoalRange(min, max)
                    if (validation != null) {
                        error = validation
                    } else {
                        viewModel.setCategoryGoal(row.key, min!!, max!!)
                        editingRow = null
                    }
                }) { Text("Save", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { editingRow = null }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        )
    }
}

@Composable
private fun GoalTrackerCard(
    summary: GoalTrackSummary,
    min: Double,
    max: Double,
    chart: @Composable () -> Unit
) {
    val totalDays = summary.daysOnTrack + summary.daysUnder + summary.daysOver

    SharpCard {
        Text("Last 30 days", style = TitleStyle)
        Spacer(Modifier.height(12.dp))

        if (summary.totalSpent <= 0.0) {
            Text("No expenses yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@SharpCard
        }

        val status = summary.finalStatus
        Row(verticalAlignment = Alignment.CenterVertically) {
            GoalScoreRing(scorePercent = summary.scorePercent)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .border(1.dp, statusColor(status), RoundedCornerShape(0.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(statusLabel(status).uppercase(), style = MonoSmall, color = statusColor(status))
                }
                Spacer(Modifier.height(8.dp))
                Text(formatZar(summary.totalSpent), style = MonoBody.copy(fontWeight = FontWeight.Medium))
                Text("of ${formatZar(min)} – ${formatZar(max)}", style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text("${summary.daysOnTrack} / $totalDays days on track", style = MonoSmall)
            }
        }

        Spacer(Modifier.height(16.dp))
        chart()
    }
}
