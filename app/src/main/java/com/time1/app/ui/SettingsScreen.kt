package com.time1.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.time1.app.domain.SalaryCalculator
import com.time1.app.domain.model.AppSettings
import com.time1.app.domain.model.WorkSchedule
import com.time1.app.ui.theme.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    modifier: Modifier = Modifier,
    onSave: (AppSettings) -> Unit
) {
    var salary by remember(settings) { mutableStateOf(settings.monthlySalary.toString()) }
    var hoursPerShift by remember(settings) { mutableStateOf(settings.hoursPerShift.toString()) }
    var schedule by remember(settings) { mutableStateOf(settings.workSchedule) }
    var cycleStartDate by remember(settings) {
        mutableStateOf(
            java.time.Instant.ofEpochMilli(settings.cycleStartDate)
                .atZone(ZoneId.systemDefault()).toLocalDate()
        )
    }

    val currentSettings = remember(salary, hoursPerShift, schedule, cycleStartDate) {
        AppSettings(
            monthlySalary = salary.toDoubleOrNull() ?: 50000.0,
            hoursPerShift = hoursPerShift.toDoubleOrNull() ?: 8.0,
            workSchedule = schedule,
            cycleStartDate = cycleStartDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
    }

    val now = LocalDateTime.now()
    val earned = SalaryCalculator.earnedThisMonth(currentSettings, now)
    val progress = SalaryCalculator.shiftProgress(currentSettings, now)
    val weekDays = SalaryCalculator.weekDaysInfo(currentSettings)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Настройки зарплаты",
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Salary input
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Зарплата и рабочее время", color = TextSecondary, fontSize = 12.sp)

                OutlinedTextField(
                    value = salary,
                    onValueChange = { salary = it },
                    label = { Text("Месячная зарплата (₽)", color = TextSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = hoursPerShift,
                    onValueChange = { hoursPerShift = it },
                    label = { Text("Часов в смене", color = TextSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // Schedule selector
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("График работы", color = TextSecondary, fontSize = 12.sp)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScheduleButton(
                        label = "5/2",
                        selected = schedule == WorkSchedule.FIVE_TWO,
                        onClick = { schedule = WorkSchedule.FIVE_TWO },
                        modifier = Modifier.weight(1f)
                    )
                    ScheduleButton(
                        label = "2/2",
                        selected = schedule == WorkSchedule.TWO_TWO,
                        onClick = { schedule = WorkSchedule.TWO_TWO },
                        modifier = Modifier.weight(1f)
                    )
                    ScheduleButton(
                        label = "День/Ночь",
                        selected = schedule == WorkSchedule.DAY_NIGHT_TWO,
                        onClick = { schedule = WorkSchedule.DAY_NIGHT_TWO },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (schedule != WorkSchedule.FIVE_TWO) {
                    CycleDatePicker(
                        date = cycleStartDate,
                        onDateChange = { cycleStartDate = it }
                    )
                }
            }
        }

        // Widget preview
        WidgetPreview(
            settings = currentSettings,
            earned = earned,
            progress = progress,
            weekDays = weekDays
        )

        // Save button
        Button(
            onClick = {
                onSave(currentSettings)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Сохранить", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ScheduleButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) AccentBlue else Color(0xFF2A2A4A))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else TextSecondary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp
        )
    }
}

@Composable
fun CycleDatePicker(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    var showDialog by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { showDialog = true },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
        border = BorderStroke(1.dp, TextSecondary)
    ) {
        Text("Дата старта цикла: ${date.format(formatter)}", color = TextPrimary)
    }

    if (showDialog) {
        DatePickerDialog(
            date = date,
            onDismiss = { showDialog = false },
            onDateSelected = {
                onDateChange(it)
                showDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    date: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val initialMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val selectedDate = java.time.Instant.ofEpochMilli(millis)
                        .atZone(ZoneId.systemDefault()).toLocalDate()
                    onDateSelected(selectedDate)
                }
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
fun WidgetPreview(
    settings: AppSettings,
    earned: Double,
    progress: Float,
    weekDays: List<SalaryCalculator.DayInfo>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Превью виджета", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                // Left - progress circle area
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(80.dp),
                            color = AccentGreen,
                            trackColor = Color(0xFF2A2A4A),
                            strokeWidth = 8.dp
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    val todayInfo = weekDays.firstOrNull { it.isToday }
                    Text(
                        text = todayInfo?.shiftLabel ?: "Выходной",
                        color = if (todayInfo?.isWorkDay == true) AccentGreen else TextSecondary,
                        fontSize = 12.sp
                    )
                }

                // Right - metrics
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricItem(label = "Заработано", value = "%.0f ₽".format(earned), color = AccentGreen)
                    MetricItem(
                        label = "Ставка/час",
                        value = "%.0f ₽".format(SalaryCalculator.hourlyRate(settings)),
                        color = AccentBlue
                    )
                    MetricItem(
                        label = "Смена",
                        value = "%.1f ч".format(SalaryCalculator.hoursWorkedToday(settings)),
                        color = AccentOrange
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Week strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDays.forEach { day ->
                    DayChip(dayInfo = day)
                }
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.End) {
        Text(text = label, color = TextSecondary, fontSize = 11.sp)
        Text(text = value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DayChip(dayInfo: SalaryCalculator.DayInfo) {
    val dayName = SalaryCalculator.DAY_NAMES_RU[dayInfo.date.dayOfWeek.value % 7]

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when {
                        dayInfo.isToday -> TodayColor
                        dayInfo.isWorkDay -> Color(0xFF2A3F2A)
                        else -> Color(0xFF2A2A2A)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dayInfo.date.dayOfMonth.toString(),
                color = if (dayInfo.isToday) Color.White else if (dayInfo.isWorkDay) AccentGreen else TextSecondary,
                fontSize = 12.sp,
                fontWeight = if (dayInfo.isToday) FontWeight.Bold else FontWeight.Normal
            )
        }
        Text(text = dayName, color = TextSecondary, fontSize = 10.sp)
    }
}
