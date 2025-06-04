package com.kulucka.mk_v5.fragments;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kulucka.mk_v5.R;
import com.kulucka.mk_v5.services.ApiService;

import org.json.JSONObject;

import java.util.Locale;

public class PidSettingsFragment extends Fragment {

    private static final String TAG = "PidSettings";

    // UI Components - Sıcaklık PID Ayarları
    private EditText etTempKp;
    private EditText etTempKi;
    private EditText etTempKd;
    private SeekBar sbTempKp;
    private SeekBar sbTempKi;
    private SeekBar sbTempKd;
    private TextView tvTempPidStatus;

    // UI Components - Nem PID Ayarları
    private EditText etHumidKp;
    private EditText etHumidKi;
    private EditText etHumidKd;
    private SeekBar sbHumidKp;
    private SeekBar sbHumidKi;
    private SeekBar sbHumidKd;
    private TextView tvHumidPidStatus;

    // UI Components - PID Durumu ve Performans
    private TextView tvTempError;
    private TextView tvTempOutput;
    private TextView tvTempSetpoint;
    private TextView tvTempCurrent;
    private ProgressBar pbTempPerformance;

    private TextView tvHumidError;
    private TextView tvHumidOutput;
    private TextView tvHumidSetpoint;
    private TextView tvHumidCurrent;
    private ProgressBar pbHumidPerformance;

    // UI Components - Genel PID Ayarları
    private Switch switchPidEnabled;
    private Switch switchAutoTuning;
    private TextView tvPidSystemStatus;
    private TextView tvLastTuneTime;

    // UI Components - Kontrol Butonları
    private Button btnSavePidSettings;
    private Button btnLoadPidSettings;
    private Button btnResetPidDefaults;
    private Button btnStartAutoTune;
    private Button btnStopAutoTune;
    private Button btnTestPidResponse;

    // UI Components - Gelişmiş Ayarlar
    private EditText etSampleTime;
    private EditText etOutputMin;
    private EditText etOutputMax;
    private Switch switchReverseDirection;

    // Veri yenileme
    private Handler refreshHandler;
    private boolean isFragmentActive = false;
    private static final long REFRESH_INTERVAL = 3000; // 3 saniye (PID için daha sık)

    // PID varsayılan değerleri - ESP32'deki değerlerle eşleşmeli
    private static final double DEFAULT_TEMP_KP = 2.0;
    private static final double DEFAULT_TEMP_KI = 0.1;
    private static final double DEFAULT_TEMP_KD = 0.5;
    private static final double DEFAULT_HUMID_KP = 1.5;
    private static final double DEFAULT_HUMID_KI = 0.05;
    private static final double DEFAULT_HUMID_KD = 0.3;

    // SeekBar ölçekleme faktörleri
    private static final int KP_SCALE = 100; // 0.00 - 10.00 aralığı için
    private static final int KI_SCALE = 1000; // 0.000 - 1.000 aralığı için
    private static final int KD_SCALE = 100; // 0.00 - 5.00 aralığı için

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pid_settings, container, false);
        initializeViews(view);
        setupEventListeners();
        setupRefreshHandler();
        loadCurrentPidSettings();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        isFragmentActive = true;
        startPeriodicRefresh();
    }

    @Override
    public void onPause() {
        super.onPause();
        isFragmentActive = false;
        stopPeriodicRefresh();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopPeriodicRefresh();
        if (refreshHandler != null) {
            refreshHandler.removeCallbacksAndMessages(null);
        }
    }

    private void initializeViews(View view) {
        // Sıcaklık PID
        etTempKp = view.findViewById(R.id.et_temp_kp);
        etTempKi = view.findViewById(R.id.et_temp_ki);
        etTempKd = view.findViewById(R.id.et_temp_kd);
        sbTempKp = view.findViewById(R.id.sb_temp_kp);
        sbTempKi = view.findViewById(R.id.sb_temp_ki);
        sbTempKd = view.findViewById(R.id.sb_temp_kd);
        tvTempPidStatus = view.findViewById(R.id.tv_temp_pid_status);

        // Nem PID
        etHumidKp = view.findViewById(R.id.et_humid_kp);
        etHumidKi = view.findViewById(R.id.et_humid_ki);
        etHumidKd = view.findViewById(R.id.et_humid_kd);
        sbHumidKp = view.findViewById(R.id.sb_humid_kp);
        sbHumidKi = view.findViewById(R.id.sb_humid_ki);
        sbHumidKd = view.findViewById(R.id.sb_humid_kd);
        tvHumidPidStatus = view.findViewById(R.id.tv_humid_pid_status);

        // PID durumu
        tvTempError = view.findViewById(R.id.tv_temp_error);
        tvTempOutput = view.findViewById(R.id.tv_temp_output);
        tvTempSetpoint = view.findViewById(R.id.tv_temp_setpoint);
        tvTempCurrent = view.findViewById(R.id.tv_temp_current);
        pbTempPerformance = view.findViewById(R.id.pb_temp_performance);

        tvHumidError = view.findViewById(R.id.tv_humid_error);
        tvHumidOutput = view.findViewById(R.id.tv_humid_output);
        tvHumidSetpoint = view.findViewById(R.id.tv_humid_setpoint);
        tvHumidCurrent = view.findViewById(R.id.tv_humid_current);
        pbHumidPerformance = view.findViewById(R.id.pb_humid_performance);

        // Genel PID
        switchPidEnabled = view.findViewById(R.id.switch_pid_enabled);
        switchAutoTuning = view.findViewById(R.id.switch_auto_tuning);
        tvPidSystemStatus = view.findViewById(R.id.tv_pid_system_status);
        tvLastTuneTime = view.findViewById(R.id.tv_last_tune_time);

        // Kontrol butonları
        btnSavePidSettings = view.findViewById(R.id.btn_save_pid_settings);
        btnLoadPidSettings = view.findViewById(R.id.btn_load_pid_settings);
        btnResetPidDefaults = view.findViewById(R.id.btn_reset_pid_defaults);
        btnStartAutoTune = view.findViewById(R.id.btn_start_auto_tune);
        btnStopAutoTune = view.findViewById(R.id.btn_stop_auto_tune);
        btnTestPidResponse = view.findViewById(R.id.btn_test_pid_response);

        // Gelişmiş ayarlar
        etSampleTime = view.findViewById(R.id.et_sample_time);
        etOutputMin = view.findViewById(R.id.et_output_min);
        etOutputMax = view.findViewById(R.id.et_output_max);
        switchReverseDirection = view.findViewById(R.id.switch_reverse_direction);

        setDefaultValues();
        setupSeekBars();
    }

    private void setDefaultValues() {
        // Varsayılan PID değerleri
        if (etTempKp != null) etTempKp.setText(String.format(Locale.getDefault(), "%.2f", DEFAULT_TEMP_KP));
        if (etTempKi != null) etTempKi.setText(String.format(Locale.getDefault(), "%.3f", DEFAULT_TEMP_KI));
        if (etTempKd != null) etTempKd.setText(String.format(Locale.getDefault(), "%.2f", DEFAULT_TEMP_KD));

        if (etHumidKp != null) etHumidKp.setText(String.format(Locale.getDefault(), "%.2f", DEFAULT_HUMID_KP));
        if (etHumidKi != null) etHumidKi.setText(String.format(Locale.getDefault(), "%.3f", DEFAULT_HUMID_KI));
        if (etHumidKd != null) etHumidKd.setText(String.format(Locale.getDefault(), "%.2f", DEFAULT_HUMID_KD));

        // Gelişmiş ayarlar
        if (etSampleTime != null) etSampleTime.setText("1000"); // 1 saniye
        if (etOutputMin != null) etOutputMin.setText("0");
        if (etOutputMax != null) etOutputMax.setText("100");

        // Durum bilgileri
        if (tvTempPidStatus != null) tvTempPidStatus.setText("PID Durumu Bilinmiyor");
        if (tvHumidPidStatus != null) tvHumidPidStatus.setText("PID Durumu Bilinmiyor");
        if (tvPidSystemStatus != null) tvPidSystemStatus.setText("Sistem durumu kontrol ediliyor...");
    }

    private void setupSeekBars() {
        // Sıcaklık PID SeekBar'ları
        if (sbTempKp != null) {
            sbTempKp.setMax(1000); // 0-10.00 aralığı
            sbTempKp.setProgress((int)(DEFAULT_TEMP_KP * KP_SCALE));
        }
        if (sbTempKi != null) {
            sbTempKi.setMax(1000); // 0-1.000 aralığı
            sbTempKi.setProgress((int)(DEFAULT_TEMP_KI * KI_SCALE));
        }
        if (sbTempKd != null) {
            sbTempKd.setMax(500); // 0-5.00 aralığı
            sbTempKd.setProgress((int)(DEFAULT_TEMP_KD * KD_SCALE));
        }

        // Nem PID SeekBar'ları
        if (sbHumidKp != null) {
            sbHumidKp.setMax(1000);
            sbHumidKp.setProgress((int)(DEFAULT_HUMID_KP * KP_SCALE));
        }
        if (sbHumidKi != null) {
            sbHumidKi.setMax(1000);
            sbHumidKi.setProgress((int)(DEFAULT_HUMID_KI * KI_SCALE));
        }
        if (sbHumidKd != null) {
            sbHumidKd.setMax(500);
            sbHumidKd.setProgress((int)(DEFAULT_HUMID_KD * KD_SCALE));
        }
    }

    private void setupEventListeners() {
        // EditText ile SeekBar senkronizasyonu - Sıcaklık
        setupEditTextSeekBarSync(etTempKp, sbTempKp, KP_SCALE, 2);
        setupEditTextSeekBarSync(etTempKi, sbTempKi, KI_SCALE, 3);
        setupEditTextSeekBarSync(etTempKd, sbTempKd, KD_SCALE, 2);

        // EditText ile SeekBar senkronizasyonu - Nem
        setupEditTextSeekBarSync(etHumidKp, sbHumidKp, KP_SCALE, 2);
        setupEditTextSeekBarSync(etHumidKi, sbHumidKi, KI_SCALE, 3);
        setupEditTextSeekBarSync(etHumidKd, sbHumidKd, KD_SCALE, 2);

        // PID enable/disable
        if (switchPidEnabled != null) {
            switchPidEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
                updatePidControlsState(isChecked);
            });
        }

        // Auto tuning
        if (switchAutoTuning != null) {
            switchAutoTuning.setOnCheckedChangeListener((buttonView, isChecked) -> {
                updateAutoTuningState(isChecked);
            });
        }

        // Buton event'leri
        if (btnSavePidSettings != null) {
            btnSavePidSettings.setOnClickListener(v -> savePidSettings());
        }

        if (btnLoadPidSettings != null) {
            btnLoadPidSettings.setOnClickListener(v -> loadCurrentPidSettings());
        }

        if (btnResetPidDefaults != null) {
            btnResetPidDefaults.setOnClickListener(v -> showResetDialog());
        }

        if (btnStartAutoTune != null) {
            btnStartAutoTune.setOnClickListener(v -> showAutoTuneDialog());
        }

        if (btnStopAutoTune != null) {
            btnStopAutoTune.setOnClickListener(v -> stopAutoTuning());
        }

        if (btnTestPidResponse != null) {
            btnTestPidResponse.setOnClickListener(v -> testPidResponse());
        }
    }

    private void setupEditTextSeekBarSync(EditText editText, SeekBar seekBar, int scale, int decimals) {
        if (editText == null || seekBar == null) return;

        // EditText değiştiğinde SeekBar'ı güncelle
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    if (!s.toString().isEmpty()) {
                        double value = Double.parseDouble(s.toString());
                        int progress = (int)(value * scale);
                        if (progress != seekBar.getProgress()) {
                            seekBar.setProgress(Math.max(0, Math.min(seekBar.getMax(), progress)));
                        }
                    }
                } catch (NumberFormatException e) {
                    // Geçersiz sayı, SeekBar'ı değiştirme
                }
            }
        });

        // SeekBar değiştiğinde EditText'i güncelle
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    double value = (double) progress / scale;
                    String formatStr = "%." + decimals + "f";
                    String valueStr = String.format(Locale.getDefault(), formatStr, value);
                    if (!editText.getText().toString().equals(valueStr)) {
                        editText.setText(valueStr);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void updatePidControlsState(boolean enabled) {
        // PID kontrollerini etkinleştir/devre dışı bırak
        enablePidControls(enabled);

        if (tvPidSystemStatus != null) {
            tvPidSystemStatus.setText(enabled ? "PID Kontrolü Aktif" : "PID Kontrolü Devre Dışı");
            tvPidSystemStatus.setTextColor(getResources().getColor(
                    enabled ? android.R.color.holo_green_dark : android.R.color.holo_red_dark
            ));
        }
    }

    private void updateAutoTuningState(boolean enabled) {
        if (btnStartAutoTune != null) {
            btnStartAutoTune.setVisibility(enabled ? View.GONE : View.VISIBLE);
        }
        if (btnStopAutoTune != null) {
            btnStopAutoTune.setVisibility(enabled ? View.VISIBLE : View.GONE);
        }

        // Auto tuning aktifken manuel PID ayarlarını devre dışı bırak
        enablePidControls(!enabled && (switchPidEnabled != null && switchPidEnabled.isChecked()));
    }

    private void enablePidControls(boolean enabled) {
        // Sıcaklık PID kontrollerini etkinleştir/devre dışı bırak
        if (etTempKp != null) etTempKp.setEnabled(enabled);
        if (etTempKi != null) etTempKi.setEnabled(enabled);
        if (etTempKd != null) etTempKd.setEnabled(enabled);
        if (sbTempKp != null) sbTempKp.setEnabled(enabled);
        if (sbTempKi != null) sbTempKi.setEnabled(enabled);
        if (sbTempKd != null) sbTempKd.setEnabled(enabled);

        // Nem PID kontrollerini etkinleştir/devre dışı bırak
        if (etHumidKp != null) etHumidKp.setEnabled(enabled);
        if (etHumidKi != null) etHumidKi.setEnabled(enabled);
        if (etHumidKd != null) etHumidKd.setEnabled(enabled);
        if (sbHumidKp != null) sbHumidKp.setEnabled(enabled);
        if (sbHumidKi != null) sbHumidKi.setEnabled(enabled);
        if (sbHumidKd != null) sbHumidKd.setEnabled(enabled);

        // Gelişmiş ayarları etkinleştir/devre dışı bırak
        if (etSampleTime != null) etSampleTime.setEnabled(enabled);
        if (etOutputMin != null) etOutputMin.setEnabled(enabled);
        if (etOutputMax != null) etOutputMax.setEnabled(enabled);
        if (switchReverseDirection != null) switchReverseDirection.setEnabled(enabled);
    }

    private void setupRefreshHandler() {
        refreshHandler = new Handler();
    }

    private void startPeriodicRefresh() {
        if (refreshHandler != null && isFragmentActive) {
            refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
        }
    }

    private void stopPeriodicRefresh() {
        if (refreshHandler != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (isFragmentActive) {
                loadCurrentPidSettings();
                refreshHandler.postDelayed(this, REFRESH_INTERVAL);
            }
        }
    };

    private void loadCurrentPidSettings() {
        // ESP32'den güncel PID ayarlarını al
        ApiService.getInstance().getPidSettings(new ApiService.DataCallback() {
            @Override
            public void onSuccess(JSONObject data) {
                if (isFragmentActive && getActivity() != null) {
                    getActivity().runOnUiThread(() -> updateUIWithPidData(data));
                }
            }

            @Override
            public void onError(String message) {
                if (isFragmentActive && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Log.w(TAG, "PID ayarları yüklenemedi: " + message);
                    });
                }
            }
        });
    }

    private void updateUIWithPidData(JSONObject data) {
        try {
            // PID genel ayarları
            boolean pidEnabled = data.optBoolean("pidEnabled", true);
            boolean autoTuning = data.optBoolean("autoTuning", false);
            long lastTuneTime = data.optLong("lastTuneTime", 0);

            // Sıcaklık PID ayarları
            double tempKp = data.optDouble("tempKp", DEFAULT_TEMP_KP);
            double tempKi = data.optDouble("tempKi", DEFAULT_TEMP_KI);
            double tempKd = data.optDouble("tempKd", DEFAULT_TEMP_KD);

            // Nem PID ayarları
            double humidKp = data.optDouble("humidKp", DEFAULT_HUMID_KP);
            double humidKi = data.optDouble("humidKi", DEFAULT_HUMID_KI);
            double humidKd = data.optDouble("humidKd", DEFAULT_HUMID_KD);

            // PID performans verileri
            double tempError = data.optDouble("tempError", 0.0);
            double tempOutput = data.optDouble("tempOutput", 0.0);
            double tempSetpoint = data.optDouble("tempSetpoint", 37.5);
            double tempCurrent = data.optDouble("tempCurrent", 0.0);

            double humidError = data.optDouble("humidError", 0.0);
            double humidOutput = data.optDouble("humidOutput", 0.0);
            double humidSetpoint = data.optDouble("humidSetpoint", 60.0);
            double humidCurrent = data.optDouble("humidCurrent", 0.0);

            // Gelişmiş ayarlar
            int sampleTime = data.optInt("sampleTime", 1000);
            double outputMin = data.optDouble("outputMin", 0.0);
            double outputMax = data.optDouble("outputMax", 100.0);
            boolean reverseDirection = data.optBoolean("reverseDirection", false);

            // UI'yi güncelle
            updateGeneralPidSettings(pidEnabled, autoTuning, lastTuneTime);
            updateTemperaturePidSettings(tempKp, tempKi, tempKd);
            updateHumidityPidSettings(humidKp, humidKi, humidKd);
            updatePidPerformance(tempError, tempOutput, tempSetpoint, tempCurrent,
                    humidError, humidOutput, humidSetpoint, humidCurrent);
            updateAdvancedSettings(sampleTime, outputMin, outputMax, reverseDirection);

        } catch (Exception e) {
            Log.e(TAG, "PID verileri işleme hatası: " + e.getMessage());
        }
    }

    private void updateGeneralPidSettings(boolean enabled, boolean autoTuning, long lastTuneTime) {
        if (switchPidEnabled != null) {
            switchPidEnabled.setChecked(enabled);
        }
        if (switchAutoTuning != null) {
            switchAutoTuning.setChecked(autoTuning);
        }

        updatePidControlsState(enabled);
        updateAutoTuningState(autoTuning);

        if (tvLastTuneTime != null && lastTuneTime > 0) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            tvLastTuneTime.setText("Son ayarlama: " + sdf.format(new java.util.Date(lastTuneTime)));
        } else if (tvLastTuneTime != null) {
            tvLastTuneTime.setText("Otomatik ayarlama yapılmamış");
        }
    }

    private void updateTemperaturePidSettings(double kp, double ki, double kd) {
        if (etTempKp != null) {
            etTempKp.setText(String.format(Locale.getDefault(), "%.2f", kp));
        }
        if (etTempKi != null) {
            etTempKi.setText(String.format(Locale.getDefault(), "%.3f", ki));
        }
        if (etTempKd != null) {
            etTempKd.setText(String.format(Locale.getDefault(), "%.2f", kd));
        }

        // SeekBar'ları güncelle
        if (sbTempKp != null) {
            sbTempKp.setProgress((int)(kp * KP_SCALE));
        }
        if (sbTempKi != null) {
            sbTempKi.setProgress((int)(ki * KI_SCALE));
        }
        if (sbTempKd != null) {
            sbTempKd.setProgress((int)(kd * KD_SCALE));
        }
    }

    private void updateHumidityPidSettings(double kp, double ki, double kd) {
        if (etHumidKp != null) {
            etHumidKp.setText(String.format(Locale.getDefault(), "%.2f", kp));
        }
        if (etHumidKi != null) {
            etHumidKi.setText(String.format(Locale.getDefault(), "%.3f", ki));
        }
        if (etHumidKd != null) {
            etHumidKd.setText(String.format(Locale.getDefault(), "%.2f", kd));
        }

        // SeekBar'ları güncelle
        if (sbHumidKp != null) {
            sbHumidKp.setProgress((int)(kp * KP_SCALE));
        }
        if (sbHumidKi != null) {
            sbHumidKi.setProgress((int)(ki * KI_SCALE));
        }
        if (sbHumidKd != null) {
            sbHumidKd.setProgress((int)(kd * KD_SCALE));
        }
    }

    private void updatePidPerformance(double tempError, double tempOutput, double tempSetpoint, double tempCurrent,
                                      double humidError, double humidOutput, double humidSetpoint, double humidCurrent) {
        // Sıcaklık performansı
        if (tvTempError != null) {
            tvTempError.setText(String.format(Locale.getDefault(), "Hata: %.2f°C", tempError));
            tvTempError.setTextColor(Math.abs(tempError) > 1.0 ? Color.RED : Color.GREEN);
        }
        if (tvTempOutput != null) {
            tvTempOutput.setText(String.format(Locale.getDefault(), "Çıkış: %.1f%%", tempOutput));
        }
        if (tvTempSetpoint != null) {
            tvTempSetpoint.setText(String.format(Locale.getDefault(), "Hedef: %.1f°C", tempSetpoint));
        }
        if (tvTempCurrent != null) {
            tvTempCurrent.setText(String.format(Locale.getDefault(), "Güncel: %.1f°C", tempCurrent));
        }

        // Nem performansı
        if (tvHumidError != null) {
            tvHumidError.setText(String.format(Locale.getDefault(), "Hata: %.1f%%", humidError));
            tvHumidError.setTextColor(Math.abs(humidError) > 5.0 ? Color.RED : Color.GREEN);
        }
        if (tvHumidOutput != null) {
            tvHumidOutput.setText(String.format(Locale.getDefault(), "Çıkış: %.1f%%", humidOutput));
        }
        if (tvHumidSetpoint != null) {
            tvHumidSetpoint.setText(String.format(Locale.getDefault(), "Hedef: %.1f%%", humidSetpoint));
        }
        if (tvHumidCurrent != null) {
            tvHumidCurrent.setText(String.format(Locale.getDefault(), "Güncel: %.1f%%", humidCurrent));
        }

        // Performans çubukları
        if (pbTempPerformance != null) {
            int tempPerf = calculatePerformanceScore(Math.abs(tempError), 2.0); // 2°C max hata
            pbTempPerformance.setProgress(tempPerf);
        }
        if (pbHumidPerformance != null) {
            int humidPerf = calculatePerformanceScore(Math.abs(humidError), 10.0); // 10% max hata
            pbHumidPerformance.setProgress(humidPerf);
        }

        // PID durum metinleri
        updatePidStatusTexts(Math.abs(tempError), Math.abs(humidError));
    }

    private int calculatePerformanceScore(double error, double maxError) {
        if (error <= maxError * 0.1) return 100; // Mükemmel
        else if (error <= maxError * 0.3) return 80; // İyi
        else if (error <= maxError * 0.5) return 60; // Orta
        else if (error <= maxError) return 40; // Zayıf
        else return 20; // Kötü
    }

    private void updatePidStatusTexts(double tempError, double humidError) {
        // Sıcaklık PID durumu
        if (tvTempPidStatus != null) {
            String tempStatus;
            if (tempError <= 0.2) tempStatus = "Mükemmel";
            else if (tempError <= 0.5) tempStatus = "İyi";
            else if (tempError <= 1.0) tempStatus = "Orta";
            else if (tempError <= 2.0) tempStatus = "Ayar Gerekli";
            else tempStatus = "Kötü - Ayar Şart";

            tvTempPidStatus.setText("Sıcaklık PID: " + tempStatus);
        }

        // Nem PID durumu
        if (tvHumidPidStatus != null) {
            String humidStatus;
            if (humidError <= 2.0) humidStatus = "Mükemmel";
            else if (humidError <= 5.0) humidStatus = "İyi";
            else if (humidError <= 8.0) humidStatus = "Orta";
            else if (humidError <= 15.0) humidStatus = "Ayar Gerekli";
            else humidStatus = "Kötü - Ayar Şart";

            tvHumidPidStatus.setText("Nem PID: " + humidStatus);
        }
    }

    private void updateAdvancedSettings(int sampleTime, double outputMin, double outputMax, boolean reverseDirection) {
        if (etSampleTime != null) {
            etSampleTime.setText(String.valueOf(sampleTime));
        }
        if (etOutputMin != null) {
            etOutputMin.setText(String.format(Locale.getDefault(), "%.1f", outputMin));
        }
        if (etOutputMax != null) {
            etOutputMax.setText(String.format(Locale.getDefault(), "%.1f", outputMax));
        }
        if (switchReverseDirection != null) {
            switchReverseDirection.setChecked(reverseDirection);
        }
    }

    private void savePidSettings() {
        if (!validatePidInputs()) {
            return;
        }

        try {
            JSONObject params = new JSONObject();

            // Genel ayarlar
            params.put("pidEnabled", switchPidEnabled.isChecked());
            params.put("autoTuning", switchAutoTuning.isChecked());

            // Sıcaklık PID
            params.put("tempKp", Double.parseDouble(etTempKp.getText().toString()));
            params.put("tempKi", Double.parseDouble(etTempKi.getText().toString()));
            params.put("tempKd", Double.parseDouble(etTempKd.getText().toString()));

            // Nem PID
            params.put("humidKp", Double.parseDouble(etHumidKp.getText().toString()));
            params.put("humidKi", Double.parseDouble(etHumidKi.getText().toString()));
            params.put("humidKd", Double.parseDouble(etHumidKd.getText().toString()));

            // Gelişmiş ayarlar
            params.put("sampleTime", Integer.parseInt(etSampleTime.getText().toString()));
            params.put("outputMin", Double.parseDouble(etOutputMin.getText().toString()));
            params.put("outputMax", Double.parseDouble(etOutputMax.getText().toString()));
            params.put("reverseDirection", switchReverseDirection.isChecked());

            // ESP32'ye PID ayarlarını gönder
            ApiService.getInstance().updatePidSettings(params, new ApiService.ParameterCallback() {
                @Override
                public void onSuccess() {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showToast("PID ayarları kaydedildi");
                        });
                    }
                }

                @Override
                public void onError(String message) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showToast("PID ayarları kaydedilemedi: " + message);
                        });
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "PID ayarları kaydetme hatası: " + e.getMessage());
            showToast("Parametre hatası: " + e.getMessage());
        }
    }

    private void showResetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Varsayılan PID Ayarları")
                .setMessage("PID ayarlarını varsayılan değerlere sıfırlamak istediğinizden emin misiniz?\n\n" +
                        "Bu işlem mevcut ayarlarınızı kaybettirecektir.")
                .setPositiveButton("Sıfırla", (dialog, which) -> resetToDefaults())
                .setNegativeButton("İptal", null)
                .show();
    }

    private void showAutoTuneDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Otomatik PID Ayarlama")
                .setMessage("Otomatik ayarlama işlemi 10-30 dakika sürebilir ve bu süre boyunca sistem kararsız çalışabilir.\n\n" +
                        "Devam etmek istediğinizden emin misiniz?")
                .setPositiveButton("Başlat", (dialog, which) -> startAutoTuning())
                .setNegativeButton("İptal", null)
                .show();
    }

    private void resetToDefaults() {
        // UI'yi varsayılan değerlere sıfırla
        setDefaultValues();
        setupSeekBars();

        switchPidEnabled.setChecked(true);
        switchAutoTuning.setChecked(false);
        switchReverseDirection.setChecked(false);

        updatePidControlsState(true);
        updateAutoTuningState(false);

        showToast("Varsayılan PID ayarları yüklendi");
    }

    private void startAutoTuning() {
        // ESP32'de otomatik PID ayarlama başlat
        ApiService.getInstance().startPidAutoTune(new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Otomatik PID ayarlama başlatıldı");
                        if (switchAutoTuning != null) {
                            switchAutoTuning.setChecked(true);
                        }
                        updateAutoTuningState(true);
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Otomatik ayarlama başlatılamadı: " + message);
                    });
                }
            }
        });
    }

    private void stopAutoTuning() {
        // ESP32'de otomatik PID ayarlamayı durdur
        ApiService.getInstance().stopPidAutoTune(new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Otomatik PID ayarlama durduruldu");
                        if (switchAutoTuning != null) {
                            switchAutoTuning.setChecked(false);
                        }
                        updateAutoTuningState(false);
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Otomatik ayarlama durdurulamadı: " + message);
                    });
                }
            }
        });
    }

    private void testPidResponse() {
        // ESP32'de PID tepki testi başlat
        ApiService.getInstance().testPidResponse(new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("PID tepki testi başlatıldı - 5 dakika sürecek");
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("PID testi başlatılamadı: " + message);
                    });
                }
            }
        });
    }

    private boolean validatePidInputs() {
        // Sıcaklık PID değerleri
        if (!validatePidValue(etTempKp, "Sıcaklık Kp", 0.0, 10.0)) return false;
        if (!validatePidValue(etTempKi, "Sıcaklık Ki", 0.0, 1.0)) return false;
        if (!validatePidValue(etTempKd, "Sıcaklık Kd", 0.0, 5.0)) return false;

        // Nem PID değerleri
        if (!validatePidValue(etHumidKp, "Nem Kp", 0.0, 10.0)) return false;
        if (!validatePidValue(etHumidKi, "Nem Ki", 0.0, 1.0)) return false;
        if (!validatePidValue(etHumidKd, "Nem Kd", 0.0, 5.0)) return false;

        // Gelişmiş ayarlar
        if (!validateIntegerValue(etSampleTime, "Örnekleme Süresi", 100, 10000)) return false;
        if (!validatePidValue(etOutputMin, "Minimum Çıkış", 0.0, 100.0)) return false;
        if (!validatePidValue(etOutputMax, "Maksimum Çıkış", 0.0, 100.0)) return false;

        // Output min < max kontrolü
        try {
            double outputMin = Double.parseDouble(etOutputMin.getText().toString());
            double outputMax = Double.parseDouble(etOutputMax.getText().toString());
            if (outputMin >= outputMax) {
                showToast("Minimum çıkış değeri maksimumdan küçük olmalı");
                return false;
            }
        } catch (NumberFormatException e) {
            showToast("Çıkış değerleri geçersiz");
            return false;
        }

        return true;
    }

    private boolean validatePidValue(EditText editText, String fieldName, double min, double max) {
        try {
            String valueStr = editText.getText().toString();
            if (valueStr.isEmpty()) {
                editText.setError(fieldName + " boş olamaz");
                return false;
            }
            double value = Double.parseDouble(valueStr);
            if (value < min || value > max) {
                editText.setError(fieldName + " " + min + "-" + max + " aralığında olmalı");
                return false;
            }
            editText.setError(null);
            return true;
        } catch (NumberFormatException e) {
            editText.setError("Geçerli bir sayı girin");
            return false;
        }
    }

    private boolean validateIntegerValue(EditText editText, String fieldName, int min, int max) {
        try {
            String valueStr = editText.getText().toString();
            if (valueStr.isEmpty()) {
                editText.setError(fieldName + " boş olamaz");
                return false;
            }
            int value = Integer.parseInt(valueStr);
            if (value < min || value > max) {
                editText.setError(fieldName + " " + min + "-" + max + " aralığında olmalı");
                return false;
            }
            editText.setError(null);
            return true;
        } catch (NumberFormatException e) {
            editText.setError("Geçerli bir tam sayı girin");
            return false;
        }
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}