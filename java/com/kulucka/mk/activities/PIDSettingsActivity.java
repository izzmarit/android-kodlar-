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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;

import androidx.appcompat.app.AlertDialog;
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
import com.kulucka.mk.models.PIDSettings;
import com.kulucka.mk.models.SystemStatus;
import com.kulucka.mk.services.ApiService;
import com.kulucka.mk.utils.Constants;
import com.kulucka.mk.utils.SharedPreferencesManager;
import com.kulucka.mk.utils.Utils;

/**
 * PID kontrol ayarları aktivitesi
 * PID parametrelerini ve modlarını ayarlama
 */
public class PIDSettingsActivity extends AppCompatActivity {

    private static final String TAG = "PIDSettingsActivity";

    // UI Components
    private Toolbar toolbar;

    // PID Status
    private MaterialTextView tvPIDStatus;
    private MaterialTextView tvCurrentError;
    private MaterialTextView tvPIDOutput;
    private MaterialTextView tvTuningStatus;

    // PID Mode Selection
    private Spinner spinnerPIDMode;
    private MaterialTextView tvModeDescription;

    // PID Parameters
    private TextInputLayout tilKp, tilKi, tilKd;
    private TextInputEditText etKp, etKi, etKd;
    private Slider sliderKp, sliderKi, sliderKd;
    private MaterialTextView tvKpValue, tvKiValue, tvKdValue;

    // Auto Tuning
    private View layoutAutoTuning;
    private MaterialTextView tvAutoTuneProgress;
    private ProgressBar progressBarAutoTune;
    private MaterialButton btnStartAutoTune;
    private MaterialButton btnStopAutoTune;

    // Action Buttons
    private MaterialButton btnApplyParameters;
    private MaterialButton btnResetDefaults;
    private MaterialButton btnLoadPresets;

    // Services and Managers
    private ApiService apiService;
    private SharedPreferencesManager prefsManager;

    // Data
    private PIDSettings pidSettings;
    private SystemStatus currentSystemStatus;
    private boolean isUpdating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pid_settings);

        Log.i(TAG, "PIDSettingsActivity oluşturuluyor");

        // Initialize services
        apiService = ApiService.getInstance(this);
        prefsManager = SharedPreferencesManager.getInstance(this);

        // Setup UI
        initializeUI();
        setupToolbar();
        setupSpinner();
        setupSliders();
        setupTextWatchers();
        setupClickListeners();

        // Load settings
        loadPIDSettings();

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

        // PID Status
        tvPIDStatus = findViewById(R.id.tvPIDStatus);
        tvCurrentError = findViewById(R.id.tvCurrentError);
        tvPIDOutput = findViewById(R.id.tvPIDOutput);
        tvTuningStatus = findViewById(R.id.tvTuningStatus);

        // PID Mode
        spinnerPIDMode = findViewById(R.id.spinnerPIDMode);
        tvModeDescription = findViewById(R.id.tvModeDescription);

        // PID Parameters
        tilKp = findViewById(R.id.tilKp);
        tilKi = findViewById(R.id.tilKi);
        tilKd = findViewById(R.id.tilKd);
        etKp = findViewById(R.id.etKp);
        etKi = findViewById(R.id.etKi);
        etKd = findViewById(R.id.etKd);
        sliderKp = findViewById(R.id.sliderKp);
        sliderKi = findViewById(R.id.sliderKi);
        sliderKd = findViewById(R.id.sliderKd);
        tvKpValue = findViewById(R.id.tvKpValue);
        tvKiValue = findViewById(R.id.tvKiValue);
        tvKdValue = findViewById(R.id.tvKdValue);

        // Auto Tuning
        layoutAutoTuning = findViewById(R.id.layoutAutoTuning);
        tvAutoTuneProgress = findViewById(R.id.tvAutoTuneProgress);
        progressBarAutoTune = findViewById(R.id.progressBarAutoTune);
        btnStartAutoTune = findViewById(R.id.btnStartAutoTune);
        btnStopAutoTune = findViewById(R.id.btnStopAutoTune);

        // Action Buttons
        btnApplyParameters = findViewById(R.id.btnApplyParameters);
        btnResetDefaults = findViewById(R.id.btnResetDefaults);
        btnLoadPresets = findViewById(R.id.btnLoadPresets);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.pid_settings_title);
        }
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, Constants.PID_MODE_NAMES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPIDMode.setAdapter(adapter);

        spinnerPIDMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                handlePIDModeChanged(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupSliders() {
        // Kp Slider
        sliderKp.setValueFrom((float) Constants.MIN_PID_KP);
        sliderKp.setValueTo((float) Constants.MAX_PID_KP);
        sliderKp.setStepSize(0.1f);

        sliderKp.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser && !isUpdating) {
                isUpdating = true;
                etKp.setText(String.format("%.2f", value));
                tvKpValue.setText(String.format("Kp: %.2f", value));
                isUpdating = false;
            }
        });

        // Ki Slider
        sliderKi.setValueFrom((float) Constants.MIN_PID_KI);
        sliderKi.setValueTo((float) Constants.MAX_PID_KI);
        sliderKi.setStepSize(0.01f);

        sliderKi.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser && !isUpdating) {
                isUpdating = true;
                etKi.setText(String.format("%.2f", value));
                tvKiValue.setText(String.format("Ki: %.2f", value));
                isUpdating = false;
            }
        });

        // Kd Slider
        sliderKd.setValueFrom((float) Constants.MIN_PID_KD);
        sliderKd.setValueTo((float) Constants.MAX_PID_KD);
        sliderKd.setStepSize(0.01f);

        sliderKd.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser && !isUpdating) {
                isUpdating = true;
                etKd.setText(String.format("%.2f", value));
                tvKdValue.setText(String.format("Kd: %.2f", value));
                isUpdating = false;
            }
        });
    }

    private void setupTextWatchers() {
        etKp.addTextChangedListener(createTextWatcher(sliderKp, tvKpValue, "Kp"));
        etKi.addTextChangedListener(createTextWatcher(sliderKi, tvKiValue, "Ki"));
        etKd.addTextChangedListener(createTextWatcher(sliderKd, tvKdValue, "Kd"));
    }

    private TextWatcher createTextWatcher(Slider slider, MaterialTextView textView, String paramName) {
        return new TextWatcher() {
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
                            if (Utils.isValidPIDValue(value, paramName.toLowerCase())) {
                                isUpdating = true;
                                slider.setValue((float) value);
                                textView.setText(String.format("%s: %.2f", paramName, value));
                                isUpdating = false;
                            }
                        } catch (NumberFormatException e) {
                            // Ignore
                        }
                    }
                }
            }
        };
    }

    private void setupClickListeners() {
        btnApplyParameters.setOnClickListener(v -> applyParameters());
        btnResetDefaults.setOnClickListener(v -> resetToDefaults());
        btnLoadPresets.setOnClickListener(v -> showPresetsDialog());
        btnStartAutoTune.setOnClickListener(v -> startAutoTune());
        btnStopAutoTune.setOnClickListener(v -> stopAutoTune());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadPIDSettings() {
        pidSettings = prefsManager.getPIDSettings();
        if (pidSettings == null) {
            pidSettings = new PIDSettings();
        }
        updateUI();
    }

    private void refreshSystemStatus() {
        apiService.getSystemStatus(new ApiService.ApiCallback<SystemStatus>() {
            @Override
            public void onSuccess(SystemStatus systemStatus) {
                runOnUiThread(() -> {
                    currentSystemStatus = systemStatus;
                    updatePIDStatus(systemStatus);
                    updateAutoTuneStatus(systemStatus);
                });
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Sistem durumu alınamadı: " + error);
            }
        });
    }

    private void updatePIDStatus(SystemStatus systemStatus) {
        if (systemStatus == null) return;

        String status = systemStatus.getPidModeString();
        tvPIDStatus.setText("PID Durumu: " + status);
        tvPIDStatus.setTextColor(Utils.getPIDModeColor(this, systemStatus.getPidMode()));

        tvCurrentError.setText(String.format("Hata: %.2f°C", systemStatus.getPidError()));
        tvPIDOutput.setText(String.format("Çıkış: %.1f%%", systemStatus.getPidOutput() * 100));

        if (systemStatus.isPidActive()) {
            tvTuningStatus.setText("PID aktif ve çalışıyor");
            tvTuningStatus.setTextColor(Utils.getStatusColor(this, true));
        } else {
            tvTuningStatus.setText("PID aktif değil");
            tvTuningStatus.setTextColor(Utils.getStatusColor(this, false));
        }
    }

    private void updateAutoTuneStatus(SystemStatus systemStatus) {
        if (systemStatus == null) return;

        boolean isAutoTuning = systemStatus.isPidAutoTuneActive();
        layoutAutoTuning.setVisibility(
                systemStatus.getPidMode() == Constants.PID_MODE_AUTO ? View.VISIBLE : View.GONE);

        if (isAutoTuning) {
            double progress = systemStatus.getAutoTuneProgress();
            progressBarAutoTune.setProgress((int) progress);
            tvAutoTuneProgress.setText(String.format("İlerleme: %.0f%%", progress));
            btnStartAutoTune.setEnabled(false);
            btnStopAutoTune.setEnabled(true);
        } else {
            progressBarAutoTune.setProgress(0);
            tvAutoTuneProgress.setText("Otomatik ayarlama beklemede");
            btnStartAutoTune.setEnabled(true);
            btnStopAutoTune.setEnabled(false);
        }

        if (systemStatus.isAutoTuneFinished()) {
            tvAutoTuneProgress.setText("Otomatik ayarlama tamamlandı!");
            tvAutoTuneProgress.setTextColor(Utils.getStatusColor(this, true));
        }
    }

    private void updateUI() {
        isUpdating = true;

        spinnerPIDMode.setSelection(pidSettings.getPidMode());

        etKp.setText(String.format("%.2f", pidSettings.getPidKp()));
        etKi.setText(String.format("%.2f", pidSettings.getPidKi()));
        etKd.setText(String.format("%.2f", pidSettings.getPidKd()));

        sliderKp.setValue((float) pidSettings.getPidKp());
        sliderKi.setValue((float) pidSettings.getPidKi());
        sliderKd.setValue((float) pidSettings.getPidKd());

        tvKpValue.setText(String.format("Kp: %.2f", pidSettings.getPidKp()));
        tvKiValue.setText(String.format("Ki: %.2f", pidSettings.getPidKi()));
        tvKdValue.setText(String.format("Kd: %.2f", pidSettings.getPidKd()));

        isUpdating = false;
    }

    private void handlePIDModeChanged(int position) {
        if (isUpdating) return;

        pidSettings.setPidMode(position);
        updateModeDescription(position);

        boolean showAutoTune = position == Constants.PID_MODE_AUTO;
        layoutAutoTuning.setVisibility(showAutoTune ? View.VISIBLE : View.GONE);

        boolean enableParams = position != Constants.PID_MODE_OFF;
        etKp.setEnabled(enableParams);
        etKi.setEnabled(enableParams);
        etKd.setEnabled(enableParams);
        sliderKp.setEnabled(enableParams);
        sliderKi.setEnabled(enableParams);
        sliderKd.setEnabled(enableParams);
    }

    private void updateModeDescription(int pidMode) {
        String description = "";
        switch (pidMode) {
            case Constants.PID_MODE_OFF:
                description = "PID kontrolü devre dışı. Basit açma/kapama kontrolü kullanılır.";
                break;
            case Constants.PID_MODE_MANUAL:
                description = "Manuel PID kontrolü. Parametreleri kendiniz ayarlayın.";
                break;
            case Constants.PID_MODE_AUTO:
                description = "Otomatik PID kontrolü. Sistem kendini ayarlar.";
                break;
        }
        tvModeDescription.setText(description);
    }

    private void applyParameters() {
        if (!validateSettings()) return;

        updatePIDSettings();

        btnApplyParameters.setEnabled(false);
        btnApplyParameters.setText("Uygulanıyor...");

        apiService.setPIDSettings(pidSettings, new ApiService.ApiCallback<ApiResponse>() {
            @Override
            public void onSuccess(ApiResponse response) {
                runOnUiThread(() -> {
                    btnApplyParameters.setEnabled(true);
                    btnApplyParameters.setText(R.string.apply_changes);

                    Utils.showToast(PIDSettingsActivity.this,
                            "PID ayarları güncellendi");

                    prefsManager.savePIDSettings(pidSettings);
                    Utils.delay(1000, () -> refreshSystemStatus());
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    btnApplyParameters.setEnabled(true);
                    btnApplyParameters.setText(R.string.apply_changes);

                    Utils.showToast(PIDSettingsActivity.this,
                            "PID ayarları uygulanamadı: " + error);
                });
            }
        });
    }

    private boolean validateSettings() {
        if (!pidSettings.isValid()) {
            Utils.showToast(this, "Geçersiz PID parametreleri");
            return false;
        }
        return true;
    }

    private void updatePIDSettings() {
        try {
            pidSettings.setPidKp(Double.parseDouble(etKp.getText().toString()));
            pidSettings.setPidKi(Double.parseDouble(etKi.getText().toString()));
            pidSettings.setPidKd(Double.parseDouble(etKd.getText().toString()));
        } catch (NumberFormatException e) {
            Log.e(TAG, "PID değerleri parse edilemedi", e);
        }
    }

    private void resetToDefaults() {
        new AlertDialog.Builder(this)
                .setTitle("Varsayılan Değerlere Sıfırla")
                .setMessage("PID parametreleri varsayılan değerlere sıfırlanacak. Devam edilsin mi?")
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    pidSettings = new PIDSettings();
                    updateUI();
                    Utils.showToast(this, "Varsayılan değerler yüklendi");
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void showPresetsDialog() {
        String[] presets = {"Hassas Kontrol (Kp:1.5, Ki:0.3, Kd:0.05)",
                "Normal Kontrol (Kp:2.0, Ki:0.5, Kd:0.1)",
                "Hızlı Kontrol (Kp:3.0, Ki:0.8, Kd:0.2)",
                "Yavaş Kontrol (Kp:1.0, Ki:0.2, Kd:0.03)"};

        new AlertDialog.Builder(this)
                .setTitle("PID Ön Ayarları")
                .setItems(presets, (dialog, which) -> {
                    switch (which) {
                        case 0: // Hassas
                            setPIDValues(1.5, 0.3, 0.05);
                            break;
                        case 1: // Normal
                            setPIDValues(2.0, 0.5, 0.1);
                            break;
                        case 2: // Hızlı
                            setPIDValues(3.0, 0.8, 0.2);
                            break;
                        case 3: // Yavaş
                            setPIDValues(1.0, 0.2, 0.03);
                            break;
                    }
                    Utils.showToast(this, "Ön ayar yüklendi: " + presets[which]);
                })
                .show();
    }

    private void setPIDValues(double kp, double ki, double kd) {
        pidSettings.setPidKp(kp);
        pidSettings.setPidKi(ki);
        pidSettings.setPidKd(kd);
        updateUI();
    }

    private void startAutoTune() {
        new AlertDialog.Builder(this)
                .setTitle("Otomatik Ayarlama")
                .setMessage("Otomatik PID ayarlama başlatılacak. Bu işlem birkaç dakika sürebilir. Devam edilsin mi?")
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    // PID modunu otomatik olarak ayarla ve başlat
                    pidSettings.setPidMode(Constants.PID_MODE_AUTO);
                    applyParameters();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void stopAutoTune() {
        // Otomatik ayarlamayı durdurmak için manuel moda geç
        pidSettings.setPidMode(Constants.PID_MODE_MANUAL);
        applyParameters();
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
                    updatePIDStatus(systemStatus);
                    updateAutoTuneStatus(systemStatus);
                }
            }
        }
    };
}