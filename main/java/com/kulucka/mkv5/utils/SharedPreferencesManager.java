package com.kulucka.mkv5.utils;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

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

    // Alarm offset metodları
    public void saveTempHighOffset(float offset) {
        sharedPreferences.edit().putFloat("temp_high_offset", offset).apply();
    }

    public float getTempHighOffset() {
        return sharedPreferences.getFloat("temp_high_offset", 1.0f);
    }

    public void saveTempLowOffset(float offset) {
        sharedPreferences.edit().putFloat("temp_low_offset", offset).apply();
    }

    public float getTempLowOffset() {
        return sharedPreferences.getFloat("temp_low_offset", 1.0f);
    }

    public void saveHumidHighOffset(float offset) {
        sharedPreferences.edit().putFloat("humid_high_offset", offset).apply();
    }

    public float getHumidHighOffset() {
        return sharedPreferences.getFloat("humid_high_offset", 10.0f);
    }

    public void saveHumidLowOffset(float offset) {
        sharedPreferences.edit().putFloat("humid_low_offset", offset).apply();
    }

    public float getHumidLowOffset() {
        return sharedPreferences.getFloat("humid_low_offset", 10.0f);
    }

    // WiFi ağ bilgilerini kaydetme
    public void saveWifiNetwork(String ssid, String password) {
        if (ssid == null || ssid.isEmpty()) {
            // Boş SSID ile çağrıldıysa son WiFi bilgilerini temizle
            sharedPreferences.edit()
                    .remove("last_wifi_ssid")
                    .remove("last_wifi_password")
                    .apply();
            return;
        }

        String networkKey = Constants.KEY_WIFI_PASSWORDS_PREFIX + ssid;
        sharedPreferences.edit()
                .putString(networkKey, password)
                .apply();

        // Son bağlanılan ağı da kaydet
        sharedPreferences.edit()
                .putString("last_wifi_ssid", ssid)
                .putString("last_wifi_password", password)
                .apply();

        // Kayıtlı ağlar listesine ekle
        Set<String> savedNetworks = getSavedWifiNetworks();
        savedNetworks.add(ssid);
        sharedPreferences.edit()
                .putStringSet("saved_wifi_networks", savedNetworks)
                .apply();
    }

    public String getWifiPassword(String ssid) {
        if (ssid == null || ssid.isEmpty()) {
            return "";
        }
        String networkKey = Constants.KEY_WIFI_PASSWORDS_PREFIX + ssid;
        return sharedPreferences.getString(networkKey, "");
    }

    public boolean hasWifiPassword(String ssid) {
        if (ssid == null || ssid.isEmpty()) {
            return false;
        }
        String networkKey = Constants.KEY_WIFI_PASSWORDS_PREFIX + ssid;
        return sharedPreferences.contains(networkKey);
    }

    public String getLastWifiSSID() {
        return sharedPreferences.getString("last_wifi_ssid", "");
    }

    public String getLastWifiPassword() {
        return sharedPreferences.getString("last_wifi_password", "");
    }

    // WiFi ağını kaldır
    public void removeWifiNetwork(String ssid) {
        if (ssid == null || ssid.isEmpty()) {
            return;
        }

        String networkKey = Constants.KEY_WIFI_PASSWORDS_PREFIX + ssid;

        // Kayıtlı ağlar listesinden kaldır
        Set<String> savedNetworks = getSavedWifiNetworks();
        savedNetworks.remove(ssid);

        sharedPreferences.edit()
                .remove(networkKey)
                .putStringSet("saved_wifi_networks", savedNetworks)
                .apply();
    }

    // Tüm kayıtlı WiFi ağlarını getir
    public Set<String> getSavedWifiNetworks() {
        return new HashSet<>(sharedPreferences.getStringSet("saved_wifi_networks", new HashSet<>()));
    }

    // Tüm WiFi ayarlarını sıfırla
    public void clearAllWifiSettings() {
        // Tüm kayıtlı WiFi şifrelerini temizle
        Set<String> savedNetworks = getSavedWifiNetworks();
        SharedPreferences.Editor editor = sharedPreferences.edit();

        for (String ssid : savedNetworks) {
            String networkKey = Constants.KEY_WIFI_PASSWORDS_PREFIX + ssid;
            editor.remove(networkKey);
        }

        // WiFi ile ilgili tüm ayarları sıfırla
        editor.remove("saved_wifi_networks")
                .remove("last_wifi_ssid")
                .remove("last_wifi_password")
                .putString(Constants.KEY_DEVICE_IP, Constants.DEFAULT_AP_IP)
                .putInt(Constants.KEY_DEVICE_PORT, Constants.DEFAULT_PORT)
                .putInt(Constants.KEY_CONNECTION_MODE, Constants.MODE_AP)
                .putString(Constants.KEY_AP_SSID, Constants.DEFAULT_AP_SSID)
                .putString(Constants.KEY_AP_PASSWORD, Constants.DEFAULT_AP_PASSWORD)
                .apply();
    }

    // Station WiFi bilgilerini kaydet/getir
    public void saveStationCredentials(String ssid, String password) {
        sharedPreferences.edit()
                .putString("station_ssid", ssid)
                .putString("station_password", password)
                .apply();
    }

    public String getStationSSID() {
        return sharedPreferences.getString("station_ssid", "");
    }

    public String getStationPassword() {
        return sharedPreferences.getString("station_password", "");
    }

    public void clearStationCredentials() {
        sharedPreferences.edit()
                .remove("station_ssid")
                .remove("station_password")
                .apply();
    }

    // Bağlantı denemesi sayacı
    public void incrementConnectionAttempt() {
        int attempts = getConnectionAttempts();
        sharedPreferences.edit()
                .putInt("connection_attempts", attempts + 1)
                .apply();
    }

    public int getConnectionAttempts() {
        return sharedPreferences.getInt("connection_attempts", 0);
    }

    public void resetConnectionAttempts() {
        sharedPreferences.edit()
                .putInt("connection_attempts", 0)
                .apply();
    }

    // Son başarılı bağlantı zamanı
    public void saveLastSuccessfulConnection(long timestamp) {
        sharedPreferences.edit()
                .putLong("last_successful_connection", timestamp)
                .apply();
    }

    public long getLastSuccessfulConnection() {
        return sharedPreferences.getLong("last_successful_connection", 0);
    }

    // Debug ve sorun giderme için
    public String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Device IP: ").append(getDeviceIp()).append("\n");
        sb.append("Device Port: ").append(getDevicePort()).append("\n");
        sb.append("Connection Mode: ").append(getConnectionMode() == Constants.MODE_AP ? "AP" : "Station").append("\n");
        sb.append("AP SSID: ").append(getAPSSID()).append("\n");
        sb.append("Station SSID: ").append(getStationSSID()).append("\n");
        sb.append("Saved Networks: ").append(getSavedWifiNetworks().size()).append("\n");
        sb.append("Connection Attempts: ").append(getConnectionAttempts()).append("\n");

        long lastConnection = getLastSuccessfulConnection();
        if (lastConnection > 0) {
            long timeSince = System.currentTimeMillis() - lastConnection;
            sb.append("Last Successful Connection: ").append(timeSince / 1000).append(" seconds ago\n");
        } else {
            sb.append("Last Successful Connection: Never\n");
        }

        return sb.toString();
    }
}