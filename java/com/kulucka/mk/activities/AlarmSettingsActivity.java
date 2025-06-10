package com.kulucka.mk.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.RangeSlider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

import com.kulucka.mk.R;
import com.kulucka.mk.models.AlarmSettings;
import com.kulucka.mk.models.ApiResponse;
import com.kulucka.mk.models.SystemStatus;
import com.kulucka.mk.services.ApiService;
import com.kulucka.mk.utils.Constants;
import com.kulucka.mk.utils.SharedPreferencesManager;
import com.kulucka.mk.utils.Utils;

import java.util.List;

/**
 * Alarm ayarları aktivitesi
 * Sıcaklık ve nem alarm limitleri ayarlama
 */
public class AlarmSettingsActivity extends AppCompatActivity {

    private static final String TAG = "AlarmSettingsActivity";

    // UI Components
    private Toolbar toolbar;

    // Alarm Enable/Disable
    private SwitchMaterial switchAlarmEnabled;
    private MaterialTextView tvAlarmStatus;
    private MaterialTextView tvLastAlarm;

    // Current Values Display
    private MaterialTextView tvCurrentTemp;
    private MaterialTextView tvCurrentHumidity;

    // Temperature Alarm Settings
    private View layoutTempAlarms;
    private TextInputLayout tilTempLow, tilTempHigh;
    private TextInputEditText etTempLow, etTempHigh;
    private RangeSlider tempRangeSlider;
    private MaterialTextView tvTempRange;

    // Humidity Alarm Settings
    private View layoutHumidityAlarms;
    private TextInputLayout tilHumidLow, tilHumidHigh;
    private TextInputEditText etHumidLow, etHumidHigh;
    private RangeSlider humidityRangeSlider;
    private MaterialTextView tvHumidityRange;

    // Action Buttons
    private MaterialButton btnApplySettings;
    private MaterialButton btnResetDefaults;
    private MaterialButton btnTestAlarm;
    private MaterialButton btnAlarmHistory;

    // Services and Managers
    private ApiService apiService;
    private SharedPreferencesManager prefsManager;

    // Data
    private AlarmSettings alarmSettings;
    private SystemStatus currentSystemStatus;
    private boolean isUpdating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_settings);

        Log.i(TAG, "AlarmSettingsActivity oluşturuluyor");

        // Initialize services
        apiService = ApiService.getInstance(this);
        prefsManager = SharedPreferencesManager.getInstance(this);

        // Setup UI
        initializeUI();
        setupToolbar();
        setupRangeSliders();
        setupClickListeners();

        // Load settings
        loadAlarmSettings();

        // Refresh status
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

    private void initializeUI() {
        toolbar = findViewById(R.id.toolbar);

        // Alarm Enable
        switchAlarmEnabled = findViewById(R.id.switchAlarmEnabled);
        tvAlarmStatus = findViewById(R.id.tvAlarmStatus);
        tvLastAlarm = findViewById(R.id.tvLastAlarm);

        // Current Values
        tvCurrentTemp = findViewById(R.id.tvCurrentTemp);
        tvCurrentHumidity = findViewById(R.id.tvCurrentHumidity);

        // Temperature Alarms
        layoutTempAlarms = findViewById(R.id.layoutTempAlarms);
        tilTempLow = findViewById(R.id.tilTempLow);
        tilTempHigh = findViewById(R.id.tilTempHigh);
        etTempLow = findViewById(R.id.etTempLow);
        etTempHigh = findViewById(R.id.etTempHigh);
        tempRangeSlider = findViewById(R.id.tempRangeSlider);
        tvTempRange = findViewById(R.id.tvTempRange);

        // Humidity Alarms
        layoutHumidityAlarms = findViewById(R.id.layoutHumidityAlarms);
        tilHumidLow = findViewById(R.id.tilHumidLow);
        tilHumidHigh = findViewById(R.id.tilHumidHigh);
        etHumidLow = findViewById(R.id.etHumidLow);
        etHumidHigh = findViewById(R.id.etHumidHigh);
        humidityRangeSlider = findViewById(R.id.humidityRangeSlider);
        tvHumidityRange = findViewById(R.id.tvHumidityRange);

        // Action Buttons
        btnApplySettings = findViewById(R.id.btnApplySettings);
        btnResetDefaults = findViewById(R.id.btnResetDefaults);
        btnTestAlarm = findViewById(R.id.btnTestAlarm);
        btnAlarmHistory = findViewById(R.id.btnAlarmHistory);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.alarm_settings_title);
        }
    }

    private void setupRangeSliders() {
        // Temperature Range Slider
        tempRangeSlider.setValueFrom((float) Constants.MIN_TEMPERATURE);
        tempRangeSlider.setValueTo((float) Constants.MAX_TEMPERATURE);
        tempRangeSlider.setStepSize(0.1f);

        tempRangeSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser && !isUpdating) {
                List<Float> values = slider.getValues();
                if (values.size() >= 2) {
                    isUpdating = true;
                    float low = values.get(0);
                    float high = values.get(1);
                    etTempLow.setText(String.format("%.1f", low));
                    etTempHigh.setText(String.format("%.1f", high));
                    updateTempRangeText(low, high);
                    isUpdating = false;
                }
            }
        });

        // Humidity Range Slider
        humidityRangeSlider.setValueFrom((float) Constants.MIN_HUMIDITY);
        humidityRangeSlider.setValueTo((float) Constants.MAX_HUMIDITY);
        humidityRangeSlider.setStepSize(1f);

        humidityRangeSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser && !isUpdating) {
                List<Float> values = slider.getValues();
                if (values.size() >= 2) {
                    isUpdating = true;
                    float low = values.get(0);
                    float high = values.get(1);
                    etHumidLow.setText(String.format("%.0f", low));
                    etHumidHigh.setText(String.format("%.0f", high));
                    updateHumidityRangeText(low, high);
                    isUpdating = false;
                }
            }
        });
    }

    private void setupClickListeners() {
        switchAlarmEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isUpdating) {
                alarmSettings.setAlarmEnabled(isChecked);
                updateAlarmEnabledUI(isChecked);
            }
        });

        btnApplySettings.setOnClickListener(v -> applySettings());
        btnResetDefaults.setOnClickListener(v -> resetToDefaults());
        btnTestAlarm.setOnClickListener(v -> testAlarm());
        btnAlarmHistory.setOnClickListener(v -> showAlarmHistory());

        // EditText focus listeners
        etTempLow.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) updateTempFromEditText();
        });

        etTempHigh.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) updateTempFromEditText();
        });

        etHumidLow.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) updateHumidityFromEditText();
        });

        etHumidHigh.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) updateHumidityFromEditText();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadAlarmSettings() {
        alarmSettings = prefsManager.getAlarmSettings();
        if (alarmSettings == null) {
            alarmSettings = new AlarmSettings();
        }
        updateUI();
    }

    private void refreshSystemStatus() {
        apiService.getSystemStatus(new ApiService.ApiCallback<SystemStatus>() {
            @Override
            public void onSuccess(SystemStatus systemStatus) {
                runOnUiThread(() -> {
                    currentSystemStatus = systemStatus;
                    updateCurrentValues(systemStatus);
                    updateAlarmStatus(systemStatus);
                    checkAlarmConditions(systemStatus);
                });
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Sistem durumu alınamadı: " + error);
            }
        });
    }

    private void updateCurrentValues(SystemStatus systemStatus) {
        if (systemStatus == null) return;

        tvCurrentTemp.setText("Mevcut: " + systemStatus.getFormattedTemperature());
        tvCurrentHumidity.setText("Mevcut: " + systemStatus.getFormattedHumidity());

        // Renk kodlaması
        int tempColor = Utils.getTemperatureColor(this,
                systemStatus.getTemperature(), systemStatus.getTargetTemp());
        tvCurrentTemp.setTextColor(tempColor);

        int humidityColor = Utils.getHumidityColor(this,
                systemStatus.getHumidity(), systemStatus.getTargetHumid());
        tvCurrentHumidity.setTextColor(humidityColor);
    }

    private void updateAlarmStatus(SystemStatus systemStatus) {
        if (systemStatus == null) return;

        // Alarmları sistem durumundan güncelle
        alarmSettings.setAlarmEnabled(systemStatus.isAlarmEnabled());
        alarmSettings.setTempLowAlarm(systemStatus.getTempLowAlarm());
        alarmSettings.setTempHighAlarm(systemStatus.getTempHighAlarm());
        alarmSettings.setHumidLowAlarm(systemStatus.getHumidLowAlarm());
        alarmSettings.setHumidHighAlarm(systemStatus.getHumidHighAlarm());

        updateUI();

        // Son alarm bilgisi
        String lastAlarmMessage = prefsManager.getLastAlarmMessage();
        long lastAlarmTime = prefsManager.getLastAlarmTime();

        if (lastAlarmMessage != null && lastAlarmTime > 0) {
            String timeAgo = Utils.getElapsedTime(lastAlarmTime);
            tvLastAlarm.setText("Son alarm: " + lastAlarmMessage + " (" + timeAgo + " önce)");
            tvLastAlarm.setVisibility(View.VISIBLE);
        } else {
            tvLastAlarm.setText(R.string.no_recent_alarms);
            tvLastAlarm.setVisibility(View.VISIBLE);
        }
    }

    private void checkAlarmConditions(SystemStatus systemStatus) {
        if (systemStatus == null || !alarmSettings.isAlarmEnabled()) return;

        boolean tempAlarm = alarmSettings.checkTemperatureAlarm(systemStatus.getTemperature());
        boolean humidityAlarm = alarmSettings.checkHumidityAlarm(systemStatus.getHumidity());

        if (tempAlarm || humidityAlarm) {
            tvAlarmStatus.setText("⚠️ ALARM AKTİF!");
            tvAlarmStatus.setTextColor(getColor(R.color.color_error));
        } else {
            tvAlarmStatus.setText("✓ Normal Aralıkta");
            tvAlarmStatus.setTextColor(getColor(R.color.color_success));
        }
    }

    private void updateUI() {
        isUpdating = true;

        // Alarm enabled
        switchAlarmEnabled.setChecked(alarmSettings.isAlarmEnabled());
        updateAlarmEnabledUI(alarmSettings.isAlarmEnabled());

        // Temperature alarms
        etTempLow.setText(String.format("%.1f", alarmSettings.getTempLowAlarm()));
        etTempHigh.setText(String.format("%.1f", alarmSettings.getTempHighAlarm()));
        tempRangeSlider.setValues((float) alarmSettings.getTempLowAlarm(),
                (float) alarmSettings.getTempHighAlarm());
        updateTempRangeText(alarmSettings.getTempLowAlarm(), alarmSettings.getTempHighAlarm());

        // Humidity alarms
        etHumidLow.setText(String.format("%.0f", alarmSettings.getHumidLowAlarm()));
        etHumidHigh.setText(String.format("%.0f", alarmSettings.getHumidHighAlarm()));
        humidityRangeSlider.setValues((float) alarmSettings.getHumidLowAlarm(),
                (float) alarmSettings.getHumidHighAlarm());
        updateHumidityRangeText(alarmSettings.getHumidLowAlarm(), alarmSettings.getHumidHighAlarm());

        isUpdating = false;
    }

    private void updateAlarmEnabledUI(boolean enabled) {
        String status = enabled ?
                "Alarmlar etkin" :
                "Alarmlar devre dışı";
        tvAlarmStatus.setText(status);
        tvAlarmStatus.setTextColor(Utils.getStatusColor(this, enabled));

        // Ayarları etkinleştir/devre dışı bırak
        layoutTempAlarms.setAlpha(enabled ? 1.0f : 0.5f);
        layoutHumidityAlarms.setAlpha(enabled ? 1.0f : 0.5f);

        etTempLow.setEnabled(enabled);
        etTempHigh.setEnabled(enabled);
        etHumidLow.setEnabled(enabled);
        etHumidHigh.setEnabled(enabled);
        tempRangeSlider.setEnabled(enabled);
        humidityRangeSlider.setEnabled(enabled);
        btnTestAlarm.setEnabled(enabled);
    }

    private void updateTempRangeText(double low, double high) {
        tvTempRange.setText(String.format("Alarm aralığı: %.1f°C - %.1f°C", low, high));
    }

    private void updateHumidityRangeText(double low, double high) {
        tvHumidityRange.setText(String.format("Alarm aralığı: %.0f%% - %.0f%%", low, high));
    }

    private void updateTempFromEditText() {
        try {
            double low = Double.parseDouble(etTempLow.getText().toString());
            double high = Double.parseDouble(etTempHigh.getText().toString());

            if (Utils.isValidTemperature(low) && Utils.isValidTemperature(high) && low < high) {
                alarmSettings.setTempLowAlarm(low);
                alarmSettings.setTempHighAlarm(high);

                isUpdating = true;
                tempRangeSlider.setValues((float) low, (float) high);
                updateTempRangeText(low, high);
                isUpdating = false;
            }
        } catch (NumberFormatException e) {
            // Ignore
        }
    }

    private void updateHumidityFromEditText() {
        try {
            double low = Double.parseDouble(etHumidLow.getText().toString());
            double high = Double.parseDouble(etHumidHigh.getText().toString());

            if (Utils.isValidHumidity(low) && Utils.isValidHumidity(high) && low < high) {
                alarmSettings.setHumidLowAlarm(low);
                alarmSettings.setHumidHighAlarm(high);

                isUpdating = true;
                humidityRangeSlider.setValues((float) low, (float) high);
                updateHumidityRangeText(low, high);
                isUpdating = false;
            }
        } catch (NumberFormatException e) {
            // Ignore
        }
    }

    private void applySettings() {
        if (!validateSettings()) return;

        btnApplySettings.setEnabled(false);
        btnApplySettings.setText("Uygulanıyor...");

        apiService.setAlarmSettings(alarmSettings, new ApiService.ApiCallback<ApiResponse>() {
            @Override
            public void onSuccess(ApiResponse response) {
                runOnUiThread(() -> {
                    btnApplySettings.setEnabled(true);
                    btnApplySettings.setText(R.string.apply_changes);

                    Utils.showToast(AlarmSettingsActivity.this,
                            "Alarm ayarları güncellendi");

                    prefsManager.saveAlarmSettings(alarmSettings);
                    Utils.delay(1000, () -> refreshSystemStatus());
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    btnApplySettings.setEnabled(true);
                    btnApplySettings.setText(R.string.apply_changes);

                    Utils.showToast(AlarmSettingsActivity.this,
                            "Alarm ayarları uygulanamadı: " + error);
                });
            }
        });
    }

    private boolean validateSettings() {
        if (!alarmSettings.isValid()) {
            Utils.showToast(this, "Geçersiz alarm değerleri. Alt limit üst limitten küçük olmalı.");
            return false;
        }
        return true;
    }

    private void resetToDefaults() {
        new AlertDialog.Builder(this)
                .setTitle("Varsayılan Değerlere Sıfırla")
                .setMessage("Alarm ayarları varsayılan değerlere sıfırlanacak. Devam edilsin mi?")
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    alarmSettings = new AlarmSettings();
                    updateUI();
                    Utils.showToast(this, "Varsayılan değerler yüklendi");
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void testAlarm() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.alarm_test)
                .setMessage("Test alarm bildirimi gönderilecek. Devam edilsin mi?")
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    // Test alarm bildirimi gönder
                    Intent intent = new Intent(Constants.ACTION_ALARM_TRIGGERED);
                    intent.putExtra(Constants.EXTRA_ERROR_MESSAGE,
                            "TEST ALARMI: Bu bir test bildirimidir.");
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

                    Utils.showToast(this, "Test alarmı gönderildi");
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void showAlarmHistory() {
        // Alarm geçmişi dialog'u
        new AlertDialog.Builder(this)
                .setTitle(R.string.alarm_history)
                .setMessage(getAlarmHistoryText())
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    private String getAlarmHistoryText() {
        String lastAlarmMessage = prefsManager.getLastAlarmMessage();
        long lastAlarmTime = prefsManager.getLastAlarmTime();

        if (lastAlarmMessage != null && lastAlarmTime > 0) {
            return "Son Alarm:\n" +
                    lastAlarmMessage + "\n\n" +
                    "Zaman: " + Utils.formatDateTime(lastAlarmTime) + "\n" +
                    "(" + Utils.getElapsedTime(lastAlarmTime) + " önce)";
        } else {
            return "Henüz alarm kaydı bulunmuyor.";
        }
    }

    private void registerBroadcastReceivers() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_UPDATE_DATA);
        filter.addAction(Constants.ACTION_ALARM_TRIGGERED);
        lbm.registerReceiver(broadcastReceiver, filter);
    }

    private void unregisterBroadcastReceivers() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

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
                        updateCurrentValues(systemStatus);
                        checkAlarmConditions(systemStatus);
                    }
                    break;

                case Constants.ACTION_ALARM_TRIGGERED:
                    String alarmMessage = intent.getStringExtra(Constants.EXTRA_ERROR_MESSAGE);
                    if (alarmMessage != null) {
                        // Alarm geçmişini güncelle
                        prefsManager.saveLastAlarm(alarmMessage, System.currentTimeMillis());
                        updateAlarmStatus(currentSystemStatus);
                    }
                    break;
            }
        }
    };
}