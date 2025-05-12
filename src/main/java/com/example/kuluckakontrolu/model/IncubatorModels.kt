package com.example.kuluckakontrolu.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

// Cihazdan alınan mevcut durum
data class IncubatorState(
    val currentTemp: Double = 0.0,
    val heaterStatus: Boolean = false,
    val currentHumidity: Int = 0,
    val humidifierStatus: Boolean = false,
    val motorStatus: Boolean = false,
    val motorTimeRemaining: Int = 0,
    val currentDay: Int = 0,
    val pidOutput: Double = 0.0,  // PID çıkış değeri (yeni)
    val pidError: Double = 0.0,   // PID hata değeri (yeni)
    val temp1: Double = 0.0,      // Sensör 1 sıcaklık (yeni)
    val temp2: Double = 0.0,      // Sensör 2 sıcaklık (yeni)
    val humidity1: Int = 0,       // Sensör 1 nem (yeni)
    val humidity2: Int = 0        // Sensör 2 nem (yeni)
)

// Kullanıcı tarafından değiştirilebilen ayarlar
data class IncubatorSettings(
    val targetTemp: Double = 37.5,
    val targetHumidity: Int = 60,
    val motorInterval: Int = 120,
    val animalType: String = "TAVUK",
    val currentDay: Int = 0,
    val pidKp: Double = 10.0,  // PID Kp katsayısı (yeni)
    val pidKi: Double = 0.1,   // PID Ki katsayısı (yeni)
    val pidKd: Double = 50.0   // PID Kd katsayısı (yeni)
) {
    val totalDays: Int
        get() = when (animalType) {
            "TAVUK" -> 21
            "ÖRDEK" -> 28
            "BILDIRCIN" -> 17
            "KAZ" -> 30
            "HİNDİ" -> 28
            "SÜLÜN" -> 25
            "GÜVERCİN" -> 18
            "KEKLİK" -> 24
            else -> 21
        }
}

// Birleştirilmiş model
data class IncubatorData(
    val state: IncubatorState = IncubatorState(),
    val settings: IncubatorSettings = IncubatorSettings(),
    val timestamp: Long = System.currentTimeMillis()
)

// Room ile veritabanı kayıtları için entity
@Entity(tableName = "incubator_history")
data class IncubatorHistoryRecord(
    @PrimaryKey val timestamp: Long,
    val temperature: Double,
    val humidity: Int,
    val heaterStatus: Boolean,
    val humidifierStatus: Boolean,
    val motorStatus: Boolean,
    val targetTemp: Double,
    val targetHumidity: Int,
    val animalType: String,
    val currentDay: Int
)

// Kuluçka döngüleri için entity
@Entity(tableName = "incubation_cycles")
data class IncubationCycle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDate: Long,
    val endDate: Long = 0,
    val animalType: String,
    val name: String,
    val notes: String = "",
    val isActive: Boolean = true,
    val successRate: Int = 0,
    val totalEggs: Int = 0,
    val hatchedEggs: Int = 0
)

// Kullanıcı tarafından eklenen notlar
@Entity(tableName = "incubation_notes")
data class IncubationNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cycleId: Long,
    val timestamp: Long,
    val text: String,
    val hasImage: Boolean = false,
    val imagePath: String = ""
)

// Tarih aralığı seçenekleri
enum class TimeRange {
    LAST_HOUR,
    LAST_DAY,
    LAST_WEEK,
    FULL_CYCLE
}

// Grafik türleri
enum class ChartType {
    TEMPERATURE,
    HUMIDITY,
    COMBINED,
    HEATER_ACTIVITY
}

// Hata tipleri
sealed class IncubatorError {
    object CONNECTION_ERROR : IncubatorError()
    object DATA_PARSING_ERROR : IncubatorError()
    object TIMEOUT_ERROR : IncubatorError()
    data class GENERIC_ERROR(val message: String) : IncubatorError()
}

// Yükleme durumu
sealed class LoadingState<out T> {
    object Loading : LoadingState<Nothing>()
    data class Success<T>(val data: T) : LoadingState<T>()
    data class Error(val error: IncubatorError) : LoadingState<Nothing>()
}

// OTA Güncelleme Durum Modeli
data class OtaUpdateStatus(
    val isUpdateAvailable: Boolean = false,
    val currentVersion: String = "",
    val availableVersion: String = "",
    val updateProgress: Int = 0,
    val isUpdating: Boolean = false,
    val errorMessage: String = ""
)

// Sistem Özeti Modeli - Genel durum bilgisi için
data class SystemSummary(
    val cycleCount: Int = 0,
    val activeIncubation: Boolean = false,
    val totalRuntime: Long = 0,  // Toplam milisaniye cinsinden çalışma süresi
    val avgTemperature: Double = 0.0,
    val avgHumidity: Int = 0,
    val lastUpdated: Date = Date()
)