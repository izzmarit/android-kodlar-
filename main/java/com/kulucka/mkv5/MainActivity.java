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
import java.util.concurrent.atomic.AtomicBoolean;

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
import com.kulucka.mkv5.utils.NetworkUtils;

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
    private AtomicBoolean isDiscovering = new AtomicBoolean(false); // Thread-safe
    private boolean isSystemVerified = false;
    private WifiStateReceiver wifiStateReceiver;
    private boolean isAutoConnectEnabled = true;
    private Timer autoConnectTimer;
    private static final long AUTO_CONNECT_INTERVAL = 10000; // 10 saniye olarak artırıldı
    private long lastAutoConnectAttempt = 0;
    private int autoConnectAttemptCount = 0;
    private static final int MAX_AUTO_CONNECT_ATTEMPTS = 5; // 5'e çıkarıldı
    private static final long WIFI_STATE_CHECK_INTERVAL = 3000; // 3 saniyeye çıkarıldı
    private Handler wifiCheckHandler = new Handler();
    private Runnable wifiCheckRunnable;
    private boolean wasConnectedBefore = false;
    private long lastDiscoveryTime = 0;
    private static final long DISCOVERY_COOLDOWN = 30000; // 30 saniye discovery cooldown
    private int connectionRetryCount = 0;
    private static final int MAX_CONNECTION_RETRIES = 3;

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

        // İlk başlatmada biraz bekle
        updateHandler.postDelayed(() -> {
            startAutoConnectSystem();
            startPeriodicUpdates();
            startWifiMonitoring();
        }, 1000);
    }

    private void startWifiMonitoring() {
        wifiCheckRunnable = new Runnable() {
            @Override
            public void run() {
                checkWifiConnectionAndReconnect();
                wifiCheckHandler.postDelayed(this, WIFI_STATE_CHECK_INTERVAL);
            }
        };
        wifiCheckHandler.post(wifiCheckRunnable);
    }

    private void checkWifiConnectionAndReconnect() {
        boolean currentlyConnected = NetworkUtils.isWifiConnected(this);

        // WiFi bağlantısı kesildi ve önceden bağlıydı
        if (!currentlyConnected && wasConnectedBefore) {
            Log.d(TAG, "WiFi bağlantısı kesildi, yeniden bağlanma bekleniyor...");
            wasConnectedBefore = false;
            isConnected = false;
            updateConnectionStatus(false, "WiFi bağlantısı kesildi");
            connectionRetryCount = 0; // Retry sayacını sıfırla
        }
        // WiFi bağlantısı yeniden kuruldu
        else if (currentlyConnected && !wasConnectedBefore) {
            Log.d(TAG, "WiFi bağlantısı yeniden kuruldu, cihaza bağlanılıyor...");
            wasConnectedBefore = true;

            // 3 saniye bekle ve cihaza bağlan
            updateHandler.postDelayed(() -> {
                resetAutoConnectAttempts();
                attemptConnection();
            }, 3000);
        }
        // İlk başlatma
        else if (currentlyConnected && !isConnected) {
            wasConnectedBefore = true;
            attemptConnection();
        }
    }

    private void attemptConnection() {
        if (isConnected || isDiscovering.get()) {
            return;
        }

        connectionRetryCount++;

        if (connectionRetryCount <= MAX_CONNECTION_RETRIES) {
            Log.d(TAG, "Bağlantı denemesi " + connectionRetryCount + "/" + MAX_CONNECTION_RETRIES);
            tryAutoConnect();
        } else {
            Log.d(TAG, "Maksimum bağlantı denemesi aşıldı, discovery başlatılıyor");
            connectionRetryCount = 0;
            startSmartDiscovery();
        }
    }

    private void startSmartDiscovery() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastDiscoveryTime < DISCOVERY_COOLDOWN) {
            Log.d(TAG, "Discovery cooldown aktif, bekleniyor...");
            return;
        }

        if (!isDiscovering.compareAndSet(false, true)) {
            Log.d(TAG, "Discovery zaten çalışıyor");
            return;
        }

        lastDiscoveryTime = currentTime;
        updateConnectionStatus(false, "Cihaz aranıyor...");

        // Önce bilinen IP'leri kontrol et
        String savedIP = prefsManager.getDeviceIp();
        if (savedIP != null && !savedIP.isEmpty() && !savedIP.equals("0.0.0.0")) {
            testDirectConnection(savedIP, () -> {
                // Başarısızsa discovery başlat
                startNetworkDiscovery();
            });
        } else {
            startNetworkDiscovery();
        }
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
        swipeRefresh.setOnRefreshListener(() -> {
            if (isConnected) {
                refreshData();
            } else {
                swipeRefresh.setRefreshing(false);
                attemptConnection();
            }
        });
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
                            Thread.sleep(2000); // 2 saniye bekle
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

    private void tryAPModeConnection(final Runnable onFailure) {
        updateConnectionStatus(false, "AP modu kontrol ediliyor...");

        String originalIP = prefsManager.getDeviceIp();
        prefsManager.saveDeviceIp(Constants.DEFAULT_AP_IP);
        prefsManager.saveDevicePort(Constants.DEFAULT_PORT);
        prefsManager.saveConnectionMode(Constants.MODE_AP);
        networkManager.resetConnection();

        new Handler().postDelayed(() -> {
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
                        prefsManager.saveDeviceIp(originalIP);
                        networkManager.resetConnection();
                        if (onFailure != null) onFailure.run();
                    }
                }

                @Override
                public void onFailure(Call<ApiService.SystemVerificationResponse> call, Throwable t) {
                    Log.e(TAG, "AP modu bağlantı testi başarısız: " + t.getMessage());
                    prefsManager.saveDeviceIp(originalIP);
                    networkManager.resetConnection();
                    if (onFailure != null) onFailure.run();
                }
            });
        }, 2000);
    }

    private void testMDNSConnection(String ip) {
        updateConnectionStatus(false, "mDNS bağlantısı test ediliyor...");

        networkManager.getSystemVerification(new Callback<ApiService.SystemVerificationResponse>() {
            @Override
            public void onResponse(Call<ApiService.SystemVerificationResponse> call,
                                   Response<ApiService.SystemVerificationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    isSystemVerified = true;
                    updateConnectionStatus(true, "mDNS bağlantısı başarılı");
                    prefsManager.saveConnectionMode(Constants.MODE_STATION);
                    refreshData();
                    Log.d(TAG, "mDNS bağlantısı başarılı: " + ip);
                } else {
                    Log.w(TAG, "mDNS IP doğru ancak sistem yanıt vermiyor");
                    startSmartDiscovery();
                }
            }

            @Override
            public void onFailure(Call<ApiService.SystemVerificationResponse> call, Throwable t) {
                Log.e(TAG, "mDNS bağlantı testi başarısız: " + t.getMessage());
                updateConnectionStatus(false, "mDNS bağlantısı başarısız");
                startSmartDiscovery();
            }
        });
    }

    private void startNetworkDiscovery() {
        if (!isDiscovering.get()) {
            Log.d(TAG, "Network discovery zaten çalışmıyor, iptal");
            return;
        }

        updateConnectionStatus(false, "Ağda cihaz aranıyor...");
        Log.d(TAG, "Cihaz keşfi başlatılıyor...");
        discoveryManager.startDiscovery(this);

        updateHandler.postDelayed(() -> {
            discoveryManager.stopDiscovery();
            isDiscovering.set(false);

            if (!isConnected) {
                int connectionMode = prefsManager.getConnectionMode();
                if (connectionMode == Constants.MODE_AP) {
                    tryDirectConnection(Constants.DEFAULT_AP_IP);
                } else {
                    updateConnectionStatus(false, "Cihaz bulunamadı");
                    connectionRetryCount = 0;
                }
            }
        }, 15000);
    }

    private void startPeriodicUpdates() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isUpdating && isConnected) {
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
                        connectionRetryCount = 0; // Başarılı bağlantıda sıfırla
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

        // Hemen yeniden deneme yerine biraz bekle
        if (!isDiscovering.get()) {
            updateHandler.postDelayed(() -> {
                attemptConnection();
            }, 5000);
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

        // RTC durumunu göster
        if (status.getTimestamp() > 0) {
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
                        return type;
                }
        }
    }

    private void tryDirectConnection(String ip) {
        updateConnectionStatus(false, "Bağlantı deneniyor: " + ip);

        prefsManager.saveDeviceIp(ip);
        networkManager.resetConnection();

        networkManager.getSystemVerification(new Callback<ApiService.SystemVerificationResponse>() {
            @Override
            public void onResponse(Call<ApiService.SystemVerificationResponse> call,
                                   Response<ApiService.SystemVerificationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Direct connection successful for IP: " + ip);
                    isSystemVerified = true;
                    runOnUiThread(() -> {
                        isConnected = true;
                        connectionRetryCount = 0;
                        refreshData();
                    });
                } else {
                    runOnUiThread(() -> updateConnectionStatus(false, "Cihaz yanıt vermiyor"));
                }
            }

            @Override
            public void onFailure(Call<ApiService.SystemVerificationResponse> call, Throwable t) {
                Log.e(TAG, "Direct connection failed for IP: " + ip);
                runOnUiThread(() -> updateConnectionStatus(false, "Bağlantı başarısız"));
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
                if (isAutoConnectEnabled && !isConnected && !isDiscovering.get()) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastAutoConnectAttempt > AUTO_CONNECT_INTERVAL) {
                        runOnUiThread(() -> tryAutoConnect());
                    }
                }
            }
        }, 5000, AUTO_CONNECT_INTERVAL);

        Log.d(TAG, "Otomatik bağlantı sistemi başlatıldı");
    }

    private void tryAutoConnect() {
        if (isConnected || isDiscovering.get()) {
            return;
        }

        lastAutoConnectAttempt = System.currentTimeMillis();
        autoConnectAttemptCount++;

        if (autoConnectAttemptCount > MAX_AUTO_CONNECT_ATTEMPTS) {
            Log.d(TAG, "Maksimum otomatik bağlantı denemesi aşıldı");
            autoConnectAttemptCount = 0;
            return;
        }

        String savedIP = prefsManager.getDeviceIp();
        int connectionMode = prefsManager.getConnectionMode();

        Log.d(TAG, "Otomatik bağlantı denemesi " + autoConnectAttemptCount + " - IP: " + savedIP +
                ", Mod: " + (connectionMode == Constants.MODE_AP ? "AP" : "Station"));

        if (savedIP != null && !savedIP.isEmpty() && !savedIP.equals("0.0.0.0")) {
            testDirectConnection(savedIP, () -> {
                if (connectionMode == Constants.MODE_AP) {
                    tryAPModeConnection(null);
                } else {
                    tryMDNSConnection(() -> startSmartDiscovery());
                }
            });
        } else {
            if (connectionMode == Constants.MODE_AP) {
                tryAPModeConnection(() -> startSmartDiscovery());
            } else {
                tryMDNSConnection(() -> startSmartDiscovery());
            }
        }
    }

    private void testDirectConnection(String ip, Runnable onFailure) {
        updateConnectionStatus(false, "Bağlantı test ediliyor: " + ip);

        networkManager.getSystemVerification(new Callback<ApiService.SystemVerificationResponse>() {
            @Override
            public void onResponse(Call<ApiService.SystemVerificationResponse> call,
                                   Response<ApiService.SystemVerificationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    handleSuccessfulConnection("Doğrudan Bağlantı");
                    refreshData();
                } else if (onFailure != null) {
                    onFailure.run();
                }
            }

            @Override
            public void onFailure(Call<ApiService.SystemVerificationResponse> call, Throwable t) {
                Log.e(TAG, "Doğrudan bağlantı başarısız: " + t.getMessage());
                if (onFailure != null) {
                    onFailure.run();
                }
            }
        });
    }

    private void handleSuccessfulConnection(String mode) {
        isConnected = true;
        isSystemVerified = true;
        autoConnectAttemptCount = 0;
        connectionRetryCount = 0;

        updateConnectionStatus(true);
        refreshData();

        Log.d(TAG, mode + " bağlantısı başarılı");
    }

    private void resetAutoConnectAttempts() {
        autoConnectAttemptCount = 0;
        lastAutoConnectAttempt = 0;
        connectionRetryCount = 0;
    }

    private void loadDetailedStatus() {
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
                            attemptConnection();
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
                                attemptConnection();
                            }
                        }, 2000);
                    }
                }
            }
        }
    }

    @Override
    public void onDeviceFound(String ipAddress, int port) {
        runOnUiThread(() -> {
            Log.d(TAG, "Cihaz bulundu: " + ipAddress + ":" + port);

            prefsManager.saveDeviceIp(ipAddress);
            prefsManager.saveDevicePort(port);
            networkManager.resetConnection();

            isSystemVerified = true;
            isConnected = true;
            isDiscovering.set(false);
            connectionRetryCount = 0;

            updateConnectionStatus(true, "Cihaz bulundu: " + ipAddress);
            refreshData();
        });
    }

    @Override
    public void onDiscoveryComplete() {
        runOnUiThread(() -> {
            isDiscovering.set(false);
            Log.d(TAG, "Cihaz keşfi tamamlandı");
        });
    }

    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            isDiscovering.set(false);
            Log.e(TAG, "Discovery hatası: " + error);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SETTINGS_REQUEST_CODE && resultCode == RESULT_OK) {
            Log.d(TAG, "Ayarlar güncellendi, veriler yenileniyor...");

            networkManager.resetConnection();
            isConnected = false;
            isSystemVerified = false;

            // Yeni bağlantı dene
            updateHandler.postDelayed(() -> {
                attemptConnection();
            }, 1000);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!isConnected) {
            attemptConnection();
        } else {
            refreshData();
        }

        updateHandler.removeCallbacks(updateRunnable);
        updateHandler.post(updateRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateHandler.removeCallbacks(updateRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (wifiCheckHandler != null && wifiCheckRunnable != null) {
            wifiCheckHandler.removeCallbacks(wifiCheckRunnable);
        }

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