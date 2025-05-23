package com.kulucka.mk_v5.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.kulucka.mk_v5.services.ApiService;
import com.kulucka.mk_v5.services.DataRefreshService;

public class NetworkChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            if (cm != null) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

                if (isConnected && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                    // WiFi'ya bağlandığında kontrol et
                    if (NetworkUtils.getInstance(context).isConnectedToDeviceWifi()) {
                        // ESP32 cihazına bağlantı varsa servisi başlat ve durum güncelle
                        context.startService(new Intent(context, DataRefreshService.class));
                        ApiService.getInstance().refreshStatus();
                    }
                }
            }
        }
    }
}