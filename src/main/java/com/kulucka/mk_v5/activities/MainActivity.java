package com.kulucka.mk_v5.activities;

import android.app.AlertDialog;
import android.content.Intent;
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

    // Service components
    private Handler dataRefreshHandler;
    private Runnable dataRefreshRunnable;
    private static final int REFRESH_INTERVAL = 5000; // 5 saniye

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupNavigationDrawer();
        setupDataRefresh();
        initializeApiService();
        setupObservers();

        // İlk fragment'i yükle
        if (savedInstanceState == null) {
            loadFragment(new DashboardFragment());
            navigationView.setCheckedItem(R.id.nav_dashboard);
        }

        // ESP32 IP adresini kontrol et
        checkEsp32IpAddress();
    }

    private void initializeViews() {
        // Mevcut layout'unuzda var olan elementleri bul
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigationView);

        // Connection status elementleri (varsa)
        tvConnectionStatus = findViewById(R.id.tv_connection_status);
        ivConnectionIcon = findViewById(R.id.iv_connection_icon);

        // Alarm paneli elementleri (varsa)
        btnDismissAlarms = findViewById(R.id.btn_dismiss_alarms);
        alarmPanel = findViewById(R.id.alarm_panel);
        tvAlarmMessage = findViewById(R.id.tv_alarm_message);
        btnDismissTemperatureAlarm = findViewById(R.id.btn_dismiss_temperature_alarm);
        btnDismissHumidityAlarm = findViewById(R.id.btn_dismiss_humidity_alarm);

        // Alarm butonları click listener'ları (sadece varsa)
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

        // Hamburger menu için (eğer btn_menu varsa)
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

    private void setupDataRefresh() {
        dataRefreshHandler = new Handler();
        dataRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                refreshData();
                dataRefreshHandler.postDelayed(this, REFRESH_INTERVAL);
            }
        };
    }

    private void initializeApiService() {
        // ApiService'i initialize et
        ApiService.getInstance().initialize(this);

        // Kaydedilmiş IP adresini yükle
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String savedIp = prefs.getString(KEY_ESP32_IP, "192.168.4.1");
        ApiService.getInstance().setBaseUrl("http://" + savedIp);

        Log.d(TAG, "ApiService initialized with IP: " + savedIp);
    }

    private void setupObservers() {
        // Bu method'u boş bırakıyoruz çünkü yeni ApiService'te LiveData yok
        // Bunun yerine periyodik refresh kullanıyoruz
    }

    private void checkEsp32IpAddress() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String savedIp = prefs.getString(KEY_ESP32_IP, "");

        if (savedIp.isEmpty()) {
            showIpConfigDialog();
        } else {
            // Bağlantıyı test et
            testConnection();
        }
    }

    private void showIpConfigDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("ESP32 IP Adresi")
                .setMessage("ESP32 cihazınızın IP adresini ayarlamak için WiFi Ayarları sayfasına gidin.")
                .setPositiveButton("WiFi Ayarları", (dialog, which) -> {
                    loadFragment(new WiFiSettingsFragment());
                    navigationView.setCheckedItem(R.id.nav_wifi_settings);
                })
                .setNegativeButton("Daha Sonra", null)
                .show();
    }

    private void testConnection() {
        ApiService.getInstance().testConnection(new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    updateConnectionStatus(true);
                    showToast("ESP32 bağlantısı başarılı");
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

    private void refreshData() {
        ApiService.getInstance().getSensorData(new ApiService.DataCallback() {
            @Override
            public void onSuccess(JSONObject data) {
                runOnUiThread(() -> {
                    updateConnectionStatus(true);
                    processReceivedData(data);
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    updateConnectionStatus(false);
                    Log.w(TAG, "Veri alınırken hata: " + message);
                });
            }
        });
    }

    private void processReceivedData(JSONObject data) {
        try {
            // Alarm durumlarını kontrol et
            boolean tempAlarm = data.optBoolean("tempAlarm", false);
            boolean humidityAlarm = data.optBoolean("humidityAlarm", false);

            updateAlarmStatus(tempAlarm, humidityAlarm);

            // Bu veriyi aktif fragment'a ilet (eğer dashboard ise)
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (currentFragment instanceof DashboardFragment) {
                ((DashboardFragment) currentFragment).updateData(data);
            }

        } catch (Exception e) {
            Log.e(TAG, "Veri işleme hatası: " + e.getMessage());
        }
    }

    private void updateConnectionStatus(boolean connected) {
        if (tvConnectionStatus != null) {
            tvConnectionStatus.setText(connected ? "Bağlı" : "Bağlantı Yok");
            tvConnectionStatus.setTextColor(getResources().getColor(
                    connected ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
        }

        if (ivConnectionIcon != null) {
            // Basit icon'lar kullan veya text color ile göster
            ivConnectionIcon.setImageResource(connected ?
                    android.R.drawable.presence_online : android.R.drawable.presence_busy);
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

            // Spesifik alarm butonlarının görünürlüğü
            if (btnDismissTemperatureAlarm != null) {
                btnDismissTemperatureAlarm.setVisibility(tempAlarm ? View.VISIBLE : View.GONE);
            }
            if (btnDismissHumidityAlarm != null) {
                btnDismissHumidityAlarm.setVisibility(humidityAlarm ? View.VISIBLE : View.GONE);
            }
        }
    }

    // Alarm kapatma metodları
    private void dismissAllAlarms() {
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
        ApiService.getInstance().dismissAlarm("temperature", new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    showToast("Sıcaklık alarmı kapatıldı");
                    if (btnDismissTemperatureAlarm != null) {
                        btnDismissTemperatureAlarm.setVisibility(View.GONE);
                    }
                    // Sadece nem alarmı kaldıysa panel görünür kalsın
                    refreshData(); // Durumu güncellemek için
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
        ApiService.getInstance().dismissAlarm("humidity", new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    showToast("Nem alarmı kapatıldı");
                    if (btnDismissHumidityAlarm != null) {
                        btnDismissHumidityAlarm.setVisibility(View.GONE);
                    }
                    // Sadece sıcaklık alarmı kaldıysa panel görünür kalsın
                    refreshData(); // Durumu güncellemek için
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

        // Menu ID'leri ile fragment seçimi
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
        // Veri yenilemeyi başlat
        if (dataRefreshHandler != null && dataRefreshRunnable != null) {
            dataRefreshHandler.post(dataRefreshRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Veri yenilemeyi durdur
        if (dataRefreshHandler != null && dataRefreshRunnable != null) {
            dataRefreshHandler.removeCallbacks(dataRefreshRunnable);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cleanup
        if (dataRefreshHandler != null && dataRefreshRunnable != null) {
            dataRefreshHandler.removeCallbacks(dataRefreshRunnable);
        }

        // API requests'leri iptal et
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

    // ESP32'den gelen parametre güncellemelerini işlemek için gerekli fonksiyon
    // WiFiManager.cpp'de _processParameterUpdate fonksiyonu tarafından çağrılır
    public void handleWifiParameterUpdate(String param, String value) {
        Log.d(TAG, "WiFi parameter update received: " + param + " = " + value);

        runOnUiThread(() -> {
            try {
                // Parametre türüne göre işleme
                switch (param) {
                    case "targetTemp":
                        updateTargetTemperature(Float.parseFloat(value));
                        break;
                    case "targetHumid":
                        updateTargetHumidity(Float.parseFloat(value));
                        break;
                    case "pidKp":
                    case "pidKi":
                    case "pidKd":
                        updatePidParameter(param, Float.parseFloat(value));
                        break;
                    case "motorWaitTime":
                    case "motorRunTime":
                        updateMotorParameter(param, Long.parseLong(value));
                        break;
                    case "tempLowAlarm":
                    case "tempHighAlarm":
                    case "humidLowAlarm":
                    case "humidHighAlarm":
                        updateAlarmParameter(param, Float.parseFloat(value));
                        break;
                    case "tempCalibration1":
                    case "tempCalibration2":
                    case "humidCalibration1":
                    case "humidCalibration2":
                        updateCalibrationParameter(param, Float.parseFloat(value));
                        break;
                    case "incubationType":
                    case "isIncubationRunning":
                    case "manualDevTemp":
                    case "manualHatchTemp":
                    case "manualDevHumid":
                    case "manualHatchHumid":
                    case "manualDevDays":
                    case "manualHatchDays":
                        updateIncubationParameter(param, value);
                        break;
                    default:
                        Log.w(TAG, "Unknown parameter: " + param);
                        break;
                }

                // Aktif fragment'i güncelle
                refreshCurrentFragment();

            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid parameter value: " + param + " = " + value, e);
            } catch (Exception e) {
                Log.e(TAG, "Error processing parameter update: " + param, e);
            }
        });
    }

    private void updateTargetTemperature(float temperature) {
        // SharedPrefs'e kaydet
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit().putFloat("target_temperature", temperature).apply();

        showToast("Hedef sıcaklık güncellendi: " + temperature + "°C");
    }

    private void updateTargetHumidity(float humidity) {
        // SharedPrefs'e kaydet
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit().putFloat("target_humidity", humidity).apply();

        showToast("Hedef nem güncellendi: " + humidity + "%");
    }

    private void updatePidParameter(String param, float value) {
        // SharedPrefs'e kaydet
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit().putFloat(param, value).apply();

        showToast("PID parametresi güncellendi: " + param + " = " + value);
    }

    private void updateMotorParameter(String param, long value) {
        // SharedPrefs'e kaydet
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit().putLong(param, value).apply();

        showToast("Motor parametresi güncellendi: " + param + " = " + value);
    }

    private void updateAlarmParameter(String param, float value) {
        // SharedPrefs'e kaydet
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit().putFloat(param, value).apply();

        showToast("Alarm parametresi güncellendi: " + param + " = " + value);
    }

    private void updateCalibrationParameter(String param, float value) {
        // SharedPrefs'e kaydet
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit().putFloat(param, value).apply();

        showToast("Kalibrasyon parametresi güncellendi: " + param + " = " + value);
    }

    private void updateIncubationParameter(String param, String value) {
        // SharedPrefs'e kaydet
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        try {
            if (param.equals("isIncubationRunning")) {
                prefs.edit().putBoolean(param, "1".equals(value) || "true".equals(value.toLowerCase())).apply();
            } else if (param.equals("incubationType")) {
                prefs.edit().putInt(param, Integer.parseInt(value)).apply();
            } else if (param.contains("Temp")) {
                prefs.edit().putFloat(param, Float.parseFloat(value)).apply();
            } else if (param.contains("Humid") || param.contains("Days")) {
                prefs.edit().putInt(param, Integer.parseInt(value)).apply();
            } else {
                prefs.edit().putString(param, value).apply();
            }

            showToast("Kuluçka parametresi güncellendi: " + param);
        } catch (Exception e) {
            Log.e(TAG, "Error updating incubation parameter: " + param, e);
        }
    }

    private void refreshCurrentFragment() {
        // Aktif fragment'i yenile
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment != null) {
            // Fragment'in kendi refresh metodunu çağır
            if (currentFragment instanceof DashboardFragment) {
                refreshData(); // Dashboard için veri yenile
            }
            // Diğer fragment'lar için onResume'u tetikle
            currentFragment.onResume();
        }
    }

    // Utility methods
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // ESP32 IP adresini kaydetme (WiFiSettingsFragment'dan çağrılacak)
    public void saveEsp32IpAddress(String ipAddress) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit().putString(KEY_ESP32_IP, ipAddress).apply();

        // ApiService'te IP'yi güncelle
        ApiService.getInstance().setBaseUrl("http://" + ipAddress);

        // Bağlantıyı test et
        testConnection();

        Log.d(TAG, "ESP32 IP adresi kaydedildi: " + ipAddress);
    }

    // Fragment'lar tarafından çağrılabilecek metodlar
    public void refreshDataNow() {
        refreshData();
    }

    public void updateIpAddress(String newIp) {
        saveEsp32IpAddress(newIp);
    }
}