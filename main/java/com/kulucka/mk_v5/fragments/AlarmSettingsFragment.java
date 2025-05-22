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
import com.kulucka.mk_v5.models.AlarmSettings;
import com.kulucka.mk_v5.services.ApiService;
import com.kulucka.mk_v5.utils.SharedPrefsManager;

public class AlarmSettingsFragment extends Fragment {

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
        etTempLowAlarm = view.findViewById(R.id.et_temp_low_alarm);
        etTempHighAlarm = view.findViewById(R.id.et_temp_high_alarm);
        etHumidLowAlarm = view.findViewById(R.id.et_humid_low_alarm);
        etHumidHighAlarm = view.findViewById(R.id.et_humid_high_alarm);
        btnSaveAlarmSettings = view.findViewById(R.id.btn_save_alarm_settings);

        // Kaydet butonuna tıklama işleyicisi
        btnSaveAlarmSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAlarmSettings();
            }
        });

        // Mevcut ayarları yükle
        loadCurrentSettings();

        return view;
    }

    private void loadCurrentSettings() {
        AlarmSettings settings = SharedPrefsManager.getInstance().loadAlarmSettings();

        etTempLowAlarm.setText(String.valueOf(settings.getTempLowAlarm()));
        etTempHighAlarm.setText(String.valueOf(settings.getTempHighAlarm()));
        etHumidLowAlarm.setText(String.valueOf(settings.getHumidLowAlarm()));
        etHumidHighAlarm.setText(String.valueOf(settings.getHumidHighAlarm()));
    }

    private void saveAlarmSettings() {
        try {
            // Değerleri al
            float tempLowAlarm = Float.parseFloat(etTempLowAlarm.getText().toString());
            float tempHighAlarm = Float.parseFloat(etTempHighAlarm.getText().toString());
            float humidLowAlarm = Float.parseFloat(etHumidLowAlarm.getText().toString());
            float humidHighAlarm = Float.parseFloat(etHumidHighAlarm.getText().toString());

            // Geçerlilik kontrolü
            if (tempLowAlarm <= 0 || tempHighAlarm <= 0 || humidLowAlarm <= 0 || humidHighAlarm <= 0) {
                showToast("Alarm değerleri sıfırdan büyük olmalıdır");
                return;
            }

            // Ayarları oluştur
            final AlarmSettings settings = new AlarmSettings();
            settings.setTempLowAlarm(tempLowAlarm);
            settings.setTempHighAlarm(tempHighAlarm);
            settings.setHumidLowAlarm(humidLowAlarm);
            settings.setHumidHighAlarm(humidHighAlarm);

            // Sunucuya gönder
            ApiService.getInstance().setParameter("tempLowAlarm", String.valueOf(tempLowAlarm), new ApiService.ParameterCallback() {
                @Override
                public void onSuccess() {
                    ApiService.getInstance().setParameter("tempHighAlarm", String.valueOf(settings.getTempHighAlarm()), new ApiService.ParameterCallback() {
                        @Override
                        public void onSuccess() {
                            ApiService.getInstance().setParameter("humidLowAlarm", String.valueOf(settings.getHumidLowAlarm()), new ApiService.ParameterCallback() {
                                @Override
                                public void onSuccess() {
                                    ApiService.getInstance().setParameter("humidHighAlarm", String.valueOf(settings.getHumidHighAlarm()), new ApiService.ParameterCallback() {
                                        @Override
                                        public void onSuccess() {
                                            // Başarılı olursa yerel olarak kaydet
                                            SharedPrefsManager.getInstance().saveAlarmSettings(settings);
                                            showToast("Alarm ayarları başarıyla kaydedildi");
                                        }

                                        @Override
                                        public void onError(String message) {
                                            showToast("Hata: " + message);
                                        }
                                    });
                                }

                                @Override
                                public void onError(String message) {
                                    showToast("Hata: " + message);
                                }
                            });
                        }

                        @Override
                        public void onError(String message) {
                            showToast("Hata: " + message);
                        }
                    });
                }

                @Override
                public void onError(String message) {
                    showToast("Hata: " + message);
                }
            });

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