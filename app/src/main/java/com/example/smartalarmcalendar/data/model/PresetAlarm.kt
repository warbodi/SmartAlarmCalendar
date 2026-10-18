package com.example.smartalarmcalendar.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "preset_alarms",
    foreignKeys = [
        ForeignKey(
            entity = Preset::class,
            parentColumns = ["id"],
            childColumns = ["presetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("presetId")]
)
data class PresetAlarm(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val presetId: Long = 0,
    val hour: Int, // 0..23
    val minute: Int, // 0..59
    val label: String = "",
    val isEnabled: Boolean = true
) {
    fun formatTime(): String {
        return String.format("%02d:%02d", hour, minute)
    }
}
