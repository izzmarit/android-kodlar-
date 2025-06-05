package com.kulucka.mk_v5.fragments;

import android.app.AlertDialog;
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

public class AlarmSettingsFragment extends Fragment {

    private static final String TAG = "AlarmSettings";

    // UI Components - Genel Alarm Ayarları
    private Switch switchAlarmEnabled;
    private TextView tvAlarmStatus;

    // UI Components - Sıcaklık Alarm Ayarları
    private EditText etTempLowAlarm;
    private EditText etTempHighAlarm;
    private TextView tvTempAlarmRange;
    private TextView tvCurrentTemp;
    private TextView tvTargetTemp;

    // UI Components - Nem Alarm Ayarları
    private EditText etHumidLowAlarm;
    private EditText etHumidHighAlarm;
    private TextView tvHumidityAlarmRange;
    private TextView tvCurrentHumidity;
    private TextView tvTargetHumidity;

    // UI Components - Alarm Durumu
    private View tempAlarmIndicator;
    private View humidityAlarmIndicator;
    private TextView tvTempAlarmStatus;
    private TextView tvHumidityAlarmStatus;
    private Button btnDismissTempAlarm;
    private Button btnDismissHumidityAlarm;
    private Button btnDismissAllAlarms;

    // UI Components - Kontrol Butonları
    private Button btnSaveAlarmSettings;
    private Button btnLoadAlarmSettings;
    private Button btnResetAlarmDefaults;
    private Button btnTestAlarms;

    // Veri yenileme
    private Handler refreshHandler;
    private boolean isFragmentActive = false;
    private static final long REFRESH_INTERVAL = 5000; // 5 saniye (alarmlar için daha sık)

    // Güncel veriler
    private double currentTemp = 0.0;
    private double currentHumidity = 0.0;
    private double targetTemp = 37.5;
    private double targetHumidity = 60.0;
    private boolean tempAlarmActive = false;
    private boolean humidityAlarmActive = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alarm_settings, container, false);
        initializeViews(view);
        setupEventListeners();
        setupRefreshHandler();
        loadCurrentSettings();
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
        // Genel alarm ayarları
        switchAlarmEnabled = view.findViewById(R.id.switch_alarm_enabled);
        tvAlarmStatus = view.findViewById(R.id.tv_alarm_status);

        // Sıcaklık alarm ayarları
        etTempLowAlarm = view.findViewById(R.id.et_temp_low_alarm);
        etTempHighAlarm = view.findViewById(R.id.et_temp_high_alarm);
        tvTempAlarmRange = view.findViewById(R.id.tv_temp_alarm_range);
        tvCurrentTemp = view.findViewById(R.id.tv_current_temp);
        tvTargetTemp = view.findViewById(R.id.tv_target_temp);

        // Nem alarm ayarları
        etHumidLowAlarm = view.findViewById(R.id.et_humid_low_alarm);
        etHumidHighAlarm = view.findViewById(R.id.et_humid_high_alarm);
        tvHumidityAlarmRange = view.findViewById(R.id.tv_humidity_alarm_range);
        tvCurrentHumidity = view.findViewById(R.id.tv_current_humidity);
        tvTargetHumidity = view.findViewById(R.id.tv_target_humidity);

        // Alarm durumu
        tempAlarmIndicator = view.findViewById(R.id.temp_alarm_indicator);
        humidityAlarmIndicator = view.findViewById(R.id.humidity_alarm_indicator);
        tvTempAlarmStatus = view.findViewById(R.id.tv_temp_alarm_status);
        tvHumidityAlarmStatus = view.findViewById(R.id.tv_humidity_alarm_status);
        btnDismissTempAlarm = view.findViewById(R.id.btn_dismiss_temp_alarm);
        btnDismissHumidityAlarm = view.findViewById(R.id.btn_dismiss_humidity_alarm);
        btnDismissAllAlarms = view.findViewById(R.id.btn_dismiss_all_alarms);

        // Kontrol butonları
        btnSaveAlarmSettings = view.findViewById(R.id.btn_save_alarm_settings);
        btnLoadAlarmSettings = view.findViewById(R.id.btn_load_alarm_settings);
        btnResetAlarmDefaults = view.findViewById(R.id.btn_reset_alarm_defaults);
        btnTestAlarms = view.findViewById(R.id.btn_test_alarms);

        // İlk değerler
        setDefaultValues();
    }

    private void setDefaultValues() {
        if (etTempLowAlarm != null) etTempLowAlarm.setText("1.0");
        if (etTempHighAlarm != null) etTempHighAlarm.setText("1.0");
        if (etHumidLowAlarm != null) etHumidLowAlarm.setText("10.0");
        if (etHumidHighAlarm != null) etHumidHighAlarm.setText("10.0");

        if (tvCurrentTemp != null) tvCurrentTemp.setText("--°C");
        if (tvTargetTemp != null) tvTargetTemp.setText("--°C");
        if (tvCurrentHumidity != null) tvCurrentHumidity.setText("--%");
        if (tvTargetHumidity != null) tvTargetHumidity.setText("--%");

        if (tvAlarmStatus != null) tvAlarmStatus.setText("Alarm durumu bilinmiyor");
        if (tvTempAlarmStatus != null) tvTempAlarmStatus.setText("Normal");
        if (tvHumidityAlarmStatus != null) tvHumidityAlarmStatus.setText("Normal");

        updateAlarmIndicators(false, false);
    }

    private void setupEventListeners() {
        // Genel alarm switch
        if (switchAlarmEnabled != null) {
            switchAlarmEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
                updateAlarmControlsState(isChecked);
                updateAlarmStatus(isChecked);
            });
        }

        // Sıcaklık alarm değişiklikleri
        if (etTempLowAlarm != null) {
            etTempLowAlarm.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    validateTempAlarms();
                    updateTempAlarmRange();
                }
            });
        }

        if (etTempHighAlarm != null) {
            etTempHighAlarm.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    validateTempAlarms();
                    updateTempAlarmRange();
                }
            });
        }

        // Nem alarm değişiklikleri
        if (etHumidLowAlarm != null) {
            etHumidLowAlarm.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    validateHumidityAlarms();
                    updateHumidityAlarmRange();
                }
            });
        }

        if (etHumidHighAlarm != null) {
            etHumidHighAlarm.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    validateHumidityAlarms();
                    updateHumidityAlarmRange();
                }
            });
        }

        // Alarm dismiss butonları
        if (btnDismissTempAlarm != null) {
            btnDismissTempAlarm.setOnClickListener(v -> dismissTemperatureAlarm());
        }

        if (btnDismissHumidityAlarm != null) {
            btnDismissHumidityAlarm.setOnClickListener(v -> dismissHumidityAlarm());
        }

        if (btnDismissAllAlarms != null) {
            btnDismissAllAlarms.setOnClickListener(v -> dismissAllAlarms());
        }

        // Kontrol butonları
        if (btnSaveAlarmSettings != null) {
            btnSaveAlarmSettings.setOnClickListener(v -> saveAlarmSettings());
        }

        if (btnLoadAlarmSettings != null) {
            btnLoadAlarmSettings.setOnClickListener(v -> loadCurrentSettings());
        }

        if (btnResetAlarmDefaults != null) {
            btnResetAlarmDefaults.setOnClickListener(v -> showResetDialog());
        }

        if (btnTestAlarms != null) {
            btnTestAlarms.setOnClickListener(v -> showTestAlarmsDialog());
        }
    }

    private void updateAlarmControlsState(boolean enabled) {
        // Alarm ayar kontrollerini etkinleştir/devre dışı bırak
        if (etTempLowAlarm != null) etTempLowAlarm.setEnabled(enabled);
        if (etTempHighAlarm != null) etTempHighAlarm.setEnabled(enabled);
        if (etHumidLowAlarm != null) etHumidLowAlarm.setEnabled(enabled);
        if (etHumidHighAlarm != null) etHumidHighAlarm.setEnabled(enabled);
    }

    private void updateAlarmStatus(boolean enabled) {
        if (tvAlarmStatus != null) {
            tvAlarmStatus.setText(enabled ? "Alarmlar Aktif" : "Alarmlar Devre Dışı");
            tvAlarmStatus.setTextColor(getResources().getColor(
                    enabled ? android.R.color.holo_green_dark : android.R.color.holo_red_dark
            ));
        }
    }

    private void validateTempAlarms() {
        boolean lowValid = true;
        boolean highValid = true;

        // Düşük sıcaklık alarmı
        if (etTempLowAlarm != null) {
            try {
                String lowStr = etTempLowAlarm.getText().toString();
                if (!lowStr.isEmpty()) {
                    double lowAlarm = Double.parseDouble(lowStr);
                    if (lowAlarm < 0.1 || lowAlarm > 5.0) {
                        etTempLowAlarm.setError("0.1-5.0°C aralığında olmalı");
                        lowValid = false;
                    } else {
                        etTempLowAlarm.setError(null);
                    }
                }
            } catch (NumberFormatException e) {
                etTempLowAlarm.setError("Geçerli bir sayı girin");
                lowValid = false;
            }
        }

        // Yüksek sıcaklık alarmı
        if (etTempHighAlarm != null) {
            try {
                String highStr = etTempHighAlarm.getText().toString();
                if (!highStr.isEmpty()) {
                    double highAlarm = Double.parseDouble(highStr);
                    if (highAlarm < 0.1 || highAlarm > 5.0) {
                        etTempHighAlarm.setError("0.1-5.0°C aralığında olmalı");
                        highValid = false;
                    } else {
                        etTempHighAlarm.setError(null);
                    }
                }
            } catch (NumberFormatException e) {
                etTempHighAlarm.setError("Geçerli bir sayı girin");
                highValid = false;
            }
        }
    }

    private void validateHumidityAlarms() {
        boolean lowValid = true;
        boolean highValid = true;

        // Düşük nem alarmı
        if (etHumidLowAlarm != null) {
            try {
                String lowStr = etHumidLowAlarm.getText().toString();
                if (!lowStr.isEmpty()) {
                    double lowAlarm = Double.parseDouble(lowStr);
                    if (lowAlarm < 1.0 || lowAlarm > 30.0) {
                        etHumidLowAlarm.setError("1.0-30.0% aralığında olmalı");
                        lowValid = false;
                    } else {
                        etHumidLowAlarm.setError(null);
                    }
                }
            } catch (NumberFormatException e) {
                etHumidLowAlarm.setError("Geçerli bir sayı girin");
                lowValid = false;
            }
        }

        // Yüksek nem alarmı
        if (etHumidHighAlarm != null) {
            try {
                String highStr = etHumidHighAlarm.getText().toString();
                if (!highStr.isEmpty()) {
                    double highAlarm = Double.parseDouble(highStr);
                    if (highAlarm < 1.0 || highAlarm > 30.0) {
                        etHumidHighAlarm.setError("1.0-30.0% aralığında olmalı");
                        highValid = false;
                    } else {
                        etHumidHighAlarm.setError(null);
                    }
                }
            } catch (NumberFormatException e) {
                etHumidHighAlarm.setError("Geçerli bir sayı girin");
                highValid = false;
            }
        }
    }

    private void updateTempAlarmRange() {
        if (tvTempAlarmRange == null) return;

        try {
            String lowStr = etTempLowAlarm.getText().toString();
            String highStr = etTempHighAlarm.getText().toString();

            if (!lowStr.isEmpty() && !highStr.isEmpty()) {
                double lowAlarm = Double.parseDouble(lowStr);
                double highAlarm = Double.parseDouble(highStr);
                double lowRange = targetTemp - lowAlarm;
                double highRange = targetTemp + highAlarm;

                tvTempAlarmRange.setText(String.format(Locale.getDefault(),
                        "Alarm aralığı: %.1f - %.1f°C", lowRange, highRange));
            } else {
                tvTempAlarmRange.setText("Geçerli değerler girin");
            }
        } catch (NumberFormatException e) {
            tvTempAlarmRange.setText("Geçersiz değerler");
        }
    }

    private void updateHumidityAlarmRange() {
        if (tvHumidityAlarmRange == null) return;

        try {
            String lowStr = etHumidLowAlarm.getText().toString();
            String highStr = etHumidHighAlarm.getText().toString();

            if (!lowStr.isEmpty() && !highStr.isEmpty()) {
                double lowAlarm = Double.parseDouble(lowStr);
                double highAlarm = Double.parseDouble(highStr);
                double lowRange = targetHumidity - lowAlarm;
                double highRange = targetHumidity + highAlarm;

                tvHumidityAlarmRange.setText(String.format(Locale.getDefault(),
                        "Alarm aralığı: %.1f - %.1f%%", lowRange, highRange));
            } else {
                tvHumidityAlarmRange.setText("Geçerli değerler girin");
            }
        } catch (NumberFormatException e) {
            tvHumidityAlarmRange.setText("Geçersiz değerler");
        }
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
                loadCurrentSettings();
                refreshHandler.postDelayed(this, REFRESH_INTERVAL);
            }
        }
    };

    private void loadCurrentSettings() {
        // ESP32'den güncel alarm ayarlarını al - getStatus kullan
        ApiService.getInstance().getStatus(new ApiService.DataCallback() {
            @Override
            public void onSuccess(JSONObject data) {
                if (isFragmentActive && getActivity() != null) {
                    getActivity().runOnUiThread(() -> updateUIWithCurrentData(data));
                }
            }

            @Override
            public void onError(String message) {
                if (isFragmentActive && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Log.w(TAG, "Alarm ayarları yüklenemedi: " + message);
                    });
                }
            }
        });
    }

    private void updateUIWithCurrentData(JSONObject data) {
        try {
            // Güncel sensor değerleri
            currentTemp = data.optDouble("temperature", 0.0);
            currentHumidity = data.optDouble("humidity", 0.0);
            targetTemp = data.optDouble("targetTemp", 37.5);
            targetHumidity = data.optDouble("targetHumid", 60.0); // ESP32'de targetHumid

            // Alarm ayarları
            boolean alarmEnabled = data.optBoolean("alarmEnabled", false);
            double tempLowAlarm = data.optDouble("tempLowAlarm", 1.0);
            double tempHighAlarm = data.optDouble("tempHighAlarm", 1.0);
            double humidLowAlarm = data.optDouble("humidLowAlarm", 10.0);
            double humidHighAlarm = data.optDouble("humidHighAlarm", 10.0);

            // Alarm durumları - ESP32'nin yeni alarm sistemine göre hesapla
            if (alarmEnabled) {
                tempAlarmActive = (currentTemp < targetTemp - tempLowAlarm) ||
                        (currentTemp > targetTemp + tempHighAlarm);
                humidityAlarmActive = (currentHumidity < targetHumidity - humidLowAlarm) ||
                        (currentHumidity > targetHumidity + humidHighAlarm);
            } else {
                tempAlarmActive = false;
                humidityAlarmActive = false;
            }

            // UI'yi güncelle
            updateCurrentValues();
            updateAlarmSettings(alarmEnabled, tempLowAlarm, tempHighAlarm, humidLowAlarm, humidHighAlarm);
            updateAlarmIndicators(tempAlarmActive, humidityAlarmActive);
            updateAlarmRanges();

        } catch (Exception e) {
            Log.e(TAG, "UI güncelleme hatası: " + e.getMessage());
        }
    }

    private void updateCurrentValues() {
        if (tvCurrentTemp != null) {
            tvCurrentTemp.setText(String.format(Locale.getDefault(), "%.1f°C", currentTemp));
        }
        if (tvTargetTemp != null) {
            tvTargetTemp.setText(String.format(Locale.getDefault(), "%.1f°C", targetTemp));
        }
        if (tvCurrentHumidity != null) {
            tvCurrentHumidity.setText(String.format(Locale.getDefault(), "%.1f%%", currentHumidity));
        }
        if (tvTargetHumidity != null) {
            tvTargetHumidity.setText(String.format(Locale.getDefault(), "%.1f%%", targetHumidity));
        }
    }

    private void updateAlarmSettings(boolean enabled, double tempLow, double tempHigh, double humidLow, double humidHigh) {
        if (switchAlarmEnabled != null) {
            switchAlarmEnabled.setChecked(enabled);
        }

        updateAlarmStatus(enabled);
        updateAlarmControlsState(enabled);

        if (etTempLowAlarm != null) {
            etTempLowAlarm.setText(String.format(Locale.getDefault(), "%.1f", tempLow));
        }
        if (etTempHighAlarm != null) {
            etTempHighAlarm.setText(String.format(Locale.getDefault(), "%.1f", tempHigh));
        }
        if (etHumidLowAlarm != null) {
            etHumidLowAlarm.setText(String.format(Locale.getDefault(), "%.1f", humidLow));
        }
        if (etHumidHighAlarm != null) {
            etHumidHighAlarm.setText(String.format(Locale.getDefault(), "%.1f", humidHigh));
        }
    }

    private void updateAlarmIndicators(boolean tempAlarm, boolean humidityAlarm) {
        // Sıcaklık alarm göstergesi
        if (tempAlarmIndicator != null) {
            tempAlarmIndicator.setVisibility(tempAlarm ? View.VISIBLE : View.GONE);
        }
        if (tvTempAlarmStatus != null) {
            if (tempAlarm) {
                String status = currentTemp < targetTemp ? "Çok Düşük" : "Çok Yüksek";
                tvTempAlarmStatus.setText("ALARM: " + status);
                tvTempAlarmStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            } else {
                tvTempAlarmStatus.setText("Normal");
                tvTempAlarmStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            }
        }
        if (btnDismissTempAlarm != null) {
            btnDismissTempAlarm.setVisibility(tempAlarm ? View.VISIBLE : View.GONE);
        }

        // Nem alarm göstergesi
        if (humidityAlarmIndicator != null) {
            humidityAlarmIndicator.setVisibility(humidityAlarm ? View.VISIBLE : View.GONE);
        }
        if (tvHumidityAlarmStatus != null) {
            if (humidityAlarm) {
                String status = currentHumidity < targetHumidity ? "Çok Düşük" : "Çok Yüksek";
                tvHumidityAlarmStatus.setText("ALARM: " + status);
                tvHumidityAlarmStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            } else {
                tvHumidityAlarmStatus.setText("Normal");
                tvHumidityAlarmStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            }
        }
        if (btnDismissHumidityAlarm != null) {
            btnDismissHumidityAlarm.setVisibility(humidityAlarm ? View.VISIBLE : View.GONE);
        }

        // Genel alarm dismiss butonu
        if (btnDismissAllAlarms != null) {
            btnDismissAllAlarms.setVisibility((tempAlarm || humidityAlarm) ? View.VISIBLE : View.GONE);
        }
    }

    private void updateAlarmRanges() {
        updateTempAlarmRange();
        updateHumidityAlarmRange();
    }

    private void dismissTemperatureAlarm() {
        // ESP32'nin /api/alarm endpoint'ini kullan
        ApiService.getInstance().dismissAlarm("temperature", new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Sıcaklık alarmı kapatıldı");
                        loadCurrentSettings(); // Durumu güncelle
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Alarm kapatılamadı: " + message);
                    });
                }
            }
        });
    }

    private void dismissHumidityAlarm() {
        // ESP32'nin /api/alarm endpoint'ini kullan
        ApiService.getInstance().dismissAlarm("humidity", new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Nem alarmı kapatıldı");
                        loadCurrentSettings(); // Durumu güncelle
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Alarm kapatılamadı: " + message);
                    });
                }
            }
        });
    }

    private void dismissAllAlarms() {
        // ESP32'nin /api/alarm endpoint'ini kullan
        ApiService.getInstance().dismissAlarm("all", new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Tüm alarmlar kapatıldı");
                        loadCurrentSettings(); // Durumu güncelle
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Alarmlar kapatılamadı: " + message);
                    });
                }
            }
        });
    }

    private void saveAlarmSettings() {
        if (!validateInputs()) {
            return;
        }

        try {
            JSONObject params = new JSONObject();

            // Alarm ayarları
            params.put("alarmEnabled", switchAlarmEnabled.isChecked());

            if (switchAlarmEnabled.isChecked()) {
                double tempLow = Double.parseDouble(etTempLowAlarm.getText().toString());
                double tempHigh = Double.parseDouble(etTempHighAlarm.getText().toString());
                double humidLow = Double.parseDouble(etHumidLowAlarm.getText().toString());
                double humidHigh = Double.parseDouble(etHumidHighAlarm.getText().toString());

                params.put("tempLowAlarm", tempLow);
                params.put("tempHighAlarm", tempHigh);
                params.put("humidLowAlarm", humidLow);
                params.put("humidHighAlarm", humidHigh);
            }

            // ESP32'nin /api/alarm endpoint'ini kullan
            ApiService.getInstance().setAlarmSettings(
                    switchAlarmEnabled.isChecked(),
                    (float) Double.parseDouble(etTempLowAlarm.getText().toString()),
                    (float) Double.parseDouble(etTempHighAlarm.getText().toString()),
                    (float) Double.parseDouble(etHumidLowAlarm.getText().toString()),
                    (float) Double.parseDouble(etHumidHighAlarm.getText().toString()),
                    new ApiService.ParameterCallback() {
                        @Override
                        public void onSuccess() {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    showToast("Alarm ayarları kaydedildi");
                                });
                            }
                        }

                        @Override
                        public void onError(String message) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    showToast("Ayarlar kaydedilemedi: " + message);
                                });
                            }
                        }
                    }
            );

        } catch (Exception e) {
            Log.e(TAG, "Alarm ayarları kaydetme hatası: " + e.getMessage());
            showToast("Parametre hatası: " + e.getMessage());
        }
    }

    private void showResetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Varsayılan Alarm Ayarları")
                .setMessage("Alarm ayarlarını varsayılan değerlere sıfırlamak istediğinizden emin misiniz?")
                .setPositiveButton("Sıfırla", (dialog, which) -> resetToDefaults())
                .setNegativeButton("İptal", null)
                .show();
    }

    private void showTestAlarmsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Alarm Testi")
                .setMessage("Bu özellik alarm sisteminin çalışıp çalışmadığını test eder. Test yapmak istediğinizden emin misiniz?")
                .setPositiveButton("Test Et", (dialog, which) -> testAlarms())
                .setNegativeButton("İptal", null)
                .show();
    }

    private void resetToDefaults() {
        switchAlarmEnabled.setChecked(true);
        etTempLowAlarm.setText("1.0");
        etTempHighAlarm.setText("1.0");
        etHumidLowAlarm.setText("10.0");
        etHumidHighAlarm.setText("10.0");

        updateAlarmControlsState(true);
        updateAlarmStatus(true);
        updateAlarmRanges();

        showToast("Varsayılan alarm ayarları yüklendi");
    }

    private void testAlarms() {
        // ESP32'nin /api/test/alarm endpoint'ini kullan (eğer varsa)
        ApiService.getInstance().testAlarms(new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Alarm testi başarılı");
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Alarm testi başarısız: " + message);
                    });
                }
            }
        });
    }

    private boolean validateInputs() {
        // Alarm aktifse değerleri kontrol et
        if (switchAlarmEnabled.isChecked()) {
            // Sıcaklık alarm değerleri
            try {
                String tempLowStr = etTempLowAlarm.getText().toString();
                String tempHighStr = etTempHighAlarm.getText().toString();

                if (tempLowStr.isEmpty() || tempHighStr.isEmpty()) {
                    showToast("Sıcaklık alarm değerleri boş olamaz");
                    return false;
                }

                double tempLow = Double.parseDouble(tempLowStr);
                double tempHigh = Double.parseDouble(tempHighStr);

                if (tempLow < 0.1 || tempLow > 5.0) {
                    etTempLowAlarm.setError("0.1-5.0°C aralığında olmalı");
                    return false;
                }
                if (tempHigh < 0.1 || tempHigh > 5.0) {
                    etTempHighAlarm.setError("0.1-5.0°C aralığında olmalı");
                    return false;
                }
            } catch (NumberFormatException e) {
                showToast("Sıcaklık alarm değerleri geçersiz");
                return false;
            }

            // Nem alarm değerleri
            try {
                String humidLowStr = etHumidLowAlarm.getText().toString();
                String humidHighStr = etHumidHighAlarm.getText().toString();

                if (humidLowStr.isEmpty() || humidHighStr.isEmpty()) {
                    showToast("Nem alarm değerleri boş olamaz");
                    return false;
                }

                double humidLow = Double.parseDouble(humidLowStr);
                double humidHigh = Double.parseDouble(humidHighStr);

                if (humidLow < 1.0 || humidLow > 30.0) {
                    etHumidLowAlarm.setError("1.0-30.0% aralığında olmalı");
                    return false;
                }
                if (humidHigh < 1.0 || humidHigh > 30.0) {
                    etHumidHighAlarm.setError("1.0-30.0% aralığında olmalı");
                    return false;
                }
            } catch (NumberFormatException e) {
                showToast("Nem alarm değerleri geçersiz");
                return false;
            }
        }

        return true;
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}