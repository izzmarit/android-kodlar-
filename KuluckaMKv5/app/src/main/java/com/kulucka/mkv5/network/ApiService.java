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

    // Discovery endpoint
    @GET("/api/discovery")
    Call<DiscoveryResponse> getDiscoveryInfo();

    // Ping endpoint
    @GET("/api/ping")
    Call<ResponseBody> ping();

    // YENİ: WiFi mod değişimi endpoint'i
    @POST("/api/wifi/mode")
    Call<WifiModeChangeResponse> changeWifiMode(@Body Map<String, Object> params);

    // YENİ: Sistem durumu doğrulama endpoint'i
    @GET("/api/system/verify")
    Call<SystemVerificationResponse> getSystemVerification();

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

    // Discovery response
    class DiscoveryResponse {
        private String device;
        private String version;
        private String ip;
        private String mode;
        private int port;

        public String getDevice() { return device; }
        public void setDevice(String device) { this.device = device; }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }

        public String getIp() { return ip; }
        public void setIp(String ip) { this.ip = ip; }

        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }

        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
    }

    // YENİ: WiFi mod değişimi yanıt sınıfı
    class WifiModeChangeResponse {
        private String status;
        private String message;
        private String newMode;
        private String ipAddress;
        private boolean connected;
        private PreservedData preservedData;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getNewMode() { return newMode; }
        public void setNewMode(String newMode) { this.newMode = newMode; }

        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

        public boolean isConnected() { return connected; }
        public void setConnected(boolean connected) { this.connected = connected; }

        public PreservedData getPreservedData() { return preservedData; }
        public void setPreservedData(PreservedData preservedData) { this.preservedData = preservedData; }

        public static class PreservedData {
            private float temperature;
            private float humidity;
            private float targetTemp;
            private float targetHumid;
            private boolean heaterState;
            private boolean humidifierState;
            private boolean motorState;

            // Getter ve setter metodları
            public float getTemperature() { return temperature; }
            public void setTemperature(float temperature) { this.temperature = temperature; }

            public float getHumidity() { return humidity; }
            public void setHumidity(float humidity) { this.humidity = humidity; }

            public float getTargetTemp() { return targetTemp; }
            public void setTargetTemp(float targetTemp) { this.targetTemp = targetTemp; }

            public float getTargetHumid() { return targetHumid; }
            public void setTargetHumid(float targetHumid) { this.targetHumid = targetHumid; }

            public boolean isHeaterState() { return heaterState; }
            public void setHeaterState(boolean heaterState) { this.heaterState = heaterState; }

            public boolean isHumidifierState() { return humidifierState; }
            public void setHumidifierState(boolean humidifierState) { this.humidifierState = humidifierState; }

            public boolean isMotorState() { return motorState; }
            public void setMotorState(boolean motorState) { this.motorState = motorState; }
        }
    }

    // YENİ: Sistem doğrulama yanıt sınıfı
    class SystemVerificationResponse {
        private String status;
        private long timestamp;
        private long freeHeap;
        private WiFiInfo wifi;
        private SystemParameters parameters;
        private IncubationInfo incubation;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

        public long getFreeHeap() { return freeHeap; }
        public void setFreeHeap(long freeHeap) { this.freeHeap = freeHeap; }

        public WiFiInfo getWifi() { return wifi; }
        public void setWifi(WiFiInfo wifi) { this.wifi = wifi; }

        public SystemParameters getParameters() { return parameters; }
        public void setParameters(SystemParameters parameters) { this.parameters = parameters; }

        public IncubationInfo getIncubation() { return incubation; }
        public void setIncubation(IncubationInfo incubation) { this.incubation = incubation; }

        public static class WiFiInfo {
            private String mode;
            private boolean connected;
            private String ssid;
            private String ip;
            private int rssi;

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
        }

        public static class SystemParameters {
            private float temperature;
            private float humidity;
            private float targetTemp;
            private float targetHumid;
            private boolean heaterState;
            private boolean humidifierState;
            private boolean motorState;
            private boolean alarmEnabled;
            private int pidMode;

            // Getter ve setter metodları
            public float getTemperature() { return temperature; }
            public void setTemperature(float temperature) { this.temperature = temperature; }

            public float getHumidity() { return humidity; }
            public void setHumidity(float humidity) { this.humidity = humidity; }

            public float getTargetTemp() { return targetTemp; }
            public void setTargetTemp(float targetTemp) { this.targetTemp = targetTemp; }

            public float getTargetHumid() { return targetHumid; }
            public void setTargetHumid(float targetHumid) { this.targetHumid = targetHumid; }

            public boolean isHeaterState() { return heaterState; }
            public void setHeaterState(boolean heaterState) { this.heaterState = heaterState; }

            public boolean isHumidifierState() { return humidifierState; }
            public void setHumidifierState(boolean humidifierState) { this.humidifierState = humidifierState; }

            public boolean isMotorState() { return motorState; }
            public void setMotorState(boolean motorState) { this.motorState = motorState; }

            public boolean isAlarmEnabled() { return alarmEnabled; }
            public void setAlarmEnabled(boolean alarmEnabled) { this.alarmEnabled = alarmEnabled; }

            public int getPidMode() { return pidMode; }
            public void setPidMode(int pidMode) { this.pidMode = pidMode; }
        }

        public static class IncubationInfo {
            private boolean running;
            private String type;
            private int currentDay;
            private int totalDays;
            private boolean completed;

            public boolean isRunning() { return running; }
            public void setRunning(boolean running) { this.running = running; }

            public String getType() { return type; }
            public void setType(String type) { this.type = type; }

            public int getCurrentDay() { return currentDay; }
            public void setCurrentDay(int currentDay) { this.currentDay = currentDay; }

            public int getTotalDays() { return totalDays; }
            public void setTotalDays(int totalDays) { this.totalDays = totalDays; }

            public boolean isCompleted() { return completed; }
            public void setCompleted(boolean completed) { this.completed = completed; }
        }
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