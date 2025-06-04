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
import com.kulucka.mk_v5.models.MotorSettings;
import com.kulucka.mk_v5.services.ApiService;
import com.kulucka.mk_v5.utils.SharedPrefsManager;

import org.json.JSONObject;

public class MotorSettingsFragment extends Fragment {

    private static final String TAG = "MotorSettingsFragment";

    // UI Elements
    private EditText etMotorWaitTime;
    private EditText etMotorRunTime;
    private Button btnSaveMotorSettings;
    private Button btnLoadFromESP32;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_motor_settings, container, false);

        initializeViews(view);
        setupClickListeners();
        loadCurrentSettings();

        return view;
    }

    private void initializeViews(View view) {
        etMotorWaitTime = view.findViewById(R.id.et_motor_wait_time);
        etMotorRunTime = view.findViewById(R.id.et_motor_run_time);
        btnSaveMotorSettings = view.findViewById(R.id.btn_save_motor_settings);
        btnLoadFromESP32 = view.findViewById(R.id.btn_load_from_esp32);
    }

    private void setupClickListeners() {
        if (btnSaveMotorSettings != null) {
            btnSaveMotorSettings.setOnClickListener(v -> saveMotorSettings());
        }

        if (btnLoadFromESP32 != null) {
            btnLoadFromESP32.setOnClickListener(v -> loadCurrentSettingsFromESP32());
        }
    }

    private void loadCurrentSettings() {
        // Önce local ayarları yükle
        MotorSettings settings = SharedPrefsManager.getInstance().loadMotorSettings();

        if (etMotorWaitTime != null) {
            etMotorWaitTime.setText(String.valueOf(settings.getMotorWaitTime()));
        }
        if (etMotorRunTime != null) {
            etMotorRunTime.setText(String.valueOf(settings.getMotorRunTime()));
        }

        // Sonra ESP32'den güncel verileri al
        loadCurrentSettingsFromESP32();
    }

    private void loadCurrentSettingsFromESP32() {
        showToast("ESP32'den motor ayarları yükleniyor...");

        // ESP32'nin createAppData fonksiyonundan motor verilerini al
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
                        showToast("ESP32'den motor verileri alınamadı: " + message);
                    });
                }
            }
        });
    }

    private void updateUIFromESP32Data(JSONObject data) {
        try {
            // ESP32'nin createAppData fonksiyonundan gelen motor verileri
            long motorWaitTime = data.optLong("motorWaitTime", 120); // dakika
            long motorRunTime = data.optLong("motorRunTime", 14);    // saniye

            // UI'yi güncelle
            if (etMotorWaitTime != null) {
                etMotorWaitTime.setText(String.valueOf(motorWaitTime));
            }
            if (etMotorRunTime != null) {
                etMotorRunTime.setText(String.valueOf(motorRunTime));
            }

            // Local storage'ı da güncelle
            MotorSettings settings = new MotorSettings();
            settings.setMotorWaitTime(motorWaitTime);
            settings.setMotorRunTime(motorRunTime);
            SharedPrefsManager.getInstance().saveMotorSettings(settings);

            showToast("ESP32'den motor ayarları güncellendi");

        } catch (Exception e) {
            showToast("ESP32 motor verilerini işleme hatası: " + e.getMessage());
        }
    }

    private void saveMotorSettings() {
        try {
            // Kullanıcı girişlerini doğrula
            String waitTimeText = etMotorWaitTime != null ? etMotorWaitTime.getText().toString().trim() : "";
            String runTimeText = etMotorRunTime != null ? etMotorRunTime.getText().toString().trim() : "";

            if (waitTimeText.isEmpty() || runTimeText.isEmpty()) {
                showToast("Lütfen tüm alanları doldurun");
                return;
            }

            long waitTime = Long.parseLong(waitTimeText);
            long runTime = Long.parseLong(runTimeText);

            // Değer kontrolü
            if (waitTime <= 0 || runTime <= 0) {
                showToast("Süre değerleri sıfırdan büyük olmalıdır");
                return;
            }

            if (waitTime > 1440) { // 24 saat = 1440 dakika
                showToast("Motor bekleme süresi 24 saatten (1440 dakika) fazla olamaz");
                return;
            }

            if (runTime > 300) { // 5 dakika = 300 saniye
                showToast("Motor çalışma süresi 5 dakikadan (300 saniye) fazla olamaz");
                return;
            }

            showToast("Motor ayarları ESP32'ye gönderiliyor...");

            // ESP32'nin /api/motor endpoint'ine gönder
            ApiService.getInstance().setMotorSettings(waitTime, runTime, new ApiService.ParameterCallback() {
                @Override
                public void onSuccess() {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            // Yerel olarak kaydet
                            MotorSettings settings = new MotorSettings();
                            settings.setMotorWaitTime(waitTime);
                            settings.setMotorRunTime(runTime);
                            SharedPrefsManager.getInstance().saveMotorSettings(settings);

                            showToast("Motor ayarları başarıyla ESP32'ye kaydedildi");
                        });
                    }
                }

                @Override
                public void onError(String message) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showToast("Motor ayarları kaydedilemedi: " + message);
                        });
                    }
                }
            });

        } catch (NumberFormatException e) {
            showToast("Lütfen geçerli sayısal değerler girin");
        } catch (Exception e) {
            showToast("Motor ayarları kaydetme hatası: " + e.getMessage());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Fragment görünür olduğunda ESP32'den güncel verileri yükle
        loadCurrentSettingsFromESP32();
    }

    private void showToast(final String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
            });
        }
    }
}