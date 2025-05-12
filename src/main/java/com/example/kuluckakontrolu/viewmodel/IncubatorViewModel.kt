package com.example.kuluckakontrolu.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kuluckakontrolu.model.*
import com.example.kuluckakontrolu.repository.ESP32IncubatorRepository
import com.example.kuluckakontrolu.repository.IncubatorRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.launchIn

class IncubatorViewModel(private val repository: IncubatorRepository) : ViewModel() {

    private val _connectionStatus = MutableLiveData<ConnectionStatus>()
    private var previousConnectionStatus: ConnectionStatus? = null
    private var hasShownConnectionError = false
    private var isDemoMode = false
    private var hasNotifiedConnectionError = false

    val connectionStatus: LiveData<ConnectionStatus> = _connectionStatus

    private val _dataState = MutableLiveData<LoadingState<IncubatorData>>()
    val dataState: LiveData<LoadingState<IncubatorData>> = _dataState

    // Tarihsel veri akışı
    private val _historicalData = MutableStateFlow<List<IncubatorData>>(emptyList())
    val historicalData: StateFlow<List<IncubatorData>> = _historicalData

    // Grafik için seçili zaman aralığı
    private val _selectedTimeRange = MutableStateFlow(TimeRange.LAST_DAY)
    val selectedTimeRange: StateFlow<TimeRange> = _selectedTimeRange

    // Aktif kuluçka döngüsü
    private val _activeCycle = MutableStateFlow<IncubationCycle?>(null)
    val activeCycle: StateFlow<IncubationCycle?> = _activeCycle

    // Tüm kuluçka döngüleri
    private val _allCycles = MutableStateFlow<List<IncubationCycle>>(emptyList())
    val allCycles: StateFlow<List<IncubationCycle>> = _allCycles

    // Not listesi
    private val _notes = MutableStateFlow<List<IncubationNote>>(emptyList())
    val notes: StateFlow<List<IncubationNote>> = _notes

    // OTA güncelleme durumu
    private val _otaUpdateStatus = MutableStateFlow(OtaUpdateStatus())
    val otaUpdateStatus: StateFlow<OtaUpdateStatus> = _otaUpdateStatus

    // Sistem özeti
    private val _systemSummary = MutableStateFlow(SystemSummary())
    val systemSummary: StateFlow<SystemSummary> = _systemSummary

    companion object {
        private const val TAG = "IncubatorViewModel"
    }

    init {
        _dataState.value = LoadingState.Loading
        val cachedData = repository.getLatestData()
        if (cachedData != null) {
            _dataState.value = LoadingState.Success(cachedData)
        } else {
            val settings = repository.getStoredSettings()
            _dataState.value = LoadingState.Success(IncubatorData(settings = settings))
        }

        // Bağlantı kontrolünü başlangıçtan kısa süre sonra yap
        viewModelScope.launch {
            delay(500) // 500ms gecikme
            connectToIncubator()
            loadActiveCycle()
            loadAllCycles()
            loadSystemSummary()
        }

        observeErrors()

        // YENİ: Bağlantı durumunu sürekli izleme
        monitorConnectionStatus()
    }

    private fun monitorConnectionStatus() {
        viewModelScope.launch {
            while (true) {
                try {
                    // Demo modunda bu kontrolü atla
                    if (isDemoMode) {
                        delay(60000) // 1 dakika bekle
                        continue
                    }

                    // Şu anki bağlantı durumunu kontrol et
                    if (_connectionStatus.value != ConnectionStatus.CONNECTED) {
                        Log.d(TAG, "Bağlantı durum kontrolü: Şu anda bağlı değil, yeniden bağlanmayı dene")
                        connectToIncubator(2)
                    } else {
                        // Mevcut bağlantı gerçekten aktif mi diye kontrol et
                        val isStillConnected = withTimeoutOrNull(5000) {
                            repository.isDeviceConnected()
                        } ?: false

                        if (!isStillConnected) {
                            Log.d(TAG, "Bağlantı kaybedildi, yeniden bağlanmayı dene")
                            _connectionStatus.value = ConnectionStatus.FAILED
                            connectToIncubator(2)
                        }
                    }

                    // Her 30 saniyede bir kontrol et
                    delay(30000)
                } catch (e: Exception) {
                    Log.e(TAG, "Bağlantı durumu izlemede hata: ${e.message}")
                    delay(15000) // Hata durumunda 15 saniye bekle
                }
            }
        }
    }

    fun connectToIncubator(retryCount: Int = 3) {
        viewModelScope.launch {
            // Demo modunda bu işlemi atla
            if (isDemoMode) {
                _connectionStatus.value = ConnectionStatus.CONNECTED
                Log.d(TAG, "Demo modunda bağlantı denemesi atlanıyor")
                return@launch
            }
            _connectionStatus.value = ConnectionStatus.CONNECTING
            Log.d(TAG, "ESP32'ye bağlanma başlatılıyor...")

            try {
                // Önce repository'den son durumu kontrol et
                val alreadyConnected = withTimeoutOrNull(2000) {
                    repository.isDeviceConnected()
                } ?: false

                if (alreadyConnected) {
                    Log.d(TAG, "Mevcut bağlantı zaten aktif, yeniden bağlanma gerekmiyor")
                    _connectionStatus.value = ConnectionStatus.CONNECTED
                    startDataMonitoring()
                    return@launch
                }

                // Bağlantı denemesinden önce kısa bekle
                delay(1000)

                Log.d(TAG, "ESP32 ile yeni bağlantı kuruluyor...")
                val connected = repository.connect(retryCount)

                if (connected) {
                    _connectionStatus.value = ConnectionStatus.CONNECTED
                    startDataMonitoring()
                    Log.d(TAG, "ESP32 bağlantısı başarılı")
                } else {
                    _connectionStatus.value = ConnectionStatus.FAILED
                    _dataState.value = LoadingState.Error(IncubatorError.CONNECTION_ERROR)
                    Log.e(TAG, "ESP32 bağlantısı başarısız")

                    // 5 saniye sonra tekrar dene
                    delay(5000)
                    retryConnection()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Bağlantı hatası: ${e.message}")
                e.printStackTrace()
                _connectionStatus.value = ConnectionStatus.FAILED
                _dataState.value = LoadingState.Error(
                    IncubatorError.GENERIC_ERROR(e.message ?: "Bilinmeyen hata")
                )

                // 5 saniye sonra tekrar dene
                delay(5000)
                retryConnection()
            }
        }
    }

    private fun startDataMonitoring() {
        viewModelScope.launch {
            try {
                repository.monitorData()
                    .catch { e ->
                        Log.e(TAG, "Veri izleme hatası: ${e.message}")

                        // 3 saniye bekle ve bağlantıyı tekrar dene
                        delay(3000)
                        retryConnection()
                    }
                    .collect { data ->
                        _dataState.value = LoadingState.Success(data)
                        Log.d(TAG, "Veri güncellendi: ${data.state.currentTemp}°C, %${data.state.currentHumidity}")
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Veri akışı başlatılamadı: ${e.message}")
            }
        }
    }

    private fun loadActiveCycle() {
        viewModelScope.launch {
            repository.getActiveCycle().collect { cycle ->
                _activeCycle.value = cycle

                // Aktif döngünün notlarını yükle
                cycle?.let { loadNotes(it.id) }

                // Aktif döngü bilgisini ESP32 ile senkronize et
                if (cycle != null && repository is ESP32IncubatorRepository) {
                    try {
                        // ProfileUtils kullanarak hayvan türü ID'sini almak
                        val profileType = when(cycle.animalType) {
                            "TAVUK" -> 0    // PROFILE_CHICKEN
                            "KAZ" -> 1      // PROFILE_GOOSE
                            "BILDIRCIN" -> 2 // PROFILE_QUAIL
                            "ÖRDEK" -> 3    // PROFILE_DUCK
                            "HİNDİ" -> 5    // PROFILE_TURKEY
                            "KEKLİK" -> 6   // PROFILE_PARTRIDGE
                            "GÜVERCİN" -> 7 // PROFILE_PIGEON
                            "SÜLÜN" -> 8    // PROFILE_PHEASANT
                            else -> 4       // PROFILE_MANUAL
                        }

                        // Kuluçka gününü hesapla
                        val currentDay = if (cycle.startDate > 0) {
                            val now = System.currentTimeMillis()
                            val days = (now - cycle.startDate) / (24 * 60 * 60 * 1000)
                            days.toInt().coerceAtLeast(0)
                        } else {
                            0
                        }

                        // Bu zaten bir coroutine içinde olduğumuz için suspend fonksiyonu direkt çağırabiliriz
                        val success = (repository as ESP32IncubatorRepository).syncProfileSettings(profileType, currentDay)
                        if (!success) {
                            Log.w(TAG, "Profil senkronizasyonu başarısız oldu")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Profil senkronizasyon hatası: ${e.message}")
                    }
                }
            }
        }
    }

    private fun loadAllCycles() {
        viewModelScope.launch {
            repository.getAllCycles().collect { cycles ->
                _allCycles.value = cycles
            }
        }
    }

    fun loadNotes(cycleId: Long) {
        viewModelScope.launch {
            repository.getNotesByCycleId(cycleId).collect { notesList ->
                _notes.value = notesList
            }
        }
    }

    private fun loadSystemSummary() {
        viewModelScope.launch {
            try {
                _systemSummary.value = repository.getSystemSummary()
            } catch (e: Exception) {
                Log.e(TAG, "Sistem özeti yüklenirken hata: ${e.message}")
            }
        }
    }

    private fun observeErrors() {
        viewModelScope.launch {
            try {
                repository.monitorErrors()
                    .catch { error ->
                        Log.e(TAG, "Hata akışında bir istisna oluştu: ${error.message}")
                    }
                    .onEach { error ->
                        // Demo modunda hata bildirimleri gösterme
                        if (isDemoMode) {
                            Log.d(TAG, "Demo modunda hata yok sayıldı: $error")
                            return@onEach
                        }

                        when (error) {
                            is IncubatorError.CONNECTION_ERROR -> {
                                if (!hasShownConnectionError) {
                                    Log.e(TAG, "Bağlantı hatası: Bildirim gösteriliyor")
                                    _connectionStatus.postValue(ConnectionStatus.FAILED)
                                    hasShownConnectionError = true
                                }
                            }
                            is IncubatorError.DATA_PARSING_ERROR -> {
                                Log.e(TAG, "Veri ayrıştırma hatası")
                                _dataState.postValue(LoadingState.Error(error))
                            }
                            is IncubatorError.TIMEOUT_ERROR -> {
                                Log.e(TAG, "Zaman aşımı hatası")
                                _dataState.postValue(LoadingState.Error(error))
                            }
                            is IncubatorError.GENERIC_ERROR -> {
                                Log.e(TAG, "Genel hata: ${(error as IncubatorError.GENERIC_ERROR).message}")
                                _dataState.postValue(LoadingState.Error(error))
                            }
                        }
                    }
                    .collect()
            } catch (e: CancellationException) {
                // Coroutine normal şekilde iptal edildi, loglama yapma
            } catch (e: Exception) {
                Log.e(TAG, "Hata izleme sırasında beklenmeyen bir hata oluştu: ${e.message}")
            }
        }
    }

    private fun updateUIState(newStatus: ConnectionStatus) {
        viewModelScope.launch(Dispatchers.Main) {
            // Eğer aynı durum tekrar gönderiliyorsa, gösterme
            if (previousConnectionStatus == newStatus) {
                return@launch
            }

            _connectionStatus.value = newStatus
            previousConnectionStatus = newStatus
        }
    }

    fun updateSettings(settings: IncubatorSettings) {
        viewModelScope.launch {
            try {
                _dataState.value = LoadingState.Loading

                // Önce bağlantıyı kontrol et
                val isConnected = withTimeoutOrNull(3000) {
                    repository.isDeviceConnected()
                } ?: false

                if (!isConnected && !isDemoMode) {
                    Log.e(TAG, "Ayarlar güncellenemiyor: ESP32 bağlı değil")
                    _dataState.value = LoadingState.Error(IncubatorError.CONNECTION_ERROR)

                    // Bağlantıyı yeniden kurmayı dene ve başarılı olursa ayarları güncelle
                    val reconnected = repository.connect(3)
                    if (!reconnected) {
                        return@launch
                    }
                }

                // Ayarları güncelle
                repository.updateSettings(settings)

                // Verilerin güncellenmesi için kısa bekle
                delay(1000)

                // Güncel verileri yenile - özellikle sensör verileri ve ayarları kontrol et
                startDataMonitoring()

                // PID ayarlarını ve diğer ayarları göster
                val cachedData = repository.getLatestData()
                if (cachedData != null) {
                    _dataState.value = LoadingState.Success(cachedData)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ayar güncelleme hatası: ${e.message}")
                _dataState.value = LoadingState.Error(
                    IncubatorError.GENERIC_ERROR(e.message ?: "Ayarlar güncellenirken hata oluştu")
                )
            }
        }
    }

    fun retryConnection() {
        viewModelScope.launch {
            // Demo modunda bu işlemi atla
            if (isDemoMode) {
                _connectionStatus.value = ConnectionStatus.CONNECTED
                Log.d(TAG, "Demo modunda bağlantı yeniden deneme atlanıyor")
                return@launch
            }
            _connectionStatus.value = ConnectionStatus.CONNECTING

            try {
                // Önce mevcut bağlantıyı temizle
                repository.disconnect()

                // 1 saniye bekle
                delay(1000)

                // Yeniden bağlan
                val connected = repository.connect(3)

                if (connected) {
                    _connectionStatus.value = ConnectionStatus.CONNECTED
                    startDataMonitoring()
                } else {
                    _connectionStatus.value = ConnectionStatus.FAILED
                    _dataState.value = LoadingState.Error(IncubatorError.CONNECTION_ERROR)
                }
            } catch (e: Exception) {
                _connectionStatus.value = ConnectionStatus.FAILED
                _dataState.value = LoadingState.Error(IncubatorError.GENERIC_ERROR(e.message ?: "Bilinmeyen hata"))
            }
        }
    }

    fun setDemoMode(isDemo: Boolean) {
        isDemoMode = isDemo
        if (isDemo) {
            _connectionStatus.value = ConnectionStatus.CONNECTED
            // Demo moduna geçildiğinde aktif bağlantı denemelerini iptal et
            previousConnectionStatus = ConnectionStatus.CONNECTED
            hasShownConnectionError = true
            Log.d(TAG, "Demo modu etkinleştirildi, tüm bağlantı işlemleri atlanacak")
        }
    }

    // Tarihsel veri yükleme
    fun loadHistoricalData(timeRange: TimeRange) {
        viewModelScope.launch {
            _selectedTimeRange.value = timeRange
            try {
                val data = repository.getHistoricalData(timeRange)
                _historicalData.value = data
            } catch (e: Exception) {
                Log.e(TAG, "Tarihsel veri yüklenirken hata: ${e.message}")
            }
        }
    }

    // Yeni kuluçka döngüsü başlatma
    fun startNewCycle(cycle: IncubationCycle) {
        viewModelScope.launch {
            try {
                val cycleId = repository.startNewCycle(cycle)
                loadActiveCycle()
                loadAllCycles()
                loadNotes(cycleId)
            } catch (e: Exception) {
                Log.e(TAG, "Kuluçka döngüsü başlatılırken hata: ${e.message}")
            }
        }
    }

    // Mevcut döngüyü sonlandırma
    fun finishCurrentCycle(hatchedEggs: Int) {
        viewModelScope.launch {
            try {
                repository.finishCurrentCycle(hatchedEggs)
                loadActiveCycle()
                loadAllCycles()
                loadSystemSummary()
            } catch (e: Exception) {
                Log.e(TAG, "Kuluçka döngüsü sonlandırılırken hata: ${e.message}")
            }
        }
    }

    // Not ekleme
    fun addNote(cycleId: Long, text: String) {
        viewModelScope.launch {
            try {
                val note = IncubationNote(
                    cycleId = cycleId,
                    timestamp = System.currentTimeMillis(),
                    text = text
                )
                repository.addNote(note)
            } catch (e: Exception) {
                Log.e(TAG, "Not eklenirken hata: ${e.message}")
            }
        }
    }

    // Resimli not ekleme
    fun addNoteWithImage(cycleId: Long, text: String, imageFile: File) {
        viewModelScope.launch {
            try {
                repository.addNoteWithImage(cycleId, text, imageFile)
            } catch (e: Exception) {
                Log.e(TAG, "Resimli not eklenirken hata: ${e.message}")
            }
        }
    }

    // Rapor oluşturma
    fun generateReport(cycleId: Long): LiveData<File> {
        val reportLiveData = MutableLiveData<File>()

        viewModelScope.launch {
            try {
                val reportFile = repository.generateReport(cycleId)
                reportLiveData.postValue(reportFile)
            } catch (e: Exception) {
                Log.e(TAG, "Rapor oluşturulurken hata: ${e.message}")
            }
        }

        return reportLiveData
    }

    // Veri dışa aktarma
    fun exportDataToCsv(timeRange: TimeRange): LiveData<File> {
        val csvFileLiveData = MutableLiveData<File>()

        viewModelScope.launch {
            try {
                val csvFile = repository.exportDataToCsv(timeRange)
                csvFileLiveData.postValue(csvFile)
            } catch (e: Exception) {
                Log.e(TAG, "Veri dışa aktarılırken hata: ${e.message}")
            }
        }

        return csvFileLiveData
    }

    // OTA güncelleme kontrolü
    fun checkForFirmwareUpdates() {
        viewModelScope.launch {
            try {
                val updateStatus = repository.checkForFirmwareUpdates()
                _otaUpdateStatus.value = updateStatus
            } catch (e: Exception) {
                Log.e(TAG, "Firmware güncelleme kontrolü sırasında hata: ${e.message}")
                _otaUpdateStatus.value = OtaUpdateStatus(errorMessage = "Kontrol sırasında hata: ${e.message}")
            }
        }
    }

    // OTA güncelleme başlatma
    fun startFirmwareUpdate() {
        viewModelScope.launch {
            try {
                repository.startFirmwareUpdate().collect { status ->
                    _otaUpdateStatus.value = status
                }
            } catch (e: Exception) {
                Log.e(TAG, "Firmware güncellemesi sırasında hata: ${e.message}")
                _otaUpdateStatus.value = OtaUpdateStatus(
                    isUpdating = false,
                    errorMessage = "Güncelleme hatası: ${e.message}"
                )
            }
        }
    }

    // Yedekleme
    fun backupData(): LiveData<File> {
        val backupFileLiveData = MutableLiveData<File>()

        viewModelScope.launch {
            try {
                val backupFile = repository.backup()
                backupFileLiveData.postValue(backupFile)
            } catch (e: Exception) {
                Log.e(TAG, "Yedekleme sırasında hata: ${e.message}")
            }
        }

        return backupFileLiveData
    }

    // Geri yükleme
    fun restoreData(backupFile: File): LiveData<Boolean> {
        val restoreResultLiveData = MutableLiveData<Boolean>()

        viewModelScope.launch {
            try {
                val result = repository.restore(backupFile)
                restoreResultLiveData.postValue(result)

                if (result) {
                    // Başarılı geri yükleme sonrası verileri yeniden yükle
                    loadActiveCycle()
                    loadAllCycles()
                    loadSystemSummary()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Geri yükleme sırasında hata: ${e.message}")
                restoreResultLiveData.postValue(false)
            }
        }

        return restoreResultLiveData
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            repository.disconnect()
        }
    }

    enum class ConnectionStatus {
        DISCONNECTED, CONNECTING, CONNECTED, FAILED
    }
}