package com.example.smartalarmcalendar.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.smartalarmcalendar.data.model.CalendarAssignment
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarDao {

    @Query("SELECT * FROM calendar_assignments")
    fun getAllAssignments(): Flow<List<CalendarAssignment>>

    @Query("SELECT * FROM calendar_assignments")
    suspend fun getAllAssignmentsSync(): List<CalendarAssignment>

    @Query("SELECT * FROM calendar_assignments WHERE dateString = :dateString")
    fun getAssignmentsForDate(dateString: String): Flow<List<CalendarAssignment>>

    @Query("SELECT * FROM calendar_assignments WHERE dateString = :dateString")
    suspend fun getAssignmentsForDateSync(dateString: String): List<CalendarAssignment>

    @Query("SELECT * FROM calendar_assignments WHERE dateString >= :startDateString")
    suspend fun getAssignmentsFromDateSync(startDateString: String): List<CalendarAssignment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: CalendarAssignment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignments(assignments: List<CalendarAssignment>): List<Long>

    @Query("DELETE FROM calendar_assignments WHERE dateString = :dateString AND presetId = :presetId")
    suspend fun deleteAssignment(dateString: String, presetId: Long): Int

    @Query("DELETE FROM calendar_assignments WHERE dateString = :dateString")
    suspend fun deleteAssignmentsForDate(dateString: String): Int
}
