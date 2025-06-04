package com.kulucka.mk_v5.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kulucka.mk_v5.R;
import com.kulucka.mk_v5.models.CalibrationSettings;
import com.kulucka.mk_v5.services.ApiService;
import com.kulucka.mk_v5.utils.SharedPrefsManager;

import org.json.JSONObject;

public class CalibrationFragment extends Fragment {

    private static final String TAG = "CalibrationFragment";

    // UI Elements - ESP32'deki 4 kalibrasyon değerine uygun
    private EditText etTempCal1;
    private EditText etTempCal2;
    private EditText etHumidCal1;
    private EditText etHumidCal2;
    private Button btnSaveCalibration;
    private Button btnLoadFromESP32;
    private Button btnResetDefaults;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calibration, container, false);

        initializeViews(view);
        setupClickListeners();
        loadCurrentSettings();

        return view;
    }

    private void initializeViews(View view) {
        // ESP32'deki kalibrasyon parametrelerine uygun
        etTempCal1 = view.findViewById(R.id.et_temp_cal_1);
        etTempCal2 = view.findViewById(R.id.et_temp_cal_2);
        etHumidCal1 = view.findViewById(R.id.et_humid_cal_1);
        etHumidCal2 = view.findViewById(R.id.et_humid_cal_2);

        btnSaveCalibration = view.findViewById(R.id.btn_save_calibration);
        btnLoadFromESP32 = view.findViewById(R.id.btn_load_from_esp32);
        btnResetDefaults = view.findViewById(R.id.btn_reset_defaults);
    }

    private void setupClickListeners() {
        if (btnSaveCalibration != null) {
            btnSaveCalibration.setOnClickListener(v -> saveCalibrationSettings());
        }

        if (btnLoadFromESP32 != null) {
            btnLoadFromESP32.setOnClickListener(v -> loadSettingsFromESP32());
        }

        if (btnResetDefaults != null) {
            btnResetDefaults.setOnClickListener(v -> resetToDefaults());
        }
    }

    private void loadCurrentSettings() {
        // Önce local ayarları yükle
        CalibrationSettings settings = SharedPrefsManager.getInstance().loadCalibrationSettings();

        if (etTempCal1 != null) etTempCal1.setText(String.valueOf(settings.getTempCalibration1()));
        if (etTempCal2 != null) etTempCal2.setText(String.valueOf(settings.getTempCalibration2()));
        if (etHumidCal1 != null) etHumidCal1.setText(String.valueOf(settings.getHumidCalibration1()));
        if (etHumidCal2 != null) etHumidCal2.setText(String.valueOf(settings.getHumidCalibration2()));

        // Sonra ESP32'den güncel verileri al
        loadSettingsFromESP32();
    }

    private void loadSettingsFromESP32() {
        showToast("ESP32'den kalibrasyon ayarları yükleniyor...");

        // ESP32'nin createAppData fonksiyonundan tüm kalibrasyon verilerini al
        ApiService.getInstance().getAppData(new ApiService.DataCallback() {
            @Override
            public void onSuccess(JSONObject data) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateUIFromESP32Data(data);
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("ESP32'den kalibrasyon verileri alınamadı: " + message);
                    });
                }
            }
        });
    }

    private void updateUIFromESP32Data(JSONObject data) {
        try {
            // ESP32'nin createAppData fonksiyonundaki kalibrasyon değerleri
            float tempCalibration1 = (float) data.optDouble("tempCalibration1", 0.0);
            float tempCalibration2 = (float) data.optDouble("tempCalibration2", 0.0);
            float humidCalibration1 = (float) data.optDouble("humidCalibration1", 0.0);
            float humidCalibration2 = (float) data.optDouble("humidCalibration2", 0.0);

            // UI'yi güncelle
            if (etTempCal1 != null) etTempCal1.setText(String.valueOf(tempCalibration1));
            if (etTempCal2 != null) etTempCal2.setText(String.valueOf(tempCalibration2));
            if (etHumidCal1 != null) etHumidCal1.setText(String.valueOf(humidCalibration1));
            if (etHumidCal2 != null) etHumidCal2.setText(String.valueOf(humidCalibration2));

            // Local storage'ı da güncelle
            CalibrationSettings settings = new CalibrationSettings();
            settings.setTempCalibration1(tempCalibration1);
            settings.setTempCalibration2(tempCalibration2);
            settings.setHumidCalibration1(humidCalibration1);
            settings.setHumidCalibration2(humidCalibration2);
            SharedPrefsManager.getInstance().saveCalibrationSettings(settings);

            showToast("ESP32'den kalibrasyon ayarları güncellendi");

        } catch (Exception e) {
            showToast("ESP32 kalibrasyon verilerini işleme hatası: " + e.getMessage());
        }
    }

    private void saveCalibrationSettings() {
        try {
            // Kullanıcı girişlerini doğrula
            String tempCal1Text = etTempCal1 != null ? etTempCal1.getText().toString().trim() : "0";
            String tempCal2Text = etTempCal2 != null ? etTempCal2.getText().toString().trim() : "0";
            String humidCal1Text = etHumidCal1 != null ? etHumidCal1.getText().toString().trim() : "0";
            String humidCal2Text = etHumidCal2 != null ? etHumidCal2.getText().toString().trim() : "0";

            if (tempCal1Text.isEmpty()) tempCal1Text = "0";
            if (tempCal2Text.isEmpty()) tempCal2Text = "0";
            if (humidCal1Text.isEmpty()) humidCal1Text = "0";
            if (humidCal2Text.isEmpty()) humidCal2Text = "0";

            float tempCal1 = Float.parseFloat(tempCal1Text);
            float tempCal2 = Float.parseFloat(tempCal2Text);
            float humidCal1 = Float.parseFloat(humidCal1Text);
            float humidCal2 = Float.parseFloat(humidCal2Text);

            // Değer aralığı kontrolü
            if (Math.abs(tempCal1) > 10 || Math.abs(tempCal2) > 10) {
                showToast("Sıcaklık kalibrasyon değerleri -10 ile +10 arasında olmalı");
                return;
            }

            if (Math.abs(humidCal1) > 20 || Math.abs(humidCal2) > 20) {
                showToast("Nem kalibrasyon değerleri -20 ile +20 arasında olmalı");
                return;
            }

            showToast("Kalibrasyon ayarları ESP32'ye gönderiliyor...");

            // ESP32'nin /api/calibration endpoint'ine gönder
            ApiService.getInstance().setCalibrationSettings(
                    tempCal1, tempCal2, humidCal1, humidCal2,
                    new ApiService.ParameterCallback() {
                        @Override
                        public void onSuccess() {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    // Yerel olarak kaydet
                                    CalibrationSettings settings = new CalibrationSettings();
                                    settings.setTempCalibration1(tempCal1);
                                    settings.setTempCalibration2(tempCal2);
                                    settings.setHumidCalibration1(humidCal1);
                                    settings.setHumidCalibration2(humidCal2);
                                    SharedPrefsManager.getInstance().saveCalibrationSettings(settings);

                                    showToast("Kalibrasyon ayarları başarıyla ESP32'ye kaydedildi");
                                });
                            }
                        }

                        @Override
                        public void onError(String message) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    showToast("Kalibrasyon ayarları kaydedilemedi: " + message);
                                });
                            }
                        }
                    }
            );

        } catch (NumberFormatException e) {
            showToast("Lütfen geçerli sayısal değerler girin");
        } catch (Exception e) {
            showToast("Kalibrasyon ayarları kaydetme hatası: " + e.getMessage());
        }
    }

    private void resetToDefaults() {
        // Varsayılan değerleri yükle (sıfır kalibrasyon)
        if (etTempCal1 != null) etTempCal1.setText("0.0");
        if (etTempCal2 != null) etTempCal2.setText("0.0");
        if (etHumidCal1 != null) etHumidCal1.setText("0.0");
        if (etHumidCal2 != null) etHumidCal2.setText("0.0");

        showToast("Varsayılan kalibrasyon değerleri yüklendi");
    }

    @Override
    public void onResume() {
        super.onResume();
        // Fragment görünür olduğunda ESP32'den güncel verileri yükle
        loadSettingsFromESP32();
    }

    private void showToast(final String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
            });
        }
    }
}