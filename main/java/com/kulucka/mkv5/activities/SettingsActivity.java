package com.kulucka.mkv5.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.kulucka.mkv5.R;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {

    private LinearLayout layoutWifiSettings;
    private LinearLayout layoutIncubationSettings;
    private LinearLayout layoutTempHumidSettings;
    private LinearLayout layoutCalibrationSettings;
    private LinearLayout layoutPidSettings;
    private LinearLayout layoutMotorSettings;
    private LinearLayout layoutAlarmSettings;
    private LinearLayout layoutRTCSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setupToolbar();
        initializeViews();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Ayarlar");
        }
    }

    private void initializeViews() {
        layoutWifiSettings = findViewById(R.id.layoutWifiSettings);
        layoutIncubationSettings = findViewById(R.id.layoutIncubationSettings);
        layoutTempHumidSettings = findViewById(R.id.layoutTempHumidSettings);
        layoutCalibrationSettings = findViewById(R.id.layoutCalibrationSettings);
        layoutPidSettings = findViewById(R.id.layoutPidSettings);
        layoutMotorSettings = findViewById(R.id.layoutMotorSettings);
        layoutAlarmSettings = findViewById(R.id.layoutAlarmSettings);
        layoutRTCSettings = findViewById(R.id.layoutRTCSettings);

        layoutWifiSettings.setOnClickListener(this);
        layoutIncubationSettings.setOnClickListener(this);
        layoutTempHumidSettings.setOnClickListener(this);
        layoutCalibrationSettings.setOnClickListener(this);
        layoutPidSettings.setOnClickListener(this);
        layoutMotorSettings.setOnClickListener(this);
        layoutAlarmSettings.setOnClickListener(this);
        layoutRTCSettings.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent = null;

        int id = v.getId();
        if (id == R.id.layoutWifiSettings) {
            intent = new Intent(this, WifiSettingsActivity.class);
        } else if (id == R.id.layoutIncubationSettings) {
            intent = new Intent(this, IncubationSettingsActivity.class);
        } else if (id == R.id.layoutTempHumidSettings) {
            intent = new Intent(this, TemperatureHumiditySettingsActivity.class);
        } else if (id == R.id.layoutCalibrationSettings) {
            intent = new Intent(this, CalibrationSettingsActivity.class);
        } else if (id == R.id.layoutPidSettings) {
            intent = new Intent(this, PidSettingsActivity.class);
        } else if (id == R.id.layoutMotorSettings) {
            intent = new Intent(this, MotorSettingsActivity.class);
        } else if (id == R.id.layoutAlarmSettings) {
            intent = new Intent(this, AlarmSettingsActivity.class);
        }else if (id == R.id.layoutRTCSettings) {
            intent = new Intent(this, RTCSettingsActivity.class);
        }

        if (intent != null) {
            startActivityForResult(intent, 1);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            // Alt aktiviteden değişiklik geldi, üst aktiviteye ilet
            setResult(RESULT_OK);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}