package com.kulucka.mkv5.activities;

import android.app.ProgressDialog;
import android.os.Bundle;
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

public class MotorSettingsActivity extends AppCompatActivity {

    private TextView tvCurrentWaitTime, tvCurrentRunTime, tvMotorStatus;
    private EditText etWaitTime, etRunTime;
    private Button btnSaveSettings;

    private NetworkManager networkManager;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_motor_settings);

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
            getSupportActionBar().setTitle("Motor Ayarları");
        }
    }

    private void initializeViews() {
        tvCurrentWaitTime = findViewById(R.id.tvCurrentWaitTime);
        tvCurrentRunTime = findViewById(R.id.tvCurrentRunTime);
        tvMotorStatus = findViewById(R.id.tvMotorStatus);

        etWaitTime = findViewById(R.id.etWaitTime);
        etRunTime = findViewById(R.id.etRunTime);

        btnSaveSettings = findViewById(R.id.btnSaveSettings);
    }

    private void setupListeners() {
        btnSaveSettings.setOnClickListener(v -> saveSettings());
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
        // Current values
        tvCurrentWaitTime.setText(status.getMotorWaitTime() + " dakika");
        tvCurrentRunTime.setText(status.getMotorRunTime() + " saniye");

        // Motor status
        if (status.isMotorState()) {
            tvMotorStatus.setText("Motor şu anda AÇIK");
            tvMotorStatus.setTextColor(getColor(R.color.success));
        } else {
            tvMotorStatus.setText("Motor şu anda KAPALI");
            tvMotorStatus.setTextColor(getColor(R.color.inactive));
        }

        // Set input fields
        etWaitTime.setText(String.valueOf(status.getMotorWaitTime()));
        etRunTime.setText(String.valueOf(status.getMotorRunTime()));
    }

    private void saveSettings() {
        String waitTimeStr = etWaitTime.getText().toString().trim();
        String runTimeStr = etRunTime.getText().toString().trim();

        if (waitTimeStr.isEmpty() || runTimeStr.isEmpty()) {
            showError("Lütfen tüm alanları doldurun");
            return;
        }

        try {
            int waitTime = Integer.parseInt(waitTimeStr);
            int runTime = Integer.parseInt(runTimeStr);

            // Validate values
            if (waitTime < 1 || waitTime > 1440) { // 1 minute to 24 hours
                showError("Bekleme süresi 1-1440 dakika arasında olmalıdır");
                return;
            }

            if (runTime < 1 || runTime > 300) { // 1 second to 5 minutes
                showError("Çalışma süresi 1-300 saniye arasında olmalıdır");
                return;
            }

            showProgressDialog("Ayarlar kaydediliyor...");

            networkManager.setMotorSettings(waitTime, runTime, new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    hideProgressDialog();

                    if (response.isSuccessful()) {
                        showSuccess("Motor ayarları güncellendi");
                        loadCurrentValues();
                    } else {
                        showError("Ayarlar güncellenemedi");
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