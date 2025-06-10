package com.kulucka.mkv5;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.kulucka.mkv5.activities.SettingsActivity;
import com.kulucka.mkv5.models.DeviceStatus;
import com.kulucka.mkv5.network.NetworkManager;
import com.kulucka.mkv5.services.BackgroundService;
import com.kulucka.mkv5.utils.Constants;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    // UI Components
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvConnectionStatus;
    private ImageView ivConnectionStatus;
    private TextView tvCurrentTemp, tvTargetTemp, tvHeaterStatus;
    private TextView tvCurrentHumid, tvTargetHumid, tvHumidifierStatus;
    private TextView tvIncubationType, tvDayCount, tvIncubationStatus, tvCompletionStatus;
    private TextView tvMotorStatus, tvMotorTiming;
    private TextView tvPidMode, tvPidValues;
    private TextView tvAlarmStatus;
    private FloatingActionButton fabSettings;

    // Variables
    private NetworkManager networkManager;
    private Handler updateHandler;
    private Runnable updateRunnable;
    private boolean isUpdating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupListeners();

        networkManager = NetworkManager.getInstance(this);
        updateHandler = new Handler();

        // Start background service
        startBackgroundService();

        // Start periodic updates
        startPeriodicUpdates();
    }

    private void initializeViews() {
        swipeRefresh = findViewById(R.id.swipeRefresh);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        ivConnectionStatus = findViewById(R.id.ivConnectionStatus);

        // Temperature views
        tvCurrentTemp = findViewById(R.id.tvCurrentTemp);
        tvTargetTemp = findViewById(R.id.tvTargetTemp);
        tvHeaterStatus = findViewById(R.id.tvHeaterStatus);

        // Humidity views
        tvCurrentHumid = findViewById(R.id.tvCurrentHumid);
        tvTargetHumid = findViewById(R.id.tvTargetHumid);
        tvHumidifierStatus = findViewById(R.id.tvHumidifierStatus);

        // Incubation views
        tvIncubationType = findViewById(R.id.tvIncubationType);
        tvDayCount = findViewById(R.id.tvDayCount);
        tvIncubationStatus = findViewById(R.id.tvIncubationStatus);
        tvCompletionStatus = findViewById(R.id.tvCompletionStatus);

        // Motor views
        tvMotorStatus = findViewById(R.id.tvMotorStatus);
        tvMotorTiming = findViewById(R.id.tvMotorTiming);

        // PID views
        tvPidMode = findViewById(R.id.tvPidMode);
        tvPidValues = findViewById(R.id.tvPidValues);

        // Alarm view
        tvAlarmStatus = findViewById(R.id.tvAlarmStatus);

        fabSettings = findViewById(R.id.fabSettings);

        // Set initial connection status
        updateConnectionStatus(false);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::refreshData);

        fabSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    private void startBackgroundService() {
        Intent serviceIntent = new Intent(this, BackgroundService.class);
        startService(serviceIntent);
    }

    private void startPeriodicUpdates() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isUpdating) {
                    refreshData();
                }
                updateHandler.postDelayed(this, Constants.STATUS_UPDATE_INTERVAL);
            }
        };
        updateHandler.post(updateRunnable);
    }

    private void refreshData() {
        if (isUpdating) {
            return;
        }

        isUpdating = true;

        networkManager.getDeviceStatus(new Callback<DeviceStatus>() {
            @Override
            public void onResponse(Call<DeviceStatus> call, Response<DeviceStatus> response) {
                runOnUiThread(() -> {
                    isUpdating = false;
                    swipeRefresh.setRefreshing(false);

                    if (response.isSuccessful() && response.body() != null) {
                        updateUI(response.body());
                        updateConnectionStatus(true);
                    } else {
                        updateConnectionStatus(false);
                        showError("Veri alınamadı");
                    }
                });
            }

            @Override
            public void onFailure(Call<DeviceStatus> call, Throwable t) {
                runOnUiThread(() -> {
                    isUpdating = false;
                    swipeRefresh.setRefreshing(false);
                    updateConnectionStatus(false);
                    Log.e(TAG, "Network error: " + t.getMessage());
                });
            }
        });
    }

    private void updateUI(DeviceStatus status) {
        // Temperature
        tvCurrentTemp.setText(String.format("%.1f°C", status.getTemperature()));
        tvTargetTemp.setText(String.format("Hedef: %.1f°C", status.getTargetTemp()));
        updateDeviceState(tvHeaterStatus, status.isHeaterState());

        // Humidity
        tvCurrentHumid.setText(String.format("%d%%", Math.round(status.getHumidity())));
        tvTargetHumid.setText(String.format("Hedef: %d%%", Math.round(status.getTargetHumid())));
        updateDeviceState(tvHumidifierStatus, status.isHumidifierState());

        // Incubation
        tvIncubationType.setText(getIncubationTypeName(status.getIncubationType()));
        tvDayCount.setText(String.format("%d/%d", status.getDisplayDay(), status.getTotalDays()));
        updateIncubationStatus(status);

        // Motor
        updateDeviceState(tvMotorStatus, status.isMotorState());
        tvMotorTiming.setText(String.format("Bekleme: %d dk\nÇalışma: %d sn",
                status.getMotorWaitTime(), status.getMotorRunTime()));

        // PID
        updatePidStatus(status);

        // Alarm
        updateAlarmStatus(status.isAlarmEnabled());
    }

    private void updateConnectionStatus(boolean connected) {
        if (connected) {
            tvConnectionStatus.setText("Bağlı");
            tvConnectionStatus.setTextColor(getColor(R.color.success));
            ivConnectionStatus.setImageResource(R.drawable.ic_wifi_on);
            ivConnectionStatus.setColorFilter(getColor(R.color.success));
        } else {
            tvConnectionStatus.setText("Bağlantı Yok");
            tvConnectionStatus.setTextColor(getColor(R.color.error));
            ivConnectionStatus.setImageResource(R.drawable.ic_wifi_off);
            ivConnectionStatus.setColorFilter(getColor(R.color.error));
        }
    }

    private void updateDeviceState(TextView textView, boolean isOn) {
        if (isOn) {
            textView.setText("AÇIK");
            textView.setTextColor(getColor(R.color.active));
        } else {
            textView.setText("KAPALI");
            textView.setTextColor(getColor(R.color.inactive));
        }
    }

    private void updateIncubationStatus(DeviceStatus status) {
        if (status.isIncubationRunning()) {
            tvIncubationStatus.setText("Çalışıyor");
            tvIncubationStatus.setTextColor(getColor(R.color.success));

            if (status.isIncubationCompleted()) {
                tvCompletionStatus.setVisibility(View.VISIBLE);
                tvCompletionStatus.setText(String.format("Kuluçka süresi tamamlandı! (Gerçek Gün: %d)",
                        status.getActualDay()));
            } else {
                tvCompletionStatus.setVisibility(View.GONE);
            }
        } else {
            tvIncubationStatus.setText("Durduruldu");
            tvIncubationStatus.setTextColor(getColor(R.color.error));
            tvCompletionStatus.setVisibility(View.GONE);
        }
    }

    private void updatePidStatus(DeviceStatus status) {
        String pidModeText;
        int pidColor;

        switch (status.getPidMode()) {
            case Constants.PID_MODE_MANUAL:
                pidModeText = "Manuel";
                pidColor = R.color.warning;
                break;
            case Constants.PID_MODE_AUTO:
                pidModeText = "Otomatik";
                pidColor = R.color.success;
                break;
            default:
                pidModeText = "Kapalı";
                pidColor = R.color.inactive;
                break;
        }

        tvPidMode.setText(pidModeText);
        tvPidMode.setTextColor(getColor(pidColor));

        tvPidValues.setText(String.format("Kp: %.2f\nKi: %.2f\nKd: %.2f",
                status.getPidKp(), status.getPidKi(), status.getPidKd()));
    }

    private void updateAlarmStatus(boolean enabled) {
        if (enabled) {
            tvAlarmStatus.setText("Açık");
            tvAlarmStatus.setTextColor(getColor(R.color.success));
        } else {
            tvAlarmStatus.setText("Kapalı");
            tvAlarmStatus.setTextColor(getColor(R.color.error));
        }
    }

    private String getIncubationTypeName(String type) {
        if (type == null) return "--";

        switch (type.toLowerCase()) {
            case "0":
            case "tavuk":
                return "Tavuk";
            case "1":
            case "bıldırcın":
                return "Bıldırcın";
            case "2":
            case "kaz":
                return "Kaz";
            case "3":
            case "manuel":
                return "Manuel";
            default:
                return type;
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (updateHandler != null && updateRunnable != null) {
            updateHandler.removeCallbacks(updateRunnable);
        }
    }
}