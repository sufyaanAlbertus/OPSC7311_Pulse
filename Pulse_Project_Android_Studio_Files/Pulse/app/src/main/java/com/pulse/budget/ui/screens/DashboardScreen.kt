package com.pulse.budget.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.pulse.budget.util.XP_PER_LEVEL
import com.pulse.budget.util.badgeProgress
import com.pulse.budget.util.levelForXp
import com.pulse.budget.util.xpToNextLevel
import com.pulse.budget.ui.components.CategoryIcon
import com.pulse.budget.ui.components.GoalScoreRing
import com.pulse.budget.ui.components.PulsePrimaryButton
import com.pulse.budget.ui.components.PulseTopBar
import com.pulse.budget.ui.components.SharpCard
import com.pulse.budget.ui.components.SharpProgressBar
import com.pulse.budget.ui.components.statusColor
import com.pulse.budget.ui.components.statusLabel
import com.pulse.budget.ui.theme.MonoBody
import com.pulse.budget.ui.theme.MonoDisplay
import com.pulse.budget.ui.theme.MonoSmall
import com.pulse.budget.util.dailyThreshold
import com.pulse.budget.util.formatZar
import com.pulse.budget.util.goalStatus
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
    onAddExpense: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenBadges: () -> Unit = {}
) {
    val username by authViewModel.currentUsername.collectAsState()
    val monthTotal by viewModel.monthTotal.collectAsState()
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()
    val minMonthlyBudget by viewModel.minMonthlyBudget.collectAsState()
    val spendGroups by viewModel.monthSpendGroups.collectAsState()
    val categoryGoals by viewModel.categoryGoals.collectAsState()
    val sevenDaySignal by viewModel.sevenDaySignal.collectAsState()
    val goalSummary by viewModel.goalSummary.collectAsState()
    val streakDays by viewModel.streakDays.collectAsState()
    val xp by viewModel.xp.collectAsState()
    val badges by viewModel.badges.collectAsState()
    val badgeStats by viewModel.badgeStats.collectAsState()

    val remaining = monthlyBudget - monthTotal.toFloat()
    val budgetProgress = if (monthlyBudget > 0) (monthTotal.toFloat() / monthlyBudget) else 0f
    val percentUsed = (budgetProgress * 100).toInt()
    val monthStatus = goalStatus(monthTotal, minMonthlyBudget.toDouble(), monthlyBudget.toDouble())

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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(greeting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))

            // Budget card.
            SharpCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Left to spend this month", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(statusLabel(monthStatus).uppercase(), style = MonoSmall, color = statusColor(monthStatus))
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    formatZar(remaining),
                    style = MonoDisplay.copy(fontSize = 36.sp),
                    color = if (remaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SharpProgressBar(
                        progress = budgetProgress,
                        overLimit = remaining < 0,
                        barHeight = 14.dp,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("$percentUsed%", style = MonoBody.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Spent ${formatZar(monthTotal)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Min ${formatZar(minMonthlyBudget)} · Max ${formatZar(monthlyBudget)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Your progress card.
            Spacer(Modifier.height(12.dp))
            val level = levelForXp(xp)
            val nextBadge = badges.filter { !it.earned }
                .mapNotNull { b -> badgeProgress(b.id, badgeStats)?.let { b to it } }
                .maxByOrNull { it.second.fraction }
            SharpCard(modifier = Modifier.clickable(onClick = onOpenBadges)) {
                Text("Your progress", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("LEVEL", style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$level", style = MonoDisplay)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("STREAK", style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$streakDays", style = MonoDisplay)
                        Text(if (streakDays == 1) "day in a row" else "days in a row", style = MonoSmall)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("BADGES", style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${badges.count { it.earned }}", style = MonoDisplay)
                        Text("of ${badges.size}", style = MonoSmall)
                    }
                }
                Spacer(Modifier.height(12.dp))
                SharpProgressBar(progress = (xp % XP_PER_LEVEL) / XP_PER_LEVEL.toFloat(), barHeight = 10.dp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "$xp XP · ${xpToNextLevel(xp)} XP to level ${level + 1}",
                    style = MonoSmall
                )
                if (nextBadge != null) {
                    val (badge, progress) = nextBadge
                    Spacer(Modifier.height(12.dp))
                    Text("Next badge: ${badge.title}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(Modifier.height(4.dp))
                    SharpProgressBar(progress = progress.fraction, barHeight = 8.dp)
                    Spacer(Modifier.height(4.dp))
                    Text("${progress.current} / ${progress.target} ${progress.unit}", style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(8.dp))
                Text("VIEW ALL BADGES →", style = MonoSmall, color = MaterialTheme.colorScheme.primary)
            }

            // Goal Health — quick view of the 30-day goal tracker.
            Spacer(Modifier.height(12.dp))
            SharpCard(modifier = Modifier.clickable(onClick = onOpenGoals)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GoalScoreRing(scorePercent = goalSummary.scorePercent, diameter = 84.dp)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Goal Health · Last 30 Days", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${goalSummary.daysOnTrack} days on track",
                            style = MonoSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("VIEW TRACKER →", style = MonoSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Category Spend This Month", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))

            if (spendGroups.isEmpty()) {
                Text("Nothing logged yet this month.", style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(spendGroups, key = { it.key }) { group ->
                        val max = categoryGoals.firstOrNull { it.key == group.key }?.maxGoal ?: 0.0
                        val progress = if (max > 0) (group.total / max).toFloat() else 0f
                        val overLimit = max > 0 && group.total > max

                        SharpCard(modifier = Modifier.width(120.dp), padding = PaddingValues(10.dp)) {
                            CategoryIcon(category = group.category, size = 32.dp)
                            Spacer(Modifier.height(6.dp))
                            Text(group.label, style = MonoSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                formatZar(group.total),
                                style = MonoBody,
                                color = if (overLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                            if (max > 0) {
                                Spacer(Modifier.height(6.dp))
                                SharpProgressBar(progress = progress, overLimit = overLimit)
                            }
                        }
                    }
                }
            }

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

@Composable
private fun SevenDaySignalLegend(threshold: Float) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LegendSwatch(color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            Text("Normal day", style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LegendSwatch(color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(6.dp))
            Text("High spend day", style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (threshold > 0f) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Limit: ${formatZar(threshold)}/day",
                style = MonoSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LegendSwatch(color: Color) {
    Box(modifier = Modifier.size(10.dp).background(color))
}

@Composable
private fun SevenDaySignalChart(days: List<DaySpend>, threshold: Float, modifier: Modifier = Modifier) {
    val blue = MaterialTheme.colorScheme.primary
    val red = MaterialTheme.colorScheme.error
    val lineColor = MaterialTheme.colorScheme.onSurfaceVariant
    val connectorColor = MaterialTheme.colorScheme.outline
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val maxAmount = (days.maxOfOrNull { it.amount } ?: 1f).coerceAtLeast(threshold).coerceAtLeast(1f) * 1.1f

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (days.isEmpty()) return@Canvas

            val stepX = size.width / (days.size - 1).coerceAtLeast(1)
            val points = days.mapIndexed { index, day ->
                val x = if (days.size == 1) size.width / 2f else index * stepX
                val y = size.height - (day.amount / maxAmount) * size.height
                Offset(x, y)
            }

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

            for (i in 0 until points.size - 1) {
                drawLine(color = connectorColor, start = points[i], end = points[i + 1], strokeWidth = 3f)
            }

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
