package com.kulucka.mkv5;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import androidx.appcompat.widget.Toolbar;
import java.util.Timer;
import java.util.TimerTask;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.kulucka.mkv5.activities.SettingsActivity;
import com.kulucka.mkv5.models.DeviceStatus;
import com.kulucka.mkv5.network.NetworkDiscoveryManager;
import com.kulucka.mkv5.network.NetworkManager;
import com.kulucka.mkv5.services.BackgroundService;
import com.kulucka.mkv5.utils.Constants;
import com.kulucka.mkv5.utils.SharedPreferencesManager;
import com.kulucka.mkv5.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements NetworkDiscoveryManager.DiscoveryCallback {
    private static final String TAG = "MainActivity";
    private static final int SETTINGS_REQUEST_CODE = 1001;

    // UI Components
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvConnectionStatus;
    private ImageView ivConnectionStatus;
    private TextView tvCurrentTemp, tvTargetTemp, tvHeaterStatus;
    private TextView tvCurrentHumid, tvTargetHumid, tvHumidifierStatus;
    private TextView tvIncubationType, tvDayCount, tvIncubationStatus, tvCompletionStatus;
    private TextView tvMotorStatus, tvMotorTiming;
    private TextView tvPidMode, tvPidValues;
    private TextView tvAlarmStatus;
    private TextView tvLastSave, tvPendingChanges, tvAutoSaveStatus;
    private FloatingActionButton fabSettings;
    private TextView tvRTCTime;

    // Variables
    private NetworkManager networkManager;
    private NetworkDiscoveryManager discoveryManager;
    private SharedPreferencesManager prefsManager;
    private Handler updateHandler;
    private Runnable updateRunnable;
    private boolean isUpdating = false;
    private boolean isConnected = false;
    private boolean isDiscovering = false;
    private boolean isSystemVerified = false;
    private WifiStateReceiver wifiStateReceiver;
    private boolean isAutoConnectEnabled = true;
    private Timer autoConnectTimer;
    private static final long AUTO_CONNECT_INTERVAL = 5000;
    private long lastAutoConnectAttempt = 0;
    private int autoConnectAttemptCount = 0;
    private static final int MAX_AUTO_CONNECT_ATTEMPTS = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupListeners();

        registerWifiStateReceiver();

        networkManager = NetworkManager.getInstance(this);
        discoveryManager = new NetworkDiscoveryManager(this);
        prefsManager = SharedPreferencesManager.getInstance(this);
        updateHandler = new Handler();

        startBackgroundService();
        startAutoConnectSystem();
        startPeriodicUpdates();
    }

    private void initializeViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ImageView ivSettings = findViewById(R.id.ivSettings);
        ivSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivityForResult(intent, SETTINGS_REQUEST_CODE);
        });

        swipeRefresh = findViewById(R.id.swipeRefresh);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        ivConnectionStatus = findViewById(R.id.ivConnectionStatus);

        tvCurrentTemp = findViewById(R.id.tvCurrentTemp);
        tvTargetTemp = findViewById(R.id.tvTargetTemp);
        tvHeaterStatus = findViewById(R.id.tvHeaterStatus);

        tvCurrentHumid = findViewById(R.id.tvCurrentHumid);
        tvTargetHumid = findViewById(R.id.tvTargetHumid);
        tvHumidifierStatus = findViewById(R.id.tvHumidifierStatus);

        tvIncubationType = findViewById(R.id.tvIncubationType);
        tvDayCount = findViewById(R.id.tvDayCount);
        tvIncubationStatus = findViewById(R.id.tvIncubationStatus);
        tvCompletionStatus = findViewById(R.id.tvCompletionStatus);

        tvMotorStatus = findViewById(R.id.tvMotorStatus);
        tvMotorTiming = findViewById(R.id.tvMotorTiming);

        tvPidMode = findViewById(R.id.tvPidMode);
        tvPidValues = findViewById(R.id.tvPidValues);

        tvAlarmStatus = findViewById(R.id.tvAlarmStatus);

        tvLastSave = findViewById(R.id.tvLastSave);
        tvPendingChanges = findViewById(R.id.tvPendingChanges);
        tvAutoSaveStatus = findViewById(R.id.tvAutoSaveStatus);

        tvRTCTime = findViewById(R.id.tvRTCTime);

        updateConnectionStatus(false);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::refreshData);
    }

    private void startBackgroundService() {
        Intent serviceIntent = new Intent(this, BackgroundService.class);
        startService(serviceIntent);
    }

    private void tryMDNSConnection(final Runnable onFailure) {
        updateConnectionStatus(false, "mDNS ile bağlanıyor: " + Constants.MDNS_HOSTNAME);

        new Thread(() -> {
            int retryCount = 0;
            final int maxRetries = 3;
            boolean resolved = false;
            String resolvedIP = null;

            while (retryCount < maxRetries && !resolved) {
                try {
                    InetAddress address = InetAddress.getByName(Constants.MDNS_HOSTNAME);
                    resolvedIP = address.getHostAddress();

                    if (resolvedIP != null && !resolvedIP.isEmpty() && !resolvedIP.equals("127.0.0.1")) {
                        resolved = true;
                        Log.d(TAG, "mDNS çözümlendi (deneme " + (retryCount + 1) + "): " +
                                Constants.MDNS_HOSTNAME + " -> " + resolvedIP);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "mDNS çözümleme hatası (deneme " + (retryCount + 1) + "): " + e.getMessage());
                    retryCount++;

                    if (retryCount < maxRetries) {
                        try {
                            Thread.sleep(1000); // 1 saniye bekle
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }

            final String finalIP = resolvedIP;
            final boolean finalResolved = resolved;

            runOnUiThread(() -> {
                if (finalResolved && finalIP != null) {
                    prefsManager.saveDeviceIp(finalIP);
                    prefsManager.saveDevicePort(Constants.DEFAULT_PORT);
                    networkManager.resetConnection();
                    testMDNSConnection(finalIP);
                } else {
                    Log.w(TAG, "mDNS çözümlenemedi (" + maxRetries + " deneme sonrası)");
                    if (onFailure != null) onFailure.run();
                }
            });
        }).start();
    }

    private void tryMDNSConnection() {
        tryMDNSConnection(null);
    }

    // Yeni metod ekle - AP modunu kontrol et
    private void tryAPModeConnection(final Runnable onFailure) {
        updateConnectionStatus(false, "AP modu kontrol ediliyor...");

        // Önce IP adresini güncelle
        String originalIP = prefsManager.getDeviceIp();
        prefsManager.saveDeviceIp(Constants.DEFAULT_AP_IP);
        prefsManager.saveDevicePort(Constants.DEFAULT_PORT);
        prefsManager.saveConnectionMode(Constants.MODE_AP);
        networkManager.resetConnection();

        // 2 saniye bekle (ESP32'nin stabilize olması için)
        new Handler().postDelayed(() -> {
            // AP modunda test et
            networkManager.getSystemVerification(new Callback<ApiService.SystemVerificationResponse>() {
                @Override
                public void onResponse(Call<ApiService.SystemVerificationResponse> call,
                                       Response<ApiService.SystemVerificationResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        isSystemVerified = true;
                        updateConnectionStatus(true, "AP modunda bağlandı");
                        refreshData();
                        Log.d(TAG, "AP modu bağlantısı başarılı");
                    } else {
                        // AP modu başarısız, orijinal IP'yi geri yükle
                        prefsManager.saveDeviceIp(originalIP);
                        networkManager.resetConnection();
                        if (onFailure != null) onFailure.run();
                    }
                }

                @Override
                public void onFailure(Call<ApiService.SystemVerificationResponse> call, Throwable t) {
                    Log.e(TAG, "AP modu bağlantı testi başarısız: " + t.getMessage());
                    // Orijinal IP'yi geri yükle
                    prefsManager.saveDeviceIp(originalIP);
                    networkManager.resetConnection();
                    if (onFailure != null) onFailure.run();
                }
            });
        }, 2000);
    }

    private void testMDNSConnection(String ip) {
        updateConnectionStatus(false, "mDNS bağlantısı test ediliyor...");

        // Sistem doğrulama endpoint'ini dene
        networkManager.getSystemVerification(new Callback<ApiService.SystemVerificationResponse>() {
            @Override
            public void onResponse(Call<ApiService.SystemVerificationResponse> call,
                                   Response<ApiService.SystemVerificationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // mDNS ile bağlantı başarılı
                    isSystemVerified = true;
                    updateConnectionStatus(true, "mDNS bağlantısı başarılı");

                    // Station modunda olduğumuzu kaydet
                    prefsManager.saveConnectionMode(Constants.MODE_STATION);

                    refreshData();

                    Log.d(TAG, "mDNS bağlantısı başarılı: " + ip);
                } else {
                    Log.w(TAG, "mDNS IP doğru ancak sistem yanıt vermiyor");
                    // mDNS başarısız, discovery başlat
                    startNetworkDiscovery();
                }
            }

            @Override
            public void onFailure(Call<ApiService.SystemVerificationResponse> call, Throwable t) {
                Log.e(TAG, "mDNS bağlantı testi başarısız: " + t.getMessage());
                updateConnectionStatus(false, "mDNS bağlantısı başarısız");
                // mDNS başarısız, discovery başlat
                startNetworkDiscovery();
            }
        });
    }

    private void startDeviceDiscovery() {
        if (isDiscovering) return;

        Log.d(TAG, "Cihaz keşfi başlatılıyor - tüm modlar kontrol edilecek");

        // Önce mevcut kayıtlı IP'yi kontrol et (her iki mod için)
        String currentIP = prefsManager.getDeviceIp();
        if (currentIP != null && !currentIP.isEmpty() && !currentIP.equals("0.0.0.0")) {
            testCurrentConnection(currentIP);
            return;
        }

        // Kayıtlı IP yoksa, sırayla tüm yöntemleri dene
        tryAllConnectionMethods();
    }

    // Yeni metod ekle
    private void tryAllConnectionMethods() {
        updateConnectionStatus(false, "Cihaz aranıyor...");

        // 1. Önce mDNS dene (station modundaysa çalışır)
        tryMDNSConnection(new Runnable() {
            @Override
            public void run() {
                // mDNS başarısızsa AP modunu dene
                tryAPModeConnection(new Runnable() {
                    @Override
                    public void run() {
                        // AP modu da başarısızsa network discovery başlat
                        startNetworkDiscovery();
                    }
                });
            }
        });
    }

    private void testCurrentConnection(String ip) {
        updateConnectionStatus(false, "Mevcut bağlantı test ediliyor...");

        // Sistem doğrulama endpoint'ini dene
        networkManager.getSystemVerification(new Callback<ApiService.SystemVerificationResponse>() {
            @Override
            public void onResponse(Call<ApiService.SystemVerificationResponse> call,
                                   Response<ApiService.SystemVerificationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Bağlantı başarılı
                    isSystemVerified = true;
                    refreshData();
                } else {
                    // Mevcut IP ile bağlantı kurulamadı, discovery başlat
                    startNetworkDiscovery();
                }
            }

            @Override
            public void onFailure(Call<ApiService.SystemVerificationResponse> call, Throwable t) {
                Log.e(TAG, "Current connection test failed: " + t.getMessage());
                startNetworkDiscovery();
            }
        });
    }

    private void verifySystemConnection() {
        updateConnectionStatus(false, "Sistem bağlantısı doğrulanıyor...");

        networkManager.getSystemVerification(new Callback<ApiService.SystemVerificationResponse>() {
            @Override
            public void onResponse(Call<ApiService.SystemVerificationResponse> call,
                                   Response<ApiService.SystemVerificationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    isSystemVerified = true;
                    updateConnectionStatus(true, "Sistem bağlantısı doğrulandı");
                    refreshData();
                } else {
                    startNetworkDiscovery();
                }
            }

            @Override
            public void onFailure(Call<ApiService.SystemVerificationResponse> call, Throwable t) {
                Log.e(TAG, "System verification failed: " + t.getMessage());
                startNetworkDiscovery();
            }
        });
    }

    private void startNetworkDiscovery() {
        if (isDiscovering) return;

        isDiscovering = true;
        updateConnectionStatus(false, "Cihaz aranıyor...");

        Log.d(TAG, "Cihaz keşfi başlatılıyor...");
        discoveryManager.startDiscovery(this);

        // 15 saniye sonra discovery'yi durdur
        updateHandler.postDelayed(() -> {
            discoveryManager.stopDiscovery();
            isDiscovering = false;

            if (!isConnected) {
                int connectionMode = prefsManager.getConnectionMode();
                if (connectionMode == Constants.MODE_AP) {
                    // AP modunda varsayılan IP'yi dene
                    tryDirectConnection(Constants.DEFAULT_AP_IP);
                } else {
                    updateConnectionStatus(false, "Cihaz bulunamadı");
                }
            }
        }, 15000);
    }

    private void startPeriodicUpdates() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isUpdating) {
                    refreshData();
                }
                updateHandler.postDelayed(this, Constants.STATUS_UPDATE_INTERVAL);
            }
        };
        updateHandler.post(updateRunnable);
    }

    private void refreshData() {
        if (isUpdating) {
            return;
        }

        isUpdating = true;

        // Ana durum verisi
        networkManager.getDeviceStatus(new Callback<DeviceStatus>() {
            @Override
            public void onResponse(Call<DeviceStatus> call, Response<DeviceStatus> response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful() && response.body() != null) {
                        updateUI(response.body());
                        updateConnectionStatus(true);
                        prefsManager.saveLastUpdateTime(System.currentTimeMillis());
                        isConnected = true;
                        isSystemVerified = true;

                        // Detaylı durum verilerini yükle
                        loadDetailedStatus();
                    } else {
                        handleConnectionError("Veri alınamadı - HTTP: " + response.code());
                    }
                    isUpdating = false;
                    swipeRefresh.setRefreshing(false);
                });
            }

            @Override
            public void onFailure(Call<DeviceStatus> call, Throwable t) {
                runOnUiThread(() -> {
                    isUpdating = false;
                    swipeRefresh.setRefreshing(false);
                    handleConnectionError("Bağlantı hatası: " + t.getMessage());
                    Log.e(TAG, "Network error: " + t.getMessage());
                });
            }
        });
    }

    private void handleConnectionError(String error) {
        isConnected = false;
        isSystemVerified = false;
        updateConnectionStatus(false, error);

        // Bağlantı moduna göre farklı davran
        int connectionMode = prefsManager.getConnectionMode();

        if (connectionMode == Constants.MODE_AP && !error.contains("192.168.4.1")) {
            // AP modunda yanlış IP kullanılıyor olabilir
            tryDirectConnection(Constants.DEFAULT_AP_IP);
        } else if (connectionMode == Constants.MODE_STATION && !error.contains("kulucka.local")) {
            // Station modunda önce mDNS dene
            updateHandler.postDelayed(this::tryMDNSConnection, 2000);
        } else if (!isDiscovering) {
            // Diğer durumlarda discovery başlat
            updateHandler.postDelayed(this::startDeviceDiscovery, 5000);
        }
    }

    private void updateUI(DeviceStatus status) {
        // Temperature
        tvCurrentTemp.setText(String.format("%.1f°C", status.getTemperature()));
        tvTargetTemp.setText(String.format("Hedef: %.1f°C", status.getTargetTemp()));
        updateDeviceState(tvHeaterStatus, status.isHeaterState());

        // Humidity
        tvCurrentHumid.setText(String.format("%d%%", Math.round(status.getHumidity())));
        tvTargetHumid.setText(String.format("Hedef: %d%%", Math.round(status.getTargetHumid())));
        updateDeviceState(tvHumidifierStatus, status.isHumidifierState());

        // Incubation
        tvIncubationType.setText(getIncubationTypeName(status.getIncubationType()));
        tvDayCount.setText(String.format("%d/%d", status.getDisplayDay(), status.getTotalDays()));
        updateIncubationStatus(status);

        // Motor
        if (status.getMotor() != null) {
            DeviceStatus.MotorInfo motorInfo = status.getMotor();
            updateDeviceState(tvMotorStatus, motorInfo.isState());
            tvMotorTiming.setText(String.format("Bekleme: %d dk\nÇalışma: %d sn",
                    motorInfo.getWaitTime(), motorInfo.getRunTime()));
        } else {
            // Eski format için fallback
            updateDeviceState(tvMotorStatus, status.isMotorState());
            tvMotorTiming.setText(String.format("Bekleme: %d dk\nÇalışma: %d sn",
                    status.getMotorWaitTime(), status.getMotorRunTime()));
        }
        // PID
        updatePidStatus(status);

        // Alarm
        updateAlarmStatus(status.isAlarmEnabled());

        // Güvenilirlik bilgilerini göster
        if (status.getReliability() != null) {
            DeviceStatus.ReliabilityInfo reliability = status.getReliability();

            long lastSaveSeconds = reliability.getLastSave();
            if (lastSaveSeconds < 60) {
                tvLastSave.setText(lastSaveSeconds + " saniye önce");
            } else if (lastSaveSeconds < 3600) {
                tvLastSave.setText((lastSaveSeconds / 60) + " dakika önce");
            } else {
                tvLastSave.setText((lastSaveSeconds / 3600) + " saat önce");
            }

            tvPendingChanges.setText(String.valueOf(reliability.getPendingChanges()));

            if (reliability.isAutoSaveEnabled()) {
                tvAutoSaveStatus.setText("✓ Otomatik kayıt aktif");
                tvAutoSaveStatus.setTextColor(getColor(R.color.success));
            } else {
                tvAutoSaveStatus.setText("✗ Otomatik kayıt kapalı");
                tvAutoSaveStatus.setTextColor(getColor(R.color.error));
            }
        }

        // RTC durumunu göster - YENİ EKLENEN
        if (status.getTimestamp() > 0) {
            // RTC saatini al ve göster
            networkManager.getRTCStatus(new Callback<ApiService.RTCStatusResponse>() {
                @Override
                public void onResponse(Call<ApiService.RTCStatusResponse> call,
                                       Response<ApiService.RTCStatusResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        tvRTCTime.setText(response.body().getTime());
                    }
                }

                @Override
                public void onFailure(Call<ApiService.RTCStatusResponse> call, Throwable t) {
                    tvRTCTime.setText("--:--");
                }
            });
        }
    }

    private void updateConnectionStatus(boolean connected) {
        updateConnectionStatus(connected, null);
    }

    private void updateConnectionStatus(boolean connected, String customMessage) {
        String message;
        int color;
        int iconRes;

        if (connected) {
            message = "Bağlı";
            color = getColor(R.color.success);
            iconRes = R.drawable.ic_wifi_on;
        } else {
            if (customMessage != null) {
                if (customMessage.contains("hata") || customMessage.contains("başarısız")) {
                    message = "Bağlantı Hatası";
                } else if (customMessage.contains("aranıyor") || customMessage.contains("bağlanıyor")) {
                    message = "Bağlanıyor...";
                } else {
                    message = "Bağlı Değil";
                }
            } else {
                message = "Bağlı Değil";
            }
            color = getColor(R.color.error);
            iconRes = R.drawable.ic_wifi_off;
        }

        tvConnectionStatus.setText(message);
        tvConnectionStatus.setTextColor(color);
        ivConnectionStatus.setImageResource(iconRes);
        ivConnectionStatus.setColorFilter(color);
    }

    private void updateDeviceState(TextView textView, boolean isOn) {
        if (isOn) {
            textView.setText("AÇIK");
            textView.setTextColor(getColor(R.color.active));
        } else {
            textView.setText("KAPALI");
            textView.setTextColor(getColor(R.color.inactive));
        }
    }

    private void updateIncubationStatus(DeviceStatus status) {
        if (status.isIncubationRunning()) {
            tvIncubationStatus.setText("Çalışıyor");
            tvIncubationStatus.setTextColor(getColor(R.color.success));

            if (status.isIncubationCompleted()) {
                tvCompletionStatus.setVisibility(View.VISIBLE);
                tvCompletionStatus.setText(String.format("Kuluçka süresi tamamlandı! (Gerçek Gün: %d)",
                        status.getActualDay()));
            } else {
                tvCompletionStatus.setVisibility(View.GONE);
            }
        } else {
            tvIncubationStatus.setText("Durduruldu");
            tvIncubationStatus.setTextColor(getColor(R.color.error));
            tvCompletionStatus.setVisibility(View.GONE);
        }
    }

    private void updatePidStatus(DeviceStatus status) {
        String pidModeText;
        int pidColor;

        switch (status.getPidMode()) {
            case Constants.PID_MODE_MANUAL:
                pidModeText = "Manuel";
                pidColor = R.color.warning;
                break;
            case Constants.PID_MODE_AUTO:
                pidModeText = "Otomatik";
                pidColor = R.color.success;
                break;
            default:
                pidModeText = "Kapalı";
                pidColor = R.color.inactive;
                break;
        }

        tvPidMode.setText(pidModeText);
        tvPidMode.setTextColor(getColor(pidColor));

        tvPidValues.setText(String.format("Kp: %.2f\nKi: %.2f\nKd: %.2f",
                status.getPidKp(), status.getPidKi(), status.getPidKd()));
    }

    private void updateAlarmStatus(boolean enabled) {
        if (enabled) {
            tvAlarmStatus.setText("Açık");
            tvAlarmStatus.setTextColor(getColor(R.color.success));
        } else {
            tvAlarmStatus.setText("Kapalı");
            tvAlarmStatus.setTextColor(getColor(R.color.error));
        }
    }

    private String getIncubationTypeName(String type) {
        if (type == null) return "--";

        // Önce sayısal değerleri kontrol et
        switch (type) {
            case "0":
                return "Tavuk";
            case "1":
                return "Bıldırcın";
            case "2":
                return "Kaz";
            case "3":
                return "Manuel";
            default:
                // Sayısal değilse string olarak kontrol et
                switch (type.toLowerCase()) {
                    case "tavuk":
                        return "Tavuk";
                    case "bıldırcın":
                        return "Bıldırcın";
                    case "kaz":
                        return "Kaz";
                    case "manuel":
                        return "Manuel";
                    default:
                        return type; // Tanınmayan değeri aynen döndür
                }
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void tryDirectConnection(String ip) {
        updateConnectionStatus(false, "Bağlantı deneniyor: " + ip);

        // IP ve port'u güncelle
        prefsManager.saveDeviceIp(ip);
        networkManager.resetConnection();

        // Sistem doğrulama endpoint'ini dene
        networkManager.getSystemVerification(new Callback<ApiService.SystemVerificationResponse>() {
            @Override
            public void onResponse(Call<ApiService.SystemVerificationResponse> call,
                                   Response<ApiService.SystemVerificationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Direct connection successful for IP: " + ip);
                    isSystemVerified = true;
                    runOnUiThread(() -> refreshData());
                } else {
                    runOnUiThread(() -> tryFallbackConnection(ip));
                }
            }

            @Override
            public void onFailure(Call<ApiService.SystemVerificationResponse> call, Throwable t) {
                Log.e(TAG, "Direct connection failed for IP: " + ip);
                runOnUiThread(() -> tryFallbackConnection(ip));
            }
        });
    }

    private void tryFallbackConnection(String ip) {
        // Eski ping endpoint'ini dene
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        String url = "http://" + ip + "/api/ping";
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.d(TAG, "Ping failed for IP: " + ip);
                runOnUiThread(() -> tryDiscoveryEndpoint(ip));
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String body = response.body().string();
                        if ("pong".equals(body)) {
                            Log.d(TAG, "Ping successful for IP: " + ip);
                            runOnUiThread(() -> refreshData());
                        } else {
                            runOnUiThread(() -> tryDiscoveryEndpoint(ip));
                        }
                    } catch (Exception e) {
                        runOnUiThread(() -> tryDiscoveryEndpoint(ip));
                    }
                } else {
                    runOnUiThread(() -> tryDiscoveryEndpoint(ip));
                }
                response.close();
            }
        });
    }

    private void tryDiscoveryEndpoint(String ip) {
        networkManager.getDiscoveryInfo(new retrofit2.Callback<ApiService.DiscoveryResponse>() {
            @Override
            public void onResponse(retrofit2.Call<ApiService.DiscoveryResponse> call,
                                   retrofit2.Response<ApiService.DiscoveryResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiService.DiscoveryResponse discovery = response.body();
                    if ("KULUCKA_MK_v5".equals(discovery.getDevice())) {
                        runOnUiThread(() -> {
                            Log.d(TAG, "Discovery successful for IP: " + ip);
                            prefsManager.saveDeviceIp(ip);
                            prefsManager.saveDevicePort(discovery.getPort());
                            networkManager.resetConnection();
                            refreshData();
                        });
                    } else {
                        runOnUiThread(() -> refreshData());
                    }
                } else {
                    runOnUiThread(() -> refreshData());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ApiService.DiscoveryResponse> call, Throwable t) {
                Log.e(TAG, "Discovery endpoint failed: " + t.getMessage());
                runOnUiThread(() -> refreshData());
            }
        });
    }

    private void registerWifiStateReceiver() {
        wifiStateReceiver = new WifiStateReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(wifiStateReceiver, filter);
        Log.d(TAG, "WiFi durumu receiver kayıt edildi");
    }

    private void startAutoConnectSystem() {
        if (autoConnectTimer != null) {
            autoConnectTimer.cancel();
        }

        autoConnectTimer = new Timer();
        autoConnectTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isAutoConnectEnabled && !isConnected && !isDiscovering) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastAutoConnectAttempt > AUTO_CONNECT_INTERVAL) {
                        runOnUiThread(() -> tryAutoConnect());
                    }
                }
            }
        }, 3000, AUTO_CONNECT_INTERVAL);

        Log.d(TAG, "Otomatik bağlantı sistemi başlatıldı");
    }

    private void tryAutoConnect() {
        if (autoConnectAttemptCount >= MAX_AUTO_CONNECT_ATTEMPTS) {
            Log.d(TAG, "Maksimum otomatik bağlantı deneme sayısına ulaşıldı");
            return;
        }

        lastAutoConnectAttempt = System.currentTimeMillis();
        autoConnectAttemptCount++;

        Log.d(TAG, "Otomatik bağlantı denemesi: " + autoConnectAttemptCount);

        String savedIP = prefsManager.getDeviceIp();
        int connectionMode = prefsManager.getConnectionMode();

        if (connectionMode == Constants.MODE_AP || (savedIP != null && savedIP.equals(Constants.DEFAULT_AP_IP))) {
            tryAPConnection();
        } else if (connectionMode == Constants.MODE_STATION) {
            tryStationConnection();
        } else {
            startDeviceDiscovery();
        }
    }

    private void tryAPConnection() {
        Log.d(TAG, "AP modunda bağlantı deneniyor");

        String apIP = Constants.DEFAULT_AP_IP;
        prefsManager.saveDeviceIp(apIP);
        prefsManager.saveConnectionMode(Constants.MODE_AP);
        networkManager.resetConnection();

        networkManager.getSystemVerification(new Callback<ApiService.SystemVerificationResponse>() {
            @Override
            public void onResponse(Call<ApiService.SystemVerificationResponse> call,
                                   Response<ApiService.SystemVerificationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    handleSuccessfulConnection("AP Modu");
                } else {
                    Log.d(TAG, "AP sistem doğrulaması başarısız");
                    tryStationConnection();
                }
            }

            @Override
            public void onFailure(Call<ApiService.SystemVerificationResponse> call, Throwable t) {
                Log.d(TAG, "AP bağlantısı başarısız: " + t.getMessage());
                tryStationConnection();
            }
        });
    }

    private void tryStationConnection() {
        Log.d(TAG, "Station modunda bağlantı deneniyor");

        new Thread(() -> {
            try {
                InetAddress address = InetAddress.getByName(Constants.MDNS_HOSTNAME);
                String resolvedIP = address.getHostAddress();

                if (resolvedIP != null && !resolvedIP.isEmpty() && !resolvedIP.equals("127.0.0.1")) {
                    runOnUiThread(() -> {
                        Log.d(TAG, "mDNS çözümlendi: " + resolvedIP);
                        prefsManager.saveDeviceIp(resolvedIP);
                        prefsManager.saveConnectionMode(Constants.MODE_STATION);
                        networkManager.resetConnection();
                        testStationConnection(resolvedIP);
                    });
                } else {
                    runOnUiThread(() -> {
                        Log.d(TAG, "mDNS çözümlenemedi, discovery başlatılıyor");
                        if (!isDiscovering) {
                            startDeviceDiscovery();
                        }
                    });
                }
            } catch (Exception e) {
                Log.d(TAG, "mDNS hatası: " + e.getMessage());
                runOnUiThread(() -> {
                    if (!isDiscovering) {
                        startDeviceDiscovery();
                    }
                });
            }
        }).start();
    }

    private void testStationConnection(String ip) {
        networkManager.getSystemVerification(new Callback<ApiService.SystemVerificationResponse>() {
            @Override
            public void onResponse(Call<ApiService.SystemVerificationResponse> call,
                                   Response<ApiService.SystemVerificationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    handleSuccessfulConnection("Station Modu");
                } else {
                    Log.d(TAG, "Station sistem doğrulaması başarısız");
                    if (!isDiscovering) {
                        startDeviceDiscovery();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiService.SystemVerificationResponse> call, Throwable t) {
                Log.d(TAG, "Station bağlantı testi başarısız: " + t.getMessage());
                if (!isDiscovering) {
                    startDeviceDiscovery();
                }
            }
        });
    }

    private void handleSuccessfulConnection(String mode) {
        isConnected = true;
        isSystemVerified = true;
        autoConnectAttemptCount = 0; // Başarılı bağlantı sonrası sayacı sıfırla

        updateConnectionStatus(true);
        refreshData();

        Log.d(TAG, mode + " bağlantısı başarılı");
    }

    private void resetAutoConnectAttempts() {
        autoConnectAttemptCount = 0;
        lastAutoConnectAttempt = 0;
    }

    private void loadDetailedStatus() {
        // PID detaylı durumu
        networkManager.getPidStatus(new Callback<ApiService.PidStatusResponse>() {
            @Override
            public void onResponse(Call<ApiService.PidStatusResponse> call,
                                   Response<ApiService.PidStatusResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiService.PidStatusResponse pidStatus = response.body();
                    updatePidCard(pidStatus);
                }
            }

            @Override
            public void onFailure(Call<ApiService.PidStatusResponse> call, Throwable t) {
                // Hata durumunda mevcut gösterimi koru
            }
        });
    }

    private void updatePidCard(ApiService.PidStatusResponse status) {
        tvPidMode.setText(status.getModeString());

        if (status.isActive()) {
            tvPidMode.setTextColor(getColor(R.color.success));
            tvPidValues.setText(String.format(
                    "Kp: %.2f Ki: %.2f Kd: %.2f\nHata: %.2f°C Çıkış: %.1f%%",
                    status.getKp(), status.getKi(), status.getKd(),
                    status.getError(), status.getOutput()
            ));
        } else {
            tvPidMode.setTextColor(getColor(R.color.inactive));
            tvPidValues.setText(String.format("Kp: %.2f\nKi: %.2f\nKd: %.2f",
                    status.getKp(), status.getKi(), status.getKd()
            ));
        }
    }

    private void updateMotorCard(ApiService.MotorStatusResponse status) {
        updateDeviceState(tvMotorStatus, status.isRunning());
        tvMotorTiming.setText(String.format(
                "Bekleme: %d dk\nÇalışma: %d sn\nDurum: %s",
                status.getWaitTime(), status.getRunTime(), status.getStatus()
        ));
    }

    private class WifiStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                        WifiManager.WIFI_STATE_UNKNOWN);

                if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
                    Log.d(TAG, "WiFi açıldı, otomatik bağlantı başlatılıyor");
                    resetAutoConnectAttempts();
                    updateHandler.postDelayed(() -> {
                        if (!isConnected) {
                            tryAutoConnect();
                        }
                    }, 3000);
                } else if (wifiState == WifiManager.WIFI_STATE_DISABLED) {
                    Log.d(TAG, "WiFi kapatıldı");
                    isConnected = false;
                    updateConnectionStatus(false, "WiFi kapalı");
                }
            } else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

                if (activeNetwork != null && activeNetwork.isConnected()) {
                    if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                        Log.d(TAG, "WiFi ağına bağlanıldı, cihaz bağlantısı kontrol ediliyor");
                        resetAutoConnectAttempts();
                        updateHandler.postDelayed(() -> {
                            if (!isConnected) {
                                tryAutoConnect();
                            }
                        }, 2000);
                    }
                }
            }
        }
    }

    // NetworkDiscoveryManager.DiscoveryCallback implementation
    @Override
    public void onDeviceFound(String ipAddress, int port) {
        runOnUiThread(() -> {
            Log.d(TAG, "Cihaz bulundu: " + ipAddress + ":" + port);

            // IP adresini güncelle
            prefsManager.saveDeviceIp(ipAddress);
            prefsManager.saveDevicePort(port);

            // NetworkManager'ı sıfırla
            networkManager.resetConnection();

            // Sistem doğrulaması yap
            verifySystemConnection();

            updateConnectionStatus(true, "Cihaz bulundu: " + ipAddress);
        });
    }

    @Override
    public void onDiscoveryComplete() {
        runOnUiThread(() -> {
            isDiscovering = false;
            Log.d(TAG, "Cihaz keşfi tamamlandı");
        });
    }

    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            isDiscovering = false;
            Log.e(TAG, "Discovery hatası: " + error);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SETTINGS_REQUEST_CODE && resultCode == RESULT_OK) {
            Log.d(TAG, "Ayarlar güncellendi, veriler yenileniyor...");

            // NetworkManager'ı sıfırla (IP değişmiş olabilir)
            networkManager.resetConnection();

            // İlk yenileme
            refreshData();

            // 1 saniye sonra tekrar yenile (ESP32'nin güncellemesi için)
            updateHandler.postDelayed(() -> {
                refreshData();
            }, 1000);

            // 3 saniye sonra bir kez daha yenile
            updateHandler.postDelayed(() -> {
                refreshData();
            }, 3000);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // NetworkManager'ı sıfırla (bağlantı değişmiş olabilir)
        networkManager.resetConnection();

        // Ana ekrana her dönüldüğünde verileri yenile
        refreshData();

        // Periyodik güncellemeyi yeniden başlat
        updateHandler.removeCallbacks(updateRunnable);
        updateHandler.post(updateRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Aktivite arka plana geçtiğinde periyodik güncellemeyi durdur
        updateHandler.removeCallbacks(updateRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (wifiStateReceiver != null) {
            try {
                unregisterReceiver(wifiStateReceiver);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Receiver already unregistered");
            }
        }

        if (autoConnectTimer != null) {
            autoConnectTimer.cancel();
            autoConnectTimer = null;
        }

        if (updateHandler != null && updateRunnable != null) {
            updateHandler.removeCallbacks(updateRunnable);
        }

        if (discoveryManager != null) {
            discoveryManager.stopDiscovery();
        }
    }
}