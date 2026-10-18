package com.example.smartalarmcalendar.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.smartalarmcalendar.alarm.AlarmScheduler
import com.example.smartalarmcalendar.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WidgetToggleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val dateString = intent.getStringExtra(EXTRA_DATE_STRING) ?: return
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        val currentEnabled = intent.getBooleanExtra(EXTRA_CURRENT_ENABLED, true)

        if (alarmId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getInstance(context)
                val newEnabledState = !currentEnabled

                if (newEnabledState) {
                    // Re-enable
                    AlarmScheduler.unmarkAlarmDismissedEarly(context, dateString, alarmId)
                    val presetAlarm = database.presetDao().getAllPresetsWithAlarms()
                    // Fetch alarm and reschedule
                    val assignments = database.calendarDao().getAssignmentsForDateSync(dateString)
                    for (ass in assignments) {
                        val preset = database.presetDao().getPresetWithAlarmsByIdSync(ass.presetId)
                        preset?.alarms?.find { it.id == alarmId }?.let { alarm ->
                            AlarmScheduler.scheduleAlarmForDate(context, dateString, alarm)
                        }
                    }
                } else {
                    // Disable for this date
                    AlarmScheduler.markAlarmDismissedEarly(context, dateString, alarmId)
                    AlarmScheduler.cancelAlarmForDate(context, dateString, alarmId)
                }

                AlarmScheduler.scheduleNextUpcomingPreAlarm(context)

                // Refresh widget views
                SmartAlarmWidgetProvider.updateAllWidgets(context)

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_ALARM = "com.example.smartalarmcalendar.ACTION_TOGGLE_ALARM"
        const val EXTRA_DATE_STRING = "extra_date_string"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_CURRENT_ENABLED = "extra_current_enabled"
    }
}
