package com.kulucka.mk.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.kulucka.mk.R;
import com.kulucka.mk.utils.SharedPreferencesManager;

/**
 * Ayarlar ana menü aktivitesi
 * Tüm ayar kategorilerine erişim sağlar
 */
public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "SettingsActivity";

    // UI Components
    private Toolbar toolbar;

    // Settings Cards
    private CardView cardWiFiSettings;
    private CardView cardIncubationSettings;
    private CardView cardTempHumidSettings;
    private CardView cardCalibrationSettings;
    private CardView cardPIDSettings;
    private CardView cardMotorSettings;
    private CardView cardAlarmSettings;

    // Managers
    private SharedPreferencesManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize managers
        prefsManager = SharedPreferencesManager.getInstance(this);

        // Setup UI
        initializeUI();
        setupToolbar();
        setupClickListeners();
    }

    /**
     * UI bileşenlerini başlatır
     */
    private void initializeUI() {
        // Toolbar
        toolbar = findViewById(R.id.toolbar);

        // Settings cards
        cardWiFiSettings = findViewById(R.id.cardWiFiSettings);
        cardIncubationSettings = findViewById(R.id.cardIncubationSettings);
        cardTempHumidSettings = findViewById(R.id.cardTempHumidSettings);
        cardCalibrationSettings = findViewById(R.id.cardCalibrationSettings);
        cardPIDSettings = findViewById(R.id.cardPIDSettings);
        cardMotorSettings = findViewById(R.id.cardMotorSettings);
        cardAlarmSettings = findViewById(R.id.cardAlarmSettings);
    }

    /**
     * Toolbar'ı ayarlar
     */
    private void setupToolbar() {
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.settings_title);
        }
    }

    /**
     * Click listener'ları ayarlar
     */
    private void setupClickListeners() {
        cardWiFiSettings.setOnClickListener(this);
        cardIncubationSettings.setOnClickListener(this);
        cardTempHumidSettings.setOnClickListener(this);
        cardCalibrationSettings.setOnClickListener(this);
        cardPIDSettings.setOnClickListener(this);
        cardMotorSettings.setOnClickListener(this);
        cardAlarmSettings.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        if (id == R.id.cardWiFiSettings) {
            openWiFiSettings();
        } else if (id == R.id.cardIncubationSettings) {
            openIncubationSettings();
        } else if (id == R.id.cardTempHumidSettings) {
            openTemperatureHumiditySettings();
        } else if (id == R.id.cardCalibrationSettings) {
            openCalibrationSettings();
        } else if (id == R.id.cardPIDSettings) {
            openPIDSettings();
        } else if (id == R.id.cardMotorSettings) {
            openMotorSettings();
        } else if (id == R.id.cardAlarmSettings) {
            openAlarmSettings();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * WiFi ayarlarını açar
     */
    private void openWiFiSettings() {
        Intent intent = new Intent(this, WiFiSettingsActivity.class);
        startActivity(intent);
    }

    /**
     * Kuluçka ayarlarını açar
     */
    private void openIncubationSettings() {
        Intent intent = new Intent(this, IncubationSettingsActivity.class);
        startActivity(intent);
    }

    /**
     * Sıcaklık ve nem ayarlarını açar
     */
    private void openTemperatureHumiditySettings() {
        Intent intent = new Intent(this, TemperatureHumidityActivity.class);
        startActivity(intent);
    }

    /**
     * Kalibrasyon ayarlarını açar
     */
    private void openCalibrationSettings() {
        Intent intent = new Intent(this, CalibrationActivity.class);
        startActivity(intent);
    }

    /**
     * PID ayarlarını açar
     */
    private void openPIDSettings() {
        Intent intent = new Intent(this, PIDSettingsActivity.class);
        startActivity(intent);
    }

    /**
     * Motor ayarlarını açar
     */
    private void openMotorSettings() {
        Intent intent = new Intent(this, MotorSettingsActivity.class);
        startActivity(intent);
    }

    /**
     * Alarm ayarlarını açar
     */
    private void openAlarmSettings() {
        Intent intent = new Intent(this, AlarmSettingsActivity.class);
        startActivity(intent);
    }
}