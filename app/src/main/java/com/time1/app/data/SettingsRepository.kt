package com.time1.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.time1.app.domain.model.AppSettings
import com.time1.app.domain.model.WorkSchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val MONTHLY_SALARY = doublePreferencesKey("monthly_salary")
        val HOURS_PER_SHIFT = doublePreferencesKey("hours_per_shift")
        val WORK_SCHEDULE = stringPreferencesKey("work_schedule")
        val CYCLE_START_DATE = longPreferencesKey("cycle_start_date")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            monthlySalary = prefs[Keys.MONTHLY_SALARY] ?: 50000.0,
            hoursPerShift = prefs[Keys.HOURS_PER_SHIFT] ?: 8.0,
            workSchedule = WorkSchedule.valueOf(
                prefs[Keys.WORK_SCHEDULE] ?: WorkSchedule.FIVE_TWO.name
            ),
            cycleStartDate = prefs[Keys.CYCLE_START_DATE] ?: System.currentTimeMillis()
        )
    }

    suspend fun saveSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MONTHLY_SALARY] = settings.monthlySalary
            prefs[Keys.HOURS_PER_SHIFT] = settings.hoursPerShift
            prefs[Keys.WORK_SCHEDULE] = settings.workSchedule.name
            prefs[Keys.CYCLE_START_DATE] = settings.cycleStartDate
        }
    }
}
