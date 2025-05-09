package com.example.kuluckakontrolu

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.kuluckakontrolu.repository.ESP32IncubatorRepository
import com.example.kuluckakontrolu.repository.IncubatorRepository
import android.app.Notification
import android.util.Log
import com.example.kuluckakontrolu.repository.DemoRepository
import com.google.firebase.FirebaseApp

class IncubatorApplication : Application() {

    companion object {
        const val CHANNEL_ID = "IncubatorAlertsChannel"
    }

    lateinit var repository: IncubatorRepository

    override fun onCreate() {
        super.onCreate()

        try {
            FirebaseApp.initializeApp(this)
            Log.d("IncubatorApp", "Firebase başlatıldı")
        } catch (e: Exception) {
            Log.e("IncubatorApp", "Firebase başlatılamadı: ${e.message}")
        }

        // Demo modu kontrolü
        val prefs = getSharedPreferences("KuluckaSettings", MODE_PRIVATE)
        val isDemoMode = prefs.getBoolean("DemoMode", false)

        // Demo modu ayarını kontrol et, eğer ayar yoksa varsayılan olarak false ata
        if (!prefs.contains("DemoMode")) {
            prefs.edit().putBoolean("DemoMode", false).apply()
        }

        // Repository seçimi - demo modu kontrolü doğru uygulanmalı
        repository = if (isDemoMode) {
            Log.d("IncubatorApp", "Demo mod başlatıldı")
            DemoRepository(applicationContext)
        } else {
            Log.d("IncubatorApp", "Normal mod başlatıldı")
            ESP32IncubatorRepository(applicationContext)
        }

        // Bildirim kanalları oluşturma - güncel bildirim kontrolleri
        createNotificationChannels()
    }


    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // Ana uyarı kanalı
                val alertsChannel = NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_alerts_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = getString(R.string.notification_alerts_channel_description)
                    enableVibration(true)
                    enableLights(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }

                // Servis kanalı
                val serviceChannel = NotificationChannel(
                    "IncubatorMonitorChannel",
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = getString(R.string.notification_channel_description)
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }

                // Kanalları kaydet
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannels(listOf(alertsChannel, serviceChannel))

                Log.d("IncubatorApp", "Bildirim kanalları başarıyla oluşturuldu")
            } catch (e: Exception) {
                Log.e("IncubatorApp", "Bildirim kanalı oluşturma hatası: ${e.message}", e)
            }
        }
    }
}