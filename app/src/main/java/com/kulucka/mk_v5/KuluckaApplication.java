package com.kulucka.mk_v5;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.kulucka.mk_v5.services.ApiService;
import com.kulucka.mk_v5.services.ESP32ConnectionService;
import com.kulucka.mk_v5.utils.SharedPrefsManager;

public class KuluckaApplication extends Application {

    private static final String TAG = "KuluckaApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "Kulucka Application starting...");

        // SharedPrefsManager'ı initialize et
        SharedPrefsManager.initialize(this);
        Log.d(TAG, "SharedPrefsManager initialized");

        // Initialize ApiService with application context
        try {
            ApiService.getInstance().initialize(this);
            Log.d(TAG, "ApiService initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize ApiService: " + e.getMessage());
        }

        // Notification kanalları oluştur
        createNotificationChannels();

        // ESP32 bağlantı servisini başlat
        startESP32ConnectionService();

        Log.d(TAG, "Kulucka Application started successfully");
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);

            // ESP32 Bağlantı Kanalı
            NotificationChannel connectionChannel = new NotificationChannel(
                    "ESP32Connection",
                    "ESP32 Bağlantı Servisi",
                    NotificationManager.IMPORTANCE_LOW
            );
            connectionChannel.setDescription("ESP32 ile sürekli bağlantı kontrolü");
            connectionChannel.setShowBadge(false);
            notificationManager.createNotificationChannel(connectionChannel);

            // Veri Yenileme Kanalı
            NotificationChannel dataChannel = new NotificationChannel(
                    "KuluckaDataRefresh",
                    "Kuluçka Veri Yenileme",
                    NotificationManager.IMPORTANCE_LOW
            );
            dataChannel.setDescription("ESP32 ile sürekli veri alışverişi");
            dataChannel.setShowBadge(false);
            notificationManager.createNotificationChannel(dataChannel);

            // Alarm Kanalı
            NotificationChannel alarmChannel = new NotificationChannel(
                    "KuluckaAlarms",
                    "Kuluçka Alarmları",
                    NotificationManager.IMPORTANCE_HIGH
            );
            alarmChannel.setDescription("Sıcaklık ve nem alarm bildirimleri");
            alarmChannel.enableVibration(true);
            alarmChannel.setVibrationPattern(new long[]{0, 500, 200, 500});
            notificationManager.createNotificationChannel(alarmChannel);

            Log.d(TAG, "Notification channels created");
        }
    }

    private void startESP32ConnectionService() {
        Intent serviceIntent = new Intent(this, ESP32ConnectionService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        Log.d(TAG, "ESP32 connection service started");
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        Log.d(TAG, "Kulucka Application terminating...");

        // Cancel any pending network requests
        try {
            ApiService.getInstance().cancelAllRequests();
            Log.d(TAG, "ApiService cleanup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error during ApiService cleanup: " + e.getMessage());
        }
    }
}