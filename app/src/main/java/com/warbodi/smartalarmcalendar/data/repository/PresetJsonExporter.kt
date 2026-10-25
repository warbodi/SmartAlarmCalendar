package com.warbodi.smartalarmcalendar.data.repository

import com.warbodi.smartalarmcalendar.data.model.Preset
import com.warbodi.smartalarmcalendar.data.model.PresetAlarm
import com.warbodi.smartalarmcalendar.data.model.PresetWithAlarms
import org.json.JSONArray
import org.json.JSONObject

object PresetJsonExporter {

    fun exportToJson(presets: List<PresetWithAlarms>): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", java.time.Instant.now().toString())

        val presetsArray = JSONArray()
        for (presetWithAlarms in presets) {
            val presetObj = JSONObject()
            presetObj.put("name", presetWithAlarms.preset.name)
            presetObj.put("colorHex", presetWithAlarms.preset.colorHex)
            presetObj.put("description", presetWithAlarms.preset.description)

            val alarmsArray = JSONArray()
            for (alarm in presetWithAlarms.alarms) {
                val alarmObj = JSONObject()
                alarmObj.put("hour", alarm.hour)
                alarmObj.put("minute", alarm.minute)
                alarmObj.put("label", alarm.label)
                alarmsArray.put(alarmObj)
            }
            presetObj.put("alarms", alarmsArray)
            presetsArray.put(presetObj)
        }

        root.put("presets", presetsArray)
        return root.toString(2)
    }

    fun parseFromJson(jsonString: String): List<Pair<Preset, List<PresetAlarm>>> {
        val result = mutableListOf<Pair<Preset, List<PresetAlarm>>>()
        val root = JSONObject(jsonString)
        val presetsArray = root.optJSONArray("presets") ?: return emptyList()

        for (i in 0 until presetsArray.length()) {
            val presetObj = presetsArray.getJSONObject(i)
            val name = presetObj.optString("name", "Preset")
            val colorHex = presetObj.optString("colorHex", "#3F51B5")
            val description = presetObj.optString("description", "")

            val preset = Preset(
                name = name,
                colorHex = colorHex,
                description = description
            )

            val alarmsList = mutableListOf<PresetAlarm>()
            val alarmsArray = presetObj.optJSONArray("alarms")
            if (alarmsArray != null) {
                for (j in 0 until alarmsArray.length()) {
                    val alarmObj = alarmsArray.getJSONObject(j)
                    val hour = alarmObj.optInt("hour", 7)
                    val minute = alarmObj.optInt("minute", 0)
                    val label = alarmObj.optString("label", "")

                    alarmsList.add(
                        PresetAlarm(
                            hour = hour.coerceIn(0, 23),
                            minute = minute.coerceIn(0, 59),
                            label = label
                        )
                    )
                }
            }

            result.add(Pair(preset, alarmsList))
        }

        return result
    }
}
