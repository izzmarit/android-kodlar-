package com.example.kuluckakontrolu.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.kuluckakontrolu.model.IncubationCycle
import com.example.kuluckakontrolu.model.IncubationNote
import com.example.kuluckakontrolu.model.IncubatorHistoryRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface IncubatorDao {
    // Tarihsel kayıtlar
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryRecord(record: IncubatorHistoryRecord)

    @Query("SELECT * FROM incubator_history WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    fun getHistoryRecords(startTime: Long, endTime: Long): Flow<List<IncubatorHistoryRecord>>

    @Query("SELECT * FROM incubator_history ORDER BY timestamp DESC LIMIT 1")
    fun getLatestRecord(): Flow<IncubatorHistoryRecord?>

    @Query("SELECT AVG(temperature) FROM incubator_history WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getAverageTemperature(startTime: Long, endTime: Long): Double

    @Query("SELECT AVG(humidity) FROM incubator_history WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getAverageHumidity(startTime: Long, endTime: Long): Double

    // Kuluçka döngüleri
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycle(cycle: IncubationCycle): Long

    @Update
    suspend fun updateCycle(cycle: IncubationCycle)

    @Query("SELECT * FROM incubation_cycles WHERE isActive = 1 LIMIT 1")
    fun getActiveCycle(): Flow<IncubationCycle?>

    @Query("SELECT * FROM incubation_cycles ORDER BY startDate DESC")
    fun getAllCycles(): Flow<List<IncubationCycle>>

    @Query("SELECT * FROM incubation_cycles WHERE id = :cycleId")
    suspend fun getCycleById(cycleId: Long): IncubationCycle?

    // Döngü notları
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: IncubationNote)

    @Query("SELECT * FROM incubation_notes WHERE cycleId = :cycleId ORDER BY timestamp DESC")
    fun getNotesByCycleId(cycleId: Long): Flow<List<IncubationNote>>

    // Özet istatistikler
    @Query("SELECT COUNT(*) FROM incubation_cycles")
    suspend fun getCycleCount(): Int

    @Query("SELECT SUM(endDate - startDate) FROM incubation_cycles WHERE endDate > 0")
    suspend fun getTotalRuntime(): Long?

    // Temizleme işlemleri
    @Query("DELETE FROM incubator_history WHERE timestamp < :timestamp")
    suspend fun deleteOldRecords(timestamp: Long)

    // Döngüyü tamamlama
    @Transaction
    suspend fun finishCycle(cycleId: Long, hatchedEggs: Int) {
        val cycle = getCycleById(cycleId)
        if (cycle != null) {
            val successRate = if (cycle.totalEggs > 0) {
                (hatchedEggs * 100) / cycle.totalEggs
            } else {
                0
            }

            val updatedCycle = cycle.copy(
                isActive = false,
                endDate = System.currentTimeMillis(),
                hatchedEggs = hatchedEggs,
                successRate = successRate
            )

            updateCycle(updatedCycle)
        }
    }
}