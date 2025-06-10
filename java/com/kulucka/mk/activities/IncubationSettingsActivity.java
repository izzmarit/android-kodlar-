package com.kulucka.mk.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

import com.kulucka.mk.R;
import com.kulucka.mk.models.ApiResponse;
import com.kulucka.mk.models.IncubationSettings;
import com.kulucka.mk.models.SystemStatus;
import com.kulucka.mk.services.ApiService;
import com.kulucka.mk.utils.Constants;
import com.kulucka.mk.utils.SharedPreferencesManager;
import com.kulucka.mk.utils.Utils;

/**
 * Kuluçka ayarları aktivitesi
 * Kuluçka tipi seçimi ve parametreleri ayarlama
 */
public class IncubationSettingsActivity extends AppCompatActivity {

    private static final String TAG = "IncubationSettingsActivity";

    // UI Components
    private Toolbar toolbar;

    // Current Status
    private MaterialTextView tvCurrentStatus;
    private MaterialTextView tvCurrentType;
    private MaterialTextView tvCurrentDay;
    private MaterialTextView tvRemainingDays;

    // Incubation Type Selection
    private Spinner spinnerIncubationType;
    private MaterialTextView tvTypeDescription;

    // Manual Settings (Conditional)
    private View layoutManualSettings;
    private TextInputLayout tilDevTemp, tilHatchTemp;
    private TextInputLayout tilDevHumid, tilHatchHumid;
    private TextInputLayout tilDevDays, tilHatchDays;
    private TextInputEditText etDevTemp, etHatchTemp;
    private TextInputEditText etDevHumid, etHatchHumid;
    private TextInputEditText etDevDays, etHatchDays;

    // Action Buttons
    private MaterialButton btnStartIncubation;
    private MaterialButton btnStopIncubation;
    private MaterialButton btnApplySettings;
    private MaterialButton btnResetDefaults;

    // Services and Managers
    private ApiService apiService;
    private SharedPreferencesManager prefsManager;

    // Data
    private IncubationSettings incubationSettings;
    private SystemStatus currentSystemStatus;
    private boolean isUpdating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incubation_settings);

        Log.i(TAG, "IncubationSettingsActivity oluşturuluyor");

        // Initialize services
        apiService = ApiService.getInstance(this);
        prefsManager = SharedPreferencesManager.getInstance(this);

        // Setup UI
        initializeUI();
        setupToolbar();
        setupSpinner();
        setupClickListeners();

        // Load settings
        loadIncubationSettings();

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

        // Current Status
        tvCurrentStatus = findViewById(R.id.tvCurrentStatus);
        tvCurrentType = findViewById(R.id.tvCurrentType);
        tvCurrentDay = findViewById(R.id.tvCurrentDay);
        tvRemainingDays = findViewById(R.id.tvRemainingDays);

        // Incubation Type
        spinnerIncubationType = findViewById(R.id.spinnerIncubationType);
        tvTypeDescription = findViewById(R.id.tvTypeDescription);

        // Manual Settings
        layoutManualSettings = findViewById(R.id.layoutManualSettings);
        tilDevTemp = findViewById(R.id.tilDevTemp);
        tilHatchTemp = findViewById(R.id.tilHatchTemp);
        tilDevHumid = findViewById(R.id.tilDevHumid);
        tilHatchHumid = findViewById(R.id.tilHatchHumid);
        tilDevDays = findViewById(R.id.tilDevDays);
        tilHatchDays = findViewById(R.id.tilHatchDays);

        etDevTemp = findViewById(R.id.etDevTemp);
        etHatchTemp = findViewById(R.id.etHatchTemp);
        etDevHumid = findViewById(R.id.etDevHumid);
        etHatchHumid = findViewById(R.id.etHatchHumid);
        etDevDays = findViewById(R.id.etDevDays);
        etHatchDays = findViewById(R.id.etHatchDays);

        // Buttons
        btnStartIncubation = findViewById(R.id.btnStartIncubation);
        btnStopIncubation = findViewById(R.id.btnStopIncubation);
        btnApplySettings = findViewById(R.id.btnApplySettings);
        btnResetDefaults = findViewById(R.id.btnResetDefaults);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.incubation_settings_title);
        }
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, Constants.INCUBATION_TYPE_NAMES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerIncubationType.setAdapter(adapter);

        spinnerIncubationType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                handleIncubationTypeChanged(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupClickListeners() {
        btnStartIncubation.setOnClickListener(v -> startIncubation());
        btnStopIncubation.setOnClickListener(v -> confirmStopIncubation());
        btnApplySettings.setOnClickListener(v -> applySettings());
        btnResetDefaults.setOnClickListener(v -> resetToDefaults());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadIncubationSettings() {
        incubationSettings = prefsManager.getIncubationSettings();
        if (incubationSettings == null) {
            incubationSettings = new IncubationSettings();
        }
        updateUI();
    }

    private void refreshSystemStatus() {
        apiService.getSystemStatus(new ApiService.ApiCallback<SystemStatus>() {
            @Override
            public void onSuccess(SystemStatus systemStatus) {
                runOnUiThread(() -> {
                    currentSystemStatus = systemStatus;
                    updateCurrentStatus(systemStatus);
                    updateButtonStates(systemStatus);
                });
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Sistem durumu alınamadı: " + error);
            }
        });
    }

    private void updateCurrentStatus(SystemStatus systemStatus) {
        if (systemStatus == null) return;

        String status = systemStatus.isIncubationRunning() ?
                getString(R.string.incubation_running) : getString(R.string.incubation_stopped);
        tvCurrentStatus.setText(status);
        tvCurrentStatus.setTextColor(Utils.getStatusColor(this, systemStatus.isIncubationRunning()));

        tvCurrentType.setText(getString(R.string.incubation_type) + ": " + systemStatus.getIncubationType());
        tvCurrentDay.setText(getString(R.string.current_day, systemStatus.getDisplayDay()));

        int remainingDays = systemStatus.getTotalDays() - systemStatus.getDisplayDay();
        tvRemainingDays.setText(getString(R.string.remaining_days, remainingDays));

        if (systemStatus.isIncubationCompleted()) {
            tvRemainingDays.setText(getString(R.string.incubation_completed));
            tvRemainingDays.setTextColor(Utils.getStatusColor(this, true));
        }
    }

    private void updateButtonStates(SystemStatus systemStatus) {
        if (systemStatus == null) return;

        boolean isRunning = systemStatus.isIncubationRunning();
        btnStartIncubation.setEnabled(!isRunning);
        btnStopIncubation.setEnabled(isRunning);
        btnApplySettings.setEnabled(!isRunning);
        spinnerIncubationType.setEnabled(!isRunning);

        if (isRunning) {
            btnStartIncubation.setText(R.string.incubation_in_progress);
        } else {
            btnStartIncubation.setText(R.string.incubation_start);
        }
    }

    private void updateUI() {
        isUpdating = true;

        spinnerIncubationType.setSelection(incubationSettings.getIncubationType());

        if (incubationSettings.isManualType()) {
            etDevTemp.setText(String.valueOf(incubationSettings.getManualDevTemp()));
            etHatchTemp.setText(String.valueOf(incubationSettings.getManualHatchTemp()));
            etDevHumid.setText(String.valueOf(incubationSettings.getManualDevHumid()));
            etHatchHumid.setText(String.valueOf(incubationSettings.getManualHatchHumid()));
            etDevDays.setText(String.valueOf(incubationSettings.getManualDevDays()));
            etHatchDays.setText(String.valueOf(incubationSettings.getManualHatchDays()));
        }

        isUpdating = false;
    }

    private void handleIncubationTypeChanged(int position) {
        if (isUpdating) return;

        incubationSettings.setIncubationType(position);
        updateTypeDescription(position);

        boolean isManual = position == Constants.INCUBATION_TYPE_MANUAL;
        layoutManualSettings.setVisibility(isManual ? View.VISIBLE : View.GONE);

        if (!isManual) {
            setDefaultValuesForType(position);
        }
    }

    private void updateTypeDescription(int incubationType) {
        String description = "";
        switch (incubationType) {
            case Constants.INCUBATION_TYPE_CHICKEN:
                description = "Tavuk: 21 gün, Gelişim 37.5°C %60, Çıkım 37.2°C %65";
                break;
            case Constants.INCUBATION_TYPE_QUAIL:
                description = "Bıldırcın: 17 gün, Gelişim 37.7°C %60, Çıkım 37.2°C %70";
                break;
            case Constants.INCUBATION_TYPE_GOOSE:
                description = "Kaz: 28 gün, Gelişim 37.4°C %55, Çıkım 37.0°C %75";
                break;
            case Constants.INCUBATION_TYPE_MANUAL:
                description = "Manuel: Kendi parametrelerinizi ayarlayın";
                break;
        }
        tvTypeDescription.setText(description);
    }

    private void setDefaultValuesForType(int incubationType) {
        // Ön tanımlı değerler zaten ESP32 tarafında ayarlı
    }

    private void applySettings() {
        if (!validateSettings()) return;

        if (incubationSettings.isManualType()) {
            updateManualSettings();
        }

        btnApplySettings.setEnabled(false);
        btnApplySettings.setText("Uygulanıyor...");

        apiService.setIncubationSettings(incubationSettings, new ApiService.ApiCallback<ApiResponse>() {
            @Override
            public void onSuccess(ApiResponse response) {
                runOnUiThread(() -> {
                    btnApplySettings.setEnabled(true);
                    btnApplySettings.setText(R.string.apply_changes);

                    Utils.showToast(IncubationSettingsActivity.this,
                            "Kuluçka ayarları güncellendi");

                    prefsManager.saveIncubationSettings(incubationSettings);
                    Utils.delay(1000, () -> refreshSystemStatus());
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    btnApplySettings.setEnabled(true);
                    btnApplySettings.setText(R.string.apply_changes);

                    Utils.showToast(IncubationSettingsActivity.this,
                            "Ayarlar uygulanamadı: " + error);
                });
            }
        });
    }

    private boolean validateSettings() {
        if (!incubationSettings.isValidConfiguration()) {
            Utils.showToast(this, incubationSettings.getValidationMessage());
            return false;
        }
        return true;
    }

    private void updateManualSettings() {
        try {
            incubationSettings.setManualDevTemp(Double.parseDouble(etDevTemp.getText().toString()));
            incubationSettings.setManualHatchTemp(Double.parseDouble(etHatchTemp.getText().toString()));
            incubationSettings.setManualDevHumid(Integer.parseInt(etDevHumid.getText().toString()));
            incubationSettings.setManualHatchHumid(Integer.parseInt(etHatchHumid.getText().toString()));
            incubationSettings.setManualDevDays(Integer.parseInt(etDevDays.getText().toString()));
            incubationSettings.setManualHatchDays(Integer.parseInt(etHatchDays.getText().toString()));
        } catch (NumberFormatException e) {
            Utils.showToast(this, "Geçersiz sayı formatı");
        }
    }

    private void startIncubation() {
        if (!validateSettings()) return;

        new AlertDialog.Builder(this)
                .setTitle(R.string.incubation_start)
                .setMessage(R.string.incubation_confirm_start)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    incubationSettings.setIncubationRunning(true);
                    applySettings();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void confirmStopIncubation() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.incubation_stop)
                .setMessage(R.string.incubation_confirm_stop)
                .setPositiveButton(R.string.yes, (dialog, which) -> stopIncubation())
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void stopIncubation() {
        incubationSettings.setIncubationRunning(false);
        applySettings();
    }

    private void resetToDefaults() {
        new AlertDialog.Builder(this)
                .setTitle("Varsayılan Değerlere Sıfırla")
                .setMessage("Tüm kuluçka ayarları varsayılan değerlere sıfırlanacak. Devam edilsin mi?")
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    incubationSettings = new IncubationSettings();
                    updateUI();
                    Utils.showToast(this, "Varsayılan değerler yüklendi");
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void registerBroadcastReceivers() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_UPDATE_DATA);
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

            if (Constants.ACTION_UPDATE_DATA.equals(action)) {
                SystemStatus systemStatus = (SystemStatus) intent.getSerializableExtra(Constants.EXTRA_SYSTEM_STATUS);
                if (systemStatus != null) {
                    currentSystemStatus = systemStatus;
                    updateCurrentStatus(systemStatus);
                    updateButtonStates(systemStatus);
                }
            }
        }
    };
}