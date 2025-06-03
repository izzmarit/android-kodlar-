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
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.kulucka.mk_v5.R;
import com.kulucka.mk_v5.activities.MainActivity;

import org.json.JSONObject;

public class DataRefreshService extends Service {

    private static final String TAG = "DataRefreshService";
    private static final String CHANNEL_ID = "KuluckaDataRefresh";
    private static final int NOTIFICATION_ID = 1;
    private static final long REFRESH_INTERVAL = 30000; // 30 seconds

    private Handler refreshHandler;
    private Runnable refreshRunnable;
    private boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "DataRefreshService created");

        createNotificationChannel();
        setupDataRefresh();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "DataRefreshService started");

        startForeground(NOTIFICATION_ID, createNotification());
        startDataRefresh();

        return START_STICKY; // Service will be restarted if killed
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "DataRefreshService destroyed");

        stopDataRefresh();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // This is not a bound service
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Kulucka Veri Yenileme",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("ESP32 ile sürekli veri alışverişi");

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

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Kulucka Makinesi")
                .setContentText("ESP32 ile bağlantı aktif")
                .setSmallIcon(R.drawable.ic_notification) // You may need to add this icon
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
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
        // Get sensor data from ESP32
        ApiService.getInstance().getSensorData(new ApiService.DataCallback() {
            @Override
            public void onSuccess(JSONObject data) {
                // Process the data
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
            // Check for alarms and critical conditions
            boolean tempAlarm = data.optBoolean("tempAlarm", false);
            boolean humidityAlarm = data.optBoolean("humidityAlarm", false);
            double temperature = data.optDouble("temperature", 0.0);
            double humidity = data.optDouble("humidity", 0.0);

            // Update notification with current data
            updateNotification(temperature, humidity, tempAlarm || humidityAlarm);

            // Send alarms if needed
            if (tempAlarm || humidityAlarm) {
                sendAlarmNotification(tempAlarm, humidityAlarm);
            }

            Log.d(TAG, "Data processed - Temp: " + temperature + "°C, Humidity: " + humidity + "%");

        } catch (Exception e) {
            Log.e(TAG, "Error processing data: " + e.getMessage());
        }
    }

    private void updateNotification(double temperature, double humidity, boolean hasAlarm) {
        String contentText = String.format("Sıcaklık: %.1f°C, Nem: %.1f%%", temperature, humidity);
        if (hasAlarm) {
            contentText += " - ALARM!";
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Kulucka Makinesi")
                .setContentText(contentText)
                .setSmallIcon(hasAlarm ? R.drawable.ic_alarm : R.drawable.ic_notification)
                .setOngoing(true)
                .build();

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }

    private void sendAlarmNotification(boolean tempAlarm, boolean humidityAlarm) {
        String alarmMessage = "";
        if (tempAlarm && humidityAlarm) {
            alarmMessage = "Sıcaklık ve Nem Alarmı!";
        } else if (tempAlarm) {
            alarmMessage = "Sıcaklık Alarmı!";
        } else if (humidityAlarm) {
            alarmMessage = "Nem Alarmı!";
        }

        Intent alarmIntent = new Intent(this, MainActivity.class);
        alarmIntent.putExtra("alarm", true);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 1, alarmIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        Notification alarmNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Kulucka Alarmı")
                .setContentText(alarmMessage)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build();

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(2, alarmNotification); // Different ID for alarm
        }
    }
}