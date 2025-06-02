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

public class AlarmSettingsFragment extends Fragment {

    private Switch swMasterAlarm;
    private TextView tvAlarmStatus;
    private EditText etTempLowAlarm;
    private EditText etTempHighAlarm;
    private EditText etHumidLowAlarm;
    private EditText etHumidHighAlarm;
    private Button btnSaveAlarmSettings;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alarm_settings, container, false);

        // Arayüz elemanlarını başlat
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
    }

    private void setupEventListeners() {
        // Master alarm switch event listener
        swMasterAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) { // Sadece kullanıcı etkileşiminde çalışsın
                updateAlarmMasterState(isChecked);
            }
        });

        // Kaydet butonuna tıklama işleyicisi
        btnSaveAlarmSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAlarmSettings();
            }
        });
    }

    private void loadCurrentSettings() {
        // Master alarm durumunu yükle
        boolean masterAlarmEnabled = SharedPrefsManager.getInstance().isMasterAlarmEnabled();
        swMasterAlarm.setChecked(masterAlarmEnabled);
        updateAlarmStatusDisplay(masterAlarmEnabled);
        updateAlarmFieldsState(masterAlarmEnabled);

        // Alarm ayarlarını yükle
        AlarmSettings settings = SharedPrefsManager.getInstance().loadAlarmSettings();
        etTempLowAlarm.setText(String.valueOf(settings.getTempLowAlarm()));
        etTempHighAlarm.setText(String.valueOf(settings.getTempHighAlarm()));
        etHumidLowAlarm.setText(String.valueOf(settings.getHumidLowAlarm()));
        etHumidHighAlarm.setText(String.valueOf(settings.getHumidHighAlarm()));
    }

    private void updateAlarmMasterState(boolean isEnabled) {
        try {
            org.json.JSONObject jsonData = new org.json.JSONObject();
            jsonData.put("masterAlarm", isEnabled);

            ApiService.getInstance().setParameter("alarm/master", jsonData.toString(), new ApiService.ParameterCallback() {
                @Override
                public void onSuccess() {
                    // Yerel olarak kaydet
                    SharedPrefsManager.getInstance().setMasterAlarmEnabled(isEnabled);

                    updateAlarmStatusDisplay(isEnabled);
                    updateAlarmFieldsState(isEnabled);

                    showToast(isEnabled ? "Tüm alarmlar etkinleştirildi" : "Tüm alarmlar devre dışı bırakıldı");
                }

                @Override
                public void onError(String message) {
                    showToast("Alarm master ayarı değiştirilemedi: " + message);
                    // Switch'i geri al
                    swMasterAlarm.setChecked(!isEnabled);
                }
            });
        } catch (org.json.JSONException e) {
            showToast("JSON oluşturma hatası: " + e.getMessage());
            swMasterAlarm.setChecked(!isEnabled);
        }
    }

    private void updateAlarmStatusDisplay(boolean isEnabled) {
        if (tvAlarmStatus != null) {
            tvAlarmStatus.setText(isEnabled ? "Alarmlar: AÇIK" : "Alarmlar: KAPALI");
            tvAlarmStatus.setTextColor(getResources().getColor(
                    isEnabled ? R.color.colorActive : R.color.colorError));
        }
    }

    private void updateAlarmFieldsState(boolean isEnabled) {
        etTempLowAlarm.setEnabled(isEnabled);
        etTempHighAlarm.setEnabled(isEnabled);
        etHumidLowAlarm.setEnabled(isEnabled);
        etHumidHighAlarm.setEnabled(isEnabled);
        btnSaveAlarmSettings.setEnabled(isEnabled);

        // Görsel feedback için alpha değerlerini ayarla
        float alpha = isEnabled ? 1.0f : 0.5f;
        etTempLowAlarm.setAlpha(alpha);
        etTempHighAlarm.setAlpha(alpha);
        etHumidLowAlarm.setAlpha(alpha);
        etHumidHighAlarm.setAlpha(alpha);
        btnSaveAlarmSettings.setAlpha(alpha);
    }

    private void saveAlarmSettings() {
        if (!swMasterAlarm.isChecked()) {
            showToast("Alarmlar kapalı. Önce alarm sistemini açın.");
            return;
        }

        try {
            float tempLowAlarm = Float.parseFloat(etTempLowAlarm.getText().toString());
            float tempHighAlarm = Float.parseFloat(etTempHighAlarm.getText().toString());
            float humidLowAlarm = Float.parseFloat(etHumidLowAlarm.getText().toString());
            float humidHighAlarm = Float.parseFloat(etHumidHighAlarm.getText().toString());

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

            // JSON verisi oluştur
            try {
                org.json.JSONObject jsonData = new org.json.JSONObject();
                jsonData.put("tempLowAlarm", tempLowAlarm);
                jsonData.put("tempHighAlarm", tempHighAlarm);
                jsonData.put("humidLowAlarm", humidLowAlarm);
                jsonData.put("humidHighAlarm", humidHighAlarm);

                ApiService.getInstance().setParameter("alarm", jsonData.toString(), new ApiService.ParameterCallback() {
                    @Override
                    public void onSuccess() {
                        // Yerel olarak kaydet
                        AlarmSettings settings = new AlarmSettings();
                        settings.setTempLowAlarm(tempLowAlarm);
                        settings.setTempHighAlarm(tempHighAlarm);
                        settings.setHumidLowAlarm(humidLowAlarm);
                        settings.setHumidHighAlarm(humidHighAlarm);
                        SharedPrefsManager.getInstance().saveAlarmSettings(settings);

                        showToast("Alarm ayarları başarıyla kaydedildi");
                    }

                    @Override
                    public void onError(String message) {
                        showToast("Hata: " + message);
                    }
                });
            } catch (org.json.JSONException e) {
                showToast("JSON oluşturma hatası: " + e.getMessage());
            }

        } catch (NumberFormatException e) {
            showToast("Lütfen geçerli sayısal değerler girin");
        }
    }

    private void showToast(final String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}