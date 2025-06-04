package com.kulucka.mk_v5.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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
    private TextView tvHeaterStatus;
    private TextView tvHumidifierStatus;
    private TextView tvMotorStatus;
    private ProgressBar progressTemperature;
    private ProgressBar progressHumidity;
    private ProgressBar progressDays;
    private Button btnStartProgram;
    private Button btnStopProgram;
    private Button btnRefresh;
    private Button btnSetTargetTemp;
    private Button btnSetTargetHumidity;

    // Current values
    private double currentTargetTemp = 37.5;
    private double currentTargetHumidity = 60.0;
    private boolean isProgramActive = false;

    // Data refresh
    private Handler refreshHandler;
    private Runnable refreshRunnable;
    private static final int REFRESH_INTERVAL = 3000; // 3 seconds

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
        tvHeaterStatus = view.findViewById(R.id.tv_heater_status);
        tvHumidifierStatus = view.findViewById(R.id.tv_humidifier_status);
        tvMotorStatus = view.findViewById(R.id.tv_motor_status);

        // Buttons
        btnStartProgram = view.findViewById(R.id.btn_start_program);
        btnStopProgram = view.findViewById(R.id.btn_stop_program);
        btnRefresh = view.findViewById(R.id.btn_refresh);
        btnSetTargetTemp = view.findViewById(R.id.btn_set_target_temp);
        btnSetTargetHumidity = view.findViewById(R.id.btn_set_target_humidity);
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

        if (btnSetTargetTemp != null) {
            btnSetTargetTemp.setOnClickListener(v -> showSetTargetTemperatureDialog());
        }

        if (btnSetTargetHumidity != null) {
            btnSetTargetHumidity.setOnClickListener(v -> showSetTargetHumidityDialog());
        }

        // Target temperature click listener
        if (tvTargetTemp != null) {
            tvTargetTemp.setOnClickListener(v -> showSetTargetTemperatureDialog());
        }

        // Target humidity click listener
        if (tvTargetHumidity != null) {
            tvTargetHumidity.setOnClickListener(v -> showSetTargetHumidityDialog());
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
            // Sensör verilerini güncelle - ESP32'deki createAppData fonksiyonuna uygun
            double temperature = data.optDouble("temperature", 0.0);
            double humidity = data.optDouble("humidity", 0.0);
            double targetTemp = data.optDouble("targetTemp", 37.5);
            // ESP32'de targetHumid kullanılıyor, targetHumidity değil!
            double targetHumidity = data.optDouble("targetHumid", 60.0);

            // Current target values'ları güncelle
            currentTargetTemp = targetTemp;
            currentTargetHumidity = targetHumidity;

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
            // Cihaz durumları - ESP32'deki createAppData fonksiyonuna uygun
            boolean heaterState = data.optBoolean("heaterState", false);
            boolean humidifierState = data.optBoolean("humidifierState", false);
            boolean motorState = data.optBoolean("motorState", false);

            // PID durumu
            int pidMode = data.optInt("pidMode", 0);
            boolean pidEnabled = pidMode > 0;
            if (tvPidStatus != null) {
                String pidText = pidEnabled ? "PID: Aktif" : "PID: Pasif";
                tvPidStatus.setText(pidText);
                tvPidStatus.setTextColor(getResources().getColor(
                        pidEnabled ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
            }

            // Cihaz durumları
            if (tvHeaterStatus != null) {
                tvHeaterStatus.setText(heaterState ? "Isıtıcı: AÇIK" : "Isıtıcı: KAPALI");
                tvHeaterStatus.setTextColor(getResources().getColor(
                        heaterState ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
            }

            if (tvHumidifierStatus != null) {
                tvHumidifierStatus.setText(humidifierState ? "Nemlendirici: AÇIK" : "Nemlendirici: KAPALI");
                tvHumidifierStatus.setTextColor(getResources().getColor(
                        humidifierState ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
            }

            if (tvMotorStatus != null) {
                tvMotorStatus.setText(motorState ? "Motor: AÇIK" : "Motor: KAPALI");
                tvMotorStatus.setTextColor(getResources().getColor(
                        motorState ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
            }

            // Çevirme durumu (motor durumundan türetiliyor)
            if (tvTurningStatus != null) {
                tvTurningStatus.setText(motorState ? "Çevirme: Aktif" : "Çevirme: Pasif");
                tvTurningStatus.setTextColor(getResources().getColor(
                        motorState ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
            }

            // Program durumu - ESP32'deki createAppData'dan gelen veriler
            boolean isIncubationRunning = data.optBoolean("isIncubationRunning", false);
            int currentDay = data.optInt("currentDay", 0);
            int totalDays = data.optInt("totalDays", 0);

            isProgramActive = isIncubationRunning;

            if (tvProgramStatus != null) {
                tvProgramStatus.setText(isProgramActive ? "Program: Çalışıyor" : "Program: Durduruldu");
                tvProgramStatus.setTextColor(getResources().getColor(
                        isProgramActive ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
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
                btnStartProgram.setEnabled(!isProgramActive);
                btnStopProgram.setEnabled(isProgramActive);
            }

        } catch (Exception e) {
            Log.e(TAG, "Status update error: " + e.getMessage());
        }
    }

    private void showSetTargetTemperatureDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Hedef Sıcaklık Ayarla");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Sıcaklık (°C)");
        input.setText(String.valueOf(currentTargetTemp));
        layout.addView(input);

        TextView info = new TextView(requireContext());
        info.setText("Kuluçka için önerilen sıcaklık aralığı: 37.0°C - 38.0°C");
        info.setTextSize(12);
        info.setPadding(0, 10, 0, 0);
        layout.addView(info);

        builder.setView(layout);

        builder.setPositiveButton("Ayarla", (dialog, which) -> {
            try {
                double newTemp = Double.parseDouble(input.getText().toString());
                if (newTemp < 30.0 || newTemp > 45.0) {
                    showToast("Sıcaklık 30.0°C ile 45.0°C arasında olmalıdır");
                    return;
                }
                setTargetTemperature(newTemp);
            } catch (NumberFormatException e) {
                showToast("Geçerli bir sıcaklık değeri girin");
            }
        });

        builder.setNegativeButton("İptal", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showSetTargetHumidityDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Hedef Nem Ayarla");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Nem (%)");
        input.setText(String.valueOf(currentTargetHumidity));
        layout.addView(input);

        TextView info = new TextView(requireContext());
        info.setText("Kuluçka için önerilen nem aralığı: %55 - %75");
        info.setTextSize(12);
        info.setPadding(0, 10, 0, 0);
        layout.addView(info);

        builder.setView(layout);

        builder.setPositiveButton("Ayarla", (dialog, which) -> {
            try {
                double newHumidity = Double.parseDouble(input.getText().toString());
                if (newHumidity < 30.0 || newHumidity > 90.0) {
                    showToast("Nem %30 ile %90 arasında olmalıdır");
                    return;
                }
                setTargetHumidity(newHumidity);
            } catch (NumberFormatException e) {
                showToast("Geçerli bir nem değeri girin");
            }
        });

        builder.setNegativeButton("İptal", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void setTargetTemperature(double temperature) {
        ApiService.getInstance().setTemperature(temperature, new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        currentTargetTemp = temperature;
                        showToast("Hedef sıcaklık " + String.format("%.1f°C", temperature) + " olarak ayarlandı");
                        refreshData();
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Sıcaklık ayarlanamadı: " + message);
                    });
                }
            }
        });
    }

    private void setTargetHumidity(double humidity) {
        ApiService.getInstance().setHumidity(humidity, new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        currentTargetHumidity = humidity;
                        showToast("Hedef nem %" + String.format("%.1f", humidity) + " olarak ayarlandı");
                        refreshData();
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Nem ayarlanamadı: " + message);
                    });
                }
            }
        });
    }

    private void loadInitialData() {
        refreshData();
    }

    private void refreshData() {
        if (getActivity() != null) {
            // ESP32'nin createAppData fonksiyonunu çağırmak için getAppData kullan
            ApiService.getInstance().getAppData(new ApiService.DataCallback() {
                @Override
                public void onSuccess(JSONObject data) {
                    if (getActivity() != null) {
                        updateData(data);
                    }
                }

                @Override
                public void onError(String message) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Log.w(TAG, "Failed to refresh data: " + message);
                            // Bağlantı kopuksa bunu göster ama sürekli toast gösterme
                            if (message.contains("network") || message.contains("connection")) {
                                // Sadece ilk bağlantı hatasında bildir
                                updateConnectionStatus(false);
                            }
                        });
                    }
                }
            });
        }
    }

    private void updateConnectionStatus(boolean connected) {
        // MainActivity'e bağlantı durumunu bildir
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateConnectionStatus(connected);
        }
    }

    private void startIncubationProgram() {
        ApiService.getInstance().startIncubation(21, new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Kuluçka programı başlatıldı");
                        isProgramActive = true;
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
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Kuluçka Durdur");
        builder.setMessage("Kuluçka programını durdurmak istediğinizden emin misiniz?");

        builder.setPositiveButton("Evet, Durdur", (dialog, which) -> {
            ApiService.getInstance().stopIncubation(new ApiService.ParameterCallback() {
                @Override
                public void onSuccess() {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showToast("Kuluçka programı durduruldu");
                            isProgramActive = false;
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
        });

        builder.setNegativeButton("İptal", (dialog, which) -> dialog.dismiss());
        builder.show();
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