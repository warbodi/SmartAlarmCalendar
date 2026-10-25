package com.warbodi.smartalarmcalendar.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.warbodi.smartalarmcalendar.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class
AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val label = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_LABEL) ?: "Sveglia"
        val time = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_TIME) ?: ""
        var colorHex = intent.getStringExtra(AlarmScheduler.EXTRA_PRESET_COLOR) ?: ""
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        val dateString = intent.getStringExtra(PreAlarmReceiver.EXTRA_DATE_STRING) ?: ""

        val database = com.warbodi.smartalarmcalendar.data.db.AppDatabase.getInstance(context)
        kotlinx.coroutines.runBlocking {
            try {
                if (dateString.isNotBlank()) {
                    val assignments = database.calendarDao().getAssignmentsForDateSync(dateString)
                    for (ass in assignments) {
                        val p = database.presetDao().getPresetWithAlarmsByIdSync(ass.presetId)
                        if (p != null && p.alarms.any { it.id == alarmId }) {
                            colorHex = p.preset.colorHex
                            break
                        }
                    }
                }
                if (colorHex.isBlank() && alarmId != -1L) {
                    val presets = database.presetDao().getAllPresetsWithAlarmsSync()
                    for (presetWithAlarms in presets) {
                        if (presetWithAlarms.alarms.any { it.id == alarmId }) {
                            colorHex = presetWithAlarms.preset.colorHex
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Acquire WakeLock to wake CPU and turn screen on
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        @Suppress("DEPRECATION")
        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
            "SmartAlarmCalendar:AlarmReceiverWakeLock"
        )
        wakeLock.acquire(15000L)

        val ringIntent = Intent(context, AlarmRingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            data = android.net.Uri.parse("alarm_color://${System.currentTimeMillis()}/$colorHex")
            putExtra(AlarmScheduler.EXTRA_ALARM_LABEL, label)
            putExtra(AlarmScheduler.EXTRA_ALARM_TIME, time)
            putExtra(AlarmScheduler.EXTRA_PRESET_COLOR, colorHex)
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Cancel previous notification
        notificationManager.cancel(NOTIFICATION_ID)

        val channelId = "smart_alarm_channel_v7"
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                channelId,
                "Sveglie Smart Alarm",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifiche ed allarmi della sveglia"
                setSound(soundUri, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 500, 500)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        val requestCode = (System.currentTimeMillis() % 1000000).toInt()
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            ringIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Sveglia: $time")
            .setContentText("È ora della sveglia!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 500, 500, 500))
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)

        // Try launching full screen activity directly
        try {
            context.startActivity(ringIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Schedule pre-alarm for the next upcoming alarm in sequence
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AlarmScheduler.scheduleNextUpcomingPreAlarm(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val NOTIFICATION_ID = 1001
    }
}
