package com.kulucka.kontrol.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.app.ActivityManager;


import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.kulucka.kontrol.R;
import com.kulucka.kontrol.activities.MainActivity;
import com.kulucka.kontrol.models.AlarmData;
import com.kulucka.kontrol.models.Profile;
import com.kulucka.kontrol.models.SensorData;
import com.kulucka.kontrol.models.AppSettings;
import com.kulucka.kontrol.utils.JSONParser;
import com.kulucka.kontrol.utils.SocketManager;
import com.kulucka.kontrol.utils.WiFiManager;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.io.Serializable;
import java.util.ArrayList;

public class ConnectionService extends Service {
    private static final String TAG = "ConnectionService";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "KuluckaKontrolChannel";

    // Broadcast Action tanımları
    public static final String ACTION_CONNECTION_STATUS = "com.kulucka.kontrol.CONNECTION_STATUS";
    public static final String ACTION_SENSOR_DATA_UPDATED = "com.kulucka.kontrol.SENSOR_DATA_UPDATED";
    public static final String ACTION_SETTINGS_UPDATED = "com.kulucka.kontrol.SETTINGS_UPDATED";
    public static final String ACTION_PROFILE_UPDATED = "com.kulucka.kontrol.PROFILE_UPDATED";
    public static final String ACTION_ALARM_UPDATED = "com.kulucka.kontrol.ALARM_UPDATED";
    public static final String ACTION_ALL_PROFILES_UPDATED = "com.kulucka.kontrol.ALL_PROFILES_UPDATED";

    // Extra key tanımları
    public static final String EXTRA_CONNECTED = "connected";
    public static final String EXTRA_SENSOR_DATA = "sensor_data";
    public static final String EXTRA_SETTINGS = "settings";
    public static final String EXTRA_PROFILE = "profile";
    public static final String EXTRA_ALARM_DATA = "alarm_data";
    public static final String EXTRA_ALL_PROFILES = "all_profiles";

    // Yöneticiler
    private WiFiManager wifiManager;
    private SocketManager socketManager;

    // Durum ve veriler
    private boolean isConnected = false;
    private SensorData lastSensorData;
    private AppSettings lastSettings;
    private Profile lastProfile;
    private AlarmData lastAlarmData;
    private List<Profile> allProfiles;

    // Zamanlayıcılar
    private Timer dataRequestTimer;
    private Timer reconnectTimer;
    private Timer pingTimer;
    private Handler mainHandler;

    // Binder
    private final IBinder binder = new LocalBinder();

    // Bildirim yönetimi
    private NotificationManager notificationManager;

    public class LocalBinder extends Binder {
        public ConnectionService getService() {
            return ConnectionService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Yöneticileri oluştur
        wifiManager = new WiFiManager(this);
        socketManager = new SocketManager();
        mainHandler = new Handler(Looper.getMainLooper());

        // Bildirim yöneticisini al
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Bildirim kanalını oluştur
        createNotificationChannel();

        // Dinleyicileri ayarla
        setupListeners();

        // Servis başlatıldı bilgisi
        Log.d(TAG, "Bağlantı servisi oluşturuldu");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Kuluçka Kontrol",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Kuluçka makinesi izleme bildirimleri");
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Bağlantı servisi başlatıldı");

        // Ön plan servisi olarak başlat - Boş bildirim metni
        startForeground(NOTIFICATION_ID, createNotification("Kuluçka Kontrol", ""));

        // WiFi ve Soket yöneticilerini kur
        setupManagers();

        // Otomatik bağlanmayı başlat
        connectToESP();

        return START_STICKY;
    }

    private Notification createNotification(String title, String text) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void updateNotification(String title, String text) {
        notificationManager.notify(NOTIFICATION_ID, createNotification(title, text));
    }

    private void setupManagers() {
        // WiFi listener ayarları
        wifiManager.setOnWiFiConnectedListener(new WiFiManager.OnWiFiConnectedListener() {
            @Override
            public void onWiFiConnected() {
                Log.d(TAG, "WiFi bağlandı, Soket bağlantısı başlatılıyor");
                socketManager.connect();
            }

            @Override
            public void onWiFiDisconnected() {
                Log.d(TAG, "WiFi bağlantısı kesildi");
                isConnected = false;
                socketManager.disconnect();
                updateConnectionStatus(false);

                // Yeniden bağlantı denemesi başlat
                startReconnectTimer();
            }

            @Override
            public void onWiFiConnectFailed() {
                Log.e(TAG, "WiFi bağlantısı başarısız oldu");
                isConnected = false;
                updateConnectionStatus(false);

                // Yeniden bağlantı denemesi başlat
                startReconnectTimer();
            }
        });
    }

    private void setupListeners() {
        // Socket dinleyicileri
        socketManager.setOnConnectionStatusListener(new SocketManager.OnConnectionStatusListener() {
            @Override
            public void onConnected() {
                Log.d(TAG, "Soket bağlantısı kuruldu");
                isConnected = true;
                updateConnectionStatus(true);
                updateNotification("Kuluçka Kontrol Bağlandı", "Veri alınıyor...");

                // Veri isteklerini başlat
                startDataRequestTimer();
                startPingTimer();

                // Hemen veri iste
                requestAllData();
            }

            @Override
            public void onDisconnected() {
                Log.d(TAG, "Soket bağlantısı kesildi");
                isConnected = false;
                updateConnectionStatus(false);
                updateNotification("Kuluçka Kontrol Çalışıyor", "Bağlantı kesildi, yeniden bağlanılıyor...");

                // Zamanlayıcıları durdur
                stopDataRequestTimer();
                stopPingTimer();

                // Yeniden bağlantı denemesi başlat
                startReconnectTimer();
            }

            @Override
            public void onConnectionFailed(String reason) {
                Log.e(TAG, "Soket bağlantısı başarısız oldu: " + reason);
                isConnected = false;
                updateConnectionStatus(false);

                // Bildirimi güncelleyen satır - BUNU DEĞİŞTİRİN
                // updateNotification("Kuluçka Kontrol Çalışıyor", "Bağlantı kurulamadı, aygıt aranıyor...");

                // Yeni kod - sessiz bildirim güncellemesi
                if (isAppInForeground()) {
                    updateNotification("Kuluçka Kontrol Aktif", "");
                }

                // Zamanlayıcıları durdur
                stopDataRequestTimer();
                stopPingTimer();

                // Yeniden bağlantı denemesi başlat
                startReconnectTimer();
            }
        });

        socketManager.setOnMessageReceivedListener(new SocketManager.OnMessageReceivedListener() {
            @Override
            public void onMessageReceived(String message) {
                processMessage(message);
            }
        });
    }

    private boolean isAppInForeground() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) return false;

        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) return false;

        final String packageName = getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    && appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    private void processMessage(String message) {
        try {
            if (message.contains("\"type\":\"sensor_data\"")) {
                SensorData sensorData = JSONParser.parseSensorData(message);
                if (sensorData != null) {
                    lastSensorData = sensorData;
                    broadcastSensorData(sensorData);

                    // Alarm kontrolü
                    checkAlarmStatus();
                }
            } else if (message.contains("\"type\":\"settings\"")) {
                AppSettings settings = JSONParser.parseSettings(message);
                if (settings != null) {
                    lastSettings = settings;
                    broadcastSettings(settings);
                }
            } else if (message.contains("\"type\":\"profile\"")) {
                Profile profile = JSONParser.parseProfile(message);
                if (profile != null) {
                    lastProfile = profile;
                    broadcastProfile(profile);
                }
            } else if (message.contains("\"type\":\"alarm_data\"")) {
                AlarmData alarmData = JSONParser.parseAlarmData(message);
                if (alarmData != null) {
                    lastAlarmData = alarmData;
                    broadcastAlarmData(alarmData);

                    // Aktif alarm varsa bildirim göster
                    if (alarmData.isAlarmActive()) {
                        showAlarmNotification(alarmData);
                    }
                }
            } else if (message.contains("\"type\":\"all_profiles\"")) {
                List<Profile> profiles = JSONParser.parseAllProfiles(message);
                if (profiles != null) {
                    allProfiles = profiles;
                    broadcastAllProfiles(profiles);
                }
            } else if (message.contains("\"cmd\":\"ping\"")) {
                // Ping yanıtı, bağlantı durumu güncelleme
                isConnected = true;
                updateConnectionStatus(true);
            }
        } catch (Exception e) {
            Log.e(TAG, "Mesaj işleme hatası: " + e.getMessage());
        }
    }

    private void checkAlarmStatus() {
        if (lastAlarmData != null && lastAlarmData.isAlarmActive()) {
            showAlarmNotification(lastAlarmData);
        }
    }

    private void showAlarmNotification(AlarmData alarmData) {
        // Alarm bildirimi oluştur
        String title = "ALARM: " + alarmData.getAlarmTypeString();
        String message = alarmData.getAlarmMessage();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .build();

        notificationManager.notify(2000 + alarmData.getAlarmType(), notification);
    }

    private void startDataRequestTimer() {
        stopDataRequestTimer();

        dataRequestTimer = new Timer();
        dataRequestTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isConnected) {
                    requestAllData();
                }
            }
        }, 0, 5000); // 5 saniyede bir veri iste
    }

    private void stopDataRequestTimer() {
        if (dataRequestTimer != null) {
            dataRequestTimer.cancel();
            dataRequestTimer = null;
        }
    }

    private void startPingTimer() {
        stopPingTimer();

        pingTimer = new Timer();
        pingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isConnected) {
                    socketManager.sendPing();
                }
            }
        }, 30000, 30000); // 30 saniyede bir ping gönder
    }

    private void stopPingTimer() {
        if (pingTimer != null) {
            pingTimer.cancel();
            pingTimer = null;
        }
    }

    private void startReconnectTimer() {
        stopReconnectTimer();

        reconnectTimer = new Timer();
        reconnectTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!isConnected) {
                    connectToESP();
                } else {
                    stopReconnectTimer();
                }
            }
        }, 5000, 10000); // 5 saniye sonra başla, 10 saniyede bir dene
    }

    private void stopReconnectTimer() {
        if (reconnectTimer != null) {
            reconnectTimer.cancel();
            reconnectTimer = null;
        }
    }

    private void requestAllData() {
        socketManager.requestSensorData();
        socketManager.requestSettings();
        socketManager.requestProfileData();
        socketManager.requestAlarmData();
    }

    public void connectToESP() {
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.enableWifi();
        }

        if (!wifiManager.isConnectedToESP()) {
            wifiManager.connectToESP();
        } else if (!isConnected) {
            socketManager.connect();
        }
    }

    public void disconnect() {
        socketManager.disconnect();
        isConnected = false;
        updateConnectionStatus(false);
    }

    private void updateConnectionStatus(boolean connected) {
        Intent intent = new Intent(ACTION_CONNECTION_STATUS);
        intent.putExtra(EXTRA_CONNECTED, connected);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastSensorData(SensorData sensorData) {
        Intent intent = new Intent(ACTION_SENSOR_DATA_UPDATED);
        intent.putExtra(EXTRA_SENSOR_DATA, sensorData);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastSettings(AppSettings settings) {
        Intent intent = new Intent(ACTION_SETTINGS_UPDATED);
        intent.putExtra(EXTRA_SETTINGS, settings);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastProfile(Profile profile) {
        Intent intent = new Intent(ACTION_PROFILE_UPDATED);
        intent.putExtra(EXTRA_PROFILE, profile);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastAlarmData(AlarmData alarmData) {
        Intent intent = new Intent(ACTION_ALARM_UPDATED);
        intent.putExtra(EXTRA_ALARM_DATA, alarmData);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastAllProfiles(List<Profile> profiles) {
        Intent intent = new Intent(ACTION_ALL_PROFILES_UPDATED);
        intent.putExtra(EXTRA_ALL_PROFILES, (Serializable) new ArrayList<>(profiles));
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // Servis bağlantı metotları
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        // Tüm zamanlayıcıları durdur
        stopDataRequestTimer();
        stopReconnectTimer();
        stopPingTimer();

        // Bağlantıları kapat
        socketManager.disconnect();

        super.onDestroy();
    }

    // API metotları
    public boolean isConnected() {
        return isConnected;
    }

    public SensorData getLastSensorData() {
        return lastSensorData;
    }

    public AppSettings getLastSettings() {
        return lastSettings;
    }

    public Profile getLastProfile() {
        return lastProfile;
    }

    public AlarmData getLastAlarmData() {
        return lastAlarmData;
    }

    public List<Profile> getAllProfiles() {
        return allProfiles;
    }

    // Komut metotları
    public void setTargets(float temperature, float humidity) {
        if (isConnected) {
            socketManager.setTargets(temperature, humidity);
        }
    }

    public void controlMotor(boolean state) {
        if (isConnected) {
            socketManager.controlMotor(state);
        }
    }

    public void startIncubation(int profileType) {
        if (isConnected) {
            socketManager.startIncubation(profileType);
        }
    }

    public void stopIncubation() {
        if (isConnected) {
            socketManager.stopIncubation();
        }
    }

    public void requestAllProfiles() {
        if (isConnected) {
            socketManager.requestAllProfiles();
        }
    }
}