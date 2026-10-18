package com.warbodi.smartalarmcalendar.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.warbodi.smartalarmcalendar.data.model.Preset
import com.warbodi.smartalarmcalendar.data.model.PresetAlarm
import com.warbodi.smartalarmcalendar.data.model.PresetWithAlarms
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {

    @Transaction
    @Query("SELECT * FROM presets ORDER BY id DESC")
    fun getAllPresetsWithAlarms(): Flow<List<PresetWithAlarms>>

    @Transaction
    @Query("SELECT * FROM presets ORDER BY id DESC")
    suspend fun getAllPresetsWithAlarmsSync(): List<PresetWithAlarms>

    @Transaction
    @Query("SELECT * FROM presets WHERE id = :presetId")
    fun getPresetWithAlarmsById(presetId: Long): Flow<PresetWithAlarms?>

    @Transaction
    @Query("SELECT * FROM presets WHERE id = :presetId")
    suspend fun getPresetWithAlarmsByIdSync(presetId: Long): PresetWithAlarms?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: Preset): Long

    @Update
    suspend fun updatePreset(preset: Preset): Int

    @Delete
    suspend fun deletePreset(preset: Preset): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: PresetAlarm): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarms(alarms: List<PresetAlarm>): List<Long>

    @Update
    suspend fun updateAlarm(alarm: PresetAlarm): Int

    @Delete
    suspend fun deleteAlarm(alarm: PresetAlarm): Int

    @Query("DELETE FROM preset_alarms WHERE presetId = :presetId")
    suspend fun deleteAlarmsByPresetId(presetId: Long): Int
}
