package com.kulucka.mk.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textview.MaterialTextView;
import com.kulucka.mk.R;
import com.kulucka.mk.models.SystemStatus;
import com.kulucka.mk.services.ApiService;
import com.kulucka.mk.services.BackgroundService;
import com.kulucka.mk.utils.Constants;
import com.kulucka.mk.utils.NetworkUtils;
import com.kulucka.mk.utils.SharedPreferencesManager;

/**
 * Ana ekran aktivitesi - Sistem durumunu görüntüler ve kontrol sağlar
 * Gerçek zamanlı sensör verileri, kuluçka durumu ve kontrol elemanları
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // UI Components
    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton fabSettings;

    // Status Cards
    private CardView cardTemperature;
    private CardView cardHumidity;
    private CardView cardIncubation;
    private CardView cardControl;
    private CardView cardConnection;

    // Temperature Card Views
    private MaterialTextView tvCurrentTemp;
    private MaterialTextView tvTargetTemp;
    private MaterialTextView tvHeaterStatus;

    // Humidity Card Views
    private MaterialTextView tvCurrentHumidity;
    private MaterialTextView tvTargetHumidity;
    private MaterialTextView tvHumidifierStatus;

    // Incubation Card Views
    private MaterialTextView tvCurrentDay;
    private MaterialTextView tvTotalDays;
    private MaterialTextView tvIncubationType;
    private MaterialTextView tvIncubationStatus;
    private MaterialTextView tvCompletionStatus;

    // Control Card Views
    private MaterialTextView tvMotorStatus;
    private MaterialTextView tvMotorSettings;
    private MaterialTextView tvPIDStatus;
    private MaterialTextView tvAlarmStatus;

    // Connection Card Views
    private MaterialTextView tvConnectionStatus;
    private MaterialTextView tvIPAddress;
    private MaterialTextView tvWiFiMode;
    private MaterialTextView tvSignalStrength;
    private MaterialTextView tvUptime;

    // Services and Managers
    private ApiService apiService;
    private SharedPreferencesManager prefsManager;
    private SystemStatus currentSystemStatus;

    // State
    private boolean isDataLoading = false;
    private long lastUpdateTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i(TAG, "MainActivity oluşturuluyor");

        // Initialize services
        initializeServices();

        // Setup UI
        initializeUI();

        // Setup listeners
        setupListeners();

        // Start background service
        startBackgroundService();

        // Load last known data
        loadLastKnownData();

        // Initial data fetch
        refreshData();

        Log.i(TAG, "MainActivity oluşturuldu");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "MainActivity resume");

        // Register broadcast receivers
        registerBroadcastReceivers();

        // Refresh data
        refreshData();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "MainActivity pause");

        // Unregister broadcast receivers
        unregisterBroadcastReceivers();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "MainActivity yok ediliyor");
    }

    /**
     * Services'ları başlatır
     */
    private void initializeServices() {
        apiService = ApiService.getInstance(this);
        prefsManager = SharedPreferencesManager.getInstance(this);
    }

    /**
     * UI bileşenlerini başlatır
     */
    private void initializeUI() {
        // Toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Swipe refresh
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        // FAB
        fabSettings = findViewById(R.id.fabSettings);

        // Status cards
        cardTemperature = findViewById(R.id.cardTemperature);
        cardHumidity = findViewById(R.id.cardHumidity);
        cardIncubation = findViewById(R.id.cardIncubation);
        cardControl = findViewById(R.id.cardControl);
        cardConnection = findViewById(R.id.cardConnection);

        // Temperature views
        tvCurrentTemp = findViewById(R.id.tvCurrentTemp);
        tvTargetTemp = findViewById(R.id.tvTargetTemp);
        tvHeaterStatus = findViewById(R.id.tvHeaterStatus);

        // Humidity views
        tvCurrentHumidity = findViewById(R.id.tvCurrentHumidity);
        tvTargetHumidity = findViewById(R.id.tvTargetHumidity);
        tvHumidifierStatus = findViewById(R.id.tvHumidifierStatus);

        // Incubation views
        tvCurrentDay = findViewById(R.id.tvCurrentDay);
        tvTotalDays = findViewById(R.id.tvTotalDays);
        tvIncubationType = findViewById(R.id.tvIncubationType);
        tvIncubationStatus = findViewById(R.id.tvIncubationStatus);
        tvCompletionStatus = findViewById(R.id.tvCompletionStatus);

        // Control views
        tvMotorStatus = findViewById(R.id.tvMotorStatus);
        tvMotorSettings = findViewById(R.id.tvMotorSettings);
        tvPIDStatus = findViewById(R.id.tvPIDStatus);
        tvAlarmStatus = findViewById(R.id.tvAlarmStatus);

        // Connection views
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        tvIPAddress = findViewById(R.id.tvIPAddress);
        tvWiFiMode = findViewById(R.id.tvWiFiMode);
        tvSignalStrength = findViewById(R.id.tvSignalStrength);
        tvUptime = findViewById(R.id.tvUptime);

        // Set initial values
        setInitialUIValues();
    }

    /**
     * Event listener'ları ayarlar
     */
    private void setupListeners() {
        // Swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(this::refreshData);

        // Settings FAB
        fabSettings.setOnClickListener(v -> openSettingsActivity());

        // Card click listeners for quick actions
        cardTemperature.setOnClickListener(v -> openTemperatureHumiditySettings());
        cardHumidity.setOnClickListener(v -> openTemperatureHumiditySettings());
        cardIncubation.setOnClickListener(v -> openIncubationSettings());
        cardControl.setOnClickListener(v -> openPIDSettings());
        cardConnection.setOnClickListener(v -> openWiFiSettings());
    }

    /**
     * Başlangıç UI değerlerini ayarlar
     */
    private void setInitialUIValues() {
        tvCurrentTemp.setText("--.-°C");
        tvTargetTemp.setText("Hedef: --.-°C");
        tvHeaterStatus.setText("Durum: Bilinmiyor");

        tvCurrentHumidity.setText("--%");
        tvTargetHumidity.setText("Hedef: --%");
        tvHumidifierStatus.setText("Durum: Bilinmiyor");

        tvCurrentDay.setText("-- gün");
        tvTotalDays.setText("/ -- gün");
        tvIncubationType.setText("Tip: Bilinmiyor");
        tvIncubationStatus.setText("Durum: Bilinmiyor");
        tvCompletionStatus.setVisibility(View.GONE);

        tvMotorStatus.setText("Durum: Bilinmiyor");
        tvMotorSettings.setText("Ayarlar: --");
        tvPIDStatus.setText("PID: Bilinmiyor");
        tvAlarmStatus.setText("Alarm: Bilinmiyor");

        tvConnectionStatus.setText("Bağlantı kontrol ediliyor...");
        tvIPAddress.setText("IP: --");
        tvWiFiMode.setText("Mod: --");
        tvSignalStrength.setText("Sinyal: --");
        tvUptime.setText("Çalışma süresi: --");
    }

    /**
     * Arka plan servisini başlatır
     */
    private void startBackgroundService() {
        if (prefsManager.isAutoConnect()) {
            BackgroundService.startService(this);
            Log.i(TAG, "Arka plan servisi başlatıldı");
        }
    }

    /**
     * Son bilinen verileri yükler
     */
    private void loadLastKnownData() {
        SystemStatus lastStatus = prefsManager.getLastSystemStatus();
        if (lastStatus != null) {
            updateUI(lastStatus);
            Log.d(TAG, "Son bilinen veriler yüklendi");
        }
    }

    /**
     * Veriyi yeniler
     */
    private void refreshData() {
        if (isDataLoading) {
            Log.d(TAG, "Veri yenileme zaten devam ediyor");
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(this)) {
            showError("İnternet bağlantısı yok");
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        isDataLoading = true;
        swipeRefreshLayout.setRefreshing(true);

        Log.d(TAG, "Veri yenileme başlatıldı");

        apiService.getSystemStatus(new ApiService.ApiCallback<SystemStatus>() {
            @Override
            public void onSuccess(SystemStatus systemStatus) {
                runOnUiThread(() -> {
                    handleDataSuccess(systemStatus);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    handleDataError(error);
                });
            }
        });
    }

    /**
     * Başarılı veri alımını işler
     */
    private void handleDataSuccess(SystemStatus systemStatus) {
        isDataLoading = false;
        swipeRefreshLayout.setRefreshing(false);
        lastUpdateTime = System.currentTimeMillis();

        currentSystemStatus = systemStatus;
        updateUI(systemStatus);

        // Save to preferences
        prefsManager.saveSystemStatus(systemStatus);

        Log.d(TAG, "Veri başarıyla güncellendi");
    }

    /**
     * Veri alım hatasını işler
     */
    private void handleDataError(String error) {
        isDataLoading = false;
        swipeRefreshLayout.setRefreshing(false);

        showError("Veri güncellenemedi: " + error);
        updateConnectionStatus(false, error);

        Log.w(TAG, "Veri güncelleme hatası: " + error);
    }

    /**
     * UI'yi sistem durumuna göre günceller
     */
    private void updateUI(SystemStatus systemStatus) {
        if (systemStatus == null) return;

        // Temperature card
        updateTemperatureCard(systemStatus);

        // Humidity card
        updateHumidityCard(systemStatus);

        // Incubation card
        updateIncubationCard(systemStatus);

        // Control card
        updateControlCard(systemStatus);

        // Connection card
        updateConnectionCard(systemStatus);

        // Check for alarms
        checkAndShowAlarms(systemStatus);
    }

    /**
     * Sıcaklık kartını günceller
     */
    private void updateTemperatureCard(SystemStatus systemStatus) {
        tvCurrentTemp.setText(systemStatus.getFormattedTemperature());
        tvTargetTemp.setText("Hedef: " + systemStatus.getFormattedTargetTemp());

        String heaterStatus = systemStatus.isHeaterState() ? "AÇIK" : "KAPALI";
        tvHeaterStatus.setText("Isıtıcı: " + heaterStatus);

        // Renk kodlaması
        int tempColor = getTempColor(systemStatus.getTemperature(), systemStatus.getTargetTemp());
        tvCurrentTemp.setTextColor(tempColor);

        int heaterColor = systemStatus.isHeaterState() ?
                getColor(R.color.color_success) : getColor(R.color.color_inactive);
        tvHeaterStatus.setTextColor(heaterColor);
    }

    /**
     * Nem kartını günceller
     */
    private void updateHumidityCard(SystemStatus systemStatus) {
        tvCurrentHumidity.setText(systemStatus.getFormattedHumidity());
        tvTargetHumidity.setText("Hedef: " + systemStatus.getFormattedTargetHumid());

        String humidifierStatus = systemStatus.isHumidifierState() ? "AÇIK" : "KAPALI";
        tvHumidifierStatus.setText("Nemlendirici: " + humidifierStatus);

        // Renk kodlaması
        int humidColor = getHumidityColor(systemStatus.getHumidity(), systemStatus.getTargetHumid());
        tvCurrentHumidity.setTextColor(humidColor);

        int humidifierColor = systemStatus.isHumidifierState() ?
                getColor(R.color.color_success) : getColor(R.color.color_inactive);
        tvHumidifierStatus.setTextColor(humidifierColor);
    }

    /**
     * Kuluçka kartını günceller
     */
    private void updateIncubationCard(SystemStatus systemStatus) {
        tvCurrentDay.setText(String.valueOf(systemStatus.getDisplayDay()));
        tvTotalDays.setText("/ " + systemStatus.getTotalDays() + " gün");
        tvIncubationType.setText("Tip: " + systemStatus.getIncubationType());

        String incubationStatus = systemStatus.isIncubationRunning() ? "ÇALIŞIYOR" : "DURDURULDU";
        tvIncubationStatus.setText("Durum: " + incubationStatus);

        int statusColor = systemStatus.isIncubationRunning() ?
                getColor(R.color.color_success) : getColor(R.color.color_warning);
        tvIncubationStatus.setTextColor(statusColor);

        // Tamamlanma durumu
        if (systemStatus.isIncubationCompleted()) {
            tvCompletionStatus.setVisibility(View.VISIBLE);
            tvCompletionStatus.setText("Kuluçka Tamamlandı - Çıkım Devam Ediyor (Gerçek Gün: " +
                    systemStatus.getActualDay() + ")");
            tvCompletionStatus.setTextColor(getColor(R.color.color_warning));
        } else {
            tvCompletionStatus.setVisibility(View.GONE);
        }
    }

    /**
     * Kontrol kartını günceller
     */
    private void updateControlCard(SystemStatus systemStatus) {
        String motorStatus = systemStatus.isMotorState() ? "ÇALIŞIYOR" : "BEKLEMEDE";
        tvMotorStatus.setText("Motor: " + motorStatus);

        tvMotorSettings.setText("Ayarlar: " + systemStatus.getFormattedMotorWaitTime() +
                " / " + systemStatus.getFormattedMotorRunTime());

        tvPIDStatus.setText("PID: " + systemStatus.getPidModeString());

        String alarmStatus = systemStatus.isAlarmEnabled() ? "AÇIK" : "KAPALI";
        tvAlarmStatus.setText("Alarm: " + alarmStatus);

        // Renk kodlaması
        int motorColor = systemStatus.isMotorState() ?
                getColor(R.color.color_success) : getColor(R.color.color_inactive);
        tvMotorStatus.setTextColor(motorColor);

        int alarmColor = systemStatus.isAlarmEnabled() ?
                getColor(R.color.color_success) : getColor(R.color.color_inactive);
        tvAlarmStatus.setTextColor(alarmColor);
    }

    /**
     * Bağlantı kartını günceller
     */
    private void updateConnectionCard(SystemStatus systemStatus) {
        tvConnectionStatus.setText(systemStatus.getWifiStatus());
        tvIPAddress.setText("IP: " + systemStatus.getIpAddress());
        tvWiFiMode.setText("Mod: " + systemStatus.getWifiMode());

        if (systemStatus.isStationMode()) {
            String signalText = "Sinyal: " + systemStatus.getSignalStrength() + " dBm";
            tvSignalStrength.setText(signalText);
            tvSignalStrength.setVisibility(View.VISIBLE);
        } else {
            tvSignalStrength.setVisibility(View.GONE);
        }

        tvUptime.setText("Çalışma süresi: " + systemStatus.getFormattedUptime());

        // Bağlantı durumu rengi
        int connectionColor = systemStatus.isConnected() ?
                getColor(R.color.color_success) : getColor(R.color.color_error);
        tvConnectionStatus.setTextColor(connectionColor);
    }

    /**
     * Bağlantı durumunu günceller
     */
    private void updateConnectionStatus(boolean connected, String message) {
        tvConnectionStatus.setText(message);
        int color = connected ? getColor(R.color.color_success) : getColor(R.color.color_error);
        tvConnectionStatus.setTextColor(color);
    }

    /**
     * Alarm kontrolü ve gösterimi
     */
    private void checkAndShowAlarms(SystemStatus systemStatus) {
        if (systemStatus.hasAlarmCondition()) {
            String alarmMessage = systemStatus.getAlarmMessage();
            showAlarmDialog(alarmMessage);
        }
    }

    /**
     * Sıcaklık rengini hesaplar
     */
    private int getTempColor(double current, double target) {
        double diff = Math.abs(current - target);
        if (diff <= 0.5) return getColor(R.color.color_success);
        if (diff <= 1.0) return getColor(R.color.color_warning);
        return getColor(R.color.color_error);
    }

    /**
     * Nem rengini hesaplar
     */
    private int getHumidityColor(double current, double target) {
        double diff = Math.abs(current - target);
        if (diff <= 3.0) return getColor(R.color.color_success);
        if (diff <= 6.0) return getColor(R.color.color_warning);
        return getColor(R.color.color_error);
    }

    /**
     * Broadcast receiver'ları kaydeder
     */
    private void registerBroadcastReceivers() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_UPDATE_DATA);
        filter.addAction(Constants.ACTION_CONNECTION_CHANGED);
        filter.addAction(Constants.ACTION_ALARM_TRIGGERED);

        lbm.registerReceiver(broadcastReceiver, filter);
    }

    /**
     * Broadcast receiver'ları kaldırır
     */
    private void unregisterBroadcastReceivers() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    /**
     * Broadcast receiver
     */
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            switch (action) {
                case Constants.ACTION_UPDATE_DATA:
                    SystemStatus systemStatus = (SystemStatus) intent.getSerializableExtra(Constants.EXTRA_SYSTEM_STATUS);
                    if (systemStatus != null) {
                        currentSystemStatus = systemStatus;
                        updateUI(systemStatus);
                    }
                    break;

                case Constants.ACTION_CONNECTION_CHANGED:
                    boolean connected = intent.getBooleanExtra(Constants.EXTRA_CONNECTION_STATUS, false);
                    String error = intent.getStringExtra(Constants.EXTRA_ERROR_MESSAGE);
                    updateConnectionStatus(connected, error != null ? error : "Bağlantı durumu değişti");
                    break;

                case Constants.ACTION_ALARM_TRIGGERED:
                    String alarmMessage = intent.getStringExtra(Constants.EXTRA_ERROR_MESSAGE);
                    if (alarmMessage != null) {
                        showAlarmDialog(alarmMessage);
                    }
                    break;
            }
        }
    };

    // === Menu ===

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            refreshData();
            return true;
        } else if (id == R.id.action_settings) {
            openSettingsActivity();
            return true;
        } else if (id == R.id.action_wifi) {
            openWiFiSettings();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // === Navigation Methods ===

    private void openSettingsActivity() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void openWiFiSettings() {
        Intent intent = new Intent(this, WiFiSettingsActivity.class);
        startActivity(intent);
    }

    private void openTemperatureHumiditySettings() {
        Intent intent = new Intent(this, TemperatureHumidityActivity.class);
        startActivity(intent);
    }

    private void openIncubationSettings() {
        Intent intent = new Intent(this, IncubationSettingsActivity.class);
        startActivity(intent);
    }

    private void openPIDSettings() {
        Intent intent = new Intent(this, PIDSettingsActivity.class);
        startActivity(intent);
    }

    // === Helper Methods ===

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showAlarmDialog(String message) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🚨 KULUCKA ALARMI")
                .setMessage(message)
                .setPositiveButton("Tamam", null)
                .setIcon(R.drawable.ic_alarm)
                .show();
    }

    /**
     * Debug amaçlı sistem bilgilerini loglar
     */
    private void logSystemInfo() {
        Log.d(TAG, "=== System Info ===");
        if (currentSystemStatus != null) {
            Log.d(TAG, currentSystemStatus.toString());
        }
        NetworkUtils.logNetworkInfo(this);
        Log.d(TAG, "Last update: " + lastUpdateTime);
        Log.d(TAG, "==================");
    }
}