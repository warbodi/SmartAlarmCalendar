package com.warbodi.smartalarmcalendar.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.warbodi.smartalarmcalendar.data.db.AppDatabase
import com.warbodi.smartalarmcalendar.data.model.PresetAlarm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object AlarmScheduler {

    const val EXTRA_ALARM_ID = "extra_alarm_id"
    const val EXTRA_ALARM_LABEL = "extra_alarm_label"
    const val EXTRA_ALARM_TIME = "extra_alarm_time"
    const val EXTRA_PRESET_COLOR = "extra_preset_color"
    private const val PRE_ALARM_REQUEST_CODE = 9999

    private val _dismissedEarlySet = MutableStateFlow<Set<String>>(emptySet())
    val dismissedEarlySet: StateFlow<Set<String>> = _dismissedEarlySet.asStateFlow()

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun loadDismissedEarlySet(context: Context) {
        val prefs = context.getSharedPreferences("smart_alarm_prefs", Context.MODE_PRIVATE)
        val set = prefs.getStringSet("dismissed_early_set", emptySet()) ?: emptySet()
        _dismissedEarlySet.value = set
    }

    fun markAlarmDismissedEarly(context: Context, dateString: String, alarmId: Long) {
        val prefs = context.getSharedPreferences("smart_alarm_prefs", Context.MODE_PRIVATE)
        val set = prefs.getStringSet("dismissed_early_set", emptySet()) ?: emptySet()
        val newSet = set.toMutableSet()
        newSet.add("${dateString}_$alarmId")
        prefs.edit().putStringSet("dismissed_early_set", newSet).apply()
        _dismissedEarlySet.value = newSet
    }

    fun unmarkAlarmDismissedEarly(context: Context, dateString: String, alarmId: Long) {
        val prefs = context.getSharedPreferences("smart_alarm_prefs", Context.MODE_PRIVATE)
        val set = prefs.getStringSet("dismissed_early_set", emptySet()) ?: emptySet()
        val newSet = set.toMutableSet()
        newSet.remove("${dateString}_$alarmId")
        prefs.edit().putStringSet("dismissed_early_set", newSet).apply()
        _dismissedEarlySet.value = newSet
    }

    fun isAlarmDismissedEarly(context: Context, dateString: String, alarmId: Long): Boolean {
        val prefs = context.getSharedPreferences("smart_alarm_prefs", Context.MODE_PRIVATE)
        val set = prefs.getStringSet("dismissed_early_set", emptySet()) ?: emptySet()
        return set.contains("${dateString}_$alarmId")
    }

    fun scheduleAlarmForDate(
        context: Context,
        dateString: String, // "YYYY-MM-DD"
        alarm: PresetAlarm,
        colorHex: String = "#E53935"
    ) {
        if (!alarm.isEnabled) return
        if (isAlarmDismissedEarly(context, dateString, alarm.id)) return
        if (!canScheduleExactAlarms(context)) return

        try {
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE
            val date = LocalDate.parse(dateString, formatter)
            val time = LocalTime.of(alarm.hour, alarm.minute)
            val dateTime = LocalDateTime.of(date, time)

            val triggerAtMillis = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            if (triggerAtMillis <= System.currentTimeMillis()) return

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(EXTRA_ALARM_ID, alarm.id)
                putExtra(PreAlarmReceiver.EXTRA_DATE_STRING, dateString)
                putExtra(EXTRA_ALARM_LABEL, alarm.label)
                putExtra(EXTRA_ALARM_TIME, alarm.formatTime())
                putExtra(EXTRA_PRESET_COLOR, colorHex)
            }

            val requestCode = generateRequestCode(dateString, alarm.id)

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, pendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelAlarmForDate(context: Context, dateString: String, alarmId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val requestCode = generateRequestCode(dateString, alarmId)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    suspend fun rescheduleAllAlarms(context: Context) {
        loadDismissedEarlySet(context)
        val database = AppDatabase.getInstance(context)
        val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val futureAssignments = database.calendarDao().getAssignmentsFromDateSync(todayStr)

        for (assignment in futureAssignments) {
            val presetWithAlarms = database.presetDao().getPresetWithAlarmsByIdSync(assignment.presetId)
            presetWithAlarms?.alarms?.forEach { alarm ->
                if (alarm.isEnabled && !isAlarmDismissedEarly(context, assignment.dateString, alarm.id)) {
                    scheduleAlarmForDate(context, assignment.dateString, alarm, presetWithAlarms.preset.colorHex)
                }
            }
        }

        scheduleNextUpcomingPreAlarm(context)
    }

    suspend fun scheduleNextUpcomingPreAlarm(context: Context) {
        val database = AppDatabase.getInstance(context)
        val now = LocalDateTime.now()
        val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        val assignments = database.calendarDao().getAssignmentsFromDateSync(todayStr)

        data class AlarmOccurrence(
            val dateString: String,
            val alarm: PresetAlarm,
            val triggerDateTime: LocalDateTime
        )

        val upcomingOccurrences = mutableListOf<AlarmOccurrence>()

        for (assignment in assignments) {
            val presetWithAlarms = database.presetDao().getPresetWithAlarmsByIdSync(assignment.presetId)
            presetWithAlarms?.alarms?.forEach { alarm ->
                if (alarm.isEnabled && !isAlarmDismissedEarly(context, assignment.dateString, alarm.id)) {
                    val date = LocalDate.parse(assignment.dateString, DateTimeFormatter.ISO_LOCAL_DATE)
                    val dateTime = LocalDateTime.of(date, LocalTime.of(alarm.hour, alarm.minute))
                    if (dateTime.isAfter(now)) {
                        upcomingOccurrences.add(AlarmOccurrence(assignment.dateString, alarm, dateTime))
                    }
                }
            }
        }

        upcomingOccurrences.sortBy { it.triggerDateTime }

        val nextAlarm = upcomingOccurrences.firstOrNull()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val cancelIntent = Intent(context, PreAlarmReceiver::class.java)
        val previousPendingIntent = PendingIntent.getBroadcast(
            context,
            PRE_ALARM_REQUEST_CODE,
            cancelIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        previousPendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }

        if (nextAlarm == null) return

        val preAlarmDateTime = nextAlarm.triggerDateTime.minusHours(1)
        val preAlarmMillis = preAlarmDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val intent = Intent(context, PreAlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, nextAlarm.alarm.id)
            putExtra(PreAlarmReceiver.EXTRA_DATE_STRING, nextAlarm.dateString)
            putExtra(EXTRA_ALARM_TIME, nextAlarm.alarm.formatTime())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            PRE_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (preAlarmDateTime.isAfter(now)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, preAlarmMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, preAlarmMillis, pendingIntent)
            }
        } else {
            PreAlarmReceiver.showPreAlarmNotification(
                context,
                nextAlarm.alarm.id,
                nextAlarm.dateString,
                nextAlarm.alarm.formatTime()
            )
        }
    }

    private fun generateRequestCode(dateString: String, alarmId: Long): Int {
        return (dateString.hashCode() * 31 + alarmId.hashCode()).toInt()
    }
}
