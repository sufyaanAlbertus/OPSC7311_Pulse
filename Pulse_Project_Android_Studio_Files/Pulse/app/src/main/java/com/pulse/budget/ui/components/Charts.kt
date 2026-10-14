package com.pulse.budget.ui.components

/*
 * References:
 *  - Android Developers (2025) Graphics in Compose. https://developer.android.com/develop/ui/compose/graphics/draw/overview
 *  - Anthropic (2026) Claude [Large language model] — used as a coding assistant; see AI declaration in README.
 */

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pulse.budget.ui.theme.GoalMinAmber
import com.pulse.budget.ui.theme.MonoBody
import com.pulse.budget.ui.theme.MonoSmall
import com.pulse.budget.ui.theme.OnTrackGreen
import com.pulse.budget.util.CategoryChartRow
import com.pulse.budget.util.GoalStatus
import com.pulse.budget.util.GoalTrackPoint
import com.pulse.budget.util.formatZar
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// The two new required visuals.

@Composable
fun statusColor(status: GoalStatus): Color = when (status) {
    GoalStatus.ON_TRACK -> OnTrackGreen
    GoalStatus.UNDER_MIN -> GoalMinAmber
    GoalStatus.OVER_MAX -> MaterialTheme.colorScheme.error
    GoalStatus.NO_GOAL -> MaterialTheme.colorScheme.primary
}

fun statusLabel(status: GoalStatus): String = when (status) {
    GoalStatus.ON_TRACK -> "On track"
    GoalStatus.UNDER_MIN -> "Under min goal"
    GoalStatus.OVER_MAX -> "Over max goal"
    GoalStatus.NO_GOAL -> "No goal set"
}

// Category spending graph

@Composable
fun CategorySpendChart(rows: List<CategoryChartRow>, modifier: Modifier = Modifier) {
    // One shared scale for every bar, so bar lengths can be compared across categories.
    val scaleMax = (rows.maxOfOrNull { maxOf(it.total, it.maxGoal, it.minGoal) } ?: 0.0).coerceAtLeast(1.0) * 1.1

    Column(modifier = modifier.fillMaxWidth()) {
        CategoryChartLegend()
        Spacer(Modifier.height(8.dp))
        rows.forEach { row -> CategoryBarRow(row = row, scaleMax = scaleMax) }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("R0", style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatZar(scaleMax / 2), style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatZar(scaleMax), style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CategoryBarRow(row: CategoryChartRow, scaleMax: Double) {
    val barColor = statusColor(row.status)
    val trackColor = MaterialTheme.colorScheme.outline
    val maxColor = MaterialTheme.colorScheme.error
    val description = "${row.label}: spent ${formatZar(row.total)}" +
            (if (row.minGoal > 0) ", minimum goal ${formatZar(row.minGoal)}" else "") +
            (if (row.maxGoal > 0) ", maximum goal ${formatZar(row.maxGoal)}" else "") +
            ", ${statusLabel(row.status)}"

    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                row.label,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                formatZar(row.total),
                style = MonoBody,
                color = if (row.status == GoalStatus.OVER_MAX) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .semantics { contentDescription = description }
        ) {
            val barHeight = size.height * 0.6f
            val top = (size.height - barHeight) / 2f
            fun xFor(value: Double) = ((value / scaleMax).toFloat() * size.width).coerceIn(0f, size.width)

            drawRect(color = trackColor.copy(alpha = 0.35f), topLeft = Offset(0f, top), size = Size(size.width, barHeight))
            drawRect(color = barColor, topLeft = Offset(0f, top), size = Size(xFor(row.total), barHeight))

            if (row.minGoal > 0) {
                val x = xFor(row.minGoal)
                drawLine(GoalMinAmber, Offset(x, 0f), Offset(x, size.height), strokeWidth = 3.dp.toPx())
            }
            if (row.maxGoal > 0) {
                val x = xFor(row.maxGoal)
                drawLine(
                    maxColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 3.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                )
            }
        }
        val goalText = when {
            row.minGoal > 0 && row.maxGoal > 0 -> "Min ${formatZar(row.minGoal)} · Max ${formatZar(row.maxGoal)}"
            row.maxGoal > 0 -> "Max ${formatZar(row.maxGoal)}"
            row.minGoal > 0 -> "Min ${formatZar(row.minGoal)}"
            else -> "No goal"
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(goalText, style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(statusLabel(row.status).uppercase(), style = MonoSmall, color = barColor)
        }
    }
}

@Composable
private fun CategoryChartLegend() {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendItem(color = OnTrackGreen, text = "On track")
            LegendItem(color = GoalMinAmber, text = "Under min")
            LegendItem(color = MaterialTheme.colorScheme.error, text = "Over max")
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendItem(color = GoalMinAmber, text = "Min goal", isLine = true)
            LegendItem(color = MaterialTheme.colorScheme.error, text = "Max goal", isLine = true)
            LegendItem(color = MaterialTheme.colorScheme.primary, text = "No goal")
        }
    }
}

@Composable
fun LegendItem(color: Color, text: String, isLine: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = if (isLine) 3.dp else 10.dp, height = 10.dp)
                .background(color)
        )
        Spacer(Modifier.width(5.dp))
        Text(text, style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// 30-day goal tracker

@Composable
fun GoalTrackerChart(points: List<GoalTrackPoint>, modifier: Modifier = Modifier, chartHeight: Dp = 170.dp) {
    val lineColor = MaterialTheme.colorScheme.primary
    val bandColor = OnTrackGreen.copy(alpha = 0.14f)
    val maxColor = MaterialTheme.colorScheme.error
    val gridColor = MaterialTheme.colorScheme.outline
    val last = points.lastOrNull()
    val description = if (last == null) "No data yet" else
        "Spent ${formatZar(last.cumulative)} in the last ${points.size} days. Healthy range by today is " +
                "${formatZar(last.minPace)} to ${formatZar(last.maxPace)}. ${statusLabel(last.status)}."
    val endDotColor = if (last != null) statusColor(last.status) else lineColor
    // Same top-of-chart value the Canvas uses, so the axis labels match the drawing.
    val axisMax = if (points.isEmpty()) 0.0 else
        maxOf(points.maxOf { it.cumulative }, points.last().maxPace, points.last().minPace).coerceAtLeast(1.0) * 1.1

    Column(modifier = modifier.fillMaxWidth()) {
        // Y-axis hint so the user can read rough amounts off the chart.
        Text(formatZar(axisMax), style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .semantics { contentDescription = description }
        ) {
            if (points.isEmpty()) return@Canvas
            val n = points.size
            val maxValue = maxOf(points.maxOf { it.cumulative }, points.last().maxPace, points.last().minPace)
                .coerceAtLeast(1.0) * 1.1
            fun x(i: Int) = if (n == 1) size.width / 2f else i * size.width / (n - 1)
            fun y(v: Double) = size.height - (v / maxValue).toFloat() * size.height

            // Light horizontal grid lines at 25 / 50 / 75 %.
            listOf(0.25f, 0.5f, 0.75f).forEach { f ->
                val gy = size.height * f
                drawLine(gridColor.copy(alpha = 0.4f), Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
            }

            // Healthy band: the area between the minimum pace and the maximum pace.
            if (points.last().maxPace > 0) {
                val band = Path().apply {
                    moveTo(x(0), y(points[0].maxPace))
                    for (i in 1 until n) lineTo(x(i), y(points[i].maxPace))
                    for (i in n - 1 downTo 0) lineTo(x(i), y(points[i].minPace))
                    close()
                }
                drawPath(band, bandColor)
                drawLine(
                    maxColor, Offset(x(0), y(points[0].maxPace)), Offset(x(n - 1), y(points[n - 1].maxPace)),
                    strokeWidth = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )
            }
            if (points.last().minPace > 0) {
                drawLine(
                    GoalMinAmber, Offset(x(0), y(points[0].minPace)), Offset(x(n - 1), y(points[n - 1].minPace)),
                    strokeWidth = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )
            }

            // The user's cumulative spending.
            val spendPath = Path().apply {
                moveTo(x(0), y(points[0].cumulative))
                for (i in 1 until n) lineTo(x(i), y(points[i].cumulative))
            }
            drawPath(spendPath, lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

            val end = Offset(x(n - 1), y(points.last().cumulative))
            drawCircle(endDotColor.copy(alpha = 0.3f), radius = 8.dp.toPx(), center = end)
            drawCircle(endDotColor, radius = 4.dp.toPx(), center = end)
        }

        Spacer(Modifier.height(8.dp))
        // Day-by-day status strip — one cell per day, coloured by where the running total sat.
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            points.forEach { p ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .background(statusColor(p.status))
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        if (points.isNotEmpty()) {
            val fmt = DateTimeFormatter.ofPattern("d MMM")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(LocalDate.ofEpochDay(points.first().epochDay).format(fmt), style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Today", style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendItem(color = lineColor, text = "Spent", isLine = true)
            LegendItem(color = OnTrackGreen, text = "Target")
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendItem(color = GoalMinAmber, text = "Minimum", isLine = true)
            LegendItem(color = maxColor, text = "Maximum", isLine = true)
        }
    }
}

/** Circular score: share of the last 30 days spent inside the goal band. */
@Composable
fun GoalScoreRing(scorePercent: Int, modifier: Modifier = Modifier, diameter: Dp = 96.dp) {
    val trackColor = MaterialTheme.colorScheme.outline
    val ringColor = when {
        scorePercent >= 70 -> OnTrackGreen
        scorePercent >= 40 -> GoalMinAmber
        else -> MaterialTheme.colorScheme.error
    }
    Box(
        modifier = modifier
            .size(diameter)
            .semantics { contentDescription = "Goal score $scorePercent percent of days in range" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(diameter)) {
            val stroke = 9.dp.toPx()
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)
            drawArc(trackColor, startAngle = -90f, sweepAngle = 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
            drawArc(
                ringColor, startAngle = -90f, sweepAngle = 360f * scorePercent.coerceIn(0, 100) / 100f,
                useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Butt)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$scorePercent%", style = MonoBody.copy(fontWeight = FontWeight.Medium))
            Text("ON TRACK", style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
