package com.kulucka.kontrol.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.List;

public class WiFiManager {
    private static final String TAG = "WiFiManager";
    private static final String ESP_SSID = "KuluçkaKontrol"; // ESP32 AP SSID
    private static final String ESP_PASSWORD = "12345678"; // ESP32 AP şifresi

    private Context context;
    private WifiManager wifiManager;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private OnWiFiConnectedListener listener;

    public interface OnWiFiConnectedListener {
        void onWiFiConnected();
        void onWiFiDisconnected();
        void onWiFiConnectFailed();
    }

    public WiFiManager(Context context) {
        this.context = context;
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public void setOnWiFiConnectedListener(OnWiFiConnectedListener listener) {
        this.listener = listener;
    }

    public boolean isWifiEnabled() {
        return wifiManager.isWifiEnabled();
    }

    public void enableWifi() {
        if (!wifiManager.isWifiEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ için WiFi ayarlar paneline yönlendir
                Intent intent = new Intent(Settings.Panel.ACTION_WIFI);
                if (context instanceof Activity) {
                    ((Activity) context).startActivity(intent);
                } else {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            } else {
                // Android 9 ve öncesi için doğrudan değiştir
                wifiManager.setWifiEnabled(true);
            }
        }
    }

    public boolean isConnectedToESP() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Network network = connectivityManager.getActiveNetwork();
            if (network != null) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                return capabilities != null &&
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
                // Not ideal, but Android 10+ restricts SSID info
            }
            return false;
        } else {
            android.net.wifi.WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                String ssid = wifiInfo.getSSID();
                return ssid != null && ssid.replace("\"", "").equals(ESP_SSID);
            }
            return false;
        }
    }

    public void connectToESP() {
        // Disconnect old callback if exists
        if (networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering network callback: " + e.getMessage());
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!hasRequiredPermissions()) {
                // İzinleri talep et - Bildirimi gösterme
                requestRequiredPermissions();
                if (listener != null) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            listener.onWiFiConnectFailed());
                }
                return;
            }
            connectToESPAndroid10Plus();
        } else {
            connectToESPLegacy();
        }
    }

    private void connectToESPAndroid10Plus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                WifiNetworkSpecifier.Builder builder = new WifiNetworkSpecifier.Builder();
                builder.setSsid(ESP_SSID);
                builder.setWpa2Passphrase(ESP_PASSWORD);

                NetworkRequest request = new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                        .setNetworkSpecifier(builder.build())
                        .build();

                networkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        super.onAvailable(network);
                        connectivityManager.bindProcessToNetwork(network);
                        if (listener != null) {
                            new Handler(Looper.getMainLooper()).post(() ->
                                    listener.onWiFiConnected());
                        }
                    }

                    @Override
                    public void onLost(@NonNull Network network) {
                        super.onLost(network);
                        if (listener != null) {
                            new Handler(Looper.getMainLooper()).post(() ->
                                    listener.onWiFiDisconnected());
                        }
                    }

                    @Override
                    public void onUnavailable() {
                        super.onUnavailable();
                        if (listener != null) {
                            new Handler(Looper.getMainLooper()).post(() ->
                                    listener.onWiFiConnectFailed());
                        }
                    }
                };

                try {
                    connectivityManager.requestNetwork(request, networkCallback);
                } catch (SecurityException e) {
                    Log.e(TAG, "İzin hatası: " + e.getMessage());

                    // Alternatif olarak, WiFi ayarlarına yönlendir
                    if (context instanceof Activity) {
                        Activity activity = (Activity) context;
                        Intent intent = new Intent(Settings.Panel.ACTION_WIFI);
                        activity.startActivity(intent);

                        Toast.makeText(context, "Lütfen WiFi ayarlarından '" + ESP_SSID +
                                "' ağına manuel olarak bağlanın", Toast.LENGTH_LONG).show();
                    }

                    if (listener != null) {
                        new Handler(Looper.getMainLooper()).post(() ->
                                listener.onWiFiConnectFailed());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Bağlantı hatası: " + e.getMessage());
                if (listener != null) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            listener.onWiFiConnectFailed());
                }
            }
        }
    }

    private void connectToESPLegacy() {
        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + ESP_SSID + "\"";
        conf.preSharedKey = "\"" + ESP_PASSWORD + "\"";

        int netId = wifiManager.addNetwork(conf);
        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();

        for (WifiConfiguration config : list) {
            if (config.SSID != null && config.SSID.equals("\"" + ESP_SSID + "\"")) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(config.networkId, true);
                wifiManager.reconnect();

                // Bağlantı durumunu kontrol etmek için kullanacağımız bir handler
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (isConnectedToESP()) {
                        if (listener != null) {
                            listener.onWiFiConnected();
                        }
                    } else {
                        if (listener != null) {
                            listener.onWiFiConnectFailed();
                        }
                    }
                }, 5000); // 5 saniyelik gecikme

                break;
            }
        }
    }

    public void disconnect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (networkCallback != null) {
                try {
                    connectivityManager.unregisterNetworkCallback(networkCallback);
                    connectivityManager.bindProcessToNetwork(null);
                } catch (Exception e) {
                    Log.e(TAG, "Error disconnecting: " + e.getMessage());
                }
            }
        } else {
            wifiManager.disconnect();
        }

        if (listener != null) {
            listener.onWiFiDisconnected();
        }
    }

    // İzin kontrolü
    private boolean hasRequiredPermissions() {
        // CHANGE_NETWORK_STATE kontrolü
        boolean hasChangeNetworkState = context.checkSelfPermission(
                android.Manifest.permission.CHANGE_NETWORK_STATE) ==
                PackageManager.PERMISSION_GRANTED;

        // WRITE_SETTINGS kontrolü (özel işlem gerektirir)
        boolean hasWriteSettings = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hasWriteSettings = Settings.System.canWrite(context);
        }

        return hasChangeNetworkState;  // Sadece CHANGE_NETWORK_STATE kontrolü yeterli olabilir
    }

    // İzinleri talep etme
    private void requestRequiredPermissions() {
        // Ana aktivite üzerinden izin isteği yapılmalı
        if (context instanceof Activity) {
            Activity activity = (Activity) context;

            // CHANGE_NETWORK_STATE için izin isteme
            ActivityCompat.requestPermissions(activity,
                    new String[]{android.Manifest.permission.CHANGE_NETWORK_STATE},
                    100);

            // WRITE_SETTINGS için ayarlar sayfasına yönlendirme
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.System.canWrite(context)) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    intent.setData(Uri.parse("package:" + context.getPackageName()));
                    activity.startActivity(intent);
                }
            }
        }
    }
}