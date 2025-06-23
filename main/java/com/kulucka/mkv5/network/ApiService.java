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

    // Temel cihaz durumu
    @GET("/api/status")
    Call<DeviceStatus> getStatus();

    // Sıcaklık ve nem kontrolleri
    @POST("/api/temperature")
    Call<ResponseBody> setTemperature(@Body Map<String, Object> params);

    @POST("/api/humidity")
    Call<ResponseBody> setHumidity(@Body Map<String, Object> params);

    // PID kontrolleri
    @POST("/api/pid")
    Call<ResponseBody> setPidParameters(@Body Map<String, Object> params);

    @GET("/api/pid/status")
    Call<PidStatusResponse> getPidStatus();

    // Motor kontrolleri
    @POST("/api/motor")
    Call<ResponseBody> setMotorSettings(@Body Map<String, Object> params);

    @POST("/api/motor/test")
    Call<MotorTestResponse> testMotor(@Body Map<String, Object> params);

    @GET("/api/motor/status")
    Call<MotorStatusResponse> getMotorStatus();

    // Alarm ayarları
    @POST("/api/alarm")
    Call<ResponseBody> setAlarmSettings(@Body Map<String, Object> params);

    // Kalibrasyon ayarları
    @POST("/api/calibration")
    Call<ResponseBody> setCalibration(@Body Map<String, Object> params);

    // Kuluçka ayarları
    @POST("/api/incubation")
    Call<ResponseBody> setIncubationSettings(@Body Map<String, Object> params);

    // WiFi işlemleri
    @GET("/api/wifi/networks")
    Call<WifiNetworksResponse> getWifiNetworks();

    @POST("/api/wifi/connect")
    Call<ResponseBody> connectToWifi(@Body Map<String, Object> params);

    @POST("/api/wifi/connect")
    Call<WifiModeChangeResponse> connectToWifiWithDetails(@Body Map<String, Object> params);

    @POST("/api/wifi/ap")
    Call<ResponseBody> switchToAPMode();

    @GET("/api/wifi/status")
    Call<WifiStatusResponse> getWifiStatus();

    @POST("/api/wifi/save")
    Call<ResponseBody> saveWifiSettings();

    @POST("/api/wifi/mode")
    Call<WifiModeChangeResponse> changeWifiMode(@Body Map<String, Object> params);

    @GET("/api/wifi/mode/status")
    Call<WifiModeChangeStatusResponse> getWifiModeChangeStatus();

    @GET("/api/wifi/credentials")
    Call<WifiCredentialsResponse> getWifiCredentials();

    // Sistem işlemleri
    @GET("/api/system/verify")
    Call<SystemVerificationResponse> getSystemVerification();

    @POST("/api/system/save")
    Call<SystemSaveResponse> saveSystem();

    @GET("/api/system/health")
    Call<SystemHealthResponse> getSystemHealth();

    @GET("/api/system/logs")
    Call<SystemLogsResponse> getSystemLogs();

    @POST("/api/system/action")
    Call<ResponseBody> systemAction(@Body Map<String, Object> params);

    @GET("/api/status/complete")
    Call<CompleteSystemStatusResponse> getCompleteSystemStatus();

    // Keşif ve test işlemleri
    @GET("/api/discovery")
    Call<DiscoveryResponse> getDiscoveryInfo();

    @GET("/api/ping")
    Call<ResponseBody> ping();

    @POST("/api/network/test")
    Call<NetworkTestResponse> testNetworkConnection(@Body Map<String, Object> params);

    // Manuel kuluçka parametreleri toplu güncelleme
    @POST("/api/incubation/manual")
    Call<ResponseBody> setManualIncubationParameters(@Body Map<String, Object> params);

    // Sensör detayları endpoint'i
    @GET("/api/sensors/details")
    Call<SensorDetailsResponse> getSensorDetails();

    // RTC işlemleri
    @GET("/api/rtc/status")
    Call<RTCStatusResponse> getRTCStatus();

    @POST("/api/rtc/time")
    Call<ResponseBody> setRTCTime(@Body Map<String, Object> params);

    @POST("/api/rtc/date")
    Call<ResponseBody> setRTCDate(@Body Map<String, Object> params);

    // Sensör detayları için response sınıfı
    public static class SensorDetailsResponse {
        private AverageValues average;
        private SensorInfo sensor1;
        private SensorInfo sensor2;
        private HealthInfo health;

        public static class AverageValues {
            private float temperature;
            private float humidity;

            public float getTemperature() { return temperature; }
            public void setTemperature(float temperature) { this.temperature = temperature; }

            public float getHumidity() { return humidity; }
            public void setHumidity(float humidity) { this.humidity = humidity; }
        }

        public static class SensorInfo {
            private String id;
            private String address;
            private float temperature;
            private float humidity;
            private boolean working;
            private CalibrationInfo calibration;

            public static class CalibrationInfo {
                private float temperature;
                private float humidity;

                public float getTemperature() { return temperature; }
                public void setTemperature(float temperature) { this.temperature = temperature; }

                public float getHumidity() { return humidity; }
                public void setHumidity(float humidity) { this.humidity = humidity; }
            }

            public String getId() { return id; }
            public void setId(String id) { this.id = id; }

            public String getAddress() { return address; }
            public void setAddress(String address) { this.address = address; }

            public float getTemperature() { return temperature; }
            public void setTemperature(float temperature) { this.temperature = temperature; }

            public float getHumidity() { return humidity; }
            public void setHumidity(float humidity) { this.humidity = humidity; }

            public boolean isWorking() { return working; }
            public void setWorking(boolean working) { this.working = working; }

            public CalibrationInfo getCalibration() { return calibration; }
            public void setCalibration(CalibrationInfo calibration) { this.calibration = calibration; }
        }

        public static class HealthInfo {
            private boolean sensorsWorking;
            private boolean allSensorsWorking;
            private boolean temperatureValid;
            private boolean humidityValid;

            public boolean isSensorsWorking() { return sensorsWorking; }
            public void setSensorsWorking(boolean sensorsWorking) { this.sensorsWorking = sensorsWorking; }

            public boolean isAllSensorsWorking() { return allSensorsWorking; }
            public void setAllSensorsWorking(boolean allSensorsWorking) { this.allSensorsWorking = allSensorsWorking; }

            public boolean isTemperatureValid() { return temperatureValid; }
            public void setTemperatureValid(boolean temperatureValid) { this.temperatureValid = temperatureValid; }

            public boolean isHumidityValid() { return humidityValid; }
            public void setHumidityValid(boolean humidityValid) { this.humidityValid = humidityValid; }
        }

        public AverageValues getAverage() { return average; }
        public void setAverage(AverageValues average) { this.average = average; }

        public SensorInfo getSensor1() { return sensor1; }
        public void setSensor1(SensorInfo sensor1) { this.sensor1 = sensor1; }

        public SensorInfo getSensor2() { return sensor2; }
        public void setSensor2(SensorInfo sensor2) { this.sensor2 = sensor2; }

        public HealthInfo getHealth() { return health; }
        public void setHealth(HealthInfo health) { this.health = health; }
    }

    // RTC durumu için response sınıfı
    public static class RTCStatusResponse {
        private String status;
        private String time;
        private String date;
        private long timestamp;
        private int errorCount;
        private RTCDetails details;

        public static class RTCDetails {
            private int hour;
            private int minute;
            private int second;
            private int day;
            private int month;
            private int year;

            public int getHour() { return hour; }
            public void setHour(int hour) { this.hour = hour; }

            public int getMinute() { return minute; }
            public void setMinute(int minute) { this.minute = minute; }

            public int getSecond() { return second; }
            public void setSecond(int second) { this.second = second; }

            public int getDay() { return day; }
            public void setDay(int day) { this.day = day; }

            public int getMonth() { return month; }
            public void setMonth(int month) { this.month = month; }

            public int getYear() { return year; }
            public void setYear(int year) { this.year = year; }
        }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

        public int getErrorCount() { return errorCount; }
        public void setErrorCount(int errorCount) { this.errorCount = errorCount; }

        public RTCDetails getDetails() { return details; }
        public void setDetails(RTCDetails details) { this.details = details; }
    }

    // Response sınıfları
    public static class WifiStatusResponse {
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

    public static class DiscoveryResponse {
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

    public static class WifiModeChangeResponse {
        private String status;
        private String message;
        private String newMode;
        private String ipAddress;
        private boolean connected;
        private PreservedData preservedData;
        private int estimatedTime;
        private boolean credentialsSaved;

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

        public int getEstimatedTime() { return estimatedTime; }
        public void setEstimatedTime(int estimatedTime) { this.estimatedTime = estimatedTime; }

        public boolean isCredentialsSaved() { return credentialsSaved; }
        public void setCredentialsSaved(boolean credentialsSaved) { this.credentialsSaved = credentialsSaved; }

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

    public static class WifiModeChangeStatusResponse {
        private String status;
        private String currentMode;
        private String targetMode;
        private int progress;
        private String message;
        private long estimatedTimeRemaining;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getCurrentMode() { return currentMode; }
        public void setCurrentMode(String currentMode) { this.currentMode = currentMode; }

        public String getTargetMode() { return targetMode; }
        public void setTargetMode(String targetMode) { this.targetMode = targetMode; }

        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = progress; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public long getEstimatedTimeRemaining() { return estimatedTimeRemaining; }
        public void setEstimatedTimeRemaining(long estimatedTimeRemaining) {
            this.estimatedTimeRemaining = estimatedTimeRemaining;
        }
    }

    public static class SystemVerificationResponse {
        private String status;
        private long timestamp;
        private long freeHeap;
        private WiFiInfo wifi;
        private SystemParameters parameters;
        private IncubationInfo incubation;
        private String firmwareVersion;
        private ReliabilityInfo reliability;

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

        public String getFirmwareVersion() { return firmwareVersion; }
        public void setFirmwareVersion(String firmwareVersion) { this.firmwareVersion = firmwareVersion; }

        public ReliabilityInfo getReliability() { return reliability; }
        public void setReliability(ReliabilityInfo reliability) { this.reliability = reliability; }

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

        public static class ReliabilityInfo {
            private long lastSave;
            private int pendingChanges;
            private boolean autoSaveEnabled;
            private boolean criticalParamsProtected;

            public long getLastSave() { return lastSave; }
            public void setLastSave(long lastSave) { this.lastSave = lastSave; }

            public int getPendingChanges() { return pendingChanges; }
            public void setPendingChanges(int pendingChanges) { this.pendingChanges = pendingChanges; }

            public boolean isAutoSaveEnabled() { return autoSaveEnabled; }
            public void setAutoSaveEnabled(boolean autoSaveEnabled) { this.autoSaveEnabled = autoSaveEnabled; }

            public boolean isCriticalParamsProtected() { return criticalParamsProtected; }
            public void setCriticalParamsProtected(boolean criticalParamsProtected) {
                this.criticalParamsProtected = criticalParamsProtected;
            }
        }
    }

    public static class MotorTestResponse {
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

    public static class MotorStatusResponse {

        private String status;
        private boolean isRunning;
        private int waitTime;
        private int runTime;
        private boolean testAvailable;
        private long lastRunTime;
        private int totalRuns;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public boolean isRunning() { return isRunning; }
        public void setRunning(boolean running) { isRunning = running; }

        public int getWaitTime() { return waitTime; }
        public void setWaitTime(int waitTime) { this.waitTime = waitTime; }

        public int getRunTime() { return runTime; }
        public void setRunTime(int runTime) { this.runTime = runTime; }

        public boolean isTestAvailable() { return testAvailable; }
        public void setTestAvailable(boolean testAvailable) { this.testAvailable = testAvailable; }

        public long getLastRunTime() { return lastRunTime; }
        public void setLastRunTime(long lastRunTime) { this.lastRunTime = lastRunTime; }

        public int getTotalRuns() { return totalRuns; }
        public void setTotalRuns(int totalRuns) { this.totalRuns = totalRuns; }
    }

    public static class TimingInfo {
        private int waitTime;
        private int runTime;
        private int waitTimeLeft;
        private int runTimeLeft;

        // Getter ve setter metodları
        public int getWaitTime() { return waitTime; }
        public void setWaitTime(int waitTime) { this.waitTime = waitTime; }

        public int getRunTime() { return runTime; }
        public void setRunTime(int runTime) { this.runTime = runTime; }

        public int getWaitTimeLeft() { return waitTimeLeft; }
        public void setWaitTimeLeft(int waitTimeLeft) { this.waitTimeLeft = waitTimeLeft; }

        public int getRunTimeLeft() { return runTimeLeft; }
        public void setRunTimeLeft(int runTimeLeft) { this.runTimeLeft = runTimeLeft; }
    }

    public static class PidStatusResponse {
        private String status;
        private int mode;
        private String modeString;
        private boolean active;
        private float kp;
        private float ki;
        private float kd;
        private float error;
        private float output;
        private boolean autoTuneActive;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public int getMode() { return mode; }
        public void setMode(int mode) { this.mode = mode; }

        public String getModeString() { return modeString; }
        public void setModeString(String modeString) { this.modeString = modeString; }

        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }

        public float getKp() { return kp; }
        public void setKp(float kp) { this.kp = kp; }

        public float getKi() { return ki; }
        public void setKi(float ki) { this.ki = ki; }

        public float getKd() { return kd; }
        public void setKd(float kd) { this.kd = kd; }

        public float getError() { return error; }
        public void setError(float error) { this.error = error; }

        public float getOutput() { return output; }
        public void setOutput(float output) { this.output = output; }

        public boolean isAutoTuneActive() { return autoTuneActive; }
        public void setAutoTuneActive(boolean autoTuneActive) { this.autoTuneActive = autoTuneActive; }
    }

    public static class SystemSaveResponse {
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

    public static class WifiCredentialsResponse {
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

    public static class SystemHealthResponse {
        private String status;
        private long uptime;
        private long freeHeap;
        private long totalHeap;
        private int heapFragmentation;
        private int wifiRssi;
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

        public long getTotalHeap() { return totalHeap; }
        public void setTotalHeap(long totalHeap) { this.totalHeap = totalHeap; }

        public int getHeapFragmentation() { return heapFragmentation; }
        public void setHeapFragmentation(int heapFragmentation) {
            this.heapFragmentation = heapFragmentation;
        }

        public int getWifiRssi() { return wifiRssi; }
        public void setWifiRssi(int wifiRssi) { this.wifiRssi = wifiRssi; }

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

    public static class NetworkTestResponse {
        private String status;
        private String targetIP;
        private boolean reachable;
        private long pingTime;
        private String error;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getTargetIP() { return targetIP; }
        public void setTargetIP(String targetIP) { this.targetIP = targetIP; }

        public boolean isReachable() { return reachable; }
        public void setReachable(boolean reachable) { this.reachable = reachable; }

        public long getPingTime() { return pingTime; }
        public void setPingTime(long pingTime) { this.pingTime = pingTime; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }

    public static class SystemLogsResponse {
        private String status;
        private List<LogEntry> logs;
        private int totalCount;
        private long timestamp;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public List<LogEntry> getLogs() { return logs; }
        public void setLogs(List<LogEntry> logs) { this.logs = logs; }

        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

        public static class LogEntry {
            private long timestamp;
            private String level;
            private String message;
            private String source;

            public long getTimestamp() { return timestamp; }
            public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

            public String getLevel() { return level; }
            public void setLevel(String level) { this.level = level; }

            public String getMessage() { return message; }
            public void setMessage(String message) { this.message = message; }

            public String getSource() { return source; }
            public void setSource(String source) { this.source = source; }
        }
    }

    public static class CompleteSystemStatusResponse {
        private String status;
        private DeviceStatus deviceStatus;
        private SystemHealthResponse systemHealth;
        private WifiStatusResponse wifiStatus;
        private MotorStatusResponse motorStatus;
        private PidStatusResponse pidStatus;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public DeviceStatus getDeviceStatus() { return deviceStatus; }
        public void setDeviceStatus(DeviceStatus deviceStatus) { this.deviceStatus = deviceStatus; }

        public SystemHealthResponse getSystemHealth() { return systemHealth; }
        public void setSystemHealth(SystemHealthResponse systemHealth) { this.systemHealth = systemHealth; }

        public WifiStatusResponse getWifiStatus() { return wifiStatus; }
        public void setWifiStatus(WifiStatusResponse wifiStatus) { this.wifiStatus = wifiStatus; }

        public MotorStatusResponse getMotorStatus() { return motorStatus; }
        public void setMotorStatus(MotorStatusResponse motorStatus) { this.motorStatus = motorStatus; }

        public PidStatusResponse getPidStatus() { return pidStatus; }
        public void setPidStatus(PidStatusResponse pidStatus) { this.pidStatus = pidStatus; }
    }

    public static class WifiNetworksResponse {
        private List<WifiNetwork> networks;

        public List<WifiNetwork> getNetworks() {
            return networks;
        }

        public void setNetworks(List<WifiNetwork> networks) {
            this.networks = networks;
        }
    }
}