package com.kulucka.mk_v5.utils;

public class Constants {
    // API URL'leri - ESP32 AP modu IP adresi
    public static final String DEFAULT_IP_ADDRESS = "192.168.4.1";
    public static final String BASE_URL = "http://%s/";
    public static final String STATUS_ENDPOINT = "status";
    public static final String SET_PARAM_ENDPOINT = "api/set";

    // AP Bilgileri - ESP32 kodundaki ile eşleşmeli
    public static final String DEFAULT_AP_SSID = "KULUCKA_MK_v5";
    public static final String DEFAULT_AP_PASSWORD = "12345678";

    // Shared Preferences Anahtarları
    public static final String PREF_FILE_NAME = "kulucka_prefs";
    public static final String PREF_DEVICE_IP = "device_ip";
    public static final String PREF_AP_SSID = "ap_ssid";
    public static final String PREF_AP_PASSWORD = "ap_password";
    public static final String PREF_AUTO_CONNECT = "auto_connect";

    // Bildirim Kanalları
    public static final String ALARM_NOTIFICATION_CHANNEL_ID = "alarm_channel";
    public static final String CONNECTION_NOTIFICATION_CHANNEL_ID = "connection_channel";

    // Kuluçka Türleri
    public static final int INCUBATION_CHICKEN = 0;
    public static final int INCUBATION_QUAIL = 1;
    public static final int INCUBATION_GOOSE = 2;
    public static final int INCUBATION_MANUAL = 3;

    // Servis Güncelleme Aralıkları (ms)
    public static final long STATUS_UPDATE_INTERVAL = 5000;
    public static final long CONNECTION_CHECK_INTERVAL = 10000;

    // Bildirim Özellikleri
    public static final int ALARM_NOTIFICATION_ID = 1001;
    public static final int CONNECTION_NOTIFICATION_ID = 1002;

    // Network timeout değerleri
    public static final int NETWORK_TIMEOUT_SECONDS = 10;
    public static final int MAX_RETRY_COUNT = 3;
}