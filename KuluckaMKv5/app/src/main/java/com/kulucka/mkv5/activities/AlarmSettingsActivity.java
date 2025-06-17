package com.kulucka.mkv5.activities;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.kulucka.mkv5.R;
import com.kulucka.mkv5.models.DeviceStatus;
import com.kulucka.mkv5.network.NetworkManager;
import com.kulucka.mkv5.utils.SharedPreferencesManager;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlarmSettingsActivity extends AppCompatActivity {

    private Switch switchAlarmEnabled;
    private EditText etTempHighOffset, etTempLowOffset, etHumidHighOffset, etHumidLowOffset;
    private TextView tvTargetTemp, tvTargetHumid;
    private TextView tvTempHighLimit, tvTempLowLimit, tvHumidHighLimit, tvHumidLowLimit;
    private Button btnSaveSettings;

    private NetworkManager networkManager;
    private SharedPreferencesManager prefsManager;
    private ProgressDialog progressDialog;
    private DeviceStatus currentStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_settings);

        setupToolbar();
        initializeViews();
        setupListeners();

        networkManager = NetworkManager.getInstance(this);
        prefsManager = SharedPreferencesManager.getInstance(this);

        // Load current values
        loadCurrentValues();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Alarm Ayarları");
        }
    }

    private void initializeViews() {
        switchAlarmEnabled = findViewById(R.id.switchAlarmEnabled);

        etTempHighOffset = findViewById(R.id.etTempHighOffset);
        etTempLowOffset = findViewById(R.id.etTempLowOffset);
        etHumidHighOffset = findViewById(R.id.etHumidHighOffset);
        etHumidLowOffset = findViewById(R.id.etHumidLowOffset);

        tvTargetTemp = findViewById(R.id.tvTargetTemp);
        tvTargetHumid = findViewById(R.id.tvTargetHumid);

        tvTempHighLimit = findViewById(R.id.tvTempHighLimit);
        tvTempLowLimit = findViewById(R.id.tvTempLowLimit);
        tvHumidHighLimit = findViewById(R.id.tvHumidHighLimit);
        tvHumidLowLimit = findViewById(R.id.tvHumidLowLimit);

        btnSaveSettings = findViewById(R.id.btnSaveSettings);
    }

    private void setupListeners() {
        switchAlarmEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            enableDisableInputs(isChecked);
            saveAlarmEnabled(isChecked);
        });

        // Virgül desteği ve limit hesaplama için TextWatcher
        TextWatcher offsetWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Virgülü noktaya çevir
                String text = s.toString();
                if (text.contains(",")) {
                    s.replace(0, s.length(), text.replace(',', '.'));
                }
                calculateAndDisplayLimits();

                // Değerleri otomatik olarak kaydet
                saveOffsetValues();
            }
        };

        etTempHighOffset.addTextChangedListener(offsetWatcher);
        etTempLowOffset.addTextChangedListener(offsetWatcher);
        etHumidHighOffset.addTextChangedListener(offsetWatcher);
        etHumidLowOffset.addTextChangedListener(offsetWatcher);

        btnSaveSettings.setOnClickListener(v -> saveAlarmLimits());
    }

    private void loadCurrentValues() {
        showProgressDialog("Değerler yükleniyor...");

        networkManager.getDeviceStatus(new Callback<DeviceStatus>() {
            @Override
            public void onResponse(Call<DeviceStatus> call, Response<DeviceStatus> response) {
                hideProgressDialog();

                if (response.isSuccessful() && response.body() != null) {
                    currentStatus = response.body();
                    updateUI();
                } else {
                    showError("Değerler yüklenemedi");
                }
            }

            @Override
            public void onFailure(Call<DeviceStatus> call, Throwable t) {
                hideProgressDialog();
                showError("Bağlantı hatası: " + t.getMessage());
            }
        });
    }

    private void updateUI() {
        if (currentStatus == null) return;

        switchAlarmEnabled.setChecked(currentStatus.isAlarmEnabled());

        // Hedef değerleri göster
        tvTargetTemp.setText(String.format("%.1f°C", currentStatus.getTargetTemp()).replace('.', ','));
        tvTargetHumid.setText(String.format("%.0f%%", currentStatus.getTargetHumid()));

        // Kaydedilmiş sapma değerlerini yükle - önce SharedPreferences'tan kontrol et
        float savedTempHighOffset = prefsManager.getTempHighOffset();
        float savedTempLowOffset = prefsManager.getTempLowOffset();
        float savedHumidHighOffset = prefsManager.getHumidHighOffset();
        float savedHumidLowOffset = prefsManager.getHumidLowOffset();

        // Eğer kaydedilmiş değerler varsa onları kullan, yoksa mevcut alarm değerlerinden hesapla
        float tempHighOffset, tempLowOffset, humidHighOffset, humidLowOffset;

        // Mevcut limitlerin hedef değerlerle farkını hesapla
        float currentTempHighOffset = currentStatus.getTempHighAlarm() - currentStatus.getTargetTemp();
        float currentTempLowOffset = currentStatus.getTargetTemp() - currentStatus.getTempLowAlarm();
        float currentHumidHighOffset = currentStatus.getHumidHighAlarm() - currentStatus.getTargetHumid();
        float currentHumidLowOffset = currentStatus.getTargetHumid() - currentStatus.getHumidLowAlarm();

        // Eğer ESP32'den gelen değerler makul ise (0-10 arası) onları kullan, değilse kaydedilenleri kullan
        if (Math.abs(currentTempHighOffset) > 0.1 && Math.abs(currentTempHighOffset) <= 5.0) {
            tempHighOffset = Math.abs(currentTempHighOffset);
            prefsManager.saveTempHighOffset(tempHighOffset); // Güncelle
        } else {
            tempHighOffset = savedTempHighOffset;
        }

        if (Math.abs(currentTempLowOffset) > 0.1 && Math.abs(currentTempLowOffset) <= 5.0) {
            tempLowOffset = Math.abs(currentTempLowOffset);
            prefsManager.saveTempLowOffset(tempLowOffset);
        } else {
            tempLowOffset = savedTempLowOffset;
        }

        if (Math.abs(currentHumidHighOffset) > 0.1 && Math.abs(currentHumidHighOffset) <= 20.0) {
            humidHighOffset = Math.abs(currentHumidHighOffset);
            prefsManager.saveHumidHighOffset(humidHighOffset);
        } else {
            humidHighOffset = savedHumidHighOffset;
        }

        if (Math.abs(currentHumidLowOffset) > 0.1 && Math.abs(currentHumidLowOffset) <= 20.0) {
            humidLowOffset = Math.abs(currentHumidLowOffset);
            prefsManager.saveHumidLowOffset(humidLowOffset);
        } else {
            humidLowOffset = savedHumidLowOffset;
        }

        // Sapma değerlerini göster (virgülle)
        etTempHighOffset.setText(String.format("%.1f", tempHighOffset).replace('.', ','));
        etTempLowOffset.setText(String.format("%.1f", tempLowOffset).replace('.', ','));
        etHumidHighOffset.setText(String.format("%.0f", humidHighOffset));
        etHumidLowOffset.setText(String.format("%.0f", humidLowOffset));

        enableDisableInputs(currentStatus.isAlarmEnabled());
        calculateAndDisplayLimits();
    }

    private void calculateAndDisplayLimits() {
        if (currentStatus == null) return;

        try {
            float targetTemp = currentStatus.getTargetTemp();
            float targetHumid = currentStatus.getTargetHumid();

            // Sıcaklık limitleri
            String tempHighStr = etTempHighOffset.getText().toString().replace(',', '.');
            String tempLowStr = etTempLowOffset.getText().toString().replace(',', '.');

            if (!tempHighStr.isEmpty()) {
                float tempHighOffset = Float.parseFloat(tempHighStr);
                float tempHighLimit = targetTemp + tempHighOffset;
                tvTempHighLimit.setText(String.format("= %.1f°C", tempHighLimit).replace('.', ','));
            } else {
                tvTempHighLimit.setText("= --,--°C");
            }

            if (!tempLowStr.isEmpty()) {
                float tempLowOffset = Float.parseFloat(tempLowStr);
                float tempLowLimit = targetTemp - tempLowOffset;
                tvTempLowLimit.setText(String.format("= %.1f°C", tempLowLimit).replace('.', ','));
            } else {
                tvTempLowLimit.setText("= --,--°C");
            }

            // Nem limitleri
            String humidHighStr = etHumidHighOffset.getText().toString().replace(',', '.');
            String humidLowStr = etHumidLowOffset.getText().toString().replace(',', '.');

            if (!humidHighStr.isEmpty()) {
                float humidHighOffset = Float.parseFloat(humidHighStr);
                float humidHighLimit = targetHumid + humidHighOffset;
                tvHumidHighLimit.setText(String.format("= %.0f%%", humidHighLimit));
            } else {
                tvHumidHighLimit.setText("= --%");
            }

            if (!humidLowStr.isEmpty()) {
                float humidLowOffset = Float.parseFloat(humidLowStr);
                float humidLowLimit = targetHumid - humidLowOffset;
                tvHumidLowLimit.setText(String.format("= %.0f%%", humidLowLimit));
            } else {
                tvHumidLowLimit.setText("= --%");
            }

        } catch (NumberFormatException e) {
            // Hatalı giriş, limitleri gösterme
        }
    }

    // Yeni metod: Sapma değerlerini kaydet
    private void saveOffsetValues() {
        try {
            String tempHighStr = etTempHighOffset.getText().toString().replace(',', '.');
            String tempLowStr = etTempLowOffset.getText().toString().replace(',', '.');
            String humidHighStr = etHumidHighOffset.getText().toString().replace(',', '.');
            String humidLowStr = etHumidLowOffset.getText().toString().replace(',', '.');

            if (!tempHighStr.isEmpty()) {
                prefsManager.saveTempHighOffset(Float.parseFloat(tempHighStr));
            }
            if (!tempLowStr.isEmpty()) {
                prefsManager.saveTempLowOffset(Float.parseFloat(tempLowStr));
            }
            if (!humidHighStr.isEmpty()) {
                prefsManager.saveHumidHighOffset(Float.parseFloat(humidHighStr));
            }
            if (!humidLowStr.isEmpty()) {
                prefsManager.saveHumidLowOffset(Float.parseFloat(humidLowStr));
            }
        } catch (NumberFormatException e) {
            // Geçersiz değer durumunda sessizce devam et
        }
    }

    private void enableDisableInputs(boolean enabled) {
        etTempHighOffset.setEnabled(enabled);
        etTempLowOffset.setEnabled(enabled);
        etHumidHighOffset.setEnabled(enabled);
        etHumidLowOffset.setEnabled(enabled);
        btnSaveSettings.setEnabled(enabled);
    }

    private void saveAlarmEnabled(boolean enabled) {
        showProgressDialog("Alarm durumu güncelleniyor...");

        networkManager.setAlarmEnabled(enabled, new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                hideProgressDialog();

                if (response.isSuccessful()) {
                    String message = enabled ? "Alarmlar açıldı" : "Alarmlar kapatıldı";
                    showSuccess(message);
                    setResult(RESULT_OK);
                } else {
                    showError("Alarm durumu güncellenemedi");
                    // Revert switch state on error
                    switchAlarmEnabled.setChecked(!enabled);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                hideProgressDialog();
                showError("Bağlantı hatası: " + t.getMessage());
                // Revert switch state on error
                switchAlarmEnabled.setChecked(!enabled);
            }
        });
    }

    private void saveAlarmLimits() {
        if (currentStatus == null) return;

        try {
            // Sapma değerlerini al
            float tempHighOffset = Float.parseFloat(etTempHighOffset.getText().toString().replace(',', '.'));
            float tempLowOffset = Float.parseFloat(etTempLowOffset.getText().toString().replace(',', '.'));
            float humidHighOffset = Float.parseFloat(etHumidHighOffset.getText().toString().replace(',', '.'));
            float humidLowOffset = Float.parseFloat(etHumidLowOffset.getText().toString().replace(',', '.'));

            // Validate offset values
            if (tempHighOffset < 0 || tempHighOffset > 5) {
                showError("Yüksek sıcaklık sapması 0-5°C arasında olmalıdır");
                return;
            }

            if (tempLowOffset < 0 || tempLowOffset > 5) {
                showError("Düşük sıcaklık sapması 0-5°C arasında olmalıdır");
                return;
            }

            if (humidHighOffset < 0 || humidHighOffset > 20) {
                showError("Yüksek nem sapması 0-20% arasında olmalıdır");
                return;
            }

            if (humidLowOffset < 0 || humidLowOffset > 20) {
                showError("Düşük nem sapması 0-20% arasında olmalıdır");
                return;
            }

            // Limitleri hesapla
            float targetTemp = currentStatus.getTargetTemp();
            float targetHumid = currentStatus.getTargetHumid();

            float tempHigh = targetTemp + tempHighOffset;
            float tempLow = targetTemp - tempLowOffset;
            float humidHigh = targetHumid + humidHighOffset;
            float humidLow = targetHumid - humidLowOffset;

            // Limitlerin mantıklı olup olmadığını kontrol et
            if (tempHigh > 45 || tempLow < 20) {
                showError("Hesaplanan sıcaklık limitleri 20-45°C arasında olmalıdır");
                return;
            }

            if (humidHigh > 100 || humidLow < 0) {
                showError("Hesaplanan nem limitleri 0-100% arasında olmalıdır");
                return;
            }

            showProgressDialog("Alarm limitleri kaydediliyor...");

            networkManager.setAlarmLimits(tempLow, tempHigh, humidLow, humidHigh,
                    new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            hideProgressDialog();

                            if (response.isSuccessful()) {
                                showSuccess("Alarm limitleri güncellendi");
                                setResult(RESULT_OK);
                                loadCurrentValues();
                            } else {
                                showError("Alarm limitleri güncellenemedi");
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            hideProgressDialog();
                            showError("Bağlantı hatası: " + t.getMessage());
                        }
                    });

        } catch (NumberFormatException e) {
            showError("Geçersiz sayı formatı");
        }
    }

    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideProgressDialog();
    }
}