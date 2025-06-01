package com.kulucka.mk_v5.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.NotificationCompat;

import com.kulucka.mk_v5.R;
import com.kulucka.mk_v5.activities.MainActivity;
import com.kulucka.mk_v5.models.IncubationStatus;
import com.kulucka.mk_v5.utils.Constants;

import java.util.concurrent.atomic.AtomicBoolean;

public class DataRefreshService extends Service {
    private Handler handler;
    private AtomicBoolean isServiceRunning = new AtomicBoolean(false);
    private IncubationStatus lastStatus;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground();

        if (isServiceRunning.compareAndSet(false, true)) {
            startStatusUpdates();
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isServiceRunning.set(false);
        handler.removeCallbacksAndMessages(null);
    }

    // Servisi ön planda başlat (bildirim ile)
    private void startForeground() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, Constants.CONNECTION_NOTIFICATION_CHANNEL_ID)
                .setContentTitle("KULUCKA MK Bağlantı Servisi")
                .setContentText("Kuluçka makinesi ile bağlantı aktif.")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(Constants.CONNECTION_NOTIFICATION_ID, notification);
    }

    // Durum güncellemelerini başlat
    private void startStatusUpdates() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (!isServiceRunning.get()) {
                    return;
                }

                ApiService.getInstance().refreshStatus();
                monitorStatus();

                handler.postDelayed(this, Constants.STATUS_UPDATE_INTERVAL);
            }
        });
    }

    // Durum değişikliklerini izle ve bildirim gönder
    private void monitorStatus() {
        IncubationStatus currentStatus = ApiService.getInstance().getStatusLiveData().getValue();

        if (currentStatus != null && lastStatus != null) {
            // Alarm durumlarını kontrol et
            checkAlarmConditions(currentStatus);
        }

        lastStatus = currentStatus;
    }

    // Alarm durumlarını kontrol et
    private void checkAlarmConditions(IncubationStatus status) {
        // Burada ESP32'den gelen alarm durumlarını kontrol edeceğiz
        // Şu an için sadece sıcaklık ve nem hedef değerlerinden önemli sapmaları kontrol ediyoruz

        float tempDiff = Math.abs(status.getTemperature() - status.getTargetTemp());
        float humidityDiff = Math.abs(status.getHumidity() - status.getTargetHumid());

        // Alarm eşik değerlerini SharedPreferences'dan alabilirsiniz
        float tempThreshold = 1.0f; // Varsayılan 1°C
        float humidThreshold = 10.0f; // Varsayılan %10

        if (tempDiff > tempThreshold || humidityDiff > humidThreshold) {
            // Alarm bildirimini gönder
            Intent intent = new Intent(this, NotificationService.class);

            if (tempDiff > tempThreshold) {
                intent.putExtra("alarm_type", "temperature");
                intent.putExtra("message", "Sıcaklık hedef değerden " + tempDiff + "°C sapma gösteriyor!");
            } else {
                intent.putExtra("alarm_type", "humidity");
                intent.putExtra("message", "Nem hedef değerden %" + humidityDiff + " sapma gösteriyor!");
            }

            startService(intent);
        }
    }
}