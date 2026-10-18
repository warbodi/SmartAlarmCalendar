package com.warbodi.smartalarmcalendar.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.warbodi.smartalarmcalendar.data.model.Preset
import com.warbodi.smartalarmcalendar.data.model.PresetAlarm
import com.warbodi.smartalarmcalendar.data.model.PresetWithAlarms
import com.warbodi.smartalarmcalendar.data.repository.AlarmRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AlarmRepository(application)
    private val prefs = application.getSharedPreferences("smart_alarm_prefs", Context.MODE_PRIVATE)

    val presetsWithAlarms: StateFlow<List<PresetWithAlarms>> = repository.allPresetsWithAlarms
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val calendarAssignmentsMap: StateFlow<Map<String, List<Long>>> = repository.allCalendarAssignments
        .map { list -> list.groupBy({ it.dateString }, { it.presetId }) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    val dismissedEarlySet: StateFlow<Set<String>> = com.warbodi.smartalarmcalendar.alarm.AlarmScheduler.dismissedEarlySet

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _appThemeMode = MutableStateFlow(prefs.getString("app_theme_mode", "system") ?: "system")
    val appThemeMode: StateFlow<String> = _appThemeMode.asStateFlow()

    private val _editingPreset = MutableStateFlow<PresetWithAlarms?>(null)
    val editingPreset: StateFlow<PresetWithAlarms?> = _editingPreset.asStateFlow()

    init {
        com.warbodi.smartalarmcalendar.alarm.AlarmScheduler.loadDismissedEarlySet(application)
        // Populate sample presets ONLY ONCE on very first launch
        val hasRunFirstLaunch = prefs.getBoolean("has_run_first_launch", false)
        if (!hasRunFirstLaunch) {
            prefs.edit().putBoolean("has_run_first_launch", true).apply()
            viewModelScope.launch {
                createSamplePresets()
            }
        } else {
            viewModelScope.launch {
                repository.refreshAlarmsAndPreAlarms()
            }
        }
    }

    fun refreshAlarms() {
        viewModelScope.launch {
            repository.refreshAlarmsAndPreAlarms()
        }
    }

    fun setAppThemeMode(mode: String) {
        prefs.edit().putString("app_theme_mode", mode).apply()
        _appThemeMode.value = mode
    }

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun setEditingPreset(presetWithAlarms: PresetWithAlarms?) {
        _editingPreset.value = presetWithAlarms
    }

    fun assignPresetToDate(date: LocalDate, presetId: Long) {
        viewModelScope.launch {
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            repository.assignPresetToDate(dateStr, presetId)
        }
    }

    fun assignPresetToDates(dates: List<LocalDate>, presetId: Long) {
        viewModelScope.launch {
            val dateStrings = dates.map { it.format(DateTimeFormatter.ISO_LOCAL_DATE) }
            repository.assignPresetToDates(dateStrings, presetId)
        }
    }

    fun removePresetFromDate(date: LocalDate, presetId: Long) {
        viewModelScope.launch {
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            repository.removePresetFromDate(dateStr, presetId)
        }
    }

    fun removeAllPresetsFromDate(date: LocalDate) {
        viewModelScope.launch {
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            repository.removeAllPresetsFromDate(dateStr)
        }
    }

    fun savePreset(preset: Preset, alarms: List<PresetAlarm>) {
        viewModelScope.launch {
            repository.savePresetWithAlarms(preset, alarms)
            _editingPreset.value = null
        }
    }

    fun deletePreset(preset: Preset) {
        viewModelScope.launch {
            repository.deletePreset(preset)
        }
    }

    fun toggleAlarmState(date: LocalDate, alarm: PresetAlarm, isEnabled: Boolean) {
        viewModelScope.launch {
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            repository.toggleAlarmEnabled(dateStr, alarm, isEnabled)
        }
    }

    fun createAndAssignTestPreset() {
        viewModelScope.launch {
            val testTime = java.time.LocalTime.now().plusMinutes(1)
            val testPreset = Preset(
                name = "Test",
                colorHex = "#E53935",
                description = "Preset di test (+1 minuto da adesso)"
            )
            val testAlarm = PresetAlarm(
                hour = testTime.hour,
                minute = testTime.minute
            )
            val presetId = repository.savePresetWithAlarms(testPreset, listOf(testAlarm))
            val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            repository.assignPresetToDate(todayStr, presetId)
        }
    }

    private suspend fun createSamplePresets() {
        val preset1 = Preset(
            name = "Turno Mattina",
            colorHex = "#E53935", // Red
            description = "Sveglie per il turno di mattina"
        )
        val alarms1 = listOf(
            PresetAlarm(hour = 6, minute = 0),
            PresetAlarm(hour = 6, minute = 15),
            PresetAlarm(hour = 6, minute = 30)
        )
        repository.savePresetWithAlarms(preset1, alarms1)

        val preset2 = Preset(
            name = "Turno Pomeriggio",
            colorHex = "#1E88E5", // Blue
            description = "Sveglie per il turno pomeridiano"
        )
        val alarms2 = listOf(
            PresetAlarm(hour = 12, minute = 30)
        )
        repository.savePresetWithAlarms(preset2, alarms2)

        val preset3 = Preset(
            name = "Giorno Libero",
            colorHex = "#43A047", // Green
            description = "Sveglia rilassata"
        )
        val alarms3 = listOf(
            PresetAlarm(hour = 9, minute = 30)
        )
        repository.savePresetWithAlarms(preset3, alarms3)
    }
}
