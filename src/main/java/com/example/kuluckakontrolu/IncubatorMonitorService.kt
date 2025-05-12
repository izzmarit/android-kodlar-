package com.example.kuluckakontrolu

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.kuluckakontrolu.model.IncubationCycle
import com.example.kuluckakontrolu.model.IncubatorError
import com.example.kuluckakontrolu.repository.IncubatorRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException

class IncubatorMonitorService : LifecycleService() {

    companion object {
        private const val TAG = "IncubatorService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "IncubatorMonitorChannel"

        // Monitoring constants
        private const val CONNECTION_CHECK_INTERVAL = 15 * 60 * 1000L // 15 minutes
        private const val DATA_CHECK_INTERVAL = 10 * 60 * 1000L // 10 minutes
        private const val CONNECTION_TIMEOUT = 10000L // 10 seconds
    }

    private lateinit var repository: IncubatorRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isDemoMode = false
    private var activeCycle: IncubationCycle? = null

    override fun onCreate() {
        super.onCreate()
        repository = (application as IncubatorApplication).repository

        // Create notification channel
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // Check if demo mode is enabled from intent
        isDemoMode = intent?.getBooleanExtra("isDemoMode", false) ?: false

        // Start service with foreground notification
        startForeground(NOTIFICATION_ID, createNotification())

        // Start background monitoring only if not in demo mode
        if (!isDemoMode) {
            startActiveMonitoring()
        } else {
            Log.d(TAG, "Demo modunda aktif izleme başlatılmadı")
        }

        return Service.START_STICKY
    }

    private fun startActiveMonitoring() {
        // Sadece demo modunda değilse aktif izleme başlat
        if (isDemoMode) {
            Log.d(TAG, "Demo modunda izleme atlandı")
            return
        }

        // Aktif döngü yükleme
        lifecycleScope.launch {
            try {
                repository.getActiveCycle().collect { cycle ->
                    activeCycle = cycle
                    updateNotification()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading active cycle: ${e.message}")
            }
        }

        // 2. Monitor connection
        serviceScope.launch {
            while (isActive) {
                try {
                    if (!isDemoMode) {
                        // Check if device is connected
                        val connected = withTimeoutOrNull(CONNECTION_TIMEOUT) {
                            repository.isDeviceConnected()
                        } ?: false

                        // If not connected and there's an active cycle, try to reconnect
                        if (!connected && activeCycle != null) {
                            Log.i(TAG, "Connection lost, attempting to reconnect...")
                            repository.connect(3)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking connection: ${e.message}")
                }

                delay(CONNECTION_CHECK_INTERVAL)
            }
        }

        // 3. Monitor data for critical conditions
        serviceScope.launch {
            try {
                repository.monitorErrors()
                    .filter { it is IncubatorError.CONNECTION_ERROR }
                    .collect {
                        // Update the notification
                        updateNotification(connectionLost = true)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error in error monitoring: ${e.message}")
            }
        }

        // 4. Monitor system health (cleanup old data)
        serviceScope.launch {
            while (isActive) {
                try {
                    // Clean up old records (older than 30 days)
                    val thirtyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
                    repository.cleanupOldData(thirtyDaysAgo)
                } catch (e: Exception) {
                    Log.e(TAG, "Error cleaning up old data: ${e.message}")
                }

                // Run cleanup once per day
                delay(TimeUnit.DAYS.toMillis(1))
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = getString(R.string.notification_channel_description)

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(connectionLost: Boolean = false): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        val title = if (isDemoMode) {
            getString(R.string.service_running_demo)
        } else if (connectionLost) {
            getString(R.string.connection_lost)
        } else {
            getString(R.string.service_running)
        }

        val content = if (activeCycle != null) {
            getString(R.string.monitoring_active_incubation, activeCycle?.name)
        } else {
            getString(R.string.no_active_incubation)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(connectionLost: Boolean = false) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(connectionLost))
    }

    override fun onDestroy() {
        Log.d(TAG, "Service onDestroy called")

        try {
            // Tüm coroutine'leri iptal et
            serviceScope.cancel()

            // Repository bağlantısını kapat
            try {
                // Servis LifecycleService'ten türetildiği için, lifecycleScope kullanılabilir
                lifecycleScope.launch {
                    try {
                        withTimeout(3000) { // 3 saniye zaman aşımı
                            repository.disconnect()
                        }
                    } catch (e: TimeoutCancellationException) {
                        Log.w(TAG, "Repository disconnect timed out")
                    } catch (e: Exception) {
                        Log.e(TAG, "Repository disconnect error: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Service cleanup error: ${e.message}")
            }

            // Bildirimleri temizle
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancelAll()
        } catch (e: Exception) {
            Log.e(TAG, "Service cleanup error: ${e.message}")
        } finally {
            // Her durumda üst sınıf metodunu çağır
            super.onDestroy()
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}