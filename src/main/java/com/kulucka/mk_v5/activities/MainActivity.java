package com.kulucka.mk_v5.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import com.kulucka.mk_v5.R;
import com.kulucka.mk_v5.models.IncubationStatus;
import com.kulucka.mk_v5.services.ApiService;
import com.kulucka.mk_v5.services.DataRefreshService;
import com.kulucka.mk_v5.utils.NetworkUtils;
import com.kulucka.mk_v5.utils.SharedPrefsManager;

public class MainActivity extends AppCompatActivity {
    private TextView tvTemperature;
    private TextView tvHumidity;
    private TextView tvTargetTemp;
    private TextView tvTargetHumidity;
    private TextView tvDay;
    private TextView tvType;
    private TextView tvHeaterStatus;
    private TextView tvHumidifierStatus;
    private TextView tvMotorStatus;
    private TextView tvConnectionStatus;
    private View btnConnect;
    private TextView tvPidMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ÖNCELİKLE arayüz elemanlarını başlat
        setupUI();

        // SONRASINDA veri gözleyicilerini ayarla
        observeData();

        // Arka plan servisini başlat
        startBackgroundService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Network callback'lerini temizle
        NetworkUtils.getInstance(this).cleanup();
    }

    private void setupUI() {
        tvTemperature = findViewById(R.id.tv_temperature);
        tvHumidity = findViewById(R.id.tv_humidity);
        tvTargetTemp = findViewById(R.id.tv_target_temp);
        tvTargetHumidity = findViewById(R.id.tv_target_humidity);
        tvDay = findViewById(R.id.tv_day);
        tvType = findViewById(R.id.tv_type);
        tvHeaterStatus = findViewById(R.id.tv_heater_status);
        tvHumidifierStatus = findViewById(R.id.tv_humidifier_status);
        tvMotorStatus = findViewById(R.id.tv_motor_status);
        tvConnectionStatus = findViewById(R.id.tv_connection_status);
        btnConnect = findViewById(R.id.btn_connect);
        tvPidMode = findViewById(R.id.tv_pid_mode);

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectToDevice();
            }
        });
    }

    private void observeData() {
        // Durum verisini gözle
        ApiService.getInstance().getStatusLiveData().observe(this, new Observer<IncubationStatus>() {
            @Override
            public void onChanged(IncubationStatus status) {
                updateUI(status);
            }
        });

        // Bağlantı durumunu gözle
        ApiService.getInstance().getConnectionLiveData().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isConnected) {
                updateConnectionStatus(isConnected);
            }
        });

        // WiFi bağlantı durumunu gözle
        NetworkUtils.getInstance(this).getDeviceConnectionStatus().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isConnected) {
                if (isConnected) {
                    ApiService.getInstance().refreshStatus();
                }
            }
        });
    }

    private void updateUI(IncubationStatus status) {
        if (status == null) {
            return;
        }

        // TextView'ların null kontrolü
        if (tvTemperature == null || tvHumidity == null || tvTargetTemp == null ||
                tvTargetHumidity == null || tvDay == null || tvType == null ||
                tvHeaterStatus == null || tvHumidifierStatus == null || tvMotorStatus == null) {
            return;
        }

        tvTemperature.setText(String.format("%.1f°C", status.getTemperature()));
        tvHumidity.setText(String.format("%.0f%%", status.getHumidity()));
        tvTargetTemp.setText(String.format("%.1f°C", status.getTargetTemp()));
        tvTargetHumidity.setText(String.format("%.0f%%", status.getTargetHumid()));
        tvDay.setText(String.format("%d/%d", status.getCurrentDay(), status.getTotalDays()));
        tvType.setText(status.getIncubationType());

        tvHeaterStatus.setText(status.isHeaterState() ? "AÇIK" : "KAPALI");
        tvHumidifierStatus.setText(status.isHumidifierState() ? "AÇIK" : "KAPALI");
        tvMotorStatus.setText(status.isMotorState() ? "AÇIK" : "KAPALI");

        // Durum renkleri
        int activeColor = getResources().getColor(R.color.colorActive);
        int inactiveColor = getResources().getColor(R.color.colorInactive);

        tvHeaterStatus.setTextColor(status.isHeaterState() ? activeColor : inactiveColor);
        tvHumidifierStatus.setTextColor(status.isHumidifierState() ? activeColor : inactiveColor);
        tvMotorStatus.setTextColor(status.isMotorState() ? activeColor : inactiveColor);

        // PID durumunu güncelle
        if (status.getPidMode() != null) {
            tvPidMode.setText(status.getPidMode());
            tvPidMode.setTextColor(status.isPidModeActive() ?
                    getResources().getColor(R.color.colorActive) :
                    getResources().getColor(R.color.colorInactive));
        }
    }

    private void updateConnectionStatus(Boolean isConnected) {
        if (isConnected) {
            tvConnectionStatus.setText("Bağlı");
            tvConnectionStatus.setTextColor(getResources().getColor(R.color.colorActive));
        } else {
            tvConnectionStatus.setText("Bağlantı Yok");
            tvConnectionStatus.setTextColor(getResources().getColor(R.color.colorError));
        }
    }

    private void startBackgroundService() {
        Intent serviceIntent = new Intent(this, DataRefreshService.class);
        startService(serviceIntent);
    }

    private void connectToDevice() {
        final String ssid = SharedPrefsManager.getInstance().getApSsid();
        final String password = SharedPrefsManager.getInstance().getApPassword();

        // WiFi'ya bağlanmayı dene
        boolean result = NetworkUtils.getInstance(this).connectToWifi(ssid, password);

        if (result) {
            Toast.makeText(this, "Bağlanılıyor...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Bağlantı başlatılamadı", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}