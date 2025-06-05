package com.kulucka.mk_v5.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.kulucka.mk_v5.models.AlarmSettings;
import com.kulucka.mk_v5.models.PidSettings;
import com.kulucka.mk_v5.models.CalibrationSettings;
import com.kulucka.mk_v5.models.MotorSettings;

public class SharedPrefsManager {

    private static final String PREF_NAME = "KuluckaMKV5Prefs";
    private static SharedPrefsManager instance;
    private SharedPreferences sharedPreferences;

    // Keys for various settings
    private static final String KEY_TARGET_TEMPERATURE = "target_temperature";
    private static final String KEY_TARGET_HUMIDITY = "target_humidity";
    private static final String KEY_DEVICE_IP = "device_ip";
    private static final String KEY_DEVICE_PORT = "device_port";
    private static final String KEY_CONNECTION_TIMEOUT = "connection_timeout";
    private static final String KEY_IS_CONNECTED = "is_connected";
    private static final String KEY_LAST_CONNECTION_TIME = "last_connection_time";

    // WiFi Access Point Settings Keys
    private static final String KEY_AP_SSID = "ap_ssid";
    private static final String KEY_AP_PASSWORD = "ap_password";
    private static final String KEY_AUTO_CONNECT_ENABLED = "auto_connect_enabled";

    // Temperature PID Settings Keys - Sadece sıcaklık PID
    private static final String KEY_PID_MODE = "pid_mode";
    private static final String KEY_TEMP_PID_KP = "temp_pid_kp";
    private static final String KEY_TEMP_PID_KI = "temp_pid_ki";
    private static final String KEY_TEMP_PID_KD = "temp_pid_kd";
    private static final String KEY_PID_ENABLED = "pid_enabled";
    private static final String KEY_AUTO_TUNING_ENABLED = "auto_tuning_enabled";

    // PID Advanced Settings
    private static final String KEY_PID_SAMPLE_TIME = "pid_sample_time";
    private static final String KEY_PID_OUTPUT_MIN = "pid_output_min";
    private static final String KEY_PID_OUTPUT_MAX = "pid_output_max";
    private static final String KEY_PID_REVERSE_DIRECTION = "pid_reverse_direction";

    // Alarm Settings Keys
    private static final String KEY_MASTER_ALARM_ENABLED = "master_alarm_enabled";
    private static final String KEY_TEMP_LOW_ALARM = "temp_low_alarm";
    private static final String KEY_TEMP_HIGH_ALARM = "temp_high_alarm";
    private static final String KEY_HUMID_LOW_ALARM = "humid_low_alarm";
    private static final String KEY_HUMID_HIGH_ALARM = "humid_high_alarm";
    private static final String KEY_SOUND_ALARM_ENABLED = "sound_alarm_enabled";
    private static final String KEY_VIBRATION_ALARM_ENABLED = "vibration_alarm_enabled";

    // Calibration Settings Keys
    private static final String KEY_TEMP_OFFSET = "temp_offset";
    private static final String KEY_HUMIDITY_OFFSET = "humidity_offset";
    private static final String KEY_TEMP_CALIBRATION_FACTOR = "temp_calibration_factor";
    private static final String KEY_HUMIDITY_CALIBRATION_FACTOR = "humidity_calibration_factor";

    // Motor Settings Keys
    private static final String KEY_MOTOR_TURNING_INTERVAL = "motor_turning_interval";
    private static final String KEY_MOTOR_TURNING_DURATION = "motor_turning_duration";
    private static final String KEY_MOTOR_TURNING_ENABLED = "motor_turning_enabled";
    private static final String KEY_MOTOR_DIRECTION = "motor_direction";

    // Application Settings Keys
    private static final String KEY_AUTO_SYNC_ENABLED = "auto_sync_enabled";
    private static final String KEY_SYNC_INTERVAL = "sync_interval";
    private static final String KEY_DARK_MODE_ENABLED = "dark_mode_enabled";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_UNIT_SYSTEM = "unit_system";

    private SharedPrefsManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SharedPrefsManager getInstance() {
        return instance;
    }

    public static synchronized void initialize(Context context) {
        if (instance == null) {
            instance = new SharedPrefsManager(context.getApplicationContext());
        }
    }

    // Alternative init method for backward compatibility
    public static synchronized void init(Context context) {
        initialize(context);
    }

    // Basic Temperature and Humidity Settings
    public void setTargetTemperature(float temperature) {
        sharedPreferences.edit().putFloat(KEY_TARGET_TEMPERATURE, temperature).apply();
    }

    public float getTargetTemperature() {
        return sharedPreferences.getFloat(KEY_TARGET_TEMPERATURE, 37.5f);
    }

    public void setTargetHumidity(float humidity) {
        sharedPreferences.edit().putFloat(KEY_TARGET_HUMIDITY, humidity).apply();
    }

    public float getTargetHumidity() {
        return sharedPreferences.getFloat(KEY_TARGET_HUMIDITY, 60.0f);
    }

    // Device Connection Settings
    public void setDeviceIp(String ip) {
        sharedPreferences.edit().putString(KEY_DEVICE_IP, ip).apply();
    }

    public String getDeviceIp() {
        return sharedPreferences.getString(KEY_DEVICE_IP, "192.168.4.1"); // ESP32 AP modu IP
    }

    public void setDevicePort(int port) {
        sharedPreferences.edit().putInt(KEY_DEVICE_PORT, port).apply();
    }

    public int getDevicePort() {
        return sharedPreferences.getInt(KEY_DEVICE_PORT, 80);
    }

    public void setConnectionTimeout(int timeout) {
        sharedPreferences.edit().putInt(KEY_CONNECTION_TIMEOUT, timeout).apply();
    }

    public int getConnectionTimeout() {
        return sharedPreferences.getInt(KEY_CONNECTION_TIMEOUT, 5000);
    }

    public void setConnected(boolean connected) {
        sharedPreferences.edit().putBoolean(KEY_IS_CONNECTED, connected).apply();
        if (connected) {
            setLastConnectionTime(System.currentTimeMillis());
        }
    }

    public boolean isConnected() {
        return sharedPreferences.getBoolean(KEY_IS_CONNECTED, false);
    }

    public void setLastConnectionTime(long timestamp) {
        sharedPreferences.edit().putLong(KEY_LAST_CONNECTION_TIME, timestamp).apply();
    }

    public long getLastConnectionTime() {
        return sharedPreferences.getLong(KEY_LAST_CONNECTION_TIME, 0);
    }

    // WiFi Access Point Settings
    public void setApSsid(String ssid) {
        sharedPreferences.edit().putString(KEY_AP_SSID, ssid).apply();
    }

    public String getApSsid() {
        return sharedPreferences.getString(KEY_AP_SSID, "KULUCKA_MK_V5");
    }

    public void setApPassword(String password) {
        sharedPreferences.edit().putString(KEY_AP_PASSWORD, password).apply();
    }

    public String getApPassword() {
        return sharedPreferences.getString(KEY_AP_PASSWORD, "12345678");
    }

    public void setAutoConnectEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_AUTO_CONNECT_ENABLED, enabled).apply();
    }

    public boolean isAutoConnectEnabled() {
        return sharedPreferences.getBoolean(KEY_AUTO_CONNECT_ENABLED, true);
    }

    // Temperature PID Settings Methods - Sadece sıcaklık PID
    public String getPidMode() {
        return sharedPreferences.getString(KEY_PID_MODE, "off");
    }

    public void setPidMode(String mode) {
        sharedPreferences.edit().putString(KEY_PID_MODE, mode).apply();
    }

    public void savePidSettings(PidSettings settings) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(KEY_TEMP_PID_KP, settings.getPidKp());
        editor.putFloat(KEY_TEMP_PID_KI, settings.getPidKi());
        editor.putFloat(KEY_TEMP_PID_KD, settings.getPidKd());
        editor.apply();
    }

    public PidSettings loadPidSettings() {
        PidSettings settings = new PidSettings();
        settings.setPidKp(sharedPreferences.getFloat(KEY_TEMP_PID_KP, 2.0f));
        settings.setPidKi(sharedPreferences.getFloat(KEY_TEMP_PID_KI, 0.1f));
        settings.setPidKd(sharedPreferences.getFloat(KEY_TEMP_PID_KD, 0.5f));
        return settings;
    }

    public void setPidEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_PID_ENABLED, enabled).apply();
    }

    public boolean isPidEnabled() {
        return sharedPreferences.getBoolean(KEY_PID_ENABLED, false);
    }

    public void setAutoTuningEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_AUTO_TUNING_ENABLED, enabled).apply();
    }

    public boolean isAutoTuningEnabled() {
        return sharedPreferences.getBoolean(KEY_AUTO_TUNING_ENABLED, false);
    }

    // PID Advanced Settings
    public void setPidSampleTime(int sampleTime) {
        sharedPreferences.edit().putInt(KEY_PID_SAMPLE_TIME, sampleTime).apply();
    }

    public int getPidSampleTime() {
        return sharedPreferences.getInt(KEY_PID_SAMPLE_TIME, 1000);
    }

    public void setPidOutputMin(float outputMin) {
        sharedPreferences.edit().putFloat(KEY_PID_OUTPUT_MIN, outputMin).apply();
    }

    public float getPidOutputMin() {
        return sharedPreferences.getFloat(KEY_PID_OUTPUT_MIN, 0.0f);
    }

    public void setPidOutputMax(float outputMax) {
        sharedPreferences.edit().putFloat(KEY_PID_OUTPUT_MAX, outputMax).apply();
    }

    public float getPidOutputMax() {
        return sharedPreferences.getFloat(KEY_PID_OUTPUT_MAX, 100.0f);
    }

    public void setPidReverseDirection(boolean reverse) {
        sharedPreferences.edit().putBoolean(KEY_PID_REVERSE_DIRECTION, reverse).apply();
    }

    public boolean isPidReverseDirection() {
        return sharedPreferences.getBoolean(KEY_PID_REVERSE_DIRECTION, false);
    }

    // Individual PID parameter setters/getters
    public void setTempPidKp(float kp) {
        sharedPreferences.edit().putFloat(KEY_TEMP_PID_KP, kp).apply();
    }

    public float getTempPidKp() {
        return sharedPreferences.getFloat(KEY_TEMP_PID_KP, 2.0f);
    }

    public void setTempPidKi(float ki) {
        sharedPreferences.edit().putFloat(KEY_TEMP_PID_KI, ki).apply();
    }

    public float getTempPidKi() {
        return sharedPreferences.getFloat(KEY_TEMP_PID_KI, 0.1f);
    }

    public void setTempPidKd(float kd) {
        sharedPreferences.edit().putFloat(KEY_TEMP_PID_KD, kd).apply();
    }

    public float getTempPidKd() {
        return sharedPreferences.getFloat(KEY_TEMP_PID_KD, 0.5f);
    }

    // Alarm Settings Methods
    public boolean isMasterAlarmEnabled() {
        return sharedPreferences.getBoolean(KEY_MASTER_ALARM_ENABLED, true);
    }

    public void setMasterAlarmEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_MASTER_ALARM_ENABLED, enabled).apply();
    }

    public void saveAlarmSettings(AlarmSettings settings) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(KEY_TEMP_LOW_ALARM, settings.getTempLowAlarm());
        editor.putFloat(KEY_TEMP_HIGH_ALARM, settings.getTempHighAlarm());
        editor.putFloat(KEY_HUMID_LOW_ALARM, settings.getHumidLowAlarm());
        editor.putFloat(KEY_HUMID_HIGH_ALARM, settings.getHumidHighAlarm());
        editor.apply();
    }

    public AlarmSettings loadAlarmSettings() {
        AlarmSettings settings = new AlarmSettings();
        settings.setTempLowAlarm(sharedPreferences.getFloat(KEY_TEMP_LOW_ALARM, 1.0f));
        settings.setTempHighAlarm(sharedPreferences.getFloat(KEY_TEMP_HIGH_ALARM, 1.0f));
        settings.setHumidLowAlarm(sharedPreferences.getFloat(KEY_HUMID_LOW_ALARM, 5.0f));
        settings.setHumidHighAlarm(sharedPreferences.getFloat(KEY_HUMID_HIGH_ALARM, 5.0f));
        return settings;
    }

    public void setSoundAlarmEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_SOUND_ALARM_ENABLED, enabled).apply();
    }

    public boolean isSoundAlarmEnabled() {
        return sharedPreferences.getBoolean(KEY_SOUND_ALARM_ENABLED, true);
    }

    public void setVibrationAlarmEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_VIBRATION_ALARM_ENABLED, enabled).apply();
    }

    public boolean isVibrationAlarmEnabled() {
        return sharedPreferences.getBoolean(KEY_VIBRATION_ALARM_ENABLED, true);
    }

    // Calibration Settings Methods
    public void saveCalibrationSettings(CalibrationSettings settings) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(KEY_TEMP_OFFSET, settings.getTempOffset());
        editor.putFloat(KEY_HUMIDITY_OFFSET, settings.getHumidityOffset());
        editor.putFloat(KEY_TEMP_CALIBRATION_FACTOR, settings.getTempCalibrationFactor());
        editor.putFloat(KEY_HUMIDITY_CALIBRATION_FACTOR, settings.getHumidityCalibrationFactor());
        editor.apply();
    }

    public CalibrationSettings loadCalibrationSettings() {
        CalibrationSettings settings = new CalibrationSettings();
        settings.setTempOffset(sharedPreferences.getFloat(KEY_TEMP_OFFSET, 0.0f));
        settings.setHumidityOffset(sharedPreferences.getFloat(KEY_HUMIDITY_OFFSET, 0.0f));
        settings.setTempCalibrationFactor(sharedPreferences.getFloat(KEY_TEMP_CALIBRATION_FACTOR, 1.0f));
        settings.setHumidityCalibrationFactor(sharedPreferences.getFloat(KEY_HUMIDITY_CALIBRATION_FACTOR, 1.0f));
        return settings;
    }

    // Motor Settings Methods
    public void saveMotorSettings(MotorSettings settings) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_MOTOR_TURNING_INTERVAL, settings.getTurningInterval());
        editor.putInt(KEY_MOTOR_TURNING_DURATION, settings.getTurningDuration());
        editor.putBoolean(KEY_MOTOR_TURNING_ENABLED, settings.isTurningEnabled());
        editor.putString(KEY_MOTOR_DIRECTION, settings.getDirection());
        editor.apply();
    }

    public MotorSettings loadMotorSettings() {
        MotorSettings settings = new MotorSettings();
        settings.setTurningInterval(sharedPreferences.getInt(KEY_MOTOR_TURNING_INTERVAL, 120));
        settings.setTurningDuration(sharedPreferences.getInt(KEY_MOTOR_TURNING_DURATION, 5));
        settings.setTurningEnabled(sharedPreferences.getBoolean(KEY_MOTOR_TURNING_ENABLED, true));
        settings.setDirection(sharedPreferences.getString(KEY_MOTOR_DIRECTION, "clockwise"));
        return settings;
    }

    // Application Settings Methods
    public void setAutoSyncEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_AUTO_SYNC_ENABLED, enabled).apply();
    }

    public boolean isAutoSyncEnabled() {
        return sharedPreferences.getBoolean(KEY_AUTO_SYNC_ENABLED, true);
    }

    public void setSyncInterval(int intervalMinutes) {
        sharedPreferences.edit().putInt(KEY_SYNC_INTERVAL, intervalMinutes).apply();
    }

    public int getSyncInterval() {
        return sharedPreferences.getInt(KEY_SYNC_INTERVAL, 30);
    }

    public void setDarkModeEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_DARK_MODE_ENABLED, enabled).apply();
    }

    public boolean isDarkModeEnabled() {
        return sharedPreferences.getBoolean(KEY_DARK_MODE_ENABLED, false);
    }

    public void setLanguage(String language) {
        sharedPreferences.edit().putString(KEY_LANGUAGE, language).apply();
    }

    public String getLanguage() {
        return sharedPreferences.getString(KEY_LANGUAGE, "tr");
    }

    public void setUnitSystem(String unitSystem) {
        sharedPreferences.edit().putString(KEY_UNIT_SYSTEM, unitSystem).apply();
    }

    public String getUnitSystem() {
        return sharedPreferences.getString(KEY_UNIT_SYSTEM, "metric");
    }

    // Utility Methods
    public void clearAllSettings() {
        sharedPreferences.edit().clear().apply();
    }

    public void clearConnectionSettings() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_DEVICE_IP);
        editor.remove(KEY_DEVICE_PORT);
        editor.remove(KEY_IS_CONNECTED);
        editor.remove(KEY_LAST_CONNECTION_TIME);
        editor.apply();
    }

    public void resetToDefaults() {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Reset basic settings
        editor.putFloat(KEY_TARGET_TEMPERATURE, 37.5f);
        editor.putFloat(KEY_TARGET_HUMIDITY, 60.0f);

        // Reset WiFi settings
        editor.putString(KEY_AP_SSID, "KULUCKA_MK_V5");
        editor.putString(KEY_AP_PASSWORD, "12345678");
        editor.putBoolean(KEY_AUTO_CONNECT_ENABLED, true);

        // Reset Temperature PID settings - ESP32 varsayılan değerleri
        editor.putString(KEY_PID_MODE, "off");
        editor.putFloat(KEY_TEMP_PID_KP, 2.0f);
        editor.putFloat(KEY_TEMP_PID_KI, 0.1f);
        editor.putFloat(KEY_TEMP_PID_KD, 0.5f);
        editor.putBoolean(KEY_PID_ENABLED, false);
        editor.putBoolean(KEY_AUTO_TUNING_ENABLED, false);

        // Reset PID advanced settings
        editor.putInt(KEY_PID_SAMPLE_TIME, 1000);
        editor.putFloat(KEY_PID_OUTPUT_MIN, 0.0f);
        editor.putFloat(KEY_PID_OUTPUT_MAX, 100.0f);
        editor.putBoolean(KEY_PID_REVERSE_DIRECTION, false);

        // Reset alarm settings
        editor.putBoolean(KEY_MASTER_ALARM_ENABLED, true);
        editor.putFloat(KEY_TEMP_LOW_ALARM, 1.0f);
        editor.putFloat(KEY_TEMP_HIGH_ALARM, 1.0f);
        editor.putFloat(KEY_HUMID_LOW_ALARM, 5.0f);
        editor.putFloat(KEY_HUMID_HIGH_ALARM, 5.0f);
        editor.putBoolean(KEY_SOUND_ALARM_ENABLED, true);
        editor.putBoolean(KEY_VIBRATION_ALARM_ENABLED, true);

        // Reset calibration settings
        editor.putFloat(KEY_TEMP_OFFSET, 0.0f);
        editor.putFloat(KEY_HUMIDITY_OFFSET, 0.0f);
        editor.putFloat(KEY_TEMP_CALIBRATION_FACTOR, 1.0f);
        editor.putFloat(KEY_HUMIDITY_CALIBRATION_FACTOR, 1.0f);

        // Reset motor settings
        editor.putInt(KEY_MOTOR_TURNING_INTERVAL, 120);
        editor.putInt(KEY_MOTOR_TURNING_DURATION, 5);
        editor.putBoolean(KEY_MOTOR_TURNING_ENABLED, true);
        editor.putString(KEY_MOTOR_DIRECTION, "clockwise");

        // Reset application settings
        editor.putBoolean(KEY_AUTO_SYNC_ENABLED, true);
        editor.putInt(KEY_SYNC_INTERVAL, 30);
        editor.putBoolean(KEY_DARK_MODE_ENABLED, false);
        editor.putString(KEY_LANGUAGE, "tr");
        editor.putString(KEY_UNIT_SYSTEM, "metric");

        editor.apply();
    }

    // Export/Import functionality for backup - Nem PID kaldırılmış
    public String exportSettings() {
        try {
            org.json.JSONObject jsonObject = new org.json.JSONObject();

            // Basic settings
            jsonObject.put(KEY_TARGET_TEMPERATURE, getTargetTemperature());
            jsonObject.put(KEY_TARGET_HUMIDITY, getTargetHumidity());

            // WiFi settings
            jsonObject.put(KEY_AP_SSID, getApSsid());
            jsonObject.put(KEY_AP_PASSWORD, getApPassword());
            jsonObject.put(KEY_AUTO_CONNECT_ENABLED, isAutoConnectEnabled());

            // Temperature PID settings - Sadece sıcaklık PID
            jsonObject.put(KEY_PID_MODE, getPidMode());
            PidSettings pidSettings = loadPidSettings();
            jsonObject.put(KEY_TEMP_PID_KP, pidSettings.getPidKp());
            jsonObject.put(KEY_TEMP_PID_KI, pidSettings.getPidKi());
            jsonObject.put(KEY_TEMP_PID_KD, pidSettings.getPidKd());
            jsonObject.put(KEY_PID_ENABLED, isPidEnabled());
            jsonObject.put(KEY_AUTO_TUNING_ENABLED, isAutoTuningEnabled());

            // PID advanced settings
            jsonObject.put(KEY_PID_SAMPLE_TIME, getPidSampleTime());
            jsonObject.put(KEY_PID_OUTPUT_MIN, getPidOutputMin());
            jsonObject.put(KEY_PID_OUTPUT_MAX, getPidOutputMax());
            jsonObject.put(KEY_PID_REVERSE_DIRECTION, isPidReverseDirection());

            // Alarm settings
            jsonObject.put(KEY_MASTER_ALARM_ENABLED, isMasterAlarmEnabled());
            AlarmSettings alarmSettings = loadAlarmSettings();
            jsonObject.put(KEY_TEMP_LOW_ALARM, alarmSettings.getTempLowAlarm());
            jsonObject.put(KEY_TEMP_HIGH_ALARM, alarmSettings.getTempHighAlarm());
            jsonObject.put(KEY_HUMID_LOW_ALARM, alarmSettings.getHumidLowAlarm());
            jsonObject.put(KEY_HUMID_HIGH_ALARM, alarmSettings.getHumidHighAlarm());

            // Calibration settings
            CalibrationSettings calibrationSettings = loadCalibrationSettings();
            jsonObject.put(KEY_TEMP_OFFSET, calibrationSettings.getTempOffset());
            jsonObject.put(KEY_HUMIDITY_OFFSET, calibrationSettings.getHumidityOffset());
            jsonObject.put(KEY_TEMP_CALIBRATION_FACTOR, calibrationSettings.getTempCalibrationFactor());
            jsonObject.put(KEY_HUMIDITY_CALIBRATION_FACTOR, calibrationSettings.getHumidityCalibrationFactor());

            // Motor settings
            MotorSettings motorSettings = loadMotorSettings();
            jsonObject.put(KEY_MOTOR_TURNING_INTERVAL, motorSettings.getTurningInterval());
            jsonObject.put(KEY_MOTOR_TURNING_DURATION, motorSettings.getTurningDuration());
            jsonObject.put(KEY_MOTOR_TURNING_ENABLED, motorSettings.isTurningEnabled());
            jsonObject.put(KEY_MOTOR_DIRECTION, motorSettings.getDirection());

            // Application settings
            jsonObject.put(KEY_AUTO_SYNC_ENABLED, isAutoSyncEnabled());
            jsonObject.put(KEY_SYNC_INTERVAL, getSyncInterval());
            jsonObject.put(KEY_DARK_MODE_ENABLED, isDarkModeEnabled());
            jsonObject.put(KEY_LANGUAGE, getLanguage());
            jsonObject.put(KEY_UNIT_SYSTEM, getUnitSystem());

            return jsonObject.toString();
        } catch (org.json.JSONException e) {
            return null;
        }
    }

    public boolean importSettings(String jsonString) {
        try {
            org.json.JSONObject jsonObject = new org.json.JSONObject(jsonString);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            // Import basic settings
            if (jsonObject.has(KEY_TARGET_TEMPERATURE)) {
                editor.putFloat(KEY_TARGET_TEMPERATURE, (float) jsonObject.getDouble(KEY_TARGET_TEMPERATURE));
            }
            if (jsonObject.has(KEY_TARGET_HUMIDITY)) {
                editor.putFloat(KEY_TARGET_HUMIDITY, (float) jsonObject.getDouble(KEY_TARGET_HUMIDITY));
            }

            // Import WiFi settings
            if (jsonObject.has(KEY_AP_SSID)) {
                editor.putString(KEY_AP_SSID, jsonObject.getString(KEY_AP_SSID));
            }
            if (jsonObject.has(KEY_AP_PASSWORD)) {
                editor.putString(KEY_AP_PASSWORD, jsonObject.getString(KEY_AP_PASSWORD));
            }
            if (jsonObject.has(KEY_AUTO_CONNECT_ENABLED)) {
                editor.putBoolean(KEY_AUTO_CONNECT_ENABLED, jsonObject.getBoolean(KEY_AUTO_CONNECT_ENABLED));
            }

            // Import Temperature PID settings - Sadece sıcaklık PID
            if (jsonObject.has(KEY_PID_MODE)) {
                editor.putString(KEY_PID_MODE, jsonObject.getString(KEY_PID_MODE));
            }
            if (jsonObject.has(KEY_TEMP_PID_KP)) {
                editor.putFloat(KEY_TEMP_PID_KP, (float) jsonObject.getDouble(KEY_TEMP_PID_KP));
            }
            if (jsonObject.has(KEY_TEMP_PID_KI)) {
                editor.putFloat(KEY_TEMP_PID_KI, (float) jsonObject.getDouble(KEY_TEMP_PID_KI));
            }
            if (jsonObject.has(KEY_TEMP_PID_KD)) {
                editor.putFloat(KEY_TEMP_PID_KD, (float) jsonObject.getDouble(KEY_TEMP_PID_KD));
            }
            if (jsonObject.has(KEY_PID_ENABLED)) {
                editor.putBoolean(KEY_PID_ENABLED, jsonObject.getBoolean(KEY_PID_ENABLED));
            }
            if (jsonObject.has(KEY_AUTO_TUNING_ENABLED)) {
                editor.putBoolean(KEY_AUTO_TUNING_ENABLED, jsonObject.getBoolean(KEY_AUTO_TUNING_ENABLED));
            }

            // Import PID advanced settings
            if (jsonObject.has(KEY_PID_SAMPLE_TIME)) {
                editor.putInt(KEY_PID_SAMPLE_TIME, jsonObject.getInt(KEY_PID_SAMPLE_TIME));
            }
            if (jsonObject.has(KEY_PID_OUTPUT_MIN)) {
                editor.putFloat(KEY_PID_OUTPUT_MIN, (float) jsonObject.getDouble(KEY_PID_OUTPUT_MIN));
            }
            if (jsonObject.has(KEY_PID_OUTPUT_MAX)) {
                editor.putFloat(KEY_PID_OUTPUT_MAX, (float) jsonObject.getDouble(KEY_PID_OUTPUT_MAX));
            }
            if (jsonObject.has(KEY_PID_REVERSE_DIRECTION)) {
                editor.putBoolean(KEY_PID_REVERSE_DIRECTION, jsonObject.getBoolean(KEY_PID_REVERSE_DIRECTION));
            }

            // Import alarm settings
            if (jsonObject.has(KEY_MASTER_ALARM_ENABLED)) {
                editor.putBoolean(KEY_MASTER_ALARM_ENABLED, jsonObject.getBoolean(KEY_MASTER_ALARM_ENABLED));
            }
            if (jsonObject.has(KEY_TEMP_LOW_ALARM)) {
                editor.putFloat(KEY_TEMP_LOW_ALARM, (float) jsonObject.getDouble(KEY_TEMP_LOW_ALARM));
            }
            if (jsonObject.has(KEY_TEMP_HIGH_ALARM)) {
                editor.putFloat(KEY_TEMP_HIGH_ALARM, (float) jsonObject.getDouble(KEY_TEMP_HIGH_ALARM));
            }
            if (jsonObject.has(KEY_HUMID_LOW_ALARM)) {
                editor.putFloat(KEY_HUMID_LOW_ALARM, (float) jsonObject.getDouble(KEY_HUMID_LOW_ALARM));
            }
            if (jsonObject.has(KEY_HUMID_HIGH_ALARM)) {
                editor.putFloat(KEY_HUMID_HIGH_ALARM, (float) jsonObject.getDouble(KEY_HUMID_HIGH_ALARM));
            }

            // Import calibration settings
            if (jsonObject.has(KEY_TEMP_OFFSET)) {
                editor.putFloat(KEY_TEMP_OFFSET, (float) jsonObject.getDouble(KEY_TEMP_OFFSET));
            }
            if (jsonObject.has(KEY_HUMIDITY_OFFSET)) {
                editor.putFloat(KEY_HUMIDITY_OFFSET, (float) jsonObject.getDouble(KEY_HUMIDITY_OFFSET));
            }
            if (jsonObject.has(KEY_TEMP_CALIBRATION_FACTOR)) {
                editor.putFloat(KEY_TEMP_CALIBRATION_FACTOR, (float) jsonObject.getDouble(KEY_TEMP_CALIBRATION_FACTOR));
            }
            if (jsonObject.has(KEY_HUMIDITY_CALIBRATION_FACTOR)) {
                editor.putFloat(KEY_HUMIDITY_CALIBRATION_FACTOR, (float) jsonObject.getDouble(KEY_HUMIDITY_CALIBRATION_FACTOR));
            }

            // Import motor settings
            if (jsonObject.has(KEY_MOTOR_TURNING_INTERVAL)) {
                editor.putInt(KEY_MOTOR_TURNING_INTERVAL, jsonObject.getInt(KEY_MOTOR_TURNING_INTERVAL));
            }
            if (jsonObject.has(KEY_MOTOR_TURNING_DURATION)) {
                editor.putInt(KEY_MOTOR_TURNING_DURATION, jsonObject.getInt(KEY_MOTOR_TURNING_DURATION));
            }
            if (jsonObject.has(KEY_MOTOR_TURNING_ENABLED)) {
                editor.putBoolean(KEY_MOTOR_TURNING_ENABLED, jsonObject.getBoolean(KEY_MOTOR_TURNING_ENABLED));
            }
            if (jsonObject.has(KEY_MOTOR_DIRECTION)) {
                editor.putString(KEY_MOTOR_DIRECTION, jsonObject.getString(KEY_MOTOR_DIRECTION));
            }

            // Import application settings
            if (jsonObject.has(KEY_AUTO_SYNC_ENABLED)) {
                editor.putBoolean(KEY_AUTO_SYNC_ENABLED, jsonObject.getBoolean(KEY_AUTO_SYNC_ENABLED));
            }
            if (jsonObject.has(KEY_SYNC_INTERVAL)) {
                editor.putInt(KEY_SYNC_INTERVAL, jsonObject.getInt(KEY_SYNC_INTERVAL));
            }
            if (jsonObject.has(KEY_DARK_MODE_ENABLED)) {
                editor.putBoolean(KEY_DARK_MODE_ENABLED, jsonObject.getBoolean(KEY_DARK_MODE_ENABLED));
            }
            if (jsonObject.has(KEY_LANGUAGE)) {
                editor.putString(KEY_LANGUAGE, jsonObject.getString(KEY_LANGUAGE));
            }
            if (jsonObject.has(KEY_UNIT_SYSTEM)) {
                editor.putString(KEY_UNIT_SYSTEM, jsonObject.getString(KEY_UNIT_SYSTEM));
            }

            editor.apply();
            return true;
        } catch (org.json.JSONException e) {
            return false;
        }
    }
}