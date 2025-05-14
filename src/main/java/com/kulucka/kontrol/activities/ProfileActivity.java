package com.kulucka.kontrol.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.kulucka.kontrol.R;
import com.kulucka.kontrol.models.Profile;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";

    // Görünüm elemanları
    private ListView lvProfiles;
    private TextView tvNoProfiles;
    private Button btnStartProfile;

    // Veri modelleri
    private List<Profile> availableProfiles;
    private ArrayAdapter<String> profileAdapter;
    private List<String> profileNames;
    private int selectedProfileIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Geri butonu ekle
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Profil Seçimi");
        }

        // Görünüm elemanlarını tanımla
        initViews();

        // Intent'ten profil listesini al
        getProfilesFromIntent();

        // Olayları ayarla
        setupListeners();
    }

    private void initViews() {
        lvProfiles = findViewById(R.id.lvProfiles);
        tvNoProfiles = findViewById(R.id.tvNoProfiles);
        btnStartProfile = findViewById(R.id.btnStartProfile);

        // Başlangıçta başlatma butonunu devre dışı bırak
        btnStartProfile.setEnabled(false);
    }

    private void getProfilesFromIntent() {
        // Intent'ten profil listesini al
        // Bunun yerine test için dummy veri oluştur
        createDummyProfiles();

        // Profil isimleri listesi oluştur
        profileNames = new ArrayList<>();
        for (Profile profile : availableProfiles) {
            profileNames.add(profile.getName());
        }

        // ListView adaptörü
        profileAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_single_choice, profileNames);
        lvProfiles.setAdapter(profileAdapter);
        lvProfiles.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        // Profil varsa liste görünür, yoksa mesaj görünür
        if (profileNames.isEmpty()) {
            lvProfiles.setVisibility(View.GONE);
            tvNoProfiles.setVisibility(View.VISIBLE);
            btnStartProfile.setEnabled(false);
        } else {
            lvProfiles.setVisibility(View.VISIBLE);
            tvNoProfiles.setVisibility(View.GONE);
        }
    }

    private void createDummyProfiles() {
        // Test için dummy profil verileri oluştur
        availableProfiles = new ArrayList<>();

        // Profil 1: Tavuk
        Profile chickenProfile = new Profile();
        chickenProfile.setType(0);
        chickenProfile.setName("Tavuk");
        chickenProfile.setTotalDays(21);

        List<Profile.Stage> chickenStages = new ArrayList<>();

        // Aşama 1
        Profile.Stage stage1 = new Profile.Stage();
        stage1.setTemperature(37.8f);
        stage1.setHumidity(60.0f);
        stage1.setMotorActive(true);
        stage1.setStartDay(1);
        stage1.setEndDay(18);
        chickenStages.add(stage1);

        // Aşama 2
        Profile.Stage stage2 = new Profile.Stage();
        stage2.setTemperature(37.2f);
        stage2.setHumidity(70.0f);
        stage2.setMotorActive(true);
        stage2.setStartDay(19);
        stage2.setEndDay(21);
        chickenStages.add(stage2);

        chickenProfile.setStages(chickenStages);
        availableProfiles.add(chickenProfile);

        // Profil 2: Ördek
        Profile duckProfile = new Profile();
        duckProfile.setType(1);
        duckProfile.setName("Ördek");
        duckProfile.setTotalDays(28);

        List<Profile.Stage> duckStages = new ArrayList<>();

        // Aşama 1
        Profile.Stage duckStage1 = new Profile.Stage();
        duckStage1.setTemperature(37.5f);
        duckStage1.setHumidity(65.0f);
        duckStage1.setMotorActive(true);
        duckStage1.setStartDay(1);
        duckStage1.setEndDay(25);
        duckStages.add(duckStage1);

        // Aşama 2
        Profile.Stage duckStage2 = new Profile.Stage();
        duckStage2.setTemperature(37.0f);
        duckStage2.setHumidity(75.0f);
        duckStage2.setMotorActive(true);
        duckStage2.setStartDay(26);
        duckStage2.setEndDay(28);
        duckStages.add(duckStage2);

        duckProfile.setStages(duckStages);
        availableProfiles.add(duckProfile);
    }

    private void setupListeners() {
        // Profil seçimi
        lvProfiles.setOnItemClickListener((parent, view, position, id) -> {
            selectedProfileIndex = position;
            btnStartProfile.setEnabled(true);

            // Seçili profil detaylarını göster
            showProfileDetails(availableProfiles.get(position));
        });

        // Profil başlatma butonu
        btnStartProfile.setOnClickListener(v -> {
            if (selectedProfileIndex >= 0 && selectedProfileIndex < availableProfiles.size()) {
                Profile selectedProfile = availableProfiles.get(selectedProfileIndex);
                startIncubation(selectedProfile);
            }
        });
    }

    private void showProfileDetails(Profile profile) {
        // Profil detaylarını içeren dialog göster
        StringBuilder details = new StringBuilder();

        details.append("Profil: ").append(profile.getName()).append("\n");
        details.append("Toplam süre: ").append(profile.getTotalDays()).append(" gün\n\n");

        // Aşama bilgilerini ekle
        if (profile.getStages() != null) {
            details.append("Aşamalar:\n");
            for (Profile.Stage stage : profile.getStages()) {
                details.append("- Gün ").append(stage.getStartDay())
                        .append("-").append(stage.getEndDay()).append(": ")
                        .append(String.format("%.1f°C / %%%.1f",
                                stage.getTemperature(), stage.getHumidity()))
                        .append("\n");
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Profil Detayları")
                .setMessage(details.toString())
                .setPositiveButton("Tamam", null)
                .show();
    }

    private void startIncubation(Profile profile) {
        // Onay dialog'u göster
        new AlertDialog.Builder(this)
                .setTitle("Kuluçka Başlat")
                .setMessage(profile.getName() + " profili ile kuluçka başlatmak istediğinizden emin misiniz?\n\n" +
                        "Toplam süre: " + profile.getTotalDays() + " gün")
                .setPositiveButton("Başlat", (dialog, which) -> {
                    // MainActivity'e dön ve kuluçkayı başlat
                    // Intent result ile profile.getType() değerini döndür
                    Toast.makeText(this, "Kuluçka başlatılıyor: " + profile.getName(),
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Geri tuşuna basıldığında aktiviteyi kapat
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}