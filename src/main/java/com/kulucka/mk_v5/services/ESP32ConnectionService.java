package com.kulucka.mk_v5.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.kulucka.mk_v5.R;
import com.kulucka.mk_v5.activities.MainActivity;
import com.kulucka.mk_v5.utils.NetworkUtils;
import com.kulucka.mk_v5.utils.SharedPrefsManager;

import org.json.JSONObject;

public class ESP32ConnectionService extends Service {

    private static final String TAG = "ESP32ConnectionService";
    private static final String CHANNEL_ID = "ESP32Connection";
    private static final int NOTIFICATION_ID = 100;
    private static final long CHECK_INTERVAL = 30000; // 30 saniye (daha az sıklık)
    private static final String ESP32_AP_SSID = "KULUCKA_MK_v5";
    private static final String ESP32_AP_PASSWORD = "12345678";

    private Handler connectionHandler;
    private Runnable connectionChecker;
    private PowerManager.WakeLock wakeLock;
    private WifiManager wifiManager;
    private ConnectivityManager connectivityManager;
    private boolean isRunning = false;
    private boolean isConnectedToESP32 = false;
    private String currentMode = "Unknown";
    private String currentIP = "192.168.4.1";

    // Bildirim durumu takibi
    private boolean notificationShown = false;
    private long lastSuccessfulConnection = 0;
    private int consecutiveFailures = 0;
    private static final int MAX_CONSECUTIVE_FAILURES = 3;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "ESP32ConnectionService created");

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        createNotificationChannel();
        acquireWakeLock();
        setupConnectionChecker();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "ESP32ConnectionService started");

        // Sadece henüz başlamamışsa notification'ı göster
        if (!notificationShown) {
            startForeground(NOTIFICATION_ID, createNotification());
            notificationShown = true;
        }

        startConnectionMonitoring();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "ESP32ConnectionService destroyed");

        stopConnectionMonitoring();
        releaseWakeLock();
        notificationShown = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "ESP32 Bağlantı Servisi",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("ESP32 ile bağlantı kontrolü");
            channel.setShowBadge(false);
            channel.enableVibration(false);
            channel.setSound(null, null);

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

        String contentText;
        if (isConnectedToESP32) {
            contentText = "ESP32 Bağlı - " + currentMode + " - " + currentIP;
        } else {
            if (consecutiveFailures > MAX_CONSECUTIVE_FAILURES) {
                contentText = "ESP32 bağlantısı bulunamadı";
            } else {
                contentText = "ESP32 bağlantısı kontrol ediliyor...";
            }
        }

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Kuluçka Makinesi")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSilent(true) // Ses çıkarma
                .build();
    }

    private void updateNotification() {
        // Sadece durum değiştiğinde notification'ı güncelle
        if (notificationShown) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify(NOTIFICATION_ID, createNotification());
            }
        }
    }

    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "KuluckaMK:ESP32ConnectionWakeLock"
            );
            wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
            Log.d(TAG, "WakeLock acquired");
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            Log.d(TAG, "WakeLock released");
        }
    }

    private void setupConnectionChecker() {
        connectionHandler = new Handler(Looper.getMainLooper());
        connectionChecker = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    checkAndMaintainConnection();
                    connectionHandler.postDelayed(this, CHECK_INTERVAL);
                }
            }
        };
    }

    private void startConnectionMonitoring() {
        if (!isRunning) {
            isRunning = true;
            connectionHandler.post(connectionChecker);
            Log.d(TAG, "Connection monitoring started");
        }
    }

    private void stopConnectionMonitoring() {
        isRunning = false;
        if (connectionHandler != null && connectionChecker != null) {
            connectionHandler.removeCallbacks(connectionChecker);
        }
        Log.d(TAG, "Connection monitoring stopped");
    }

    private void checkAndMaintainConnection() {
        // Önce mevcut WiFi durumunu kontrol et
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String currentSSID = wifiInfo.getSSID().replace("\"", "");

        // ESP32 AP moduna bağlı mıyız?
        if (ESP32_AP_SSID.equals(currentSSID)) {
            Log.d(TAG, "Connected to ESP32 AP mode");
            currentMode = "AP";
            currentIP = "192.168.4.1";
            testESP32Connection();
        } else {
            // Ev ağındayız, ESP32'yi bulmaya çalış
            SharedPreferences prefs = getSharedPreferences("KuluckaPrefs", MODE_PRIVATE);
            String savedIP = prefs.getString("esp32_ip", "");

            if (!savedIP.isEmpty() && !savedIP.equals("192.168.4.1")) {
                // Station modda kaydedilmiş IP var
                currentMode = "Station";
                currentIP = savedIP;
                testESP32Connection();
            } else {
                // ESP32 bulunamadı
                handleConnectionFailure();
            }
        }
    }

    private void testESP32Connection() {
        com.kulucka.mk_v5.services.ApiService.getInstance().setBaseUrl("http://" + currentIP);
        com.kulucka.mk_v5.services.ApiService.getInstance().testConnection(new com.kulucka.mk_v5.services.ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                handleConnectionSuccess();
            }

            @Override
            public void onError(String message) {
                Log.w(TAG, "ESP32 connection test failed: " + message);
                handleConnectionFailure();
            }
        });
    }

    private void handleConnectionSuccess() {
        boolean wasConnected = isConnectedToESP32;
        isConnectedToESP32 = true;
        lastSuccessfulConnection = System.currentTimeMillis();
        consecutiveFailures = 0;

        if (!wasConnected) {
            Log.d(TAG, "ESP32 connection established - Mode: " + currentMode + ", IP: " + currentIP);
            updateNotification();

            // Veri yenileme servisini başlat
            Intent dataIntent = new Intent(ESP32ConnectionService.this, DataRefreshService.class);
            startService(dataIntent);
        }
    }

    private void handleConnectionFailure() {
        boolean wasConnected = isConnectedToESP32;
        isConnectedToESP32 = false;
        consecutiveFailures++;

        if (wasConnected || consecutiveFailures == MAX_CONSECUTIVE_FAILURES) {
            Log.w(TAG, "ESP32 connection lost or failed after " + consecutiveFailures + " attempts");
            updateNotification();

            // Otomatik bağlantı deneme (sadece çok başarısızlık sonrası)
            if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES &&
                    SharedPrefsManager.getInstance().isAutoConnectEnabled()) {
                tryConnectToESP32AP();
            }
        }
    }

    private void tryConnectToESP32AP() {
        // Çok sık deneme yapma
        long timeSinceLastSuccess = System.currentTimeMillis() - lastSuccessfulConnection;
        if (timeSinceLastSuccess < 60000) { // Son başarılı bağlantıdan 1 dakika geçmemişse deneme
            return;
        }

        Log.d(TAG, "Trying to connect to ESP32 AP: " + ESP32_AP_SSID);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10 ve üzeri için
            WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                    .setSsid(ESP32_AP_SSID)
                    .setWpa2Passphrase(ESP32_AP_PASSWORD)
                    .build();

            NetworkRequest request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .setNetworkSpecifier(specifier)
                    .build();

            ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    super.onAvailable(network);
                    Log.d(TAG, "Connected to ESP32 AP network");
                    connectivityManager.bindProcessToNetwork(network);
                    currentMode = "AP";
                    currentIP = "192.168.4.1";
                    testESP32Connection();
                }

                @Override
                public void onLost(Network network) {
                    super.onLost(network);
                    Log.d(TAG, "Lost connection to ESP32 AP");
                    handleConnectionFailure();
                }
            };

            try {
                connectivityManager.requestNetwork(request, networkCallback, 15000);
            } catch (Exception e) {
                Log.e(TAG, "Error requesting network: " + e.getMessage());
            }
        } else {
            // Android 10 altı için NetworkUtils kullan
            NetworkUtils.getInstance(this).connectToWifi(ESP32_AP_SSID, ESP32_AP_PASSWORD);
        }
    }
}