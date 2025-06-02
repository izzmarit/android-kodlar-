package com.kulucka.mk_v5.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.kulucka.mk_v5.R;
import com.kulucka.mk_v5.utils.NetworkUtils;
import com.kulucka.mk_v5.utils.SharedPrefsManager;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY = 1500; // 1.5 saniye

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // Otomatik bağlantı etkinse, cihaza bağlanmayı dene
                if (SharedPrefsManager.getInstance().isAutoConnectEnabled()) {
                    tryToConnectDevice();
                }

                // Ana ekrana geçiş yap
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            }
        }, SPLASH_DELAY);
    }

    private void tryToConnectDevice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.CHANGE_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(android.Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
                // Permission eksikse bağlantı deneme
                return;
            }
        }

        String ssid = SharedPrefsManager.getInstance().getApSsid();
        String password = SharedPrefsManager.getInstance().getApPassword();

        // WiFi bağlantısını başlat
        NetworkUtils.getInstance(this).connectToWifi(ssid, password);
    }
}