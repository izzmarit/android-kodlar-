package com.kulucka.mkv5.services;

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

import com.kulucka.mkv5.MainActivity;
import com.kulucka.mkv5.R;
import com.kulucka.mkv5.models.DeviceStatus;
import com.kulucka.mkv5.network.NetworkManager;
import com.kulucka.mkv5.utils.Constants;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BackgroundService extends Service {
    private static final String TAG = "BackgroundService";

    private NetworkManager networkManager;
    private Handler handler;
    private Runnable checkRunnable;
    private boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        networkManager = NetworkManager.getInstance(this);
        handler = new Handler();
        createNotificationChannel();
        startForeground(Constants.NOTIFICATION_ID, createNotification("Kuluçka sistemi izleniyor..."));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isRunning) {
            isRunning = true;
            startPeriodicCheck();
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
        isRunning = false;
        if (handler != null && checkRunnable != null) {
            handler.removeCallbacks(checkRunnable);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_ID,
                    "Kuluçka Servisi",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Kuluçka sistemi arka plan servisi");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification(String content) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
                .setContentTitle("KULUCKA MK v5.0")
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(String content) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(Constants.NOTIFICATION_ID, createNotification(content));
        }
    }

    private void startPeriodicCheck() {
        checkRunnable = new Runnable() {
            @Override
            public void run() {
                checkDeviceStatus();
                handler.postDelayed(this, Constants.BACKGROUND_UPDATE_INTERVAL);
            }
        };
        handler.post(checkRunnable);
    }

    private void checkDeviceStatus() {
        if (!networkManager.isNetworkAvailable()) {
            updateNotification("Ağ bağlantısı yok");
            return;
        }

        networkManager.getDeviceStatus(new Callback<DeviceStatus>() {
            @Override
            public void onResponse(Call<DeviceStatus> call, Response<DeviceStatus> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DeviceStatus status = response.body();
                    String notificationText = String.format(
                            "%.1f°C / %d%% | Gün: %d/%d",
                            status.getTemperature(),
                            Math.round(status.getHumidity()),
                            status.getDisplayDay(),
                            status.getTotalDays()
                    );
                    updateNotification(notificationText);

                    // Check for alarms
                    checkAlarms(status);
                } else {
                    updateNotification("Cihazdan veri alınamıyor");
                }
            }

            @Override
            public void onFailure(Call<DeviceStatus> call, Throwable t) {
                Log.e(TAG, "Status check failed: " + t.getMessage());
                updateNotification("Bağlantı hatası");
            }
        });
    }

    private void checkAlarms(DeviceStatus status) {
        if (!status.isAlarmEnabled()) {
            return;
        }

        boolean hasAlarm = false;
        StringBuilder alarmMessage = new StringBuilder();

        // Temperature alarms
        if (status.getTemperature() < status.getTempLowAlarm()) {
            hasAlarm = true;
            alarmMessage.append("Düşük sıcaklık! ");
        } else if (status.getTemperature() > status.getTempHighAlarm()) {
            hasAlarm = true;
            alarmMessage.append("Yüksek sıcaklık! ");
        }

        // Humidity alarms
        if (status.getHumidity() < status.getHumidLowAlarm()) {
            hasAlarm = true;
            alarmMessage.append("Düşük nem! ");
        } else if (status.getHumidity() > status.getHumidHighAlarm()) {
            hasAlarm = true;
            alarmMessage.append("Yüksek nem! ");
        }

        if (hasAlarm) {
            showAlarmNotification(alarmMessage.toString());
        }
    }

    private void showAlarmNotification(String message) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) return;

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
                .setContentTitle("ALARM - KULUCKA MK v5.0")
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_warning)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build();

        manager.notify(Constants.NOTIFICATION_ID + 1, notification);
    }
}