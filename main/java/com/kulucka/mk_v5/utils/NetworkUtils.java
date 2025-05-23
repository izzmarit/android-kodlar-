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
import android.util.Log;  // BU SATIRI EKLEYİN

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
    private ConnectivityManager.NetworkCallback currentNetworkCallback; // YENİ

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

    public boolean connectToWifi(String ssid, String password) {
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
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
                    Log.w("NetworkUtils", "Error unregistering previous callback", e);
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
                        Log.d("NetworkUtils", "WiFi connected successfully");
                    } catch (Exception e) {
                        Log.e("NetworkUtils", "Error binding process to network", e);
                    }
                }

                @Override
                public void onLost(@NonNull Network network) {
                    super.onLost(network);
                    isConnectedToDeviceLiveData.postValue(false);
                    Log.d("NetworkUtils", "WiFi connection lost");
                }

                @Override
                public void onUnavailable() {
                    super.onUnavailable();
                    isConnectedToDeviceLiveData.postValue(false);
                    Log.d("NetworkUtils", "WiFi network unavailable");
                }
            };

            connectivityManager.requestNetwork(request, currentNetworkCallback);
            return true;
        } catch (Exception e) {
            Log.e("NetworkUtils", "Error connecting to WiFi", e);
            return false;
        }
    }

    private boolean connectToWifiLegacy(String ssid, String password) {
        try {
            List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
            if (configuredNetworks != null) {
                for (WifiConfiguration config : configuredNetworks) {
                    if (config.SSID != null && config.SSID.equals("\"" + ssid + "\"")) {
                        wifiManager.removeNetwork(config.networkId);
                    }
                }
            }

            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = "\"" + ssid + "\"";
            conf.preSharedKey = "\"" + password + "\"";

            int netId = wifiManager.addNetwork(conf);
            wifiManager.disconnect();
            wifiManager.enableNetwork(netId, true);
            boolean result = wifiManager.reconnect();

            if (result) {
                isConnectedToDeviceLiveData.postValue(true);
            }

            return result;
        } catch (Exception e) {
            Log.e("NetworkUtils", "Error connecting to WiFi (legacy)", e);
            return false;
        }
    }

    public boolean isConnectedToDeviceWifi() {
        String ssid = SharedPrefsManager.getInstance().getApSsid();

        if (ssid.isEmpty()) {
            ssid = Constants.DEFAULT_AP_SSID;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(
                    connectivityManager.getActiveNetwork());

            return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        } else {
            try {
                String currentSsid = wifiManager.getConnectionInfo().getSSID();
                return currentSsid.equals("\"" + ssid + "\"");
            } catch (Exception e) {
                Log.e("NetworkUtils", "Error checking WiFi SSID", e);
                return false;
            }
        }
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
        } catch (Exception e) {
            Log.e("NetworkUtils", "Error disconnecting from WiFi", e);
        }
    }

    // Activity veya Service destroy olduğunda çağrılması için
    public void cleanup() {
        if (currentNetworkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(currentNetworkCallback);
                currentNetworkCallback = null;
            } catch (Exception e) {
                Log.w("NetworkUtils", "Error during cleanup", e);
            }
        }
    }
}