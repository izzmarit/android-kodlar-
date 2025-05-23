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
import com.kulucka.mk_v5.services.ApiService;
import com.kulucka.mk_v5.utils.Constants;

public class IncubationSettingsFragment extends Fragment {

    private RadioGroup rgIncubationType;
    private RadioButton rbChicken, rbQuail, rbGoose, rbManual;
    private EditText etDevTemp, etHatchTemp, etDevHumid, etHatchHumid, etDevDays, etHatchDays;
    private Button btnSaveSettings, btnStartStop;

    private boolean isIncubationRunning = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_incubation_settings, container, false);

        // Arayüz elemanlarını başlat
        rgIncubationType = view.findViewById(R.id.rg_incubation_type);
        rbChicken = view.findViewById(R.id.rb_chicken);
        rbQuail = view.findViewById(R.id.rb_quail);
        rbGoose = view.findViewById(R.id.rb_goose);
        rbManual = view.findViewById(R.id.rb_manual);

        etDevTemp = view.findViewById(R.id.et_dev_temp);
        etHatchTemp = view.findViewById(R.id.et_hatch_temp);
        etDevHumid = view.findViewById(R.id.et_dev_humid);
        etHatchHumid = view.findViewById(R.id.et_hatch_humid);
        etDevDays = view.findViewById(R.id.et_dev_days);
        etHatchDays = view.findViewById(R.id.et_hatch_days);

        btnSaveSettings = view.findViewById(R.id.btn_save_settings);
        btnStartStop = view.findViewById(R.id.btn_start_stop);

        // Manuel mod alanlarını devre dışı bırak (başlangıçta)
        setManualFieldsEnabled(false);

        // Manuel mod seçildiğinde alanları etkinleştir
        rgIncubationType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                setManualFieldsEnabled(checkedId == R.id.rb_manual);
            }
        });

        // Ayarları kaydet butonuna tıklama
        btnSaveSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });

        // Başlat/Durdur butonuna tıklama
        btnStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleIncubation();
            }
        });

        // Mevcut durumu yükle
        loadCurrentState();

        return view;
    }

    private void setManualFieldsEnabled(boolean enabled) {
        etDevTemp.setEnabled(enabled);
        etHatchTemp.setEnabled(enabled);
        etDevHumid.setEnabled(enabled);
        etHatchHumid.setEnabled(enabled);
        etDevDays.setEnabled(enabled);
        etHatchDays.setEnabled(enabled);
    }

    private void saveSettings() {
        // Kuluçka tipini belirle
        int incubationType;

        if (rbChicken.isChecked()) {
            incubationType = Constants.INCUBATION_CHICKEN;
        } else if (rbQuail.isChecked()) {
            incubationType = Constants.INCUBATION_QUAIL;
        } else if (rbGoose.isChecked()) {
            incubationType = Constants.INCUBATION_GOOSE;
        } else {
            incubationType = Constants.INCUBATION_MANUAL;
        }

        // İlk olarak kuluçka tipini ayarla
        ApiService.getInstance().setParameter("incubationType", String.valueOf(incubationType), new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                if (incubationType == Constants.INCUBATION_MANUAL) {
                    saveManualParameters();
                } else {
                    showToast("Kuluçka tipi başarıyla kaydedildi");
                }
            }

            @Override
            public void onError(String message) {
                showToast("Hata: " + message);
            }
        });
    }

    private void saveManualParameters() {
        try {
            // Manuel parametreleri al
            final float devTemp = Float.parseFloat(etDevTemp.getText().toString());
            final float hatchTemp = Float.parseFloat(etHatchTemp.getText().toString());
            final int devHumid = Integer.parseInt(etDevHumid.getText().toString());
            final int hatchHumid = Integer.parseInt(etHatchHumid.getText().toString());
            final int devDays = Integer.parseInt(etDevDays.getText().toString());
            final int hatchDays = Integer.parseInt(etHatchDays.getText().toString());

            // Parametreleri sırayla gönder
            ApiService.getInstance().setParameter("manualDevTemp", String.valueOf(devTemp), new ApiService.ParameterCallback() {
                @Override
                public void onSuccess() {
                    ApiService.getInstance().setParameter("manualHatchTemp", String.valueOf(hatchTemp), new ApiService.ParameterCallback() {
                        @Override
                        public void onSuccess() {
                            ApiService.getInstance().setParameter("manualDevHumid", String.valueOf(devHumid), new ApiService.ParameterCallback() {
                                @Override
                                public void onSuccess() {
                                    ApiService.getInstance().setParameter("manualHatchHumid", String.valueOf(hatchHumid), new ApiService.ParameterCallback() {
                                        @Override
                                        public void onSuccess() {
                                            ApiService.getInstance().setParameter("manualDevDays", String.valueOf(devDays), new ApiService.ParameterCallback() {
                                                @Override
                                                public void onSuccess() {
                                                    ApiService.getInstance().setParameter("manualHatchDays", String.valueOf(hatchDays), new ApiService.ParameterCallback() {
                                                        @Override
                                                        public void onSuccess() {
                                                            showToast("Tüm manuel ayarlar başarıyla kaydedildi");
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

    private void toggleIncubation() {
        final String paramValue = isIncubationRunning ? "0" : "1";

        ApiService.getInstance().setParameter("isIncubationRunning", paramValue, new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                isIncubationRunning = !isIncubationRunning;
                updateStartStopButton();
                showToast(isIncubationRunning ? "Kuluçka başlatıldı" : "Kuluçka durduruldu");
            }

            @Override
            public void onError(String message) {
                showToast("Hata: " + message);
            }
        });
    }

    private void loadCurrentState() {
        ApiService.getInstance().refreshStatus();
        // Burada mevcut kuluçka tipini ve durumunu yükleyebiliriz
        // İdeal olarak, ESP32'den bu bilgileri almalıyız

        updateStartStopButton();
    }

    private void updateStartStopButton() {
        btnStartStop.setText(isIncubationRunning ? "Kuluçkayı Durdur" : "Kuluçkayı Başlat");
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