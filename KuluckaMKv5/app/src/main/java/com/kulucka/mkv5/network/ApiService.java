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

    @GET("/api/wifi/status")
    Call<WifiStatusResponse> getWifiStatus();

    @POST("/api/wifi/save")
    Call<ResponseBody> saveWifiSettings();

    @GET("/api/discovery")
    Call<DiscoveryResponse> getDiscoveryInfo();

    @GET("/api/ping")
    Call<ResponseBody> ping();

    @POST("/api/wifi/mode")
    Call<WifiModeChangeResponse> changeWifiMode(@Body Map<String, Object> params);

    @GET("/api/system/verify")
    Call<SystemVerificationResponse> getSystemVerification();

    @POST("/api/motor/test")
    Call<MotorTestResponse> testMotor(@Body Map<String, Object> params);

    @POST("/api/system/save")
    Call<SystemSaveResponse> saveSystem();

    @GET("/api/wifi/credentials")
    Call<WifiCredentialsResponse> getWifiCredentials();

    @GET("/api/system/health")
    Call<SystemHealthResponse> getSystemHealth();

    // Response sınıfları
    class WifiStatusResponse {
        private String mode;
        private boolean connected;
        private String ssid;
        private String ip;
        private int rssi;
        private String status;

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

    class MotorTestResponse {
        private String status;
        private String message;
        private int duration;
        private boolean motorState;
        private long timestamp;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public int getDuration() { return duration; }
        public void setDuration(int duration) { this.duration = duration; }

        public boolean isMotorState() { return motorState; }
        public void setMotorState(boolean motorState) { this.motorState = motorState; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }

    class SystemSaveResponse {
        private String status;
        private String message;
        private int pendingChanges;
        private long timestamp;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public int getPendingChanges() { return pendingChanges; }
        public void setPendingChanges(int pendingChanges) { this.pendingChanges = pendingChanges; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }

    class WifiCredentialsResponse {
        private String currentMode;
        private String apSSID;
        private boolean hasStationCredentials;
        private String stationSSID;
        private boolean stationSaved;
        private int stationPasswordLength;
        private boolean connected;
        private String ipAddress;

        public String getCurrentMode() { return currentMode; }
        public void setCurrentMode(String currentMode) { this.currentMode = currentMode; }

        public String getApSSID() { return apSSID; }
        public void setApSSID(String apSSID) { this.apSSID = apSSID; }

        public boolean isHasStationCredentials() { return hasStationCredentials; }
        public void setHasStationCredentials(boolean hasStationCredentials) {
            this.hasStationCredentials = hasStationCredentials;
        }

        public String getStationSSID() { return stationSSID; }
        public void setStationSSID(String stationSSID) { this.stationSSID = stationSSID; }

        public boolean isStationSaved() { return stationSaved; }
        public void setStationSaved(boolean stationSaved) { this.stationSaved = stationSaved; }

        public int getStationPasswordLength() { return stationPasswordLength; }
        public void setStationPasswordLength(int stationPasswordLength) {
            this.stationPasswordLength = stationPasswordLength;
        }

        public boolean isConnected() { return connected; }
        public void setConnected(boolean connected) { this.connected = connected; }

        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    }

    class SystemHealthResponse {
        private String status;
        private long uptime;
        private long freeHeap;
        private int heapFragmentation;
        private StorageInfo storage;
        private SensorsInfo sensors;
        private ControlInfo control;
        private WiFiInfo wifi;
        private IncubationInfo incubation;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public long getUptime() { return uptime; }
        public void setUptime(long uptime) { this.uptime = uptime; }

        public long getFreeHeap() { return freeHeap; }
        public void setFreeHeap(long freeHeap) { this.freeHeap = freeHeap; }

        public int getHeapFragmentation() { return heapFragmentation; }
        public void setHeapFragmentation(int heapFragmentation) {
            this.heapFragmentation = heapFragmentation;
        }

        public StorageInfo getStorage() { return storage; }
        public void setStorage(StorageInfo storage) { this.storage = storage; }

        public SensorsInfo getSensors() { return sensors; }
        public void setSensors(SensorsInfo sensors) { this.sensors = sensors; }

        public ControlInfo getControl() { return control; }
        public void setControl(ControlInfo control) { this.control = control; }

        public WiFiInfo getWifi() { return wifi; }
        public void setWifi(WiFiInfo wifi) { this.wifi = wifi; }

        public IncubationInfo getIncubation() { return incubation; }
        public void setIncubation(IncubationInfo incubation) { this.incubation = incubation; }

        public static class StorageInfo {
            private boolean initialized;
            private int pendingChanges;
            private long lastSaveTime;
            private boolean criticalParameters;

            public boolean isInitialized() { return initialized; }
            public void setInitialized(boolean initialized) { this.initialized = initialized; }

            public int getPendingChanges() { return pendingChanges; }
            public void setPendingChanges(int pendingChanges) { this.pendingChanges = pendingChanges; }

            public long getLastSaveTime() { return lastSaveTime; }
            public void setLastSaveTime(long lastSaveTime) { this.lastSaveTime = lastSaveTime; }

            public boolean isCriticalParameters() { return criticalParameters; }
            public void setCriticalParameters(boolean criticalParameters) {
                this.criticalParameters = criticalParameters;
            }
        }

        public static class SensorsInfo {
            private float temperature;
            private float humidity;
            private boolean tempValid;
            private boolean humidValid;

            public float getTemperature() { return temperature; }
            public void setTemperature(float temperature) { this.temperature = temperature; }

            public float getHumidity() { return humidity; }
            public void setHumidity(float humidity) { this.humidity = humidity; }

            public boolean isTempValid() { return tempValid; }
            public void setTempValid(boolean tempValid) { this.tempValid = tempValid; }

            public boolean isHumidValid() { return humidValid; }
            public void setHumidValid(boolean humidValid) { this.humidValid = humidValid; }
        }

        public static class ControlInfo {
            private int pidMode;
            private boolean heaterState;
            private boolean humidifierState;
            private boolean motorState;
            private boolean alarmEnabled;

            public int getPidMode() { return pidMode; }
            public void setPidMode(int pidMode) { this.pidMode = pidMode; }

            public boolean isHeaterState() { return heaterState; }
            public void setHeaterState(boolean heaterState) { this.heaterState = heaterState; }

            public boolean isHumidifierState() { return humidifierState; }
            public void setHumidifierState(boolean humidifierState) {
                this.humidifierState = humidifierState;
            }

            public boolean isMotorState() { return motorState; }
            public void setMotorState(boolean motorState) { this.motorState = motorState; }

            public boolean isAlarmEnabled() { return alarmEnabled; }
            public void setAlarmEnabled(boolean alarmEnabled) { this.alarmEnabled = alarmEnabled; }
        }

        public static class WiFiInfo {
            private String mode;
            private boolean connected;
            private int rssi;
            private String ip;

            public String getMode() { return mode; }
            public void setMode(String mode) { this.mode = mode; }

            public boolean isConnected() { return connected; }
            public void setConnected(boolean connected) { this.connected = connected; }

            public int getRssi() { return rssi; }
            public void setRssi(int rssi) { this.rssi = rssi; }

            public String getIp() { return ip; }
            public void setIp(String ip) { this.ip = ip; }
        }

        public static class IncubationInfo {
            private boolean running;
            private int currentDay;
            private int totalDays;
            private boolean completed;

            public boolean isRunning() { return running; }
            public void setRunning(boolean running) { this.running = running; }

            public int getCurrentDay() { return currentDay; }
            public void setCurrentDay(int currentDay) { this.currentDay = currentDay; }

            public int getTotalDays() { return totalDays; }
            public void setTotalDays(int totalDays) { this.totalDays = totalDays; }

            public boolean isCompleted() { return completed; }
            public void setCompleted(boolean completed) { this.completed = completed; }
        }
    }

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