package com.example.kuluckakontrolu.repository

import com.example.kuluckakontrolu.model.IncubationCycle
import com.example.kuluckakontrolu.model.IncubationNote
import com.example.kuluckakontrolu.model.IncubatorData
import com.example.kuluckakontrolu.model.IncubatorError
import com.example.kuluckakontrolu.model.IncubatorSettings
import com.example.kuluckakontrolu.model.OtaUpdateStatus
import com.example.kuluckakontrolu.model.SystemSummary
import com.example.kuluckakontrolu.model.TimeRange
import kotlinx.coroutines.flow.Flow
import java.io.File

interface IncubatorRepository {
    // Temel bağlantı fonksiyonları
    suspend fun connect(retryCount: Int = 3): Boolean
    suspend fun disconnect()
    fun monitorData(): Flow<IncubatorData>
    fun monitorErrors(): Flow<IncubatorError>
    suspend fun updateSettings(settings: IncubatorSettings)
    fun getStoredSettings(): IncubatorSettings
    fun getLatestData(): IncubatorData?
    fun cleanup()
    suspend fun isDeviceConnected(): Boolean

    // Yeni gelişmiş özellikler

    // Tarihsel veri erişimi
    suspend fun getHistoricalData(timeRange: TimeRange): List<IncubatorData>
    fun observeHistoricalData(timeRange: TimeRange): Flow<List<IncubatorData>>

    // Kuluçka döngüsü yönetimi
    suspend fun startNewCycle(cycle: IncubationCycle): Long
    suspend fun finishCurrentCycle(hatchedEggs: Int)
    suspend fun updateCycle(cycle: IncubationCycle)
    fun getActiveCycle(): Flow<IncubationCycle?>
    fun getAllCycles(): Flow<List<IncubationCycle>>

    // Not yönetimi
    suspend fun addNote(note: IncubationNote)
    suspend fun addNoteWithImage(cycleId: Long, text: String, imageFile: File)
    fun getNotesByCycleId(cycleId: Long): Flow<List<IncubationNote>>

    // İstatistik ve raporlama
    suspend fun getSystemSummary(): SystemSummary
    suspend fun generateReport(cycleId: Long): File
    suspend fun exportDataToCsv(timeRange: TimeRange): File

    // OTA güncelleme
    suspend fun checkForFirmwareUpdates(): OtaUpdateStatus
    suspend fun startFirmwareUpdate(): Flow<OtaUpdateStatus>

    // Veritabanı bakımı
    suspend fun cleanupOldData(olderThan: Long)
    suspend fun backup(): File
    suspend fun restore(backupFile: File): Boolean
}