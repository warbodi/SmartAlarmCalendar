package com.example.smartalarmcalendar.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.smartalarmcalendar.R

class PreAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        val dateString = intent.getStringExtra(EXTRA_DATE_STRING) ?: ""
        val time = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_TIME) ?: ""

        if (alarmId == -1L || dateString.isBlank()) return

        showPreAlarmNotification(context, alarmId, dateString, time)
    }

    companion object {
        const val PRE_ALARM_NOTIFICATION_ID = 2001
        const val EXTRA_DATE_STRING = "extra_date_string"

        fun showPreAlarmNotification(
            context: Context,
            alarmId: Long,
            dateString: String,
            time: String
        ) {
            val prefs = context.getSharedPreferences("smart_alarm_prefs", Context.MODE_PRIVATE)
            val enablePreAlarm = prefs.getBoolean("enable_pre_alarm", true)
            if (!enablePreAlarm) return

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channelId = "smart_alarm_pre_v5"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Notifica Disattiva Sveglia",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifica per disattivare la sveglia in anticipo"
                    setSound(null, null)
                    enableVibration(false)
                    vibrationPattern = longArrayOf(0L)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                notificationManager.createNotificationChannel(channel)
            }

            val dismissIntent = Intent(context, DismissEarlyReceiver::class.java).apply {
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                putExtra(EXTRA_DATE_STRING, dateString)
                putExtra(AlarmScheduler.EXTRA_ALARM_TIME, time)
            }

            val dismissPendingIntent = PendingIntent.getBroadcast(
                context,
                (dateString.hashCode() * 37 + alarmId.hashCode()).toInt(),
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val rv = RemoteViews(context.packageName, R.layout.notification_pre_alarm)
            rv.setTextViewText(R.id.tv_notif_title, "Prossima sveglia: $time")
            rv.setTextViewText(R.id.tv_notif_subtitle, "Sveglia")
            rv.setOnClickPendingIntent(R.id.btn_notif_dismiss, dismissPendingIntent)

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setCustomContentView(rv)
                .setCustomHeadsUpContentView(rv)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSound(null)
                .setVibrate(longArrayOf(0L))
                .setOngoing(true)
                .setAutoCancel(false)
                .build()

            notificationManager.notify(PRE_ALARM_NOTIFICATION_ID, notification)
        }
    }
}
