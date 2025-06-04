package com.kulucka.mk_v5.activities;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;
import com.kulucka.mk_v5.R;
import com.kulucka.mk_v5.fragments.AlarmSettingsFragment;
import com.kulucka.mk_v5.fragments.DashboardFragment;
import com.kulucka.mk_v5.fragments.IncubationSettingsFragment;
import com.kulucka.mk_v5.fragments.PidSettingsFragment;
import com.kulucka.mk_v5.fragments.WiFiSettingsFragment;
import com.kulucka.mk_v5.fragments.MotorSettingsFragment;
import com.kulucka.mk_v5.fragments.CalibrationFragment;
import com.kulucka.mk_v5.services.ApiService;
import com.kulucka.mk_v5.services.DataRefreshService;
import com.kulucka.mk_v5.services.ESP32ConnectionService;
import com.kulucka.mk_v5.utils.NetworkUtils;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";
    private static final String PREF_NAME = "KuluckaPrefs";
    private static final String KEY_ESP32_IP = "esp32_ip";

    // UI Components
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView tvConnectionStatus;
    private ImageView ivConnectionIcon;
    private Button btnDismissAlarms;
    private View alarmPanel;
    private TextView tvAlarmMessage;
    private Button btnDismissTemperatureAlarm;
    private Button btnDismissHumidityAlarm;

    // Broadcast receiver
    private BroadcastReceiver dataUpdateReceiver;

    // Connection status tracking
    private boolean lastConnectionStatus = false;
    private long lastConnectionCheckTime = 0;
    private static final long CONNECTION_CHECK_INTERVAL = 5000; // 5 saniye

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupNavigationDrawer();
        setupBroadcastReceiver();
        checkServices();

        // İlk fragment'i yükle
        if (savedInstanceState == null) {
            loadFragment(new DashboardFragment());
            navigationView.setCheckedItem(R.id.nav_dashboard);
        }

        // ESP32 IP adresini kontrol et
        checkEsp32Connection();

        // Intent'ten alarm kontrolü
        handleAlarmIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleAlarmIntent(intent);
    }

    private void handleAlarmIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("alarm", false)) {
            boolean tempAlarm = intent.getBooleanExtra("tempAlarm", false);
            boolean humidityAlarm = intent.getBooleanExtra("humidityAlarm", false);
            updateAlarmStatus(tempAlarm, humidityAlarm);
        }
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigationView);
        tvConnectionStatus = findViewById(R.id.tv_connection_status);
        ivConnectionIcon = findViewById(R.id.iv_connection_icon);
        btnDismissAlarms = findViewById(R.id.btn_dismiss_alarms);
        alarmPanel = findViewById(R.id.alarm_panel);
        tvAlarmMessage = findViewById(R.id.tv_alarm_message);
        btnDismissTemperatureAlarm = findViewById(R.id.btn_dismiss_temperature_alarm);
        btnDismissHumidityAlarm = findViewById(R.id.btn_dismiss_humidity_alarm);

        // Alarm butonları click listener'ları
        if (btnDismissAlarms != null) {
            btnDismissAlarms.setOnClickListener(v -> dismissAllAlarms());
        }
        if (btnDismissTemperatureAlarm != null) {
            btnDismissTemperatureAlarm.setOnClickListener(v -> dismissTemperatureAlarm());
        }
        if (btnDismissHumidityAlarm != null) {
            btnDismissHumidityAlarm.setOnClickListener(v -> dismissHumidityAlarm());
        }
    }

    private void setupNavigationDrawer() {
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
        }

        View menuButton = findViewById(R.id.btn_menu);
        if (menuButton != null && drawerLayout != null) {
            menuButton.setOnClickListener(v -> {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }
    }

    private void setupBroadcastReceiver() {
        dataUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.kulucka.mk_v5.DATA_UPDATE".equals(intent.getAction())) {
                    String dataStr = intent.getStringExtra("data");
                    if (dataStr != null) {
                        try {
                            JSONObject data = new JSONObject(dataStr);
                            processReceivedData(data);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing broadcast data: " + e.getMessage());
                        }
                    }
                }
            }
        };
    }

    private void checkServices() {
        // ESP32 bağlantı servisi kontrolü
        Intent connectionIntent = new Intent(this, ESP32ConnectionService.class);
        startService(connectionIntent);

        // Veri yenileme servisi kontrolü
        Intent dataIntent = new Intent(this, DataRefreshService.class);
        startService(dataIntent);
    }

    private void checkEsp32Connection() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String savedIp = prefs.getString(KEY_ESP32_IP, "");

        if (savedIp.isEmpty()) {
            // ESP32 AP'sine bağlanmayı dene
            if (NetworkUtils.getInstance(this).isConnectedToESP32AP()) {
                savedIp = "192.168.4.1";
                saveEsp32IpAddress(savedIp);
            } else {
                showIpConfigDialog();
            }
        } else {
            ApiService.getInstance().setBaseUrl("http://" + savedIp);
            testConnection();
        }
    }

    private void showIpConfigDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("ESP32 Bağlantısı")
                .setMessage("ESP32'ye bağlanmak için:\n\n" +
                        "1. WiFi ayarlarına gidin\n" +
                        "2. 'KULUCKA_MK_v5' ağına bağlanın\n" +
                        "3. Şifre: 12345678\n\n" +
                        "veya ESP32'yi ev ağınıza bağlayın.")
                .setPositiveButton("WiFi Ayarları", (dialog, which) -> {
                    loadFragment(new WiFiSettingsFragment());
                    navigationView.setCheckedItem(R.id.nav_wifi_settings);
                })
                .setNegativeButton("Daha Sonra", null)
                .show();
    }

    private void testConnection() {
        // Sadece belirli aralıklarla bağlantı testi yap
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastConnectionCheckTime < CONNECTION_CHECK_INTERVAL) {
            return;
        }
        lastConnectionCheckTime = currentTime;

        // ESP32'nin yeni /api/status endpoint'ini kullan
        ApiService.getInstance().testConnection(new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    updateConnectionStatus(true);
                    Log.d(TAG, "ESP32 bağlantısı başarılı");
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    updateConnectionStatus(false);
                    Log.w(TAG, "ESP32 bağlantı hatası: " + message);
                });
            }
        });
    }

    private void processReceivedData(JSONObject data) {
        try {
            updateConnectionStatus(true);

            // Bu veriyi aktif fragment'a ilet
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (currentFragment instanceof DashboardFragment) {
                ((DashboardFragment) currentFragment).updateData(data);
            }

            // Alarm durumunu kontrol et - ESP32'nin yeni alarm sistemine uygun
            checkAndUpdateAlarmStatus(data);

        } catch (Exception e) {
            Log.e(TAG, "Veri işleme hatası: " + e.getMessage());
        }
    }

    private void checkAndUpdateAlarmStatus(JSONObject data) {
        try {
            // ESP32'nin createAppData fonksiyonundan gelen alarm verileri
            boolean alarmEnabled = data.optBoolean("alarmEnabled", false);

            if (alarmEnabled) {
                double currentTemp = data.optDouble("temperature", 0.0);
                double currentHumidity = data.optDouble("humidity", 0.0);
                double targetTemp = data.optDouble("targetTemp", 0.0);
                double targetHumid = data.optDouble("targetHumid", 0.0);

                double tempLowAlarm = data.optDouble("tempLowAlarm", 1.0);
                double tempHighAlarm = data.optDouble("tempHighAlarm", 1.0);
                double humidLowAlarm = data.optDouble("humidLowAlarm", 10.0);
                double humidHighAlarm = data.optDouble("humidHighAlarm", 10.0);

                // Alarm kontrolü - hedef değerlerden sapma bazında
                boolean tempAlarm = (currentTemp < targetTemp - tempLowAlarm) ||
                        (currentTemp > targetTemp + tempHighAlarm);
                boolean humidityAlarm = (currentHumidity < targetHumid - humidLowAlarm) ||
                        (currentHumidity > targetHumid + humidHighAlarm);

                updateAlarmStatus(tempAlarm, humidityAlarm);
            } else {
                updateAlarmStatus(false, false);
            }
        } catch (Exception e) {
            Log.e(TAG, "Alarm durumu kontrol hatası: " + e.getMessage());
        }
    }

    /**
     * Bağlantı durumunu günceller - sadece durum değiştiğinde UI'yi günceller
     */
    public void updateConnectionStatus(boolean connected) {
        // Sadece durum değişirse güncelle (gereksiz UI güncellemelerini önle)
        if (lastConnectionStatus != connected) {
            lastConnectionStatus = connected;

            if (tvConnectionStatus != null) {
                tvConnectionStatus.setText(connected ? "Bağlı" : "Bağlantı Yok");
                tvConnectionStatus.setTextColor(getResources().getColor(
                        connected ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
            }

            if (ivConnectionIcon != null) {
                ivConnectionIcon.setImageResource(connected ?
                        android.R.drawable.presence_online : android.R.drawable.presence_busy);
            }

            Log.d(TAG, "Connection status updated: " + (connected ? "Connected" : "Disconnected"));
        }
    }

    private void updateAlarmStatus(boolean tempAlarm, boolean humidityAlarm) {
        boolean anyAlarm = tempAlarm || humidityAlarm;

        if (alarmPanel != null) {
            alarmPanel.setVisibility(anyAlarm ? View.VISIBLE : View.GONE);
        }

        if (anyAlarm) {
            String alarmMessage = "";
            if (tempAlarm && humidityAlarm) {
                alarmMessage = "Sıcaklık ve Nem Alarmı Aktif!";
            } else if (tempAlarm) {
                alarmMessage = "Sıcaklık Alarmı Aktif!";
            } else if (humidityAlarm) {
                alarmMessage = "Nem Alarmı Aktif!";
            }

            if (tvAlarmMessage != null) {
                tvAlarmMessage.setText(alarmMessage);
            }

            if (btnDismissTemperatureAlarm != null) {
                btnDismissTemperatureAlarm.setVisibility(tempAlarm ? View.VISIBLE : View.GONE);
            }
            if (btnDismissHumidityAlarm != null) {
                btnDismissHumidityAlarm.setVisibility(humidityAlarm ? View.VISIBLE : View.GONE);
            }
        }
    }

    private void dismissAllAlarms() {
        // ESP32'nin yeni /api/alarm endpoint'ini kullan
        ApiService.getInstance().dismissAlarm("all", new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    showToast("Tüm alarmlar kapatıldı");
                    hideAlarmPanel();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    showToast("Alarmlar kapatılamadı: " + message);
                });
            }
        });
    }

    private void dismissTemperatureAlarm() {
        // ESP32'nin yeni /api/alarm endpoint'ini kullan
        ApiService.getInstance().dismissAlarm("temperature", new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    showToast("Sıcaklık alarmı kapatıldı");
                    if (btnDismissTemperatureAlarm != null) {
                        btnDismissTemperatureAlarm.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    showToast("Sıcaklık alarmı kapatılamadı: " + message);
                });
            }
        });
    }

    private void dismissHumidityAlarm() {
        // ESP32'nin yeni /api/alarm endpoint'ini kullan
        ApiService.getInstance().dismissAlarm("humidity", new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    showToast("Nem alarmı kapatıldı");
                    if (btnDismissHumidityAlarm != null) {
                        btnDismissHumidityAlarm.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    showToast("Nem alarmı kapatılamadı: " + message);
                });
            }
        });
    }

    private void hideAlarmPanel() {
        if (alarmPanel != null) {
            alarmPanel.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;
        int itemId = item.getItemId();

        if (itemId == R.id.nav_dashboard) {
            fragment = new DashboardFragment();
        } else if (itemId == R.id.nav_incubation_settings) {
            fragment = new IncubationSettingsFragment();
        } else if (itemId == R.id.nav_pid_settings) {
            fragment = new PidSettingsFragment();
        } else if (itemId == R.id.nav_alarm_settings) {
            fragment = new AlarmSettingsFragment();
        } else if (itemId == R.id.nav_wifi_settings) {
            fragment = new WiFiSettingsFragment();
        } else if (itemId == R.id.nav_motor_settings) {
            fragment = new MotorSettingsFragment();
        } else if (itemId == R.id.nav_calibration) {
            fragment = new CalibrationFragment();
        }

        if (fragment != null) {
            loadFragment(fragment);
        }

        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Broadcast receiver'ı kaydet
        IntentFilter filter = new IntentFilter("com.kulucka.mk_v5.DATA_UPDATE");
        registerReceiver(dataUpdateReceiver, filter);

        // Sadece uzun süre geçmişse bağlantı testi yap
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastConnectionCheckTime > CONNECTION_CHECK_INTERVAL) {
            testConnection();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Broadcast receiver'ı kaldır
        try {
            unregisterReceiver(dataUpdateReceiver);
        } catch (Exception e) {
            Log.w(TAG, "Receiver already unregistered");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        ApiService.getInstance().cancelAllRequests();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    public void saveEsp32IpAddress(String ipAddress) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit().putString(KEY_ESP32_IP, ipAddress).apply();

        ApiService.getInstance().setBaseUrl("http://" + ipAddress);

        // IP değiştiğinde bağlantı kontrol zamanını sıfırla
        lastConnectionCheckTime = 0;
        testConnection();

        Log.d(TAG, "ESP32 IP adresi kaydedildi: " + ipAddress);
    }

    public void refreshDataNow() {
        testConnection();
    }

    public void updateIpAddress(String newIp) {
        saveEsp32IpAddress(newIp);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}