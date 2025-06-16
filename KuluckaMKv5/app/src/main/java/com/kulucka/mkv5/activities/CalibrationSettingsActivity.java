package com.kulucka.mkv5.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
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

public class CalibrationSettingsActivity extends AppCompatActivity {

    // Sensor 1 views
    private TextView tvSensor1Temp, tvSensor1Humid;
    private EditText etSensor1TempCal, etSensor1HumidCal;
    private Button btnSaveSensor1;

    // Sensor 2 views
    private TextView tvSensor2Temp, tvSensor2Humid;
    private EditText etSensor2TempCal, etSensor2HumidCal;
    private Button btnSaveSensor2;

    private Button btnResetAll;

    private NetworkManager networkManager;
    private ProgressDialog progressDialog;
    private DeviceStatus currentStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration_settings);

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
            getSupportActionBar().setTitle("Kalibrasyon Ayarları");
        }
    }

    private void initializeViews() {
        // Sensor 1
        tvSensor1Temp = findViewById(R.id.tvSensor1Temp);
        tvSensor1Humid = findViewById(R.id.tvSensor1Humid);
        etSensor1TempCal = findViewById(R.id.etSensor1TempCal);
        etSensor1HumidCal = findViewById(R.id.etSensor1HumidCal);
        btnSaveSensor1 = findViewById(R.id.btnSaveSensor1);

        // Sensor 2
        tvSensor2Temp = findViewById(R.id.tvSensor2Temp);
        tvSensor2Humid = findViewById(R.id.tvSensor2Humid);
        etSensor2TempCal = findViewById(R.id.etSensor2TempCal);
        etSensor2HumidCal = findViewById(R.id.etSensor2HumidCal);
        btnSaveSensor2 = findViewById(R.id.btnSaveSensor2);

        btnResetAll = findViewById(R.id.btnResetAll);
    }

    private void setupListeners() {
        // Ondalık ayırıcı desteği için TextWatcher'lar ekleyin
        etSensor1TempCal.addTextChangedListener(new DecimalInputTextWatcher(etSensor1TempCal));
        etSensor1HumidCal.addTextChangedListener(new DecimalInputTextWatcher(etSensor1HumidCal));
        etSensor2TempCal.addTextChangedListener(new DecimalInputTextWatcher(etSensor2TempCal));
        etSensor2HumidCal.addTextChangedListener(new DecimalInputTextWatcher(etSensor2HumidCal));

        btnSaveSensor1.setOnClickListener(v -> saveSensorCalibration(1));
        btnSaveSensor2.setOnClickListener(v -> saveSensorCalibration(2));
        btnResetAll.setOnClickListener(v -> resetAllCalibration());
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

        // Sensor readings
        tvSensor1Temp.setText(String.format("%.1f°C", currentStatus.getTemperature()));
        tvSensor1Humid.setText(String.format("%d%%", Math.round(currentStatus.getHumidity())));
        tvSensor2Temp.setText(String.format("%.1f°C", currentStatus.getTemperature()));
        tvSensor2Humid.setText(String.format("%d%%", Math.round(currentStatus.getHumidity())));

        // Calibration values - virgülle göster
        etSensor1TempCal.setText(String.format("%.1f", currentStatus.getTempCalibration1()).replace('.', ','));
        etSensor1HumidCal.setText(String.format("%.1f", currentStatus.getHumidCalibration1()).replace('.', ','));
        etSensor2TempCal.setText(String.format("%.1f", currentStatus.getTempCalibration2()).replace('.', ','));
        etSensor2HumidCal.setText(String.format("%.1f", currentStatus.getHumidCalibration2()).replace('.', ','));
    }

    private void saveSensorCalibration(int sensorNumber) {
        try {
            String tempCalStr, humidCalStr;

            if (sensorNumber == 1) {
                tempCalStr = etSensor1TempCal.getText().toString().trim();
                humidCalStr = etSensor1HumidCal.getText().toString().trim();
            } else {
                tempCalStr = etSensor2TempCal.getText().toString().trim();
                humidCalStr = etSensor2HumidCal.getText().toString().trim();
            }

            // Boş değer kontrolü
            if (tempCalStr.isEmpty()) tempCalStr = "0.0";
            if (humidCalStr.isEmpty()) humidCalStr = "0.0";

            // Virgülü noktaya çevir
            tempCalStr = tempCalStr.replace(',', '.');
            humidCalStr = humidCalStr.replace(',', '.');

            Log.d("Calibration", "Sensör " + sensorNumber + " - Temp Cal: " + tempCalStr + ", Humid Cal: " + humidCalStr);

            float tempCal = Float.parseFloat(tempCalStr);
            float humidCal = Float.parseFloat(humidCalStr);

            // Kalibrasyon değerlerini doğrula
            if (Math.abs(tempCal) > 5.0) {
                showError("Sıcaklık kalibrasyonu -5.0 ile +5.0 arasında olmalıdır");
                return;
            }

            if (Math.abs(humidCal) > 10.0) {
                showError("Nem kalibrasyonu -10.0 ile +10.0 arasında olmalıdır");
                return;
            }

            showProgressDialog("Kalibrasyon kaydediliyor...");

            networkManager.setCalibration(sensorNumber, tempCal, humidCal, new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    hideProgressDialog();

                    if (response.isSuccessful()) {
                        showSuccess("Sensör " + sensorNumber + " kalibrasyonu güncellendi");
                        setResult(RESULT_OK);
                        loadCurrentValues();
                    } else {
                        String errorMessage = "Kalibrasyon güncellenemedi - HTTP: " + response.code();
                        try {
                            if (response.errorBody() != null) {
                                String errorBody = response.errorBody().string();
                                if (!errorBody.isEmpty()) {
                                    errorMessage += " - " + errorBody;
                                }
                            }
                        } catch (IOException e) {
                            // Error body okunamadı
                        }
                        showError(errorMessage);
                        Log.e("Calibration", errorMessage);
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    hideProgressDialog();
                    showError("Bağlantı hatası: " + t.getMessage());
                    Log.e("Calibration", "Network Error: " + t.getMessage());
                }
            });

        } catch (NumberFormatException e) {
            showError("Geçersiz kalibrasyon değeri: " + e.getMessage());
            Log.e("Calibration", "Number format error: " + e.getMessage());
        } catch (Exception e) {
            showError("Beklenmeyen hata: " + e.getMessage());
            Log.e("Calibration", "Unexpected error: " + e.getMessage());
        }
    }

    private void resetAllCalibration() {
        showProgressDialog("Kalibrasyon sıfırlanıyor...");

        // Reset sensor 1
        networkManager.setCalibration(1, 0.0f, 0.0f, new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    // Reset sensor 2
                    networkManager.setCalibration(2, 0.0f, 0.0f, new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call2, Response<ResponseBody> response2) {
                            hideProgressDialog();

                            if (response2.isSuccessful()) {
                                showSuccess("Tüm kalibrasyonlar sıfırlandı");
                                loadCurrentValues();
                            } else {
                                showError("Sensör 2 kalibrasyonu sıfırlanamadı");
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call2, Throwable t) {
                            hideProgressDialog();
                            showError("Bağlantı hatası: " + t.getMessage());
                        }
                    });
                } else {
                    hideProgressDialog();
                    showError("Sensör 1 kalibrasyonu sıfırlanamadı");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                hideProgressDialog();
                showError("Bağlantı hatası: " + t.getMessage());
            }
        });
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

    private class DecimalInputTextWatcher implements TextWatcher {
        private boolean isEditing = false;
        private EditText editText;

        public DecimalInputTextWatcher(EditText editText) {
            this.editText = editText;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            if (isEditing) return;

            isEditing = true;
            String input = s.toString();

            if (input.isEmpty()) {
                isEditing = false;
                return;
            }

            try {
                String cleanInput = input.replace(',', '.');
                StringBuilder cleaned = new StringBuilder();
                boolean hasDot = false;
                boolean hasNegative = false;

                for (int i = 0; i < cleanInput.length(); i++) {
                    char c = cleanInput.charAt(i);

                    if (c == '-' && i == 0 && !hasNegative) {
                        cleaned.append(c);
                        hasNegative = true;
                    } else if (c == '.' && !hasDot) {
                        cleaned.append(c);
                        hasDot = true;
                    } else if (Character.isDigit(c)) {
                        cleaned.append(c);
                    }
                }

                String finalInput = cleaned.toString();

                // Geçerli sayı formatı kontrolü
                if (!finalInput.isEmpty() && !finalInput.equals(".") && !finalInput.equals("-")) {
                    try {
                        Double.parseDouble(finalInput);

                        // Ondalık kısmını 1 haneyle sınırla (kalibrasyon için)
                        if (finalInput.contains(".")) {
                            String[] parts = finalInput.split("\\.");
                            if (parts.length == 2 && parts[1].length() > 1) {
                                finalInput = parts[0] + "." + parts[1].substring(0, 1);
                            }
                        }

                        if (!input.equals(finalInput)) {
                            editText.setText(finalInput);
                            editText.setSelection(finalInput.length());
                        }
                    } catch (NumberFormatException e) {
                        if (finalInput.length() > 1) {
                            finalInput = finalInput.substring(0, finalInput.length() - 1);
                            editText.setText(finalInput);
                            editText.setSelection(finalInput.length());
                        }
                    }
                }

            } catch (Exception e) {
                Log.e("CalibrationInput", "TextWatcher hatası: " + e.getMessage());
            }

            isEditing = false;
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