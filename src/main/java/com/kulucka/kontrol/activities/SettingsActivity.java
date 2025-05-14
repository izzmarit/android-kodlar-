package com.kulucka.kontrol.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.kulucka.kontrol.R;
import com.kulucka.kontrol.models.AlarmData;
import com.kulucka.kontrol.services.ConnectionService;

import org.json.JSONException;
import org.json.JSONObject;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";

    // PID ayarları
    private EditText etKp;
    private EditText etKi;
    private EditText etKd;
    private Button btnSavePid;

    // Alarm eşikleri
    private SeekBar sbHighTemp;
    private SeekBar sbLowTemp;
    private SeekBar sbHighHum;
    private SeekBar sbLowHum;
    private TextView tvHighTempValue;
    private TextView tvLowTempValue;
    private TextView tvHighHumValue;
    private TextView tvLowHumValue;
    private EditText etTempDiff;
    private EditText etHumDiff;
    private Switch switchAlarmEnabled;
    private Button btnSaveAlarm;

    // Motor ayarları
    private EditText etMotorDuration;
    private EditText etMotorInterval;
    private Button btnSaveMotor;

    // WiFi ayarları
    private EditText etWifiSsid;
    private EditText etWifiPassword;
    private Button btnConnectWifi;
    private Button btnStartAp;

    // Veri modellerini geçici depolamak için
    private AlarmData.Thresholds alarmThresholds;

    // Servis referansı
    private ConnectionService connectionService;
    private boolean bound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Geri butonu ekle
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Ayarlar");
        }

        // Görünüm elemanlarını tanımla
        initViews();

        // Olayları ayarla
        setupListeners();

        // Varsayılan değerleri yükle
        loadDefaultValues();
    }

    private void initViews() {
        // PID ayarları
        etKp = findViewById(R.id.etKp);
        etKi = findViewById(R.id.etKi);
        etKd = findViewById(R.id.etKd);
        btnSavePid = findViewById(R.id.btnSavePid);

        // Alarm eşikleri
        sbHighTemp = findViewById(R.id.sbHighTemp);
        sbLowTemp = findViewById(R.id.sbLowTemp);
        sbHighHum = findViewById(R.id.sbHighHum);
        sbLowHum = findViewById(R.id.sbLowHum);
        tvHighTempValue = findViewById(R.id.tvHighTempValue);
        tvLowTempValue = findViewById(R.id.tvLowTempValue);
        tvHighHumValue = findViewById(R.id.tvHighHumValue);
        tvLowHumValue = findViewById(R.id.tvLowHumValue);
        etTempDiff = findViewById(R.id.etTempDiff);
        etHumDiff = findViewById(R.id.etHumDiff);
        switchAlarmEnabled = findViewById(R.id.switchAlarmEnabled);
        btnSaveAlarm = findViewById(R.id.btnSaveAlarm);

        // Motor ayarları
        etMotorDuration = findViewById(R.id.etMotorDuration);
        etMotorInterval = findViewById(R.id.etMotorInterval);
        btnSaveMotor = findViewById(R.id.btnSaveMotor);

        // WiFi ayarları
        etWifiSsid = findViewById(R.id.etWifiSsid);
        etWifiPassword = findViewById(R.id.etWifiPassword);
        btnConnectWifi = findViewById(R.id.btnConnectWifi);
        btnStartAp = findViewById(R.id.btnStartAp);

        // Slider aralıkları
        sbHighTemp.setMax(100); // 30-45°C aralığı (0.15°C adımlar)
        sbLowTemp.setMax(100);  // 30-45°C aralığı (0.15°C adımlar)
        sbHighHum.setMax(100);  // 0-100% aralığı (1% adımlar)
        sbLowHum.setMax(100);   // 0-100% aralığı (1% adımlar)
    }

    private void setupListeners() {
        // Yüksek sıcaklık eşiği değişimi
        sbHighTemp.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float temp = 30.0f + (progress * 0.15f); // 30-45°C aralığı
                tvHighTempValue.setText(String.format("%.1f°C", temp));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Düşük sıcaklık eşiği değişimi
        sbLowTemp.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float temp = 30.0f + (progress * 0.15f); // 30-45°C aralığı
                tvLowTempValue.setText(String.format("%.1f°C", temp));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Yüksek nem eşiği değişimi
        sbHighHum.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvHighHumValue.setText(String.format("%%%d", progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Düşük nem eşiği değişimi
        sbLowHum.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvLowHumValue.setText(String.format("%%%d", progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // PID ayarlarını kaydetme butonu
        btnSavePid.setOnClickListener(v -> {
            savePidSettings();
        });

        // Alarm eşiklerini kaydetme butonu
        btnSaveAlarm.setOnClickListener(v -> {
            saveAlarmThresholds();
        });

        // Motor ayarlarını kaydetme butonu
        btnSaveMotor.setOnClickListener(v -> {
            saveMotorSettings();
        });

        // WiFi'a bağlanma butonu
        btnConnectWifi.setOnClickListener(v -> {
            connectToWifi();
        });

        // AP modu başlatma butonu
        btnStartAp.setOnClickListener(v -> {
            startApMode();
        });
    }

    private void loadDefaultValues() {
        // PID varsayılan değerleri
        etKp.setText("10.0");
        etKi.setText("0.01");
        etKd.setText("5.0");

        // Alarm eşikleri varsayılan değerleri
        sbHighTemp.setProgress(57); // 38.5°C
        sbLowTemp.setProgress(43);  // 36.5°C
        sbHighHum.setProgress(80);  // 80%
        sbLowHum.setProgress(50);   // 50%
        etTempDiff.setText("2.0");
        etHumDiff.setText("10.0");
        switchAlarmEnabled.setChecked(true);

        // Motor ayarları varsayılan değerleri
        etMotorDuration.setText("14");  // 14 saniye
        etMotorInterval.setText("120"); // 120 dakika (2 saat)
    }

    private void savePidSettings() {
        try {
            float kp = Float.parseFloat(etKp.getText().toString());
            float ki = Float.parseFloat(etKi.getText().toString());
            float kd = Float.parseFloat(etKd.getText().toString());

            // Değerleri sınırlandır
            if (kp < 0 || ki < 0 || kd < 0) {
                Toast.makeText(this, "PID değerleri negatif olamaz!", Toast.LENGTH_SHORT).show();
                return;
            }

            // JSON oluştur
            JSONObject pidJson = new JSONObject();
            pidJson.put("cmd", "update_settings");
            pidJson.put("pid_kp", kp);
            pidJson.put("pid_ki", ki);
            pidJson.put("pid_kd", kd);

            // Burada SocketManager üzerinden veri gönderilmesi gerekiyor
            // socketManager.sendMessage(pidJson.toString());

            // Bildirim
            Toast.makeText(this, "PID ayarları kaydedildi", Toast.LENGTH_SHORT).show();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Geçersiz sayı formatı!", Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            Toast.makeText(this, "JSON hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveAlarmThresholds() {
        try {
            // Eşik değerlerini al
            float highTemp = 30.0f + (sbHighTemp.getProgress() * 0.15f);
            float lowTemp = 30.0f + (sbLowTemp.getProgress() * 0.15f);
            float highHum = sbHighHum.getProgress();
            float lowHum = sbLowHum.getProgress();
            float tempDiff = Float.parseFloat(etTempDiff.getText().toString());
            float humDiff = Float.parseFloat(etHumDiff.getText().toString());
            boolean enabled = switchAlarmEnabled.isChecked();

            // Değerleri sınırlandır ve kontrol et
            if (highTemp <= lowTemp) {
                Toast.makeText(this, "Yüksek sıcaklık düşük sıcaklıktan büyük olmalı!",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (highHum <= lowHum) {
                Toast.makeText(this, "Yüksek nem düşük nemden büyük olmalı!",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (tempDiff < 0 || humDiff < 0) {
                Toast.makeText(this, "Fark değerleri negatif olamaz!",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // JSON oluştur
            JSONObject settingsJson = new JSONObject();
            settingsJson.put("cmd", "update_settings");

            JSONObject alarmJson = new JSONObject();
            alarmJson.put("enabled", enabled);
            alarmJson.put("high_temp", highTemp);
            alarmJson.put("low_temp", lowTemp);
            alarmJson.put("high_hum", highHum);
            alarmJson.put("low_hum", lowHum);
            alarmJson.put("temp_diff", tempDiff);
            alarmJson.put("hum_diff", humDiff);

            settingsJson.put("alarm", alarmJson);

            // Burada SocketManager üzerinden veri gönderilmesi gerekiyor
            // socketManager.sendMessage(settingsJson.toString());

            // Bildirim
            Toast.makeText(this, "Alarm eşikleri kaydedildi", Toast.LENGTH_SHORT).show();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Geçersiz sayı formatı!", Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            Toast.makeText(this, "JSON hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveMotorSettings() {
        try {
            long duration = Long.parseLong(etMotorDuration.getText().toString()) * 1000; // ms'ye çevir
            long interval = Long.parseLong(etMotorInterval.getText().toString()) * 60 * 1000; // ms'ye çevir

            // Değerleri sınırlandır
            if (duration < 1000 || duration > 60000) {
                Toast.makeText(this, "Motor süresi 1-60 saniye aralığında olmalıdır!",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (interval < 600000 || interval > 86400000) {
                Toast.makeText(this, "Motor aralığı 10-1440 dakika aralığında olmalıdır!",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // JSON oluştur
            JSONObject motorJson = new JSONObject();
            motorJson.put("cmd", "set_motor");
            motorJson.put("duration", duration);
            motorJson.put("interval", interval);

            // Burada SocketManager üzerinden veri gönderilmesi gerekiyor
            // socketManager.sendMessage(motorJson.toString());

            // Bildirim
            Toast.makeText(this, "Motor ayarları kaydedildi", Toast.LENGTH_SHORT).show();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Geçersiz sayı formatı!", Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            Toast.makeText(this, "JSON hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void connectToWifi() {
        String ssid = etWifiSsid.getText().toString().trim();
        String password = etWifiPassword.getText().toString().trim();

        if (ssid.isEmpty()) {
            Toast.makeText(this, "SSID boş olamaz!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Onay dialog'u göster
        new AlertDialog.Builder(this)
                .setTitle("WiFi Bağlantısı")
                .setMessage("ESP32'yi " + ssid + " ağına bağlamak istediğinizden emin misiniz?")
                .setPositiveButton("Evet", (dialog, which) -> {
                    try {
                        // JSON oluştur
                        JSONObject wifiJson = new JSONObject();
                        wifiJson.put("cmd", "set_wifi");
                        wifiJson.put("mode", "sta");
                        wifiJson.put("ssid", ssid);
                        wifiJson.put("password", password);

                        // Burada SocketManager üzerinden veri gönderilmesi gerekiyor
                        // socketManager.sendMessage(wifiJson.toString());

                        // Bildirim
                        Toast.makeText(this, "WiFi bağlantı isteği gönderildi",
                                Toast.LENGTH_SHORT).show();

                    } catch (JSONException e) {
                        Toast.makeText(this, "JSON hatası: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hayır", null)
                .show();
    }

    private void startApMode() {
        // Onay dialog'u göster
        new AlertDialog.Builder(this)
                .setTitle("AP Modu")
                .setMessage("ESP32'yi AP moduna geçirmek istediğinizden emin misiniz?")
                .setPositiveButton("Evet", (dialog, which) -> {
                    try {
                        // JSON oluştur
                        JSONObject wifiJson = new JSONObject();
                        wifiJson.put("cmd", "set_wifi");
                        wifiJson.put("mode", "ap");

                        // Burada SocketManager üzerinden veri gönderilmesi gerekiyor
                        // socketManager.sendMessage(wifiJson.toString());

                        // Bildirim
                        Toast.makeText(this, "AP modu başlatma isteği gönderildi",
                                Toast.LENGTH_SHORT).show();

                    } catch (JSONException e) {
                        Toast.makeText(this, "JSON hatası: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hayır", null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Geri tuşuna basıldığında aktiviteyi kapat
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}