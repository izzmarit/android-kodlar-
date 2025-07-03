package com.kulucka.mkv5.activities;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.os.Handler;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.kulucka.mkv5.R;
import com.kulucka.mkv5.models.DeviceStatus;
import com.kulucka.mkv5.network.NetworkManager;
import com.kulucka.mkv5.utils.Constants;
import com.kulucka.mkv5.network.ApiService;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.Map;
import java.util.HashMap;

public class PidSettingsActivity extends AppCompatActivity {

    private static final String TAG = "PidSettingsActivity";
    private RadioGroup radioGroupPidMode;
    private RadioButton radioPidOff, radioPidManual, radioPidAuto;
    private CardView cardPidParameters, cardPidStatus;
    private EditText etKp, etKi, etKd;
    private Button btnSaveParameters, btnStartPid, btnStopPid;
    private TextView tvPidStatus, tvPidError, tvPidOutput, tvAutoTuneStatus;

    private NetworkManager networkManager;
    private ProgressDialog progressDialog;
    private DeviceStatus currentStatus;
    private Handler statusUpdateHandler = new Handler();
    private Runnable statusUpdateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pid_settings);

        setupToolbar();
        initializeViews();
        setupListeners();

        networkManager = NetworkManager.getInstance(this);

        // Load current values
        loadCurrentValues();
    }

    private void startPidStatusUpdates() {
        statusUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                loadPidStatus();
                statusUpdateHandler.postDelayed(this, 2000); // Her 2 saniyede bir güncelle
            }
        };
        statusUpdateHandler.post(statusUpdateRunnable);
    }

    private void stopPidStatusUpdates() {
        if (statusUpdateHandler != null && statusUpdateRunnable != null) {
            statusUpdateHandler.removeCallbacks(statusUpdateRunnable);
        }
    }

    private void loadPidStatus() {
        networkManager.getPidStatus(new Callback<ApiService.PidStatusResponse>() {
            @Override
            public void onResponse(Call<ApiService.PidStatusResponse> call,
                                   Response<ApiService.PidStatusResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updatePidStatusUI(response.body());
                }
            }

            @Override
            public void onFailure(Call<ApiService.PidStatusResponse> call, Throwable t) {
                Log.e(TAG, "PID status yükleme hatası: " + t.getMessage());
            }
        });
    }

    private void updatePidStatusUI(ApiService.PidStatusResponse status) {
        // Detaylı PID durumu gösterimi
        String modeText = "Durum: " + status.getModeString();
        tvPidStatus.setText(modeText);

        if (status.isActive()) {
            cardPidStatus.setVisibility(View.VISIBLE);
            tvPidError.setText(String.format("Hata: %.2f°C", status.getError()));
            tvPidOutput.setText(String.format("Çıkış: %.1f%%", status.getOutput()));

            // Parametreleri güncelle
            etKp.setText(String.format("%.2f", status.getKp()).replace('.', ','));
            etKi.setText(String.format("%.2f", status.getKi()).replace('.', ','));
            etKd.setText(String.format("%.2f", status.getKd()).replace('.', ','));

            if (status.isAutoTuneActive()) {
                tvAutoTuneStatus.setVisibility(View.VISIBLE);
                tvAutoTuneStatus.setText("Otomatik Ayarlama Aktif");
            } else {
                tvAutoTuneStatus.setVisibility(View.GONE);
            }
        } else {
            cardPidStatus.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startPidStatusUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPidStatusUpdates();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("PID Ayarları");
        }
    }

    private void initializeViews() {
        radioGroupPidMode = findViewById(R.id.radioGroupPidMode);
        radioPidOff = findViewById(R.id.radioPidOff);
        radioPidManual = findViewById(R.id.radioPidManual);
        radioPidAuto = findViewById(R.id.radioPidAuto);

        cardPidParameters = findViewById(R.id.cardPidParameters);
        cardPidStatus = findViewById(R.id.cardPidStatus);

        etKp = findViewById(R.id.etKp);
        etKi = findViewById(R.id.etKi);
        etKd = findViewById(R.id.etKd);

        btnSaveParameters = findViewById(R.id.btnSaveParameters);
        btnStartPid = findViewById(R.id.btnStartPid);
        btnStopPid = findViewById(R.id.btnStopPid);

        tvPidStatus = findViewById(R.id.tvPidStatus);
        tvPidError = findViewById(R.id.tvPidError);
        tvPidOutput = findViewById(R.id.tvPidOutput);
        tvAutoTuneStatus = findViewById(R.id.tvAutoTuneStatus);
    }

    private void setupListeners() {
        radioGroupPidMode.setOnCheckedChangeListener((group, checkedId) -> {
            updateUIForMode();
        });

        btnSaveParameters.setOnClickListener(v -> saveParameters());
        btnStartPid.setOnClickListener(v -> startPid());
        btnStopPid.setOnClickListener(v -> stopPid());
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

        // Set PID mode
        switch (currentStatus.getPidMode()) {
            case Constants.PID_MODE_OFF:
                radioPidOff.setChecked(true);
                break;
            case Constants.PID_MODE_MANUAL:
                radioPidManual.setChecked(true);
                break;
            case Constants.PID_MODE_AUTO:
                radioPidAuto.setChecked(true);
                break;
        }

        // ESP32'den gelen güncel PID parametrelerini göster
        etKp.setText(String.format("%.2f", currentStatus.getPidKp()).replace('.', ','));
        etKi.setText(String.format("%.2f", currentStatus.getPidKi()).replace('.', ','));
        etKd.setText(String.format("%.2f", currentStatus.getPidKd()).replace('.', ','));

        // Update status
        updatePidStatus();
        updateUIForMode();
    }

    private void updatePidStatus() {
        if (currentStatus == null) return;

        String statusText = "Durum: " + currentStatus.getPidModeString();
        tvPidStatus.setText(statusText);

        if (currentStatus.getPidMode() != Constants.PID_MODE_OFF) {
            cardPidStatus.setVisibility(View.VISIBLE);
            tvPidError.setText(String.format("Hata: %.2f°C", currentStatus.getPidError()));
            tvPidOutput.setText(String.format("Çıkış: %.1f%%", currentStatus.getPidOutput()));

            if (currentStatus.isPidAutoTuneActive()) {
                tvAutoTuneStatus.setVisibility(View.VISIBLE);
                tvAutoTuneStatus.setText("Otomatik Ayarlama Aktif");
            } else {
                tvAutoTuneStatus.setVisibility(View.GONE);
            }
        } else {
            cardPidStatus.setVisibility(View.GONE);
        }
    }

    private void updateUIForMode() {
        int checkedId = radioGroupPidMode.getCheckedRadioButtonId();

        if (checkedId == R.id.radioPidOff) {
            cardPidParameters.setVisibility(View.GONE);
            btnStartPid.setEnabled(false);
            btnStopPid.setEnabled(false);
        } else {
            cardPidParameters.setVisibility(View.VISIBLE);
            btnStartPid.setEnabled(true);
            btnStopPid.setEnabled(true);
        }
    }

    private void saveParameters() {
        try {
            String kpStr = etKp.getText().toString().replace(',', '.');
            String kiStr = etKi.getText().toString().replace(',', '.');
            String kdStr = etKd.getText().toString().replace(',', '.');

            float kp = Float.parseFloat(kpStr);
            float ki = Float.parseFloat(kiStr);
            float kd = Float.parseFloat(kdStr);

            if (kp < Constants.PID_KP_MIN || kp > Constants.PID_KP_MAX) {
                showError("Kp değeri " + Constants.PID_KP_MIN + "-" + Constants.PID_KP_MAX + " arasında olmalıdır");
                return;
            }
            if (ki < Constants.PID_KI_MIN || ki > Constants.PID_KI_MAX) {
                showError("Ki değeri " + Constants.PID_KI_MIN + "-" + Constants.PID_KI_MAX + " arasında olmalıdır");
                return;
            }
            if (kd < Constants.PID_KD_MIN || kd > Constants.PID_KD_MAX) {
                showError("Kd değeri " + Constants.PID_KD_MIN + "-" + Constants.PID_KD_MAX + " arasında olmalıdır");
                return;
            }

            showProgressDialog("Parametreler kaydediliyor...");

            // NetworkManager üzerinden direkt olarak çağır
            networkManager.setPidParameters(kp, ki, kd, new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    hideProgressDialog();
                    if (response.isSuccessful()) {
                        showSuccess("PID parametreleri güncellendi");
                        saveSystemState();
                    } else {
                        showError("Parametreler güncellenemedi");
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    hideProgressDialog();
                    showError("Bağlantı hatası: " + t.getMessage());
                }
            });

        } catch (NumberFormatException e) {
            showError("Geçersiz parametre değeri");
        }
    }

    private void saveSystemState() {
        networkManager.saveSystem(new Callback<ApiService.SystemSaveResponse>() {
            @Override
            public void onResponse(Call<ApiService.SystemSaveResponse> call,
                                   Response<ApiService.SystemSaveResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Sistem durumu kaydedildi");
                }
                loadCurrentValues();
            }

            @Override
            public void onFailure(Call<ApiService.SystemSaveResponse> call, Throwable t) {
                Log.e(TAG, "Sistem kaydetme hatası: " + t.getMessage());
                loadCurrentValues();
            }
        });
    }

    private void startPid() {
        int mode = getSelectedMode();

        if (mode == Constants.PID_MODE_OFF) {
            showError("Lütfen bir PID modu seçin");
            return;
        }

        String modeText = mode == Constants.PID_MODE_MANUAL ? "Manuel" : "Otomatik";

        new AlertDialog.Builder(this)
                .setTitle("PID Başlat")
                .setMessage(modeText + " PID kontrolü başlatılsın mı?")
                .setPositiveButton("Evet", (dialog, which) -> {
                    showProgressDialog("PID başlatılıyor...");

                    networkManager.setPidMode(mode, new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            hideProgressDialog();

                            if (response.isSuccessful()) {
                                showSuccess(modeText + " PID başlatıldı");
                                loadCurrentValues();
                            } else {
                                showError("PID başlatılamadı");
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            hideProgressDialog();
                            showError("Bağlantı hatası: " + t.getMessage());
                        }
                    });
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    private void stopPid() {
        new AlertDialog.Builder(this)
                .setTitle("PID Durdur")
                .setMessage("PID kontrolü durdurulsun mu?")
                .setPositiveButton("Evet", (dialog, which) -> {
                    showProgressDialog("PID durduruluyor...");

                    networkManager.setPidMode(Constants.PID_MODE_OFF, new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            hideProgressDialog();

                            if (response.isSuccessful()) {
                                showSuccess("PID durduruldu");
                                radioPidOff.setChecked(true);
                                loadCurrentValues();
                            } else {
                                showError("PID durdurulamadı");
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            hideProgressDialog();
                            showError("Bağlantı hatası: " + t.getMessage());
                        }
                    });
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    private int getSelectedMode() {
        int checkedId = radioGroupPidMode.getCheckedRadioButtonId();
        if (checkedId == R.id.radioPidManual) return Constants.PID_MODE_MANUAL;
        if (checkedId == R.id.radioPidAuto) return Constants.PID_MODE_AUTO;
        return Constants.PID_MODE_OFF;
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