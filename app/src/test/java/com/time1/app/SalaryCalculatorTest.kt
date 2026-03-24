package com.time1.app

import com.time1.app.domain.SalaryCalculator
import com.time1.app.domain.model.AppSettings
import com.time1.app.domain.model.WorkSchedule
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class SalaryCalculatorTest {

    private val cycleStart = LocalDate.of(2024, 1, 1)
        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun `5_2 schedule - weekdays are work days`() {
        val settings = AppSettings(workSchedule = WorkSchedule.FIVE_TWO, cycleStartDate = cycleStart)
        assertTrue(SalaryCalculator.isWorkDay(LocalDate.of(2024, 1, 1), settings)) // Monday
        assertTrue(SalaryCalculator.isWorkDay(LocalDate.of(2024, 1, 5), settings)) // Friday
        assertFalse(SalaryCalculator.isWorkDay(LocalDate.of(2024, 1, 6), settings)) // Saturday
        assertFalse(SalaryCalculator.isWorkDay(LocalDate.of(2024, 1, 7), settings)) // Sunday
    }

    @Test
    fun `2_2 schedule - correct cycle`() {
        val settings = AppSettings(workSchedule = WorkSchedule.TWO_TWO, cycleStartDate = cycleStart)
        // cycle starts Jan 1 2024 (Monday) => day 0, 1 = work, 2, 3 = off
        assertTrue(SalaryCalculator.isWorkDay(LocalDate.of(2024, 1, 1), settings)) // day 0 - work
        assertTrue(SalaryCalculator.isWorkDay(LocalDate.of(2024, 1, 2), settings)) // day 1 - work
        assertFalse(SalaryCalculator.isWorkDay(LocalDate.of(2024, 1, 3), settings)) // day 2 - off
        assertFalse(SalaryCalculator.isWorkDay(LocalDate.of(2024, 1, 4), settings)) // day 3 - off
        assertTrue(SalaryCalculator.isWorkDay(LocalDate.of(2024, 1, 5), settings)) // day 0 - work again
    }

    @Test
    fun `day_night_2 schedule - correct labels`() {
        val settings = AppSettings(workSchedule = WorkSchedule.DAY_NIGHT_TWO, cycleStartDate = cycleStart)
        assertEquals("День", SalaryCalculator.getShiftLabel(LocalDate.of(2024, 1, 1), settings))
        assertEquals("Ночь", SalaryCalculator.getShiftLabel(LocalDate.of(2024, 1, 2), settings))
        assertNull(SalaryCalculator.getShiftLabel(LocalDate.of(2024, 1, 3), settings))
        assertNull(SalaryCalculator.getShiftLabel(LocalDate.of(2024, 1, 4), settings))
    }

    @Test
    fun `hourly rate calculation`() {
        // January 2024 has 23 working days (Mon-Fri, no public holidays in this test)
        val settings = AppSettings(
            monthlySalary = 46000.0,
            hoursPerShift = 8.0,
            workSchedule = WorkSchedule.FIVE_TWO
        )
        val yearMonth = YearMonth.of(2024, 1)
        val workDays = SalaryCalculator.workingDaysInMonth(yearMonth, settings)
        assertEquals(23, workDays)
        val rate = SalaryCalculator.hourlyRate(settings, yearMonth)
        assertEquals(46000.0 / (23 * 8), rate, 0.01)
    }

    @Test
    fun `earned this month is zero at start of first day off`() {
        val settings = AppSettings(
            monthlySalary = 50000.0,
            hoursPerShift = 8.0,
            workSchedule = WorkSchedule.FIVE_TWO
        )
        // Saturday midnight - no earnings
        val saturdayMidnight = LocalDateTime.of(2024, 1, 6, 0, 0, 0)
        val earned = SalaryCalculator.earnedThisMonth(settings, saturdayMidnight)
        // All Mon-Fri of first week are complete (5 days * 8h)
        val yearMonth = YearMonth.of(2024, 1)
        val rate = SalaryCalculator.hourlyRate(settings, yearMonth)
        assertEquals(5 * 8 * rate, earned, 0.01)
    }

    @Test
    fun `shift progress mid-shift`() {
        val settings = AppSettings(
            monthlySalary = 50000.0,
            hoursPerShift = 8.0,
            workSchedule = WorkSchedule.FIVE_TWO
        )
        // Monday noon = 4 hours worked of 8
        val mondayNoon = LocalDateTime.of(2024, 1, 1, 12, 0, 0) // 12:00 = 4h after shift start (8:00)
        val progress = SalaryCalculator.shiftProgress(settings, mondayNoon)
        assertEquals(0.5f, progress, 0.01f)
    }

    @Test
    fun `getCycleDay wraps correctly`() {
        val settings = AppSettings(workSchedule = WorkSchedule.TWO_TWO, cycleStartDate = cycleStart)
        assertEquals(0, SalaryCalculator.getCycleDay(LocalDate.of(2024, 1, 1), settings, 4))
        assertEquals(3, SalaryCalculator.getCycleDay(LocalDate.of(2024, 1, 4), settings, 4))
        assertEquals(0, SalaryCalculator.getCycleDay(LocalDate.of(2024, 1, 5), settings, 4))
    }

    @Test
    fun `weekDaysInfo returns 7 days starting today`() {
        val settings = AppSettings(workSchedule = WorkSchedule.FIVE_TWO)
        val today = LocalDate.of(2024, 1, 1)
        val result = SalaryCalculator.weekDaysInfo(settings, today)
        assertEquals(7, result.size)
        assertTrue(result[0].isToday)
        assertFalse(result[1].isToday)
        assertEquals(today, result[0].date)
        assertEquals(today.plusDays(6), result[6].date)
    }
}
