package com.kulucka.mk_v5.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.kulucka.mk_v5.R;
import com.kulucka.mk_v5.activities.MainActivity;
import com.kulucka.mk_v5.utils.SharedPrefsManager;

import org.json.JSONObject;

public class DataRefreshService extends Service {

    private static final String TAG = "DataRefreshService";
    private static final String CHANNEL_ID = "KuluckaDataRefresh";
    private static final int NOTIFICATION_ID = 1;
    private static final long REFRESH_INTERVAL = 5000; // 5 saniye

    private Handler refreshHandler;
    private Runnable refreshRunnable;
    private boolean isRunning = false;
    private PowerManager.WakeLock wakeLock;

    // ESP32'den gelen güncel veriler
    private double currentTemp = 0.0;
    private double currentHumidity = 0.0;
    private boolean heaterState = false;
    private boolean humidifierState = false;
    private boolean motorState = false;
    private boolean tempAlarm = false;
    private boolean humidityAlarm = false;
    private String incubationType = "";
    private int currentDay = 0;
    private int totalDays = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "DataRefreshService created");

        createNotificationChannel();
        setupDataRefresh();
        acquireWakeLock();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "DataRefreshService started");

        startForeground(NOTIFICATION_ID, createNotification());
        startDataRefresh();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "DataRefreshService destroyed");

        stopDataRefresh();
        releaseWakeLock();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Kuluçka Veri Yenileme",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("ESP32 ile sürekli veri alışverişi");
            channel.setShowBadge(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        String contentText = String.format("Sıcaklık: %.1f°C, Nem: %.1f%%", currentTemp, currentHumidity);
        if (tempAlarm || humidityAlarm) {
            contentText += " - ALARM!";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Kuluçka Makinesi")
                .setContentText(contentText)
                .setSmallIcon(tempAlarm || humidityAlarm ? R.drawable.ic_alarm : R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        // Durum bilgilerini ekle
        if (!incubationType.isEmpty() && currentDay > 0) {
            builder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(contentText + "\n" + incubationType + " - Gün: " + currentDay + "/" + totalDays));
        }

        return builder.build();
    }

    private void updateNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification());
        }
    }

    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "KuluckaMK:DataRefreshWakeLock"
            );
            wakeLock.acquire();
            Log.d(TAG, "WakeLock acquired");
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            Log.d(TAG, "WakeLock released");
        }
    }

    private void setupDataRefresh() {
        refreshHandler = new Handler();
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    refreshData();
                    refreshHandler.postDelayed(this, REFRESH_INTERVAL);
                }
            }
        };
    }

    private void startDataRefresh() {
        if (!isRunning) {
            isRunning = true;
            refreshHandler.post(refreshRunnable);
            Log.d(TAG, "Data refresh started");
        }
    }

    private void stopDataRefresh() {
        isRunning = false;
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
        Log.d(TAG, "Data refresh stopped");
    }

    private void refreshData() {
        // ESP32'nin createAppData fonksiyonundan güncel verileri al
        ApiService.getInstance().getAppData(new ApiService.DataCallback() {
            @Override
            public void onSuccess(JSONObject data) {
                processDataUpdate(data);
            }

            @Override
            public void onError(String message) {
                Log.w(TAG, "Data refresh failed: " + message);
            }
        });
    }

    private void processDataUpdate(JSONObject data) {
        try {
            // ESP32'nin createAppData fonksiyonundan gelen veriler
            currentTemp = data.optDouble("temperature", 0.0);
            currentHumidity = data.optDouble("humidity", 0.0);

            // Cihaz durumları
            heaterState = data.optBoolean("heaterState", false);
            humidifierState = data.optBoolean("humidifierState", false);
            motorState = data.optBoolean("motorState", false);

            // Kuluçka bilgileri
            currentDay = data.optInt("currentDay", 0);
            totalDays = data.optInt("totalDays", 0);
            incubationType = data.optString("incubationType", "");

            // Hedef değerler - ESP32'de targetHumid kullanılıyor!
            double targetTemp = data.optDouble("targetTemp", 0.0);
            double targetHumid = data.optDouble("targetHumid", 0.0);

            // Alarm kontrolü - ESP32'nin yeni alarm sistemine uygun
            boolean alarmEnabled = data.optBoolean("alarmEnabled", true);
            if (alarmEnabled) {
                double tempLowAlarm = data.optDouble("tempLowAlarm", 1.0);
                double tempHighAlarm = data.optDouble("tempHighAlarm", 1.0);
                double humidLowAlarm = data.optDouble("humidLowAlarm", 10.0);
                double humidHighAlarm = data.optDouble("humidHighAlarm", 10.0);

                // Alarm kontrolü - hedef değerlerden sapma bazında
                tempAlarm = (currentTemp < targetTemp - tempLowAlarm) ||
                        (currentTemp > targetTemp + tempHighAlarm);
                humidityAlarm = (currentHumidity < targetHumid - humidLowAlarm) ||
                        (currentHumidity > targetHumid + humidHighAlarm);
            } else {
                tempAlarm = false;
                humidityAlarm = false;
            }

            // Bildirimi güncelle
            updateNotification();

            // Alarm durumunda bildirim gönder
            if (tempAlarm || humidityAlarm) {
                sendAlarmNotification(tempAlarm, humidityAlarm);
            }

            // Verileri broadcast et (MainActivity dinliyor olabilir)
            broadcastDataUpdate(data);

            Log.d(TAG, String.format("Data updated - Temp: %.1f°C, Humidity: %.1f%%, Alarms: %s/%s",
                    currentTemp, currentHumidity, tempAlarm ? "T" : "-", humidityAlarm ? "H" : "-"));

        } catch (Exception e) {
            Log.e(TAG, "Error processing data: " + e.getMessage());
        }
    }

    private void sendAlarmNotification(boolean tempAlarm, boolean humidityAlarm) {
        // Alarm bildirimi için ayrı bir kanal
        String ALARM_CHANNEL_ID = "KuluckaAlarms";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel alarmChannel = new NotificationChannel(
                    ALARM_CHANNEL_ID,
                    "Kuluçka Alarmları",
                    NotificationManager.IMPORTANCE_HIGH
            );
            alarmChannel.setDescription("Sıcaklık ve nem alarm bildirimleri");
            alarmChannel.enableVibration(true);
            alarmChannel.setVibrationPattern(new long[]{0, 500, 200, 500});

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(alarmChannel);
            }
        }

        String alarmMessage = "";
        if (tempAlarm && humidityAlarm) {
            alarmMessage = "Sıcaklık ve Nem Alarmı!";
        } else if (tempAlarm) {
            alarmMessage = "Sıcaklık Alarmı! Mevcut: " + String.format("%.1f°C", currentTemp);
        } else if (humidityAlarm) {
            alarmMessage = "Nem Alarmı! Mevcut: " + String.format("%.1f%%", currentHumidity);
        }

        Intent alarmIntent = new Intent(this, MainActivity.class);
        alarmIntent.putExtra("alarm", true);
        alarmIntent.putExtra("tempAlarm", tempAlarm);
        alarmIntent.putExtra("humidityAlarm", humidityAlarm);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 2, alarmIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT :
                        PendingIntent.FLAG_UPDATE_CURRENT
        );

        Notification alarmNotification = new NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
                .setContentTitle("Kuluçka Alarmı")
                .setContentText(alarmMessage)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 500, 200, 500})
                .build();

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(2, alarmNotification);
        }
    }

    private void broadcastDataUpdate(JSONObject data) {
        Intent intent = new Intent("com.kulucka.mk_v5.DATA_UPDATE");
        intent.putExtra("data", data.toString());
        sendBroadcast(intent);
    }
}