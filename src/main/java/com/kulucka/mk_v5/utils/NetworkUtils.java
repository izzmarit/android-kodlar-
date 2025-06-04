package com.kulucka.mk_v5.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.kulucka.mk_v5.services.ApiService;

import java.util.ArrayList;
import java.util.List;

public class NetworkUtils {
    private static final String TAG = "NetworkUtils";
    private static NetworkUtils instance;
    private Context context;
    private ConnectivityManager connectivityManager;
    private WifiManager wifiManager;
    private MutableLiveData<Boolean> isConnectedToDeviceLiveData = new MutableLiveData<>();
    private ConnectivityManager.NetworkCallback currentNetworkCallback;
    private MutableLiveData<List<ScanResult>> wifiScanResultsLiveData = new MutableLiveData<>();

    private NetworkUtils(Context context) {
        this.context = context.getApplicationContext();
        connectivityManager = (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
        wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
        isConnectedToDeviceLiveData.setValue(false);
    }

    public static NetworkUtils getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkUtils(context);
        }
        return instance;
    }

    public LiveData<Boolean> getDeviceConnectionStatus() {
        return isConnectedToDeviceLiveData;
    }

    public LiveData<List<ScanResult>> getWifiScanResults() {
        return wifiScanResultsLiveData;
    }

    public boolean connectToWifi(String ssid, String password) {
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
            try {
                Thread.sleep(2000); // WiFi açılmasını bekle
            } catch (InterruptedException e) {
                Log.e(TAG, "Sleep interrupted", e);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return connectToWifiAndroid10Plus(ssid, password);
        } else {
            return connectToWifiLegacy(ssid, password);
        }
    }

    private boolean connectToWifiAndroid10Plus(String ssid, String password) {
        try {
            // Önceki callback'i temizle
            if (currentNetworkCallback != null) {
                try {
                    connectivityManager.unregisterNetworkCallback(currentNetworkCallback);
                } catch (Exception e) {
                    Log.w(TAG, "Error unregistering previous callback", e);
                }
            }

            WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                    .setSsid(ssid)
                    .setWpa2Passphrase(password)
                    .build();

            NetworkRequest request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .setNetworkSpecifier(specifier)
                    .build();

            currentNetworkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    try {
                        connectivityManager.bindProcessToNetwork(network);
                        isConnectedToDeviceLiveData.postValue(true);
                        Log.d(TAG, "WiFi connected successfully to: " + ssid);
                    } catch (Exception e) {
                        Log.e(TAG, "Error binding process to network", e);
                    }
                }

                @Override
                public void onLost(@NonNull Network network) {
                    super.onLost(network);
                    isConnectedToDeviceLiveData.postValue(false);
                    Log.d(TAG, "WiFi connection lost: " + ssid);
                }

                @Override
                public void onUnavailable() {
                    super.onUnavailable();
                    isConnectedToDeviceLiveData.postValue(false);
                    Log.d(TAG, "WiFi network unavailable: " + ssid);
                }
            };

            connectivityManager.requestNetwork(request, currentNetworkCallback);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error connecting to WiFi", e);
            return false;
        }
    }

    private boolean connectToWifiLegacy(String ssid, String password) {
        try {
            // Mevcut ağları kontrol et ve varsa sil
            List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
            if (configuredNetworks != null) {
                for (WifiConfiguration config : configuredNetworks) {
                    if (config.SSID != null && config.SSID.equals("\"" + ssid + "\"")) {
                        wifiManager.removeNetwork(config.networkId);
                    }
                }
            }

            // Yeni ağ yapılandırması oluştur
            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = "\"" + ssid + "\"";
            conf.preSharedKey = "\"" + password + "\"";
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

            int netId = wifiManager.addNetwork(conf);
            if (netId == -1) {
                Log.e(TAG, "Failed to add network configuration");
                return false;
            }

            wifiManager.disconnect();
            wifiManager.enableNetwork(netId, true);
            boolean result = wifiManager.reconnect();

            if (result) {
                isConnectedToDeviceLiveData.postValue(true);
                Log.d(TAG, "WiFi connection initiated to: " + ssid);
            }

            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error connecting to WiFi (legacy)", e);
            return false;
        }
    }

    public void scanWifiNetworks() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Location permission not granted for WiFi scanning");
                wifiScanResultsLiveData.postValue(new ArrayList<>());
                return;
            }
        }

        boolean scanStarted = wifiManager.startScan();
        if (scanStarted) {
            Log.d(TAG, "WiFi scan started");
            // Tarama sonuçlarını al
            new android.os.Handler().postDelayed(() -> {
                List<ScanResult> results = wifiManager.getScanResults();
                wifiScanResultsLiveData.postValue(results);
                Log.d(TAG, "WiFi scan completed, found " + results.size() + " networks");
            }, 3000); // 3 saniye bekle
        } else {
            Log.e(TAG, "Failed to start WiFi scan");
            wifiScanResultsLiveData.postValue(new ArrayList<>());
        }
    }

    public boolean isConnectedToESP32AP() {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null) {
            String currentSSID = wifiInfo.getSSID().replace("\"", "");
            return "KULUCKA_MK_v5".equals(currentSSID);
        }
        return false;
    }

    public boolean isConnectedToWifi() {
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork != null) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        }
        return false;
    }

    public String getCurrentSSID() {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null) {
            return wifiInfo.getSSID().replace("\"", "");
        }
        return "";
    }

    public String getCurrentIPAddress() {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null) {
            int ip = wifiInfo.getIpAddress();
            return String.format("%d.%d.%d.%d",
                    (ip & 0xff),
                    (ip >> 8 & 0xff),
                    (ip >> 16 & 0xff),
                    (ip >> 24 & 0xff));
        }
        return "";
    }

    public void disconnectFromWifi() {
        try {
            // Network callback'i temizle
            if (currentNetworkCallback != null) {
                connectivityManager.unregisterNetworkCallback(currentNetworkCallback);
                currentNetworkCallback = null;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                connectivityManager.bindProcessToNetwork(null);
            } else {
                wifiManager.disconnect();
            }

            isConnectedToDeviceLiveData.postValue(false);
            Log.d(TAG, "Disconnected from WiFi");
        } catch (Exception e) {
            Log.e(TAG, "Error disconnecting from WiFi", e);
        }
    }

    // ESP32'yi otomatik bulma
    public void findESP32InNetwork(ESP32FoundCallback callback) {
        if (!isConnectedToWifi()) {
            callback.onError("WiFi bağlantısı yok");
            return;
        }

        // Mevcut ağdaki IP aralığını tara
        String currentIP = getCurrentIPAddress();
        if (currentIP.isEmpty()) {
            callback.onError("IP adresi alınamadı");
            return;
        }

        // IP adresinin ilk 3 oktetini al (örn: 192.168.1)
        String[] parts = currentIP.split("\\.");
        if (parts.length != 4) {
            callback.onError("Geçersiz IP adresi");
            return;
        }

        String subnet = parts[0] + "." + parts[1] + "." + parts[2] + ".";

        // Ağdaki tüm IP'leri tara (1-254)
        new Thread(() -> {
            for (int i = 1; i <= 254; i++) {
                String testIP = subnet + i;

                // ESP32'yi test et
                ApiService.getInstance().setBaseUrl("http://" + testIP);
                ApiService.getInstance().testConnection(new ApiService.ParameterCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "ESP32 found at: " + testIP);
                        callback.onFound(testIP);
                    }

                    @Override
                    public void onError(String message) {
                        // Bu IP'de ESP32 yok, devam et
                    }
                });

                try {
                    Thread.sleep(50); // Her IP için 50ms bekle
                } catch (InterruptedException e) {
                    Log.e(TAG, "Scan interrupted", e);
                    break;
                }
            }

            callback.onScanComplete();
        }).start();
    }

    public interface ESP32FoundCallback {
        void onFound(String ipAddress);
        void onError(String message);
        void onScanComplete();
    }

    public void cleanup() {
        if (currentNetworkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(currentNetworkCallback);
                currentNetworkCallback = null;
            } catch (Exception e) {
                Log.w(TAG, "Error during cleanup", e);
            }
        }
    }
}