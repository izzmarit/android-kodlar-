package com.kulucka.mk_v5.activities;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.kulucka.mk_v5.R;
import com.kulucka.mk_v5.fragments.AlarmSettingsFragment;
import com.kulucka.mk_v5.fragments.CalibrationFragment;
import com.kulucka.mk_v5.fragments.IncubationSettingsFragment;
import com.kulucka.mk_v5.fragments.MotorSettingsFragment;

public class SettingsActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Geri butonunu aktifleştir
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Ayarlar");
        }

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;

                if (item.getItemId() == R.id.nav_incubation) {
                    selectedFragment = new IncubationSettingsFragment();
                } else if (item.getItemId() == R.id.nav_motor) {
                    selectedFragment = new MotorSettingsFragment();
                } else if (item.getItemId() == R.id.nav_pid) {
                    selectedFragment = new PidSettingsFragment();
                } else if (item.getItemId() == R.id.nav_calibration) {
                    selectedFragment = new CalibrationFragment();
                } else if (item.getItemId() == R.id.nav_alarm) {
                    selectedFragment = new AlarmSettingsFragment();
                }

                if (selectedFragment != null) {
                    loadFragment(selectedFragment);
                    return true;
                }

                return false;
            }
        });

        // Varsayılan olarak kuluçka ayarları fragmentını yükle
        loadFragment(new IncubationSettingsFragment());
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}