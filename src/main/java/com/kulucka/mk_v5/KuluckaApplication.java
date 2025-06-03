package com.kulucka.mk_v5;

import android.app.Application;
import android.util.Log;

import com.kulucka.mk_v5.services.ApiService;

public class KuluckaApplication extends Application {

    private static final String TAG = "KuluckaApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "Kulucka Application starting...");

        // Initialize ApiService with application context
        try {
            ApiService.getInstance().initialize(this);
            Log.d(TAG, "ApiService initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize ApiService: " + e.getMessage());
        }

        Log.d(TAG, "Kulucka Application started successfully");
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        Log.d(TAG, "Kulucka Application terminating...");

        // Cancel any pending network requests
        try {
            ApiService.getInstance().cancelAllRequests();
            Log.d(TAG, "ApiService cleanup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error during ApiService cleanup: " + e.getMessage());
        }
    }
}