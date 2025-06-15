package com.kulucka.mkv5.network;

import com.kulucka.mkv5.models.DeviceStatus;
import com.kulucka.mkv5.models.WifiNetwork;

import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {

    @GET("/api/status")
    Call<DeviceStatus> getStatus();

    @POST("/api/temperature")
    Call<ResponseBody> setTemperature(@Body Map<String, Object> params);

    @POST("/api/humidity")
    Call<ResponseBody> setHumidity(@Body Map<String, Object> params);

    @POST("/api/pid")
    Call<ResponseBody> setPidParameters(@Body Map<String, Object> params);

    @POST("/api/motor")
    Call<ResponseBody> setMotorSettings(@Body Map<String, Object> params);

    @POST("/api/alarm")
    Call<ResponseBody> setAlarmSettings(@Body Map<String, Object> params);

    @POST("/api/calibration")
    Call<ResponseBody> setCalibration(@Body Map<String, Object> params);

    @POST("/api/incubation")
    Call<ResponseBody> setIncubationSettings(@Body Map<String, Object> params);

    @GET("/api/wifi/networks")
    Call<WifiNetworksResponse> getWifiNetworks();

    @POST("/api/wifi/connect")
    Call<ResponseBody> connectToWifi(@Body Map<String, Object> params);

    @POST("/api/wifi/ap")
    Call<ResponseBody> switchToAPMode();

    // WiFi durumu
    @GET("/api/wifi/status")
    Call<WifiStatusResponse> getWifiStatus();

    // WiFi ayarlarını kaydet
    @POST("/api/wifi/save")
    Call<ResponseBody> saveWifiSettings();

    // WiFi ayarlarını al
    @GET("/api/wifi/settings")
    Call<WifiSettingsResponse> getWifiSettings();

    // Response sınıfları
    class WifiStatusResponse {
        private String mode;
        private boolean connected;
        private String ssid;
        private String ip;
        private int rssi;
        private String status;

        // Getter ve setter metodları
        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }

        public boolean isConnected() { return connected; }
        public void setConnected(boolean connected) { this.connected = connected; }

        public String getSsid() { return ssid; }
        public void setSsid(String ssid) { this.ssid = ssid; }

        public String getIp() { return ip; }
        public void setIp(String ip) { this.ip = ip; }

        public int getRssi() { return rssi; }
        public void setRssi(int rssi) { this.rssi = rssi; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    class WifiSettingsResponse {
        private int mode;
        private String apSSID;
        private String stationSSID;
        private boolean hasPassword;

        // Getter ve setter metodları
        public int getMode() { return mode; }
        public void setMode(int mode) { this.mode = mode; }

        public String getApSSID() { return apSSID; }
        public void setApSSID(String apSSID) { this.apSSID = apSSID; }

        public String getStationSSID() { return stationSSID; }
        public void setStationSSID(String stationSSID) { this.stationSSID = stationSSID; }

        public boolean isHasPassword() { return hasPassword; }
        public void setHasPassword(boolean hasPassword) { this.hasPassword = hasPassword; }
    }

    // Response wrapper class for WiFi networks
    class WifiNetworksResponse {
        private List<WifiNetwork> networks;

        public List<WifiNetwork> getNetworks() {
            return networks;
        }

        public void setNetworks(List<WifiNetwork> networks) {
            this.networks = networks;
        }
    }
}