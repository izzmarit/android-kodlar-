package com.kulucka.mkv5.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesManager {
    private static SharedPreferencesManager instance;
    private final SharedPreferences sharedPreferences;

    private SharedPreferencesManager(Context context) {
        sharedPreferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
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
}