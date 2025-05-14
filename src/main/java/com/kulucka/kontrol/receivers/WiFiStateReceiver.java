package com.kulucka.kontrol.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.kulucka.kontrol.services.ConnectionService;

public class WiFiStateReceiver extends BroadcastReceiver {
    private static final String TAG = "WiFiStateReceiver";
    private static final String ESP_SSID = "KuluçkaKontrol"; // ESP32 AP SSID

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action != null) {
            if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);

                if (networkInfo != null && networkInfo.isConnected()) {
                    // WiFi bağlandı, ESP32 mi kontrol et
                    checkAndStartServiceIfConnectedToESP(context);
                }
            } else if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
                // Cihaz yeniden başlatıldı, servisi başlat
                startConnectionService(context);
            }
        }
    }

    private void checkAndStartServiceIfConnectedToESP(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();

            if (wifiInfo != null) {
                String ssid = wifiInfo.getSSID().replace("\"", "");

                if (ssid.equals(ESP_SSID)) {
                    // ESP32'ye bağlandı, servisi başlat
                    Log.d(TAG, "ESP32 AP'ye bağlandı, servisi başlatıyor");
                    startConnectionService(context);
                }
            }
        }
    }

    private void startConnectionService(Context context) {
        Intent serviceIntent = new Intent(context, ConnectionService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}