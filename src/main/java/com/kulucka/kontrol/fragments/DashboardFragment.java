package com.kulucka.kontrol.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.LinearLayout;
import androidx.fragment.app.Fragment;
import androidx.cardview.widget.CardView;

import com.kulucka.kontrol.R;
import com.kulucka.kontrol.models.AlarmData;
import com.kulucka.kontrol.models.Profile;
import com.kulucka.kontrol.models.SensorData;
import com.kulucka.kontrol.models.AppSettings;

public class DashboardFragment extends Fragment {
    private static final String TAG = "DashboardFragment";

    // Ana durum görünümleri
    private TextView tvTemperature;
    private TextView tvHumidity;
    private TextView tvStatus;
    private TextView tvDayInfo;

    // Hedef değerler
    private TextView tvTargetTemp;
    private TextView tvTargetHumidity;

    // Sensör detayları
    private TextView tvTemp1;
    private TextView tvTemp2;
    private TextView tvHum1;
    private TextView tvHum2;

    // Röle durumları
    private ImageView ivHeater;
    private ImageView ivHumidifier;
    private ImageView ivMotor;

    // Alarm paneli
    private CardView cvAlarmPanel;
    private TextView tvAlarmMessage;
    private TextView tvAlarmType;
    private TextView tvAlarmTime;

    // Profil bilgileri
    private TextView tvProfileName;
    private TextView tvProfileDays;

    // Bağlantı durum göstergesi
    private LinearLayout llConnectionStatus;
    private TextView tvConnectionStatus;
    private ProgressBar pbLoading;

    public DashboardFragment() {
        // Gerekli boş kurucu
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Görünümü inflate et
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Görünüm elemanlarını tanımla
        initViews(view);

        return view;
    }

    private void initViews(View view) {
        // Ana durum görünümleri
        tvTemperature = view.findViewById(R.id.tvTemperature);
        tvHumidity = view.findViewById(R.id.tvHumidity);
        tvStatus = view.findViewById(R.id.tvStatus);
        tvDayInfo = view.findViewById(R.id.tvDayInfo);

        // Hedef değerler
        tvTargetTemp = view.findViewById(R.id.tvTargetTemp);
        tvTargetHumidity = view.findViewById(R.id.tvTargetHumidity);

        // Sensör detayları
        tvTemp1 = view.findViewById(R.id.tvTemp1);
        tvTemp2 = view.findViewById(R.id.tvTemp2);
        tvHum1 = view.findViewById(R.id.tvHum1);
        tvHum2 = view.findViewById(R.id.tvHum2);

        // Röle durumları
        ivHeater = view.findViewById(R.id.ivHeater);
        ivHumidifier = view.findViewById(R.id.ivHumidifier);
        ivMotor = view.findViewById(R.id.ivMotor);

        // Alarm paneli
        cvAlarmPanel = view.findViewById(R.id.cvAlarmPanel);
        tvAlarmMessage = view.findViewById(R.id.tvAlarmMessage);
        tvAlarmType = view.findViewById(R.id.tvAlarmType);
        tvAlarmTime = view.findViewById(R.id.tvAlarmTime);

        // Profil bilgileri
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileDays = view.findViewById(R.id.tvProfileDays);

        // Bağlantı durumu
        llConnectionStatus = view.findViewById(R.id.llConnectionStatus);
        tvConnectionStatus = view.findViewById(R.id.tvConnectionStatus);
        pbLoading = view.findViewById(R.id.pbLoading);

        // Başlangıçta alarım panelini gizle
        cvAlarmPanel.setVisibility(View.GONE);
    }

    // MainActivity'den çağrılan güncelleme metodları
    public void updateConnectionStatus(boolean connected) {
        if (isAdded()) {
            llConnectionStatus.setVisibility(connected ? View.GONE : View.VISIBLE);
            tvConnectionStatus.setText(connected ? "Bağlı" : "Bağlantı kuruluyor...");
            pbLoading.setVisibility(connected ? View.GONE : View.VISIBLE);
        }
    }

    public void updateSensorData(SensorData sensorData) {
        if (isAdded() && sensorData != null) {
            // Ana sıcaklık ve nem değerlerini güncelle
            tvTemperature.setText(String.format("%.1f°C", sensorData.getTemperature()));
            tvHumidity.setText(String.format("%%%.1f", sensorData.getHumidity()));

            // Sensör detaylarını güncelle
            tvTemp1.setText(String.format("%.1f°C", sensorData.getTemperature1()));
            tvTemp2.setText(String.format("%.1f°C", sensorData.getTemperature2()));
            tvHum1.setText(String.format("%%%.1f", sensorData.getHumidity1()));
            tvHum2.setText(String.format("%%%.1f", sensorData.getHumidity2()));

            // Röle durumlarını güncelle
            ivHeater.setImageResource(sensorData.isHeaterActive() ?
                    R.drawable.ic_heater_on : R.drawable.ic_heater_off);
            ivHumidifier.setImageResource(sensorData.isHumidifierActive() ?
                    R.drawable.ic_humidifier_on : R.drawable.ic_humidifier_off);
            ivMotor.setImageResource(sensorData.isMotorActive() ?
                    R.drawable.ic_motor_on : R.drawable.ic_motor_off);

            // Gün bilgisini güncelle
            if (sensorData.getCurrentDay() > 0) {
                tvDayInfo.setVisibility(View.VISIBLE);
                tvDayInfo.setText(String.format("Gün: %d", sensorData.getCurrentDay()));
            } else {
                tvDayInfo.setVisibility(View.GONE);
            }
        }
    }

    public void updateSettings(AppSettings settings) {
        if (isAdded() && settings != null) {
            // Hedef değerleri güncelle
            tvTargetTemp.setText(String.format("Hedef: %.1f°C", settings.getTargetTemperature()));
            tvTargetHumidity.setText(String.format("Hedef: %%%.1f", settings.getTargetHumidity()));

            // Kuluçka durumu
            if (settings.getStartTime() > 0) {
                tvStatus.setText("Kuluçka aktif");
                tvStatus.setTextColor(getResources().getColor(R.color.colorActive));
            } else {
                tvStatus.setText("Kuluçka beklemede");
                tvStatus.setTextColor(getResources().getColor(R.color.colorInactive));
            }
        }
    }

    public void updateProfile(Profile profile) {
        if (isAdded() && profile != null) {
            // Profil bilgilerini güncelle
            tvProfileName.setText(profile.getName());
            tvProfileDays.setText(String.format("Toplam %d gün", profile.getTotalDays()));
        }
    }

    public void updateAlarmData(AlarmData alarmData) {
        if (isAdded() && alarmData != null) {
            if (alarmData.isAlarmActive()) {
                // Alarm aktifse paneli göster ve bilgileri doldur
                cvAlarmPanel.setVisibility(View.VISIBLE);
                tvAlarmType.setText(alarmData.getAlarmTypeString());
                tvAlarmMessage.setText(alarmData.getAlarmMessage());

                // Alarm zamanını formatla
                long alarmTime = alarmData.getAlarmStartTime();
                tvAlarmTime.setText(formatAlarmTime(alarmTime));
            } else {
                // Alarm yoksa paneli gizle
                cvAlarmPanel.setVisibility(View.GONE);
            }
        }
    }

    private String formatAlarmTime(long timeMillis) {
        // Alarm zamanını formatlama işlemi
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        return sdf.format(new java.util.Date(timeMillis));
    }
}