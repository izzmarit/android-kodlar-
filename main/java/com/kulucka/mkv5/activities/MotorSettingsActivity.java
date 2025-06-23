package com.kulucka.mkv5.activities;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.kulucka.mkv5.R;
import com.kulucka.mkv5.models.DeviceStatus;
import com.kulucka.mkv5.network.ApiService;
import com.kulucka.mkv5.network.NetworkManager;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MotorSettingsActivity extends AppCompatActivity {
    private static final String TAG = "MotorSettingsActivity";
    private TextView tvCurrentWaitTime, tvCurrentRunTime, tvMotorStatus;
    private EditText etWaitTime, etRunTime;
    private Button btnSaveSettings, btnTestMotor;

    private NetworkManager networkManager;
    private ProgressDialog progressDialog;
    private boolean isMotorRunning = false;
    private Handler motorStatusHandler = new Handler();
    private Runnable motorStatusRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_motor_settings);

        setupToolbar();
        initializeViews();
        setupListeners();

        networkManager = NetworkManager.getInstance(this);

        loadCurrentValues();
    }

    private void startMotorStatusUpdates() {
        motorStatusRunnable = new Runnable() {
            @Override
            public void run() {
                loadMotorStatus();
                motorStatusHandler.postDelayed(this, 1000); // Her saniye güncelle
            }
        };
        motorStatusHandler.post(motorStatusRunnable);
    }

    private void stopMotorStatusUpdates() {
        if (motorStatusHandler != null && motorStatusRunnable != null) {
            motorStatusHandler.removeCallbacks(motorStatusRunnable);
        }
    }

    private void loadMotorStatus() {
        networkManager.getMotorStatus(new Callback<ApiService.MotorStatusResponse>() {
            @Override
            public void onResponse(Call<ApiService.MotorStatusResponse> call,
                                   Response<ApiService.MotorStatusResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateMotorStatusUI(response.body());
                }
            }

            @Override
            public void onFailure(Call<ApiService.MotorStatusResponse> call, Throwable t) {
                Log.e(TAG, "Motor status yükleme hatası: " + t.getMessage());
            }
        });
    }

    private void updateMotorStatusUI(ApiService.MotorStatusResponse status) {
        isMotorRunning = status.isRunning();

        if (isMotorRunning) {
            tvMotorStatus.setText("Motor şu anda AÇIK (" + status.getStatus() + ")");
            tvMotorStatus.setTextColor(getColor(R.color.success));
            btnTestMotor.setEnabled(false);
        } else {
            tvMotorStatus.setText("Motor şu anda KAPALI (" + status.getStatus() + ")");
            tvMotorStatus.setTextColor(getColor(R.color.inactive));
            btnTestMotor.setEnabled(status.isTestAvailable());
        }

        // Zamanlama bilgilerini güncelle - DÜZELTME
        tvCurrentWaitTime.setText(status.getWaitTime() + " dakika");
        tvCurrentRunTime.setText(status.getRunTime() + " saniye");

        // Input alanlarını güncelle
        if (!etWaitTime.isFocused()) {
            etWaitTime.setText(String.valueOf(status.getWaitTime()));
        }
        if (!etRunTime.isFocused()) {
            etRunTime.setText(String.valueOf(status.getRunTime()));
        }

        // Ek bilgiler
        if (findViewById(R.id.tvMotorStats) != null) {
            TextView tvMotorStats = findViewById(R.id.tvMotorStats);
            tvMotorStats.setText(String.format("Toplam Çalışma: %d kez", status.getTotalRuns()));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startMotorStatusUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopMotorStatusUpdates();
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
        btnTestMotor = findViewById(R.id.btnTestMotor);
    }

    private void setupListeners() {
        btnSaveSettings.setOnClickListener(v -> saveSettings());
        btnTestMotor.setOnClickListener(v -> showMotorTestDialog());
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
        // Motor bilgilerini al
        if (status.getMotor() != null) {
            DeviceStatus.MotorInfo motorInfo = status.getMotor();
            tvCurrentWaitTime.setText(motorInfo.getWaitTime() + " dakika");
            tvCurrentRunTime.setText(motorInfo.getRunTime() + " saniye");

            isMotorRunning = motorInfo.isState();
            btnTestMotor.setEnabled(motorInfo.isTestAvailable());

            etWaitTime.setText(String.valueOf(motorInfo.getWaitTime()));
            etRunTime.setText(String.valueOf(motorInfo.getRunTime()));
        } else {
            // Fallback - eski format
            tvCurrentWaitTime.setText(status.getMotorWaitTime() + " dakika");
            tvCurrentRunTime.setText(status.getMotorRunTime() + " saniye");

            isMotorRunning = status.isMotorState();
            btnTestMotor.setEnabled(!isMotorRunning);

            etWaitTime.setText(String.valueOf(status.getMotorWaitTime()));
            etRunTime.setText(String.valueOf(status.getMotorRunTime()));
        }

        if (isMotorRunning) {
            tvMotorStatus.setText("Motor şu anda AÇIK");
            tvMotorStatus.setTextColor(getColor(R.color.success));
        } else {
            tvMotorStatus.setText("Motor şu anda KAPALI");
            tvMotorStatus.setTextColor(getColor(R.color.inactive));
        }
    }

    private void showMotorTestDialog() {
        if (isMotorRunning) {
            showError("Motor zaten çalışıyor, test yapılamaz!");
            return;
        }

        // Özel test süresi dialog'u oluştur
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Motor Testi");

        // Özel layout inflate et
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_motor_test_duration, null);
        EditText etTestDuration = dialogView.findViewById(R.id.etTestDuration);

        // Mevcut çalışma süresini varsayılan olarak göster
        String currentRunTime = etRunTime.getText().toString().trim();
        if (!currentRunTime.isEmpty()) {
            etTestDuration.setText(currentRunTime);
        }

        builder.setView(dialogView);
        builder.setPositiveButton("Test Et", (dialog, which) -> {
            String durationStr = etTestDuration.getText().toString().trim();
            if (durationStr.isEmpty()) {
                testMotor(); // Varsayılan süre ile test et
            } else {
                try {
                    int duration = Integer.parseInt(durationStr);
                    if (duration < 1 || duration > 60) {
                        showError("Test süresi 1-60 saniye arasında olmalıdır");
                        return;
                    }
                    // Geçici olarak etRunTime'ı güncelle
                    String originalRunTime = etRunTime.getText().toString();
                    etRunTime.setText(String.valueOf(duration));
                    testMotor();
                    // Orijinal değeri geri yükle
                    etRunTime.setText(originalRunTime);
                } catch (NumberFormatException e) {
                    showError("Geçersiz süre");
                }
            }
        });
        builder.setNegativeButton("İptal", null);
        builder.show();
    }

    private void testMotor() {
        showProgressDialog("Motor test ediliyor...");

        // Mevcut çalışma süresini al
        String runTimeStr = etRunTime.getText().toString().trim();
        int testDuration = 0;

        try {
            if (!runTimeStr.isEmpty()) {
                testDuration = Integer.parseInt(runTimeStr);
            } else {
                // Varsayılan süre kullan
                testDuration = 14; // 14 saniye varsayılan
            }
        } catch (NumberFormatException e) {
            testDuration = 14;
        }

        // Süre kontrolü ekle
        if (testDuration < 1 || testDuration > 60) {
            hideProgressDialog();
            showError("Test süresi 1-60 saniye arasında olmalıdır");
            return;
        }

        // ESP32'nin beklediği formatta gönder
        networkManager.testMotor(testDuration, new Callback<ApiService.MotorTestResponse>() {
            @Override
            public void onResponse(Call<ApiService.MotorTestResponse> call,
                                   Response<ApiService.MotorTestResponse> response) {
                hideProgressDialog();

                if (response.isSuccessful() && response.body() != null) {
                    ApiService.MotorTestResponse testResponse = response.body();
                    showSuccess("Motor testi başlatıldı! Süre: " + testResponse.getDuration() + " saniye");

                    btnTestMotor.setEnabled(false);
                    btnTestMotor.setText("Motor Test Ediliyor...");
                    tvMotorStatus.setText("Motor TEST modunda");
                    tvMotorStatus.setTextColor(getColor(R.color.warning));

                    // Test süresinden sonra durumu güncelle
                    new android.os.Handler().postDelayed(() -> {
                        btnTestMotor.setEnabled(true);
                        btnTestMotor.setText("Motor Testi Yap");
                        loadCurrentValues();
                    }, (testResponse.getDuration() + 2) * 1000);
                } else {
                    showError("Motor testi başlatılamadı");
                }
            }

            @Override
            public void onFailure(Call<ApiService.MotorTestResponse> call, Throwable t) {
                hideProgressDialog();
                showError("Bağlantı hatası: " + t.getMessage());
            }
        });
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

            if (waitTime < 1 || waitTime > 1440) {
                showError("Bekleme süresi 1-1440 dakika arasında olmalıdır");
                return;
            }

            if (runTime < 1 || runTime > 300) {
                showError("Çalışma süresi 1-300 saniye arasında olmalıdır");
                return;
            }

            showProgressDialog("Ayarlar kaydediliyor...");

            networkManager.setMotorSettings(waitTime, runTime, new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        saveSystemState();
                    } else {
                        hideProgressDialog();
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

    private void saveSystemState() {
        networkManager.saveSystem(new Callback<ApiService.SystemSaveResponse>() {
            @Override
            public void onResponse(Call<ApiService.SystemSaveResponse> call,
                                   Response<ApiService.SystemSaveResponse> response) {
                hideProgressDialog();

                if (response.isSuccessful()) {
                    showSuccess("Motor ayarları güncellendi ve kaydedildi");
                    loadCurrentValues();
                } else {
                    showSuccess("Motor ayarları güncellendi");
                    loadCurrentValues();
                }
            }

            @Override
            public void onFailure(Call<ApiService.SystemSaveResponse> call, Throwable t) {
                hideProgressDialog();
                showSuccess("Motor ayarları güncellendi");
                loadCurrentValues();
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