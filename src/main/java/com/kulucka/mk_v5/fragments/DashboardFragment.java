package com.kulucka.mk_v5.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kulucka.mk_v5.R;
import com.kulucka.mk_v5.activities.MainActivity;
import com.kulucka.mk_v5.services.ApiService;

import org.json.JSONObject;

public class DashboardFragment extends Fragment {

    private static final String TAG = "DashboardFragment";

    // UI Elements
    private TextView tvCurrentTemp;
    private TextView tvTargetTemp;
    private TextView tvCurrentHumidity;
    private TextView tvTargetHumidity;
    private TextView tvCurrentDay;
    private TextView tvTotalDays;
    private TextView tvPidStatus;
    private TextView tvTurningStatus;
    private TextView tvProgramStatus;
    private ProgressBar progressTemperature;
    private ProgressBar progressHumidity;
    private ProgressBar progressDays;
    private Button btnStartProgram;
    private Button btnStopProgram;
    private Button btnRefresh;

    // Data refresh
    private Handler refreshHandler;
    private Runnable refreshRunnable;
    private static final int REFRESH_INTERVAL = 5000; // 5 seconds

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        initializeViews(view);
        setupClickListeners();
        setupDataRefresh();
        loadInitialData();

        return view;
    }

    private void initializeViews(View view) {
        // Temperature views
        tvCurrentTemp = view.findViewById(R.id.tv_current_temp);
        tvTargetTemp = view.findViewById(R.id.tv_target_temp);
        progressTemperature = view.findViewById(R.id.progress_temperature);

        // Humidity views
        tvCurrentHumidity = view.findViewById(R.id.tv_current_humidity);
        tvTargetHumidity = view.findViewById(R.id.tv_target_humidity);
        progressHumidity = view.findViewById(R.id.progress_humidity);

        // Program views
        tvCurrentDay = view.findViewById(R.id.tv_current_day);
        tvTotalDays = view.findViewById(R.id.tv_total_days);
        progressDays = view.findViewById(R.id.progress_days);

        // Status views
        tvPidStatus = view.findViewById(R.id.tv_pid_status);
        tvTurningStatus = view.findViewById(R.id.tv_turning_status);
        tvProgramStatus = view.findViewById(R.id.tv_program_status);

        // Buttons
        btnStartProgram = view.findViewById(R.id.btn_start_program);
        btnStopProgram = view.findViewById(R.id.btn_stop_program);
        btnRefresh = view.findViewById(R.id.btn_refresh);
    }

    private void setupClickListeners() {
        if (btnStartProgram != null) {
            btnStartProgram.setOnClickListener(v -> startIncubationProgram());
        }

        if (btnStopProgram != null) {
            btnStopProgram.setOnClickListener(v -> stopIncubationProgram());
        }

        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> refreshData());
        }
    }

    private void setupDataRefresh() {
        refreshHandler = new Handler();
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                refreshData();
                refreshHandler.postDelayed(this, REFRESH_INTERVAL);
            }
        };
    }

    public void updateData(JSONObject data) {
        try {
            // Sensör verilerini güncelle
            double temperature = data.optDouble("temperature", 0.0);
            double humidity = data.optDouble("humidity", 0.0);
            double targetTemp = data.optDouble("targetTemp", 0.0);
            double targetHumidity = data.optDouble("targetHumidity", 0.0);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    // UI elementlerini güncelle
                    updateTemperatureDisplay(temperature, targetTemp);
                    updateHumidityDisplay(humidity, targetHumidity);
                    updateStatusDisplays(data);
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Data update error: " + e.getMessage());
        }
    }

    private void updateTemperatureDisplay(double current, double target) {
        // Sıcaklık göstergelerini güncelle
        if (tvCurrentTemp != null) {
            tvCurrentTemp.setText(String.format("%.1f°C", current));
        }
        if (tvTargetTemp != null) {
            tvTargetTemp.setText(String.format("Hedef: %.1f°C", target));
        }
        if (progressTemperature != null && target > 0) {
            int progress = (int) ((current / target) * 100);
            progressTemperature.setProgress(Math.min(100, Math.max(0, progress)));
        }
    }

    private void updateHumidityDisplay(double current, double target) {
        // Nem göstergelerini güncelle
        if (tvCurrentHumidity != null) {
            tvCurrentHumidity.setText(String.format("%.1f%%", current));
        }
        if (tvTargetHumidity != null) {
            tvTargetHumidity.setText(String.format("Hedef: %.1f%%", target));
        }
        if (progressHumidity != null && target > 0) {
            int progress = (int) ((current / target) * 100);
            progressHumidity.setProgress(Math.min(100, Math.max(0, progress)));
        }
    }

    private void updateStatusDisplays(JSONObject data) {
        try {
            // PID durumu
            boolean pidEnabled = data.optBoolean("pidEnabled", false);
            if (tvPidStatus != null) {
                tvPidStatus.setText(pidEnabled ? "PID: Aktif" : "PID: Pasif");
                tvPidStatus.setTextColor(getResources().getColor(
                        pidEnabled ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
            }

            // Çevirme durumu
            boolean turningEnabled = data.optBoolean("turningEnabled", false);
            if (tvTurningStatus != null) {
                tvTurningStatus.setText(turningEnabled ? "Çevirme: Aktif" : "Çevirme: Pasif");
                tvTurningStatus.setTextColor(getResources().getColor(
                        turningEnabled ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
            }

            // Program durumu
            boolean programActive = data.optBoolean("programActive", false);
            int currentDay = data.optInt("currentDay", 0);
            int totalDays = data.optInt("totalDays", 0);

            if (tvProgramStatus != null) {
                tvProgramStatus.setText(programActive ? "Program: Çalışıyor" : "Program: Durduruldu");
                tvProgramStatus.setTextColor(getResources().getColor(
                        programActive ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
            }

            if (tvCurrentDay != null) {
                tvCurrentDay.setText("Gün: " + currentDay);
            }

            if (tvTotalDays != null) {
                tvTotalDays.setText("Toplam: " + totalDays + " gün");
            }

            if (progressDays != null && totalDays > 0) {
                int progress = (int) ((currentDay / (double) totalDays) * 100);
                progressDays.setProgress(Math.min(100, Math.max(0, progress)));
            }

            // Buton durumları
            if (btnStartProgram != null && btnStopProgram != null) {
                btnStartProgram.setEnabled(!programActive);
                btnStopProgram.setEnabled(programActive);
            }

        } catch (Exception e) {
            Log.e(TAG, "Status update error: " + e.getMessage());
        }
    }

    private void loadInitialData() {
        refreshData();
    }

    private void refreshData() {
        ApiService.getInstance().getSensorData(new ApiService.DataCallback() {
            @Override
            public void onSuccess(JSONObject data) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> updateData(data));
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Log.w(TAG, "Failed to refresh data: " + message);
                        showToast("Veri güncellenemedi: " + message);
                    });
                }
            }
        });

        // Status bilgilerini de al
        ApiService.getInstance().getStatus(new ApiService.DataCallback() {
            @Override
            public void onSuccess(JSONObject data) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> updateStatusDisplays(data));
                }
            }

            @Override
            public void onError(String message) {
                Log.w(TAG, "Failed to get status: " + message);
            }
        });
    }

    private void startIncubationProgram() {
        // Default 21 gün için program başlat
        ApiService.getInstance().startIncubation(21, new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Kuluçka programı başlatıldı");
                        refreshData();
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Program başlatılamadı: " + message);
                    });
                }
            }
        });
    }

    private void stopIncubationProgram() {
        ApiService.getInstance().stopIncubation(new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Kuluçka programı durduruldu");
                        refreshData();
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Program durdurulamadı: " + message);
                    });
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.post(refreshRunnable);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}