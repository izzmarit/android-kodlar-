package com.kulucka.mkv5.activities;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.kulucka.mkv5.R;
import com.kulucka.mkv5.models.DeviceStatus;
import com.kulucka.mkv5.network.NetworkManager;
import com.kulucka.mkv5.utils.Constants;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IncubationSettingsActivity extends AppCompatActivity {

    private RadioGroup radioGroupType;
    private RadioButton radioChicken, radioQuail, radioGoose, radioManual;
    private LinearLayout layoutManualSettings;
    private EditText etDevTemp, etHatchTemp, etDevHumid, etHatchHumid, etDevDays, etHatchDays;
    private TextView tvCurrentStatus;
    private Button btnStartIncubation, btnStopIncubation;
    private CardView cardCurrentStatus;

    private NetworkManager networkManager;
    private ProgressDialog progressDialog;
    private boolean isIncubationRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incubation_settings);

        setupToolbar();
        initializeViews();
        setupListeners();

        networkManager = NetworkManager.getInstance(this);

        // Load current status
        loadCurrentStatus();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Kuluçka Ayarları");
        }
    }

    private void initializeViews() {
        radioGroupType = findViewById(R.id.radioGroupType);
        radioChicken = findViewById(R.id.radioChicken);
        radioQuail = findViewById(R.id.radioQuail);
        radioGoose = findViewById(R.id.radioGoose);
        radioManual = findViewById(R.id.radioManual);

        layoutManualSettings = findViewById(R.id.layoutManualSettings);
        etDevTemp = findViewById(R.id.etDevTemp);
        etHatchTemp = findViewById(R.id.etHatchTemp);
        etDevHumid = findViewById(R.id.etDevHumid);
        etHatchHumid = findViewById(R.id.etHatchHumid);
        etDevDays = findViewById(R.id.etDevDays);
        etHatchDays = findViewById(R.id.etHatchDays);

        tvCurrentStatus = findViewById(R.id.tvCurrentStatus);
        btnStartIncubation = findViewById(R.id.btnStartIncubation);
        btnStopIncubation = findViewById(R.id.btnStopIncubation);
        cardCurrentStatus = findViewById(R.id.cardCurrentStatus);

        // Initially hide manual settings
        layoutManualSettings.setVisibility(View.GONE);
    }

    private void setupListeners() {
        radioGroupType.setOnCheckedChangeListener((group, checkedId) -> {
            layoutManualSettings.setVisibility(
                    checkedId == R.id.radioManual ? View.VISIBLE : View.GONE
            );
        });

        btnStartIncubation.setOnClickListener(v -> startIncubation());
        btnStopIncubation.setOnClickListener(v -> stopIncubation());
    }

    private void loadCurrentStatus() {
        showProgressDialog("Durum yükleniyor...");

        networkManager.getDeviceStatus(new Callback<DeviceStatus>() {
            @Override
            public void onResponse(Call<DeviceStatus> call, Response<DeviceStatus> response) {
                hideProgressDialog();

                if (response.isSuccessful() && response.body() != null) {
                    updateUI(response.body());
                } else {
                    showError("Durum bilgisi alınamadı");
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
        isIncubationRunning = status.isIncubationRunning();

        if (isIncubationRunning) {
            cardCurrentStatus.setVisibility(View.VISIBLE);
            String statusText = String.format(
                    "Tip: %s\nGün: %d/%d\nHedef Sıcaklık: %.1f°C\nHedef Nem: %d%%",
                    getIncubationTypeName(status.getIncubationType()),
                    status.getDisplayDay(),
                    status.getTotalDays(),
                    status.getTargetTemp(),
                    Math.round(status.getTargetHumid())
            );

            if (status.isIncubationCompleted()) {
                statusText += "\n\nKuluçka süresi tamamlandı!\nGerçek Gün: " + status.getActualDay();
            }

            tvCurrentStatus.setText(statusText);
            btnStartIncubation.setEnabled(false);
            btnStopIncubation.setEnabled(true);
        } else {
            cardCurrentStatus.setVisibility(View.GONE);
            btnStartIncubation.setEnabled(true);
            btnStopIncubation.setEnabled(false);
        }

        // Set manual settings values
        etDevTemp.setText(String.valueOf(status.getManualDevTemp()));
        etHatchTemp.setText(String.valueOf(status.getManualHatchTemp()));
        etDevHumid.setText(String.valueOf(status.getManualDevHumid()));
        etHatchHumid.setText(String.valueOf(status.getManualHatchHumid()));
        etDevDays.setText(String.valueOf(status.getManualDevDays()));
        etHatchDays.setText(String.valueOf(status.getManualHatchDays()));
    }

    private void startIncubation() {
        int selectedType = getSelectedType();

        if (selectedType == Constants.TYPE_MANUAL) {
            // Validate manual settings
            if (!validateManualSettings()) {
                return;
            }

            // Update manual settings first
            updateManualSettings(() -> {
                // Then start incubation
                doStartIncubation(selectedType);
            });
        } else {
            doStartIncubation(selectedType);
        }
    }

    private void doStartIncubation(int type) {
        showProgressDialog("Kuluçka başlatılıyor...");

        // First set incubation type
        networkManager.setIncubationType(type, new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    // Then start incubation
                    networkManager.startIncubation(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            hideProgressDialog();

                            if (response.isSuccessful()) {
                                showSuccess("Kuluçka başlatıldı!");
                                loadCurrentStatus();
                            } else {
                                showError("Kuluçka başlatılamadı");
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            hideProgressDialog();
                            showError("Bağlantı hatası: " + t.getMessage());
                        }
                    });
                } else {
                    hideProgressDialog();
                    showError("Kuluçka tipi ayarlanamadı");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                hideProgressDialog();
                showError("Bağlantı hatası: " + t.getMessage());
            }
        });
    }

    private void stopIncubation() {
        new AlertDialog.Builder(this)
                .setTitle("Kuluçkayı Durdur")
                .setMessage("Kuluçkayı durdurmak istediğinizden emin misiniz?")
                .setPositiveButton("Evet", (dialog, which) -> {
                    showProgressDialog("Kuluçka durduruluyor...");

                    networkManager.stopIncubation(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            hideProgressDialog();

                            if (response.isSuccessful()) {
                                showSuccess("Kuluçka durduruldu");
                                loadCurrentStatus();
                            } else {
                                showError("Kuluçka durdurulamadı");
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

    private void updateManualSettings(Runnable onSuccess) {
        float devTemp = Float.parseFloat(etDevTemp.getText().toString());
        float hatchTemp = Float.parseFloat(etHatchTemp.getText().toString());
        int devHumid = Integer.parseInt(etDevHumid.getText().toString());
        int hatchHumid = Integer.parseInt(etHatchHumid.getText().toString());
        int devDays = Integer.parseInt(etDevDays.getText().toString());
        int hatchDays = Integer.parseInt(etHatchDays.getText().toString());

        // Yeni toplu güncelleme metodunu kullan
        networkManager.setManualIncubationParametersAdvanced(
                devTemp, hatchTemp, devHumid, hatchHumid, devDays, hatchDays,
                new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            onSuccess.run();
                        } else {
                            hideProgressDialog();
                            showError("Manuel ayarlar güncellenemedi");
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        hideProgressDialog();
                        showError("Bağlantı hatası: " + t.getMessage());
                    }
                }
        );
    }

    private int getSelectedType() {
        int checkedId = radioGroupType.getCheckedRadioButtonId();
        if (checkedId == R.id.radioChicken) return Constants.TYPE_CHICKEN;
        if (checkedId == R.id.radioQuail) return Constants.TYPE_QUAIL;
        if (checkedId == R.id.radioGoose) return Constants.TYPE_GOOSE;
        if (checkedId == R.id.radioManual) return Constants.TYPE_MANUAL;
        return Constants.TYPE_CHICKEN;
    }

    private String getIncubationTypeName(String type) {
        if (type == null) return "Bilinmeyen";

        switch (type) {
            case "0":
            case "Tavuk":
                return "Tavuk";
            case "1":
            case "Bıldırcın":
                return "Bıldırcın";
            case "2":
            case "Kaz":
                return "Kaz";
            case "3":
            case "Manuel":
                return "Manuel";
            default:
                return type;
        }
    }

    private boolean validateManualSettings() {
        try {
            float devTemp = Float.parseFloat(etDevTemp.getText().toString());
            float hatchTemp = Float.parseFloat(etHatchTemp.getText().toString());
            int devHumid = Integer.parseInt(etDevHumid.getText().toString());
            int hatchHumid = Integer.parseInt(etHatchHumid.getText().toString());
            int devDays = Integer.parseInt(etDevDays.getText().toString());
            int hatchDays = Integer.parseInt(etHatchDays.getText().toString());

            if (devTemp < 30 || devTemp > 40 || hatchTemp < 30 || hatchTemp > 40) {
                showError("Sıcaklık değerleri 30-40°C arasında olmalıdır");
                return false;
            }

            if (devHumid < 0 || devHumid > 100 || hatchHumid < 0 || hatchHumid > 100) {
                showError("Nem değerleri 0-100% arasında olmalıdır");
                return false;
            }

            if (devDays < 1 || hatchDays < 1) {
                showError("Gün değerleri en az 1 olmalıdır");
                return false;
            }

            return true;
        } catch (NumberFormatException e) {
            showError("Lütfen tüm alanları doldurun");
            return false;
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