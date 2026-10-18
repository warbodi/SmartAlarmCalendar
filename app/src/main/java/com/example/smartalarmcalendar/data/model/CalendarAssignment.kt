package com.example.smartalarmcalendar.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "calendar_assignments",
    primaryKeys = ["dateString", "presetId"],
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
data class CalendarAssignment(
    val dateString: String, // Format "YYYY-MM-DD" e.g., "2025-03-29"
    val presetId: Long
)
