package com.time1.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.*
import com.time1.app.data.SettingsRepository
import com.time1.app.domain.SalaryCalculator
import com.time1.app.domain.model.AppSettings
import com.time1.app.ui.MainActivity
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime

class SalaryWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = SettingsRepository(context)
        val settings = repository.settingsFlow.first()

        provideContent {
            WidgetContent(settings = settings, context = context)
        }
    }
}

@Composable
private fun WidgetContent(settings: AppSettings, context: Context) {
    val now = LocalDateTime.now()
    val earned = SalaryCalculator.earnedThisMonth(settings, now)
    val progress = SalaryCalculator.shiftProgress(settings, now)
    val hourlyRate = SalaryCalculator.hourlyRate(settings)
    val hoursWorked = SalaryCalculator.hoursWorkedToday(settings, now)
    val weekDays = SalaryCalculator.weekDaysInfo(settings)
    val todayInfo = weekDays.firstOrNull { it.isToday }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF16213E))
            .clickable(actionStartActivity<MainActivity>())
            .padding(12.dp)
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Header
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Зарплата",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFB0B0B0)),
                        fontSize = 11.sp
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Main content row
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                // Left: Progress indicator
                Column(
                    modifier = GlanceModifier.width(90.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        progress = progress,
                        modifier = GlanceModifier.size(72.dp),
                        color = ColorProvider(Color(0xFF4CAF50)),
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = todayInfo?.shiftLabel ?: "Выходной",
                        style = TextStyle(
                            color = ColorProvider(
                                if (todayInfo?.isWorkDay == true) Color(0xFF4CAF50)
                                else Color(0xFF888888)
                            ),
                            fontSize = 10.sp
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.width(8.dp))

                // Right: Metrics
                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WidgetMetric(
                        label = "Заработано",
                        value = "%.0f ₽".format(earned),
                        valueColor = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    WidgetMetric(
                        label = "Ставка / час",
                        value = "%.0f ₽".format(hourlyRate),
                        valueColor = Color(0xFF2196F3)
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    WidgetMetric(
                        label = "Смена",
                        value = "%.1f / %.0f ч".format(hoursWorked, settings.hoursPerShift),
                        valueColor = Color(0xFFFF9800)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            // Week strip
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val dayNames = listOf("Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб")
                weekDays.forEach { day ->
                    val dayName = dayNames[day.date.dayOfWeek.value % 7]
                    Column(
                        modifier = GlanceModifier.defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .size(28.dp)
                                .background(
                                    when {
                                        day.isToday -> Color(0xFF2196F3)
                                        day.isWorkDay -> Color(0xFF1A3A1A)
                                        else -> Color(0xFF2A2A2A)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.date.dayOfMonth.toString(),
                                style = TextStyle(
                                    color = ColorProvider(
                                        when {
                                            day.isToday -> Color.White
                                            day.isWorkDay -> Color(0xFF4CAF50)
                                            else -> Color(0xFF666666)
                                        }
                                    ),
                                    fontSize = 10.sp
                                )
                            )
                        }
                        Text(
                            text = dayName,
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF888888)),
                                fontSize = 9.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetMetric(label: String, value: String, valueColor: Color) {
    Column {
        Text(
            text = label,
            style = TextStyle(
                color = ColorProvider(Color(0xFF888888)),
                fontSize = 10.sp
            )
        )
        Text(
            text = value,
            style = TextStyle(
                color = ColorProvider(valueColor),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}
