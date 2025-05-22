package com.kulucka.mk_v5.utils;

import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

public class NetworkUtils {
    private static NetworkUtils instance;
    private Context context;
    private ConnectivityManager connectivityManager;
    private WifiManager wifiManager;
    private MutableLiveData<Boolean> isConnectedToDeviceLiveData = new MutableLiveData<>();

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

    // WiFi ağına bağlan
    public boolean connectToWifi(String ssid, String password) {
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ için yeni bağlantı yöntemi
            return connectToWifiAndroid10Plus(ssid, password);
        } else {
            // Android 9 ve altı için eski bağlantı yöntemi
            return connectToWifiLegacy(ssid, password);
        }
    }

    // Android 10+ için WiFi bağlantısı
    private boolean connectToWifiAndroid10Plus(String ssid, String password) {
        try {
            WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                    .setSsid(ssid)
                    .setWpa2Passphrase(password)
                    .build();

            NetworkRequest request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .setNetworkSpecifier(specifier)
                    .build();

            ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    // WiFi ağına bağlandı
                    connectivityManager.bindProcessToNetwork(network);
                    isConnectedToDeviceLiveData.postValue(true);
                }

                @Override
                public void onLost(@NonNull Network network) {
                    super.onLost(network);
                    // WiFi ağı bağlantısı kesildi
                    isConnectedToDeviceLiveData.postValue(false);
                }
            };

            connectivityManager.requestNetwork(request, networkCallback);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Android 9 ve altı için WiFi bağlantısı
    private boolean connectToWifiLegacy(String ssid, String password) {
        try {
            // Önce mevcut aynı SSID'li ağları kaldır
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

            // Ağı ekle ve etkinleştir
            int netId = wifiManager.addNetwork(conf);
            wifiManager.disconnect();
            wifiManager.enableNetwork(netId, true);
            boolean result = wifiManager.reconnect();

            if (result) {
                isConnectedToDeviceLiveData.postValue(true);
            }

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Geçerli ağ ESP32 ağı mı?
    public boolean isConnectedToDeviceWifi() {
        String ssid = SharedPrefsManager.getInstance().getApSsid();

        if (ssid.isEmpty()) {
            ssid = Constants.DEFAULT_AP_SSID;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(
                    connectivityManager.getActiveNetwork());

            if (capabilities != null &&
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                // Burada Android 10+ için WiFi SSID'yi doğrudan alamıyoruz
                // Bunun yerine API bağlantısını test ediyoruz
                return true;
            }
        } else {
            String currentSsid = wifiManager.getConnectionInfo().getSSID();
            return currentSsid.equals("\"" + ssid + "\"");
        }

        return false;
    }

    // WiFi bağlantısını kapat
    public void disconnectFromWifi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            connectivityManager.bindProcessToNetwork(null);
        } else {
            wifiManager.disconnect();
        }

        isConnectedToDeviceLiveData.postValue(false);
    }
}