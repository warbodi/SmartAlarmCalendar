package com.example.smartalarmcalendar.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class PresetWithAlarms(
    @Embedded
    val preset: Preset,
    @Relation(
        parentColumn = "id",
        entityColumn = "presetId"
    )
    val alarms: List<PresetAlarm>
)
