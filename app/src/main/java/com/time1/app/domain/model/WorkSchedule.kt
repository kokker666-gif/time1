package com.time1.app.domain.model

enum class WorkSchedule {
    FIVE_TWO,        // 5 work days / 2 days off (Sat/Sun)
    TWO_TWO,         // 2 work days / 2 days off (cyclic)
    DAY_NIGHT_TWO    // Day shift / Night shift / 2 days off (4-day cycle)
}
