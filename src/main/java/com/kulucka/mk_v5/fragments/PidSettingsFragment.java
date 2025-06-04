package com.kulucka.mk_v5.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kulucka.mk_v5.R;
import com.kulucka.mk_v5.services.ApiService;

import org.json.JSONException;
import org.json.JSONObject;

public class PidSettingsFragment extends Fragment {

    // ESP32 Varsayılan PID Değerleri (ESP32 kodundan alınan değerler)
    private static final double DEFAULT_TEMP_KP = 2.0;
    private static final double DEFAULT_TEMP_KI = 0.5;
    private static final double DEFAULT_TEMP_KD = 1.0;

    private Switch swPidEnable;
    private RadioGroup rgPidMode;
    private RadioButton rbManualMode, rbAutoMode;
    private EditText etTempKp, etTempKi, etTempKd;
    private Button btnSavePidSettings;
    private Button btnLoadDefaults;
    private Button btnStartAutoTune;
    private Button btnRefreshData;
    private TextView tvPidStatus;
    private TextView tvAutoTuneStatus;

    private boolean isPidEnabled = false;
    private boolean isAutoTuneRunning = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pid_settings, container, false);

        initializeViews(view);
        setupEventListeners();
        loadCurrentSettings();

        return view;
    }

    private void initializeViews(View view) {
        swPidEnable = view.findViewById(R.id.sw_pid_enable);
        rgPidMode = view.findViewById(R.id.rg_pid_mode);
        rbManualMode = view.findViewById(R.id.rb_manual_mode);
        rbAutoMode = view.findViewById(R.id.rb_auto_mode);
        etTempKp = view.findViewById(R.id.et_temp_kp);
        etTempKi = view.findViewById(R.id.et_temp_ki);
        etTempKd = view.findViewById(R.id.et_temp_kd);
        btnSavePidSettings = view.findViewById(R.id.btn_save_pid_settings);
        btnLoadDefaults = view.findViewById(R.id.btn_load_defaults);
        btnStartAutoTune = view.findViewById(R.id.btn_start_auto_tune);
        btnRefreshData = view.findViewById(R.id.btn_refresh_data);
        tvPidStatus = view.findViewById(R.id.tv_pid_status);
        tvAutoTuneStatus = view.findViewById(R.id.tv_auto_tune_status);
    }

    private void setupEventListeners() {
        swPidEnable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) { // Sadece kullanıcı etkileşiminde
                enablePidControls(isChecked);
                updatePidEnabledOnESP32(isChecked);
            }
        });

        rgPidMode.setOnCheckedChangeListener((group, checkedId) -> {
            updateModeUI(checkedId);
        });

        btnSavePidSettings.setOnClickListener(v -> savePidSettings());
        btnLoadDefaults.setOnClickListener(v -> loadDefaultValues());
        btnStartAutoTune.setOnClickListener(v -> startAutoTune());
        btnRefreshData.setOnClickListener(v -> loadCurrentSettings());
    }

    private void loadCurrentSettings() {
        showToast("ESP32'den PID verileri yükleniyor...");

        ApiService.getInstance().getStatus(new ApiService.DataCallback() {
            @Override
            public void onSuccess(JSONObject data) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        try {
                            // ESP32'den gelen PID verilerini parse et
                            parsePidDataFromStatus(data);
                            showToast("PID ayarları ESP32'den yüklendi");

                        } catch (Exception e) {
                            showToast("PID verilerini okuma hatası: " + e.getMessage());
                            loadDefaultValues();
                        }
                    });
                }
            }

            @Override
            public void onError(String message) {
                showToast("ESP32 bağlantı hatası: " + message);
                loadDefaultValues();
            }
        });
    }

    private void parsePidDataFromStatus(JSONObject data) {
        try {
            // ESP32'den gelen genel durum verilerinden PID bilgilerini çıkar
            boolean pidEnabled = data.optBoolean("pidEnabled", false);

            // PID parametreleri ESP32 status'ta olmayabilir, varsayılanları kullan
            double kp = data.optDouble("pidKp", DEFAULT_TEMP_KP);
            double ki = data.optDouble("pidKi", DEFAULT_TEMP_KI);
            double kd = data.optDouble("pidKd", DEFAULT_TEMP_KD);

            String pidMode = data.optString("pidMode", "manual");
            boolean autoTuneActive = data.optBoolean("autoTuneActive", false);

            // UI'yi güncelle
            isPidEnabled = pidEnabled;
            swPidEnable.setChecked(pidEnabled);

            etTempKp.setText(String.valueOf(kp));
            etTempKi.setText(String.valueOf(ki));
            etTempKd.setText(String.valueOf(kd));

            if ("auto".equals(pidMode) || "autoTune".equals(pidMode)) {
                rbAutoMode.setChecked(true);
                updateModeUI(R.id.rb_auto_mode);
                isAutoTuneRunning = autoTuneActive;
            } else {
                rbManualMode.setChecked(true);
                updateModeUI(R.id.rb_manual_mode);
                isAutoTuneRunning = false;
            }

            enablePidControls(pidEnabled);
            updateAutoTuneStatus();
            updatePidStatusDisplay();

        } catch (Exception e) {
            loadDefaultValues();
        }
    }

    private void loadDefaultValues() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                etTempKp.setText(String.valueOf(DEFAULT_TEMP_KP));
                etTempKi.setText(String.valueOf(DEFAULT_TEMP_KI));
                etTempKd.setText(String.valueOf(DEFAULT_TEMP_KD));

                swPidEnable.setChecked(true);
                rbManualMode.setChecked(true);
                isPidEnabled = true;
                isAutoTuneRunning = false;

                enablePidControls(true);
                updateModeUI(R.id.rb_manual_mode);
                updatePidStatusDisplay();

                showToast("Varsayılan PID değerleri yüklendi");
            });
        }
    }

    private void enablePidControls(boolean enabled) {
        isPidEnabled = enabled;

        rgPidMode.setEnabled(enabled);
        rbManualMode.setEnabled(enabled);
        rbAutoMode.setEnabled(enabled);

        if (enabled) {
            updateModeUI(rgPidMode.getCheckedRadioButtonId());
        } else {
            etTempKp.setEnabled(false);
            etTempKi.setEnabled(false);
            etTempKd.setEnabled(false);
            btnSavePidSettings.setEnabled(false);
            btnLoadDefaults.setEnabled(false);
            btnStartAutoTune.setEnabled(false);
        }

        updatePidStatusDisplay();
    }

    private void updateModeUI(int checkedId) {
        boolean isManualMode = (checkedId == R.id.rb_manual_mode);
        boolean isAutoMode = (checkedId == R.id.rb_auto_mode);

        // Manuel mod parametreleri
        etTempKp.setEnabled(isManualMode && isPidEnabled);
        etTempKi.setEnabled(isManualMode && isPidEnabled);
        etTempKd.setEnabled(isManualMode && isPidEnabled);
        btnSavePidSettings.setEnabled(isManualMode && isPidEnabled);
        btnLoadDefaults.setEnabled(isManualMode && isPidEnabled);

        // Otomatik ayarlama butonu
        btnStartAutoTune.setEnabled(isAutoMode && isPidEnabled && !isAutoTuneRunning);

        // Görsel feedback
        float manualAlpha = (isManualMode && isPidEnabled) ? 1.0f : 0.5f;
        float autoAlpha = (isAutoMode && isPidEnabled) ? 1.0f : 0.5f;

        etTempKp.setAlpha(manualAlpha);
        etTempKi.setAlpha(manualAlpha);
        etTempKd.setAlpha(manualAlpha);
        btnSavePidSettings.setAlpha(manualAlpha);
        btnLoadDefaults.setAlpha(manualAlpha);
        btnStartAutoTune.setAlpha(autoAlpha);
    }

    private void updatePidEnabledOnESP32(boolean enabled) {
        ApiService.getInstance().setPidEnabled(enabled, new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                showToast("PID " + (enabled ? "etkinleştirildi" : "devre dışı bırakıldı"));
                updatePidStatusDisplay();
            }

            @Override
            public void onError(String message) {
                showToast("PID durumu değiştirilemedi: " + message);
                // Switch'i geri al
                swPidEnable.setChecked(!enabled);
                isPidEnabled = !enabled;
                enablePidControls(!enabled);
            }
        });
    }

    private void savePidSettings() {
        if (!isPidEnabled) {
            showToast("PID kapalı. Önce PID'yi açın.");
            return;
        }

        try {
            // Kullanıcı girişlerini doğrula
            String kpText = etTempKp.getText().toString().trim();
            String kiText = etTempKi.getText().toString().trim();
            String kdText = etTempKd.getText().toString().trim();

            if (kpText.isEmpty() || kiText.isEmpty() || kdText.isEmpty()) {
                showToast("Lütfen tüm PID değerlerini girin");
                return;
            }

            double tempKp = Double.parseDouble(kpText);
            double tempKi = Double.parseDouble(kiText);
            double tempKd = Double.parseDouble(kdText);

            if (tempKp < 0 || tempKp > 10 || tempKi < 0 || tempKi > 5 || tempKd < 0 || tempKd > 5) {
                showToast("PID değerleri geçerli aralıkta olmalı (Kp:0-10, Ki:0-5, Kd:0-5)");
                return;
            }

            showToast("PID ayarları ESP32'ye gönderiliyor...");

            // ESP32'ye PID parametrelerini gönder
            ApiService.getInstance().setPidParameters(tempKp, tempKi, tempKd, new ApiService.ParameterCallback() {
                @Override
                public void onSuccess() {
                    showToast("Manuel PID ayarları başarıyla kaydedildi");
                    // Ayarları tekrar yükle
                    loadCurrentSettings();
                }

                @Override
                public void onError(String message) {
                    showToast("PID ayarları kaydedilemedi: " + message);
                }
            });

        } catch (NumberFormatException e) {
            showToast("Lütfen geçerli sayısal değerler girin");
        }
    }

    private void startAutoTune() {
        if (!isPidEnabled) {
            showToast("PID kapalı. Önce PID'yi açın.");
            return;
        }

        try {
            // ESP32'ye otomatik PID ayarlama komutunu gönder
            JSONObject jsonData = new JSONObject();
            jsonData.put("pidEnabled", true);
            jsonData.put("pidMode", "autoTune");
            jsonData.put("action", "startAutoTune");

            showToast("Otomatik PID ayarlama başlatılıyor...");

            ApiService.getInstance().setParameter("api/pid", jsonData.toString(), new ApiService.ParameterCallback() {
                @Override
                public void onSuccess() {
                    isAutoTuneRunning = true;
                    showToast("PID otomatik ayarlama başlatıldı. Bu işlem 5-10 dakika sürecek.");

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            updateAutoTuneStatus();
                            updateModeUI(R.id.rb_auto_mode);

                            // 2 dakika sonra durumu kontrol et
                            new android.os.Handler().postDelayed(() -> {
                                if (getActivity() != null) {
                                    loadCurrentSettings(); // Güncel verileri yükle
                                }
                            }, 120000); // 2 dakika
                        });
                    }
                }

                @Override
                public void onError(String message) {
                    isAutoTuneRunning = false;
                    showToast("Otomatik ayarlama başlatılamadı: " + message);
                    updateAutoTuneStatus();
                }
            });

        } catch (JSONException e) {
            showToast("Veri formatı hatası: " + e.getMessage());
        }
    }

    private void updatePidStatusDisplay() {
        if (tvPidStatus != null) {
            String status = isPidEnabled ? "PID Kontrolü: AÇIK" : "PID Kontrolü: KAPALI";
            tvPidStatus.setText(status);
            int colorRes = isPidEnabled ? android.R.color.holo_green_dark : android.R.color.holo_red_dark;
            tvPidStatus.setTextColor(getResources().getColor(colorRes));
        }
    }

    private void updateAutoTuneStatus() {
        if (tvAutoTuneStatus != null) {
            if (isAutoTuneRunning) {
                tvAutoTuneStatus.setText("Otomatik Ayarlama: DEVAM EDİYOR");
                tvAutoTuneStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
                tvAutoTuneStatus.setVisibility(View.VISIBLE);
            } else {
                tvAutoTuneStatus.setVisibility(View.GONE);
            }
        }

        if (btnStartAutoTune != null) {
            if (isAutoTuneRunning) {
                btnStartAutoTune.setText("Otomatik Ayarlama Devam Ediyor...");
                btnStartAutoTune.setEnabled(false);
            } else {
                btnStartAutoTune.setText("Otomatik Ayarlamayı Başlat");
                btnStartAutoTune.setEnabled(isPidEnabled && rbAutoMode.isChecked());
            }
        }
    }

    private void showToast(final String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Fragment görünür olduğunda güncel verileri yükle
        loadCurrentSettings();
    }
}