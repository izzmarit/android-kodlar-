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

public class MotorSettingsFragment extends Fragment {

    private EditText etMotorWaitTime;
    private EditText etMotorRunTime;
    private Button btnSaveMotorSettings;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_motor_settings, container, false);

        // Arayüz elemanlarını başlat
        etMotorWaitTime = view.findViewById(R.id.et_motor_wait_time);
        etMotorRunTime = view.findViewById(R.id.et_motor_run_time);
        btnSaveMotorSettings = view.findViewById(R.id.btn_save_motor_settings);

        // Kaydet butonuna tıklama işleyicisi
        btnSaveMotorSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveMotorSettings();
            }
        });

        // Mevcut ayarları yükle
        loadCurrentSettings();

        return view;
    }

    private void loadCurrentSettings() {
        MotorSettings settings = SharedPrefsManager.getInstance().loadMotorSettings();

        etMotorWaitTime.setText(String.valueOf(settings.getMotorWaitTime()));
        etMotorRunTime.setText(String.valueOf(settings.getMotorRunTime()));
    }

    private void saveMotorSettings() {
        try {
            long waitTime = Long.parseLong(etMotorWaitTime.getText().toString());
            long runTime = Long.parseLong(etMotorRunTime.getText().toString());

            if (waitTime <= 0 || runTime <= 0) {
                showToast("Süre değerleri sıfırdan büyük olmalıdır");
                return;
            }

            // JSON verisi oluştur
            try {
                org.json.JSONObject jsonData = new org.json.JSONObject();
                jsonData.put("waitTime", waitTime);
                jsonData.put("runTime", runTime);

                ApiService.getInstance().setParameter("motor", jsonData.toString(), new ApiService.ParameterCallback() {
                    @Override
                    public void onSuccess() {
                        // Yerel olarak kaydet
                        MotorSettings settings = new MotorSettings();
                        settings.setMotorWaitTime(waitTime);
                        settings.setMotorRunTime(runTime);
                        SharedPrefsManager.getInstance().saveMotorSettings(settings);
                        showToast("Motor ayarları başarıyla kaydedildi");
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