package com.kulucka.mk.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.kulucka.mk.services.ApiService;

/**
 * WiFi ve ağ bağlantısı durumu değişikliklerini dinleyen BroadcastReceiver
 * WiFi bağlantısı kesildiğinde veya değiştiğinde uygulamayı bilgilendirir
 */
public class WiFiStateReceiver extends BroadcastReceiver {

    private static final String TAG = "WiFiStateReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;

        String action = intent.getAction();
        if (action == null) return;

        Log.d(TAG, "WiFi durumu değişti: " + action);

        switch (action) {
            case WifiManager.WIFI_STATE_CHANGED_ACTION:
                handleWifiStateChanged(context, intent);
                break;

            case WifiManager.NETWORK_STATE_CHANGED_ACTION:
                handleNetworkStateChanged(context, intent);
                break;

            case ConnectivityManager.CONNECTIVITY_ACTION:
                handleConnectivityChanged(context);
                break;
        }
    }

    /**
     * WiFi durumu değiştiğinde çağrılır (WiFi açık/kapalı)
     */
    private void handleWifiStateChanged(Context context, Intent intent) {
        int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);

        String stateString = getWifiStateString(wifiState);
        Log.d(TAG, "WiFi durumu: " + stateString);

        switch (wifiState) {
            case WifiManager.WIFI_STATE_ENABLED:
                onWifiEnabled(context);
                break;

            case WifiManager.WIFI_STATE_DISABLED:
                onWifiDisabled(context);
                break;

            case WifiManager.WIFI_STATE_ENABLING:
                onWifiEnabling(context);
                break;

            case WifiManager.WIFI_STATE_DISABLING:
                onWifiDisabling(context);
                break;

            case WifiManager.WIFI_STATE_UNKNOWN:
                onWifiUnknown(context);
                break;
        }

        // Broadcast gönder
        sendWifiStateChangedBroadcast(context, wifiState, stateString);
    }

    /**
     * Ağ bağlantısı durumu değiştiğinde çağrılır (ağa bağlandı/bağlantı kesildi)
     */
    private void handleNetworkStateChanged(Context context, Intent intent) {
        NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);

        if (networkInfo == null) return;

        Log.d(TAG, "Ağ durumu değişti: " + networkInfo.getDetailedState());

        if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            if (networkInfo.isConnected()) {
                onWifiNetworkConnected(context);
            } else if (!networkInfo.isConnectedOrConnecting()) {
                onWifiNetworkDisconnected(context);
            }
        }

        // Broadcast gönder
        sendNetworkStateChangedBroadcast(context, networkInfo.isConnected(),
                networkInfo.getDetailedState().toString());
    }

    /**
     * Genel bağlantı durumu değiştiğinde çağrılır
     */
    private void handleConnectivityChanged(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        Log.d(TAG, "Bağlantı durumu: " + (isConnected ? "Bağlı" : "Bağlantısız"));

        if (isConnected) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                onConnectivityRestored(context);
            }
        } else {
            onConnectivityLost(context);
        }

        // Broadcast gönder
        sendConnectivityChangedBroadcast(context, isConnected,
                activeNetwork != null ? activeNetwork.getTypeName() : "None");
    }

    /**
     * WiFi etkinleştirildiğinde
     */
    private void onWifiEnabled(Context context) {
        Log.i(TAG, "WiFi etkinleştirildi");

        // WiFi taraması başlat
        startWifiScan(context);

        // SharedPreferences güncelle
        SharedPreferencesManager prefsManager = SharedPreferencesManager.getInstance(context);
        prefsManager.updateConnectionStats(true);
    }

    /**
     * WiFi devre dışı bırakıldığında
     */
    private void onWifiDisabled(Context context) {
        Log.i(TAG, "WiFi devre dışı bırakıldı");

        // API servisine bildir
        ApiService apiService = ApiService.getInstance(context);
        // Bağlantı durumunu güncelle (disconnected olarak işaretle)

        SharedPreferencesManager prefsManager = SharedPreferencesManager.getInstance(context);
        prefsManager.updateConnectionStats(false);
    }

    /**
     * WiFi etkinleştiriliyor
     */
    private void onWifiEnabling(Context context) {
        Log.d(TAG, "WiFi etkinleştiriliyor...");
    }

    /**
     * WiFi devre dışı bırakılıyor
     */
    private void onWifiDisabling(Context context) {
        Log.d(TAG, "WiFi devre dışı bırakılıyor...");
    }

    /**
     * WiFi durumu bilinmiyor
     */
    private void onWifiUnknown(Context context) {
        Log.w(TAG, "WiFi durumu bilinmiyor");
    }

    /**
     * WiFi ağa bağlandığında
     */
    private void onWifiNetworkConnected(Context context) {
        Log.i(TAG, "WiFi ağa bağlandı");

        // Bağlı olunan ağ bilgilerini al
        WifiInfo wifiInfo = getWifiInfo(context);
        if (wifiInfo != null) {
            String ssid = wifiInfo.getSSID();
            if (ssid != null) {
                ssid = ssid.replace("\"", ""); // Çift tırnak işaretlerini kaldır
                Log.i(TAG, "Bağlı olunan ağ: " + ssid);

                // Son bağlanan ağı kaydet
                String ipAddress = NetworkUtils.getCurrentWiFiIP(context);
                SharedPreferencesManager prefsManager = SharedPreferencesManager.getInstance(context);
                prefsManager.saveLastConnectedWiFi(ssid, ipAddress);

                // ESP32 IP'sini güncelle (aynı ağda ise)
                checkAndUpdateESP32IP(context, ipAddress);
            }
        }

        // API servisine yeni IP ile bağlanmayı dene
        attemptESP32Connection(context);
    }

    /**
     * WiFi ağ bağlantısı kesildiğinde
     */
    private void onWifiNetworkDisconnected(Context context) {
        Log.i(TAG, "WiFi ağ bağlantısı kesildi");

        // API servisi bağlantısını bildir
        ApiService apiService = ApiService.getInstance(context);
        // Disconnection durumunu işle

        SharedPreferencesManager prefsManager = SharedPreferencesManager.getInstance(context);
        prefsManager.updateConnectionStats(false);
    }

    /**
     * İnternet bağlantısı geri geldiğinde
     */
    private void onConnectivityRestored(Context context) {
        Log.i(TAG, "İnternet bağlantısı geri geldi");

        // ESP32 bağlantısını yeniden dene
        Utils.delay(2000, () -> attemptESP32Connection(context));
    }

    /**
     * İnternet bağlantısı kaybolduğunda
     */
    private void onConnectivityLost(Context context) {
        Log.i(TAG, "İnternet bağlantısı kayboldu");
    }

    /**
     * WiFi taraması başlatır
     */
    private void startWifiScan(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);

        if (wifiManager != null && wifiManager.isWifiEnabled()) {
            boolean scanStarted = wifiManager.startScan();
            Log.d(TAG, "WiFi taraması " + (scanStarted ? "başlatıldı" : "başlatılamadı"));
        }
    }

    /**
     * ESP32 bağlantısını deneme
     */
    private void attemptESP32Connection(Context context) {
        Log.d(TAG, "ESP32 bağlantısı deneniyor...");

        SharedPreferencesManager prefsManager = SharedPreferencesManager.getInstance(context);
        ApiService apiService = ApiService.getInstance(context);

        // Mevcut IP'yi dene
        String currentIP = prefsManager.getESP32IP();

        apiService.testConnection(new ApiService.ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean connected) {
                if (connected) {
                    Log.i(TAG, "ESP32 bağlantısı başarılı: " + currentIP);
                    prefsManager.setLastKnownIP(currentIP);
                    prefsManager.updateConnectionStats(true);
                    sendConnectionSuccessBroadcast(context, currentIP);
                } else {
                    // Alternatif IP'leri dene
                    tryAlternativeIPs(context);
                }
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "ESP32 bağlantı hatası: " + error);
                tryAlternativeIPs(context);
            }
        });
    }

    /**
     * Alternatif IP adreslerini dener
     */
    private void tryAlternativeIPs(Context context) {
        Log.d(TAG, "Alternatif IP adresleri deneniyor...");

        // Varsayılan IP'leri kontrol et
        NetworkUtils.checkDefaultESP32IPs(new NetworkUtils.DefaultIPCallback() {
            @Override
            public void onResult(String ipAddress) {
                if (ipAddress != null) {
                    Log.i(TAG, "ESP32 alternatif IP'de bulundu: " + ipAddress);

                    // IP'yi güncelle
                    SharedPreferencesManager prefsManager = SharedPreferencesManager.getInstance(context);
                    prefsManager.setESP32IP(ipAddress);
                    prefsManager.setLastKnownIP(ipAddress);

                    ApiService apiService = ApiService.getInstance(context);
                    apiService.updateIpAddress(ipAddress);

                    sendConnectionSuccessBroadcast(context, ipAddress);
                } else {
                    // Tam tarama yap
                    performFullScan(context);
                }
            }
        });
    }

    /**
     * Tam IP taraması yapar
     */
    private void performFullScan(Context context) {
        Log.d(TAG, "Tam IP taraması yapılıyor...");

        NetworkUtils.scanForESP32(context, new NetworkUtils.ScanCallback() {
            @Override
            public void onScanCompleted(java.util.List<String> foundIPs) {
                if (!foundIPs.isEmpty()) {
                    String newIP = foundIPs.get(0);
                    Log.i(TAG, "ESP32 tam taramada bulundu: " + newIP);

                    // IP'yi güncelle
                    SharedPreferencesManager prefsManager = SharedPreferencesManager.getInstance(context);
                    prefsManager.setESP32IP(newIP);
                    prefsManager.setLastKnownIP(newIP);

                    ApiService apiService = ApiService.getInstance(context);
                    apiService.updateIpAddress(newIP);

                    sendConnectionSuccessBroadcast(context, newIP);
                } else {
                    Log.w(TAG, "ESP32 hiçbir IP'de bulunamadı");
                    sendConnectionFailureBroadcast(context, "ESP32 cihazı bulunamadı");
                }
            }
        });
    }

    /**
     * ESP32 IP adresini kontrol eder ve günceller
     */
    private void checkAndUpdateESP32IP(Context context, String currentIP) {
        if (currentIP == null) return;

        SharedPreferencesManager prefsManager = SharedPreferencesManager.getInstance(context);
        String lastKnownIP = prefsManager.getLastKnownIP();

        // Aynı subnet'te ise IP'yi güncelle
        if (lastKnownIP != null && !lastKnownIP.equals(Constants.ESP32_AP_IP)) {
            if (Utils.isInSameSubnet(currentIP, lastKnownIP)) {
                String possibleESP32IP = Utils.getSubnetBase(currentIP);
                if (possibleESP32IP != null) {
                    // Son okteti ESP32'nin muhtemel IP'si ile değiştir
                    possibleESP32IP = possibleESP32IP.substring(0, possibleESP32IP.lastIndexOf('.') + 1) + "1";

                    if (NetworkUtils.isHostReachable(possibleESP32IP, Constants.ESP32_PORT)) {
                        prefsManager.setESP32IP(possibleESP32IP);
                        ApiService.getInstance(context).updateIpAddress(possibleESP32IP);
                        Log.i(TAG, "ESP32 IP güncellendi: " + possibleESP32IP);
                    }
                }
            }
        }
    }

    /**
     * WiFi bilgilerini alır
     */
    private WifiInfo getWifiInfo(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);

        if (wifiManager != null) {
            return wifiManager.getConnectionInfo();
        }

        return null;
    }

    /**
     * WiFi durumu string'ini döndürür
     */
    private String getWifiStateString(int wifiState) {
        switch (wifiState) {
            case WifiManager.WIFI_STATE_DISABLED:
                return "Devre Dışı";
            case WifiManager.WIFI_STATE_DISABLING:
                return "Devre Dışı Bırakılıyor";
            case WifiManager.WIFI_STATE_ENABLED:
                return "Etkin";
            case WifiManager.WIFI_STATE_ENABLING:
                return "Etkinleştiriliyor";
            case WifiManager.WIFI_STATE_UNKNOWN:
                return "Bilinmiyor";
            default:
                return "Tanımlanmamış";
        }
    }

    // === Broadcast Gönderme Metodları ===

    /**
     * WiFi durum değişikliği broadcast'i gönderir
     */
    private void sendWifiStateChangedBroadcast(Context context, int wifiState, String stateString) {
        Intent intent = new Intent(Constants.ACTION_WIFI_CHANGED);
        intent.putExtra("wifi_state", wifiState);
        intent.putExtra("wifi_state_string", stateString);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * Ağ durum değişikliği broadcast'i gönderir
     */
    private void sendNetworkStateChangedBroadcast(Context context, boolean isConnected, String detailedState) {
        Intent intent = new Intent(Constants.ACTION_CONNECTION_CHANGED);
        intent.putExtra(Constants.EXTRA_CONNECTION_STATUS, isConnected);
        intent.putExtra("detailed_state", detailedState);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * Bağlantı durum değişikliği broadcast'i gönderir
     */
    private void sendConnectivityChangedBroadcast(Context context, boolean isConnected, String networkType) {
        Intent intent = new Intent(Constants.ACTION_CONNECTION_CHANGED);
        intent.putExtra(Constants.EXTRA_CONNECTION_STATUS, isConnected);
        intent.putExtra("network_type", networkType);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * Bağlantı başarısı broadcast'i gönderir
     */
    private void sendConnectionSuccessBroadcast(Context context, String ipAddress) {
        Intent intent = new Intent(Constants.ACTION_CONNECTION_CHANGED);
        intent.putExtra(Constants.EXTRA_CONNECTION_STATUS, true);
        intent.putExtra(Constants.EXTRA_IP_ADDRESS, ipAddress);
        intent.putExtra(Constants.EXTRA_ERROR_MESSAGE, "ESP32'ye başarıyla bağlanıldı");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * Bağlantı başarısızlığı broadcast'i gönderir
     */
    private void sendConnectionFailureBroadcast(Context context, String errorMessage) {
        Intent intent = new Intent(Constants.ACTION_CONNECTION_CHANGED);
        intent.putExtra(Constants.EXTRA_CONNECTION_STATUS, false);
        intent.putExtra(Constants.EXTRA_ERROR_MESSAGE, errorMessage);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}