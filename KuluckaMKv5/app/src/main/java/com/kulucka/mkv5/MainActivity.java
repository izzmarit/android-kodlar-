package com.kulucka.mkv5;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
    private FloatingActionButton fabSettings;

    // Variables
    private NetworkManager networkManager;
    private NetworkDiscoveryManager discoveryManager;
    private SharedPreferencesManager prefsManager;
    private Handler updateHandler;
    private Runnable updateRunnable;
    private boolean isUpdating = false;
    private boolean isConnected = false;
    private boolean isDiscovering = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupListeners();

        networkManager = NetworkManager.getInstance(this);
        discoveryManager = new NetworkDiscoveryManager(this);
        prefsManager = SharedPreferencesManager.getInstance(this);
        updateHandler = new Handler();

        // Start background service
        startBackgroundService();

        // Auto-discover device if needed
        startDeviceDiscovery();

        // Start periodic updates
        startPeriodicUpdates();
    }

    private void initializeViews() {
        swipeRefresh = findViewById(R.id.swipeRefresh);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        ivConnectionStatus = findViewById(R.id.ivConnectionStatus);

        // Temperature views
        tvCurrentTemp = findViewById(R.id.tvCurrentTemp);
        tvTargetTemp = findViewById(R.id.tvTargetTemp);
        tvHeaterStatus = findViewById(R.id.tvHeaterStatus);

        // Humidity views
        tvCurrentHumid = findViewById(R.id.tvCurrentHumid);
        tvTargetHumid = findViewById(R.id.tvTargetHumid);
        tvHumidifierStatus = findViewById(R.id.tvHumidifierStatus);

        // Incubation views
        tvIncubationType = findViewById(R.id.tvIncubationType);
        tvDayCount = findViewById(R.id.tvDayCount);
        tvIncubationStatus = findViewById(R.id.tvIncubationStatus);
        tvCompletionStatus = findViewById(R.id.tvCompletionStatus);

        // Motor views
        tvMotorStatus = findViewById(R.id.tvMotorStatus);
        tvMotorTiming = findViewById(R.id.tvMotorTiming);

        // PID views
        tvPidMode = findViewById(R.id.tvPidMode);
        tvPidValues = findViewById(R.id.tvPidValues);

        // Alarm view
        tvAlarmStatus = findViewById(R.id.tvAlarmStatus);

        fabSettings = findViewById(R.id.fabSettings);

        // Set initial connection status
        updateConnectionStatus(false);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::refreshData);

        fabSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivityForResult(intent, SETTINGS_REQUEST_CODE);
        });
    }

    private void startBackgroundService() {
        Intent serviceIntent = new Intent(this, BackgroundService.class);
        startService(serviceIntent);
    }

    private void startDeviceDiscovery() {
        if (isDiscovering) return;

        // Mevcut bağlantı modunu kontrol et
        int connectionMode = prefsManager.getConnectionMode();

        // AP modundaysa ve IP doğruysa discovery yapmaya gerek yok
        if (connectionMode == Constants.MODE_AP) {
            String currentIP = prefsManager.getDeviceIp();
            if (currentIP.equals(Constants.DEFAULT_AP_IP)) {
                // AP modunda sabit IP kullanılıyor, doğrudan bağlan
                return;
            }
        }

        // Station modunda veya IP belirsizse discovery başlat
        long lastUpdate = prefsManager.getLastUpdateTime();
        long timeDiff = System.currentTimeMillis() - lastUpdate;

        if (!isConnected || timeDiff > 10000) { // 10 saniyeye düşürüldü
            isDiscovering = true;
            updateConnectionStatus(false, "Cihaz aranıyor...");

            Log.d(TAG, "Cihaz keşfi başlatılıyor...");
            discoveryManager.startDiscovery(this);

            // 10 saniye sonra discovery'yi durdur (15'ten düşürüldü)
            updateHandler.postDelayed(() -> {
                discoveryManager.stopDiscovery();
                isDiscovering = false;

                if (!isConnected) {
                    // Bağlantı modu kontrol et
                    if (connectionMode == Constants.MODE_AP) {
                        // AP modunda varsayılan IP'yi dene
                        tryDirectConnection(Constants.DEFAULT_AP_IP);
                    } else {
                        updateConnectionStatus(false, "Cihaz bulunamadı");
                    }
                }
            }, 10000);
        }
    }

    // Yeni metod: Doğrudan IP bağlantısı deneme
    private void tryDirectConnection(String ipAddress) {
        updateConnectionStatus(false, "Bağlantı deneniyor: " + ipAddress);

        // IP ve port'u güncelle
        prefsManager.saveDeviceIp(ipAddress);
        networkManager.resetConnection();

        // Veri yenileme dene
        refreshData();
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

        networkManager.getDeviceStatus(new Callback<DeviceStatus>() {
            @Override
            public void onResponse(Call<DeviceStatus> call, Response<DeviceStatus> response) {
                runOnUiThread(() -> {
                    isUpdating = false;
                    swipeRefresh.setRefreshing(false);

                    if (response.isSuccessful() && response.body() != null) {
                        updateUI(response.body());
                        updateConnectionStatus(true);
                        prefsManager.saveLastUpdateTime(System.currentTimeMillis());
                        isConnected = true;
                    } else {
                        handleConnectionError("Veri alınamadı");
                    }
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
        updateConnectionStatus(false, error);

        // Bağlantı moduna göre farklı davran
        int connectionMode = prefsManager.getConnectionMode();

        if (connectionMode == Constants.MODE_AP && !error.contains("192.168.4.1")) {
            // AP modunda yanlış IP kullanılıyor olabilir
            tryDirectConnection(Constants.DEFAULT_AP_IP);
        } else if (!isDiscovering) {
            // Diğer durumlarda discovery başlat
            updateHandler.postDelayed(this::startDeviceDiscovery, 3000); // 5 saniyeden düşürüldü
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
        updateDeviceState(tvMotorStatus, status.isMotorState());
        tvMotorTiming.setText(String.format("Bekleme: %d dk\nÇalışma: %d sn",
                status.getMotorWaitTime(), status.getMotorRunTime()));

        // PID
        updatePidStatus(status);

        // Alarm
        updateAlarmStatus(status.isAlarmEnabled());
    }

    private void updateConnectionStatus(boolean connected) {
        updateConnectionStatus(connected, null);
    }

    private void updateConnectionStatus(boolean connected, String customMessage) {
        String message;
        int color;
        int iconRes;

        if (connected) {
            message = customMessage != null ? customMessage : "Bağlı";
            color = getColor(R.color.success);
            iconRes = R.drawable.ic_wifi_on;
        } else {
            message = customMessage != null ? customMessage : "Bağlantı Yok";
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

        switch (type.toLowerCase()) {
            case "0":
            case "tavuk":
                return "Tavuk";
            case "1":
            case "bıldırcın":
                return "Bıldırcın";
            case "2":
            case "kaz":
                return "Kaz";
            case "3":
            case "manuel":
                return "Manuel";
            default:
                return type;
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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

            // Bağlantıyı test et
            refreshData();

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
            // Ayarlar değiştirildi, verileri yenile
            Log.d(TAG, "Ayarlar güncellendi, veriler yenileniyor...");

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
        if (updateHandler != null && updateRunnable != null) {
            updateHandler.removeCallbacks(updateRunnable);
        }
        if (discoveryManager != null) {
            discoveryManager.stopDiscovery();
        }
    }
}