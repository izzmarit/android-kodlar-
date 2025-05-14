package com.kulucka.kontrol.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kulucka.kontrol.R;
import com.kulucka.kontrol.models.Profile;

import java.util.List;

public class ProfileListAdapter extends ArrayAdapter<Profile> {
    private Context context;
    private List<Profile> profiles;

    public ProfileListAdapter(@NonNull Context context, List<Profile> profiles) {
        super(context, 0, profiles);
        this.context = context;
        this.profiles = profiles;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // Görünüm oluşturma
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_profile, parent, false);
        }

        // Geçerli profil
        Profile profile = profiles.get(position);

        // Görünüm elemanları
        TextView tvProfileName = convertView.findViewById(R.id.tvProfileName);
        TextView tvProfileDays = convertView.findViewById(R.id.tvProfileDays);
        TextView tvProfileInfo = convertView.findViewById(R.id.tvProfileInfo);

        // Veri dolumu
        tvProfileName.setText(profile.getName());
        tvProfileDays.setText(String.format("Toplam %d gün", profile.getTotalDays()));

        // Özet bilgi
        StringBuilder infoBuilder = new StringBuilder();
        if (profile.getStages() != null && !profile.getStages().isEmpty()) {
            Profile.Stage firstStage = profile.getStages().get(0);
            infoBuilder.append(String.format("Başlangıç: %.1f°C, %%%.1f",
                    firstStage.getTemperature(), firstStage.getHumidity()));
        }
        tvProfileInfo.setText(infoBuilder.toString());

        return convertView;
    }
}