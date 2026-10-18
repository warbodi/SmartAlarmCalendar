package com.warbodi.smartalarmcalendar.data.repository

import android.content.Context
import com.warbodi.smartalarmcalendar.alarm.AlarmScheduler
import com.warbodi.smartalarmcalendar.data.db.AppDatabase
import com.warbodi.smartalarmcalendar.data.model.CalendarAssignment
import com.warbodi.smartalarmcalendar.data.model.Preset
import com.warbodi.smartalarmcalendar.data.model.PresetAlarm
import com.warbodi.smartalarmcalendar.data.model.PresetWithAlarms
import com.warbodi.smartalarmcalendar.widget.SmartAlarmWidgetProvider
import kotlinx.coroutines.flow.Flow

class AlarmRepository(private val context: Context) {

    private val database = AppDatabase.getInstance(context)
    private val presetDao = database.presetDao()
    private val calendarDao = database.calendarDao()

    val allPresetsWithAlarms: Flow<List<PresetWithAlarms>> = presetDao.getAllPresetsWithAlarms()
    val allCalendarAssignments: Flow<List<CalendarAssignment>> = calendarDao.getAllAssignments()

    fun getPresetWithAlarmsById(presetId: Long): Flow<PresetWithAlarms?> {
        return presetDao.getPresetWithAlarmsById(presetId)
    }

    suspend fun cleanCorruptedData() {
        val presets = presetDao.getAllPresetsWithAlarmsSync()
        for (presetWithAlarms in presets) {
            for (alarm in presetWithAlarms.alarms) {
                val cleanLabel = alarm.label
                    .replace("(Già suonata)", "")
                    .replace("Già suonata", "")
                    .trim()

                if (cleanLabel != alarm.label || !alarm.isEnabled) {
                    presetDao.updateAlarm(alarm.copy(label = cleanLabel, isEnabled = true))
                }
            }
        }
    }

    suspend fun refreshAlarmsAndPreAlarms() {
        cleanCorruptedData()
        AlarmScheduler.rescheduleAllAlarms(context)
        SmartAlarmWidgetProvider.updateAllWidgets(context)
    }

    suspend fun savePresetWithAlarms(preset: Preset, alarms: List<PresetAlarm>): Long {
        if (preset.id != 0L) {
            val existingPreset = presetDao.getPresetWithAlarmsByIdSync(preset.id)
            if (existingPreset != null) {
                val allAssignments = calendarDao.getAllAssignmentsSync()
                val assignedDates = allAssignments.filter { it.presetId == preset.id }.map { it.dateString }
                for (dateStr in assignedDates) {
                    existingPreset.alarms.forEach { alarm ->
                        AlarmScheduler.cancelAlarmForDate(context, dateStr, alarm.id)
                    }
                }
            }
            presetDao.updatePreset(preset)
        }

        val presetId = if (preset.id == 0L) presetDao.insertPreset(preset) else preset.id

        // Remove previous alarms and insert updated list
        presetDao.deleteAlarmsByPresetId(presetId)
        val updatedAlarms = alarms.map {
            it.copy(
                presetId = presetId,
                label = it.label.replace("(Già suonata)", "").replace("Già suonata", "").trim(),
                isEnabled = true
            )
        }
        presetDao.insertAlarms(updatedAlarms)

        // Reschedule alarms, pre-alarms and widget
        AlarmScheduler.rescheduleAllAlarms(context)
        SmartAlarmWidgetProvider.updateAllWidgets(context)

        return presetId
    }

    suspend fun deletePreset(preset: Preset) {
        val existingPreset = presetDao.getPresetWithAlarmsByIdSync(preset.id)
        if (existingPreset != null) {
            val allAssignments = calendarDao.getAllAssignmentsSync()
            val assignedDates = allAssignments.filter { it.presetId == preset.id }.map { it.dateString }
            for (dateStr in assignedDates) {
                existingPreset.alarms.forEach { alarm ->
                    AlarmScheduler.cancelAlarmForDate(context, dateStr, alarm.id)
                }
            }
        }
        presetDao.deletePreset(preset)
        AlarmScheduler.rescheduleAllAlarms(context)
        SmartAlarmWidgetProvider.updateAllWidgets(context)
    }

    suspend fun assignPresetToDate(dateString: String, presetId: Long) {
        val assignment = CalendarAssignment(dateString = dateString, presetId = presetId)
        calendarDao.insertAssignment(assignment)

        // Schedule new alarms for this date
        val newPreset = presetDao.getPresetWithAlarmsByIdSync(presetId)
        newPreset?.alarms?.forEach { alarm ->
            if (alarm.isEnabled) {
                AlarmScheduler.scheduleAlarmForDate(context, dateString, alarm)
            }
        }

        AlarmScheduler.scheduleNextUpcomingPreAlarm(context)
        SmartAlarmWidgetProvider.updateAllWidgets(context)
    }

    suspend fun assignPresetToDates(dateStrings: List<String>, presetId: Long) {
        val newPreset = presetDao.getPresetWithAlarmsByIdSync(presetId) ?: return

        for (dateStr in dateStrings) {
            val assignment = CalendarAssignment(dateString = dateStr, presetId = presetId)
            calendarDao.insertAssignment(assignment)

            newPreset.alarms.forEach { alarm ->
                if (alarm.isEnabled) {
                    AlarmScheduler.scheduleAlarmForDate(context, dateStr, alarm)
                }
            }
        }

        AlarmScheduler.scheduleNextUpcomingPreAlarm(context)
        SmartAlarmWidgetProvider.updateAllWidgets(context)
    }

    suspend fun removePresetFromDate(dateString: String, presetId: Long) {
        val preset = presetDao.getPresetWithAlarmsByIdSync(presetId)
        preset?.alarms?.forEach { alarm ->
            AlarmScheduler.cancelAlarmForDate(context, dateString, alarm.id)
        }
        calendarDao.deleteAssignment(dateString, presetId)

        AlarmScheduler.scheduleNextUpcomingPreAlarm(context)
        SmartAlarmWidgetProvider.updateAllWidgets(context)
    }

    suspend fun removeAllPresetsFromDate(dateString: String) {
        val assignments = calendarDao.getAssignmentsForDateSync(dateString)
        for (assignment in assignments) {
            val preset = presetDao.getPresetWithAlarmsByIdSync(assignment.presetId)
            preset?.alarms?.forEach { alarm ->
                AlarmScheduler.cancelAlarmForDate(context, dateString, alarm.id)
            }
        }
        calendarDao.deleteAssignmentsForDate(dateString)

        AlarmScheduler.scheduleNextUpcomingPreAlarm(context)
        SmartAlarmWidgetProvider.updateAllWidgets(context)
    }

    suspend fun toggleAlarmEnabled(dateString: String, alarm: PresetAlarm, isEnabled: Boolean) {
        if (isEnabled) {
            AlarmScheduler.unmarkAlarmDismissedEarly(context, dateString, alarm.id)
            AlarmScheduler.scheduleAlarmForDate(context, dateString, alarm)
        } else {
            AlarmScheduler.markAlarmDismissedEarly(context, dateString, alarm.id)
            AlarmScheduler.cancelAlarmForDate(context, dateString, alarm.id)
        }

        AlarmScheduler.scheduleNextUpcomingPreAlarm(context)
        SmartAlarmWidgetProvider.updateAllWidgets(context)
    }
}
