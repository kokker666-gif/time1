package com.time1.app.domain.model

data class AppSettings(
    val monthlySalary: Double = 50000.0,
    val hoursPerShift: Double = 8.0,
    val workSchedule: WorkSchedule = WorkSchedule.FIVE_TWO,
    val cycleStartDate: Long = System.currentTimeMillis() // epoch millis
)
