package com.kulucka.mkv5.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesManager {
    private static SharedPreferencesManager instance;
    private final SharedPreferences sharedPreferences;

    private SharedPreferencesManager(Context context) {
        sharedPreferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void removeWifiNetwork(String ssid) {
        sharedPreferences.edit()
                .remove("wifi_passwords_" + ssid)
                .apply();
    }

    public static synchronized SharedPreferencesManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPreferencesManager(context);
        }
        return instance;
    }

    public void saveDeviceIp(String ip) {
        sharedPreferences.edit().putString(Constants.KEY_DEVICE_IP, ip).apply();
    }

    public String getDeviceIp() {
        return sharedPreferences.getString(Constants.KEY_DEVICE_IP, Constants.DEFAULT_AP_IP);
    }

    public void saveDevicePort(int port) {
        sharedPreferences.edit().putInt(Constants.KEY_DEVICE_PORT, port).apply();
    }

    public int getDevicePort() {
        return sharedPreferences.getInt(Constants.KEY_DEVICE_PORT, Constants.DEFAULT_PORT);
    }

    public void saveConnectionMode(int mode) {
        sharedPreferences.edit().putInt(Constants.KEY_CONNECTION_MODE, mode).apply();
    }

    public int getConnectionMode() {
        return sharedPreferences.getInt(Constants.KEY_CONNECTION_MODE, Constants.MODE_AP);
    }

    public void saveAPCredentials(String ssid, String password) {
        sharedPreferences.edit()
                .putString(Constants.KEY_AP_SSID, ssid)
                .putString(Constants.KEY_AP_PASSWORD, password)
                .apply();
    }

    public String getAPSSID() {
        return sharedPreferences.getString(Constants.KEY_AP_SSID, Constants.DEFAULT_AP_SSID);
    }

    public String getAPPassword() {
        return sharedPreferences.getString(Constants.KEY_AP_PASSWORD, Constants.DEFAULT_AP_PASSWORD);
    }

    public void saveLastUpdateTime(long timestamp) {
        sharedPreferences.edit().putLong(Constants.KEY_LAST_UPDATE_TIME, timestamp).apply();
    }

    public long getLastUpdateTime() {
        return sharedPreferences.getLong(Constants.KEY_LAST_UPDATE_TIME, 0);
    }

    public void clearAll() {
        sharedPreferences.edit().clear().apply();
    }

    public void saveTempHighOffset(float offset) {
        sharedPreferences.edit().putFloat("temp_high_offset", offset).apply();
    }

    public float getTempHighOffset() {
        return sharedPreferences.getFloat("temp_high_offset", 1.0f); // Varsayılan 1.0
    }

    public void saveTempLowOffset(float offset) {
        sharedPreferences.edit().putFloat("temp_low_offset", offset).apply();
    }

    public float getTempLowOffset() {
        return sharedPreferences.getFloat("temp_low_offset", 1.0f); // Varsayılan 1.0
    }

    public void saveHumidHighOffset(float offset) {
        sharedPreferences.edit().putFloat("humid_high_offset", offset).apply();
    }

    public float getHumidHighOffset() {
        return sharedPreferences.getFloat("humid_high_offset", 10.0f); // Varsayılan 10
    }

    public void saveHumidLowOffset(float offset) {
        sharedPreferences.edit().putFloat("humid_low_offset", offset).apply();
    }

    public float getHumidLowOffset() {
        return sharedPreferences.getFloat("humid_low_offset", 10.0f); // Varsayılan 10
    }

    // WiFi ağ bilgilerini kaydetme
    public void saveWifiNetwork(String ssid, String password) {
        String networkKey = "wifi_network_" + ssid;
        sharedPreferences.edit()
                .putString(networkKey + "_ssid", ssid)
                .putString(networkKey + "_password", password)
                .apply();

        // Son bağlanılan ağı da kaydet
        sharedPreferences.edit()
                .putString("last_wifi_ssid", ssid)
                .putString("last_wifi_password", password)
                .apply();
    }

    public String getWifiPassword(String ssid) {
        String networkKey = "wifi_network_" + ssid;
        return sharedPreferences.getString(networkKey + "_password", "");
    }

    public boolean hasWifiPassword(String ssid) {
        String networkKey = "wifi_network_" + ssid;
        return sharedPreferences.contains(networkKey + "_password");
    }

    public String getLastWifiSSID() {
        return sharedPreferences.getString("last_wifi_ssid", "");
    }

    public String getLastWifiPassword() {
        return sharedPreferences.getString("last_wifi_password", "");
    }
}