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
import com.kulucka.mk_v5.models.PidSettings;
import com.kulucka.mk_v5.services.ApiService;
import com.kulucka.mk_v5.utils.SharedPrefsManager;

public class PidSettingsFragment extends Fragment {

    private EditText etPidKp;
    private EditText etPidKi;
    private EditText etPidKd;
    private Button btnSavePidSettings;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pid_settings, container, false);

        // Arayüz elemanlarını başlat
        etPidKp = view.findViewById(R.id.et_pid_kp);
        etPidKi = view.findViewById(R.id.et_pid_ki);
        etPidKd = view.findViewById(R.id.et_pid_kd);
        btnSavePidSettings = view.findViewById(R.id.btn_save_pid_settings);

        // Kaydet butonuna tıklama işleyicisi
        btnSavePidSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePidSettings();
            }
        });

        // Mevcut ayarları yükle
        loadCurrentSettings();

        return view;
    }

    private void loadCurrentSettings() {
        PidSettings settings = SharedPrefsManager.getInstance().loadPidSettings();

        etPidKp.setText(String.valueOf(settings.getPidKp()));
        etPidKi.setText(String.valueOf(settings.getPidKi()));
        etPidKd.setText(String.valueOf(settings.getPidKd()));
    }

    private void savePidSettings() {
        try {
            // Değerleri al
            float kp = Float.parseFloat(etPidKp.getText().toString());
            float ki = Float.parseFloat(etPidKi.getText().toString());
            float kd = Float.parseFloat(etPidKd.getText().toString());

            // Geçerlilik kontrolü
            if (kp < 0 || ki < 0 || kd < 0) {
                showToast("PID değerleri negatif olamaz");
                return;
            }

            // Ayarları kaydet
            final PidSettings settings = new PidSettings();
            settings.setPidKp(kp);
            settings.setPidKi(ki);
            settings.setPidKd(kd);

            // Önce sunucuya gönder
            ApiService.getInstance().setParameter("pidKp", String.valueOf(kp), new ApiService.ParameterCallback() {
                @Override
                public void onSuccess() {
                    ApiService.getInstance().setParameter("pidKi", String.valueOf(settings.getPidKi()), new ApiService.ParameterCallback() {
                        @Override
                        public void onSuccess() {
                            ApiService.getInstance().setParameter("pidKd", String.valueOf(settings.getPidKd()), new ApiService.ParameterCallback() {
                                @Override
                                public void onSuccess() {
                                    // Başarılı olursa yerel olarak kaydet
                                    SharedPrefsManager.getInstance().savePidSettings(settings);
                                    showToast("PID ayarları başarıyla kaydedildi");
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