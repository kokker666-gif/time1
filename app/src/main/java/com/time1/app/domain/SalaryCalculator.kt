package com.time1.app.domain

import com.time1.app.domain.model.AppSettings
import com.time1.app.domain.model.WorkSchedule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.max

/**
 * Core domain logic for salary and schedule calculations.
 */
object SalaryCalculator {

    /**
     * Determines if a given date is a working day based on the schedule.
     */
    fun isWorkDay(date: LocalDate, settings: AppSettings): Boolean {
        return when (settings.workSchedule) {
            WorkSchedule.FIVE_TWO -> {
                val dow = date.dayOfWeek
                dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY
            }
            WorkSchedule.TWO_TWO -> {
                val cycleDay = getCycleDay(date, settings, cycleLength = 4)
                cycleDay == 0 || cycleDay == 1  // days 0,1 = work; 2,3 = off
            }
            WorkSchedule.DAY_NIGHT_TWO -> {
                val cycleDay = getCycleDay(date, settings, cycleLength = 4)
                cycleDay == 0 || cycleDay == 1  // day shift or night shift
            }
        }
    }

    /**
     * Returns shift type label for a given date.
     * Returns null if day is off.
     */
    fun getShiftLabel(date: LocalDate, settings: AppSettings): String? {
        return when (settings.workSchedule) {
            WorkSchedule.FIVE_TWO -> {
                if (isWorkDay(date, settings)) "День" else null
            }
            WorkSchedule.TWO_TWO -> {
                if (isWorkDay(date, settings)) "Работа" else null
            }
            WorkSchedule.DAY_NIGHT_TWO -> {
                val cycleDay = getCycleDay(date, settings, cycleLength = 4)
                when (cycleDay) {
                    0 -> "День"
                    1 -> "Ночь"
                    else -> null
                }
            }
        }
    }

    /**
     * Returns the number of working days in the given month.
     */
    fun workingDaysInMonth(yearMonth: YearMonth, settings: AppSettings): Int {
        return (1..yearMonth.lengthOfMonth()).count { day ->
            isWorkDay(yearMonth.atDay(day), settings)
        }
    }

    /**
     * Calculates hourly rate: monthlySalary / (workingDays * hoursPerShift)
     */
    fun hourlyRate(settings: AppSettings, yearMonth: YearMonth = YearMonth.now()): Double {
        val workDays = workingDaysInMonth(yearMonth, settings)
        if (workDays == 0) return 0.0
        return settings.monthlySalary / (workDays * settings.hoursPerShift)
    }

    /**
     * Calculates the earned salary so far this month up to the current moment.
     */
    fun earnedThisMonth(settings: AppSettings, now: LocalDateTime = LocalDateTime.now()): Double {
        val yearMonth = YearMonth.of(now.year, now.month)
        val rate = hourlyRate(settings, yearMonth)
        if (rate == 0.0) return 0.0

        var totalHours = 0.0
        val today = now.toLocalDate()

        // Sum full working days before today
        var date = yearMonth.atDay(1)
        while (date.isBefore(today)) {
            if (isWorkDay(date, settings)) {
                totalHours += settings.hoursPerShift
            }
            date = date.plusDays(1)
        }

        // Add partial hours for today if it's a work day
        if (isWorkDay(today, settings)) {
            val shiftStartHour = shiftStartHour(today, settings)
            val nowHour = now.hour + now.minute / 60.0 + now.second / 3600.0
            val workedHours = max(0.0, nowHour - shiftStartHour)
            totalHours += workedHours.coerceAtMost(settings.hoursPerShift)
        }

        return totalHours * rate
    }

    /**
     * Returns hours worked today (0..hoursPerShift) if it's a work day, else 0.
     */
    fun hoursWorkedToday(settings: AppSettings, now: LocalDateTime = LocalDateTime.now()): Double {
        if (!isWorkDay(now.toLocalDate(), settings)) return 0.0
        val shiftStartHour = shiftStartHour(now.toLocalDate(), settings)
        val nowHour = now.hour + now.minute / 60.0 + now.second / 3600.0
        return max(0.0, nowHour - shiftStartHour).coerceAtMost(settings.hoursPerShift)
    }

    /**
     * Returns the shift start hour (0-23) for the given date.
     * Day shift starts at 8:00, night shift starts at 20:00.
     */
    fun shiftStartHour(date: LocalDate, settings: AppSettings): Int {
        return if (settings.workSchedule == WorkSchedule.DAY_NIGHT_TWO) {
            val cycleDay = getCycleDay(date, settings, cycleLength = 4)
            if (cycleDay == 1) 20 else 8  // night shift starts 20:00
        } else {
            8
        }
    }

    /**
     * Returns the cycle day index (0-based) for the given date.
     */
    fun getCycleDay(date: LocalDate, settings: AppSettings, cycleLength: Int): Int {
        val startDate = java.time.Instant.ofEpochMilli(settings.cycleStartDate)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        val daysDiff = ChronoUnit.DAYS.between(startDate, date).toInt()
        return ((daysDiff % cycleLength) + cycleLength) % cycleLength
    }

    /**
     * Returns the progress (0f..1f) of the current shift.
     */
    fun shiftProgress(settings: AppSettings, now: LocalDateTime = LocalDateTime.now()): Float {
        if (!isWorkDay(now.toLocalDate(), settings)) return 0f
        val worked = hoursWorkedToday(settings, now)
        return (worked / settings.hoursPerShift).toFloat().coerceIn(0f, 1f)
    }

    /**
     * Returns info for the next 7 days starting from today.
     */
    fun weekDaysInfo(settings: AppSettings, today: LocalDate = LocalDate.now()): List<DayInfo> {
        return (0 until 7).map { offset ->
            val date = today.plusDays(offset.toLong())
            DayInfo(
                date = date,
                isWorkDay = isWorkDay(date, settings),
                shiftLabel = getShiftLabel(date, settings),
                isToday = offset == 0
            )
        }
    }

    /** 
     * Day names indexed by [java.time.DayOfWeek.value] % 7.
     * DayOfWeek values: Mon=1..Sun=7. So 7 % 7 = 0 → "Вс" (Sunday),
     * and 1..6 map directly to Пн..Сб.
     */
    val DAY_NAMES_RU = listOf("Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб")

    data class DayInfo(
        val date: LocalDate,
        val isWorkDay: Boolean,
        val shiftLabel: String?,
        val isToday: Boolean
    )
}
