package com.pulse.budget.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pulse.budget.data.local.CategoryType
import com.pulse.budget.data.local.RecurrenceType
import com.pulse.budget.ui.components.label
import com.pulse.budget.ui.components.PulsePrimaryButton
import com.pulse.budget.ui.components.PulseTextField
import com.pulse.budget.ui.components.PulseTopBar
import com.pulse.budget.ui.theme.MonoBody
import com.pulse.budget.ui.theme.MonoSmall
import com.pulse.budget.viewmodel.ExpenseViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** null selectedCustomCategory means a built-in CategoryType is in use; a non-null value means
 *  the user picked (or just created) a custom category, and `category` is stored as OTHER. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    viewModel: ExpenseViewModel,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val customCategories by viewModel.customCategories.collectAsState()

    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(CategoryType.GROCERIES) }
    var selectedCustomCategory by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    // Date: auto-filled with today the moment this screen opens. The user never has to type a
    // date — tapping the field opens a calendar (DatePickerDialog) to change it if logging a
    // past expense.
    var date by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Start time: what time the spending activity began — auto-filled to the current time when
    // this screen opens, same as the date. End time: what time it finished, left blank by
    // default since not every expense has a clear end (a single purchase is instantaneous), but
    // useful for anything that spans a period — a shopping trip, a meal out, a taxi ride — so you
    // can later see not just what you spent but how long you spent it over.
    var startTime by remember { mutableStateOf(LocalTime.now()) }
    var endTime by remember { mutableStateOf<LocalTime?>(null) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    var description by remember { mutableStateOf("") }
    var receiptUri by remember { mutableStateOf<Uri?>(null) }
    var isRecurring by remember { mutableStateOf(false) }
    var recurrenceType by remember { mutableStateOf(RecurrenceType.WEEKLY) }
    var amountError by remember { mutableStateOf(false) }

    // Opens the system photo picker / gallery and returns a content Uri (Part 2 brief: "optionally
    // add a photograph to each expense entry").
    val receiptPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> if (uri != null) receiptUri = uri }

    val categoryDisplayLabel = selectedCustomCategory ?: category.label()

    Scaffold(topBar = { PulseTopBar(title = "Add Expense", isDark = isDark, onToggleTheme = onToggleTheme, onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            PulseTextField(
                value = amount,
                onValueChange = { amount = it; amountError = false },
                label = "Amount (R)",
                isMonospace = true
            )
            if (amountError) {
                Text("Enter a valid amount greater than 0", style = MonoSmall, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(12.dp))

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                PulseTextField(
                    value = categoryDisplayLabel,
                    onValueChange = {},
                    label = "Category",
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    CategoryType.entries.forEach { c ->
                        DropdownMenuItem(
                            text = { Text(c.label()) },
                            onClick = { category = c; selectedCustomCategory = null; expanded = false }
                        )
                    }
                    if (customCategories.isNotEmpty()) {
                        androidx.compose.material3.HorizontalDivider()
                        customCategories.forEach { custom ->
                            DropdownMenuItem(
                                text = { Text(custom.name) },
                                onClick = {
                                    category = CategoryType.OTHER
                                    selectedCustomCategory = custom.name
                                    expanded = false
                                }
                            )
                        }
                    }
                    androidx.compose.material3.HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("+ Add new category", color = MaterialTheme.colorScheme.primary) },
                        onClick = { expanded = false; showAddCategoryDialog = true }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // Date — auto-filled to today, tap opens a calendar to change it.
            PickerField(
                label = "Date",
                value = date.toString(),
                icon = Icons.Outlined.CalendarToday,
                onClick = { showDatePicker = true }
            )
            Spacer(Modifier.height(12.dp))

            // Start/end time — see the explanation above the variable declarations. Both auto-fill
            // or stay blank sensibly, and both open a clock picker on tap rather than free typing.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PickerField(
                    label = "Start time",
                    value = startTime.format(TIME_FORMAT),
                    icon = Icons.Outlined.AccessTime,
                    onClick = { showStartTimePicker = true },
                    modifier = Modifier.weight(1f)
                )
                PickerField(
                    label = "End time (optional)",
                    value = endTime?.format(TIME_FORMAT) ?: "—",
                    icon = Icons.Outlined.AccessTime,
                    onClick = { showEndTimePicker = true },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Start time is when the spending activity began (defaults to now). End time is " +
                        "when it finished — useful for anything that spans a period, like a shopping " +
                        "trip or a meal out. Leave end time blank for a single instant purchase.",
                style = MonoSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            PulseTextField(value = description, onValueChange = { if (it.length <= 100) description = it }, label = "Description (optional)")
            Text("${description.length}/100", style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))

            // Recurring toggle — FR7: mark an expense as recurring weekly or monthly.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recurring expense", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = isRecurring, onCheckedChange = { isRecurring = it })
            }
            if (isRecurring) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(RecurrenceType.WEEKLY, RecurrenceType.MONTHLY).forEach { type ->
                        val selected = recurrenceType == type
                        Text(
                            text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MonoSmall,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                                .clickable { recurrenceType = type }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Receipt drop zone — launches the system photo picker; shows a thumbnail once picked.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (receiptUri != null) 160.dp else 96.dp)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(0.dp))
                    .clickable { receiptPicker.launch("image/*") },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (receiptUri != null) {
                    AsyncImage(
                        model = receiptUri,
                        contentDescription = "Receipt preview",
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Receipt attached — tap to change", style = MonoSmall, color = MaterialTheme.colorScheme.primary)
                } else {
                    Icon(Icons.Outlined.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text("Tap to attach a receipt photo", style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(24.dp))
            PulsePrimaryButton(
                text = "Save Expense",
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (amountValue == null || amountValue <= 0.0) {
                        amountError = true
                        return@PulsePrimaryButton
                    }
                    viewModel.addExpense(
                        amount = amountValue,
                        category = category,
                        dateEpochDay = date.toEpochDay(),
                        startTime = startTime.format(TIME_FORMAT),
                        endTime = endTime?.format(TIME_FORMAT) ?: "",
                        description = description,
                        receiptUri = receiptUri?.toString(),
                        recurrence = if (isRecurring) recurrenceType else RecurrenceType.NONE,
                        customCategoryName = selectedCustomCategory
                    )
                    onSaved()
                }
            )
        }
    }

    // "+ Add new category" dialog — Part 2 brief: "the user must be able to create categories."
    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            shape = RoundedCornerShape(0.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("New category") },
            text = {
                PulseTextField(value = newCategoryName, onValueChange = { newCategoryName = it }, label = "Category name")
            },
            confirmButton = {
                PulsePrimaryButton(
                    text = "Add",
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            viewModel.addCustomCategory(newCategoryName)
                            category = CategoryType.OTHER
                            selectedCustomCategory = newCategoryName.trim()
                            newCategoryName = ""
                        }
                        showAddCategoryDialog = false
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Date picker dialog.
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            shape = RoundedCornerShape(0.dp),
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    headlineContentColor = MaterialTheme.colorScheme.onSurface,
                    weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    subheadContentColor = MaterialTheme.colorScheme.onSurface,
                    yearContentColor = MaterialTheme.colorScheme.onSurface,
                    currentYearContentColor = MaterialTheme.colorScheme.primary,
                    selectedYearContainerColor = MaterialTheme.colorScheme.primary,
                    selectedYearContentColor = MaterialTheme.colorScheme.background,
                    dayContentColor = MaterialTheme.colorScheme.onSurface,
                    selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                    selectedDayContentColor = MaterialTheme.colorScheme.background,
                    todayContentColor = MaterialTheme.colorScheme.primary,
                    todayDateBorderColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }

    // Start time picker dialog.
    if (showStartTimePicker) {
        TimePickerDialogBox(
            initial = startTime,
            onDismiss = { showStartTimePicker = false },
            onConfirm = { startTime = it; showStartTimePicker = false }
        )
    }

    // End time picker dialog.
    if (showEndTimePicker) {
        TimePickerDialogBox(
            initial = endTime ?: startTime,
            onDismiss = { showEndTimePicker = false },
            onConfirm = { endTime = it; showEndTimePicker = false }
        )
    }
}

/** A bordered, tappable field that looks like the app's text fields but opens a picker instead
 *  of a keyboard — used for date and time so the user never has to type a format-sensitive string. */
@Composable
private fun PickerField(
    label: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label.uppercase(), style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(value, style = MonoBody)
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialogBox(initial: LocalTime, onDismiss: () -> Unit, onConfirm: (LocalTime) -> Unit) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(0.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                Text("OK", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = MaterialTheme.colorScheme.background,
                    clockDialSelectedContentColor = MaterialTheme.colorScheme.background,
                    clockDialUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
                    selectorColor = MaterialTheme.colorScheme.primary,
                    containerColor = MaterialTheme.colorScheme.surface,
                    periodSelectorBorderColor = MaterialTheme.colorScheme.outline,
                    periodSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary,
                    periodSelectorUnselectedContainerColor = MaterialTheme.colorScheme.background,
                    periodSelectorSelectedContentColor = MaterialTheme.colorScheme.background,
                    periodSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary,
                    timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.background,
                    timeSelectorSelectedContentColor = MaterialTheme.colorScheme.background,
                    timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    )
}