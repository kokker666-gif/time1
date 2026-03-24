package com.time1.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.glance.appwidget.updateAll
import com.time1.app.data.SettingsRepository
import com.time1.app.domain.model.AppSettings
import com.time1.app.ui.theme.Time1Theme
import com.time1.app.widget.SalaryWidget
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = SettingsRepository(applicationContext)

        setContent {
            Time1Theme(darkTheme = true) {
                val scope = rememberCoroutineScope()
                var settings by remember { mutableStateOf(AppSettings()) }

                LaunchedEffect(Unit) {
                    repository.settingsFlow.collect { saved ->
                        settings = saved
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SettingsScreen(
                        settings = settings,
                        modifier = Modifier.padding(innerPadding),
                        onSave = { newSettings ->
                            scope.launch {
                                repository.saveSettings(newSettings)
                                settings = newSettings
                                SalaryWidget().updateAll(this@MainActivity)
                            }
                        }
                    )
                }
            }
        }
    }
}
