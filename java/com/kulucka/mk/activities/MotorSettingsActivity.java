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
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

import com.kulucka.mk.R;
import com.kulucka.mk.models.ApiResponse;
import com.kulucka.mk.models.MotorSettings;
import com.kulucka.mk.models.SystemStatus;
import com.kulucka.mk.services.ApiService;
import com.kulucka.mk.utils.Constants;
import com.kulucka.mk.utils.SharedPreferencesManager;
import com.kulucka.mk.utils.Utils;

/**
 * Motor ayarları aktivitesi
 * Motor zamanlama ve kontrol ayarları
 */
public class MotorSettingsActivity extends AppCompatActivity {

    private static final String TAG = "MotorSettingsActivity";

    // UI Components
    private Toolbar toolbar;

    // Motor Status
    private MaterialTextView tvMotorStatus;
    private MaterialTextView tvCurrentCycle;
    private MaterialTextView tvNextAction;
    private MaterialTextView tvTotalRotations;

    // Motor Enable/Disable
    private SwitchMaterial switchMotorEnabled;
    private MaterialTextView tvMotorEnabledDesc;

    // Motor Timing Settings
    private TextInputLayout tilWaitTime, tilRunTime;
    private TextInputEditText etWaitTime, etRunTime;
    private Slider sliderWaitTime, sliderRunTime;
    private MaterialTextView tvWaitTimeValue, tvRunTimeValue;

    // Manual Control
    private View layoutManualControl;
    private MaterialButton btnStartMotor;
    private MaterialButton btnStopMotor;
    private MaterialTextView tvManualControlStatus;

    // Action Buttons
    private MaterialButton btnApplySettings;
    private MaterialButton btnResetDefaults;
    private MaterialButton btnTestMotor;

    // Services and Managers
    private ApiService apiService;
    private SharedPreferencesManager prefsManager;

    // Data
    private MotorSettings motorSettings;
    private SystemStatus currentSystemStatus;
    private boolean isUpdating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_motor_settings);

        Log.i(TAG, "MotorSettingsActivity oluşturuluyor");

        // Initialize services
        apiService = ApiService.getInstance(this);
        prefsManager = SharedPreferencesManager.getInstance(this);

        // Setup UI
        initializeUI();
        setupToolbar();
        setupSliders();
        setupClickListeners();

        // Load settings
        loadMotorSettings();

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

        // Motor Status
        tvMotorStatus = findViewById(R.id.tvMotorStatus);
        tvCurrentCycle = findViewById(R.id.tvCurrentCycle);
        tvNextAction = findViewById(R.id.tvNextAction);
        tvTotalRotations = findViewById(R.id.tvTotalRotations);

        // Motor Enable
        switchMotorEnabled = findViewById(R.id.switchMotorEnabled);
        tvMotorEnabledDesc = findViewById(R.id.tvMotorEnabledDesc);

        // Timing Settings
        tilWaitTime = findViewById(R.id.tilWaitTime);
        tilRunTime = findViewById(R.id.tilRunTime);
        etWaitTime = findViewById(R.id.etWaitTime);
        etRunTime = findViewById(R.id.etRunTime);
        sliderWaitTime = findViewById(R.id.sliderWaitTime);
        sliderRunTime = findViewById(R.id.sliderRunTime);
        tvWaitTimeValue = findViewById(R.id.tvWaitTimeValue);
        tvRunTimeValue = findViewById(R.id.tvRunTimeValue);

        // Manual Control
        layoutManualControl = findViewById(R.id.layoutManualControl);
        btnStartMotor = findViewById(R.id.btnStartMotor);
        btnStopMotor = findViewById(R.id.btnStopMotor);
        tvManualControlStatus = findViewById(R.id.tvManualControlStatus);

        // Action Buttons
        btnApplySettings = findViewById(R.id.btnApplySettings);
        btnResetDefaults = findViewById(R.id.btnResetDefaults);
        btnTestMotor = findViewById(R.id.btnTestMotor);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.motor_settings_title);
        }
    }

    private void setupSliders() {
        // Wait Time Slider (dakika)
        sliderWaitTime.setValueFrom(Constants.MIN_MOTOR_WAIT_TIME);
        sliderWaitTime.setValueTo(Constants.MAX_MOTOR_WAIT_TIME);
        sliderWaitTime.setStepSize(1);

        sliderWaitTime.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser && !isUpdating) {
                isUpdating = true;
                int minutes = (int) value;
                etWaitTime.setText(String.valueOf(minutes));
                tvWaitTimeValue.setText("Bekleme: " + minutes + " dakika");
                isUpdating = false;
            }
        });

        // Run Time Slider (saniye)
        sliderRunTime.setValueFrom(Constants.MIN_MOTOR_RUN_TIME);
        sliderRunTime.setValueTo(Constants.MAX_MOTOR_RUN_TIME);
        sliderRunTime.setStepSize(1);

        sliderRunTime.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser && !isUpdating) {
                isUpdating = true;
                int seconds = (int) value;
                etRunTime.setText(String.valueOf(seconds));
                tvRunTimeValue.setText("Çalışma: " + seconds + " saniye");
                isUpdating = false;
            }
        });
    }

    private void setupClickListeners() {
        switchMotorEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isUpdating) {
                motorSettings.setMotorEnabled(isChecked);
                updateMotorEnabledUI(isChecked);
            }
        });

        btnApplySettings.setOnClickListener(v -> applySettings());
        btnResetDefaults.setOnClickListener(v -> resetToDefaults());
        btnTestMotor.setOnClickListener(v -> testMotor());
        btnStartMotor.setOnClickListener(v -> startMotorManually());
        btnStopMotor.setOnClickListener(v -> stopMotorManually());

        // EditText listeners
        etWaitTime.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) updateWaitTimeFromEditText();
        });

        etRunTime.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) updateRunTimeFromEditText();
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

    private void loadMotorSettings() {
        // Load from preferences
        motorSettings = new MotorSettings();
        motorSettings.setMotorWaitTime(prefsManager.getMotorWaitTime());
        motorSettings.setMotorRunTime(prefsManager.getMotorRunTime());
        updateUI();
    }

    private void refreshSystemStatus() {
        apiService.getSystemStatus(new ApiService.ApiCallback<SystemStatus>() {
            @Override
            public void onSuccess(SystemStatus systemStatus) {
                runOnUiThread(() -> {
                    currentSystemStatus = systemStatus;
                    updateMotorStatus(systemStatus);
                    updateTimingFromSystem(systemStatus);
                });
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Sistem durumu alınamadı: " + error);
            }
        });
    }

    private void updateMotorStatus(SystemStatus systemStatus) {
        if (systemStatus == null) return;

        // Motor durumu
        String status = systemStatus.isMotorState() ?
                getString(R.string.motor_running) : getString(R.string.motor_waiting);
        tvMotorStatus.setText("Motor: " + status);
        tvMotorStatus.setTextColor(Utils.getStatusColor(this, systemStatus.isMotorState()));

        // Mevcut döngü
        tvCurrentCycle.setText("Gün " + systemStatus.getDisplayDay() + " / " + systemStatus.getTotalDays());

        // Sonraki işlem
        if (systemStatus.isMotorState()) {
            tvNextAction.setText("Motor durma zamanı: " + systemStatus.getMotorRunTime() + " saniye sonra");
        } else {
            tvNextAction.setText("Sonraki çevirme: " + systemStatus.getMotorWaitTime() + " dakika sonra");
        }

        // Toplam çevirme sayısı (tahmini)
        int totalRotations = (systemStatus.getCurrentDay() - 1) * (1440 / systemStatus.getMotorWaitTime());
        tvTotalRotations.setText("Toplam çevirme: ~" + totalRotations + " kez");

        // Manuel kontrol durumu
        updateManualControlUI(systemStatus.isMotorState());
    }

    private void updateTimingFromSystem(SystemStatus systemStatus) {
        if (systemStatus == null) return;

        motorSettings.setMotorWaitTime(systemStatus.getMotorWaitTime());
        motorSettings.setMotorRunTime(systemStatus.getMotorRunTime());
        updateUI();
    }

    private void updateUI() {
        isUpdating = true;

        // Motor enabled
        switchMotorEnabled.setChecked(motorSettings.isMotorEnabled());
        updateMotorEnabledUI(motorSettings.isMotorEnabled());

        // Wait time
        etWaitTime.setText(String.valueOf(motorSettings.getMotorWaitTime()));
        sliderWaitTime.setValue(motorSettings.getMotorWaitTime());
        tvWaitTimeValue.setText("Bekleme: " + motorSettings.getMotorWaitTime() + " dakika");

        // Run time
        etRunTime.setText(String.valueOf(motorSettings.getMotorRunTime()));
        sliderRunTime.setValue(motorSettings.getMotorRunTime());
        tvRunTimeValue.setText("Çalışma: " + motorSettings.getMotorRunTime() + " saniye");

        isUpdating = false;
    }

    private void updateMotorEnabledUI(boolean enabled) {
        String description = enabled ?
                "Motor otomatik çevirme etkin" :
                "Motor otomatik çevirme devre dışı";
        tvMotorEnabledDesc.setText(description);
        tvMotorEnabledDesc.setTextColor(Utils.getStatusColor(this, enabled));

        // Ayarları etkinleştir/devre dışı bırak
        etWaitTime.setEnabled(enabled);
        etRunTime.setEnabled(enabled);
        sliderWaitTime.setEnabled(enabled);
        sliderRunTime.setEnabled(enabled);
        btnTestMotor.setEnabled(enabled);
    }

    private void updateManualControlUI(boolean isRunning) {
        btnStartMotor.setEnabled(!isRunning);
        btnStopMotor.setEnabled(isRunning);

        String status = isRunning ?
                "Motor çalışıyor (Manuel)" :
                "Motor beklemede";
        tvManualControlStatus.setText(status);
        tvManualControlStatus.setTextColor(Utils.getStatusColor(this, isRunning));
    }

    private void updateWaitTimeFromEditText() {
        try {
            int waitTime = Integer.parseInt(etWaitTime.getText().toString());
            if (Utils.isValidMotorTime(waitTime, true)) {
                motorSettings.setMotorWaitTime(waitTime);
                isUpdating = true;
                sliderWaitTime.setValue(waitTime);
                tvWaitTimeValue.setText("Bekleme: " + waitTime + " dakika");
                isUpdating = false;
            }
        } catch (NumberFormatException e) {
            // Ignore
        }
    }

    private void updateRunTimeFromEditText() {
        try {
            int runTime = Integer.parseInt(etRunTime.getText().toString());
            if (Utils.isValidMotorTime(runTime, false)) {
                motorSettings.setMotorRunTime(runTime);
                isUpdating = true;
                sliderRunTime.setValue(runTime);
                tvRunTimeValue.setText("Çalışma: " + runTime + " saniye");
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

        apiService.setMotorSettings(
                motorSettings.getMotorWaitTime(),
                motorSettings.getMotorRunTime(),
                new ApiService.ApiCallback<ApiResponse>() {
                    @Override
                    public void onSuccess(ApiResponse response) {
                        runOnUiThread(() -> {
                            btnApplySettings.setEnabled(true);
                            btnApplySettings.setText(R.string.apply_changes);

                            Utils.showToast(MotorSettingsActivity.this,
                                    "Motor ayarları güncellendi");

                            prefsManager.saveMotorSettings(
                                    motorSettings.getMotorWaitTime(),
                                    motorSettings.getMotorRunTime());

                            Utils.delay(1000, () -> refreshSystemStatus());
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            btnApplySettings.setEnabled(true);
                            btnApplySettings.setText(R.string.apply_changes);

                            Utils.showToast(MotorSettingsActivity.this,
                                    "Motor ayarları uygulanamadı: " + error);
                        });
                    }
                });
    }

    private boolean validateSettings() {
        if (!motorSettings.isValid()) {
            Utils.showToast(this, motorSettings.getValidationMessage());
            return false;
        }
        return true;
    }

    private void resetToDefaults() {
        new AlertDialog.Builder(this)
                .setTitle("Varsayılan Değerlere Sıfırla")
                .setMessage("Motor ayarları varsayılan değerlere sıfırlanacak. Devam edilsin mi?")
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    motorSettings.setMotorWaitTime(Constants.DEFAULT_MOTOR_WAIT_TIME);
                    motorSettings.setMotorRunTime(Constants.DEFAULT_MOTOR_RUN_TIME);
                    motorSettings.setMotorEnabled(true);
                    updateUI();
                    Utils.showToast(this, "Varsayılan değerler yüklendi");
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void testMotor() {
        new AlertDialog.Builder(this)
                .setTitle("Motor Testi")
                .setMessage("Motor " + motorSettings.getMotorRunTime() +
                        " saniye boyunca çalıştırılacak. Devam edilsin mi?")
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    startMotorManually();

                    // Belirlenen süre sonra otomatik durdur
                    Utils.delay(motorSettings.getMotorRunTime() * 1000, () -> {
                        stopMotorManually();
                        Utils.showToast(this, "Motor testi tamamlandı");
                    });
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void startMotorManually() {
        // Manuel motor başlatma API çağrısı
        btnStartMotor.setEnabled(false);
        btnStartMotor.setText("Başlatılıyor...");

        // API'de manuel motor kontrolü için endpoint eklenebilir
        // Şimdilik simüle ediyoruz
        Utils.delay(1000, () -> {
            btnStartMotor.setEnabled(true);
            btnStartMotor.setText("Manuel Başlat");
            updateManualControlUI(true);
            Utils.showToast(this, "Motor manuel olarak başlatıldı");
        });
    }

    private void stopMotorManually() {
        // Manuel motor durdurma API çağrısı
        btnStopMotor.setEnabled(false);
        btnStopMotor.setText("Durduruluyor...");

        // API'de manuel motor kontrolü için endpoint eklenebilir
        // Şimdilik simüle ediyoruz
        Utils.delay(1000, () -> {
            btnStopMotor.setEnabled(true);
            btnStopMotor.setText("Manuel Durdur");
            updateManualControlUI(false);
            Utils.showToast(this, "Motor manuel olarak durduruldu");
        });
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
                    updateMotorStatus(systemStatus);
                }
            }
        }
    };
}