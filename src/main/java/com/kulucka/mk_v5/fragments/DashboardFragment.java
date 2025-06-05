package com.kulucka.mk_v5.fragments;

import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kulucka.mk_v5.R;
import com.kulucka.mk_v5.services.ApiService;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private static final String TAG = "DashboardFragment";

    // Temperature UI Components
    private TextView tvCurrentTemp;
    private TextView tvTargetTemp;
    private TextView tvTempStatus;
    private ProgressBar pbTemperature;
    private ImageView ivHeaterStatus;

    // Humidity UI Components
    private TextView tvCurrentHumidity;
    private TextView tvTargetHumidity;
    private TextView tvHumidityStatus;
    private ProgressBar pbHumidity;
    private ImageView ivHumidifierStatus;

    // Motor UI Components
    private TextView tvMotorStatus;
    private TextView tvLastTurnTime;
    private ImageView ivMotorStatus;

    // Incubation Info UI Components
    private TextView tvIncubationType;
    private TextView tvCurrentDay;
    private TextView tvRemainingDays;
    private TextView tvEstimatedHatchDate;
    private ProgressBar pbIncubationProgress;

    // System Status UI Components
    private TextView tvSystemStatus;
    private TextView tvLastUpdateTime;
    private ImageView ivSystemStatus;

    // Alarm UI Components
    private View alarmIndicator;
    private TextView tvAlarmStatus;

    // Data refresh
    private Handler refreshHandler;
    private boolean isFragmentActive = false;
    private static final long REFRESH_INTERVAL = 10000; // 10 saniye

    // Son alınan veriler
    private JSONObject lastReceivedData;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        initializeViews(view);
        setupRefreshHandler();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        isFragmentActive = true;
        startPeriodicRefresh();
        refreshDataFromServer();
    }

    @Override
    public void onPause() {
        super.onPause();
        isFragmentActive = false;
        stopPeriodicRefresh();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopPeriodicRefresh();
        if (refreshHandler != null) {
            refreshHandler.removeCallbacksAndMessages(null);
        }
    }

    private void initializeViews(View view) {
        // Temperature views
        tvCurrentTemp = view.findViewById(R.id.tv_current_temp);
        tvTargetTemp = view.findViewById(R.id.tv_target_temp);
        tvTempStatus = view.findViewById(R.id.tv_temp_status);
        pbTemperature = view.findViewById(R.id.pb_temperature);
        ivHeaterStatus = view.findViewById(R.id.iv_heater_status);

        // Humidity views
        tvCurrentHumidity = view.findViewById(R.id.tv_current_humidity);
        tvTargetHumidity = view.findViewById(R.id.tv_target_humidity);
        tvHumidityStatus = view.findViewById(R.id.tv_humidity_status);
        pbHumidity = view.findViewById(R.id.pb_humidity);
        ivHumidifierStatus = view.findViewById(R.id.iv_humidifier_status);

        // Motor views
        tvMotorStatus = view.findViewById(R.id.tv_motor_status);
        tvLastTurnTime = view.findViewById(R.id.tv_last_turn_time);
        ivMotorStatus = view.findViewById(R.id.iv_motor_status);

        // Incubation info views
        tvIncubationType = view.findViewById(R.id.tv_incubation_type);
        tvCurrentDay = view.findViewById(R.id.tv_current_day);
        tvRemainingDays = view.findViewById(R.id.tv_remaining_days);
        tvEstimatedHatchDate = view.findViewById(R.id.tv_estimated_hatch_date);
        pbIncubationProgress = view.findViewById(R.id.pb_incubation_progress);

        // System status views
        tvSystemStatus = view.findViewById(R.id.tv_system_status);
        tvLastUpdateTime = view.findViewById(R.id.tv_last_update_time);
        ivSystemStatus = view.findViewById(R.id.iv_system_status);

        // Alarm views
        alarmIndicator = view.findViewById(R.id.alarm_indicator);
        tvAlarmStatus = view.findViewById(R.id.tv_alarm_status);

        // İlk değerler
        setDefaultValues();
    }

    private void setDefaultValues() {
        if (tvCurrentTemp != null) tvCurrentTemp.setText("--°C");
        if (tvTargetTemp != null) tvTargetTemp.setText("--°C");
        if (tvCurrentHumidity != null) tvCurrentHumidity.setText("--%");
        if (tvTargetHumidity != null) tvTargetHumidity.setText("--%");
        if (tvMotorStatus != null) tvMotorStatus.setText("Bilinmiyor");
        if (tvIncubationType != null) tvIncubationType.setText("Kuluçka Başlatılmamış");
        if (tvCurrentDay != null) tvCurrentDay.setText("0");
        if (tvRemainingDays != null) tvRemainingDays.setText("--");
        if (tvSystemStatus != null) tvSystemStatus.setText("Bağlantı Bekleniyor");
        if (tvTempStatus != null) tvTempStatus.setText("--");
        if (tvHumidityStatus != null) tvHumidityStatus.setText("--");
        if (tvAlarmStatus != null) tvAlarmStatus.setText("Alarm Yok");

        if (alarmIndicator != null) alarmIndicator.setVisibility(View.GONE);
    }

    private void setupRefreshHandler() {
        refreshHandler = new Handler();
    }

    private void startPeriodicRefresh() {
        if (refreshHandler != null && isFragmentActive) {
            refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
        }
    }

    private void stopPeriodicRefresh() {
        if (refreshHandler != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (isFragmentActive) {
                refreshDataFromServer();
                refreshHandler.postDelayed(this, REFRESH_INTERVAL);
            }
        }
    };

    private void refreshDataFromServer() {
        // ESP32'nin /api/status endpoint'ini kullan (getAppData yerine getStatus)
        ApiService.getInstance().getStatus(new ApiService.DataCallback() {
            @Override
            public void onSuccess(JSONObject data) {
                if (isFragmentActive && getActivity() != null) {
                    getActivity().runOnUiThread(() -> updateData(data));
                }
            }

            @Override
            public void onError(String message) {
                if (isFragmentActive && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Log.w(TAG, "Veri yenileme hatası: " + message);
                        updateConnectionStatus(false);
                    });
                }
            }
        });
    }

    /**
     * MainActivity veya DataRefreshService'den gelen veriyi işle
     */
    public void updateData(JSONObject data) {
        if (data == null) return;

        try {
            lastReceivedData = data;

            // Sıcaklık verileri
            updateTemperatureData(data);

            // Nem verileri
            updateHumidityData(data);

            // Motor verileri
            updateMotorData(data);

            // Kuluçka bilgileri
            updateIncubationData(data);

            // Sistem durumu
            updateSystemStatus(data);

            // Alarm durumu
            updateAlarmStatus(data);

            // Son güncelleme zamanı
            updateLastUpdateTime();

            // Bağlantı durumu
            updateConnectionStatus(true);

        } catch (Exception e) {
            Log.e(TAG, "Veri güncelleme hatası: " + e.getMessage());
        }
    }

    private void updateTemperatureData(JSONObject data) {
        try {
            double currentTemp = data.optDouble("temperature", 0.0);
            double targetTemp = data.optDouble("targetTemp", 37.5);
            boolean heaterState = data.optBoolean("heaterState", false);

            // Sıcaklık değerleri
            if (tvCurrentTemp != null) {
                tvCurrentTemp.setText(String.format(Locale.getDefault(), "%.1f°C", currentTemp));
            }
            if (tvTargetTemp != null) {
                tvTargetTemp.setText(String.format(Locale.getDefault(), "%.1f°C", targetTemp));
            }

            // Sıcaklık durumu
            String tempStatus;
            int tempColor;
            if (Math.abs(currentTemp - targetTemp) <= 0.2) {
                tempStatus = "İdeal";
                tempColor = Color.GREEN;
            } else if (currentTemp < targetTemp) {
                tempStatus = "Düşük";
                tempColor = Color.BLUE;
            } else {
                tempStatus = "Yüksek";
                tempColor = Color.RED;
            }

            if (tvTempStatus != null) {
                tvTempStatus.setText(tempStatus);
                tvTempStatus.setTextColor(tempColor);
            }

            // Isıtıcı durumu
            if (ivHeaterStatus != null) {
                ivHeaterStatus.setImageResource(heaterState ?
                        R.drawable.ic_heater_on : R.drawable.ic_heater_off);
            }

            // Sıcaklık progress bar'ı (30-40°C arası)
            if (pbTemperature != null) {
                float tempProgress = ((float) (currentTemp - 30.0) / 10.0) * 100;
                tempProgress = Math.max(0, Math.min(100, tempProgress));
                animateProgressBar(pbTemperature, tempProgress);
            }

        } catch (Exception e) {
            Log.e(TAG, "Sıcaklık verisi güncelleme hatası: " + e.getMessage());
        }
    }

    private void updateHumidityData(JSONObject data) {
        try {
            double currentHumidity = data.optDouble("humidity", 0.0);
            double targetHumidity = data.optDouble("targetHumid", 60.0); // ESP32'de targetHumid kullanılıyor!
            boolean humidifierState = data.optBoolean("humidifierState", false);

            // Nem değerleri
            if (tvCurrentHumidity != null) {
                tvCurrentHumidity.setText(String.format(Locale.getDefault(), "%.1f%%", currentHumidity));
            }
            if (tvTargetHumidity != null) {
                tvTargetHumidity.setText(String.format(Locale.getDefault(), "%.1f%%", targetHumidity));
            }

            // Nem durumu
            String humidityStatus;
            int humidityColor;
            if (Math.abs(currentHumidity - targetHumidity) <= 5.0) {
                humidityStatus = "İdeal";
                humidityColor = Color.GREEN;
            } else if (currentHumidity < targetHumidity) {
                humidityStatus = "Düşük";
                humidityColor = Color.BLUE;
            } else {
                humidityStatus = "Yüksek";
                humidityColor = Color.RED;
            }

            if (tvHumidityStatus != null) {
                tvHumidityStatus.setText(humidityStatus);
                tvHumidityStatus.setTextColor(humidityColor);
            }

            // Nemlendirici durumu
            if (ivHumidifierStatus != null) {
                ivHumidifierStatus.setImageResource(humidifierState ?
                        R.drawable.ic_humidifier_on : R.drawable.ic_humidifier_off);
            }

            // Nem progress bar'ı (0-100% arası)
            if (pbHumidity != null) {
                animateProgressBar(pbHumidity, (float) currentHumidity);
            }

        } catch (Exception e) {
            Log.e(TAG, "Nem verisi güncelleme hatası: " + e.getMessage());
        }
    }

    private void updateMotorData(JSONObject data) {
        try {
            boolean motorState = data.optBoolean("motorState", false);
            long lastTurnTime = data.optLong("lastTurnTime", 0);

            // Motor durumu
            String motorStatus = motorState ? "Çalışıyor" : "Durdu";
            if (tvMotorStatus != null) {
                tvMotorStatus.setText(motorStatus);
                tvMotorStatus.setTextColor(motorState ? Color.GREEN : Color.GRAY);
            }

            // Motor ikonu
            if (ivMotorStatus != null) {
                ivMotorStatus.setImageResource(motorState ?
                        R.drawable.ic_motor_on : R.drawable.ic_motor_off);
            }

            // Son çevirme zamanı
            if (tvLastTurnTime != null && lastTurnTime > 0) {
                Date lastTurn = new Date(lastTurnTime);
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                tvLastTurnTime.setText("Son: " + sdf.format(lastTurn));
            } else if (tvLastTurnTime != null) {
                tvLastTurnTime.setText("Bilinmiyor");
            }

        } catch (Exception e) {
            Log.e(TAG, "Motor verisi güncelleme hatası: " + e.getMessage());
        }
    }

    private void updateIncubationData(JSONObject data) {
        try {
            String incubationType = data.optString("incubationType", "");
            int currentDay = data.optInt("currentDay", 0);
            int totalDays = data.optInt("totalDays", 21);
            long startTime = data.optLong("startTime", 0);

            // Kuluçka tipi
            if (tvIncubationType != null) {
                if (incubationType.isEmpty()) {
                    tvIncubationType.setText("Kuluçka Başlatılmamış");
                } else {
                    tvIncubationType.setText(incubationType);
                }
            }

            // Güncel gün
            if (tvCurrentDay != null) {
                tvCurrentDay.setText(String.valueOf(currentDay));
            }

            // Kalan günler
            int remainingDays = Math.max(0, totalDays - currentDay);
            if (tvRemainingDays != null) {
                tvRemainingDays.setText(String.valueOf(remainingDays));
            }

            // Tahmini çıkış tarihi
            if (tvEstimatedHatchDate != null && startTime > 0 && totalDays > 0) {
                long hatchTime = startTime + ((long) totalDays * 24 * 60 * 60 * 1000);
                Date hatchDate = new Date(hatchTime);
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                tvEstimatedHatchDate.setText(sdf.format(hatchDate));
            } else if (tvEstimatedHatchDate != null) {
                tvEstimatedHatchDate.setText("--/--/----");
            }

            // İlerleme çubuğu
            if (pbIncubationProgress != null && totalDays > 0) {
                float progress = ((float) currentDay / totalDays) * 100;
                progress = Math.max(0, Math.min(100, progress));
                animateProgressBar(pbIncubationProgress, progress);
            }

        } catch (Exception e) {
            Log.e(TAG, "Kuluçka verisi güncelleme hatası: " + e.getMessage());
        }
    }

    private void updateSystemStatus(JSONObject data) {
        try {
            // ESP32'nin sistem durumu bilgisi
            boolean systemOk = data.optBoolean("systemOk", true);
            String statusMessage = data.optString("statusMessage", "Normal");

            if (tvSystemStatus != null) {
                tvSystemStatus.setText(statusMessage);
                tvSystemStatus.setTextColor(systemOk ? Color.GREEN : Color.RED);
            }

            if (ivSystemStatus != null) {
                ivSystemStatus.setImageResource(systemOk ?
                        android.R.drawable.presence_online : android.R.drawable.presence_busy);
            }

        } catch (Exception e) {
            Log.e(TAG, "Sistem durumu güncelleme hatası: " + e.getMessage());
        }
    }

    private void updateAlarmStatus(JSONObject data) {
        try {
            boolean alarmEnabled = data.optBoolean("alarmEnabled", false);

            if (alarmEnabled) {
                double currentTemp = data.optDouble("temperature", 0.0);
                double currentHumidity = data.optDouble("humidity", 0.0);
                double targetTemp = data.optDouble("targetTemp", 0.0);
                double targetHumid = data.optDouble("targetHumid", 0.0);

                double tempLowAlarm = data.optDouble("tempLowAlarm", 1.0);
                double tempHighAlarm = data.optDouble("tempHighAlarm", 1.0);
                double humidLowAlarm = data.optDouble("humidLowAlarm", 10.0);
                double humidHighAlarm = data.optDouble("humidHighAlarm", 10.0);

                // Alarm kontrolü - hedef değerlerden sapma bazında
                boolean tempAlarm = (currentTemp < targetTemp - tempLowAlarm) ||
                        (currentTemp > targetTemp + tempHighAlarm);
                boolean humidityAlarm = (currentHumidity < targetHumid - humidLowAlarm) ||
                        (currentHumidity > targetHumid + humidHighAlarm);

                boolean anyAlarm = tempAlarm || humidityAlarm;

                if (alarmIndicator != null) {
                    alarmIndicator.setVisibility(anyAlarm ? View.VISIBLE : View.GONE);
                }

                if (tvAlarmStatus != null) {
                    if (anyAlarm) {
                        String alarmText = "";
                        if (tempAlarm && humidityAlarm) {
                            alarmText = "Sıcaklık ve Nem Alarmı";
                        } else if (tempAlarm) {
                            alarmText = "Sıcaklık Alarmı";
                        } else if (humidityAlarm) {
                            alarmText = "Nem Alarmı";
                        }
                        tvAlarmStatus.setText(alarmText);
                        tvAlarmStatus.setTextColor(Color.RED);
                    } else {
                        tvAlarmStatus.setText("Alarm Yok");
                        tvAlarmStatus.setTextColor(Color.GREEN);
                    }
                }
            } else {
                if (alarmIndicator != null) {
                    alarmIndicator.setVisibility(View.GONE);
                }
                if (tvAlarmStatus != null) {
                    tvAlarmStatus.setText("Alarm Devre Dışı");
                    tvAlarmStatus.setTextColor(Color.GRAY);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Alarm durumu güncelleme hatası: " + e.getMessage());
        }
    }

    private void updateLastUpdateTime() {
        if (tvLastUpdateTime != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            tvLastUpdateTime.setText("Son güncelleme: " + sdf.format(new Date()));
        }
    }

    private void updateConnectionStatus(boolean connected) {
        if (tvSystemStatus != null && !connected) {
            tvSystemStatus.setText("Bağlantı Sorunu");
            tvSystemStatus.setTextColor(Color.RED);
        }

        if (ivSystemStatus != null) {
            ivSystemStatus.setImageResource(connected ?
                    android.R.drawable.presence_online : android.R.drawable.presence_busy);
        }
    }

    private void animateProgressBar(ProgressBar progressBar, float targetProgress) {
        if (progressBar == null) return;

        ObjectAnimator animation = ObjectAnimator.ofInt(
                progressBar,
                "progress",
                progressBar.getProgress(),
                Math.round(targetProgress)
        );
        animation.setDuration(1000);
        animation.start();
    }

    /**
     * Manuel veri yenileme
     */
    public void refreshData() {
        refreshDataFromServer();
    }

    /**
     * Son alınan veriyi döndür
     */
    public JSONObject getLastReceivedData() {
        return lastReceivedData;
    }
}