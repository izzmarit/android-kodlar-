package com.kulucka.mk_v5;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import com.evernote.android.job.JobManager;
import com.kulucka.mk_v5.services.ApiService;
import com.kulucka.mk_v5.utils.Constants;
import com.kulucka.mk_v5.utils.SharedPrefsManager;

public class KuluckaApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // SharedPreferences yöneticisini başlat
        SharedPrefsManager.init(this);

        // API servisini başlat
        ApiService.init();

        // Bildirim kanallarını oluştur
        createNotificationChannels();

        // JobManager'ı başlat
        JobManager.create(this).addJobCreator(new KuluckaJobCreator());
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Alarm bildirimleri kanalı
            NotificationChannel alarmChannel = new NotificationChannel(
                    Constants.ALARM_NOTIFICATION_CHANNEL_ID,
                    "Kuluçka Alarmları",
                    NotificationManager.IMPORTANCE_HIGH
            );
            alarmChannel.setDescription("Kuluçka makinesi alarm bildirimleri");

            // Bağlantı bildirimleri kanalı
            NotificationChannel connectionChannel = new NotificationChannel(
                    Constants.CONNECTION_NOTIFICATION_CHANNEL_ID,
                    "Bağlantı Durumu",
                    NotificationManager.IMPORTANCE_LOW
            );
            connectionChannel.setDescription("Cihaz bağlantı durumu bildirimleri");

            // Kanalları sisteme kaydet
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(alarmChannel);
                manager.createNotificationChannel(connectionChannel);
            }
        }
    }
}