package com.kulucka.mk.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.kulucka.mk.R;
import com.kulucka.mk.activities.MainActivity;
import com.kulucka.mk.models.SystemStatus;
import com.kulucka.mk.utils.Constants;
import com.kulucka.mk.utils.NetworkUtils;
import com.kulucka.mk.utils.SharedPreferencesManager;

/**
 * ESP32 ile sürekli iletişim kurarak veri güncellemesi yapan arka plan servisi
 * Foreground service olarak çalışır ve bildirimler gönderir
 */
public class BackgroundService extends Service {

    private static final String TAG = "BackgroundService";

    private ApiService apiService;
    private SharedPreferencesManager prefsManager;
    private Handler mainHandler;
    private Runnable dataUpdateRunnable;
    private NotificationManager notificationManager;

    private boolean isServiceRunning = false;
    private SystemStatus lastSystemStatus;
    private long lastSuccessfulUpdate = 0;
    private int connectionFailureCount = 0;
    private String currentIpAddress;

    // Service control
    private static final String ACTION_START_SERVICE = "com.kulucka.mk.START_SERVICE";
    private static final String ACTION_STOP_SERVICE = "com.kulucka.mk.STOP_SERVICE";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "BackgroundService oluşturuldu");

        // Initialize components
        apiService = ApiService.getInstance(this);
        prefsManager = SharedPreferencesManager.getInstance(this);
        mainHandler = new Handler(Looper.getMainLooper());
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create notification channel
        createNotificationChannel();

        // Load last known IP
        currentIpAddress = prefsManager.getESP32IP();
        apiService.updateIpAddress(currentIpAddress);

        // Load last system status
        lastSystemStatus = prefsManager.getLastSystemStatus();

        setupDataUpdateRunnable();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;

        if (ACTION_STOP_SERVICE.equals(action)) {
            stopForegroundService();
            return START_NOT_STICKY;
        }

        if (!isServiceRunning) {
            startForegroundService();
        }

        return START_STICKY; // Sistem tarafından öldürülürse yeniden başlatılsın
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Bound service değil
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "BackgroundService yok ediliyor");
        stopDataUpdates();
        super.onDestroy();
    }

    /**
     * Foreground service olarak başlatır
     */
    private void startForegroundService() {
        if (isServiceRunning) return;

        Log.i(TAG, "Foreground service başlatılıyor");

        // Create ongoing notification
        Notification notification = createStatusNotification();
        startForeground(Constants.SERVICE_NOTIFICATION_ID, notification);

        isServiceRunning = true;
        startDataUpdates();

        // Increment app statistics
        prefsManager.incrementAppLaunchCount();

        Log.i(TAG, "Foreground service başlatıldı");
    }

    /**
     * Foreground service'i durdurur
     */
    private void stopForegroundService() {
        if (!isServiceRunning) return;

        Log.i(TAG, "Foreground service durduruluyor");

        stopDataUpdates();
        stopForeground(true);
        isServiceRunning = false;

        stopSelf();

        Log.i(TAG, "Foreground service durduruldu");
    }

    /**
     * Veri güncelleme döngüsünü başlatır
     */
    private void startDataUpdates() {
        if (dataUpdateRunnable == null) return;

        Log.i(TAG, "Veri güncelleme döngüsü başlatıldı");

        // İlk güncellemeyi hemen yap
        mainHandler.post(dataUpdateRunnable);
    }

    /**
     * Veri güncelleme döngüsünü durdurur
     */
    private void stopDataUpdates() {
        if (dataUpdateRunnable != null) {
            mainHandler.removeCallbacks(dataUpdateRunnable);
            Log.i(TAG, "Veri güncelleme döngüsü durduruldu");
        }
    }

    /**
     * Veri güncelleme Runnable'ını hazırlar
     */
    private void setupDataUpdateRunnable() {
        dataUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isServiceRunning) return;

                // Network kontrolü
                if (!NetworkUtils.isNetworkAvailable(BackgroundService.this)) {
                    Log.w(TAG, "Ağ bağlantısı yok, veri güncelleme atlanıyor");
                    scheduleNextUpdate();
                    return;
                }

                // ESP32'den veri al
                requestSystemStatus();

                // Bir sonraki güncellemeyi planla
                scheduleNextUpdate();
            }
        };
    }

    /**
     * ESP32'den sistem durumunu ister
     */
    private void requestSystemStatus() {
        apiService.getSystemStatus(new ApiService.ApiCallback<SystemStatus>() {
            @Override
            public void onSuccess(SystemStatus systemStatus) {
                handleSuccessfulUpdate(systemStatus);
            }

            @Override
            public void onError(String error) {
                handleUpdateError(error);
            }
        });
    }

    /**
     * Başarılı veri güncellemesini işler
     */
    private void handleSuccessfulUpdate(SystemStatus systemStatus) {
        if (systemStatus == null) return;

        lastSystemStatus = systemStatus;
        lastSuccessfulUpdate = System.currentTimeMillis();
        connectionFailureCount = 0;

        // Save to preferences
        prefsManager.saveSystemStatus(systemStatus);
        prefsManager.updateConnectionStats(true);

        // Update notification
        updateStatusNotification(systemStatus);

        // Check for alarms
        checkAlarmConditions(systemStatus);

        // Send broadcast to activities
        sendStatusUpdateBroadcast(systemStatus);

        Log.d(TAG, "Sistem durumu güncellendi - Sıcaklık: " + systemStatus.getFormattedTemperature() +
                ", Nem: " + systemStatus.getFormattedHumidity());
    }

    /**
     * Veri güncelleme hatasını işler
     */
    private void handleUpdateError(String error) {
        connectionFailureCount++;
        prefsManager.updateConnectionStats(false);

        Log.w(TAG, "Veri güncelleme hatası (" + connectionFailureCount + ". deneme): " + error);

        // Çok fazla hata varsa IP adresini yeniden tara
        if (connectionFailureCount >= 5) {
            attemptIPRediscovery();
        }

        // Error broadcast gönder
        sendErrorBroadcast(error);

        // Update notification with error
        updateErrorNotification(error);
    }

    /**
     * IP adresini yeniden bulmaya çalışır
     */
    private void attemptIPRediscovery() {
        Log.i(TAG, "IP adresi yeniden taranıyor...");

        // Önce varsayılan IP'leri kontrol et
        NetworkUtils.checkDefaultESP32IPs(new NetworkUtils.DefaultIPCallback() {
            @Override
            public void onResult(String ipAddress) {
                if (ipAddress != null && !ipAddress.equals(currentIpAddress)) {
                    Log.i(TAG, "Yeni IP adresi bulundu: " + ipAddress);

                    currentIpAddress = ipAddress;
                    apiService.updateIpAddress(ipAddress);
                    prefsManager.setESP32IP(ipAddress);

                    connectionFailureCount = 0;
                } else {
                    // Tam tarama yap
                    performFullIPScan();
                }
            }
        });
    }

    /**
     * Tam IP taraması yapar
     */
    private void performFullIPScan() {
        NetworkUtils.scanForESP32(this, foundIPs -> {
            if (!foundIPs.isEmpty()) {
                String newIP = foundIPs.get(0);
                Log.i(TAG, "Tam taramada IP bulundu: " + newIP);

                currentIpAddress = newIP;
                apiService.updateIpAddress(newIP);
                prefsManager.setESP32IP(newIP);

                connectionFailureCount = 0;
            } else {
                Log.w(TAG, "Hiçbir ESP32 cihazı bulunamadı");
            }
        });
    }

    /**
     * Alarm koşullarını kontrol eder
     */
    private void checkAlarmConditions(SystemStatus systemStatus) {
        if (!prefsManager.isNotificationEnabled()) return;

        if (systemStatus.hasAlarmCondition()) {
            String alarmMessage = systemStatus.getAlarmMessage();

            // Son alarm zamanını kontrol et (spam önleme)
            long lastAlarmTime = prefsManager.getLastAlarmTime();
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastAlarmTime > 60000) { // 1 dakika
                showAlarmNotification(alarmMessage);
                prefsManager.saveLastAlarm(alarmMessage, currentTime);

                // Alarm broadcast gönder
                sendAlarmBroadcast(alarmMessage);
            }
        }
    }

    /**
     * Bir sonraki güncellemeyi planlar
     */
    private void scheduleNextUpdate() {
        long interval = prefsManager.getUpdateInterval();

        // Bağlantı hatası varsa aralığı artır
        if (connectionFailureCount > 0) {
            interval *= Math.min(connectionFailureCount, 5); // Maksimum 5x
        }

        mainHandler.postDelayed(dataUpdateRunnable, interval);
    }

    /**
     * Notification channel oluşturur
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_ID,
                    Constants.NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Kuluçka MK sistem durumu bildirimleri");
            channel.setShowBadge(false);
            channel.setSound(null, null);

            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Durum bildirimi oluşturur
     */
    private Notification createStatusNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_temperature)
                .setContentTitle("KULUCKA MK v5.0")
                .setContentText("Sistem durumu kontrol ediliyor...")
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        // Stop action
        Intent stopIntent = new Intent(this, BackgroundService.class);
        stopIntent.setAction(ACTION_STOP_SERVICE);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        builder.addAction(R.drawable.ic_stop, "Durdur", stopPendingIntent);

        return builder.build();
    }

    /**
     * Durum bildirimini günceller
     */
    private void updateStatusNotification(SystemStatus systemStatus) {
        if (!prefsManager.isNotificationEnabled()) return;

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String contentText = String.format("🌡️ %s | 💧 %s | 📅 %s",
                systemStatus.getFormattedTemperature(),
                systemStatus.getFormattedHumidity(),
                systemStatus.getFormattedDays());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_temperature)
                .setContentTitle("KULUCKA MK v5.0")
                .setContentText(contentText)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        // Sistem durumu detayları
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle("Kuluçka Durumu");
        inboxStyle.addLine("🌡️ Sıcaklık: " + systemStatus.getFormattedTemperature() +
                " (Hedef: " + systemStatus.getFormattedTargetTemp() + ")");
        inboxStyle.addLine("💧 Nem: " + systemStatus.getFormattedHumidity() +
                " (Hedef: " + systemStatus.getFormattedTargetHumid() + ")");
        inboxStyle.addLine("📅 Gün: " + systemStatus.getFormattedDays());
        inboxStyle.addLine("🔥 Isıtıcı: " + (systemStatus.isHeaterState() ? "AÇIK" : "KAPALI"));
        inboxStyle.addLine("💨 Nemlendirici: " + (systemStatus.isHumidifierState() ? "AÇIK" : "KAPALI"));
        inboxStyle.addLine("⚙️ Motor: " + (systemStatus.isMotorState() ? "AÇIK" : "KAPALI"));

        builder.setStyle(inboxStyle);

        // Stop action
        Intent stopIntent = new Intent(this, BackgroundService.class);
        stopIntent.setAction(ACTION_STOP_SERVICE);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        builder.addAction(R.drawable.ic_stop, "Durdur", stopPendingIntent);

        notificationManager.notify(Constants.SERVICE_NOTIFICATION_ID, builder.build());
    }

    /**
     * Hata bildirimi gösterir
     */
    private void updateErrorNotification(String error) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_error)
                .setContentTitle("KULUCKA MK - Bağlantı Hatası")
                .setContentText("ESP32 cihazına bağlanılamıyor")
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(error));

        // Stop action
        Intent stopIntent = new Intent(this, BackgroundService.class);
        stopIntent.setAction(ACTION_STOP_SERVICE);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        builder.addAction(R.drawable.ic_stop, "Durdur", stopPendingIntent);

        notificationManager.notify(Constants.SERVICE_NOTIFICATION_ID, builder.build());
    }

    /**
     * Alarm bildirimi gösterir
     */
    private void showAlarmNotification(String alarmMessage) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle("🚨 KULUCKA ALARMI")
                .setContentText(alarmMessage)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(alarmMessage));

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    /**
     * Durum güncelleme broadcast'i gönderir
     */
    private void sendStatusUpdateBroadcast(SystemStatus systemStatus) {
        Intent intent = new Intent(Constants.ACTION_UPDATE_DATA);
        intent.putExtra(Constants.EXTRA_SYSTEM_STATUS, systemStatus);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Hata broadcast'i gönderir
     */
    private void sendErrorBroadcast(String error) {
        Intent intent = new Intent(Constants.ACTION_CONNECTION_CHANGED);
        intent.putExtra(Constants.EXTRA_CONNECTION_STATUS, false);
        intent.putExtra(Constants.EXTRA_ERROR_MESSAGE, error);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Alarm broadcast'i gönderir
     */
    private void sendAlarmBroadcast(String alarmMessage) {
        Intent intent = new Intent(Constants.ACTION_ALARM_TRIGGERED);
        intent.putExtra(Constants.EXTRA_ERROR_MESSAGE, alarmMessage);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // === Static Helper Methods ===

    /**
     * Servisi başlatır
     */
    public static void startService(Context context) {
        Intent intent = new Intent(context, BackgroundService.class);
        intent.setAction(ACTION_START_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    /**
     * Servisi durdurur
     */
    public static void stopService(Context context) {
        Intent intent = new Intent(context, BackgroundService.class);
        intent.setAction(ACTION_STOP_SERVICE);
        context.startService(intent);
    }
}