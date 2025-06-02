package com.kulucka.mk_v5.fragments;

import android.os.Bundle;
import android.text.TextUtils;
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
import com.kulucka.mk_v5.activities.MainActivity;
import com.kulucka.mk_v5.services.ApiService;

import org.json.JSONObject;

public class IncubationSettingsFragment extends Fragment {

    private static final String TAG = "IncubationSettings";

    // UI Elements
    private EditText etTargetTemperature;
    private EditText etTargetHumidity;
    private EditText etTotalDays;
    private Switch switchPidEnabled;
    private Switch switchTurningEnabled;
    private EditText etTurningInterval;
    private Button btnSaveSettings;
    private Button btnStartProgram;
    private Button btnStopProgram;
    private TextView tvCurrentStatus;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_incubation_settings, container, false);

        initializeViews(view);
        setupClickListeners();
        loadCurrentSettings();

        return view;
    }

    private void initializeViews(View view) {
        etTargetTemperature = view.findViewById(R.id.et_target_temperature);
        etTargetHumidity = view.findViewById(R.id.et_target_humidity);
        etTotalDays = view.findViewById(R.id.et_total_days);
        switchPidEnabled = view.findViewById(R.id.switch_pid_enabled);
        switchTurningEnabled = view.findViewById(R.id.switch_turning_enabled);
        etTurningInterval = view.findViewById(R.id.et_turning_interval);
        btnSaveSettings = view.findViewById(R.id.btn_save_settings);
        btnStartProgram = view.findViewById(R.id.btn_start_program);
        btnStopProgram = view.findViewById(R.id.btn_stop_program);
        tvCurrentStatus = view.findViewById(R.id.tv_current_status);

        // Default values
        if (etTargetTemperature != null) {
            etTargetTemperature.setText("37.5");
        }
        if (etTargetHumidity != null) {
            etTargetHumidity.setText("60.0");
        }
        if (etTotalDays != null) {
            etTotalDays.setText("21");
        }
        if (etTurningInterval != null) {
            etTurningInterval.setText("4");
        }
    }

    private void setupClickListeners() {
        if (btnSaveSettings != null) {
            btnSaveSettings.setOnClickListener(v -> saveSettings());
        }

        if (btnStartProgram != null) {
            btnStartProgram.setOnClickListener(v -> startIncubationProgram());
        }

        if (btnStopProgram != null) {
            btnStopProgram.setOnClickListener(v -> stopIncubationProgram());
        }

        if (switchPidEnabled != null) {
            switchPidEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
                setPidEnabled(isChecked);
            });
        }

        if (switchTurningEnabled != null) {
            switchTurningEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
                setTurningEnabled(isChecked);
            });
        }
    }

    private void loadCurrentSettings() {
        ApiService.getInstance().getStatus(new ApiService.DataCallback() {
            @Override
            public void onSuccess(JSONObject data) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> updateUI(data));
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Log.w(TAG, "Failed to load settings: " + message);
                        showToast("Ayarlar yüklenemedi: " + message);
                    });
                }
            }
        });
    }

    private void updateUI(JSONObject data) {
        try {
            // Current values from ESP32
            double targetTemp = data.optDouble("targetTemp", 37.5);
            double targetHumidity = data.optDouble("targetHumidity", 60.0);
            int totalDays = data.optInt("totalDays", 21);
            boolean pidEnabled = data.optBoolean("pidEnabled", true);
            boolean turningEnabled = data.optBoolean("turningEnabled", true);
            int turningInterval = data.optInt("turningInterval", 4);
            boolean programActive = data.optBoolean("programActive", false);
            int currentDay = data.optInt("currentDay", 0);

            // Update UI elements
            if (etTargetTemperature != null) {
                etTargetTemperature.setText(String.valueOf(targetTemp));
            }
            if (etTargetHumidity != null) {
                etTargetHumidity.setText(String.valueOf(targetHumidity));
            }
            if (etTotalDays != null) {
                etTotalDays.setText(String.valueOf(totalDays));
            }
            if (switchPidEnabled != null) {
                switchPidEnabled.setChecked(pidEnabled);
            }
            if (switchTurningEnabled != null) {
                switchTurningEnabled.setChecked(turningEnabled);
            }
            if (etTurningInterval != null) {
                etTurningInterval.setText(String.valueOf(turningInterval));
            }

            // Update status
            if (tvCurrentStatus != null) {
                String status = programActive ?
                        "Program Aktif - Gün: " + currentDay + "/" + totalDays :
                        "Program Durduruldu";
                tvCurrentStatus.setText(status);
            }

            // Update button states
            if (btnStartProgram != null && btnStopProgram != null) {
                btnStartProgram.setEnabled(!programActive);
                btnStopProgram.setEnabled(programActive);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error updating UI: " + e.getMessage());
        }
    }

    private void saveSettings() {
        if (!validateInputs()) {
            return;
        }

        try {
            double temperature = Double.parseDouble(etTargetTemperature.getText().toString());
            double humidity = Double.parseDouble(etTargetHumidity.getText().toString());
            int turningInterval = Integer.parseInt(etTurningInterval.getText().toString());

            // Save temperature
            ApiService.getInstance().setTemperature(temperature, new ApiService.ParameterCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Temperature saved successfully");
                }

                @Override
                public void onError(String message) {
                    Log.e(TAG, "Failed to save temperature: " + message);
                }
            });

            // Save humidity
            ApiService.getInstance().setHumidity(humidity, new ApiService.ParameterCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Humidity saved successfully");
                }

                @Override
                public void onError(String message) {
                    Log.e(TAG, "Failed to save humidity: " + message);
                }
            });

            // Save turning interval
            ApiService.getInstance().setTurningInterval(turningInterval, new ApiService.ParameterCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Turning interval saved successfully");
                }

                @Override
                public void onError(String message) {
                    Log.e(TAG, "Failed to save turning interval: " + message);
                }
            });

            showToast("Ayarlar kaydedildi");
            refreshData();

        } catch (NumberFormatException e) {
            showToast("Geçersiz sayı formatı");
        }
    }

    private boolean validateInputs() {
        if (etTargetTemperature == null || TextUtils.isEmpty(etTargetTemperature.getText())) {
            showToast("Hedef sıcaklık boş olamaz");
            return false;
        }

        if (etTargetHumidity == null || TextUtils.isEmpty(etTargetHumidity.getText())) {
            showToast("Hedef nem boş olamaz");
            return false;
        }

        if (etTotalDays == null || TextUtils.isEmpty(etTotalDays.getText())) {
            showToast("Toplam gün sayısı boş olamaz");
            return false;
        }

        if (etTurningInterval == null || TextUtils.isEmpty(etTurningInterval.getText())) {
            showToast("Çevirme aralığı boş olamaz");
            return false;
        }

        try {
            double temp = Double.parseDouble(etTargetTemperature.getText().toString());
            if (temp < 30 || temp > 45) {
                showToast("Sıcaklık 30-45°C arasında olmalı");
                return false;
            }

            double humidity = Double.parseDouble(etTargetHumidity.getText().toString());
            if (humidity < 40 || humidity > 80) {
                showToast("Nem %40-80 arasında olmalı");
                return false;
            }

            int days = Integer.parseInt(etTotalDays.getText().toString());
            if (days < 1 || days > 30) {
                showToast("Toplam gün 1-30 arasında olmalı");
                return false;
            }

            int interval = Integer.parseInt(etTurningInterval.getText().toString());
            if (interval < 1 || interval > 24) {
                showToast("Çevirme aralığı 1-24 saat arasında olmalı");
                return false;
            }

        } catch (NumberFormatException e) {
            showToast("Sayısal değerler geçersiz");
            return false;
        }

        return true;
    }

    private void startIncubationProgram() {
        if (!validateInputs()) {
            return;
        }

        try {
            int totalDays = Integer.parseInt(etTotalDays.getText().toString());

            ApiService.getInstance().startIncubation(totalDays, new ApiService.ParameterCallback() {
                @Override
                public void onSuccess() {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showToast("Kuluçka programı başlatıldı");
                            refreshData();
                        });
                    }
                }

                @Override
                public void onError(String message) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showToast("Program başlatılamadı: " + message);
                        });
                    }
                }
            });
        } catch (NumberFormatException e) {
            showToast("Toplam gün sayısı geçersiz");
        }
    }

    private void stopIncubationProgram() {
        ApiService.getInstance().stopIncubation(new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Kuluçka programı durduruldu");
                        refreshData();
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Program durdurulamadı: " + message);
                    });
                }
            }
        });
    }

    private void setPidEnabled(boolean enabled) {
        ApiService.getInstance().setPidEnabled(enabled, new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("PID " + (enabled ? "etkinleştirildi" : "devre dışı bırakıldı"));
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("PID ayarlanamadı: " + message);
                        // Revert switch state
                        if (switchPidEnabled != null) {
                            switchPidEnabled.setChecked(!enabled);
                        }
                    });
                }
            }
        });
    }

    private void setTurningEnabled(boolean enabled) {
        ApiService.getInstance().setTurningEnabled(enabled, new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Çevirme " + (enabled ? "etkinleştirildi" : "devre dışı bırakıldı"));
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Çevirme ayarlanamadı: " + message);
                        // Revert switch state
                        if (switchTurningEnabled != null) {
                            switchTurningEnabled.setChecked(!enabled);
                        }
                    });
                }
            }
        });
    }

    private void refreshData() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).refreshDataNow();
        }
        loadCurrentSettings();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCurrentSettings();
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}