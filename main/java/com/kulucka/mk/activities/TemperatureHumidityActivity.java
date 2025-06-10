package com.kulucka.mk.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

import com.kulucka.mk.R;
import com.kulucka.mk.models.ApiResponse;
import com.kulucka.mk.models.SystemStatus;
import com.kulucka.mk.services.ApiService;
import com.kulucka.mk.utils.Constants;
import com.kulucka.mk.utils.SharedPreferencesManager;
import com.kulucka.mk.utils.Utils;

/**
 * Sıcaklık ve nem hedef değerlerini ayarlama aktivitesi
 */
public class TemperatureHumidityActivity extends AppCompatActivity {

    private static final String TAG = "TempHumidActivity";

    // UI Components
    private Toolbar toolbar;

    // Current Values Display
    private MaterialTextView tvCurrentTemp;
    private MaterialTextView tvCurrentHumidity;
    private MaterialTextView tvTempStatus;
    private MaterialTextView tvHumidityStatus;

    // Temperature Controls
    private TextInputLayout tilTargetTemp;
    private TextInputEditText etTargetTemp;
    private Slider sliderTargetTemp;
    private MaterialTextView tvTempRange;

    // Humidity Controls
    private TextInputLayout tilTargetHumidity;
    private TextInputEditText etTargetHumidity;
    private Slider sliderTargetHumidity;
    private MaterialTextView tvHumidityRange;

    // Action Buttons
    private MaterialButton btnApplyTemp;
    private MaterialButton btnApplyHumidity;
    private MaterialButton btnApplyAll;
    private MaterialButton btnResetToDefaults;

    // Services and Managers
    private ApiService apiService;
    private SharedPreferencesManager prefsManager;

    // Current Values
    private SystemStatus currentSystemStatus;
    private double currentTargetTemp = Constants.DEFAULT_TARGET_TEMP;
    private double currentTargetHumidity = Constants.DEFAULT_TARGET_HUMIDITY;

    // State
    private boolean isUpdating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temperature_humidity);

        Log.i(TAG, "TemperatureHumidityActivity oluşturuluyor");

        // Initialize services
        apiService = ApiService.getInstance(this);
        prefsManager = SharedPreferencesManager.getInstance(this);

        // Setup UI
        initializeUI();
        setupToolbar();
        setupSliders();
        setupTextWatchers();
        setupClickListeners();

        // Load current values
        loadCurrentValues();

        // Refresh data
        refreshSystemStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerBroadcastReceivers();
        refreshSystemStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterBroadcastReceivers();
    }

    /**
     * UI bileşenlerini başlatır
     */
    private void initializeUI() {
        toolbar = findViewById(R.id.toolbar);

        // Current values
        tvCurrentTemp = findViewById(R.id.tvCurrentTemp);
        tvCurrentHumidity = findViewById(R.id.tvCurrentHumidity);
        tvTempStatus = findViewById(R.id.tvTempStatus);
        tvHumidityStatus = findViewById(R.id.tvHumidityStatus);

        // Temperature controls
        tilTargetTemp = findViewById(R.id.tilTargetTemp);
        etTargetTemp = findViewById(R.id.etTargetTemp);
        sliderTargetTemp = findViewById(R.id.sliderTargetTemp);
        tvTempRange = findViewById(R.id.tvTempRange);

        // Humidity controls
        tilTargetHumidity = findViewById(R.id.tilTargetHumidity);
        etTargetHumidity = findViewById(R.id.etTargetHumidity);
        sliderTargetHumidity = findViewById(R.id.sliderTargetHumidity);
        tvHumidityRange = findViewById(R.id.tvHumidityRange);

        // Buttons
        btnApplyTemp = findViewById(R.id.btnApplyTemp);
        btnApplyHumidity = findViewById(R.id.btnApplyHumidity);
        btnApplyAll = findViewById(R.id.btnApplyAll);
        btnResetToDefaults = findViewById(R.id.btnResetToDefaults);

        // Set range info
        tvTempRange.setText(getString(R.string.temp_range_info));
        tvHumidityRange.setText(getString(R.string.humidity_range_info));
    }

    /**
     * Toolbar'ı ayarlar
     */
    private void setupToolbar() {
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.temp_humid_settings_title);
        }
    }

    /**
     * Slider'ları ayarlar
     */
    private void setupSliders() {
        // Temperature slider
        sliderTargetTemp.setValueFrom((float) Constants.MIN_TEMPERATURE);
        sliderTargetTemp.setValueTo((float) Constants.MAX_TEMPERATURE);
        sliderTargetTemp.setStepSize(0.1f);
        sliderTargetTemp.setValue((float) currentTargetTemp);

        sliderTargetTemp.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser && !isUpdating) {
                isUpdating = true;
                etTargetTemp.setText(String.format("%.1f", value));
                currentTargetTemp = value;
                updateButtonStates();
                isUpdating = false;
            }
        });

        // Humidity slider
        sliderTargetHumidity.setValueFrom((float) Constants.MIN_HUMIDITY);
        sliderTargetHumidity.setValueTo((float) Constants.MAX_HUMIDITY);
        sliderTargetHumidity.setStepSize(1.0f);
        sliderTargetHumidity.setValue((float) currentTargetHumidity);

        sliderTargetHumidity.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser && !isUpdating) {
                isUpdating = true;
                etTargetHumidity.setText(String.format("%.0f", value));
                currentTargetHumidity = value;
                updateButtonStates();
                isUpdating = false;
            }
        });
    }

    /**
     * Text watcher'ları ayarlar
     */
    private void setupTextWatchers() {
        etTargetTemp.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!isUpdating) {
                    String text = s.toString().trim();
                    if (!text.isEmpty()) {
                        try {
                            double value = Double.parseDouble(text);
                            if (Utils.isValidTemperature(value)) {
                                isUpdating = true;
                                sliderTargetTemp.setValue((float) value);
                                currentTargetTemp = value;
                                tilTargetTemp.setError(null);
                                updateButtonStates();
                                isUpdating = false;
                            } else {
                                tilTargetTemp.setError("Geçersiz sıcaklık değeri");
                            }
                        } catch (NumberFormatException e) {
                            tilTargetTemp.setError("Geçersiz sayı formatı");
                        }
                    }
                }
            }
        });

        etTargetHumidity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!isUpdating) {
                    String text = s.toString().trim();
                    if (!text.isEmpty()) {
                        try {
                            double value = Double.parseDouble(text);
                            if (Utils.isValidHumidity(value)) {
                                isUpdating = true;
                                sliderTargetHumidity.setValue((float) value);
                                currentTargetHumidity = value;
                                tilTargetHumidity.setError(null);
                                updateButtonStates();
                                isUpdating = false;
                            } else {
                                tilTargetHumidity.setError("Geçersiz nem değeri");
                            }
                        } catch (NumberFormatException e) {
                            tilTargetHumidity.setError("Geçersiz sayı formatı");
                        }
                    }
                }
            }
        });
    }

    /**
     * Click listener'ları ayarlar
     */
    private void setupClickListeners() {
        btnApplyTemp.setOnClickListener(v -> applyTemperature());
        btnApplyHumidity.setOnClickListener(v -> applyHumidity());
        btnApplyAll.setOnClickListener(v -> applyAllSettings());
        btnResetToDefaults.setOnClickListener(v -> resetToDefaults());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Mevcut değerleri yükler
     */
    private void loadCurrentValues() {
        currentTargetTemp = prefsManager.getTargetTemperature();
        currentTargetHumidity = prefsManager.getTargetHumidity();

        updateUI();

        Log.d(TAG, "Mevcut değerler yüklendi - Sıcaklık: " + currentTargetTemp + ", Nem: " + currentTargetHumidity);
    }

    /**
     * Sistem durumunu yeniler
     */
    private void refreshSystemStatus() {
        apiService.getSystemStatus(new ApiService.ApiCallback<SystemStatus>() {
            @Override
            public void onSuccess(SystemStatus systemStatus) {
                runOnUiThread(() -> {
                    currentSystemStatus = systemStatus;
                    updateCurrentValues(systemStatus);
                    updateTargetValues(systemStatus);
                });
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Sistem durumu alınamadı: " + error);
            }
        });
    }

    /**
     * Mevcut değerleri günceller
     */
    private void updateCurrentValues(SystemStatus systemStatus) {
        if (systemStatus == null) return;

        // Current temperature
        tvCurrentTemp.setText(systemStatus.getFormattedTemperature());
        int tempColor = Utils.getTemperatureColor(this,
                systemStatus.getTemperature(), systemStatus.getTargetTemp());
        tvCurrentTemp.setTextColor(tempColor);

        // Current humidity
        tvCurrentHumidity.setText(systemStatus.getFormattedHumidity());
        int humidityColor = Utils.getHumidityColor(this,
                systemStatus.getHumidity(), systemStatus.getTargetHumid());
        tvCurrentHumidity.setTextColor(humidityColor);

        // Status messages
        updateStatusMessages(systemStatus);
    }

    /**
     * Hedef değerleri günceller
     */
    private void updateTargetValues(SystemStatus systemStatus) {
        if (systemStatus == null) return;

        isUpdating = true;

        currentTargetTemp = systemStatus.getTargetTemp();
        currentTargetHumidity = systemStatus.getTargetHumid();

        etTargetTemp.setText(String.format("%.1f", currentTargetTemp));
        etTargetHumidity.setText(String.format("%.0f", currentTargetHumidity));

        sliderTargetTemp.setValue((float) currentTargetTemp);
        sliderTargetHumidity.setValue((float) currentTargetHumidity);

        isUpdating = false;

        updateButtonStates();
    }

    /**
     * Durum mesajlarını günceller
     */
    private void updateStatusMessages(SystemStatus systemStatus) {
        // Temperature status
        String tempStatus = systemStatus.isHeaterState() ?
                "Isıtıcı: AÇIK" : "Isıtıcı: KAPALI";
        tvTempStatus.setText(tempStatus);
        tvTempStatus.setTextColor(Utils.getStatusColor(this, systemStatus.isHeaterState()));

        // Humidity status
        String humidityStatus = systemStatus.isHumidifierState() ?
                "Nemlendirici: AÇIK" : "Nemlendirici: KAPALI";
        tvHumidityStatus.setText(humidityStatus);
        tvHumidityStatus.setTextColor(Utils.getStatusColor(this, systemStatus.isHumidifierState()));
    }

    /**
     * UI'yi günceller
     */
    private void updateUI() {
        isUpdating = true;

        etTargetTemp.setText(String.format("%.1f", currentTargetTemp));
        etTargetHumidity.setText(String.format("%.0f", currentTargetHumidity));

        sliderTargetTemp.setValue((float) currentTargetTemp);
        sliderTargetHumidity.setValue((float) currentTargetHumidity);

        isUpdating = false;

        updateButtonStates();
    }

    /**
     * Buton durumlarını günceller
     */
    private void updateButtonStates() {
        boolean validTemp = Utils.isValidTemperature(currentTargetTemp);
        boolean validHumidity = Utils.isValidHumidity(currentTargetHumidity);

        btnApplyTemp.setEnabled(validTemp);
        btnApplyHumidity.setEnabled(validHumidity);
        btnApplyAll.setEnabled(validTemp && validHumidity);

        // Check if values changed
        if (currentSystemStatus != null) {
            boolean tempChanged = Math.abs(currentTargetTemp - currentSystemStatus.getTargetTemp()) > 0.1;
            boolean humidityChanged = Math.abs(currentTargetHumidity - currentSystemStatus.getTargetHumid()) > 0.5;

            btnApplyTemp.setText(tempChanged ? "Sıcaklığı Uygula *" : "Sıcaklığı Uygula");
            btnApplyHumidity.setText(humidityChanged ? "Nemi Uygula *" : "Nemi Uygula");
            btnApplyAll.setText((tempChanged || humidityChanged) ? "Tümünü Uygula *" : "Tümünü Uygula");
        }
    }

    /**
     * Sıcaklık ayarını uygular
     */
    private void applyTemperature() {
        if (!Utils.isValidTemperature(currentTargetTemp)) {
            Utils.showToast(this, "Geçersiz sıcaklık değeri");
            return;
        }

        btnApplyTemp.setEnabled(false);
        btnApplyTemp.setText("Uygulanıyor...");

        apiService.setTargetTemperature(currentTargetTemp, new ApiService.ApiCallback<ApiResponse>() {
            @Override
            public void onSuccess(ApiResponse response) {
                runOnUiThread(() -> {
                    btnApplyTemp.setEnabled(true);
                    btnApplyTemp.setText("Sıcaklığı Uygula");

                    Utils.showToast(TemperatureHumidityActivity.this,
                            "Hedef sıcaklık güncellendi: " + Utils.formatTemperature(currentTargetTemp));

                    prefsManager.saveTargetValues(currentTargetTemp, currentTargetHumidity);

                    // Refresh after 1 second
                    Utils.delay(1000, this::refreshSystemStatus);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    btnApplyTemp.setEnabled(true);
                    btnApplyTemp.setText("Sıcaklığı Uygula");

                    Utils.showToast(TemperatureHumidityActivity.this,
                            "Sıcaklık ayarlanamadı: " + error);
                });
            }
        });
    }

    /**
     * Nem ayarını uygular
     */
    private void applyHumidity() {
        if (!Utils.isValidHumidity(currentTargetHumidity)) {
            Utils.showToast(this, "Geçersiz nem değeri");
            return;
        }

        btnApplyHumidity.setEnabled(false);
        btnApplyHumidity.setText("Uygulanıyor...");

        apiService.setTargetHumidity(currentTargetHumidity, new ApiService.ApiCallback<ApiResponse>() {
            @Override
            public void onSuccess(ApiResponse response) {
                runOnUiThread(() -> {
                    btnApplyHumidity.setEnabled(true);
                    btnApplyHumidity.setText("Nemi Uygula");

                    Utils.showToast(TemperatureHumidityActivity.this,
                            "Hedef nem güncellendi: " + Utils.formatHumidity(currentTargetHumidity));

                    prefsManager.saveTargetValues(currentTargetTemp, currentTargetHumidity);

                    // Refresh after 1 second
                    Utils.delay(1000, this::refreshSystemStatus);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    btnApplyHumidity.setEnabled(true);
                    btnApplyHumidity.setText("Nemi Uygula");

                    Utils.showToast(TemperatureHumidityActivity.this,
                            "Nem ayarlanamadı: " + error);
                });
            }
        });
    }

    /**
     * Tüm ayarları uygular
     */
    private void applyAllSettings() {
        if (!Utils.isValidTemperature(currentTargetTemp) || !Utils.isValidHumidity(currentTargetHumidity)) {
            Utils.showToast(this, "Geçersiz değerler var");
            return;
        }

        btnApplyAll.setEnabled(false);
        btnApplyAll.setText("Uygulanıyor...");

        // First apply temperature
        apiService.setTargetTemperature(currentTargetTemp, new ApiService.ApiCallback<ApiResponse>() {
            @Override
            public void onSuccess(ApiResponse response) {
                // Then apply humidity
                apiService.setTargetHumidity(currentTargetHumidity, new ApiService.ApiCallback<ApiResponse>() {
                    @Override
                    public void onSuccess(ApiResponse response2) {
                        runOnUiThread(() -> {
                            btnApplyAll.setEnabled(true);
                            btnApplyAll.setText("Tümünü Uygula");

                            Utils.showToast(TemperatureHumidityActivity.this,
                                    "Tüm ayarlar güncellendi");

                            prefsManager.saveTargetValues(currentTargetTemp, currentTargetHumidity);

                            // Refresh after 1 second
                            Utils.delay(1000, TemperatureHumidityActivity.this::refreshSystemStatus);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            btnApplyAll.setEnabled(true);
                            btnApplyAll.setText("Tümünü Uygula");

                            Utils.showToast(TemperatureHumidityActivity.this,
                                    "Nem ayarlanamadı: " + error);
                        });
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    btnApplyAll.setEnabled(true);
                    btnApplyAll.setText("Tümünü Uygula");

                    Utils.showToast(TemperatureHumidityActivity.this,
                            "Sıcaklık ayarlanamadı: " + error);
                });
            }
        });
    }

    /**
     * Varsayılan değerlere sıfırlar
     */
    private void resetToDefaults() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Varsayılan Değerlere Sıfırla")
                .setMessage("Sıcaklık ve nem değerleri varsayılan değerlere sıfırlanacak. Devam edilsin mi?")
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    currentTargetTemp = Constants.DEFAULT_TARGET_TEMP;
                    currentTargetHumidity = Constants.DEFAULT_TARGET_HUMIDITY;
                    updateUI();
                    Utils.showToast(this, "Varsayılan değerler yüklendi");
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    /**
     * Broadcast receiver'ları kaydeder
     */
    private void registerBroadcastReceivers() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_UPDATE_DATA);

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

            if (Constants.ACTION_UPDATE_DATA.equals(action)) {
                SystemStatus systemStatus = (SystemStatus) intent.getSerializableExtra(Constants.EXTRA_SYSTEM_STATUS);
                if (systemStatus != null) {
                    currentSystemStatus = systemStatus;
                    updateCurrentValues(systemStatus);

                    // Update target values only if not currently editing
                    if (!etTargetTemp.hasFocus() && !etTargetHumidity.hasFocus()) {
                        updateTargetValues(systemStatus);
                    }
                }
            }
        }
    };
}