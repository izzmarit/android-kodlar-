package com.kulucka.mk_v5.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.kulucka.mk_v5.services.ApiService;

public class NetworkChangeReceiver extends BroadcastReceiver {

    private static final String TAG = "NetworkChangeReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Network change detected");

        if (isNetworkAvailable(context)) {
            Log.d(TAG, "Network is available, testing ESP32 connection");

            // Wait a moment for network to stabilize, then test connection
            new android.os.Handler().postDelayed(() -> {
                testEsp32Connection();
            }, 2000);

        } else {
            Log.d(TAG, "Network is not available");
        }
    }

    private boolean isNetworkAvailable(Context context) {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);

            if (connectivityManager != null) {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                return activeNetworkInfo != null && activeNetworkInfo.isConnected();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking network availability: " + e.getMessage());
        }

        return false;
    }

    private void testEsp32Connection() {
        ApiService.getInstance().testConnection(new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "ESP32 connection test successful after network change");

                // Refresh data after successful connection
                refreshEsp32Data();
            }

            @Override
            public void onError(String message) {
                Log.w(TAG, "ESP32 connection test failed after network change: " + message);
            }
        });
    }

    private void refreshEsp32Data() {
        // Get fresh data from ESP32
        ApiService.getInstance().getSensorData(new ApiService.DataCallback() {
            @Override
            public void onSuccess(org.json.JSONObject data) {
                Log.d(TAG, "ESP32 data refreshed successfully after network change");
            }

            @Override
            public void onError(String message) {
                Log.w(TAG, "Failed to refresh ESP32 data after network change: " + message);
            }
        });

        // Also get status information
        ApiService.getInstance().getStatus(new ApiService.DataCallback() {
            @Override
            public void onSuccess(org.json.JSONObject data) {
                Log.d(TAG, "ESP32 status refreshed successfully after network change");
            }

            @Override
            public void onError(String message) {
                Log.w(TAG, "Failed to refresh ESP32 status after network change: " + message);
            }
        });
    }
}