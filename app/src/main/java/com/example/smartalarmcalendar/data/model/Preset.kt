package com.example.smartalarmcalendar.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "presets")
data class Preset(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorHex: String = "#3F51B5", // Hex color string e.g. #FF5722
    val description: String = ""
)
