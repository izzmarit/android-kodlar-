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

    // mDNS Configuration
    public static final String MDNS_HOSTNAME = "kulucka.local";
    public static final int MDNS_RESOLUTION_TIMEOUT = 5000; // 5 saniye

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

    // Yeni API Endpoints
    public static final String API_MOTOR_TEST = "/api/motor/test";
    public static final String API_MOTOR_STATUS = "/api/motor/status";
    public static final String API_PID_STATUS = "/api/pid/status";
    public static final String API_SYSTEM_SAVE = "/api/system/save";
    public static final String API_SYSTEM_HEALTH = "/api/system/health";
    public static final String API_SYSTEM_LOGS = "/api/system/logs";
    public static final String API_SYSTEM_ACTION = "/api/system/action";
    public static final String API_WIFI_CREDENTIALS = "/api/wifi/credentials";
    public static final String API_WIFI_MODE_STATUS = "/api/wifi/mode/status";
    public static final String API_NETWORK_TEST = "/api/network/test";
    public static final String API_STATUS_COMPLETE = "/api/status/complete";

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
    public static final String WIFI_MODE_CHANGE_IN_PROGRESS = "in_progress";
    public static final String WIFI_MODE_CHANGE_COMPLETED = "completed";
    public static final String WIFI_MODE_CHANGE_FAILED = "failed";

    // Sistem Aksiyonları
    public static final String SYSTEM_ACTION_RESTART = "restart";
    public static final String SYSTEM_ACTION_FACTORY_RESET = "factory_reset";
    public static final String SYSTEM_ACTION_SAVE_SETTINGS = "save_settings";
    public static final String SYSTEM_ACTION_LOAD_DEFAULTS = "load_defaults";

    // Motor Test Durumları
    public static final String MOTOR_TEST_IDLE = "idle";
    public static final String MOTOR_TEST_RUNNING = "running";
    public static final String MOTOR_TEST_COMPLETED = "completed";
    public static final String MOTOR_TEST_FAILED = "failed";

    // PID Durumları
    public static final String PID_STATUS_OFF = "off";
    public static final String PID_STATUS_MANUAL = "manual";
    public static final String PID_STATUS_AUTO = "auto";
    public static final String PID_STATUS_TUNING = "tuning";
    public static final String PID_STATUS_ERROR = "error";

    // Sistem Sağlık Durumları
    public static final String SYSTEM_HEALTH_EXCELLENT = "excellent";
    public static final String SYSTEM_HEALTH_GOOD = "good";
    public static final String SYSTEM_HEALTH_WARNING = "warning";
    public static final String SYSTEM_HEALTH_CRITICAL = "critical";
    public static final String SYSTEM_HEALTH_ERROR = "error";

    // Log Seviyeleri
    public static final String LOG_LEVEL_DEBUG = "DEBUG";
    public static final String LOG_LEVEL_INFO = "INFO";
    public static final String LOG_LEVEL_WARNING = "WARNING";
    public static final String LOG_LEVEL_ERROR = "ERROR";
    public static final String LOG_LEVEL_CRITICAL = "CRITICAL";

    // Timeout Değerleri - ESP32 yapısına uygun
    public static final int MOTOR_TEST_TIMEOUT = 120; // 2 dakika
    public static final int SYSTEM_SAVE_TIMEOUT = 10; // 10 saniye
    public static final int HEALTH_CHECK_TIMEOUT = 15; // 15 saniye
    public static final int LOG_FETCH_TIMEOUT = 20; // 20 saniye
    public static final int SYSTEM_ACTION_TIMEOUT = 30; // 30 saniye

    // API Response Kodları
    public static final int API_SUCCESS = 200;
    public static final int API_BAD_REQUEST = 400;
    public static final int API_UNAUTHORIZED = 401;
    public static final int API_NOT_FOUND = 404;
    public static final int API_TIMEOUT = 408;
    public static final int API_INTERNAL_ERROR = 500;
    public static final int API_SERVICE_UNAVAILABLE = 503;

    // Sistem Durumu Polling Aralıkları
    public static final long MOTOR_STATUS_POLL_INTERVAL = 1000; // 1 saniye
    public static final long PID_STATUS_POLL_INTERVAL = 2000; // 2 saniye
    public static final long HEALTH_CHECK_POLL_INTERVAL = 30000; // 30 saniye
    public static final long WIFI_MODE_CHANGE_POLL_INTERVAL = 2000; // 2 saniye

    // Shared Preferences Keys - Yeni ayarlar
    public static final String KEY_MOTOR_TEST_ENABLED = "motor_test_enabled";
    public static final String KEY_HEALTH_CHECK_ENABLED = "health_check_enabled";
    public static final String KEY_AUTO_SAVE_ENABLED = "auto_save_enabled";
    public static final String KEY_LOG_LEVEL = "log_level";
    public static final String KEY_SYSTEM_NOTIFICATIONS = "system_notifications";
    public static final String KEY_ADVANCED_MODE = "advanced_mode";

    // Bildirim Türleri
    public static final String NOTIFICATION_TYPE_STATUS = "status";
    public static final String NOTIFICATION_TYPE_ALARM = "alarm";
    public static final String NOTIFICATION_TYPE_WARNING = "warning";
    public static final String NOTIFICATION_TYPE_INFO = "info";
    public static final String NOTIFICATION_TYPE_ERROR = "error";

    // Motor Test Parametreleri
    public static final int MOTOR_TEST_MIN_DURATION = 1; // saniye
    public static final int MOTOR_TEST_MAX_DURATION = 60; // saniye
    public static final int MOTOR_TEST_DEFAULT_DURATION = 14; // saniye

    // PID Parametreleri - ESP32'deki limitlerle uyumlu
    public static final float PID_KP_MIN = 0.0f;
    public static final float PID_KP_MAX = 100.0f;
    public static final float PID_KI_MIN = 0.0f;
    public static final float PID_KI_MAX = 50.0f;
    public static final float PID_KD_MIN = 0.0f;
    public static final float PID_KD_MAX = 10.0f;

    // Kalibrasyon Limitleri
    public static final float CALIBRATION_TEMP_MIN = -5.0f;
    public static final float CALIBRATION_TEMP_MAX = 5.0f;
    public static final float CALIBRATION_HUMID_MIN = -10.0f;
    public static final float CALIBRATION_HUMID_MAX = 10.0f;

    // Alarm Limitleri
    public static final float ALARM_TEMP_OFFSET_MIN = 0.0f;
    public static final float ALARM_TEMP_OFFSET_MAX = 5.0f;
    public static final float ALARM_HUMID_OFFSET_MIN = 0.0f;
    public static final float ALARM_HUMID_OFFSET_MAX = 20.0f;
}