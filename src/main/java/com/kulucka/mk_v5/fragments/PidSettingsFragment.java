package com.kulucka.mk_v5.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kulucka.mk_v5.R;
import com.kulucka.mk_v5.models.PidSettings;
import com.kulucka.mk_v5.services.ApiService;
import com.kulucka.mk_v5.utils.SharedPrefsManager;

public class PidSettingsFragment extends Fragment {

    private RadioGroup rgPidMode;
    private RadioButton rbPidOff, rbPidManual, rbPidAutoTune;
    private EditText etPidKp;
    private EditText etPidKi;
    private EditText etPidKd;
    private Button btnSavePidSettings;
    private Button btnStartAutoTune;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pid_settings, container, false);

        // Arayüz elemanlarını başlat
        initializeViews(view);
        setupEventListeners();
        loadCurrentSettings();

        return view;
    }

    private void initializeViews(View view) {
        rgPidMode = view.findViewById(R.id.rg_pid_mode);
        rbPidOff = view.findViewById(R.id.rb_pid_off);
        rbPidManual = view.findViewById(R.id.rb_pid_manual);
        rbPidAutoTune = view.findViewById(R.id.rb_pid_auto_tune);

        etPidKp = view.findViewById(R.id.et_pid_kp);
        etPidKi = view.findViewById(R.id.et_pid_ki);
        etPidKd = view.findViewById(R.id.et_pid_kd);

        btnSavePidSettings = view.findViewById(R.id.btn_save_pid_settings);
        btnStartAutoTune = view.findViewById(R.id.btn_start_auto_tune);
    }

    private void setupEventListeners() {
        // PID modu değişikliği event listener
        rgPidMode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                updatePidModeUI(checkedId);
                savePidModeChange(checkedId);
            }
        });

        // Kaydet butonuna tıklama işleyicisi
        btnSavePidSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePidSettings();
            }
        });

        // Otomatik ayarlama başlatma butonu
        btnStartAutoTune.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPidAutoTune();
            }
        });
    }

    private void loadCurrentSettings() {
        PidSettings settings = SharedPrefsManager.getInstance().loadPidSettings();

        etPidKp.setText(String.valueOf(settings.getPidKp()));
        etPidKi.setText(String.valueOf(settings.getPidKi()));
        etPidKd.setText(String.valueOf(settings.getPidKd()));

        // Kaydedilmiş PID modunu yükle
        String savedMode = SharedPrefsManager.getInstance().getPidMode();
        updatePidModeSelection(savedMode);
    }

    private void updatePidModeSelection(String mode) {
        switch (mode) {
            case "off":
                rbPidOff.setChecked(true);
                break;
            case "manual":
                rbPidManual.setChecked(true);
                break;
            case "autoTune":
                rbPidAutoTune.setChecked(true);
                break;
            default:
                rbPidOff.setChecked(true);
                break;
        }
        updatePidModeUI(rgPidMode.getCheckedRadioButtonId());
    }

    private void updatePidModeUI(int checkedId) {
        boolean isManualMode = (checkedId == R.id.rb_pid_manual);
        boolean isAutoTuneMode = (checkedId == R.id.rb_pid_auto_tune);
        boolean isOffMode = (checkedId == R.id.rb_pid_off);

        // Manuel parametreler sadece manuel modda düzenlenebilir
        etPidKp.setEnabled(isManualMode);
        etPidKi.setEnabled(isManualMode);
        etPidKd.setEnabled(isManualMode);
        btnSavePidSettings.setEnabled(isManualMode);

        // Otomatik ayarlama butonu sadece otomatik ayarlama modunda aktif
        btnStartAutoTune.setEnabled(isAutoTuneMode);

        // UI görünümünü güncelle
        if (isOffMode) {
            setFieldsAlpha(0.5f);
        } else if (isManualMode) {
            setFieldsAlpha(1.0f);
        } else if (isAutoTuneMode) {
            setFieldsAlpha(0.7f);
        }
    }

    private void setFieldsAlpha(float alpha) {
        etPidKp.setAlpha(alpha);
        etPidKi.setAlpha(alpha);
        etPidKd.setAlpha(alpha);
        btnSavePidSettings.setAlpha(alpha);
    }

    private void savePidModeChange(int checkedId) {
        String pidMode = "off";

        if (checkedId == R.id.rb_pid_manual) {
            pidMode = "manual";
        } else if (checkedId == R.id.rb_pid_auto_tune) {
            pidMode = "autoTune";
        }

        // PID modunu yerel olarak kaydet
        SharedPrefsManager.getInstance().setPidMode(pidMode);

        // ESP32'ye mod değişikliğini gönder
        sendPidModeToESP32(pidMode);
    }

    private void sendPidModeToESP32(String pidMode) {
        try {
            org.json.JSONObject jsonData = new org.json.JSONObject();
            jsonData.put("pidMode", pidMode);

            ApiService.getInstance().setParameter("pid/mode", jsonData.toString(), new ApiService.ParameterCallback() {
                @Override
                public void onSuccess() {
                    showToast("PID modu değiştirildi: " + getPidModeDisplayName(pidMode));
                }

                @Override
                public void onError(String message) {
                    showToast("PID modu değiştirilemedi: " + message);
                }
            });
        } catch (org.json.JSONException e) {
            showToast("JSON oluşturma hatası: " + e.getMessage());
        }
    }

    private String getPidModeDisplayName(String mode) {
        switch (mode) {
            case "off": return "Kapalı";
            case "manual": return "Manuel";
            case "autoTune": return "Otomatik Ayarlama";
            default: return "Bilinmiyor";
        }
    }

    private void savePidSettings() {
        try {
            float kp = Float.parseFloat(etPidKp.getText().toString());
            float ki = Float.parseFloat(etPidKi.getText().toString());
            float kd = Float.parseFloat(etPidKd.getText().toString());

            if (kp < 0 || ki < 0 || kd < 0) {
                showToast("PID değerleri negatif olamaz");
                return;
            }

            // JSON verisi oluştur
            try {
                org.json.JSONObject jsonData = new org.json.JSONObject();
                jsonData.put("pidMode", "manual");
                jsonData.put("kp", kp);
                jsonData.put("ki", ki);
                jsonData.put("kd", kd);

                ApiService.getInstance().setParameter("pid", jsonData.toString(), new ApiService.ParameterCallback() {
                    @Override
                    public void onSuccess() {
                        // Yerel olarak kaydet
                        PidSettings settings = new PidSettings();
                        settings.setPidKp(kp);
                        settings.setPidKi(ki);
                        settings.setPidKd(kd);
                        SharedPrefsManager.getInstance().savePidSettings(settings);
                        showToast("PID parametreleri başarıyla kaydedildi");
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

    private void startPidAutoTune() {
        try {
            org.json.JSONObject jsonData = new org.json.JSONObject();
            jsonData.put("action", "startAutoTune");

            ApiService.getInstance().setParameter("pid/autotune", jsonData.toString(), new ApiService.ParameterCallback() {
                @Override
                public void onSuccess() {
                    showToast("PID otomatik ayarlama başlatıldı. Bu işlem birkaç dakika sürebilir.");
                    btnStartAutoTune.setEnabled(false);
                    btnStartAutoTune.setText("Otomatik Ayarlama Devam Ediyor...");

                    // 30 saniye sonra butonu tekrar aktif et
                    new android.os.Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (getActivity() != null) {
                                btnStartAutoTune.setEnabled(true);
                                btnStartAutoTune.setText("Otomatik Ayarlamayı Başlat");
                            }
                        }
                    }, 30000);
                }

                @Override
                public void onError(String message) {
                    showToast("Otomatik ayarlama başlatılamadı: " + message);
                }
            });
        } catch (org.json.JSONException e) {
            showToast("JSON oluşturma hatası: " + e.getMessage());
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