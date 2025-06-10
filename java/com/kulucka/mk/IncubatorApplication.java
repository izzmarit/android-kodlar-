package com.kulucka.controller;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.kulucka.controller.services.BackgroundService;
import com.kulucka.controller.utils.Constants;
import com.kulucka.controller.utils.SharedPreferencesManager;

/**
 * Ana Application sınıfı
 * Uygulama geneli başlatma işlemlerini yönetir
 */
public class IncubatorApplication extends Application {

    private static IncubatorApplication instance;
    private SharedPreferencesManager preferencesManager;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // SharedPreferences yöneticisini başlat
        preferencesManager = new SharedPreferencesManager(this);

        // Bildirim kanallarını oluştur
        createNotificationChannels();

        // Arka plan servisini başlat (eğer etkinse)
        if (preferencesManager.isBackgroundServiceEnabled()) {
            startBackgroundService();
        }
    }

    /**
     * Application instance'ını al
     * @return Application instance
     */
    public static IncubatorApplication getInstance() {
        return instance;
    }

    /**
     * SharedPreferences yöneticisini al
     * @return SharedPreferencesManager instance
     */
    public SharedPreferencesManager getPreferencesManager() {
        return preferencesManager;
    }

    /**
     * Bildirim kanallarını oluştur
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            // Arka plan servisi bildirimleri için kanal
            NotificationChannel serviceChannel = new NotificationChannel(
                    Constants.SERVICE_CHANNEL_ID,
                    "Kuluçka Sistemi Servisi",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            serviceChannel.setDescription("Kuluçka sistemi arka plan servisi bildirimleri");
            serviceChannel.setShowBadge(false);
            notificationManager.createNotificationChannel(serviceChannel);

            // Alarm bildirimleri için kanal
            NotificationChannel alarmChannel = new NotificationChannel(
                    Constants.ALARM_CHANNEL_ID,
                    "Kuluçka Sistemi Alarmları",
                    NotificationManager.IMPORTANCE_HIGH
            );
            alarmChannel.setDescription("Kuluçka sistemi alarm bildirimleri");
            alarmChannel.enableVibration(true);
            alarmChannel.enableLights(true);
            notificationManager.createNotificationChannel(alarmChannel);

            // Genel bildirimler için kanal
            NotificationChannel generalChannel = new NotificationChannel(
                    Constants.GENERAL_CHANNEL_ID,
                    "Genel Bildirimler",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            generalChannel.setDescription("Kuluçka sistemi genel bildirimleri");
            notificationManager.createNotificationChannel(generalChannel);
        }
    }

    /**
     * Arka plan servisini başlat
     */
    public void startBackgroundService() {
        Intent serviceIntent = new Intent(this, BackgroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    /**
     * Arka plan servisini durdur
     */
    public void stopBackgroundService() {
        Intent serviceIntent = new Intent(this, BackgroundService.class);
        stopService(serviceIntent);
    }
}