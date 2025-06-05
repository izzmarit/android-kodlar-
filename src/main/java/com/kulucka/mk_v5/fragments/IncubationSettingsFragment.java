package com.kulucka.mk_v5.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
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

public class IncubationSettingsFragment extends Fragment {

    private static final String TAG = "IncubationSettings";

    // UI Components - Kuluçka Tipi
    private Spinner spinnerIncubationType;
    private TextView tvIncubationDescription;
    private TextView tvEstimatedDuration;

    // UI Components - Hedef Değerler
    private EditText etTargetTemp;
    private EditText etTargetHumidity;
    private TextView tvTempRange;
    private TextView tvHumidityRange;

    // UI Components - Motor Ayarları
    private Switch switchMotorEnabled;
    private EditText etTurnInterval;
    private EditText etTurnDuration;
    private TextView tvMotorStatus;

    // UI Components - Kuluçka Durumu
    private TextView tvCurrentStatus;
    private TextView tvCurrentDay;
    private TextView tvRemainingDays;
    private TextView tvStartDate;
    private TextView tvEstimatedEndDate;
    private ProgressBar pbIncubationProgress;

    // UI Components - Kontrol Butonları
    private Button btnStartIncubation;
    private Button btnStopIncubation;
    private Button btnSaveSettings;
    private Button btnLoadSettings;
    private Button btnResetToDefaults;

    // Veri yenileme
    private Handler refreshHandler;
    private boolean isFragmentActive = false;
    private static final long REFRESH_INTERVAL = 15000; // 15 saniye

    // Kuluçka tipleri - ESP32'deki enum ile eşleşmeli
    private static final String[] INCUBATION_TYPES = {
            "Tavuk Yumurtası",
            "Ördek Yumurtası",
            "Kaz Yumurtası",
            "Hindi Yumurtası",
            "Güvercin Yumurtası",
            "Özel Ayar"
    };

    // Her tip için varsayılan değerler
    private static final int[] DEFAULT_DAYS = {21, 28, 30, 28, 18, 21};
    private static final double[] DEFAULT_TEMPS = {37.5, 37.5, 37.5, 37.5, 37.5, 37.5};
    private static final double[] DEFAULT_HUMIDITIES = {60.0, 65.0, 65.0, 65.0, 60.0, 60.0};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_incubation_settings, container, false);
        initializeViews(view);
        setupEventListeners();
        setupRefreshHandler();
        loadCurrentSettings();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        isFragmentActive = true;
        startPeriodicRefresh();
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
        // Kuluçka tipi
        spinnerIncubationType = view.findViewById(R.id.spinner_incubation_type);
        tvIncubationDescription = view.findViewById(R.id.tv_incubation_description);
        tvEstimatedDuration = view.findViewById(R.id.tv_estimated_duration);

        // Hedef değerler
        etTargetTemp = view.findViewById(R.id.et_target_temp);
        etTargetHumidity = view.findViewById(R.id.et_target_humidity);
        tvTempRange = view.findViewById(R.id.tv_temp_range);
        tvHumidityRange = view.findViewById(R.id.tv_humidity_range);

        // Motor ayarları
        switchMotorEnabled = view.findViewById(R.id.switch_motor_enabled);
        etTurnInterval = view.findViewById(R.id.et_turn_interval);
        etTurnDuration = view.findViewById(R.id.et_turn_duration);
        tvMotorStatus = view.findViewById(R.id.tv_motor_status);

        // Kuluçka durumu
        tvCurrentStatus = view.findViewById(R.id.tv_current_status);
        tvCurrentDay = view.findViewById(R.id.tv_current_day);
        tvRemainingDays = view.findViewById(R.id.tv_remaining_days);
        tvStartDate = view.findViewById(R.id.tv_start_date);
        tvEstimatedEndDate = view.findViewById(R.id.tv_estimated_end_date);
        pbIncubationProgress = view.findViewById(R.id.pb_incubation_progress);

        // Kontrol butonları
        btnStartIncubation = view.findViewById(R.id.btn_start_incubation);
        btnStopIncubation = view.findViewById(R.id.btn_stop_incubation);
        btnSaveSettings = view.findViewById(R.id.btn_save_settings);
        btnLoadSettings = view.findViewById(R.id.btn_load_settings);
        btnResetToDefaults = view.findViewById(R.id.btn_reset_defaults);

        setupSpinner();
    }

    private void setupSpinner() {
        if (spinnerIncubationType != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    INCUBATION_TYPES
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerIncubationType.setAdapter(adapter);
        }
    }

    private void setupEventListeners() {
        // Kuluçka tipi değişikliği
        if (spinnerIncubationType != null) {
            spinnerIncubationType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    updateIncubationTypeDetails(position);
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {
                }
            });
        }

        // Hedef değer değişiklikleri
        if (etTargetTemp != null) {
            etTargetTemp.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    validateTemperature();
                }
            });
        }

        if (etTargetHumidity != null) {
            etTargetHumidity.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    validateHumidity();
                }
            });
        }

        // Motor ayarları
        if (switchMotorEnabled != null) {
            switchMotorEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
                updateMotorControls(isChecked);
            });
        }

        // Buton click listener'ları
        if (btnStartIncubation != null) {
            btnStartIncubation.setOnClickListener(v -> showStartIncubationDialog());
        }

        if (btnStopIncubation != null) {
            btnStopIncubation.setOnClickListener(v -> showStopIncubationDialog());
        }

        if (btnSaveSettings != null) {
            btnSaveSettings.setOnClickListener(v -> saveSettings());
        }

        if (btnLoadSettings != null) {
            btnLoadSettings.setOnClickListener(v -> loadCurrentSettings());
        }

        if (btnResetToDefaults != null) {
            btnResetToDefaults.setOnClickListener(v -> showResetDialog());
        }
    }

    private void updateIncubationTypeDetails(int position) {
        if (position < 0 || position >= INCUBATION_TYPES.length) return;

        String type = INCUBATION_TYPES[position];
        int days = DEFAULT_DAYS[position];
        double temp = DEFAULT_TEMPS[position];
        double humidity = DEFAULT_HUMIDITIES[position];

        // Açıklama güncelle
        if (tvIncubationDescription != null) {
            String description = getIncubationDescription(position);
            tvIncubationDescription.setText(description);
        }

        // Tahmini süre
        if (tvEstimatedDuration != null) {
            tvEstimatedDuration.setText(days + " gün");
        }

        // Özel ayar değilse hedef değerleri güncelle
        if (position != INCUBATION_TYPES.length - 1) { // "Özel Ayar" değilse
            if (etTargetTemp != null) {
                etTargetTemp.setText(String.format(Locale.getDefault(), "%.1f", temp));
            }
            if (etTargetHumidity != null) {
                etTargetHumidity.setText(String.format(Locale.getDefault(), "%.1f", humidity));
            }
        }

        // Güvenli aralıkları göster
        updateRangeDisplays();
    }

    private String getIncubationDescription(int position) {
        switch (position) {
            case 0: return "En yaygın kuluçka türü. 21 gün sürer. Orta seviye bakım gerektirir.";
            case 1: return "Tavuk yumurtasından daha uzun sürer. 28 gün. Nem kontrolü önemli.";
            case 2: return "En uzun kuluçka süresi. 30 gün. Yüksek nem gerektirir.";
            case 3: return "Hindi yumurtası için özel ayarlar. 28 gün sürer.";
            case 4: return "Küçük yumurtalar için kısa kuluçka süresi. 18 gün.";
            case 5: return "Manuel olarak ayarlanmış özel kuluçka parametreleri.";
            default: return "Bilinmeyen kuluçka tipi.";
        }
    }

    private void validateTemperature() {
        if (etTargetTemp == null) return;

        try {
            String tempStr = etTargetTemp.getText().toString();
            if (!tempStr.isEmpty()) {
                double temp = Double.parseDouble(tempStr);
                if (temp < 35.0 || temp > 40.0) {
                    etTargetTemp.setError("Sıcaklık 35-40°C aralığında olmalı");
                } else {
                    etTargetTemp.setError(null);
                }
                updateRangeDisplays();
            }
        } catch (NumberFormatException e) {
            etTargetTemp.setError("Geçerli bir sayı girin");
        }
    }

    private void validateHumidity() {
        if (etTargetHumidity == null) return;

        try {
            String humidStr = etTargetHumidity.getText().toString();
            if (!humidStr.isEmpty()) {
                double humidity = Double.parseDouble(humidStr);
                if (humidity < 40.0 || humidity > 80.0) {
                    etTargetHumidity.setError("Nem %40-80 aralığında olmalı");
                } else {
                    etTargetHumidity.setError(null);
                }
                updateRangeDisplays();
            }
        } catch (NumberFormatException e) {
            etTargetHumidity.setError("Geçerli bir sayı girin");
        }
    }

    private void updateRangeDisplays() {
        // Sıcaklık aralığı
        if (tvTempRange != null && etTargetTemp != null) {
            try {
                String tempStr = etTargetTemp.getText().toString();
                if (!tempStr.isEmpty()) {
                    double temp = Double.parseDouble(tempStr);
                    double lowRange = temp - 0.5;
                    double highRange = temp + 0.5;
                    tvTempRange.setText(String.format(Locale.getDefault(),
                            "Güvenli aralık: %.1f - %.1f°C", lowRange, highRange));
                }
            } catch (NumberFormatException e) {
                tvTempRange.setText("Geçerli sıcaklık girin");
            }
        }

        // Nem aralığı
        if (tvHumidityRange != null && etTargetHumidity != null) {
            try {
                String humidStr = etTargetHumidity.getText().toString();
                if (!humidStr.isEmpty()) {
                    double humidity = Double.parseDouble(humidStr);
                    double lowRange = humidity - 10.0;
                    double highRange = humidity + 10.0;
                    tvHumidityRange.setText(String.format(Locale.getDefault(),
                            "Güvenli aralık: %.1f - %.1f%%", lowRange, highRange));
                }
            } catch (NumberFormatException e) {
                tvHumidityRange.setText("Geçerli nem girin");
            }
        }
    }

    private void updateMotorControls(boolean enabled) {
        if (etTurnInterval != null) {
            etTurnInterval.setEnabled(enabled);
        }
        if (etTurnDuration != null) {
            etTurnDuration.setEnabled(enabled);
        }

        if (tvMotorStatus != null) {
            tvMotorStatus.setText(enabled ? "Motor Aktif" : "Motor Devre Dışı");
        }
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
                loadCurrentSettings();
                refreshHandler.postDelayed(this, REFRESH_INTERVAL);
            }
        }
    };

    private void loadCurrentSettings() {
        // ESP32'den güncel kuluçka ayarlarını al
        ApiService.getInstance().getAppData(new ApiService.DataCallback() {
            @Override
            public void onSuccess(JSONObject data) {
                if (isFragmentActive && getActivity() != null) {
                    getActivity().runOnUiThread(() -> updateUIWithCurrentData(data));
                }
            }

            @Override
            public void onError(String message) {
                if (isFragmentActive && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Log.w(TAG, "Ayarlar yüklenemedi: " + message);
                        showToast("Bağlantı hatası: " + message);
                    });
                }
            }
        });
    }

    private void updateUIWithCurrentData(JSONObject data) {
        try {
            // Kuluçka durumu
            String incubationType = data.optString("incubationType", "");
            int currentDay = data.optInt("currentDay", 0);
            int totalDays = data.optInt("totalDays", 21);
            long startTime = data.optLong("startTime", 0);
            boolean isActive = data.optBoolean("incubationActive", false);

            // Hedef değerler
            double targetTemp = data.optDouble("targetTemp", 37.5);
            double targetHumid = data.optDouble("targetHumid", 60.0); // ESP32'de targetHumid

            // Motor ayarları
            boolean motorEnabled = data.optBoolean("motorEnabled", true);
            int turnInterval = data.optInt("turnInterval", 120); // dakika
            int turnDuration = data.optInt("turnDuration", 5); // saniye

            // UI'yi güncelle
            updateCurrentStatus(isActive, incubationType, currentDay, totalDays, startTime);
            updateTargetValues(targetTemp, targetHumid);
            updateMotorSettings(motorEnabled, turnInterval, turnDuration);
            updateButtons(isActive);

        } catch (Exception e) {
            Log.e(TAG, "UI güncelleme hatası: " + e.getMessage());
        }
    }

    private void updateCurrentStatus(boolean isActive, String type, int currentDay, int totalDays, long startTime) {
        // Durum
        if (tvCurrentStatus != null) {
            tvCurrentStatus.setText(isActive ? "Aktif" : "Durdurulmuş");
        }

        // Güncel gün
        if (tvCurrentDay != null) {
            tvCurrentDay.setText(String.valueOf(currentDay));
        }

        // Kalan günler
        if (tvRemainingDays != null) {
            int remaining = Math.max(0, totalDays - currentDay);
            tvRemainingDays.setText(String.valueOf(remaining));
        }

        // Başlangıç tarihi
        if (tvStartDate != null && startTime > 0) {
            Date startDate = new Date(startTime);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            tvStartDate.setText(sdf.format(startDate));
        } else if (tvStartDate != null) {
            tvStartDate.setText("Başlatılmamış");
        }

        // Tahmini bitiş tarihi
        if (tvEstimatedEndDate != null && startTime > 0 && totalDays > 0) {
            long endTime = startTime + ((long) totalDays * 24 * 60 * 60 * 1000);
            Date endDate = new Date(endTime);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            tvEstimatedEndDate.setText(sdf.format(endDate));
        } else if (tvEstimatedEndDate != null) {
            tvEstimatedEndDate.setText("--/--/---- --:--");
        }

        // İlerleme çubuğu
        if (pbIncubationProgress != null && totalDays > 0) {
            float progress = ((float) currentDay / totalDays) * 100;
            progress = Math.max(0, Math.min(100, progress));
            pbIncubationProgress.setProgress((int) progress);
        }

        // Spinner'ı güncelle
        if (spinnerIncubationType != null && !type.isEmpty()) {
            for (int i = 0; i < INCUBATION_TYPES.length; i++) {
                if (INCUBATION_TYPES[i].equals(type)) {
                    spinnerIncubationType.setSelection(i);
                    break;
                }
            }
        }
    }

    private void updateTargetValues(double targetTemp, double targetHumid) {
        if (etTargetTemp != null) {
            etTargetTemp.setText(String.format(Locale.getDefault(), "%.1f", targetTemp));
        }
        if (etTargetHumidity != null) {
            etTargetHumidity.setText(String.format(Locale.getDefault(), "%.1f", targetHumid));
        }
        updateRangeDisplays();
    }

    private void updateMotorSettings(boolean enabled, int interval, int duration) {
        if (switchMotorEnabled != null) {
            switchMotorEnabled.setChecked(enabled);
        }
        if (etTurnInterval != null) {
            etTurnInterval.setText(String.valueOf(interval));
        }
        if (etTurnDuration != null) {
            etTurnDuration.setText(String.valueOf(duration));
        }
        updateMotorControls(enabled);
    }

    private void updateButtons(boolean isActive) {
        if (btnStartIncubation != null) {
            btnStartIncubation.setEnabled(!isActive);
        }
        if (btnStopIncubation != null) {
            btnStopIncubation.setEnabled(isActive);
        }
    }

    private void showStartIncubationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Kuluçka Başlat")
                .setMessage("Kuluçka işlemini başlatmak istediğinizden emin misiniz?\n\n" +
                        "Bu işlem mevcut ayarları kullanarak yeni bir kuluçka döngüsü başlatacaktır.")
                .setPositiveButton("Başlat", (dialog, which) -> startIncubation())
                .setNegativeButton("İptal", null)
                .show();
    }

    private void showStopIncubationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Kuluçka Durdur")
                .setMessage("Kuluçka işlemini durdurmak istediğinizden emin misiniz?\n\n" +
                        "Bu işlem mevcut kuluçka döngüsünü sonlandıracaktır.")
                .setPositiveButton("Durdur", (dialog, which) -> stopIncubation())
                .setNegativeButton("İptal", null)
                .show();
    }

    private void showResetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Varsayılan Ayarlar")
                .setMessage("Tüm ayarları varsayılan değerlere sıfırlamak istediğinizden emin misiniz?")
                .setPositiveButton("Sıfırla", (dialog, which) -> resetToDefaults())
                .setNegativeButton("İptal", null)
                .show();
    }

    private void startIncubation() {
        if (!validateInputs()) {
            return;
        }

        try {
            JSONObject params = new JSONObject();

            // Kuluçka tipi
            int selectedIndex = spinnerIncubationType.getSelectedItemPosition();
            params.put("type", INCUBATION_TYPES[selectedIndex]);
            params.put("totalDays", DEFAULT_DAYS[selectedIndex]);

            // Hedef değerler
            double targetTemp = Double.parseDouble(etTargetTemp.getText().toString());
            double targetHumid = Double.parseDouble(etTargetHumidity.getText().toString());
            params.put("targetTemp", targetTemp);
            params.put("targetHumid", targetHumid); // ESP32'de targetHumid kullanılıyor

            // Motor ayarları
            params.put("motorEnabled", switchMotorEnabled.isChecked());
            if (switchMotorEnabled.isChecked()) {
                int interval = Integer.parseInt(etTurnInterval.getText().toString());
                int duration = Integer.parseInt(etTurnDuration.getText().toString());
                params.put("turnInterval", interval);
                params.put("turnDuration", duration);
            }

            // ESP32'nin /api/incubation/start endpoint'ini kullan
            ApiService.getInstance().startIncubation(params, new ApiService.ParameterCallback() {
                @Override
                public void onSuccess() {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showToast("Kuluçka başarıyla başlatıldı");
                            loadCurrentSettings(); // UI'yi güncelle
                        });
                    }
                }

                @Override
                public void onError(String message) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showToast("Kuluçka başlatılamadı: " + message);
                        });
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Kuluçka başlatma hatası: " + e.getMessage());
            showToast("Parametre hatası: " + e.getMessage());
        }
    }

    private void stopIncubation() {
        // ESP32'nin /api/incubation/stop endpoint'ini kullan
        ApiService.getInstance().stopIncubation(new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Kuluçka durduruldu");
                        loadCurrentSettings(); // UI'yi güncelle
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Kuluçka durdurulamadı: " + message);
                    });
                }
            }
        });
    }

    private void saveSettings() {
        if (!validateInputs()) {
            return;
        }

        try {
            JSONObject params = new JSONObject();

            // Hedef değerler
            double targetTemp = Double.parseDouble(etTargetTemp.getText().toString());
            double targetHumid = Double.parseDouble(etTargetHumidity.getText().toString());
            params.put("targetTemp", targetTemp);
            params.put("targetHumid", targetHumid); // ESP32'de targetHumid kullanılıyor

            // Motor ayarları
            params.put("motorEnabled", switchMotorEnabled.isChecked());
            if (switchMotorEnabled.isChecked()) {
                int interval = Integer.parseInt(etTurnInterval.getText().toString());
                int duration = Integer.parseInt(etTurnDuration.getText().toString());
                params.put("turnInterval", interval);
                params.put("turnDuration", duration);
            }

            // ESP32'nin /api/settings endpoint'ini kullan
            ApiService.getInstance().updateSettings(params, new ApiService.ParameterCallback() {
                @Override
                public void onSuccess() {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showToast("Ayarlar kaydedildi");
                        });
                    }
                }

                @Override
                public void onError(String message) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showToast("Ayarlar kaydedilemedi: " + message);
                        });
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Ayar kaydetme hatası: " + e.getMessage());
            showToast("Parametre hatası: " + e.getMessage());
        }
    }

    private void resetToDefaults() {
        int selectedIndex = spinnerIncubationType.getSelectedItemPosition();
        if (selectedIndex >= 0 && selectedIndex < DEFAULT_TEMPS.length) {
            etTargetTemp.setText(String.format(Locale.getDefault(), "%.1f", DEFAULT_TEMPS[selectedIndex]));
            etTargetHumidity.setText(String.format(Locale.getDefault(), "%.1f", DEFAULT_HUMIDITIES[selectedIndex]));
        }

        switchMotorEnabled.setChecked(true);
        etTurnInterval.setText("120"); // 2 saatte bir
        etTurnDuration.setText("5"); // 5 saniye

        updateRangeDisplays();
        updateMotorControls(true);

        showToast("Varsayılan ayarlar yüklendi");
    }

    private boolean validateInputs() {
        // Sıcaklık kontrolü
        try {
            String tempStr = etTargetTemp.getText().toString();
            if (tempStr.isEmpty()) {
                etTargetTemp.setError("Sıcaklık boş olamaz");
                return false;
            }
            double temp = Double.parseDouble(tempStr);
            if (temp < 35.0 || temp > 40.0) {
                etTargetTemp.setError("Sıcaklık 35-40°C aralığında olmalı");
                return false;
            }
        } catch (NumberFormatException e) {
            etTargetTemp.setError("Geçerli bir sıcaklık girin");
            return false;
        }

        // Nem kontrolü
        try {
            String humidStr = etTargetHumidity.getText().toString();
            if (humidStr.isEmpty()) {
                etTargetHumidity.setError("Nem boş olamaz");
                return false;
            }
            double humidity = Double.parseDouble(humidStr);
            if (humidity < 40.0 || humidity > 80.0) {
                etTargetHumidity.setError("Nem %40-80 aralığında olmalı");
                return false;
            }
        } catch (NumberFormatException e) {
            etTargetHumidity.setError("Geçerli bir nem değeri girin");
            return false;
        }

        // Motor ayarları kontrolü
        if (switchMotorEnabled.isChecked()) {
            try {
                String intervalStr = etTurnInterval.getText().toString();
                if (intervalStr.isEmpty()) {
                    etTurnInterval.setError("Çevirme aralığı boş olamaz");
                    return false;
                }
                int interval = Integer.parseInt(intervalStr);
                if (interval < 30 || interval > 480) { // 30 dakika - 8 saat
                    etTurnInterval.setError("Aralık 30-480 dakika olmalı");
                    return false;
                }

                String durationStr = etTurnDuration.getText().toString();
                if (durationStr.isEmpty()) {
                    etTurnDuration.setError("Çevirme süresi boş olamaz");
                    return false;
                }
                int duration = Integer.parseInt(durationStr);
                if (duration < 1 || duration > 30) { // 1-30 saniye
                    etTurnDuration.setError("Süre 1-30 saniye olmalı");
                    return false;
                }
            } catch (NumberFormatException e) {
                showToast("Motor ayarlarında geçersiz değer");
                return false;
            }
        }

        return true;
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}