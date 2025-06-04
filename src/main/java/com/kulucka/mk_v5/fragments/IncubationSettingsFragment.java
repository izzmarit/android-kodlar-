package com.kulucka.mk_v5.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import com.kulucka.mk_v5.activities.MainActivity;
import com.kulucka.mk_v5.services.ApiService;

import org.json.JSONObject;

public class IncubationSettingsFragment extends Fragment {

    private static final String TAG = "IncubationSettings";

    // Kuluçka türleri - ESP32 kodundaki enum değerleri
    private static final int INCUBATION_CHICKEN = 0;
    private static final int INCUBATION_QUAIL = 1;
    private static final int INCUBATION_GOOSE = 2;
    private static final int INCUBATION_MANUAL = 3;

    // UI Elements
    private RadioGroup rgIncubationType;
    private RadioButton rbChicken, rbQuail, rbGoose, rbManual;

    // Manuel ayarlar için UI elementleri
    private View manualSettingsLayout;
    private EditText etManualDevTemp;
    private EditText etManualHatchTemp;
    private EditText etManualDevHumid;
    private EditText etManualHatchHumid;
    private EditText etManualDevDays;
    private EditText etManualHatchDays;

    // Genel kontroller
    private Switch switchIncubationRunning;
    private Button btnSaveSettings;
    private Button btnStartProgram;
    private Button btnStopProgram;
    private TextView tvCurrentStatus;
    private TextView tvCurrentDay;
    private TextView tvTotalDays;

    // Mevcut değerler
    private int currentIncubationType = INCUBATION_CHICKEN;
    private boolean isIncubationRunning = false;

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
        rgIncubationType = view.findViewById(R.id.rg_incubation_type);
        rbChicken = view.findViewById(R.id.rb_chicken);
        rbQuail = view.findViewById(R.id.rb_quail);
        rbGoose = view.findViewById(R.id.rb_goose);
        rbManual = view.findViewById(R.id.rb_manual);

        manualSettingsLayout = view.findViewById(R.id.manual_settings_layout);
        etManualDevTemp = view.findViewById(R.id.et_manual_dev_temp);
        etManualHatchTemp = view.findViewById(R.id.et_manual_hatch_temp);
        etManualDevHumid = view.findViewById(R.id.et_manual_dev_humid);
        etManualHatchHumid = view.findViewById(R.id.et_manual_hatch_humid);
        etManualDevDays = view.findViewById(R.id.et_manual_dev_days);
        etManualHatchDays = view.findViewById(R.id.et_manual_hatch_days);

        switchIncubationRunning = view.findViewById(R.id.switch_incubation_running);
        btnSaveSettings = view.findViewById(R.id.btn_save_settings);
        btnStartProgram = view.findViewById(R.id.btn_start_program);
        btnStopProgram = view.findViewById(R.id.btn_stop_program);
        tvCurrentStatus = view.findViewById(R.id.tv_current_status);
        tvCurrentDay = view.findViewById(R.id.tv_current_day);
        tvTotalDays = view.findViewById(R.id.tv_total_days);

        // Varsayılan değerleri ayarla
        setDefaultValues();
    }

    private void setDefaultValues() {
        // Tavuk için varsayılan değerler
        if (etManualDevTemp != null) etManualDevTemp.setText("37.5");
        if (etManualHatchTemp != null) etManualHatchTemp.setText("37.0");
        if (etManualDevHumid != null) etManualDevHumid.setText("60");
        if (etManualHatchHumid != null) etManualHatchHumid.setText("70");
        if (etManualDevDays != null) etManualDevDays.setText("18");
        if (etManualHatchDays != null) etManualHatchDays.setText("3");

        // Başlangıçta tavuk seçili
        if (rbChicken != null) rbChicken.setChecked(true);
        currentIncubationType = INCUBATION_CHICKEN;
        updateManualSettingsVisibility();
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

        if (switchIncubationRunning != null) {
            switchIncubationRunning.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed()) { // Sadece kullanıcı etkileşiminde
                    isIncubationRunning = isChecked;
                    updateButtonStates();
                }
            });
        }

        if (rgIncubationType != null) {
            rgIncubationType.setOnCheckedChangeListener((group, checkedId) -> {
                updateIncubationTypeSettings(checkedId);
            });
        }
    }

    private void updateIncubationTypeSettings(int checkedId) {
        if (checkedId == R.id.rb_chicken) {
            currentIncubationType = INCUBATION_CHICKEN;
            setPresetValues(37.5f, 37.0f, 60, 70, 18, 3);
        } else if (checkedId == R.id.rb_quail) {
            currentIncubationType = INCUBATION_QUAIL;
            setPresetValues(37.8f, 37.5f, 55, 65, 14, 3);
        } else if (checkedId == R.id.rb_goose) {
            currentIncubationType = INCUBATION_GOOSE;
            setPresetValues(37.5f, 37.0f, 55, 65, 25, 3);
        } else if (checkedId == R.id.rb_manual) {
            currentIncubationType = INCUBATION_MANUAL;
            // Manuel modda kullanıcı kendi değerlerini girer
        }

        updateManualSettingsVisibility();
    }

    private void setPresetValues(float devTemp, float hatchTemp, int devHumid, int hatchHumid, int devDays, int hatchDays) {
        if (etManualDevTemp != null) etManualDevTemp.setText(String.valueOf(devTemp));
        if (etManualHatchTemp != null) etManualHatchTemp.setText(String.valueOf(hatchTemp));
        if (etManualDevHumid != null) etManualDevHumid.setText(String.valueOf(devHumid));
        if (etManualHatchHumid != null) etManualHatchHumid.setText(String.valueOf(hatchHumid));
        if (etManualDevDays != null) etManualDevDays.setText(String.valueOf(devDays));
        if (etManualHatchDays != null) etManualHatchDays.setText(String.valueOf(hatchDays));
    }

    private void updateManualSettingsVisibility() {
        if (manualSettingsLayout != null) {
            boolean isManual = (currentIncubationType == INCUBATION_MANUAL);
            manualSettingsLayout.setVisibility(isManual ? View.VISIBLE : View.GONE);
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
                        Log.w(TAG, "Failed to load ESP32 incubation settings: " + message);
                        showToast("Ayarlar yüklenemedi: " + message);
                    });
                }
            }
        });
    }

    private void updateUI(JSONObject data) {
        try {
            // ESP32'den gelen veriler
            String incubationType = data.optString("incubationType", "Tavuk");
            int currentDay = data.optInt("currentDay", 0);
            int totalDays = data.optInt("totalDays", 21);
            boolean programActive = data.optBoolean("isIncubationRunning", false);

            // Mevcut değerleri güncelle
            isIncubationRunning = programActive;

            // Kuluçka tipini ayarla
            setIncubationTypeFromString(incubationType);

            // UI elementlerini güncelle
            if (switchIncubationRunning != null) {
                switchIncubationRunning.setChecked(isIncubationRunning);
            }

            if (tvCurrentDay != null) {
                tvCurrentDay.setText("Mevcut Gün: " + currentDay);
            }

            if (tvTotalDays != null) {
                tvTotalDays.setText("Toplam Gün: " + totalDays);
            }

            if (tvCurrentStatus != null) {
                String status = isIncubationRunning ?
                        "Program Aktif - Gün: " + currentDay + "/" + totalDays :
                        "Program Durduruldu";
                tvCurrentStatus.setText(status);
            }

            updateButtonStates();

        } catch (Exception e) {
            Log.e(TAG, "Error updating incubation UI: " + e.getMessage());
        }
    }

    private void setIncubationTypeFromString(String type) {
        switch (type.toLowerCase()) {
            case "tavuk":
            case "chicken":
                currentIncubationType = INCUBATION_CHICKEN;
                if (rbChicken != null) rbChicken.setChecked(true);
                break;
            case "bıldırcın":
            case "quail":
                currentIncubationType = INCUBATION_QUAIL;
                if (rbQuail != null) rbQuail.setChecked(true);
                break;
            case "kaz":
            case "goose":
                currentIncubationType = INCUBATION_GOOSE;
                if (rbGoose != null) rbGoose.setChecked(true);
                break;
            default:
                currentIncubationType = INCUBATION_MANUAL;
                if (rbManual != null) rbManual.setChecked(true);
                break;
        }
        updateManualSettingsVisibility();
    }

    private void saveSettings() {
        if (!validateInputs()) {
            return;
        }

        try {
            float manualDevTemp = Float.parseFloat(etManualDevTemp.getText().toString());
            float manualHatchTemp = Float.parseFloat(etManualHatchTemp.getText().toString());
            int manualDevHumid = Integer.parseInt(etManualDevHumid.getText().toString());
            int manualHatchHumid = Integer.parseInt(etManualHatchHumid.getText().toString());
            int manualDevDays = Integer.parseInt(etManualDevDays.getText().toString());
            int manualHatchDays = Integer.parseInt(etManualHatchDays.getText().toString());

            // ESP32'ye ayarları gönder
            ApiService.getInstance().setIncubationSettings(
                    currentIncubationType,
                    isIncubationRunning,
                    manualDevTemp,
                    manualHatchTemp,
                    manualDevHumid,
                    manualHatchHumid,
                    manualDevDays,
                    manualHatchDays,
                    new ApiService.ParameterCallback() {
                        @Override
                        public void onSuccess() {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    showToast("Kuluçka ayarları ESP32'ye kaydedildi");
                                    refreshData();
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

        } catch (NumberFormatException e) {
            showToast("Geçersiz sayı formatı");
        }
    }

    private boolean validateInputs() {
        if (etManualDevTemp == null || TextUtils.isEmpty(etManualDevTemp.getText())) {
            showToast("Gelişim sıcaklığı boş olamaz");
            return false;
        }

        if (etManualHatchTemp == null || TextUtils.isEmpty(etManualHatchTemp.getText())) {
            showToast("Çıkım sıcaklığı boş olamaz");
            return false;
        }

        if (etManualDevHumid == null || TextUtils.isEmpty(etManualDevHumid.getText())) {
            showToast("Gelişim nemi boş olamaz");
            return false;
        }

        if (etManualHatchHumid == null || TextUtils.isEmpty(etManualHatchHumid.getText())) {
            showToast("Çıkım nemi boş olamaz");
            return false;
        }

        if (etManualDevDays == null || TextUtils.isEmpty(etManualDevDays.getText())) {
            showToast("Gelişim günleri boş olamaz");
            return false;
        }

        if (etManualHatchDays == null || TextUtils.isEmpty(etManualHatchDays.getText())) {
            showToast("Çıkım günleri boş olamaz");
            return false;
        }

        try {
            float devTemp = Float.parseFloat(etManualDevTemp.getText().toString());
            if (devTemp < 30 || devTemp > 45) {
                showToast("Gelişim sıcaklığı 30-45°C arasında olmalı");
                return false;
            }

            float hatchTemp = Float.parseFloat(etManualHatchTemp.getText().toString());
            if (hatchTemp < 30 || hatchTemp > 45) {
                showToast("Çıkım sıcaklığı 30-45°C arasında olmalı");
                return false;
            }

            int devHumid = Integer.parseInt(etManualDevHumid.getText().toString());
            if (devHumid < 40 || devHumid > 80) {
                showToast("Gelişim nemi %40-80 arasında olmalı");
                return false;
            }

            int hatchHumid = Integer.parseInt(etManualHatchHumid.getText().toString());
            if (hatchHumid < 40 || hatchHumid > 90) {
                showToast("Çıkım nemi %40-90 arasında olmalı");
                return false;
            }

            int devDays = Integer.parseInt(etManualDevDays.getText().toString());
            if (devDays < 1 || devDays > 35) {
                showToast("Gelişim günleri 1-35 arasında olmalı");
                return false;
            }

            int hatchDays = Integer.parseInt(etManualHatchDays.getText().toString());
            if (hatchDays < 1 || hatchDays > 7) {
                showToast("Çıkım günleri 1-7 arasında olmalı");
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

        isIncubationRunning = true;
        saveSettings(); // Önce ayarları kaydet, sonra başlat
    }

    private void stopIncubationProgram() {
        isIncubationRunning = false;
        saveSettings(); // Durdurma ayarını kaydet
    }

    private void updateButtonStates() {
        if (btnStartProgram != null && btnStopProgram != null) {
            btnStartProgram.setEnabled(!isIncubationRunning);
            btnStopProgram.setEnabled(isIncubationRunning);
        }

        if (switchIncubationRunning != null) {
            switchIncubationRunning.setChecked(isIncubationRunning);
        }
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