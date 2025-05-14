package com.kulucka.kontrol.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.cardview.widget.CardView;

import com.kulucka.kontrol.R;
import com.kulucka.kontrol.activities.MainActivity;
import com.kulucka.kontrol.models.Profile;
import com.kulucka.kontrol.models.AppSettings;

import java.util.ArrayList;
import java.util.List;

public class ControlFragment extends Fragment {
    private static final String TAG = "ControlFragment";

    // Sıcaklık ve nem kontrolü
    private TextView tvTempValue;
    private TextView tvHumValue;
    private SeekBar sbTemperature;
    private SeekBar sbHumidity;
    private Button btnSetTargets;

    // Motor kontrolü
    private Button btnTurnMotor;
    private TextView tvMotorInfo;
    private Switch switchMotorAuto;

    // Kuluçka kontrolü
    private Spinner spinnerProfiles;
    private Button btnStartIncubation;
    private Button btnStopIncubation;
    private TextView tvIncubationStatus;
    private CardView cvProfileInfo;
    private TextView tvProfileDetails;

    // Bağlantı durumu
    private boolean isConnected = false;

    // Veri modelleri
    private AppSettings currentSettings;
    private Profile currentProfile;
    private List<Profile> availableProfiles;
    private List<String> profileNames;
    private ArrayAdapter<String> profileAdapter;
    private int selectedProfileIndex = 0;

    // Ana aktivite referansı
    private MainActivity mainActivity;

    public ControlFragment() {
        // Gerekli boş kurucu
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Görünümü inflate et
        View view = inflater.inflate(R.layout.fragment_control, container, false);

        // Görünüm elemanlarını tanımla
        initViews(view);

        // Olayları ayarla
        setupListeners();

        // Profil listesi
        profileNames = new ArrayList<>();
        profileAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, profileNames);
        spinnerProfiles.setAdapter(profileAdapter);

        return view;
    }

    private void initViews(View view) {
        // Sıcaklık ve nem kontrolü
        tvTempValue = view.findViewById(R.id.tvTempValue);
        tvHumValue = view.findViewById(R.id.tvHumValue);
        sbTemperature = view.findViewById(R.id.sbTemperature);
        sbHumidity = view.findViewById(R.id.sbHumidity);
        btnSetTargets = view.findViewById(R.id.btnSetTargets);

        // Motor kontrolü
        btnTurnMotor = view.findViewById(R.id.btnTurnMotor);
        tvMotorInfo = view.findViewById(R.id.tvMotorInfo);
        switchMotorAuto = view.findViewById(R.id.switchMotorAuto);

        // Kuluçka kontrolü
        spinnerProfiles = view.findViewById(R.id.spinnerProfiles);
        btnStartIncubation = view.findViewById(R.id.btnStartIncubation);
        btnStopIncubation = view.findViewById(R.id.btnStopIncubation);
        tvIncubationStatus = view.findViewById(R.id.tvIncubationStatus);
        cvProfileInfo = view.findViewById(R.id.cvProfileInfo);
        tvProfileDetails = view.findViewById(R.id.tvProfileDetails);

        // Seekbar ayarları
        sbTemperature.setMax(100); // 30-40°C aralığı için (100 adım)
        sbHumidity.setMax(100);    // 30-90% aralığı için (100 adım)
    }

    private void setupListeners() {
        // Sıcaklık seekbar değişimi
        sbTemperature.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float temperature = 30.0f + (progress / 10.0f); // 30-40°C aralığı
                tvTempValue.setText(String.format("%.1f°C", temperature));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Nem seekbar değişimi
        sbHumidity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float humidity = 30.0f + (progress * 0.6f); // 30-90% aralığı
                tvHumValue.setText(String.format("%%%.1f", humidity));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Hedef değerleri ayarlama butonu
        btnSetTargets.setOnClickListener(v -> {
            if (isConnected) {
                float temperature = 30.0f + (sbTemperature.getProgress() / 10.0f);
                float humidity = 30.0f + (sbHumidity.getProgress() * 0.6f);

                mainActivity.setTargets(temperature, humidity);
                Toast.makeText(getContext(), "Hedef değerler gönderiliyor...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Bağlantı yok!", Toast.LENGTH_SHORT).show();
            }
        });

        // Motor çevirme butonu
        btnTurnMotor.setOnClickListener(v -> {
            if (isConnected) {
                mainActivity.controlMotor(true);
                Toast.makeText(getContext(), "Motor çalıştırılıyor...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Bağlantı yok!", Toast.LENGTH_SHORT).show();
            }
        });

        // Otomatik motor kontrolü switch
        switchMotorAuto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isConnected && currentSettings != null) {
                // Otomatik motor ayarını değiştir
                // Bu kısım için backend tarafında bir API eklenmesi gerekebilir
                Toast.makeText(getContext(), "Otomatik motor " +
                        (isChecked ? "aktif" : "devre dışı"), Toast.LENGTH_SHORT).show();
            }
        });

        // Profil seçimi
        spinnerProfiles.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedProfileIndex = position;
                updateProfileInfo();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Kuluçka başlatma butonu
        btnStartIncubation.setOnClickListener(v -> {
            if (isConnected && availableProfiles != null && !availableProfiles.isEmpty()) {
                Profile selectedProfile = availableProfiles.get(selectedProfileIndex);
                mainActivity.startIncubation(selectedProfile.getType());
                Toast.makeText(getContext(), "Kuluçka başlatılıyor...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Bağlantı yok veya profil seçilmedi!", Toast.LENGTH_SHORT).show();
            }
        });

        // Kuluçka durdurma butonu
        btnStopIncubation.setOnClickListener(v -> {
            if (isConnected) {
                mainActivity.stopIncubation();
                Toast.makeText(getContext(), "Kuluçka durduruluyor...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Bağlantı yok!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateProfileInfo() {
        if (availableProfiles != null && selectedProfileIndex >= 0 &&
                selectedProfileIndex < availableProfiles.size()) {

            Profile selectedProfile = availableProfiles.get(selectedProfileIndex);
            StringBuilder details = new StringBuilder();

            details.append("Türü: ").append(selectedProfile.getName()).append("\n");
            details.append("Toplam süre: ").append(selectedProfile.getTotalDays()).append(" gün\n\n");

            // Aşama bilgilerini ekle
            if (selectedProfile.getStages() != null) {
                details.append("Aşamalar:\n");
                for (Profile.Stage stage : selectedProfile.getStages()) {
                    details.append("- Gün ").append(stage.getStartDay())
                            .append("-").append(stage.getEndDay()).append(": ")
                            .append(String.format("%.1f°C / %%%.1f",
                                    stage.getTemperature(), stage.getHumidity()))
                            .append("\n");
                }
            }

            tvProfileDetails.setText(details.toString());
            cvProfileInfo.setVisibility(View.VISIBLE);
        } else {
            cvProfileInfo.setVisibility(View.GONE);
        }
    }

    // MainActivity'den çağrılan güncelleme metodları
    public void updateConnectionStatus(boolean connected) {
        this.isConnected = connected;

        if (isAdded()) {
            // UI elemanlarını bağlantı durumuna göre aktif/pasif yap
            btnSetTargets.setEnabled(connected);
            btnTurnMotor.setEnabled(connected);
            switchMotorAuto.setEnabled(connected);
            btnStartIncubation.setEnabled(connected);
            btnStopIncubation.setEnabled(connected);
        }
    }

    public void updateSettings(AppSettings settings) {
        if (isAdded() && settings != null) {
            this.currentSettings = settings;

            // Hedef değerleri UI'a yansıt
            float tempProgress = (settings.getTargetTemperature() - 30.0f) * 10.0f;
            float humProgress = (settings.getTargetHumidity() - 30.0f) / 0.6f;

            sbTemperature.setProgress((int) tempProgress);
            sbHumidity.setProgress((int) humProgress);

            // Otomatik motor durumunu güncelle
            switchMotorAuto.setChecked(settings.isMotorEnabled());

            // Kuluçka durumunu güncelle
            if (settings.getStartTime() > 0) {
                btnStartIncubation.setVisibility(View.GONE);
                btnStopIncubation.setVisibility(View.VISIBLE);
                tvIncubationStatus.setText(String.format("Kuluçka aktif - Gün %d / %d",
                        settings.getCurrentDay(), settings.getTotalDays()));
            } else {
                btnStartIncubation.setVisibility(View.VISIBLE);
                btnStopIncubation.setVisibility(View.GONE);
                tvIncubationStatus.setText("Kuluçka beklemede");
            }
        }
    }

    public void updateProfile(Profile profile) {
        if (isAdded() && profile != null) {
            this.currentProfile = profile;
        }
    }

    public void updateAllProfiles(List<Profile> profiles) {
        if (isAdded() && profiles != null) {
            this.availableProfiles = profiles;

            // Profil isimlerini güncelle
            profileNames.clear();
            for (Profile profile : profiles) {
                profileNames.add(profile.getName());
            }

            // Adapter'a değişikliği bildir
            profileAdapter.notifyDataSetChanged();

            // Profil bilgisini güncelle
            updateProfileInfo();
        }
    }
}