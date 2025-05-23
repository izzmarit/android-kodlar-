package com.kulucka.mk_v5.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.kulucka.mk_v5.models.AlarmSettings;
import com.kulucka.mk_v5.models.CalibrationSettings;
import com.kulucka.mk_v5.models.MotorSettings;
import com.kulucka.mk_v5.models.PidSettings;

public class SharedPrefsManager {
    private static SharedPrefsManager instance;
    private SharedPreferences sharedPreferences;

    private SharedPrefsManager(Context context) {
        sharedPreferences = context.getSharedPreferences(Constants.PREF_FILE_NAME, Context.MODE_PRIVATE);
    }

    public static void init(Context context) {
        if (instance == null) {
            instance = new SharedPrefsManager(context);
        }
    }

    public static SharedPrefsManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SharedPrefsManager must be initialized with init() before use");
        }
        return instance;
    }

    public String getDeviceIp() {
        return sharedPreferences.getString(Constants.PREF_DEVICE_IP, Constants.DEFAULT_IP_ADDRESS);
    }

    public void setDeviceIp(String ip) {
        sharedPreferences.edit().putString(Constants.PREF_DEVICE_IP, ip).apply();
    }

    public String getApSsid() {
        return sharedPreferences.getString(Constants.PREF_AP_SSID, Constants.DEFAULT_AP_SSID);
    }

    public void setApSsid(String ssid) {
        sharedPreferences.edit().putString(Constants.PREF_AP_SSID, ssid).apply();
    }

    public String getApPassword() {
        return sharedPreferences.getString(Constants.PREF_AP_PASSWORD, Constants.DEFAULT_AP_PASSWORD);
    }

    public void setApPassword(String password) {
        sharedPreferences.edit().putString(Constants.PREF_AP_PASSWORD, password).apply();
    }

    public boolean isAutoConnectEnabled() {
        return sharedPreferences.getBoolean(Constants.PREF_AUTO_CONNECT, true);
    }

    public void setAutoConnectEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(Constants.PREF_AUTO_CONNECT, enabled).apply();
    }

    // PID ayarlarını kaydet
    public void savePidSettings(PidSettings settings) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat("pid_kp", settings.getPidKp());
        editor.putFloat("pid_ki", settings.getPidKi());
        editor.putFloat("pid_kd", settings.getPidKd());
        editor.apply();
    }

    // PID ayarlarını yükle
    public PidSettings loadPidSettings() {
        PidSettings settings = new PidSettings();
        settings.setPidKp(sharedPreferences.getFloat("pid_kp", 10.0f));
        settings.setPidKi(sharedPreferences.getFloat("pid_ki", 0.1f));
        settings.setPidKd(sharedPreferences.getFloat("pid_kd", 5.0f));
        return settings;
    }

    // Motor ayarlarını kaydet
    public void saveMotorSettings(MotorSettings settings) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong("motor_wait_time", settings.getMotorWaitTime());
        editor.putLong("motor_run_time", settings.getMotorRunTime());
        editor.apply();
    }

    // Motor ayarlarını yükle
    public MotorSettings loadMotorSettings() {
        MotorSettings settings = new MotorSettings();
        settings.setMotorWaitTime(sharedPreferences.getLong("motor_wait_time", 120));
        settings.setMotorRunTime(sharedPreferences.getLong("motor_run_time", 14));
        return settings;
    }

    // Kalibrasyon ayarlarını kaydet
    public void saveCalibrationSettings(CalibrationSettings settings) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat("temp_calibration_1", settings.getTempCalibration1());
        editor.putFloat("temp_calibration_2", settings.getTempCalibration2());
        editor.putFloat("humid_calibration_1", settings.getHumidCalibration1());
        editor.putFloat("humid_calibration_2", settings.getHumidCalibration2());
        editor.apply();
    }

    // Kalibrasyon ayarlarını yükle
    public CalibrationSettings loadCalibrationSettings() {
        CalibrationSettings settings = new CalibrationSettings();
        settings.setTempCalibration1(sharedPreferences.getFloat("temp_calibration_1", 0.0f));
        settings.setTempCalibration2(sharedPreferences.getFloat("temp_calibration_2", 0.0f));
        settings.setHumidCalibration1(sharedPreferences.getFloat("humid_calibration_1", 0.0f));
        settings.setHumidCalibration2(sharedPreferences.getFloat("humid_calibration_2", 0.0f));
        return settings;
    }

    // Alarm ayarlarını kaydet
    public void saveAlarmSettings(AlarmSettings settings) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat("temp_low_alarm", settings.getTempLowAlarm());
        editor.putFloat("temp_high_alarm", settings.getTempHighAlarm());
        editor.putFloat("humid_low_alarm", settings.getHumidLowAlarm());
        editor.putFloat("humid_high_alarm", settings.getHumidHighAlarm());
        editor.apply();
    }

    // Alarm ayarlarını yükle
    public AlarmSettings loadAlarmSettings() {
        AlarmSettings settings = new AlarmSettings();
        settings.setTempLowAlarm(sharedPreferences.getFloat("temp_low_alarm", 1.0f));
        settings.setTempHighAlarm(sharedPreferences.getFloat("temp_high_alarm", 1.0f));
        settings.setHumidLowAlarm(sharedPreferences.getFloat("humid_low_alarm", 10.0f));
        settings.setHumidHighAlarm(sharedPreferences.getFloat("humid_high_alarm", 10.0f));
        return settings;
    }
}