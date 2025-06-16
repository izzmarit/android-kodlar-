package com.kulucka.mkv5.activities;

import android.app.ProgressDialog;
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

public class TemperatureHumiditySettingsActivity extends AppCompatActivity {

    private TextView tvCurrentTemp, tvCurrentHumid;
    private EditText etTargetTemp, etTargetHumid;
    private Button btnSaveTemp, btnSaveHumid;

    private NetworkManager networkManager;
    private ProgressDialog progressDialog;
    private boolean hasChanges = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temperature_humidity_settings);

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
            getSupportActionBar().setTitle("Sıcaklık ve Nem Ayarları");
        }
    }

    private void initializeViews() {
        tvCurrentTemp = findViewById(R.id.tvCurrentTemp);
        tvCurrentHumid = findViewById(R.id.tvCurrentHumid);
        etTargetTemp = findViewById(R.id.etTargetTemp);
        etTargetHumid = findViewById(R.id.etTargetHumid);
        btnSaveTemp = findViewById(R.id.btnSaveTemp);
        btnSaveHumid = findViewById(R.id.btnSaveHumid);
    }

    private void setupListeners() {
        // Virgül desteği için özel TextWatcher ekle
        etTargetTemp.addTextChangedListener(new DecimalInputTextWatcher(etTargetTemp));
        etTargetHumid.addTextChangedListener(new DecimalInputTextWatcher(etTargetHumid));

        btnSaveTemp.setOnClickListener(v -> saveTemperature());
        btnSaveHumid.setOnClickListener(v -> saveHumidity());
    }

    private void loadCurrentValues() {
        showProgressDialog("Değerler yükleniyor...");

        networkManager.getDeviceStatus(new Callback<DeviceStatus>() {
            @Override
            public void onResponse(Call<DeviceStatus> call, Response<DeviceStatus> response) {
                hideProgressDialog();

                if (response.isSuccessful() && response.body() != null) {
                    DeviceStatus status = response.body();
                    updateUI(status);
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
        tvCurrentTemp.setText(String.format("%.1f°C", status.getTemperature()).replace('.', ','));
        tvCurrentHumid.setText(String.format("%d%%", Math.round(status.getHumidity())));
        etTargetTemp.setText(String.format("%.1f", status.getTargetTemp()).replace('.', ','));
        etTargetHumid.setText(String.format("%d", Math.round(status.getTargetHumid())));
    }

    private void saveTemperature() {
        String tempStr = etTargetTemp.getText().toString().trim();
        if (tempStr.isEmpty()) {
            showError("Lütfen hedef sıcaklık değeri girin");
            return;
        }

        try {
            tempStr = tempStr.replace(',', '.');
            float temperature = Float.parseFloat(tempStr);

            if (temperature < 30 || temperature > 40) {
                showError("Sıcaklık değeri 30-40°C arasında olmalıdır");
                return;
            }

            showProgressDialog("Sıcaklık ayarlanıyor...");

            networkManager.setTemperature(temperature, new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    hideProgressDialog();

                    if (response.isSuccessful()) {
                        showSuccess("Hedef sıcaklık güncellendi");
                        hasChanges = true;
                        setResult(RESULT_OK);

                        // Ana ekrana dön
                        finish();
                    } else {
                        showError("Sıcaklık güncellenemedi");
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    hideProgressDialog();
                    showError("Bağlantı hatası: " + t.getMessage());
                }
            });

        } catch (NumberFormatException e) {
            showError("Geçersiz sıcaklık değeri");
        }
    }

    private void saveHumidity() {
        String humidStr = etTargetHumid.getText().toString().trim();
        if (humidStr.isEmpty()) {
            showError("Lütfen hedef nem değeri girin");
            return;
        }

        try {
            humidStr = humidStr.replace(',', '.');
            float humidity = Float.parseFloat(humidStr);

            if (humidity < 0 || humidity > 100) {
                showError("Nem değeri 0-100% arasında olmalıdır");
                return;
            }

            showProgressDialog("Nem ayarlanıyor...");

            networkManager.setHumidity(humidity, new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    hideProgressDialog();

                    if (response.isSuccessful()) {
                        showSuccess("Hedef nem güncellendi");
                        hasChanges = true;
                        setResult(RESULT_OK);

                        // Ana ekrana dön
                        finish();
                    } else {
                        showError("Nem güncellenemedi");
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    hideProgressDialog();
                    showError("Bağlantı hatası: " + t.getMessage());
                }
            });

        } catch (NumberFormatException e) {
            showError("Geçersiz nem değeri");
        }
    }

    @Override
    public void onBackPressed() {
        if (hasChanges) {
            setResult(RESULT_OK);
        }
        super.onBackPressed();
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

                if (!finalInput.isEmpty() && !finalInput.equals(".") && !finalInput.equals("-")) {
                    try {
                        Double.parseDouble(finalInput);

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
                Log.e("DecimalInput", "TextWatcher hatası: " + e.getMessage());
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
            if (hasChanges) {
                setResult(RESULT_OK);
            }
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