package com.kulucka.mk.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.kulucka.mk.models.*;

/**
 * SharedPreferences kullanarak yerel veri depolama işlemlerini yöneten sınıf
 * Uygulama ayarları, ESP32 IP adresi ve son bilinen değerleri saklar
 */
public class SharedPreferencesManager {

    private static final String TAG = "SharedPreferencesManager";
    private static final String PREF_NAME = "kulucka_mk_prefs";
    private static final String PREF_SYSTEM_STATUS = "system_status_prefs";

    private static SharedPreferencesManager instance;
    private SharedPreferences preferences;
    private SharedPreferences systemPreferences;
    private Gson gson;

    // Singleton pattern
    public static synchronized SharedPreferencesManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPreferencesManager(context.getApplicationContext());
        }
        return instance;
    }

    private SharedPreferencesManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        systemPreferences = context.getSharedPreferences(PREF_SYSTEM_STATUS, Context.MODE_PRIVATE);
        gson = new Gson();

        // İlk kurulum kontrolü
        if (isFirstLaunch()) {
            initializeDefaultValues();
            setFirstLaunch(false);
        }
    }

    /**
     * Varsayılan değerleri ayarlar
     */
    private void initializeDefaultValues() {
        Log.i(TAG, "İlk kurulum - varsayılan değerler ayarlanıyor");

        // Network ayarları
        setESP32IP(Constants.ESP32_AP_IP);
        setAutoConnect(true);
        setNotificationEnabled(true);
        setUpdateInterval(Constants.DATA_UPDATE_INTERVAL);

        // UI ayarları
        setThemeMode("light");
        setLanguage("tr");

        Log.i(TAG, "Varsayılan değerler ayarlandı");
    }

    // === ESP32 Bağlantı Ayarları ===

    public String getESP32IP() {
        return preferences.getString(Constants.PREF_ESP32_IP, Constants.ESP32_AP_IP);
    }

    public void setESP32IP(String ipAddress) {
        preferences.edit()
                .putString(Constants.PREF_ESP32_IP, ipAddress)
                .apply();
        Log.d(TAG, "ESP32 IP adresi kaydedildi: " + ipAddress);
    }

    public String getLastKnownIP() {
        return preferences.getString(Constants.PREF_LAST_KNOWN_IP, Constants.ESP32_AP_IP);
    }

    public void setLastKnownIP(String ipAddress) {
        preferences.edit()
                .putString(Constants.PREF_LAST_KNOWN_IP, ipAddress)
                .apply();
        Log.d(TAG, "Son bilinen IP adresi kaydedildi: " + ipAddress);
    }

    public boolean isAutoConnect() {
        return preferences.getBoolean(Constants.PREF_AUTO_CONNECT, true);
    }

    public void setAutoConnect(boolean autoConnect) {
        preferences.edit()
                .putBoolean(Constants.PREF_AUTO_CONNECT, autoConnect)
                .apply();
    }

    // === Bildirim Ayarları ===

    public boolean isNotificationEnabled() {
        return preferences.getBoolean(Constants.PREF_NOTIFICATION_ENABLED, true);
    }

    public void setNotificationEnabled(boolean enabled) {
        preferences.edit()
                .putBoolean(Constants.PREF_NOTIFICATION_ENABLED, enabled)
                .apply();
    }

    public long getUpdateInterval() {
        return preferences.getLong(Constants.PREF_UPDATE_INTERVAL, Constants.DATA_UPDATE_INTERVAL);
    }

    public void setUpdateInterval(long interval) {
        preferences.edit()
                .putLong(Constants.PREF_UPDATE_INTERVAL, interval)
                .apply();
    }

    // === UI Ayarları ===

    public String getThemeMode() {
        return preferences.getString(Constants.PREF_THEME_MODE, "light");
    }

    public void setThemeMode(String themeMode) {
        preferences.edit()
                .putString(Constants.PREF_THEME_MODE, themeMode)
                .apply();
    }

    public String getLanguage() {
        return preferences.getString(Constants.PREF_LANGUAGE, "tr");
    }

    public void setLanguage(String language) {
        preferences.edit()
                .putString(Constants.PREF_LANGUAGE, language)
                .apply();
    }

    public boolean isFirstLaunch() {
        return preferences.getBoolean(Constants.PREF_FIRST_LAUNCH, true);
    }

    public void setFirstLaunch(boolean firstLaunch) {
        preferences.edit()
                .putBoolean(Constants.PREF_FIRST_LAUNCH, firstLaunch)
                .apply();
    }

    // === Sistem Durumu Saklama ===

    /**
     * Son sistem durumunu saklar
     */
    public void saveSystemStatus(SystemStatus systemStatus) {
        if (systemStatus == null) return;

        try {
            String json = gson.toJson(systemStatus);
            systemPreferences.edit()
                    .putString("last_system_status", json)
                    .putLong("last_update_time", System.currentTimeMillis())
                    .apply();

            Log.d(TAG, "Sistem durumu kaydedildi");
        } catch (Exception e) {
            Log.e(TAG, "Sistem durumu kaydedilemedi", e);
        }
    }

    /**
     * Son sistem durumunu getirir
     */
    public SystemStatus getLastSystemStatus() {
        try {
            String json = systemPreferences.getString("last_system_status", null);
            if (json != null) {
                return gson.fromJson(json, SystemStatus.class);
            }
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "Sistem durumu okunamadı", e);
        }
        return null;
    }

    /**
     * Son güncelleme zamanını getirir
     */
    public long getLastUpdateTime() {
        return systemPreferences.getLong("last_update_time", 0);
    }

    // === Kuluçka Ayarları Saklama ===

    public void saveIncubationSettings(IncubationSettings settings) {
        if (settings == null) return;

        try {
            String json = gson.toJson(settings);
            preferences.edit()
                    .putString("incubation_settings", json)
                    .apply();
            Log.d(TAG, "Kuluçka ayarları kaydedildi");
        } catch (Exception e) {
            Log.e(TAG, "Kuluçka ayarları kaydedilemedi", e);
        }
    }

    public IncubationSettings getIncubationSettings() {
        try {
            String json = preferences.getString("incubation_settings", null);
            if (json != null) {
                return gson.fromJson(json, IncubationSettings.class);
            }
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "Kuluçka ayarları okunamadı", e);
        }
        return new IncubationSettings(); // Varsayılan değerlerle döndür
    }

    // === PID Ayarları Saklama ===

    public void savePIDSettings(PIDSettings settings) {
        if (settings == null) return;

        try {
            String json = gson.toJson(settings);
            preferences.edit()
                    .putString("pid_settings", json)
                    .apply();
            Log.d(TAG, "PID ayarları kaydedildi");
        } catch (Exception e) {
            Log.e(TAG, "PID ayarları kaydedilemedi", e);
        }
    }

    public PIDSettings getPIDSettings() {
        try {
            String json = preferences.getString("pid_settings", null);
            if (json != null) {
                return gson.fromJson(json, PIDSettings.class);
            }
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "PID ayarları okunamadı", e);
        }
        return new PIDSettings(); // Varsayılan değerlerle döndür
    }

    // === Alarm Ayarları Saklama ===

    public void saveAlarmSettings(AlarmSettings settings) {
        if (settings == null) return;

        try {
            String json = gson.toJson(settings);
            preferences.edit()
                    .putString("alarm_settings", json)
                    .apply();
            Log.d(TAG, "Alarm ayarları kaydedildi");
        } catch (Exception e) {
            Log.e(TAG, "Alarm ayarları kaydedilemedi", e);
        }
    }

    public AlarmSettings getAlarmSettings() {
        try {
            String json = preferences.getString("alarm_settings", null);
            if (json != null) {
                return gson.fromJson(json, AlarmSettings.class);
            }
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "Alarm ayarları okunamadı", e);
        }
        return new AlarmSettings(); // Varsayılan değerlerle döndür
    }

    // === Kalibrasyon Ayarları Saklama ===

    public void saveCalibrationData(CalibrationData data) {
        if (data == null) return;

        try {
            String json = gson.toJson(data);
            preferences.edit()
                    .putString("calibration_data", json)
                    .apply();
            Log.d(TAG, "Kalibrasyon verileri kaydedildi");
        } catch (Exception e) {
            Log.e(TAG, "Kalibrasyon verileri kaydedilemedi", e);
        }
    }

    public CalibrationData getCalibrationData() {
        try {
            String json = preferences.getString("calibration_data", null);
            if (json != null) {
                return gson.fromJson(json, CalibrationData.class);
            }
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "Kalibrasyon verileri okunamadı", e);
        }
        return new CalibrationData(); // Varsayılan değerlerle döndür
    }

    // === Hedef Değerler ===

    public void saveTargetValues(double temperature, double humidity) {
        preferences.edit()
                .putFloat("target_temperature", (float) temperature)
                .putFloat("target_humidity", (float) humidity)
                .putLong("target_values_time", System.currentTimeMillis())
                .apply();
        Log.d(TAG, "Hedef değerler kaydedildi - Sıcaklık: " + temperature + ", Nem: " + humidity);
    }

    public double getTargetTemperature() {
        return preferences.getFloat("target_temperature", (float) Constants.DEFAULT_TARGET_TEMP);
    }

    public double getTargetHumidity() {
        return preferences.getFloat("target_humidity", (float) Constants.DEFAULT_TARGET_HUMIDITY);
    }

    // === Motor Ayarları ===

    public void saveMotorSettings(int waitTime, int runTime) {
        preferences.edit()
                .putInt("motor_wait_time", waitTime)
                .putInt("motor_run_time", runTime)
                .apply();
        Log.d(TAG, "Motor ayarları kaydedildi - Bekleme: " + waitTime + ", Çalışma: " + runTime);
    }

    public int getMotorWaitTime() {
        return preferences.getInt("motor_wait_time", Constants.DEFAULT_MOTOR_WAIT_TIME);
    }

    public int getMotorRunTime() {
        return preferences.getInt("motor_run_time", Constants.DEFAULT_MOTOR_RUN_TIME);
    }

    // === WiFi Bilgileri ===

    public void saveLastConnectedWiFi(String ssid, String ipAddress) {
        preferences.edit()
                .putString("last_wifi_ssid", ssid)
                .putString("last_wifi_ip", ipAddress)
                .putLong("last_wifi_time", System.currentTimeMillis())
                .apply();
        Log.d(TAG, "Son WiFi bilgileri kaydedildi - SSID: " + ssid + ", IP: " + ipAddress);
    }

    public String getLastConnectedWiFiSSID() {
        return preferences.getString("last_wifi_ssid", null);
    }

    public String getLastConnectedWiFiIP() {
        return preferences.getString("last_wifi_ip", null);
    }

    public long getLastWiFiConnectionTime() {
        return preferences.getLong("last_wifi_time", 0);
    }

    // === Alarm Geçmişi ===

    public void saveLastAlarm(String alarmMessage, long timestamp) {
        preferences.edit()
                .putString("last_alarm_message", alarmMessage)
                .putLong("last_alarm_time", timestamp)
                .apply();
    }

    public String getLastAlarmMessage() {
        return preferences.getString("last_alarm_message", null);
    }

    public long getLastAlarmTime() {
        return preferences.getLong("last_alarm_time", 0);
    }

    // === İstatistikler ===

    public void incrementAppLaunchCount() {
        int count = preferences.getInt("app_launch_count", 0);
        preferences.edit()
                .putInt("app_launch_count", count + 1)
                .putLong("last_launch_time", System.currentTimeMillis())
                .apply();
    }

    public int getAppLaunchCount() {
        return preferences.getInt("app_launch_count", 0);
    }

    public long getLastLaunchTime() {
        return preferences.getLong("last_launch_time", 0);
    }

    public void updateConnectionStats(boolean success) {
        int totalAttempts = preferences.getInt("connection_attempts", 0);
        int successfulConnections = preferences.getInt("successful_connections", 0);

        totalAttempts++;
        if (success) {
            successfulConnections++;
        }

        preferences.edit()
                .putInt("connection_attempts", totalAttempts)
                .putInt("successful_connections", successfulConnections)
                .putLong("last_connection_attempt", System.currentTimeMillis())
                .apply();
    }

    public int getConnectionAttempts() {
        return preferences.getInt("connection_attempts", 0);
    }

    public int getSuccessfulConnections() {
        return preferences.getInt("successful_connections", 0);
    }

    public double getConnectionSuccessRate() {
        int attempts = getConnectionAttempts();
        if (attempts == 0) return 0;
        return (double) getSuccessfulConnections() / attempts * 100;
    }

    // === Veri Temizleme ===

    /**
     * Tüm ayarları sıfırlar
     */
    public void clearAllSettings() {
        preferences.edit().clear().apply();
        systemPreferences.edit().clear().apply();
        Log.i(TAG, "Tüm ayarlar temizlendi");
    }

    /**
     * Sadece sistem durumu verilerini temizler
     */
    public void clearSystemData() {
        systemPreferences.edit().clear().apply();
        Log.i(TAG, "Sistem verileri temizlendi");
    }

    /**
     * Sadece istatistikleri temizler
     */
    public void clearStatistics() {
        preferences.edit()
                .remove("app_launch_count")
                .remove("last_launch_time")
                .remove("connection_attempts")
                .remove("successful_connections")
                .remove("last_connection_attempt")
                .apply();
        Log.i(TAG, "İstatistikler temizlendi");
    }

    // === Debug ===

    /**
     * Tüm ayarları loglar (debug amaçlı)
     */
    public void logAllSettings() {
        Log.d(TAG, "=== Tüm Ayarlar ===");
        Log.d(TAG, "ESP32 IP: " + getESP32IP());
        Log.d(TAG, "Son Bilinen IP: " + getLastKnownIP());
        Log.d(TAG, "Otomatik Bağlan: " + isAutoConnect());
        Log.d(TAG, "Bildirim Etkin: " + isNotificationEnabled());
        Log.d(TAG, "Güncelleme Aralığı: " + getUpdateInterval());
        Log.d(TAG, "Tema: " + getThemeMode());
        Log.d(TAG, "Dil: " + getLanguage());
        Log.d(TAG, "İlk Açılış: " + isFirstLaunch());
        Log.d(TAG, "Uygulama Açılış Sayısı: " + getAppLaunchCount());
        Log.d(TAG, "Bağlantı Başarı Oranı: %.1f%%", getConnectionSuccessRate());
        Log.d(TAG, "==================");
    }
}