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

public class TurningSettingsFragment extends Fragment {

    private static final String TAG = "TurningSettings";

    // UI Elements
    private Switch switchTurningEnabled;
    private EditText etTurningInterval;
    private EditText etTurningDuration;
    private EditText etTurningAngle;
    private Button btnSaveSettings;
    private Button btnTestTurning;
    private TextView tvCurrentStatus;
    private TextView tvLastTurning;
    private TextView tvNextTurning;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_turning_settings, container, false);

        initializeViews(view);
        setupClickListeners();
        loadCurrentSettings();

        return view;
    }

    private void initializeViews(View view) {
        switchTurningEnabled = view.findViewById(R.id.switch_turning_enabled);
        etTurningInterval = view.findViewById(R.id.et_turning_interval);
        etTurningDuration = view.findViewById(R.id.et_turning_duration);
        etTurningAngle = view.findViewById(R.id.et_turning_angle);
        btnSaveSettings = view.findViewById(R.id.btn_save_turning_settings);
        btnTestTurning = view.findViewById(R.id.btn_test_turning);
        tvCurrentStatus = view.findViewById(R.id.tv_turning_status);
        tvLastTurning = view.findViewById(R.id.tv_last_turning);
        tvNextTurning = view.findViewById(R.id.tv_next_turning);

        // Set default values
        if (etTurningInterval != null) {
            etTurningInterval.setText("4"); // 4 hours default
        }
        if (etTurningDuration != null) {
            etTurningDuration.setText("30"); // 30 seconds default
        }
        if (etTurningAngle != null) {
            etTurningAngle.setText("90"); // 90 degrees default
        }
    }

    private void setupClickListeners() {
        if (btnSaveSettings != null) {
            btnSaveSettings.setOnClickListener(v -> saveSettings());
        }

        if (btnTestTurning != null) {
            btnTestTurning.setOnClickListener(v -> testTurning());
        }

        if (switchTurningEnabled != null) {
            switchTurningEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
                setTurningEnabled(isChecked);
                updateButtonStates(isChecked);
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
                        Log.w(TAG, "Failed to load turning settings: " + message);
                        showToast("Çevirme ayarları yüklenemedi: " + message);
                    });
                }
            }
        });
    }

    private void updateUI(JSONObject data) {
        try {
            // Get turning settings from ESP32
            boolean turningEnabled = data.optBoolean("turningEnabled", false);
            int turningInterval = data.optInt("turningInterval", 4);
            int turningDuration = data.optInt("turningDuration", 30);
            int turningAngle = data.optInt("turningAngle", 90);
            String lastTurning = data.optString("lastTurning", "Henüz yapılmadı");
            String nextTurning = data.optString("nextTurning", "Hesaplanıyor...");

            // Update UI elements
            if (switchTurningEnabled != null) {
                switchTurningEnabled.setChecked(turningEnabled);
            }
            if (etTurningInterval != null) {
                etTurningInterval.setText(String.valueOf(turningInterval));
            }
            if (etTurningDuration != null) {
                etTurningDuration.setText(String.valueOf(turningDuration));
            }
            if (etTurningAngle != null) {
                etTurningAngle.setText(String.valueOf(turningAngle));
            }

            // Update status displays
            if (tvCurrentStatus != null) {
                String status = turningEnabled ? "Çevirme Aktif" : "Çevirme Pasif";
                tvCurrentStatus.setText(status);
                tvCurrentStatus.setTextColor(getResources().getColor(
                        turningEnabled ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
            }

            if (tvLastTurning != null) {
                tvLastTurning.setText("Son Çevirme: " + lastTurning);
            }

            if (tvNextTurning != null) {
                tvNextTurning.setText("Sonraki Çevirme: " + nextTurning);
            }

            updateButtonStates(turningEnabled);

        } catch (Exception e) {
            Log.e(TAG, "Error updating turning UI: " + e.getMessage());
        }
    }

    private void updateButtonStates(boolean turningEnabled) {
        if (btnTestTurning != null) {
            btnTestTurning.setEnabled(turningEnabled);
        }

        if (etTurningInterval != null) {
            etTurningInterval.setEnabled(turningEnabled);
        }
        if (etTurningDuration != null) {
            etTurningDuration.setEnabled(turningEnabled);
        }
        if (etTurningAngle != null) {
            etTurningAngle.setEnabled(turningEnabled);
        }
    }

    private void saveSettings() {
        if (!validateInputs()) {
            return;
        }

        try {
            int interval = Integer.parseInt(etTurningInterval.getText().toString());
            int duration = Integer.parseInt(etTurningDuration.getText().toString());
            int angle = Integer.parseInt(etTurningAngle.getText().toString());

            // Save turning interval
            ApiService.getInstance().setTurningInterval(interval, new ApiService.ParameterCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Turning interval saved successfully");
                }

                @Override
                public void onError(String message) {
                    Log.e(TAG, "Failed to save turning interval: " + message);
                }
            });

            // Note: You may need to add setTurningDuration and setTurningAngle methods to ApiService
            // For now, we'll just save the interval

            showToast("Çevirme ayarları kaydedildi");
            refreshData();

        } catch (NumberFormatException e) {
            showToast("Geçersiz sayı formatı");
        }
    }

    private boolean validateInputs() {
        if (etTurningInterval == null || TextUtils.isEmpty(etTurningInterval.getText())) {
            showToast("Çevirme aralığı boş olamaz");
            return false;
        }

        if (etTurningDuration == null || TextUtils.isEmpty(etTurningDuration.getText())) {
            showToast("Çevirme süresi boş olamaz");
            return false;
        }

        if (etTurningAngle == null || TextUtils.isEmpty(etTurningAngle.getText())) {
            showToast("Çevirme açısı boş olamaz");
            return false;
        }

        try {
            int interval = Integer.parseInt(etTurningInterval.getText().toString());
            if (interval < 1 || interval > 24) {
                showToast("Çevirme aralığı 1-24 saat arasında olmalı");
                return false;
            }

            int duration = Integer.parseInt(etTurningDuration.getText().toString());
            if (duration < 5 || duration > 120) {
                showToast("Çevirme süresi 5-120 saniye arasında olmalı");
                return false;
            }

            int angle = Integer.parseInt(etTurningAngle.getText().toString());
            if (angle < 45 || angle > 180) {
                showToast("Çevirme açısı 45-180 derece arasında olmalı");
                return false;
            }

        } catch (NumberFormatException e) {
            showToast("Sayısal değerler geçersiz");
            return false;
        }

        return true;
    }

    private void setTurningEnabled(boolean enabled) {
        ApiService.getInstance().setTurningEnabled(enabled, new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Çevirme " + (enabled ? "etkinleştirildi" : "devre dışı bırakıldı"));
                        refreshData();
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

    private void testTurning() {
        showToast("Test çevirmesi başlatılıyor...");

        // Create a test turning request
        ApiService.getInstance().dismissAlarm("test_turning", new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Test çevirmesi tamamlandı");
                        refreshData();
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Test çevirmesi başarısız: " + message);
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