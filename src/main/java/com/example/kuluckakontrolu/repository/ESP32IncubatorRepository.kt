package com.example.kuluckakontrolu.repository

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.room.RoomDatabase
import com.example.kuluckakontrolu.IncubatorApplication
import com.example.kuluckakontrolu.MainActivity
import com.example.kuluckakontrolu.R
import com.example.kuluckakontrolu.data.AppDatabase
import com.example.kuluckakontrolu.model.*
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import com.itextpdf.text.Document
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ESP32IncubatorRepository(private val context: Context) : IncubatorRepository {

    companion object {
        private const val TAG = "ESP32Repository"
        private const val DEFAULT_ESP32_IP = "192.168.4.1"
        private const val DEFAULT_ESP32_PORT = 80
        private const val PREFS_NAME = "KuluckaSettings"
        // Bağlantı zaman aşımı artırıldı (2s -> 5s)
        private const val CONNECTION_TIMEOUT = 5000L
        // Veri sorgulama aralığı düzenlendi (5s -> 3s)
        private const val DATA_POLLING_INTERVAL = 3000L
        private const val NOTIFICATION_ID = 1001

        // OTA güncelleme ayarları
        private const val OTA_MQTT_BROKER = "mqtt.example.com"
        private const val OTA_MQTT_PORT = 1883
        private const val OTA_MQTT_USERNAME = "otauser"
        private const val OTA_MQTT_PASSWORD = "otapassword"
        private const val OTA_TOPIC_BASE = "esp32/incubator/ota"
    }

    private var socket: Socket? = null
    private var out: PrintWriter? = null
    private var `in`: BufferedReader? = null
    private var isConnected = false
    private var isMonitoring = false
    private var esp32Ip: String
    private var esp32Port: Int

    private var connectionWasEstablished = false
    private var lastConnectionErrorTime = 0L

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val db = AppDatabase.getDatabase(context)
    private val incubatorDao = db.incubatorDao()

    // Firebase bağlantısı - güvenli şekilde başlatma
    private var firebaseDatabase: FirebaseDatabase? = null
    private var firebaseDeviceRef: DatabaseReference? = null

    // Coroutine scope for repository operations
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Data flows
    private val dataFlow = MutableSharedFlow<IncubatorData>(replay = 1)
    private val errorFlow = MutableSharedFlow<IncubatorError>(replay = 1)
    private val cachedData = MutableStateFlow<IncubatorData?>(null)

    // OTA update status
    private val otaUpdateStatusFlow = MutableStateFlow(OtaUpdateStatus())

    // İşlem sayacı - yeniden deneme için
    private var requestCounter = 0

    init {
        // Kaydedilmiş bağlantı bilgilerini yükle
        esp32Ip = prefs.getString("esp32_ip", DEFAULT_ESP32_IP) ?: DEFAULT_ESP32_IP
        esp32Port = prefs.getInt("esp32_port", DEFAULT_ESP32_PORT)

        // Firebase'i güvenli şekilde başlat
        try {
            firebaseDatabase = FirebaseDatabase.getInstance()
            firebaseDeviceRef = firebaseDatabase?.getReference("devices")
            Log.d(TAG, "Firebase başarıyla başlatıldı")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase başlatma hatası: ${e.message}")
            // Firebase olmadığında null değerler kalacak
        }

        // Veri arşivleme işlemini başlat
        startDataArchiving()
    }

    // Verileri düzenli olarak arşivleyen bir fonksiyon
    private fun startDataArchiving() {
        repositoryScope.launch {
            while(true) {
                try {
                    // Her gün gece yarısı veri arşivle
                    val calendar = Calendar.getInstance()
                    val now = calendar.timeInMillis
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    calendar.add(Calendar.DAY_OF_MONTH, 1) // yarınki gün

                    val delayMillis = calendar.timeInMillis - now
                    delay(delayMillis)

                    // Bir aydan eski verileri arşivle
                    val oneMonthAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
                    cleanupOldData(oneMonthAgo)

                } catch (e: Exception) {
                    Log.e(TAG, "Veri arşivleme hatası: ${e.message}")
                }

                // Her gün kontrol et
                delay(24 * 60 * 60 * 1000)
            }
        }
    }

    // ESP32 cihazına komut gönderme fonksiyonu
    private suspend fun sendCommand(command: String, params: Map<String, Any> = emptyMap()): String? {
        if (!isDeviceConnected()) {
            Log.e(TAG, "Komut gönderilemiyor: Cihaz bağlı değil")
            return null
        }

        requestCounter++
        val requestId = requestCounter

        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Komut gönderiliyor [$requestId]: $command, params: $params")
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(esp32Ip, esp32Port), CONNECTION_TIMEOUT.toInt())
                    socket.soTimeout = 8000 // 8 saniye okuma zaman aşımı (artırıldı)

                    val out = PrintWriter(socket.getOutputStream(), true)
                    val input = BufferedReader(InputStreamReader(socket.getInputStream()))

                    // JSON komut nesnesini oluştur - ESP32'nin beklediği formatla uyumlu
                    val jsonCmd = JsonObject()
                    jsonCmd.addProperty("cmd", command)

                    // Ek parametreleri ekle - ESP32'nin WifiModule.cpp dosyasındaki JSON ayrıştırma yapısına uygun
                    params.forEach { (key, value) ->
                        when (value) {
                            is String -> jsonCmd.addProperty(key, value)
                            is Double -> jsonCmd.addProperty(key, value)
                            is Int -> jsonCmd.addProperty(key, value)
                            is Boolean -> jsonCmd.addProperty(key, value as Boolean)
                            else -> jsonCmd.addProperty(key, value.toString())
                        }
                    }

                    // Komutu gönder
                    val cmdJson = gson.toJson(jsonCmd)
                    out.println(cmdJson)
                    out.flush()

                    // Cevabı bekle - geliştirilmiş bekleme mekanizması
                    var response: String? = null
                    val startTime = System.currentTimeMillis()
                    val maxWaitTime = 8000 // 8 saniye (artırıldı)

                    while (System.currentTimeMillis() - startTime < maxWaitTime) {
                        if (input.ready()) {
                            response = input.readLine()
                            if (response != null && response.isNotEmpty()) {
                                Log.d(TAG, "Cevap alındı [$requestId]: $response")
                                break
                            }
                        }
                        delay(100)
                    }

                    if (response == null) {
                        Log.e(TAG, "Cevap alınamadı [$requestId] - zaman aşımı")
                    }

                    return@use response
                }
            } catch (e: Exception) {
                Log.e(TAG, "Komut gönderilirken hata [$requestId]: ${e.message}")
                null
            }
        }
    }

    // Motor kontrolü - WifiModule.cpp'deki "control_motor" komutuna uygun
    suspend fun controlMotor(state: Boolean): Boolean {
        Log.d(TAG, "Motor kontrolü: ${if (state) "açılıyor" else "kapatılıyor"}")
        // "state" parametresi boolean olarak gönderiliyor - ESP32'nin WifiModule.cpp'de beklediği şekilde
        val response = sendCommand("control_motor", mapOf("state" to state))
        return response?.contains("success") == true
    }

    // Hedef sıcaklık ve nem değerlerini ayarla - WifiModule.cpp'deki "set_targets" komutuna uygun
    suspend fun setTargets(temp: Double, humidity: Int): Boolean {
        Log.d(TAG, "Hedef değerler ayarlanıyor: Sıcaklık=$temp, Nem=$humidity")
        val response = sendCommand("set_targets", mapOf("temp" to temp, "humidity" to humidity))
        return response?.contains("success") == true
    }

    // Motor ayarlarını güncelle - WifiModule.cpp'deki "set_motor" komutuna uygun
    suspend fun setMotorSettings(duration: Int, interval: Int): Boolean {
        Log.d(TAG, "Motor ayarları güncelleniyor: duration=$duration, interval=$interval")

        // ESP32'nin beklediği şekilde milisaniye cinsinden değerlere dönüştür
        val durationMs = duration * 1000 // saniye -> milisaniye
        val intervalMs = interval * 60 * 1000 // dakika -> milisaniye

        val response = sendCommand("set_motor", mapOf("duration" to durationMs, "interval" to intervalMs))
        return response?.contains("success") == true
    }

    // Cihaz durumunu al - WifiModule.cpp'deki "get_system_status" komutuna uygun
    suspend fun getSystemStatus(): JsonObject? {
        val response = sendCommand("get_system_status")
        if (response != null) {
            try {
                return gson.fromJson(response, JsonObject::class.java)
            } catch (e: JsonSyntaxException) {
                Log.e(TAG, "Sistem durumu JSON ayrıştırma hatası: ${e.message}")
            }
        }
        return null
    }

    // Sensör verilerini al - WifiModule.cpp'deki "get_sensor_data" komutuna uygun
    suspend fun getSensorData(): JsonObject? {
        val response = sendCommand("get_sensor_data")
        if (response != null) {
            try {
                return gson.fromJson(response, JsonObject::class.java)
            } catch (e: JsonSyntaxException) {
                Log.e(TAG, "Sensör verisi JSON ayrıştırma hatası: ${e.message}")
            }
        }
        return null
    }

    override fun getStoredSettings(): IncubatorSettings {
        val targetTemp = prefs.getFloat("target_temp", 37.5f).toDouble()
        val targetHumidity = prefs.getInt("target_humidity", 60)
        val motorInterval = prefs.getInt("motor_interval", 120)
        val animalType = prefs.getString("animal_type", "TAVUK") ?: "TAVUK"
        val currentDay = prefs.getInt("current_day", 0)
        val pidKp = prefs.getFloat("pid_kp", 10.0f).toDouble()
        val pidKi = prefs.getFloat("pid_ki", 0.1f).toDouble()
        val pidKd = prefs.getFloat("pid_kd", 50.0f).toDouble()

        return IncubatorSettings(
            targetTemp = targetTemp,
            targetHumidity = targetHumidity,
            motorInterval = motorInterval,
            animalType = animalType,
            currentDay = currentDay,
            pidKp = pidKp,
            pidKi = pidKi,
            pidKd = pidKd
        )
    }

    fun saveConnectionSettings(ip: String, port: Int) {
        prefs.edit().apply {
            putString("esp32_ip", ip)
            putInt("esp32_port", port)
            apply()
        }

        // Sınıf değişkenlerini güncelle
        esp32Ip = ip
        esp32Port = port
    }

    fun wasEverConnected(): Boolean {
        return connectionWasEstablished
    }

    override suspend fun isDeviceConnected(): Boolean {
        Log.d(TAG, "isDeviceConnected çağrıldı. ESP32 IP: $esp32Ip")

        return try {
            withContext(Dispatchers.IO) {
                try {
                    val socket = Socket()
                    socket.soTimeout = 3000 // 3 saniye timeout
                    socket.connect(InetSocketAddress(esp32Ip, esp32Port), 3000)

                    if (!socket.isConnected) {
                        socket.close()
                        return@withContext false
                    }

                    // Gerçek bir ESP32 cihazı olup olmadığını kontrol etmek için
                    // özel bir komut gönderip cevabı kontrol edelim
                    val out = PrintWriter(socket.getOutputStream(), true)
                    val input = BufferedReader(InputStreamReader(socket.getInputStream()))

                    // ESP32'nin WifiModule.cpp'deki komut formatına uygun olarak
                    out.println("{\"cmd\":\"ping\"}")
                    out.flush()

                    var response: String? = null
                    val startTime = System.currentTimeMillis()
                    while (System.currentTimeMillis() - startTime < 3000) {
                        if (input.ready()) {
                            response = input.readLine()
                            break
                        }
                        delay(100)
                    }

                    socket.close()

                    // ESP32 cevabını kontrol et
                    val isConnected = if (response != null) {
                        try {
                            val jsonResponse = gson.fromJson(response, JsonObject::class.java)
                            jsonResponse.has("status") && jsonResponse.get("status").asString == "success"
                        } catch (e: Exception) {
                            false
                        }
                    } else {
                        false
                    }

                    if (isConnected) {
                        connectionWasEstablished = true
                    }

                    isConnected
                } catch (e: Exception) {
                    Log.e(TAG, "Bağlantı hatası: ${e.message}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Bağlantı kontrolü sırasında hata: ${e.message}")
            false
        }
    }

    override suspend fun connect(retryCount: Int): Boolean {
        // İlk önce cihazın bağlı olup olmadığını kontrol et
        if (!isDeviceConnected()) {
            Log.e(TAG, "ESP32 cihazı ağda bulunamadı")
            errorFlow.emit(IncubatorError.CONNECTION_ERROR)
            return false
        }

        // Mevcut bağlantıyı temizle
        disconnect()

        for (attempt in 1..retryCount) {
            try {
                Log.d(TAG, "ESP32 bağlantısı kuruluyor... (Deneme $attempt/$retryCount)")

                val newSocket = Socket()
                withContext(Dispatchers.IO) {
                    newSocket.connect(InetSocketAddress(esp32Ip, esp32Port), CONNECTION_TIMEOUT.toInt())
                    newSocket.soTimeout = 5000 // 5 saniyelik okuma zaman aşımı (artırıldı)
                }

                val tempOut = PrintWriter(newSocket.getOutputStream(), true)
                val tempIn = BufferedReader(InputStreamReader(newSocket.getInputStream()))

                // Test mesajı gönder - ESP32'nin anlayacağı formatta
                withContext(Dispatchers.IO) {
                    tempOut.println("{\"cmd\":\"get_sensor_data\"}")
                    tempOut.flush()

                    try {
                        // Cevabı bekle
                        val waitStart = System.currentTimeMillis()
                        val waitTimeout = 3000L

                        var responseReceived = false
                        while (System.currentTimeMillis() - waitStart < waitTimeout) {
                            if (tempIn.ready()) {
                                val response = tempIn.readLine()
                                Log.d(TAG, "ESP32 cevap: $response")

                                // JSON cevabı doğrula
                                try {
                                    gson.fromJson(response, JsonObject::class.java)
                                    responseReceived = true
                                    break
                                } catch (e: Exception) {
                                    Log.e(TAG, "Geçersiz JSON cevabı: ${e.message}")
                                }
                            }
                            delay(100)
                        }

                        if (!responseReceived) {
                            throw IOException("ESP32'den cevap alınamadı")
                        }
                    } catch (e: SocketTimeoutException) {
                        throw IOException("Okuma zaman aşımı")
                    }
                }

                // Bağlantı başarılı
                socket = newSocket
                out = tempOut
                `in` = tempIn
                isConnected = true
                connectionWasEstablished = true

                // Bağlantı başarılı olduğunda hata bildirimlerini sıfırla
                try {
                    // MutableSharedFlow'un replay cache'ini temizle
                    (errorFlow as MutableSharedFlow<IncubatorError>).resetReplayCache()
                } catch (e: Exception) {
                    Log.e(TAG, "Hata akışını sıfırlama sırasında hata: ${e.message}")
                }

                // Firebase'e bağlantı durumunu güncelle
                updateFirebaseConnectionStatus(true)

                Log.d(TAG, "ESP32 bağlantısı başarılı")
                return true

            } catch (e: Exception) {
                Log.e(TAG, "Bağlantı hatası (Deneme $attempt/$retryCount): ${e.message}")

                try {
                    socket?.close()
                } catch (ignore: Exception) {}

                socket = null
                out = null
                `in` = null
                isConnected = false

                errorFlow.emit(IncubatorError.CONNECTION_ERROR)

                if (attempt < retryCount) {
                    delay(1000) // Yeniden denemeden önce bekle
                }
            }
        }

        // Firebase'e bağlantı durumunu güncelle
        updateFirebaseConnectionStatus(false)

        return false
    }

    private fun updateFirebaseConnectionStatus(connected: Boolean) {
        try {
            val deviceId = prefs.getString("device_id", null) ?: return
            firebaseDeviceRef?.child(deviceId)?.child("online")?.setValue(connected)

            if (connected) {
                firebaseDeviceRef?.child(deviceId)?.child("lastSeen")?.setValue(ServerValue.TIMESTAMP)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase güncelleme hatası: ${e.message}")
        }
    }

    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            try {
                isConnected = false
                isMonitoring = false

                socket?.close()
                out?.close()
                `in`?.close()

                socket = null
                out = null
                `in` = null

                // Firebase durumunu güncelle
                updateFirebaseConnectionStatus(false)

            } catch (e: IOException) {
                Log.e(TAG, "Bağlantı kapatma hatası: ${e.message}")
            }
        }
    }

    override fun monitorData(): Flow<IncubatorData> {
        if (!isMonitoring && isConnected) {
            isMonitoring = true

            repositoryScope.launch {
                try {
                    while (isConnected && isMonitoring) {
                        try {
                            // ESP32'den sensör verilerini al
                            val sensorData = getSensorData()

                            // Sensör verilerini işle
                            if (sensorData != null) {
                                processIncomingData(sensorData.toString())
                            } else {
                                // Veri alınamadıysa bağlantıyı kontrol et
                                if (!isDeviceConnected()) {
                                    Log.e(TAG, "Veri alınamadı, bağlantı kopmuş olabilir")
                                    errorFlow.emit(IncubatorError.CONNECTION_ERROR)
                                    isConnected = false
                                    isMonitoring = false
                                    updateFirebaseConnectionStatus(false)
                                    break
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Veri izleme döngüsünde hata: ${e.message}")
                        }

                        delay(DATA_POLLING_INTERVAL)
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Veri okuma hatası: ${e.message}")
                    errorFlow.emit(IncubatorError.CONNECTION_ERROR)
                    isMonitoring = false
                    isConnected = false
                    updateFirebaseConnectionStatus(false)
                } catch (e: Exception) {
                    Log.e(TAG, "Beklenmeyen hata: ${e.message}")
                    errorFlow.emit(IncubatorError.GENERIC_ERROR(e.message ?: "Bilinmeyen hata"))
                    isMonitoring = false
                    updateFirebaseConnectionStatus(false)
                }
            }
        }

        return dataFlow
    }

    override fun monitorErrors(): Flow<IncubatorError> {
        return errorFlow
    }

    override fun getLatestData(): IncubatorData? {
        return cachedData.value
    }

    override suspend fun updateSettings(settings: IncubatorSettings) {
        // Ayarları SharedPreferences'a kaydet
        prefs.edit().apply {
            putFloat("target_temp", settings.targetTemp.toFloat())
            putInt("target_humidity", settings.targetHumidity)
            putInt("motor_interval", settings.motorInterval)
            putString("animal_type", settings.animalType)
            putInt("current_day", settings.currentDay)
            putFloat("pid_kp", settings.pidKp.toFloat())
            putFloat("pid_ki", settings.pidKi.toFloat())
            putFloat("pid_kd", settings.pidKd.toFloat())
            apply()
        }

        // ESP32'ye ayarları gönder
        if (isDeviceConnected()) {
            // Sıcaklık ve nem hedeflerini ayarla
            if (!setTargets(settings.targetTemp, settings.targetHumidity)) {
                Log.e(TAG, "Hedef değerler güncellenemedi")
            }

            // Motor ayarlarını güncelle
            if (!setMotorSettings(14, settings.motorInterval)) {
                Log.e(TAG, "Motor ayarları güncellenemedi")
            }

            // PID parametrelerini güncelle - ESP32'nin beklediği formatta
            val response = sendCommand("update_settings", mapOf(
                "pid_kp" to settings.pidKp,
                "pid_ki" to settings.pidKi,
                "pid_kd" to settings.pidKd
            ))

            if (response?.contains("success") != true) {
                Log.e(TAG, "PID parametreleri güncellenemedi")
            }
        } else {
            Log.e(TAG, "ESP32 cihazı bağlı değil, ayarlar sadece yerel olarak kaydedildi")
            errorFlow.emit(IncubatorError.CONNECTION_ERROR)
        }
    }

    override fun cleanup() {
        repositoryScope.launch {
            disconnect()
        }
        repositoryScope.cancel()
    }

    private suspend fun processIncomingData(data: String) {
        try {
            val jsonObject = gson.fromJson(data, JsonObject::class.java)
            val currentSettings = getStoredSettings()

            // Gelen JSON verisini incele
            // "type" alanını kontrol et - ESP32'nin WifiModule.cpp send_sensor_data fonksiyonu ile uyumlu
            if (jsonObject.has("type") && jsonObject.get("type").asString == "sensor_data") {
                // Gelişmiş sensör verilerini işle (ESP32 JSON formatına uygun şekilde)
                val state = IncubatorState(
                    currentTemp = jsonObject.get("temperature")?.asDouble ?: 0.0,
                    heaterStatus = jsonObject.get("heater_status")?.asBoolean ?: false,
                    currentHumidity = jsonObject.get("humidity")?.asInt ?: 0,
                    humidifierStatus = jsonObject.get("humidifier_status")?.asBoolean ?: false,
                    motorStatus = jsonObject.get("motor_status")?.asBoolean ?: false,
                    motorTimeRemaining = jsonObject.get("motor_time_remaining")?.asInt ?: 0,
                    currentDay = jsonObject.get("current_day")?.asInt ?: 0,
                    // Yeni eklenen detaylı sensör verileri - ESP32'nin WifiModule.cpp formatıyla uyumlu
                    pidOutput = jsonObject.get("pid_output")?.asDouble ?: 0.0,
                    pidError = jsonObject.get("temp_error")?.asDouble ?: 0.0,
                    temp1 = jsonObject.get("temp1")?.asDouble ?: 0.0,
                    temp2 = jsonObject.get("temp2")?.asDouble ?: 0.0,
                    humidity1 = jsonObject.get("hum1")?.asInt ?: 0,
                    humidity2 = jsonObject.get("hum2")?.asInt ?: 0
                )

                val timestamp = System.currentTimeMillis()
                val updatedData = IncubatorData(state = state, settings = currentSettings, timestamp = timestamp)
                cachedData.value = updatedData
                dataFlow.emit(updatedData)

                // Veriyi veritabanına kaydet
                saveHistoryRecord(state, currentSettings, timestamp)

// Firebase'e son verileri güncelle (opsiyonel)
                updateFirebaseLiveData(updatedData)

                // Kritik durum kontrolü
                checkCriticalConditions(updatedData)
            } else {
                Log.d(TAG, "Bilinmeyen veri formatı: $data")
            }
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "JSON ayrıştırma hatası: ${e.message}", e)
            errorFlow.emit(IncubatorError.DATA_PARSING_ERROR)
        }
    }

    private suspend fun saveHistoryRecord(state: IncubatorState, settings: IncubatorSettings, timestamp: Long) {
        try {
            val record = IncubatorHistoryRecord(
                timestamp = timestamp,
                temperature = state.currentTemp,
                humidity = state.currentHumidity,
                heaterStatus = state.heaterStatus,
                humidifierStatus = state.humidifierStatus,
                motorStatus = state.motorStatus,
                targetTemp = settings.targetTemp,
                targetHumidity = settings.targetHumidity,
                animalType = settings.animalType,
                currentDay = settings.currentDay
            )

            incubatorDao.insertHistoryRecord(record)
        } catch (e: Exception) {
            Log.e(TAG, "Veri kaydı hatası: ${e.message}")
        }
    }

    private fun updateFirebaseLiveData(data: IncubatorData) {
        try {
            val deviceId = prefs.getString("device_id", null) ?: return
            val liveDataRef = firebaseDeviceRef?.child(deviceId)?.child("liveData")

            // Kritik verileri güncelle
            val updates = HashMap<String, Any>()
            updates["temperature"] = data.state.currentTemp
            updates["humidity"] = data.state.currentHumidity
            updates["heaterStatus"] = data.state.heaterStatus
            updates["motorStatus"] = data.state.motorStatus
            updates["currentDay"] = data.state.currentDay
            updates["timestamp"] = ServerValue.TIMESTAMP

            liveDataRef?.updateChildren(updates)
        } catch (e: Exception) {
            Log.e(TAG, "Firebase canlı veri güncellemesi hatası: ${e.message}")
        }
    }

    private fun checkCriticalConditions(data: IncubatorData) {
        val temp = data.state.currentTemp
        val targetTemp = data.settings.targetTemp

        // Sıcaklık kritik eşikleri aştı mı?
        if (temp > targetTemp + 1.0 || temp < targetTemp - 1.5) {
            showTemperatureAlert(temp, targetTemp)
        }

        // Nem kontrolü
        val humidity = data.state.currentHumidity
        val targetHumidity = data.settings.targetHumidity
        if (humidity > targetHumidity + 10 || humidity < targetHumidity - 10) {
            showHumidityAlert(humidity, targetHumidity)
        }

        // Sensör hata kontrolü - ESP32'deki AlarmModule.cpp ile uyumlu
        if (data.state.temp1 == 0.0 && data.state.temp2 == 0.0) {
            showSensorAlert()
        }
    }

    private fun showTemperatureAlert(currentTemp: Double, targetTemp: Double) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val message = if (currentTemp > targetTemp) {
            "Sıcaklık çok yüksek: ${String.format("%.1f°C", currentTemp)}"
        } else {
            "Sıcaklık çok düşük: ${String.format("%.1f°C", currentTemp)}"
        }

        // Tıklandığında açılacak activity
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NOTIFICATION_TYPE", "TEMPERATURE")
        }

        // FLAG_IMMUTABLE Android 12+ için kullanılmalı
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(context, 0, intent, flags)

        val notification = NotificationCompat.Builder(context, IncubatorApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Kuluçka Makinesi Uyarısı")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showHumidityAlert(currentHumidity: Int, targetHumidity: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val message = if (currentHumidity > targetHumidity) {
            "Nem seviyesi çok yüksek: %$currentHumidity"
        } else {
            "Nem seviyesi çok düşük: %$currentHumidity"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NOTIFICATION_TYPE", "HUMIDITY")
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(context, 1, intent, flags)

        val notification = NotificationCompat.Builder(context, IncubatorApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Kuluçka Makinesi Nem Uyarısı")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    // Yeni eklenen sensör hatası bildirimi
    private fun showSensorAlert() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val message = "Sensör hatası: Sıcaklık okunamıyor!"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NOTIFICATION_TYPE", "SENSOR_ERROR")
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(context, 3, intent, flags)

        val notification = NotificationCompat.Builder(context, IncubatorApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Kuluçka Makinesi Sensör Hatası")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID + 3, notification)
    }

    private fun showConnectionAlert() {
        // Demo modunda ve ilk kez bağlantı yapılamadığında bildirimleri engelle
        val isDemoMode = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("DemoMode", false)
        if (isDemoMode || !connectionWasEstablished) {
            Log.d(TAG, "Bildirim engellendi: Demo mod=${isDemoMode}, İlk bağlantı=${!connectionWasEstablished}")
            return
        }

        // Bildirim sıklığını sınırla - son 5 dakika içinde bildirim gösterildiyse yeni bildirim gösterme
        val currentTime = System.currentTimeMillis()
        if (lastConnectionErrorTime > 0 && currentTime - lastConnectionErrorTime < 5 * 60 * 1000) {
            Log.d(TAG, "Son bildirimden beri 5 dakika geçmedi, bildirim engellendi")
            return
        }

        try {
            // Bildirimi oluştur ve göster
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("NOTIFICATION_TYPE", "CONNECTION")
            }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = PendingIntent.getActivity(context, 2, intent, flags)
            val notification = NotificationCompat.Builder(context, IncubatorApplication.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.connection_error_title))
                .setContentText(context.getString(R.string.connection_error_message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(NOTIFICATION_ID + 2, notification)

            // Son bildirim zamanını güncelle
            lastConnectionErrorTime = currentTime
        } catch (e: Exception) {
            Log.e(TAG, "Bildirim gösterilirken hata oluştu: ${e.message}")
        }
    }

    // Yeni eklenen işlevler

    override suspend fun getHistoricalData(timeRange: TimeRange): List<IncubatorData> {
        val endTime = System.currentTimeMillis()
        val startTime = when(timeRange) {
            TimeRange.LAST_HOUR -> endTime - TimeUnit.HOURS.toMillis(1)
            TimeRange.LAST_DAY -> endTime - TimeUnit.DAYS.toMillis(1)
            TimeRange.LAST_WEEK -> endTime - TimeUnit.DAYS.toMillis(7)
            TimeRange.FULL_CYCLE -> {
                val activeCycle = incubatorDao.getActiveCycle().first()
                activeCycle?.startDate ?: (endTime - TimeUnit.DAYS.toMillis(21))
            }
        }

        val records = incubatorDao.getHistoryRecords(startTime, endTime).first()

        return records.map { record ->
            val state = IncubatorState(
                currentTemp = record.temperature,
                currentHumidity = record.humidity,
                heaterStatus = record.heaterStatus,
                humidifierStatus = record.humidifierStatus,
                motorStatus = record.motorStatus,
                currentDay = record.currentDay
            )

            val settings = IncubatorSettings(
                targetTemp = record.targetTemp,
                targetHumidity = record.targetHumidity,
                animalType = record.animalType,
                currentDay = record.currentDay
            )

            IncubatorData(state, settings, record.timestamp)
        }
    }

    override fun observeHistoricalData(timeRange: TimeRange): Flow<List<IncubatorData>> = flow {
        val endTime = System.currentTimeMillis()
        val startTime = when(timeRange) {
            TimeRange.LAST_HOUR -> endTime - TimeUnit.HOURS.toMillis(1)
            TimeRange.LAST_DAY -> endTime - TimeUnit.DAYS.toMillis(1)
            TimeRange.LAST_WEEK -> endTime - TimeUnit.DAYS.toMillis(7)
            TimeRange.FULL_CYCLE -> {
                val activeCycle = incubatorDao.getActiveCycle().first()
                activeCycle?.startDate ?: (endTime - TimeUnit.DAYS.toMillis(21))
            }
        }

        emitAll(incubatorDao.getHistoryRecords(startTime, endTime).map { records ->
            records.map { record ->
                val state = IncubatorState(
                    currentTemp = record.temperature,
                    currentHumidity = record.humidity,
                    heaterStatus = record.heaterStatus,
                    humidifierStatus = record.humidifierStatus,
                    motorStatus = record.motorStatus,
                    currentDay = record.currentDay
                )

                val settings = IncubatorSettings(
                    targetTemp = record.targetTemp,
                    targetHumidity = record.targetHumidity,
                    animalType = record.animalType,
                    currentDay = record.currentDay
                )

                IncubatorData(state, settings, record.timestamp)
            }
        })
    }

    override suspend fun startNewCycle(cycle: IncubationCycle): Long {
        // Önce mevcut aktif döngüyü kontrol et
        val activeCycle = incubatorDao.getActiveCycle().first()
        if (activeCycle != null) {
            // Mevcut aktif döngüyü sonlandır
            incubatorDao.finishCycle(activeCycle.id, 0)
        }

        // ESP32'ye kuluçka profilini gönder
        // WifiModule.cpp'deki "start_incubation" komutu ile uyumlu
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

        sendCommand("start_incubation", mapOf("profile_type" to profileType))

        // Yeni döngüyü başlat
        return incubatorDao.insertCycle(cycle.copy(startDate = System.currentTimeMillis()))
    }

    override suspend fun finishCurrentCycle(hatchedEggs: Int) {
        val activeCycle = incubatorDao.getActiveCycle().first() ?: return

        // ESP32'ye kuluçka döngüsünü sonlandır komutu gönder
        sendCommand("stop_incubation")

        incubatorDao.finishCycle(activeCycle.id, hatchedEggs)
    }

    override suspend fun updateCycle(cycle: IncubationCycle) {
        incubatorDao.updateCycle(cycle)
    }

    override fun getActiveCycle(): Flow<IncubationCycle?> {
        return incubatorDao.getActiveCycle()
    }

    override fun getAllCycles(): Flow<List<IncubationCycle>> {
        return incubatorDao.getAllCycles()
    }

    override suspend fun addNote(note: IncubationNote) {
        incubatorDao.insertNote(note)
    }

    override suspend fun addNoteWithImage(cycleId: Long, text: String, imageFile: File) {
        // Görüntü dosyasını dahili depolamaya kopyala
        val imageDir = File(context.filesDir, "incubation_images")
        if (!imageDir.exists()) {
            imageDir.mkdirs()
        }

        val timestamp = System.currentTimeMillis()
        val destFile = File(imageDir, "img_${timestamp}.jpg")

        try {
            imageFile.copyTo(destFile, overwrite = true)

            // Notu ekle
            val note = IncubationNote(
                cycleId = cycleId,
                timestamp = timestamp,
                text = text,
                hasImage = true,
                imagePath = destFile.absolutePath
            )

            incubatorDao.insertNote(note)
        } catch (e: Exception) {
            Log.e(TAG, "Resim kopyalama hatası: ${e.message}")

            // Resim olmadan notu kaydet
            val note = IncubationNote(
                cycleId = cycleId,
                timestamp = timestamp,
                text = "$text (Resim eklenirken hata oluştu: ${e.message})"
            )

            incubatorDao.insertNote(note)
        }
    }

    override fun getNotesByCycleId(cycleId: Long): Flow<List<IncubationNote>> {
        return incubatorDao.getNotesByCycleId(cycleId)
    }

    override suspend fun getSystemSummary(): SystemSummary {
        val cycleCount = incubatorDao.getCycleCount()
        val activeCycle = incubatorDao.getActiveCycle().first()
        val totalRuntime = incubatorDao.getTotalRuntime() ?: 0L

        // Son haftalık verileri al
        val endTime = System.currentTimeMillis()
        val startTime = endTime - TimeUnit.DAYS.toMillis(7)

        val avgTemp = incubatorDao.getAverageTemperature(startTime, endTime)
        val avgHumidity = incubatorDao.getAverageHumidity(startTime, endTime).toInt()

        return SystemSummary(
            cycleCount = cycleCount,
            activeIncubation = activeCycle != null,
            totalRuntime = totalRuntime,
            avgTemperature = avgTemp,
            avgHumidity = avgHumidity,
            lastUpdated = Date()
        )
    }

    override suspend fun generateReport(cycleId: Long): File {
        val cycle = incubatorDao.getCycleById(cycleId) ?: throw IllegalArgumentException("Döngü bulunamadı")
        val notes = incubatorDao.getNotesByCycleId(cycleId).first()

        // Tarih formatı
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getDefault()

        // PDF dosyasını oluştur
        val reportsDir = File(context.filesDir, "reports")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }

        val pdfFile = File(reportsDir, "incubation_report_${cycle.id}.pdf")

        val document = Document()
        PdfWriter.getInstance(document, FileOutputStream(pdfFile))
        document.open()

        // Başlık
        document.add(Paragraph("Kuluçka Raporu: ${cycle.name}"))
        document.add(Paragraph("Hayvan Türü: ${cycle.animalType}"))
        document.add(Paragraph("Başlangıç: ${dateFormat.format(Date(cycle.startDate))}"))

        if (cycle.endDate > 0) {
            document.add(Paragraph("Bitiş: ${dateFormat.format(Date(cycle.endDate))}"))
            document.add(Paragraph("Süre: ${TimeUnit.MILLISECONDS.toDays(cycle.endDate - cycle.startDate)} gün"))
            document.add(Paragraph("Başarı Oranı: %${cycle.successRate}"))
            document.add(Paragraph("Yumurta Sayısı: ${cycle.totalEggs}"))
            document.add(Paragraph("Çıkan Yumurta: ${cycle.hatchedEggs}"))
        } else {
            document.add(Paragraph("Durum: Devam Ediyor"))
            val duration = System.currentTimeMillis() - cycle.startDate
            document.add(Paragraph("Geçen Süre: ${TimeUnit.MILLISECONDS.toDays(duration)} gün"))
        }

        document.add(Paragraph("Notlar:"))
        if (notes.isEmpty()) {
            document.add(Paragraph("Not bulunamadı"))
        } else {
            for (note in notes) {
                document.add(Paragraph("${dateFormat.format(Date(note.timestamp))}: ${note.text}"))
            }
        }

        document.close()

        return pdfFile
    }

    override suspend fun exportDataToCsv(timeRange: TimeRange): File {
        val data = getHistoricalData(timeRange)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

        val exportDir = File(context.filesDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        val csvFile = File(exportDir, "incubator_data_${System.currentTimeMillis()}.csv")

        csvFile.bufferedWriter().use { writer ->
            writer.write("Tarih,Saat,Sıcaklık,Nem,Isıtıcı,Nemlendirici,Motor,Hedef Sıcaklık,Hedef Nem,Gün\n")

            for (record in data) {
                val date = Date(record.timestamp)
                writer.write("${dateFormat.format(date)},")
                writer.write("${record.state.currentTemp},")
                writer.write("${record.state.currentHumidity},")
                writer.write("${if (record.state.heaterStatus) 1 else 0},")
                writer.write("${if (record.state.humidifierStatus) 1 else 0},")
                writer.write("${if (record.state.motorStatus) 1 else 0},")
                writer.write("${record.settings.targetTemp},")
                writer.write("${record.settings.targetHumidity},")
                writer.write("${record.state.currentDay}\n")
            }
        }

        return csvFile
    }

    override suspend fun cleanupOldData(olderThan: Long) {
        try {
            incubatorDao.deleteOldRecords(olderThan)
        } catch (e: Exception) {
            Log.e(TAG, "Eski verileri temizlerken hata: ${e.message}")
        }
    }

    override suspend fun backup(): File {
        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }

        val backupFile = File(backupDir, "incubator_backup_${System.currentTimeMillis()}.zip")

        try {
            // Veritabanı dosyasını kopyala
            val dbFile = context.getDatabasePath("incubator_database")

            // Zip dosyası oluştur
            ZipOutputStream(FileOutputStream(backupFile)).use { zipOut ->
                // Veritabanını ekle
                zipOut.putNextEntry(ZipEntry("database.db"))
                dbFile.inputStream().copyTo(zipOut)
                zipOut.closeEntry()

                // Ayarları JSON olarak ekle
                val settingsJson = gson.toJson(getStoredSettings())
                zipOut.putNextEntry(ZipEntry("settings.json"))
                zipOut.write(settingsJson.toByteArray())
                zipOut.closeEntry()

                // Resim dosyalarını ekle
                val imageDir = File(context.filesDir, "incubation_images")
                if (imageDir.exists() && imageDir.isDirectory) {
                    imageDir.listFiles()?.forEach { imageFile ->
                        zipOut.putNextEntry(ZipEntry("images/${imageFile.name}"))
                        imageFile.inputStream().copyTo(zipOut)
                        zipOut.closeEntry()
                    }
                }
            }

            // Firebase'e yedek kaydı
            val deviceId = prefs.getString("device_id", null)
            if (deviceId != null) {
                firebaseDeviceRef?.child(deviceId)?.child("lastBackup")?.setValue(ServerValue.TIMESTAMP)
            }

            return backupFile
        } catch (e: Exception) {
            Log.e(TAG, "Yedekleme hatası: ${e.message}")
            throw e
        }
    }

    override suspend fun restore(backupFile: File): Boolean {
        try {
            // Önce aktif bağlantıyı kapat
            disconnect()

            // Veritabanını kapat
            (db as RoomDatabase).close()

            // Geçici dizin oluştur
            val tempDir = File(context.cacheDir, "restore_temp")
            if (tempDir.exists()) {
                tempDir.deleteRecursively()
            }
            tempDir.mkdirs()

            // Zip dosyasını aç
            ZipInputStream(backupFile.inputStream()).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    val filePath = File(tempDir, entry.name)

                    // Dizin yapısını oluştur
                    if (entry.isDirectory) {
                        filePath.mkdirs()
                    } else {
                        filePath.parentFile?.mkdirs()

                        // Dosyayı yaz
                        FileOutputStream(filePath).use { output ->
                            zipIn.copyTo(output)
                        }
                    }

                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }

            // Veritabanını geri yükle
            val dbFile = context.getDatabasePath("incubator_database")
            val tempDbFile = File(tempDir, "database.db")
            if (tempDbFile.exists()) {
                tempDbFile.copyTo(dbFile, overwrite = true)
            }

            // Ayarları geri yükle
            val settingsFile = File(tempDir, "settings.json")
            if (settingsFile.exists()) {
                val settingsJson = settingsFile.readText()
                val settings = gson.fromJson(settingsJson, IncubatorSettings::class.java)

                prefs.edit().apply {
                    putFloat("target_temp", settings.targetTemp.toFloat())
                    putInt("target_humidity", settings.targetHumidity)
                    putInt("motor_interval", settings.motorInterval)
                    putString("animal_type", settings.animalType)
                    putInt("current_day", settings.currentDay)
                    putFloat("pid_kp", settings.pidKp.toFloat())
                    putFloat("pid_ki", settings.pidKi.toFloat())
                    putFloat("pid_kd", settings.pidKd.toFloat())
                    apply()
                }
            }

            // Resimleri geri yükle
            val imageSourceDir = File(tempDir, "images")
            val imageTargetDir = File(context.filesDir, "incubation_images")
            if (!imageTargetDir.exists()) {
                imageTargetDir.mkdirs()
            }

            if (imageSourceDir.exists() && imageSourceDir.isDirectory) {
                imageSourceDir.listFiles()?.forEach { srcImage ->
                    srcImage.copyTo(File(imageTargetDir, srcImage.name), overwrite = true)
                }
            }

            // Geçici dizini temizle
            tempDir.deleteRecursively()

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Geri yükleme hatası: ${e.message}")
            return false
        }
    }

    override suspend fun checkForFirmwareUpdates(): OtaUpdateStatus {
        try {
            // ESP32'den mevcut firmware versiyonunu iste
            if (!isDeviceConnected()) {
                return OtaUpdateStatus(
                    isUpdateAvailable = false,
                    errorMessage = "Cihaz bağlı değil"
                )
            }

            // WifiModule.cpp'deki "get_version" komutuyla uyumlu
            val versionResponse = sendCommand("get_version")

            if (versionResponse.isNullOrEmpty()) {
                return OtaUpdateStatus(
                    isUpdateAvailable = false,
                    errorMessage = "Versiyon bilgisi alınamadı"
                )
            }

            // Yanıtı JSON olarak ayrıştır
            val versionJson = gson.fromJson(versionResponse, JsonObject::class.java)
            val currentVersion = versionJson.get("version")?.asString ?: "Bilinmiyor"

            // ESP32'nin AppConfig.h dosyasındaki APP_VERSION ile uyumlu olmalı
            if (currentVersion == "5.0.0") {
                return OtaUpdateStatus(
                    isUpdateAvailable = false,
                    currentVersion = currentVersion,
                    availableVersion = currentVersion
                )
            }

            // Test amacıyla her zaman güncelleme gerekiyor gibi göster
            return OtaUpdateStatus(
                isUpdateAvailable = true,
                currentVersion = currentVersion,
                availableVersion = "5.0.0"
            )

        } catch (e: Exception) {
            Log.e(TAG, "Firmware güncelleme kontrolü hatası: ${e.message}")
            return OtaUpdateStatus(
                isUpdateAvailable = false,
                errorMessage = "Kontrol sırasında hata: ${e.message}"
            )
        }
    }

    override suspend fun startFirmwareUpdate(): Flow<OtaUpdateStatus> = flow {
        try {
            emit(otaUpdateStatusFlow.value.copy(isUpdating = true, updateProgress = 0))

            // MQTT bağlantısı kur
            val clientId = "Android_${System.currentTimeMillis()}"
            val client = MqttClient(
                "tcp://$OTA_MQTT_BROKER:$OTA_MQTT_PORT",
                clientId,
                MemoryPersistence()
            )

            val options = MqttConnectOptions().apply {
                userName = OTA_MQTT_USERNAME
                password = OTA_MQTT_PASSWORD.toCharArray()
                isCleanSession = true
                connectionTimeout = 30
                keepAliveInterval = 60
            }

            client.connect(options)

            // ESP32'ye güncelleme komutu gönder - ESP32'nin WifiModule.cpp'de beklediği formatta
            val updateTopic = "$OTA_TOPIC_BASE/start"
            val message = MqttMessage("{\"cmd\":\"start_update\"}".toByteArray())
            message.qos = 1
            client.publish(updateTopic, message)

// İlerleme durumunu dinle
            client.subscribe("$OTA_TOPIC_BASE/progress") { _, msg ->
                val progressJson = gson.fromJson(String(msg.payload), JsonObject::class.java)
                val progress = progressJson.get("progress")?.asInt ?: 0

                val updatedStatus = otaUpdateStatusFlow.value.copy(
                    isUpdating = progress < 100,
                    updateProgress = progress
                )

                otaUpdateStatusFlow.value = updatedStatus
                repositoryScope.launch {
                    emit(updatedStatus)
                }
            }

            // Güncelleme tamamlanana kadar bekle
            var timeout = 0
            while (otaUpdateStatusFlow.value.updateProgress < 100 && timeout < 300) {
                delay(1000)
                timeout++
            }

            client.disconnect()

            if (timeout >= 300) {
                val timeoutStatus = otaUpdateStatusFlow.value.copy(
                    isUpdating = false,
                    errorMessage = "Güncelleme zaman aşımına uğradı"
                )
                otaUpdateStatusFlow.value = timeoutStatus
                emit(timeoutStatus)
            } else {
                val completedStatus = otaUpdateStatusFlow.value.copy(
                    isUpdating = false,
                    updateProgress = 100
                )
                otaUpdateStatusFlow.value = completedStatus
                emit(completedStatus)
            }

        } catch (e: Exception) {
            val errorStatus = otaUpdateStatusFlow.value.copy(
                isUpdating = false,
                errorMessage = "Güncelleme hatası: ${e.message}"
            )
            otaUpdateStatusFlow.value = errorStatus
            emit(errorStatus)
        }
    }
}