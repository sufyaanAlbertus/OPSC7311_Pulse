package com.pulse.budget.ui.components

/*
 * References:
 *  - Android Developers (2025) Date pickers (Material 3). https://developer.android.com/develop/ui/compose/components/datepickers
 *  - Anthropic (2026) Claude [Large language model] — used as a coding assistant; see AI declaration in README.
 */

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pulse.budget.ui.theme.MonoSmall
import com.pulse.budget.viewmodel.PeriodOption
import java.time.Instant
import java.time.ZoneOffset

/** One shared period selector. */
@Composable
fun PeriodSelector(
    selected: PeriodOption,
    periodLabel: String,
    onSelect: (PeriodOption) -> Unit,
    onCustomRange: (startEpochDay: Long, endEpochDay: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showRangePicker by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PeriodOption.entries.forEach { option ->
                val isSelected = option == selected
                Text(
                    text = option.label.uppercase(),
                    style = MonoSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .border(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(0.dp)
                        )
                        .clickable {
                            if (option == PeriodOption.CUSTOM) showRangePicker = true else onSelect(option)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
        if (periodLabel.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(periodLabel, style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (showRangePicker) {
        DateRangePickerDialog(
            onDismiss = { showRangePicker = false },
            onConfirm = { start, end ->
                onCustomRange(start, end)
                showRangePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerDialog(onDismiss: () -> Unit, onConfirm: (Long, Long) -> Unit) {
    val state = rememberDateRangePickerState()
    // The picker returns UTC midnight in milliseconds; convert to an epoch day.
    fun toEpochDay(millis: Long) = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(0.dp),
        colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        confirmButton = {
            TextButton(
                enabled = state.selectedStartDateMillis != null,
                onClick = {
                    val start = state.selectedStartDateMillis ?: return@TextButton
                    val end = state.selectedEndDateMillis ?: start
                    onConfirm(toEpochDay(start), toEpochDay(end))
                }
            ) { Text("Apply", color = MaterialTheme.colorScheme.primary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    ) {
        DateRangePicker(
            state = state,
            modifier = Modifier.weight(1f),
            title = {
                Text(
                    "Select a start and end date",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                )
            },
            showModeToggle = false
        )
    }
}
