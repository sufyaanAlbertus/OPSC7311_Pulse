package com.pulse.budget.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import com.pulse.budget.data.local.CategoryType
import com.pulse.budget.ui.components.CategoryIcon
import com.pulse.budget.ui.components.PulsePrimaryButton
import com.pulse.budget.ui.components.PulseTopBar
import com.pulse.budget.ui.components.SharpCard
import com.pulse.budget.ui.components.SharpProgressBar
import com.pulse.budget.ui.components.label
import com.pulse.budget.ui.theme.MonoBody
import com.pulse.budget.ui.theme.MonoDisplay
import com.pulse.budget.ui.theme.MonoSmall
import com.pulse.budget.util.dailyThreshold
import com.pulse.budget.util.formatZar
import com.pulse.budget.viewmodel.AuthViewModel
import com.pulse.budget.viewmodel.DaySpend
import com.pulse.budget.viewmodel.ExpenseViewModel
import java.time.LocalTime

@Composable
fun DashboardScreen(
    viewModel: ExpenseViewModel,
    authViewModel: AuthViewModel,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onAddExpense: () -> Unit
) {
    val username by authViewModel.currentUsername.collectAsState()
    val monthTotal by viewModel.monthTotal.collectAsState()
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()
    val categoryTotals by viewModel.categoryTotals.collectAsState()
    val categoryLimits by viewModel.categoryLimits.collectAsState()
    val sevenDaySignal by viewModel.sevenDaySignal.collectAsState()
    val streakDays by viewModel.streakDays.collectAsState()
    val xp by viewModel.xp.collectAsState()

    val remaining = monthlyBudget - monthTotal.toFloat()
    val budgetProgress = if (monthlyBudget > 0) (monthTotal.toFloat() / monthlyBudget) else 0f
    val percentUsed = (budgetProgress * 100).toInt()
    val isOverBudget = monthTotal.toFloat() > monthlyBudget

    // Only show categories that actually have spend this month, highest first — a fixed first-5
    // list regardless of activity told the user nothing useful.
    val activeCategories = categoryTotals
        .filter { it.total > 0 }
        .sortedByDescending { it.total }

    val greeting = remember(username) {
        val hour = LocalTime.now().hour
        val timeOfDay = when {
            hour < 12 -> "Good morning"
            hour < 18 -> "Good afternoon"
            else -> "Good evening"
        }
        if (username != null) "$timeOfDay, $username" else timeOfDay
    }

    Scaffold(topBar = { PulseTopBar(title = "Dashboard", isDark = isDark, onToggleTheme = onToggleTheme) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(greeting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))

            // Budget card — a visible progress bar instead of just a percentage number.
            SharpCard {
                Text("Remaining Budget", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(
                    formatZar(remaining),
                    style = MonoDisplay,
                    color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(10.dp))
                SharpProgressBar(progress = budgetProgress, overLimit = isOverBudget)
                Spacer(Modifier.height(4.dp))
                Text(
                    "OF ${formatZar(monthlyBudget)} · $percentUsed% USED",
                    style = MonoSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Streak / level quick-glance — so the gamification hooks are visible without a
            // separate trip to the Badges tab.
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SharpCard(modifier = Modifier.weight(1f), padding = PaddingValues(12.dp)) {
                    Text("Streak", style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$streakDays days", style = MonoBody)
                }
                SharpCard(modifier = Modifier.weight(1f), padding = PaddingValues(12.dp)) {
                    Text("XP", style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$xp", style = MonoBody)
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Category Spend This Month", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))

            if (activeCategories.isEmpty()) {
                Text(
                    "Nothing logged yet this month.",
                    style = MonoSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(activeCategories) { ct ->
                        val limit = categoryLimits.firstOrNull { it.category == ct.category }?.limit ?: 0.0
                        val progress = if (limit > 0) (ct.total / limit).toFloat() else 0f
                        val overLimit = limit > 0 && ct.total > limit

                        SharpCard(
                            modifier = Modifier.width(120.dp),
                            padding = PaddingValues(10.dp)
                        ) {
                            CategoryIcon(category = ct.category, size = 32.dp)
                            Spacer(Modifier.height(6.dp))
                            Text(ct.category.label(), style = MonoSmall, maxLines = 1)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                formatZar(ct.total),
                                style = MonoBody,
                                color = if (overLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                            if (limit > 0) {
                                Spacer(Modifier.height(6.dp))
                                SharpProgressBar(progress = progress, overLimit = overLimit)
                            }
                        }
                    }
                }
            }

            // 7-Day Signal — redesigned so "why is this bar red" is answered visually: a dashed
            // threshold line drawn across the chart at the actual cutoff, plus a legend with
            // color swatches and the real Rand figure, not just a caption sentence.
            Spacer(Modifier.height(20.dp))
            val threshold = dailyThreshold(monthlyBudget)
            SharpCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("7-Day Signal", style = MaterialTheme.typography.bodyMedium)
                    Text("Daily Spend (R)", style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(12.dp))
                SevenDaySignalChart(
                    days = sevenDaySignal,
                    threshold = threshold,
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                )
                Spacer(Modifier.height(12.dp))
                SevenDaySignalLegend(threshold = threshold)
            }

            Spacer(Modifier.height(20.dp))
            PulsePrimaryButton(text = "+ Log Expense", onClick = onAddExpense)
        }
    }
}

/** Small color-swatch legend explaining the bar chart above, with the real threshold figure
 *  spelled out rather than left as an unexplained color choice. */
@Composable
private fun SevenDaySignalLegend(threshold: Float) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LegendSwatch(color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            Text("Under daily average", style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LegendSwatch(color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(6.dp))
            Text("Over daily average — flagged for spending more than usual", style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (threshold > 0f) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Threshold: ${formatZar(threshold)}/day (1.5\u00d7 your implied daily budget)",
                style = MonoSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LegendSwatch(color: Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .background(color)
    )
}

@Composable
private fun SevenDaySignalChart(days: List<DaySpend>, threshold: Float, modifier: Modifier = Modifier) {
    val blue = MaterialTheme.colorScheme.primary
    val red = MaterialTheme.colorScheme.error
    val lineColor = MaterialTheme.colorScheme.onSurfaceVariant
    val connectorColor = MaterialTheme.colorScheme.outline
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val maxAmount = (days.maxOfOrNull { it.amount } ?: 1f).coerceAtLeast(threshold).coerceAtLeast(1f)

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (days.isEmpty()) return@Canvas

            val stepX = size.width / (days.size - 1).coerceAtLeast(1)
            val points = days.mapIndexed { index, day ->
                val x = if (days.size == 1) size.width / 2f else index * stepX
                val y = size.height - (day.amount / maxAmount) * size.height
                Offset(x, y)
            }

            // Dashed reference line at the actual threshold height, so a dot crossing into red
            // is visibly explained rather than an unexplained color choice.
            if (threshold > 0f) {
                val thresholdY = size.height - (threshold / maxAmount) * size.height
                drawLine(
                    color = lineColor,
                    start = Offset(0f, thresholdY),
                    end = Offset(size.width, thresholdY),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                )
            }

            // Thin connector line joining each day's dot, so the trend across the week reads at
            // a glance rather than needing to compare seven isolated points.
            for (i in 0 until points.size - 1) {
                drawLine(
                    color = connectorColor,
                    start = points[i],
                    end = points[i + 1],
                    strokeWidth = 3f
                )
            }

            // One dot per day — red if that day crossed the threshold, blue otherwise. A ring
            // behind each dot gives it a bit of visual weight against the connector line.
            points.forEachIndexed { index, point ->
                val dotColor = if (days[index].overLimit) red else blue
                drawCircle(color = dotColor.copy(alpha = 0.25f), radius = 12f, center = point)
                drawCircle(color = dotColor, radius = 6f, center = point)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            days.forEach { day -> Text(day.label, style = MonoSmall, color = textColor) }
        }
    }
}