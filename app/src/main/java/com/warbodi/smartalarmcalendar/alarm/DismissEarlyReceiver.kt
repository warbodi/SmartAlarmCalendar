package com.warbodi.smartalarmcalendar.alarm

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DismissEarlyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        val dateString = intent.getStringExtra(PreAlarmReceiver.EXTRA_DATE_STRING) ?: ""

        if (alarmId != -1L && dateString.isNotBlank()) {
            // Mark as dismissed early so scheduleNextUpcomingPreAlarm ignores it
            AlarmScheduler.markAlarmDismissedEarly(context, dateString, alarmId)
            // Cancel main alarm
            AlarmScheduler.cancelAlarmForDate(context, dateString, alarmId)
        }

        // Remove pre-alarm notification
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(PreAlarmReceiver.PRE_ALARM_NOTIFICATION_ID)

        // Schedule pre-alarm for the next upcoming alarm and update widget
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AlarmScheduler.scheduleNextUpcomingPreAlarm(context)
                com.warbodi.smartalarmcalendar.widget.SmartAlarmWidgetProvider.updateAllWidgets(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
