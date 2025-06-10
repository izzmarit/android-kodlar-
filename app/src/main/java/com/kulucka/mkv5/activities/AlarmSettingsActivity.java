package com.kulucka.mkv5.activities;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.kulucka.mkv5.R;
import com.kulucka.mkv5.models.DeviceStatus;
import com.kulucka.mkv5.network.NetworkManager;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlarmSettingsActivity extends AppCompatActivity {

    private Switch switchAlarmEnabled;
    private EditText etTempLowAlarm, etTempHighAlarm, etHumidLowAlarm, etHumidHighAlarm;
    private Button btnSaveSettings;

    private NetworkManager networkManager;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_settings);

        setupToolbar();
        initializeViews();
        setupListeners();

        networkManager = NetworkManager.getInstance(this);

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
        etTempLowAlarm = findViewById(R.id.etTempLowAlarm);
        etTempHighAlarm = findViewById(R.id.etTempHighAlarm);
        etHumidLowAlarm = findViewById(R.id.etHumidLowAlarm);
        etHumidHighAlarm = findViewById(R.id.etHumidHighAlarm);
        btnSaveSettings = findViewById(R.id.btnSaveSettings);
    }

    private void setupListeners() {
        switchAlarmEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            enableDisableInputs(isChecked);
            saveAlarmEnabled(isChecked);
        });

        btnSaveSettings.setOnClickListener(v -> saveAlarmLimits());
    }

    private void loadCurrentValues() {
        showProgressDialog("Değerler yükleniyor...");

        networkManager.getDeviceStatus(new Callback<DeviceStatus>() {
            @Override
            public void onResponse(Call<DeviceStatus> call, Response<DeviceStatus> response) {
                hideProgressDialog();

                if (response.isSuccessful() && response.body() != null) {
                    updateUI(response.body());
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

    private void updateUI(DeviceStatus status) {
        switchAlarmEnabled.setChecked(status.isAlarmEnabled());

        etTempLowAlarm.setText(String.format("%.1f", status.getTempLowAlarm()));
        etTempHighAlarm.setText(String.format("%.1f", status.getTempHighAlarm()));
        etHumidLowAlarm.setText(String.format("%.0f", status.getHumidLowAlarm()));
        etHumidHighAlarm.setText(String.format("%.0f", status.getHumidHighAlarm()));

        enableDisableInputs(status.isAlarmEnabled());
    }

    private void enableDisableInputs(boolean enabled) {
        etTempLowAlarm.setEnabled(enabled);
        etTempHighAlarm.setEnabled(enabled);
        etHumidLowAlarm.setEnabled(enabled);
        etHumidHighAlarm.setEnabled(enabled);
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
        try {
            float tempLow = Float.parseFloat(etTempLowAlarm.getText().toString());
            float tempHigh = Float.parseFloat(etTempHighAlarm.getText().toString());
            float humidLow = Float.parseFloat(etHumidLowAlarm.getText().toString());
            float humidHigh = Float.parseFloat(etHumidHighAlarm.getText().toString());

            // Validate values
            if (tempLow < 20 || tempLow > 40) {
                showError("Düşük sıcaklık alarmı 20-40°C arasında olmalıdır");
                return;
            }

            if (tempHigh < 20 || tempHigh > 40) {
                showError("Yüksek sıcaklık alarmı 20-40°C arasında olmalıdır");
                return;
            }

            if (tempLow >= tempHigh) {
                showError("Düşük sıcaklık alarmı yüksek sıcaklık alarmından küçük olmalıdır");
                return;
            }

            if (humidLow < 0 || humidLow > 100) {
                showError("Düşük nem alarmı 0-100% arasında olmalıdır");
                return;
            }

            if (humidHigh < 0 || humidHigh > 100) {
                showError("Yüksek nem alarmı 0-100% arasında olmalıdır");
                return;
            }

            if (humidLow >= humidHigh) {
                showError("Düşük nem alarmı yüksek nem alarmından küçük olmalıdır");
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