package com.kulucka.kontrol.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.kulucka.kontrol.R;
import com.kulucka.kontrol.adapters.ViewPagerAdapter;
import com.kulucka.kontrol.fragments.ControlFragment;
import com.kulucka.kontrol.fragments.DashboardFragment;
import com.kulucka.kontrol.fragments.GraphFragment;
import com.kulucka.kontrol.models.AlarmData;
import com.kulucka.kontrol.models.Profile;
import com.kulucka.kontrol.models.SensorData;
import com.kulucka.kontrol.models.AppSettings; // Settings yerine AppSettings kullanıyoruz
import com.kulucka.kontrol.services.ConnectionService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNav;

    private ConnectionService connectionService;
    private boolean bound = false;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action != null) {
                if (action.equals(ConnectionService.ACTION_CONNECTION_STATUS)) {
                    boolean connected = intent.getBooleanExtra(ConnectionService.EXTRA_CONNECTED, false);
                    updateConnectionStatus(connected);
                } else if (action.equals(ConnectionService.ACTION_SENSOR_DATA_UPDATED)) {
                    SensorData sensorData = (SensorData) intent.getSerializableExtra(ConnectionService.EXTRA_SENSOR_DATA);
                    updateSensorData(sensorData);
                } else if (action.equals(ConnectionService.ACTION_SETTINGS_UPDATED)) {
                    AppSettings settings = (AppSettings) intent.getSerializableExtra(ConnectionService.EXTRA_SETTINGS);
                    updateSettings(settings);
                } else if (action.equals(ConnectionService.ACTION_PROFILE_UPDATED)) {
                    Profile profile = (Profile) intent.getSerializableExtra(ConnectionService.EXTRA_PROFILE);
                    updateProfile(profile);
                } else if (action.equals(ConnectionService.ACTION_ALARM_UPDATED)) {
                    AlarmData alarmData = (AlarmData) intent.getSerializableExtra(ConnectionService.EXTRA_ALARM_DATA);
                    updateAlarmData(alarmData);
                } else if (action.equals(ConnectionService.ACTION_ALL_PROFILES_UPDATED)) {
                    @SuppressWarnings("unchecked")
                    List<Profile> profiles = (List<Profile>) intent.getSerializableExtra(ConnectionService.EXTRA_ALL_PROFILES);
                    updateAllProfiles(profiles);
                }
            }
        }
    };

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ConnectionService.LocalBinder binder = (ConnectionService.LocalBinder) service;
            connectionService = binder.getService();
            bound = true;

            // Servis bağlandı, veri güncellemelerini başlat
            updateInitialData();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // İzinleri kontrol et ve iste
        checkAndRequestPermissions();

        // View elemanlarını tanımla
        viewPager = findViewById(R.id.viewPager);
        bottomNav = findViewById(R.id.bottomNavigation);

        // ViewPager için fragmentları ayarla
        setupViewPager();

        // BottomNavigation için dinleyici ekle
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_dashboard) {
                viewPager.setCurrentItem(0);
                return true;
            } else if (itemId == R.id.nav_control) {
                viewPager.setCurrentItem(1);
                return true;
            } else if (itemId == R.id.nav_graph) {
                viewPager.setCurrentItem(2);
                return true;
            }
            return false;
        });

        // Servisi başlat
        startConnectionService();

        // Broadcast receiver'ı kaydet
        registerBroadcastReceiver();
    }

    private void checkAndRequestPermissions() {
        // Normal izinleri iste
        List<String> permissionsNeeded = new ArrayList<>();

        if (checkSelfPermission(Manifest.permission.CHANGE_NETWORK_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CHANGE_NETWORK_STATE);
        }

        if (!permissionsNeeded.isEmpty()) {
            requestPermissions(permissionsNeeded.toArray(new String[0]), 100);
        }

        // WRITE_SETTINGS izni (özel kontrolle)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                Toast.makeText(this, "Uygulamanın sistem ayarlarını değiştirme izni gerekiyor",
                        Toast.LENGTH_LONG).show();
                startActivity(intent);
            }
        }
    }

    private void setupViewPager() {
        List<Fragment> fragments = new ArrayList<>();
        fragments.add(new DashboardFragment());
        fragments.add(new ControlFragment());
        fragments.add(new GraphFragment());

        ViewPagerAdapter adapter = new ViewPagerAdapter(this, fragments);
        viewPager.setAdapter(adapter);

        // ViewPager sayfa değişim dinleyicisi
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        bottomNav.setSelectedItemId(R.id.nav_dashboard);
                        break;
                    case 1:
                        bottomNav.setSelectedItemId(R.id.nav_control);
                        break;
                    case 2:
                        bottomNav.setSelectedItemId(R.id.nav_graph);
                        break;
                }
            }
        });
    }

    private void startConnectionService() {
        Intent serviceIntent = new Intent(this, ConnectionService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // Servise bağlan
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void registerBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectionService.ACTION_CONNECTION_STATUS);
        filter.addAction(ConnectionService.ACTION_SENSOR_DATA_UPDATED);
        filter.addAction(ConnectionService.ACTION_SETTINGS_UPDATED);
        filter.addAction(ConnectionService.ACTION_PROFILE_UPDATED);
        filter.addAction(ConnectionService.ACTION_ALARM_UPDATED);
        filter.addAction(ConnectionService.ACTION_ALL_PROFILES_UPDATED);

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, filter);
    }

    private void updateInitialData() {
        if (bound && connectionService != null) {
            // Eğer servis bağlıysa ve önceden veri alınmışsa, fragmentları güncelle
            SensorData sensorData = connectionService.getLastSensorData();
            AppSettings settings = connectionService.getLastSettings();
            Profile profile = connectionService.getLastProfile();
            AlarmData alarmData = connectionService.getLastAlarmData();
            List<Profile> profiles = connectionService.getAllProfiles();

            if (sensorData != null) {
                updateSensorData(sensorData);
            }

            if (settings != null) {
                updateSettings(settings);
            }

            if (profile != null) {
                updateProfile(profile);
            }

            if (alarmData != null) {
                updateAlarmData(alarmData);
            }

            if (profiles != null) {
                updateAllProfiles(profiles);
            }

            // Bağlantı durumunu güncelle
            updateConnectionStatus(connectionService.isConnected());
        }
    }

    private void updateConnectionStatus(boolean connected) {
        // Ana aktivite başlık çubuğunu güncelle
        runOnUiThread(() -> {
            if (connected) {
                setTitle(getString(R.string.app_name) + " - Bağlı");
            } else {
                setTitle(getString(R.string.app_name) + " - Bağlantı Kesik");
            }

            // Her fragment'a bağlantı durumunu bildirmek için
            for (Fragment fragment : getSupportFragmentManager().getFragments()) {
                if (fragment instanceof DashboardFragment) {
                    ((DashboardFragment) fragment).updateConnectionStatus(connected);
                } else if (fragment instanceof ControlFragment) {
                    ((ControlFragment) fragment).updateConnectionStatus(connected);
                } else if (fragment instanceof GraphFragment) {
                    ((GraphFragment) fragment).updateConnectionStatus(connected);
                }
            }
        });
    }

    private void updateSensorData(SensorData sensorData) {
        // Fragment'lara sensör verilerini ilet
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof DashboardFragment) {
                ((DashboardFragment) fragment).updateSensorData(sensorData);
            } else if (fragment instanceof GraphFragment) {
                ((GraphFragment) fragment).updateSensorData(sensorData);
            }
        }
    }

    private void updateSettings(AppSettings settings) {
        // Fragment'lara ayarları ilet
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof DashboardFragment) {
                ((DashboardFragment) fragment).updateSettings(settings);
            } else if (fragment instanceof ControlFragment) {
                ((ControlFragment) fragment).updateSettings(settings);
            }
        }
    }

    private void updateProfile(Profile profile) {
        // Fragment'lara profil bilgilerini ilet
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof DashboardFragment) {
                ((DashboardFragment) fragment).updateProfile(profile);
            } else if (fragment instanceof ControlFragment) {
                ((ControlFragment) fragment).updateProfile(profile);
            }
        }
    }

    private void updateAlarmData(AlarmData alarmData) {
        // Fragment'lara alarm verilerini ilet
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof DashboardFragment) {
                ((DashboardFragment) fragment).updateAlarmData(alarmData);
            }
        }
    }

    private void updateAllProfiles(List<Profile> profiles) {
        // Fragment'lara tüm profil listesini ilet
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof ControlFragment) {
                ((ControlFragment) fragment).updateAllProfiles(profiles);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            // Manuel yenileme
            if (bound && connectionService != null) {
                connectionService.connectToESP();
                Toast.makeText(this, "Bağlantı yenileniyor...", Toast.LENGTH_SHORT).show();
            }
            return true;
        } else if (id == R.id.action_settings) {
            // Ayarlar ekranına git
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Servis bağlantısını kontrol et
        if (!bound) {
            Intent intent = new Intent(this, ConnectionService.class);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Aktivite duraklatıldığında servis bağlantısını kes
        if (bound) {
            unbindService(serviceConnection);
            bound = false;
        }
    }

    @Override
    protected void onDestroy() {
        // Broadcast receiver'ı kaldır
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);

        super.onDestroy();
    }

    // Servis API'si için yardımcı metotlar
    public void setTargets(float temperature, float humidity) {
        if (bound && connectionService != null) {
            connectionService.setTargets(temperature, humidity);
        }
    }

    public void controlMotor(boolean state) {
        if (bound && connectionService != null) {
            connectionService.controlMotor(state);
        }
    }

    public void startIncubation(int profileType) {
        if (bound && connectionService != null) {
            connectionService.startIncubation(profileType);
        }
    }

    public void stopIncubation() {
        if (bound && connectionService != null) {
            connectionService.stopIncubation();
        }
    }
}