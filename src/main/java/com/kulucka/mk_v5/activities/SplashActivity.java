package com.kulucka.mk_v5.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.kulucka.mk_v5.R;
import com.kulucka.mk_v5.utils.NetworkUtils;
import com.kulucka.mk_v5.utils.SharedPrefsManager;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final long SPLASH_DELAY = 1500; // 1.5 saniye

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // SharedPrefsManager'ı initialize et
        initializeSharedPrefsManager();

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    // Otomatik bağlantı etkinse, cihaza bağlanmayı dene
                    if (SharedPrefsManager.getInstance().isAutoConnectEnabled()) {
                        tryToConnectDevice();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error during auto connect check: " + e.getMessage());
                }

                // Ana ekrana geçiş yap
                startMainActivity();
            }
        }, SPLASH_DELAY);
    }

    private void initializeSharedPrefsManager() {
        try {
            SharedPrefsManager.initialize(this);
            Log.d(TAG, "SharedPrefsManager initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize SharedPrefsManager: " + e.getMessage());
        }
    }

    private void tryToConnectDevice() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(android.Manifest.permission.CHANGE_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED ||
                        checkSelfPermission(android.Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
                    // Permission eksikse bağlantı deneme
                    Log.w(TAG, "Network permissions not granted, skipping auto connect");
                    return;
                }
            }

            String ssid = SharedPrefsManager.getInstance().getApSsid();
            String password = SharedPrefsManager.getInstance().getApPassword();

            if (ssid != null && !ssid.isEmpty() && password != null && !password.isEmpty()) {
                // WiFi bağlantısını başlat
                NetworkUtils.getInstance(this).connectToWifi(ssid, password);
                Log.d(TAG, "Auto connect initiated for SSID: " + ssid);
            } else {
                Log.w(TAG, "SSID or password is empty, skipping auto connect");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during auto connect: " + e.getMessage());
        }
    }

    private void startMainActivity() {
        try {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error starting MainActivity: " + e.getMessage());
            // Hata durumunda da finish() çağır
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // NetworkUtils cleanup
        try {
            NetworkUtils networkUtils = NetworkUtils.getInstance(this);
            if (networkUtils != null) {
                networkUtils.cleanup();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup: " + e.getMessage());
        }
    }
}