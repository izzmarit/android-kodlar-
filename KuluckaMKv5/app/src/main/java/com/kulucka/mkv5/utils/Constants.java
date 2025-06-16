package com.kulucka.mkv5.utils;

public class Constants {
    // Shared Preferences Keys
    public static final String PREFS_NAME = "KuluckaMKv5Prefs";
    public static final String KEY_DEVICE_IP = "device_ip";
    public static final String KEY_DEVICE_PORT = "device_port";
    public static final String KEY_CONNECTION_MODE = "connection_mode";
    public static final String KEY_AP_SSID = "ap_ssid";
    public static final String KEY_AP_PASSWORD = "ap_password";
    public static final String KEY_LAST_UPDATE_TIME = "last_update_time";

    // Default Values
    public static final String DEFAULT_AP_SSID = "KuluckaMK";
    public static final String DEFAULT_AP_PASSWORD = "kulucka12345";
    public static final String DEFAULT_AP_IP = "192.168.4.1";
    public static final int DEFAULT_PORT = 80;

    // Connection Modes
    public static final int MODE_AP = 0;
    public static final int MODE_STATION = 1;

    // API Endpoints
    public static final String API_STATUS = "/api/status";
    public static final String API_SETTINGS = "/api/settings";
    public static final String API_TEMPERATURE = "/api/temperature";
    public static final String API_HUMIDITY = "/api/humidity";
    public static final String API_PID = "/api/pid";
    public static final String API_MOTOR = "/api/motor";
    public static final String API_ALARM = "/api/alarm";
    public static final String API_CALIBRATION = "/api/calibration";
    public static final String API_INCUBATION = "/api/incubation";
    public static final String API_WIFI_NETWORKS = "/api/wifi/networks";
    public static final String API_WIFI_CONNECT = "/api/wifi/connect";
    public static final String API_WIFI_AP = "/api/wifi/ap";
    public static final String API_WIFI_MODE = "/api/wifi/mode";
    public static final String API_SYSTEM_VERIFY = "/api/system/verify";

    // Incubation Types
    public static final int TYPE_CHICKEN = 0;
    public static final int TYPE_QUAIL = 1;
    public static final int TYPE_GOOSE = 2;
    public static final int TYPE_MANUAL = 3;

    // PID Modes
    public static final int PID_MODE_OFF = 0;
    public static final int PID_MODE_MANUAL = 1;
    public static final int PID_MODE_AUTO = 2;

    // Update Intervals
    public static final long STATUS_UPDATE_INTERVAL = 2000; // 2 seconds
    public static final long BACKGROUND_UPDATE_INTERVAL = 60000; // 1 minute
    public static final long RECONNECT_INTERVAL = 30000; // 30 seconds

    // Notification
    public static final int NOTIFICATION_ID = 1001;
    public static final String NOTIFICATION_CHANNEL_ID = "kulucka_service_channel";

    // Request Timeouts - WiFi mod değişimleri için optimize edildi
    public static final int CONNECTION_TIMEOUT = 10; // seconds (genel bağlantılar için)
    public static final int READ_TIMEOUT = 15; // seconds
    public static final int WRITE_TIMEOUT = 10; // seconds

    // WiFi işlemleri için özel timeout değerleri
    public static final int WIFI_MODE_CHANGE_TIMEOUT = 25; // seconds
    public static final int WIFI_CONNECT_TIMEOUT = 20; // seconds
    public static final int WIFI_SCAN_TIMEOUT = 20; // seconds
    public static final int SYSTEM_VERIFY_TIMEOUT = 10; // seconds

    // Discovery Timeouts
    public static final int DISCOVERY_TIMEOUT = 15000; // 15 saniye
    public static final int QUICK_DISCOVERY_TIMEOUT = 5000; // 5 saniye
    public static final int DIRECT_CONNECTION_TIMEOUT = 3000; // 3 saniye

    // WiFi Mode Change Delays - ESP32 yeniden başlatma gerektirmediği için kısaltıldı
    public static final int MODE_CHANGE_DELAY = 1000; // 1 saniye (mod değişikliği sonrası bekleme)
    public static final int IP_CHECK_DELAY = 2000; // 2 saniye (IP kontrolü için bekleme)
    public static final int SYSTEM_STABILIZATION_DELAY = 3000; // 3 saniye (sistem kararlılığı için)

    // Yeniden bağlanma stratejisi için sabitler
    public static final int MAX_RECONNECT_ATTEMPTS = 3;
    public static final int RECONNECT_DELAY_BASE = 2000; // 2 saniye
    public static final int RECONNECT_DELAY_MULTIPLIER = 2; // Exponential backoff

    // WiFi mod değişim durumları
    public static final String WIFI_MODE_CHANGE_SUCCESS = "success";
    public static final String WIFI_MODE_CHANGE_ERROR = "error";
    public static final String WIFI_MODE_CHANGE_PENDING = "pending";
}