package com.kulucka.mk_v5.fragments;

import android.os.Bundle;
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
import com.kulucka.mk_v5.models.AlarmSettings;
import com.kulucka.mk_v5.services.ApiService;
import com.kulucka.mk_v5.utils.SharedPrefsManager;

import org.json.JSONException;
import org.json.JSONObject;

public class AlarmSettingsFragment extends Fragment {

    private Switch swMasterAlarm;
    private TextView tvAlarmStatus;
    private EditText etTempLowAlarm;
    private EditText etTempHighAlarm;
    private EditText etHumidLowAlarm;
    private EditText etHumidHighAlarm;
    private Button btnSaveAlarmSettings;
    private Button btnLoadCurrentSettings;

    private boolean currentAlarmState = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alarm_settings, container, false);

        initializeViews(view);
        setupEventListeners();
        loadCurrentSettings();

        return view;
    }

    private void initializeViews(View view) {
        swMasterAlarm = view.findViewById(R.id.sw_master_alarm);
        tvAlarmStatus = view.findViewById(R.id.tv_alarm_status);
        etTempLowAlarm = view.findViewById(R.id.et_temp_low_alarm);
        etTempHighAlarm = view.findViewById(R.id.et_temp_high_alarm);
        etHumidLowAlarm = view.findViewById(R.id.et_humid_low_alarm);
        etHumidHighAlarm = view.findViewById(R.id.et_humid_high_alarm);
        btnSaveAlarmSettings = view.findViewById(R.id.btn_save_alarm_settings);
        btnLoadCurrentSettings = view.findViewById(R.id.btn_load_current_settings);
    }

    private void setupEventListeners() {
        // Master alarm switch event listener
        if (swMasterAlarm != null) {
            swMasterAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed()) { // Sadece kullanıcı etkileşiminde çalışsın
                    updateAlarmMasterState(isChecked);
                }
            });
        }

        // Kaydet butonuna tıklama işleyicisi
        if (btnSaveAlarmSettings != null) {
            btnSaveAlarmSettings.setOnClickListener(v -> saveAlarmSettings());
        }

        // ESP32'den güncel verileri yükle butonu
        if (btnLoadCurrentSettings != null) {
            btnLoadCurrentSettings.setOnClickListener(v -> loadCurrentSettingsFromESP32());
        }
    }

    private void loadCurrentSettings() {
        // Önce local ayarları yükle
        loadLocalSettings();

        // Sonra ESP32'den güncel verileri al
        loadCurrentSettingsFromESP32();
    }

    private void loadLocalSettings() {
        // Master alarm durumunu yükle
        boolean masterAlarmEnabled = SharedPrefsManager.getInstance().isMasterAlarmEnabled();
        currentAlarmState = masterAlarmEnabled;

        if (swMasterAlarm != null) {
            swMasterAlarm.setChecked(masterAlarmEnabled);
        }

        updateAlarmStatusDisplay(masterAlarmEnabled);
        updateAlarmFieldsState(masterAlarmEnabled);

        // Alarm ayarlarını yükle
        AlarmSettings settings = SharedPrefsManager.getInstance().loadAlarmSettings();
        if (etTempLowAlarm != null) etTempLowAlarm.setText(String.valueOf(settings.getTempLowAlarm()));
        if (etTempHighAlarm != null) etTempHighAlarm.setText(String.valueOf(settings.getTempHighAlarm()));
        if (etHumidLowAlarm != null) etHumidLowAlarm.setText(String.valueOf(settings.getHumidLowAlarm()));
        if (etHumidHighAlarm != null) etHumidHighAlarm.setText(String.valueOf(settings.getHumidHighAlarm()));
    }

    private void loadCurrentSettingsFromESP32() {
        ApiService.getInstance().getStatus(new ApiService.DataCallback() {
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
                        showToast("ESP32'den alarm verileri alınamadı: " + message);
                    });
                }
            }
        });
    }

    private void updateUIFromESP32Data(JSONObject data) {
        try {
            // ESP32'den gelen alarm durumu
            boolean alarmEnabled = data.optBoolean("alarmEnabled", currentAlarmState);
            float tempLowAlarm = (float) data.optDouble("tempLowAlarm", 1.0);
            float tempHighAlarm = (float) data.optDouble("tempHighAlarm", 1.0);
            float humidLowAlarm = (float) data.optDouble("humidLowAlarm", 10.0);
            float humidHighAlarm = (float) data.optDouble("humidHighAlarm", 10.0);

            currentAlarmState = alarmEnabled;

            // UI'yi güncelle
            if (swMasterAlarm != null) {
                swMasterAlarm.setChecked(alarmEnabled);
            }

            updateAlarmStatusDisplay(alarmEnabled);
            updateAlarmFieldsState(alarmEnabled);

            // Alarm değerlerini güncelle
            if (etTempLowAlarm != null) etTempLowAlarm.setText(String.valueOf(tempLowAlarm));
            if (etTempHighAlarm != null) etTempHighAlarm.setText(String.valueOf(tempHighAlarm));
            if (etHumidLowAlarm != null) etHumidLowAlarm.setText(String.valueOf(humidLowAlarm));
            if (etHumidHighAlarm != null) etHumidHighAlarm.setText(String.valueOf(humidHighAlarm));

            // Local storage'ı da güncelle
            SharedPrefsManager.getInstance().setMasterAlarmEnabled(alarmEnabled);
            AlarmSettings settings = new AlarmSettings();
            settings.setTempLowAlarm(tempLowAlarm);
            settings.setTempHighAlarm(tempHighAlarm);
            settings.setHumidLowAlarm(humidLowAlarm);
            settings.setHumidHighAlarm(humidHighAlarm);
            SharedPrefsManager.getInstance().saveAlarmSettings(settings);

            showToast("ESP32'den alarm ayarları güncellendi");

        } catch (Exception e) {
            showToast("ESP32 alarm verilerini işleme hatası: " + e.getMessage());
        }
    }

    private void updateAlarmMasterState(boolean isEnabled) {
        try {
            // ESP32'ye alarm durumunu gönder
            ApiService.getInstance().setAlarmSettings(
                    isEnabled,
                    getCurrentTempLowAlarm(),
                    getCurrentTempHighAlarm(),
                    getCurrentHumidLowAlarm(),
                    getCurrentHumidHighAlarm(),
                    new ApiService.ParameterCallback() {
                        @Override
                        public void onSuccess() {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    // Yerel olarak kaydet
                                    SharedPrefsManager.getInstance().setMasterAlarmEnabled(isEnabled);
                                    currentAlarmState = isEnabled;

                                    updateAlarmStatusDisplay(isEnabled);
                                    updateAlarmFieldsState(isEnabled);

                                    showToast(isEnabled ? "Tüm alarmlar etkinleştirildi" : "Tüm alarmlar devre dışı bırakıldı");
                                });
                            }
                        }

                        @Override
                        public void onError(String message) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    showToast("Alarm master ayarı değiştirilemedi: " + message);
                                    // Switch'i geri al
                                    if (swMasterAlarm != null) {
                                        swMasterAlarm.setChecked(!isEnabled);
                                    }
                                });
                            }
                        }
                    }
            );
        } catch (Exception e) {
            showToast("Alarm ayarı hatası: " + e.getMessage());
            if (swMasterAlarm != null) {
                swMasterAlarm.setChecked(!isEnabled);
            }
        }
    }

    private void updateAlarmStatusDisplay(boolean isEnabled) {
        if (tvAlarmStatus != null) {
            tvAlarmStatus.setText(isEnabled ? "Alarmlar: AÇIK" : "Alarmlar: KAPALI");
            tvAlarmStatus.setTextColor(getResources().getColor(
                    isEnabled ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
        }
    }

    private void updateAlarmFieldsState(boolean isEnabled) {
        if (etTempLowAlarm != null) etTempLowAlarm.setEnabled(isEnabled);
        if (etTempHighAlarm != null) etTempHighAlarm.setEnabled(isEnabled);
        if (etHumidLowAlarm != null) etHumidLowAlarm.setEnabled(isEnabled);
        if (etHumidHighAlarm != null) etHumidHighAlarm.setEnabled(isEnabled);
        if (btnSaveAlarmSettings != null) btnSaveAlarmSettings.setEnabled(isEnabled);

        // Görsel feedback için alpha değerlerini ayarla
        float alpha = isEnabled ? 1.0f : 0.5f;
        if (etTempLowAlarm != null) etTempLowAlarm.setAlpha(alpha);
        if (etTempHighAlarm != null) etTempHighAlarm.setAlpha(alpha);
        if (etHumidLowAlarm != null) etHumidLowAlarm.setAlpha(alpha);
        if (etHumidHighAlarm != null) etHumidHighAlarm.setAlpha(alpha);
        if (btnSaveAlarmSettings != null) btnSaveAlarmSettings.setAlpha(alpha);
    }

    private void saveAlarmSettings() {
        if (!currentAlarmState) {
            showToast("Alarmlar kapalı. Önce alarm sistemini açın.");
            return;
        }

        try {
            float tempLowAlarm = getCurrentTempLowAlarm();
            float tempHighAlarm = getCurrentTempHighAlarm();
            float humidLowAlarm = getCurrentHumidLowAlarm();
            float humidHighAlarm = getCurrentHumidHighAlarm();

            // Değer kontrolü
            if (tempLowAlarm <= 0 || tempHighAlarm <= 0 || humidLowAlarm <= 0 || humidHighAlarm <= 0) {
                showToast("Alarm değerleri sıfırdan büyük olmalıdır");
                return;
            }

            if (tempLowAlarm >= tempHighAlarm) {
                showToast("Düşük sıcaklık alarmı, yüksek sıcaklık alarmından küçük olmalıdır");
                return;
            }

            if (humidLowAlarm >= humidHighAlarm) {
                showToast("Düşük nem alarmı, yüksek nem alarmından küçük olmalıdır");
                return;
            }

            // ESP32'ye alarm ayarlarını gönder
            ApiService.getInstance().setAlarmSettings(
                    currentAlarmState,
                    tempLowAlarm,
                    tempHighAlarm,
                    humidLowAlarm,
                    humidHighAlarm,
                    new ApiService.ParameterCallback() {
                        @Override
                        public void onSuccess() {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    // Yerel olarak kaydet
                                    AlarmSettings settings = new AlarmSettings();
                                    settings.setTempLowAlarm(tempLowAlarm);
                                    settings.setTempHighAlarm(tempHighAlarm);
                                    settings.setHumidLowAlarm(humidLowAlarm);
                                    settings.setHumidHighAlarm(humidHighAlarm);
                                    SharedPrefsManager.getInstance().saveAlarmSettings(settings);

                                    showToast("Alarm ayarları başarıyla ESP32'ye kaydedildi");
                                });
                            }
                        }

                        @Override
                        public void onError(String message) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    showToast("Alarm ayarları kaydedilemedi: " + message);
                                });
                            }
                        }
                    }
            );

        } catch (NumberFormatException e) {
            showToast("Lütfen geçerli sayısal değerler girin");
        } catch (Exception e) {
            showToast("Alarm ayarları kaydetme hatası: " + e.getMessage());
        }
    }

    private float getCurrentTempLowAlarm() {
        try {
            return etTempLowAlarm != null ? Float.parseFloat(etTempLowAlarm.getText().toString()) : 1.0f;
        } catch (NumberFormatException e) {
            return 1.0f;
        }
    }

    private float getCurrentTempHighAlarm() {
        try {
            return etTempHighAlarm != null ? Float.parseFloat(etTempHighAlarm.getText().toString()) : 1.0f;
        } catch (NumberFormatException e) {
            return 1.0f;
        }
    }

    private float getCurrentHumidLowAlarm() {
        try {
            return etHumidLowAlarm != null ? Float.parseFloat(etHumidLowAlarm.getText().toString()) : 10.0f;
        } catch (NumberFormatException e) {
            return 10.0f;
        }
    }

    private float getCurrentHumidHighAlarm() {
        try {
            return etHumidHighAlarm != null ? Float.parseFloat(etHumidHighAlarm.getText().toString()) : 10.0f;
        } catch (NumberFormatException e) {
            return 10.0f;
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