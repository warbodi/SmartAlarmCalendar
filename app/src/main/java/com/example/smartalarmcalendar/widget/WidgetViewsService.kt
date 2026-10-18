package com.example.smartalarmcalendar.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.smartalarmcalendar.R
import com.example.smartalarmcalendar.alarm.AlarmScheduler
import com.example.smartalarmcalendar.data.db.AppDatabase
import com.example.smartalarmcalendar.ui.components.parseHexColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class WidgetViewsService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return WidgetAlarmFactory(applicationContext)
    }
}

enum class WidgetItemType { HEADER, ALARM }

data class WidgetListItem(
    val type: WidgetItemType,
    val headerTitle: String = "",
    val dateString: String = "",
    val isToday: Boolean = true,
    val alarmId: Long = 0L,
    val time: String = "",
    val presetName: String = "",
    val presetColorHex: String = "#E53935",
    val isEnabled: Boolean = true
)

class WidgetAlarmFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private val items = mutableListOf<WidgetListItem>()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        items.clear()
        try {
            kotlinx.coroutines.runBlocking {
                val database = AppDatabase.getInstance(context)
                val today = LocalDate.now()
                val tomorrow = today.plusDays(1)

                val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val tomorrowStr = tomorrow.format(DateTimeFormatter.ISO_LOCAL_DATE)

                // Today's Alarms
                val now = java.time.LocalDateTime.now()
                val todayAssignments = database.calendarDao().getAssignmentsForDateSync(todayStr)
                val todayItems = mutableListOf<WidgetListItem>()

                for (assignment in todayAssignments) {
                    val preset = database.presetDao().getPresetWithAlarmsByIdSync(assignment.presetId)
                    preset?.alarms?.forEach { alarm ->
                        val alarmDateTime = java.time.LocalDateTime.of(today, java.time.LocalTime.of(alarm.hour, alarm.minute))
                        if (!alarmDateTime.isBefore(now)) {
                            val isDismissed = AlarmScheduler.isAlarmDismissedEarly(context, todayStr, alarm.id)
                            val effectiveEnabled = alarm.isEnabled && !isDismissed
                            todayItems.add(
                                WidgetListItem(
                                    type = WidgetItemType.ALARM,
                                    dateString = todayStr,
                                    isToday = true,
                                    alarmId = alarm.id,
                                    time = alarm.formatTime(),
                                    presetName = preset.preset.name,
                                    presetColorHex = preset.preset.colorHex,
                                    isEnabled = effectiveEnabled
                                )
                            )
                        }
                    }
                }

                if (todayItems.isNotEmpty()) {
                    todayItems.sortBy { parseTimeToMinutes(it.time) }
                    val todayHeader = "OGGI - " + today.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ITALIAN)).replaceFirstChar { it.uppercase() }
                    items.add(WidgetListItem(type = WidgetItemType.HEADER, headerTitle = todayHeader))
                    items.addAll(todayItems)
                }

                // Tomorrow's Alarms
                val tomorrowAssignments = database.calendarDao().getAssignmentsForDateSync(tomorrowStr)
                val tomorrowItems = mutableListOf<WidgetListItem>()

                for (assignment in tomorrowAssignments) {
                    val preset = database.presetDao().getPresetWithAlarmsByIdSync(assignment.presetId)
                    preset?.alarms?.forEach { alarm ->
                        val isDismissed = AlarmScheduler.isAlarmDismissedEarly(context, tomorrowStr, alarm.id)
                        val effectiveEnabled = alarm.isEnabled && !isDismissed
                        tomorrowItems.add(
                            WidgetListItem(
                                type = WidgetItemType.ALARM,
                                dateString = tomorrowStr,
                                isToday = false,
                                alarmId = alarm.id,
                                time = alarm.formatTime(),
                                presetName = preset.preset.name,
                                presetColorHex = preset.preset.colorHex,
                                isEnabled = effectiveEnabled
                            )
                        )
                    }
                }

                if (tomorrowItems.isNotEmpty()) {
                    tomorrowItems.sortBy { parseTimeToMinutes(it.time) }
                    val tomorrowHeader = "DOMANI - " + tomorrow.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ITALIAN)).replaceFirstChar { it.uppercase() }
                    items.add(WidgetListItem(type = WidgetItemType.HEADER, headerTitle = tomorrowHeader))
                    items.addAll(tomorrowItems)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseTimeToMinutes(timeStr: String): Int {
        return try {
            val parts = timeStr.split(":")
            parts[0].toInt() * 60 + parts[1].toInt()
        } catch (e: Exception) {
            0
        }
    }

    override fun onDestroy() {
        items.clear()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position !in items.indices) return null

        val item = items[position]

        return if (item.type == WidgetItemType.HEADER) {
            val rv = RemoteViews(context.packageName, R.layout.widget_header_item)
            rv.setTextViewText(R.id.tv_widget_header_title, item.headerTitle)
            rv
        } else {
            val rv = RemoteViews(context.packageName, R.layout.widget_alarm_item)

            val color = try {
                android.graphics.Color.parseColor(item.presetColorHex)
            } catch (e: Exception) {
                android.graphics.Color.parseColor("#E53935")
            }

            rv.setInt(R.id.v_preset_accent, "setBackgroundColor", color)
            rv.setTextViewText(R.id.tv_widget_time, item.time)
            rv.setTextViewText(R.id.tv_widget_preset_name, item.presetName)

            rv.setImageViewResource(
                R.id.btn_widget_toggle,
                if (item.isEnabled) R.drawable.widget_switch_on else R.drawable.widget_switch_off
            )

            // FillInIntent for toggle switch click
            val fillInIntent = Intent().apply {
                putExtra(WidgetToggleReceiver.EXTRA_DATE_STRING, item.dateString)
                putExtra(WidgetToggleReceiver.EXTRA_ALARM_ID, item.alarmId)
                putExtra(WidgetToggleReceiver.EXTRA_CURRENT_ENABLED, item.isEnabled)
            }

            rv.setOnClickFillInIntent(R.id.btn_widget_toggle, fillInIntent)
            rv
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 2

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
