package com.kulucka.mk.utils;

/**
 * Uygulama genelinde kullanılan sabit değerler
 * ESP32 API endpoint'leri ve network konfigürasyonları
 */
public class Constants {

    // ESP32 Network Ayarları
    public static final int ESP32_PORT = 80;
    public static final String ESP32_AP_SSID = "KULUCKA_MK";
    public static final String ESP32_AP_PASSWORD = "12345678";
    public static final String ESP32_AP_IP = "192.168.4.1";
    public static final String ESP32_MDNS_NAME = "kulucka.local";

    // API Base URLs
    public static final String API_BASE_URL = "http://%s:" + ESP32_PORT + "/";
    public static final String API_PREFIX = "api/";

    // API Endpoints - ESP32 WiFi Manager kodlarından alınan endpoint'ler
    public static final String ENDPOINT_STATUS = "api/status";
    public static final String ENDPOINT_WIFI_NETWORKS = "api/wifi/networks";
    public static final String ENDPOINT_WIFI_CONNECT = "api/wifi/connect";
    public static final String ENDPOINT_WIFI_AP = "api/wifi/ap";
    public static final String ENDPOINT_TEMPERATURE = "api/temperature";
    public static final String ENDPOINT_HUMIDITY = "api/humidity";
    public static final String ENDPOINT_PID = "api/pid";
    public static final String ENDPOINT_MOTOR = "api/motor";
    public static final String ENDPOINT_ALARM = "api/alarm";
    public static final String ENDPOINT_CALIBRATION = "api/calibration";
    public static final String ENDPOINT_INCUBATION = "api/incubation";

    // Web sayfaları
    public static final String WEB_HOME = "/";
    public static final String WEB_WIFI = "/wifi";

    // Network Timeout'lar
    public static final int CONNECT_TIMEOUT = 10; // saniye
    public static final int READ_TIMEOUT = 15; // saniye
    public static final int WRITE_TIMEOUT = 10; // saniye

    // Background Service Ayarları
    public static final int SERVICE_NOTIFICATION_ID = 1001;
    public static final String NOTIFICATION_CHANNEL_ID = "kulucka_mk_channel";
    public static final String NOTIFICATION_CHANNEL_NAME = "Kuluçka MK Bildirimleri";
    public static final long DATA_UPDATE_INTERVAL = 2000; // 2 saniye
    public static final long WIFI_CHECK_INTERVAL = 5000; // 5 saniye
    public static final long CONNECTION_RETRY_INTERVAL = 10000; // 10 saniye

    // Kuluçka Tipleri - ESP32 kodundaki enum değerleri
    public static final int INCUBATION_TYPE_CHICKEN = 0;
    public static final int INCUBATION_TYPE_QUAIL = 1;
    public static final int INCUBATION_TYPE_GOOSE = 2;
    public static final int INCUBATION_TYPE_MANUAL = 3;

    public static final String[] INCUBATION_TYPE_NAMES = {
            "Tavuk",
            "Bıldırcın",
            "Kaz",
            "Manuel"
    };

    // PID Modları - ESP32 kodundaki değerler
    public static final int PID_MODE_OFF = 0;
    public static final int PID_MODE_MANUAL = 1;
    public static final int PID_MODE_AUTO = 2;

    public static final String[] PID_MODE_NAMES = {
            "Kapalı",
            "Manuel",
            "Otomatik"
    };

    // WiFi Bağlantı Durumları
    public static final String WIFI_STATUS_DISCONNECTED = "Bağlantısız";
    public static final String WIFI_STATUS_CONNECTING = "Bağlanıyor...";
    public static final String WIFI_STATUS_CONNECTED = "Bağlı";
    public static final String WIFI_STATUS_FAILED = "Bağlantı Başarısız";
    public static final String WIFI_STATUS_AP_MODE = "AP Modu";

    // Sensor Limit Değerleri
    public static final double MIN_TEMPERATURE = 20.0;
    public static final double MAX_TEMPERATURE = 45.0;
    public static final double MIN_HUMIDITY = 30.0;
    public static final double MAX_HUMIDITY = 90.0;

    // PID Parametreleri Limit Değerleri
    public static final double MIN_PID_KP = 0.0;
    public static final double MAX_PID_KP = 100.0;
    public static final double MIN_PID_KI = 0.0;
    public static final double MAX_PID_KI = 50.0;
    public static final double MIN_PID_KD = 0.0;
    public static final double MAX_PID_KD = 10.0;

    // Motor Ayarları Limit Değerleri
    public static final int MIN_MOTOR_WAIT_TIME = 60; // 1 dakika
    public static final int MAX_MOTOR_WAIT_TIME = 300; // 5 dakika
    public static final int MIN_MOTOR_RUN_TIME = 5; // 5 saniye
    public static final int MAX_MOTOR_RUN_TIME = 30; // 30 saniye

    // Kalibrasyon Limit Değerleri
    public static final double MIN_CALIBRATION = -10.0;
    public static final double MAX_CALIBRATION = 10.0;

    // Kuluçka Gün Limitleri
    public static final int MIN_INCUBATION_DAYS = 1;
    public static final int MAX_INCUBATION_DAYS = 35;

    // SharedPreferences Keys
    public static final String PREF_ESP32_IP = "esp32_ip";
    public static final String PREF_LAST_KNOWN_IP = "last_known_ip";
    public static final String PREF_AUTO_CONNECT = "auto_connect";
    public static final String PREF_NOTIFICATION_ENABLED = "notification_enabled";
    public static final String PREF_UPDATE_INTERVAL = "update_interval";
    public static final String PREF_THEME_MODE = "theme_mode";
    public static final String PREF_LANGUAGE = "language";
    public static final String PREF_FIRST_LAUNCH = "first_launch";

    // Intent Action'ları
    public static final String ACTION_UPDATE_DATA = "com.kulucka.mk.UPDATE_DATA";
    public static final String ACTION_CONNECTION_CHANGED = "com.kulucka.mk.CONNECTION_CHANGED";
    public static final String ACTION_WIFI_CHANGED = "com.kulucka.mk.WIFI_CHANGED";
    public static final String ACTION_ALARM_TRIGGERED = "com.kulucka.mk.ALARM_TRIGGERED";

    // Bundle Keys
    public static final String EXTRA_SYSTEM_STATUS = "system_status";
    public static final String EXTRA_CONNECTION_STATUS = "connection_status";
    public static final String EXTRA_ERROR_MESSAGE = "error_message";
    public static final String EXTRA_IP_ADDRESS = "ip_address";

    // Error Codes
    public static final int ERROR_NETWORK_UNAVAILABLE = 1001;
    public static final int ERROR_ESP32_UNREACHABLE = 1002;
    public static final int ERROR_INVALID_RESPONSE = 1003;
    public static final int ERROR_TIMEOUT = 1004;
    public static final int ERROR_AUTHENTICATION = 1005;
    public static final int ERROR_PARAMETER_INVALID = 1006;

    // IP Address Range for scanning
    public static final String IP_RANGE_PREFIX = "192.168.";
    public static final int IP_SCAN_TIMEOUT = 1000; // 1 saniye

    // Chart ayarları
    public static final int CHART_MAX_ENTRIES = 100;
    public static final float CHART_ANIMATION_DURATION = 1000f;

    // Format Strings
    public static final String FORMAT_TEMPERATURE = "%.1f°C";
    public static final String FORMAT_HUMIDITY = "%.0f%%";
    public static final String FORMAT_PID_VALUE = "%.2f";
    public static final String FORMAT_DAYS = "%d/%d gün";
    public static final String FORMAT_TIME = "%02d:%02d:%02d";

    // Renk kodları (tema için)
    public static final String COLOR_PRIMARY = "#2196F3";
    public static final String COLOR_PRIMARY_DARK = "#1976D2";
    public static final String COLOR_ACCENT = "#FF5722";
    public static final String COLOR_SUCCESS = "#4CAF50";
    public static final String COLOR_WARNING = "#FF9800";
    public static final String COLOR_ERROR = "#F44336";

    // Default değerler
    public static final double DEFAULT_TARGET_TEMP = 37.5;
    public static final double DEFAULT_TARGET_HUMIDITY = 60.0;
    public static final int DEFAULT_MOTOR_WAIT_TIME = 120;
    public static final int DEFAULT_MOTOR_RUN_TIME = 14;

    // Network Security
    public static final String[] ALLOWED_CIPHER_SUITES = {
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256"
    };

    // Utility methods
    public static String getApiUrl(String ipAddress) {
        return String.format(API_BASE_URL, ipAddress);
    }

    public static String getFullEndpoint(String ipAddress, String endpoint) {
        return getApiUrl(ipAddress) + endpoint;
    }

    public static boolean isValidIP(String ip) {
        if (ip == null || ip.isEmpty()) return false;
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return false;

        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) return false;
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isInLocalNetwork(String ip) {
        return ip != null && (
                ip.startsWith("192.168.") ||
                        ip.startsWith("10.") ||
                        ip.startsWith("172.")
        );
    }

    // Private constructor to prevent instantiation
    private Constants() {
        throw new UnsupportedOperationException("Constants sınıfından instance oluşturulamaz");
    }
}