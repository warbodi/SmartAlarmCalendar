package com.example.smartalarmcalendar.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.example.smartalarmcalendar.MainActivity
import com.example.smartalarmcalendar.R

class SmartAlarmWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            updateAllWidgets(context)
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.example.smartalarmcalendar.ACTION_REFRESH_WIDGET"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val rv = RemoteViews(context.packageName, R.layout.widget_smart_alarm)

            // Intent to launch MainActivity when header is clicked
            val appIntent = Intent(context, MainActivity::class.java)
            val appPendingIntent = PendingIntent.getActivity(
                context,
                0,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Intent for ListView RemoteViewsService
            val serviceIntent = Intent(context, WidgetViewsService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse("content://smartalarm_widget/refresh/${System.currentTimeMillis()}")
            }

            rv.setRemoteAdapter(R.id.widget_alarm_list, serviceIntent)
            rv.setEmptyView(R.id.widget_alarm_list, R.id.widget_empty_view)

            // PendingIntent template for toggle buttons inside ListView items
            val toggleIntent = Intent(context, WidgetToggleReceiver::class.java)
            val togglePendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            rv.setPendingIntentTemplate(R.id.widget_alarm_list, togglePendingIntent)

            // Refresh button PendingIntent
            val refreshIntent = Intent(context, SmartAlarmWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            rv.setOnClickPendingIntent(R.id.btn_refresh_widget, refreshPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, rv)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_alarm_list)
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, SmartAlarmWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_alarm_list)
        }
    }
}
