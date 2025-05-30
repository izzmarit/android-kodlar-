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

public class CalibrationFragment extends Fragment {

    private EditText etTempCal1;
    private EditText etTempCal2;
    private EditText etHumidCal1;
    private EditText etHumidCal2;
    private Button btnSaveCalibration;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calibration, container, false);

        // Arayüz elemanlarını başlat
        etTempCal1 = view.findViewById(R.id.et_temp_cal_1);
        etTempCal2 = view.findViewById(R.id.et_temp_cal_2);
        etHumidCal1 = view.findViewById(R.id.et_humid_cal_1);
        etHumidCal2 = view.findViewById(R.id.et_humid_cal_2);
        btnSaveCalibration = view.findViewById(R.id.btn_save_calibration);

        // Kaydet butonuna tıklama işleyicisi
        btnSaveCalibration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCalibrationSettings();
            }
        });

        // Mevcut ayarları yükle
        loadCurrentSettings();

        return view;
    }

    private void loadCurrentSettings() {
        CalibrationSettings settings = SharedPrefsManager.getInstance().loadCalibrationSettings();

        etTempCal1.setText(String.valueOf(settings.getTempCalibration1()));
        etTempCal2.setText(String.valueOf(settings.getTempCalibration2()));
        etHumidCal1.setText(String.valueOf(settings.getHumidCalibration1()));
        etHumidCal2.setText(String.valueOf(settings.getHumidCalibration2()));
    }

    private void saveCalibrationSettings() {
        try {
            // Değerleri al
            float tempCal1 = Float.parseFloat(etTempCal1.getText().toString());
            float tempCal2 = Float.parseFloat(etTempCal2.getText().toString());
            float humidCal1 = Float.parseFloat(etHumidCal1.getText().toString());
            float humidCal2 = Float.parseFloat(etHumidCal2.getText().toString());

            // Ayarları oluştur
            final CalibrationSettings settings = new CalibrationSettings();
            settings.setTempCalibration1(tempCal1);
            settings.setTempCalibration2(tempCal2);
            settings.setHumidCalibration1(humidCal1);
            settings.setHumidCalibration2(humidCal2);

            // Sunucuya gönder
            ApiService.getInstance().setParameter("tempCalibration1", String.valueOf(tempCal1), new ApiService.ParameterCallback() {
                @Override
                public void onSuccess() {
                    ApiService.getInstance().setParameter("tempCalibration2", String.valueOf(settings.getTempCalibration2()), new ApiService.ParameterCallback() {
                        @Override
                        public void onSuccess() {
                            ApiService.getInstance().setParameter("humidCalibration1", String.valueOf(settings.getHumidCalibration1()), new ApiService.ParameterCallback() {
                                @Override
                                public void onSuccess() {
                                    ApiService.getInstance().setParameter("humidCalibration2", String.valueOf(settings.getHumidCalibration2()), new ApiService.ParameterCallback() {
                                        @Override
                                        public void onSuccess() {
                                            // Başarılı olursa yerel olarak kaydet
                                            SharedPrefsManager.getInstance().saveCalibrationSettings(settings);
                                            showToast("Kalibrasyon ayarları başarıyla kaydedildi");
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