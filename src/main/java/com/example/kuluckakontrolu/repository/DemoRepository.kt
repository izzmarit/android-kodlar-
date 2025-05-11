package com.example.kuluckakontrolu.repository

import android.content.Context
import com.example.kuluckakontrolu.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import kotlin.random.Random
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class DemoRepository(private val context: Context) : IncubatorRepository {

    private val settings = IncubatorSettings(
        targetTemp = 37.5,
        targetHumidity = 60,
        motorInterval = 120
    )

    override fun getStoredSettings(): IncubatorSettings {
        return settings
    }

    override suspend fun connect(retryCount: Int): Boolean {
        return true
    }

    override suspend fun disconnect() {
        // Demo modunda bağlantı yok
    }

    override fun monitorData(): Flow<IncubatorData> = flow {
        try {
            // Rastgele veri akışı oluştur, ancak coroutine iptalini düzgün işle
            while (currentCoroutineContext().isActive) {
                delay(1000) // 1 saniye bekle
                emit(generateRandomData())
            }
        } catch (e: CancellationException) {
            // Coroutine normal şekilde iptal edildi
            Log.d("DemoRepository", "Demo veri akışı iptal edildi")
        } catch (e: Exception) {
            // Diğer hataları logla
            Log.e("DemoRepository", "Demo veri akışında hata: ${e.message}")
            throw e // Hatayı yukarı ilet
        } finally {
            Log.d("DemoRepository", "Demo veri akışı sonlandı")
        }
    }

    override suspend fun getHistoricalData(timeRange: TimeRange): List<IncubatorData> {
        // Demo veri üretme implementasyonu
        val endTime = System.currentTimeMillis()
        val startTime = when(timeRange) {
            TimeRange.LAST_HOUR -> endTime - 60 * 60 * 1000
            TimeRange.LAST_DAY -> endTime - 24 * 60 * 60 * 1000
            TimeRange.LAST_WEEK -> endTime - 7 * 24 * 60 * 60 * 1000
            TimeRange.FULL_CYCLE -> endTime - 21 * 24 * 60 * 60 * 1000
            else -> endTime - 24 * 60 * 60 * 1000 // Varsayılan olarak 1 gün
        }

        // Demo verisi oluştur
        val result = mutableListOf<IncubatorData>()
        var currentTime = startTime
        while (currentTime < endTime) {
            // Her 15 dakika için bir veri noktası
            val state = IncubatorState(
                currentTemp = 37.5 + Random.nextDouble(-0.5, 0.5),
                currentHumidity = 60 + Random.nextInt(-5, 6),
                heaterStatus = Random.nextBoolean(),
                humidifierStatus = Random.nextBoolean(),
                motorStatus = Random.nextBoolean()
            )
            result.add(IncubatorData(state, settings, currentTime))
            currentTime += 15 * 60 * 1000 // 15 dakika
        }

        return result
    }

    override fun observeHistoricalData(timeRange: TimeRange): Flow<List<IncubatorData>> = flow {
        emit(getHistoricalData(timeRange))
    }

    override fun monitorErrors(): Flow<IncubatorError> = flow {
        // Demo modunda hata yok
    }

    override fun getLatestData(): IncubatorData? {
        return generateRandomData()
    }

    override suspend fun updateSettings(settings: IncubatorSettings) {
        // Demo modunda ayarlar güncellenmez
    }

    override suspend fun isDeviceConnected(): Boolean {
        return true
    }

    override fun cleanup() {
        // Demo modunda temizlik yok
    }

    // Demo veri üretimi için yardımcı metot
    private fun generateRandomData(): IncubatorData {
        val state = IncubatorState(
            currentTemp = 37.5 + Random.nextDouble(-0.2, 0.2),
            currentHumidity = 60 + Random.nextInt(-2, 3),
            heaterStatus = Random.nextBoolean(),
            humidifierStatus = Random.nextBoolean(),
            motorStatus = Random.nextBoolean(),
            motorTimeRemaining = Random.nextInt(0, 120),
            currentDay = Random.nextInt(1, 21),
            pidOutput = Random.nextDouble(50.0, 100.0),
            temp1 = 37.4 + Random.nextDouble(-0.1, 0.1),
            temp2 = 37.6 + Random.nextDouble(-0.1, 0.1),
            humidity1 = 59 + Random.nextInt(-1, 2),
            humidity2 = 61 + Random.nextInt(-1, 2)
        )
        return IncubatorData(state, settings, System.currentTimeMillis())
    }

    // Kuluçka döngüsü yönetimi metodları - demo için basit implementasyonlar
    override suspend fun startNewCycle(cycle: IncubationCycle): Long {
        return 1L // Dummy ID
    }

    override suspend fun finishCurrentCycle(hatchedEggs: Int) {
        // Demo modunda işlem yok
    }

    override suspend fun updateCycle(cycle: IncubationCycle) {
        // Demo modunda işlem yok
    }

    override fun getActiveCycle(): Flow<IncubationCycle?> = flow {
        // Demo için doğru hayvan türü formatı kullanılmalı
        val cycle = IncubationCycle(
            id = 1,
            name = "Demo Kuluçka",
            animalType = "TAVUK",  // Doğru hayvan türleri kullanılmalı
            startDate = System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000,
            isActive = true,
            totalEggs = 24
        )
        emit(cycle)
    }

    override fun getAllCycles(): Flow<List<IncubationCycle>> = flow {
        // Demo kuluçka döngüleri için hayvan türleri düzeltilmeli
        val cycles = listOf(
            IncubationCycle(
                id = 1,
                name = "Demo Kuluçka",
                animalType = "TAVUK",
                startDate = System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000,
                isActive = true,
                totalEggs = 24
            ),
            IncubationCycle(
                id = 2,
                name = "Eski Demo Kuluçka",
                animalType = "ÖRDEK",
                startDate = System.currentTimeMillis() - 40 * 24 * 60 * 60 * 1000,
                endDate = System.currentTimeMillis() - 12 * 24 * 60 * 60 * 1000,
                isActive = false,
                totalEggs = 12,
                hatchedEggs = 10,
                successRate = 83
            )
        )
        emit(cycles)
    }

    override suspend fun addNote(note: IncubationNote) {
        // Demo modunda işlem yok
    }

    override suspend fun addNoteWithImage(cycleId: Long, text: String, imageFile: File) {
        // Demo modunda işlem yok
    }

    override fun getNotesByCycleId(cycleId: Long): Flow<List<IncubationNote>> = flow {
        // Demo için notlar
        val notes = listOf(
            IncubationNote(
                id = 1,
                cycleId = cycleId,
                timestamp = System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000,
                text = "Tüm yumurtalar sağlıklı görünüyor."
            ),
            IncubationNote(
                id = 2,
                cycleId = cycleId,
                timestamp = System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000,
                text = "Sıcaklık kısa süreliğine düştü, durumu kontrol ettim."
            )
        )
        emit(notes)
    }

    override suspend fun getSystemSummary(): SystemSummary {
        return SystemSummary(
            cycleCount = 2,
            activeIncubation = true,
            totalRuntime = 40 * 24 * 60 * 60 * 1000L,
            avgTemperature = 37.4,
            avgHumidity = 59,
            lastUpdated = java.util.Date()
        )
    }

    override suspend fun generateReport(cycleId: Long): File {
        // Demo için boş dosya
        val file = File(context.cacheDir, "demo_report.pdf")
        file.createNewFile()
        return file
    }

    override suspend fun exportDataToCsv(timeRange: TimeRange): File {
        // Demo için boş CSV dosyası
        val file = File(context.cacheDir, "demo_data.csv")
        file.createNewFile()
        return file
    }

    override suspend fun checkForFirmwareUpdates(): OtaUpdateStatus {
        return OtaUpdateStatus(
            isUpdateAvailable = Random.nextBoolean(),
            currentVersion = "1.0.0",
            availableVersion = "2.0.0"
        )
    }

    override suspend fun startFirmwareUpdate(): Flow<OtaUpdateStatus> = flow {
        // Demo güncelleme simülasyonu
        emit(OtaUpdateStatus(isUpdating = true, updateProgress = 0))
        for (progress in 0..100 step 10) {
            kotlinx.coroutines.delay(500)
            emit(OtaUpdateStatus(isUpdating = progress < 100, updateProgress = progress))
        }
    }

    override suspend fun cleanupOldData(olderThan: Long) {
        // Demo modunda işlem yok
    }

    override suspend fun backup(): File {
        // Demo için boş yedek dosyası
        val file = File(context.cacheDir, "demo_backup.zip")
        file.createNewFile()
        return file
    }

    override suspend fun restore(backupFile: File): Boolean {
        return true
    }
}