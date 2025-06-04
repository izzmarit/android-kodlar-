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
import android.widget.AdapterView;
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

public class MotorSettingsFragment extends Fragment {

    private static final String TAG = "MotorSettings";

    // UI Components - Motor Genel Ayarları
    private Switch switchMotorEnabled;
    private TextView tvMotorStatus;
    private TextView tvLastTurnTime;
    private TextView tvTotalTurns;
    private TextView tvMotorRuntime;

    // UI Components - Çevirme Ayarları
    private EditText etTurnInterval;
    private EditText etTurnDuration;
    private EditText etTurnSteps;
    private Spinner spinnerTurnDirection;
    private TextView tvTurnIntervalHelp;
    private TextView tvTurnDurationHelp;

    // UI Components - Motor Tipi ve Konfigürasyon
    private Spinner spinnerMotorType;
    private EditText etStepsPerRevolution;
    private EditText etMotorSpeed;
    private EditText etMotorAcceleration;
    private Switch switchMicrostep;
    private TextView tvMotorConfigHelp;

    // UI Components - Güvenlik ve Limitler
    private EditText etMaxDailyTurns;
    private EditText etMinTurnInterval;
    private Switch switchEmergencyStop;
    private Switch switchPositionFeedback;
    private TextView tvSafetyStatus;

    // UI Components - Manuel Kontrol
    private Button btnManualTurnCW;
    private Button btnManualTurnCCW;
    private Button btnStopMotor;
    private Button btnHomePosition;
    private Button btnCalibrate;
    private TextView tvCurrentPosition;
    private ProgressBar pbManualOperation;

    // UI Components - Test ve Konfigürasyon
    private Button btnTestMotor;
    private Button btnSaveMotorSettings;
    private Button btnLoadMotorSettings;
    private Button btnResetMotorDefaults;
    private Button btnMotorDiagnostics;

    // Veri yenileme
    private Handler refreshHandler;
    private boolean isFragmentActive = false;
    private static final long REFRESH_INTERVAL = 5000; // 5 saniye

    // Motor tipleri - ESP32'deki enum ile eşleşmeli
    private static final String[] MOTOR_TYPES = {
            "Servo Motor",
            "Stepper Motor",
            "DC Motor",
            "Servo + Stepper"
    };

    // Çevirme yönleri
    private static final String[] TURN_DIRECTIONS = {
            "Saat Yönü",
            "Saat Yönü Tersi",
            "Alternatif"
    };

    // Varsayılan değerler
    private static final int DEFAULT_TURN_INTERVAL = 120; // dakika
    private static final int DEFAULT_TURN_DURATION = 5; // saniye
    private static final int DEFAULT_TURN_STEPS = 180; // derece
    private static final int DEFAULT_STEPS_PER_REV = 200; // stepper için
    private static final int DEFAULT_MOTOR_SPEED = 50; // RPM
    private static final int DEFAULT_MAX_DAILY_TURNS = 12; // günde max çevirme
    private static final int DEFAULT_MIN_TURN_INTERVAL = 30; // minimum dakika

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_motor_settings, container, false);
        initializeViews(view);
        setupEventListeners();
        setupSpinners();
        setupRefreshHandler();
        loadCurrentMotorSettings();
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
        // Motor genel ayarları
        switchMotorEnabled = view.findViewById(R.id.switch_motor_enabled);
        tvMotorStatus = view.findViewById(R.id.tv_motor_status);
        tvLastTurnTime = view.findViewById(R.id.tv_last_turn_time);
        tvTotalTurns = view.findViewById(R.id.tv_total_turns);
        tvMotorRuntime = view.findViewById(R.id.tv_motor_runtime);

        // Çevirme ayarları
        etTurnInterval = view.findViewById(R.id.et_turn_interval);
        etTurnDuration = view.findViewById(R.id.et_turn_duration);
        etTurnSteps = view.findViewById(R.id.et_turn_steps);
        spinnerTurnDirection = view.findViewById(R.id.spinner_turn_direction);
        tvTurnIntervalHelp = view.findViewById(R.id.tv_turn_interval_help);
        tvTurnDurationHelp = view.findViewById(R.id.tv_turn_duration_help);

        // Motor tipi ve konfigürasyon
        spinnerMotorType = view.findViewById(R.id.spinner_motor_type);
        etStepsPerRevolution = view.findViewById(R.id.et_steps_per_revolution);
        etMotorSpeed = view.findViewById(R.id.et_motor_speed);
        etMotorAcceleration = view.findViewById(R.id.et_motor_acceleration);
        switchMicrostep = view.findViewById(R.id.switch_microstep);
        tvMotorConfigHelp = view.findViewById(R.id.tv_motor_config_help);

        // Güvenlik ve limitler
        etMaxDailyTurns = view.findViewById(R.id.et_max_daily_turns);
        etMinTurnInterval = view.findViewById(R.id.et_min_turn_interval);
        switchEmergencyStop = view.findViewById(R.id.switch_emergency_stop);
        switchPositionFeedback = view.findViewById(R.id.switch_position_feedback);
        tvSafetyStatus = view.findViewById(R.id.tv_safety_status);

        // Manuel kontrol
        btnManualTurnCW = view.findViewById(R.id.btn_manual_turn_cw);
        btnManualTurnCCW = view.findViewById(R.id.btn_manual_turn_ccw);
        btnStopMotor = view.findViewById(R.id.btn_stop_motor);
        btnHomePosition = view.findViewById(R.id.btn_home_position);
        btnCalibrate = view.findViewById(R.id.btn_calibrate);
        tvCurrentPosition = view.findViewById(R.id.tv_current_position);
        pbManualOperation = view.findViewById(R.id.pb_manual_operation);

        // Test ve konfigürasyon
        btnTestMotor = view.findViewById(R.id.btn_test_motor);
        btnSaveMotorSettings = view.findViewById(R.id.btn_save_motor_settings);
        btnLoadMotorSettings = view.findViewById(R.id.btn_load_motor_settings);
        btnResetMotorDefaults = view.findViewById(R.id.btn_reset_motor_defaults);
        btnMotorDiagnostics = view.findViewById(R.id.btn_motor_diagnostics);

        setDefaultValues();
    }

    private void setDefaultValues() {
        // Varsayılan değerleri ayarla
        if (etTurnInterval != null) etTurnInterval.setText(String.valueOf(DEFAULT_TURN_INTERVAL));
        if (etTurnDuration != null) etTurnDuration.setText(String.valueOf(DEFAULT_TURN_DURATION));
        if (etTurnSteps != null) etTurnSteps.setText(String.valueOf(DEFAULT_TURN_STEPS));
        if (etStepsPerRevolution != null) etStepsPerRevolution.setText(String.valueOf(DEFAULT_STEPS_PER_REV));
        if (etMotorSpeed != null) etMotorSpeed.setText(String.valueOf(DEFAULT_MOTOR_SPEED));
        if (etMotorAcceleration != null) etMotorAcceleration.setText("10");
        if (etMaxDailyTurns != null) etMaxDailyTurns.setText(String.valueOf(DEFAULT_MAX_DAILY_TURNS));
        if (etMinTurnInterval != null) etMinTurnInterval.setText(String.valueOf(DEFAULT_MIN_TURN_INTERVAL));

        // Durum bilgileri
        if (tvMotorStatus != null) tvMotorStatus.setText("Motor durumu kontrol ediliyor...");
        if (tvLastTurnTime != null) tvLastTurnTime.setText("Bilgi yükleniyor...");
        if (tvTotalTurns != null) tvTotalTurns.setText("0");
        if (tvMotorRuntime != null) tvMotorRuntime.setText("0 saat");
        if (tvCurrentPosition != null) tvCurrentPosition.setText("Bilinmiyor");
        if (tvSafetyStatus != null) tvSafetyStatus.setText("Güvenlik durumu kontrol ediliyor...");

        updateHelpTexts();
    }

    private void setupSpinners() {
        // Motor tipi spinner
        if (spinnerMotorType != null) {
            ArrayAdapter<String> motorTypeAdapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    MOTOR_TYPES
            );
            motorTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerMotorType.setAdapter(motorTypeAdapter);
            spinnerMotorType.setSelection(1); // Stepper motor varsayılan
        }

        // Çevirme yönü spinner
        if (spinnerTurnDirection != null) {
            ArrayAdapter<String> turnDirectionAdapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    TURN_DIRECTIONS
            );
            turnDirectionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerTurnDirection.setAdapter(turnDirectionAdapter);
            spinnerTurnDirection.setSelection(2); // Alternatif varsayılan
        }
    }

    private void setupEventListeners() {
        // Motor enable/disable
        if (switchMotorEnabled != null) {
            switchMotorEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
                updateMotorControlsState(isChecked);
            });
        }

        // Çevirme ayarları değişiklik listener'ları
        setupTextChangeListener(etTurnInterval, this::updateHelpTexts);
        setupTextChangeListener(etTurnDuration, this::updateHelpTexts);
        setupTextChangeListener(etTurnSteps, this::updateHelpTexts);

        // Motor tipi değişikliği
        if (spinnerMotorType != null) {
            spinnerMotorType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    updateMotorTypeConfiguration(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        // Microstep değişikliği
        if (switchMicrostep != null) {
            switchMicrostep.setOnCheckedChangeListener((buttonView, isChecked) -> {
                updateMicrostepConfiguration(isChecked);
            });
        }

        // Manuel kontrol butonları
        if (btnManualTurnCW != null) {
            btnManualTurnCW.setOnClickListener(v -> manualTurn("CW"));
        }
        if (btnManualTurnCCW != null) {
            btnManualTurnCCW.setOnClickListener(v -> manualTurn("CCW"));
        }
        if (btnStopMotor != null) {
            btnStopMotor.setOnClickListener(v -> stopMotor());
        }
        if (btnHomePosition != null) {
            btnHomePosition.setOnClickListener(v -> homePosition());
        }
        if (btnCalibrate != null) {
            btnCalibrate.setOnClickListener(v -> showCalibrateDialog());
        }

        // Test ve ayar butonları
        if (btnTestMotor != null) {
            btnTestMotor.setOnClickListener(v -> showTestDialog());
        }
        if (btnSaveMotorSettings != null) {
            btnSaveMotorSettings.setOnClickListener(v -> saveMotorSettings());
        }
        if (btnLoadMotorSettings != null) {
            btnLoadMotorSettings.setOnClickListener(v -> loadCurrentMotorSettings());
        }
        if (btnResetMotorDefaults != null) {
            btnResetMotorDefaults.setOnClickListener(v -> showResetDialog());
        }
        if (btnMotorDiagnostics != null) {
            btnMotorDiagnostics.setOnClickListener(v -> showDiagnosticsDialog());
        }
    }

    private void setupTextChangeListener(EditText editText, Runnable callback) {
        if (editText != null) {
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (callback != null) callback.run();
                }
            });
        }
    }

    private void updateMotorControlsState(boolean enabled) {
        // Motor kontrollerini etkinleştir/devre dışı bırak
        enableMotorControls(enabled);

        if (tvMotorStatus != null) {
            String status = enabled ? "Motor Aktif" : "Motor Devre Dışı";
            tvMotorStatus.setText(status);
            tvMotorStatus.setTextColor(getResources().getColor(
                    enabled ? android.R.color.holo_green_dark : android.R.color.holo_red_dark
            ));
        }
    }

    private void enableMotorControls(boolean enabled) {
        // Çevirme ayarları
        if (etTurnInterval != null) etTurnInterval.setEnabled(enabled);
        if (etTurnDuration != null) etTurnDuration.setEnabled(enabled);
        if (etTurnSteps != null) etTurnSteps.setEnabled(enabled);
        if (spinnerTurnDirection != null) spinnerTurnDirection.setEnabled(enabled);

        // Motor konfigürasyonu
        if (spinnerMotorType != null) spinnerMotorType.setEnabled(enabled);
        if (etStepsPerRevolution != null) etStepsPerRevolution.setEnabled(enabled);
        if (etMotorSpeed != null) etMotorSpeed.setEnabled(enabled);
        if (etMotorAcceleration != null) etMotorAcceleration.setEnabled(enabled);
        if (switchMicrostep != null) switchMicrostep.setEnabled(enabled);

        // Güvenlik ayarları
        if (etMaxDailyTurns != null) etMaxDailyTurns.setEnabled(enabled);
        if (etMinTurnInterval != null) etMinTurnInterval.setEnabled(enabled);

        // Manuel kontrol butonları
        if (btnManualTurnCW != null) btnManualTurnCW.setEnabled(enabled);
        if (btnManualTurnCCW != null) btnManualTurnCCW.setEnabled(enabled);
        if (btnHomePosition != null) btnHomePosition.setEnabled(enabled);
        if (btnCalibrate != null) btnCalibrate.setEnabled(enabled);
        if (btnTestMotor != null) btnTestMotor.setEnabled(enabled);
    }

    private void updateMotorTypeConfiguration(int motorTypeIndex) {
        String motorType = MOTOR_TYPES[motorTypeIndex];

        // Motor tipine göre ayarları güncelle
        boolean isStepperMotor = motorType.contains("Stepper");

        if (etStepsPerRevolution != null) {
            etStepsPerRevolution.setEnabled(isStepperMotor);
        }
        if (switchMicrostep != null) {
            switchMicrostep.setEnabled(isStepperMotor);
        }

        // Yardım metni güncelle
        if (tvMotorConfigHelp != null) {
            String helpText = getMotorConfigHelpText(motorType);
            tvMotorConfigHelp.setText(helpText);
        }
    }

    private String getMotorConfigHelpText(String motorType) {
        switch (motorType) {
            case "Servo Motor":
                return "Servo motor: 0-180° veya 0-360° hareket. Yüksek hassasiyet.";
            case "Stepper Motor":
                return "Stepper motor: Adım adım hareket. Hassas pozisyon kontrolü.";
            case "DC Motor":
                return "DC motor: Sürekli dönme. Encoder ile pozisyon kontrolü.";
            case "Servo + Stepper":
                return "Hibrit sistem: Servo ile hassas pozisyon, stepper ile güçlü hareket.";
            default:
                return "Motor tipi seçin";
        }
    }

    private void updateMicrostepConfiguration(boolean enabled) {
        if (tvMotorConfigHelp != null) {
            String currentText = tvMotorConfigHelp.getText().toString();
            String microstepInfo = enabled ? " Microstep aktif." : " Microstep devre dışı.";
            if (!currentText.contains("Microstep")) {
                tvMotorConfigHelp.setText(currentText + microstepInfo);
            }
        }
    }

    private void updateHelpTexts() {
        // Çevirme aralığı yardım metni
        if (tvTurnIntervalHelp != null && etTurnInterval != null) {
            try {
                int interval = Integer.parseInt(etTurnInterval.getText().toString());
                int turnsPerDay = 24 * 60 / interval;
                tvTurnIntervalHelp.setText(String.format(Locale.getDefault(),
                        "Günde yaklaşık %d kez çevrilecek", turnsPerDay));
            } catch (NumberFormatException e) {
                tvTurnIntervalHelp.setText("Geçerli bir aralık girin");
            }
        }

        // Çevirme süresi yardım metni
        if (tvTurnDurationHelp != null && etTurnDuration != null) {
            try {
                int duration = Integer.parseInt(etTurnDuration.getText().toString());
                tvTurnDurationHelp.setText(String.format(Locale.getDefault(),
                        "Motor %d saniye çalışacak", duration));
            } catch (NumberFormatException e) {
                tvTurnDurationHelp.setText("Geçerli bir süre girin");
            }
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
                loadCurrentMotorSettings();
                refreshHandler.postDelayed(this, REFRESH_INTERVAL);
            }
        }
    };

    private void loadCurrentMotorSettings() {
        // ESP32'den güncel motor ayarlarını al
        ApiService.getInstance().getMotorSettings(new ApiService.DataCallback() {
            @Override
            public void onSuccess(JSONObject data) {
                if (isFragmentActive && getActivity() != null) {
                    getActivity().runOnUiThread(() -> updateUIWithMotorData(data));
                }
            }

            @Override
            public void onError(String message) {
                if (isFragmentActive && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Log.w(TAG, "Motor ayarları yüklenemedi: " + message);
                    });
                }
            }
        });
    }

    private void updateUIWithMotorData(JSONObject data) {
        try {
            // Motor genel durumu
            boolean motorEnabled = data.optBoolean("motorEnabled", true);
            boolean motorRunning = data.optBoolean("motorRunning", false);
            long lastTurnTime = data.optLong("lastTurnTime", 0);
            int totalTurns = data.optInt("totalTurns", 0);
            long totalRuntime = data.optLong("totalRuntime", 0);

            // Çevirme ayarları
            int turnInterval = data.optInt("turnInterval", DEFAULT_TURN_INTERVAL);
            int turnDuration = data.optInt("turnDuration", DEFAULT_TURN_DURATION);
            int turnSteps = data.optInt("turnSteps", DEFAULT_TURN_STEPS);
            String turnDirection = data.optString("turnDirection", "Alternatif");

            // Motor konfigürasyonu
            String motorType = data.optString("motorType", "Stepper Motor");
            int stepsPerRevolution = data.optInt("stepsPerRevolution", DEFAULT_STEPS_PER_REV);
            int motorSpeed = data.optInt("motorSpeed", DEFAULT_MOTOR_SPEED);
            int motorAcceleration = data.optInt("motorAcceleration", 10);
            boolean microstep = data.optBoolean("microstep", false);

            // Güvenlik ayarları
            int maxDailyTurns = data.optInt("maxDailyTurns", DEFAULT_MAX_DAILY_TURNS);
            int minTurnInterval = data.optInt("minTurnInterval", DEFAULT_MIN_TURN_INTERVAL);
            boolean emergencyStop = data.optBoolean("emergencyStop", false);
            boolean positionFeedback = data.optBoolean("positionFeedback", false);

            // Pozisyon bilgisi
            int currentPosition = data.optInt("currentPosition", 0);
            String safetyStatus = data.optString("safetyStatus", "Normal");

            // UI'yi güncelle
            updateGeneralMotorStatus(motorEnabled, motorRunning, lastTurnTime, totalTurns, totalRuntime);
            updateTurnSettings(turnInterval, turnDuration, turnSteps, turnDirection);
            updateMotorConfiguration(motorType, stepsPerRevolution, motorSpeed, motorAcceleration, microstep);
            updateSafetySettings(maxDailyTurns, minTurnInterval, emergencyStop, positionFeedback);
            updatePositionInfo(currentPosition, safetyStatus);

        } catch (Exception e) {
            Log.e(TAG, "Motor verileri işleme hatası: " + e.getMessage());
        }
    }

    private void updateGeneralMotorStatus(boolean enabled, boolean running, long lastTurnTime, int totalTurns, long totalRuntime) {
        if (switchMotorEnabled != null) {
            switchMotorEnabled.setChecked(enabled);
        }

        updateMotorControlsState(enabled);

        // Motor durumu
        if (tvMotorStatus != null) {
            String status = "";
            if (!enabled) {
                status = "Motor Devre Dışı";
            } else if (running) {
                status = "Motor Çalışıyor";
            } else {
                status = "Motor Beklemede";
            }
            tvMotorStatus.setText(status);
        }

        // Son çevirme zamanı
        if (tvLastTurnTime != null && lastTurnTime > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            tvLastTurnTime.setText("Son çevirme: " + sdf.format(new Date(lastTurnTime)));
        } else if (tvLastTurnTime != null) {
            tvLastTurnTime.setText("Henüz çevirme yapılmamış");
        }

        // Toplam çevirme sayısı
        if (tvTotalTurns != null) {
            tvTotalTurns.setText("Toplam çevirme: " + totalTurns);
        }

        // Toplam çalışma süresi
        if (tvMotorRuntime != null) {
            long hours = totalRuntime / 3600000; // millisaniye'den saate
            long minutes = (totalRuntime % 3600000) / 60000;
            tvMotorRuntime.setText(String.format(Locale.getDefault(),
                    "Çalışma süresi: %d saat %d dakika", hours, minutes));
        }
    }

    private void updateTurnSettings(int interval, int duration, int steps, String direction) {
        if (etTurnInterval != null) {
            etTurnInterval.setText(String.valueOf(interval));
        }
        if (etTurnDuration != null) {
            etTurnDuration.setText(String.valueOf(duration));
        }
        if (etTurnSteps != null) {
            etTurnSteps.setText(String.valueOf(steps));
        }

        // Çevirme yönü spinner'ını güncelle
        if (spinnerTurnDirection != null) {
            for (int i = 0; i < TURN_DIRECTIONS.length; i++) {
                if (TURN_DIRECTIONS[i].equals(direction)) {
                    spinnerTurnDirection.setSelection(i);
                    break;
                }
            }
        }

        updateHelpTexts();
    }

    private void updateMotorConfiguration(String type, int stepsPerRev, int speed, int acceleration, boolean microstep) {
        // Motor tipi spinner'ını güncelle
        if (spinnerMotorType != null) {
            for (int i = 0; i < MOTOR_TYPES.length; i++) {
                if (MOTOR_TYPES[i].equals(type)) {
                    spinnerMotorType.setSelection(i);
                    updateMotorTypeConfiguration(i);
                    break;
                }
            }
        }

        if (etStepsPerRevolution != null) {
            etStepsPerRevolution.setText(String.valueOf(stepsPerRev));
        }
        if (etMotorSpeed != null) {
            etMotorSpeed.setText(String.valueOf(speed));
        }
        if (etMotorAcceleration != null) {
            etMotorAcceleration.setText(String.valueOf(acceleration));
        }
        if (switchMicrostep != null) {
            switchMicrostep.setChecked(microstep);
            updateMicrostepConfiguration(microstep);
        }
    }

    private void updateSafetySettings(int maxDaily, int minInterval, boolean emergencyStop, boolean positionFeedback) {
        if (etMaxDailyTurns != null) {
            etMaxDailyTurns.setText(String.valueOf(maxDaily));
        }
        if (etMinTurnInterval != null) {
            etMinTurnInterval.setText(String.valueOf(minInterval));
        }
        if (switchEmergencyStop != null) {
            switchEmergencyStop.setChecked(emergencyStop);
        }
        if (switchPositionFeedback != null) {
            switchPositionFeedback.setChecked(positionFeedback);
        }
    }

    private void updatePositionInfo(int position, String safetyStatus) {
        if (tvCurrentPosition != null) {
            tvCurrentPosition.setText("Pozisyon: " + position + "°");
        }
        if (tvSafetyStatus != null) {
            tvSafetyStatus.setText("Güvenlik: " + safetyStatus);
            tvSafetyStatus.setTextColor(getResources().getColor(
                    safetyStatus.equals("Normal") ? android.R.color.holo_green_dark : android.R.color.holo_red_dark
            ));
        }
    }

    private void manualTurn(String direction) {
        if (pbManualOperation != null) {
            pbManualOperation.setVisibility(View.VISIBLE);
        }

        try {
            JSONObject params = new JSONObject();
            params.put("direction", direction);
            params.put("steps", Integer.parseInt(etTurnSteps.getText().toString()));
            params.put("speed", Integer.parseInt(etMotorSpeed.getText().toString()));

            // ESP32'ye manuel çevirme komutu gönder
            ApiService.getInstance().manualTurnMotor(params, new ApiService.ParameterCallback() {
                @Override
                public void onSuccess() {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showToast("Manuel çevirme başlatıldı");
                            hideManualOperationProgress();
                        });
                    }
                }

                @Override
                public void onError(String message) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showToast("Manuel çevirme hatası: " + message);
                            hideManualOperationProgress();
                        });
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Manuel çevirme hatası: " + e.getMessage());
            showToast("Parametre hatası: " + e.getMessage());
            hideManualOperationProgress();
        }
    }

    private void stopMotor() {
        // ESP32'ye motor durdurma komutu gönder
        ApiService.getInstance().stopMotor(new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Motor durduruldu");
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Motor durdurulamadı: " + message);
                    });
                }
            }
        });
    }

    private void homePosition() {
        if (pbManualOperation != null) {
            pbManualOperation.setVisibility(View.VISIBLE);
        }

        // ESP32'ye home pozisyonu komutu gönder
        ApiService.getInstance().homeMotor(new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Motor ana pozisyona döndü");
                        hideManualOperationProgress();
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Ana pozisyona dönüş hatası: " + message);
                        hideManualOperationProgress();
                    });
                }
            }
        });
    }

    private void hideManualOperationProgress() {
        if (pbManualOperation != null) {
            pbManualOperation.setVisibility(View.GONE);
        }
    }

    private void showCalibrateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Motor Kalibrasyonu")
                .setMessage("Motor kalibrasyon işlemi motorun referans pozisyonunu belirler.\n\n" +
                        "Bu işlem 2-5 dakika sürebilir. Devam etmek istediğinizden emin misiniz?")
                .setPositiveButton("Kalibre Et", (dialog, which) -> calibrateMotor())
                .setNegativeButton("İptal", null)
                .show();
    }

    private void calibrateMotor() {
        if (pbManualOperation != null) {
            pbManualOperation.setVisibility(View.VISIBLE);
        }

        // ESP32'ye kalibrasyon komutu gönder
        ApiService.getInstance().calibrateMotor(new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Motor kalibrasyonu başlatıldı");
                        hideManualOperationProgress();
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Kalibrasyon hatası: " + message);
                        hideManualOperationProgress();
                    });
                }
            }
        });
    }

    private void showTestDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Motor Testi")
                .setMessage("Motor test işlemi çeşitli hareketler yaparak motorun düzgün çalışıp çalışmadığını kontrol eder.\n\n" +
                        "Test yapmak istediğinizden emin misiniz?")
                .setPositiveButton("Test Et", (dialog, which) -> testMotor())
                .setNegativeButton("İptal", null)
                .show();
    }

    private void testMotor() {
        // ESP32'ye motor test komutu gönder
        ApiService.getInstance().testMotor(new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Motor testi başlatıldı - 3 dakika sürecek");
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Motor testi başlatılamadı: " + message);
                    });
                }
            }
        });
    }

    private void saveMotorSettings() {
        if (!validateMotorInputs()) {
            return;
        }

        try {
            JSONObject params = new JSONObject();

            // Genel ayarlar
            params.put("motorEnabled", switchMotorEnabled.isChecked());

            // Çevirme ayarları
            params.put("turnInterval", Integer.parseInt(etTurnInterval.getText().toString()));
            params.put("turnDuration", Integer.parseInt(etTurnDuration.getText().toString()));
            params.put("turnSteps", Integer.parseInt(etTurnSteps.getText().toString()));
            params.put("turnDirection", TURN_DIRECTIONS[spinnerTurnDirection.getSelectedItemPosition()]);

            // Motor konfigürasyonu
            params.put("motorType", MOTOR_TYPES[spinnerMotorType.getSelectedItemPosition()]);
            params.put("stepsPerRevolution", Integer.parseInt(etStepsPerRevolution.getText().toString()));
            params.put("motorSpeed", Integer.parseInt(etMotorSpeed.getText().toString()));
            params.put("motorAcceleration", Integer.parseInt(etMotorAcceleration.getText().toString()));
            params.put("microstep", switchMicrostep.isChecked());

            // Güvenlik ayarları
            params.put("maxDailyTurns", Integer.parseInt(etMaxDailyTurns.getText().toString()));
            params.put("minTurnInterval", Integer.parseInt(etMinTurnInterval.getText().toString()));
            params.put("emergencyStop", switchEmergencyStop.isChecked());
            params.put("positionFeedback", switchPositionFeedback.isChecked());

            // ESP32'ye motor ayarlarını gönder
            ApiService.getInstance().updateMotorSettings(params, new ApiService.ParameterCallback() {
                @Override
                public void onSuccess() {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showToast("Motor ayarları kaydedildi");
                        });
                    }
                }

                @Override
                public void onError(String message) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showToast("Motor ayarları kaydedilemedi: " + message);
                        });
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Motor ayarları kaydetme hatası: " + e.getMessage());
            showToast("Parametre hatası: " + e.getMessage());
        }
    }

    private void showResetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Varsayılan Motor Ayarları")
                .setMessage("Motor ayarlarını varsayılan değerlere sıfırlamak istediğinizden emin misiniz?")
                .setPositiveButton("Sıfırla", (dialog, which) -> resetToDefaults())
                .setNegativeButton("İptal", null)
                .show();
    }

    private void resetToDefaults() {
        setDefaultValues();
        setupSpinners();
        switchMotorEnabled.setChecked(true);
        switchMicrostep.setChecked(false);
        switchEmergencyStop.setChecked(false);
        switchPositionFeedback.setChecked(false);
        updateMotorControlsState(true);
        showToast("Varsayılan motor ayarları yüklendi");
    }

    private void showDiagnosticsDialog() {
        // Motor diagnostik bilgilerini göster
        ApiService.getInstance().getMotorDiagnostics(new ApiService.DataCallback() {
            @Override
            public void onSuccess(JSONObject data) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> displayDiagnostics(data));
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Diagnostik bilgisi alınamadı: " + message);
                    });
                }
            }
        });
    }

    private void displayDiagnostics(JSONObject data) {
        try {
            String diagnosticInfo = data.optString("diagnostics", "Diagnostik bilgisi mevcut değil");

            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Motor Diagnostik Bilgisi")
                    .setMessage(diagnosticInfo)
                    .setPositiveButton("Tamam", null)
                    .show();
        } catch (Exception e) {
            showToast("Diagnostik bilgisi işlenemedi");
        }
    }

    private boolean validateMotorInputs() {
        // Çevirme ayarları
        if (!validateIntegerField(etTurnInterval, "Çevirme Aralığı", 30, 1440)) return false; // 30 dakika - 24 saat
        if (!validateIntegerField(etTurnDuration, "Çevirme Süresi", 1, 60)) return false; // 1-60 saniye
        if (!validateIntegerField(etTurnSteps, "Çevirme Adımı", 10, 360)) return false; // 10-360 derece

        // Motor konfigürasyonu
        if (!validateIntegerField(etStepsPerRevolution, "Devir Başına Adım", 50, 400)) return false;
        if (!validateIntegerField(etMotorSpeed, "Motor Hızı", 1, 200)) return false; // 1-200 RPM
        if (!validateIntegerField(etMotorAcceleration, "Motor İvmesi", 1, 100)) return false;

        // Güvenlik ayarları
        if (!validateIntegerField(etMaxDailyTurns, "Maksimum Günlük Çevirme", 1, 50)) return false;
        if (!validateIntegerField(etMinTurnInterval, "Minimum Çevirme Aralığı", 10, 180)) return false;

        return true;
    }

    private boolean validateIntegerField(EditText editText, String fieldName, int min, int max) {
        try {
            String valueStr = editText.getText().toString().trim();
            if (valueStr.isEmpty()) {
                editText.setError(fieldName + " boş olamaz");
                return false;
            }
            int value = Integer.parseInt(valueStr);
            if (value < min || value > max) {
                editText.setError(fieldName + " " + min + "-" + max + " aralığında olmalı");
                return false;
            }
            editText.setError(null);
            return true;
        } catch (NumberFormatException e) {
            editText.setError("Geçerli bir tam sayı girin");
            return false;
        }
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}