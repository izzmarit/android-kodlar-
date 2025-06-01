package com.kulucka.mk_v5.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import com.kulucka.mk_v5.R;
import com.kulucka.mk_v5.models.IncubationStatus;
import com.kulucka.mk_v5.services.ApiService;

public class DashboardFragment extends Fragment {

    private TextView tvTemperature;
    private TextView tvHumidity;
    private TextView tvTargetTemp;
    private TextView tvTargetHumidity;
    private TextView tvDay;
    private TextView tvType;
    private TextView tvHeaterStatus;
    private TextView tvHumidifierStatus;
    private TextView tvMotorStatus;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Arayüz elemanlarını başlat
        tvTemperature = view.findViewById(R.id.tv_temperature);
        tvHumidity = view.findViewById(R.id.tv_humidity);
        tvTargetTemp = view.findViewById(R.id.tv_target_temp);
        tvTargetHumidity = view.findViewById(R.id.tv_target_humidity);
        tvDay = view.findViewById(R.id.tv_day);
        tvType = view.findViewById(R.id.tv_type);
        tvHeaterStatus = view.findViewById(R.id.tv_heater_status);
        tvHumidifierStatus = view.findViewById(R.id.tv_humidifier_status);
        tvMotorStatus = view.findViewById(R.id.tv_motor_status);

        // Verileri gözlemle
        ApiService.getInstance().getStatusLiveData().observe(getViewLifecycleOwner(), new Observer<IncubationStatus>() {
            @Override
            public void onChanged(IncubationStatus status) {
                updateUI(status);
            }
        });

        return view;
    }

    private void updateUI(IncubationStatus status) {
        if (status != null) {
            tvTemperature.setText(String.format("%.1f°C", status.getTemperature()));
            tvHumidity.setText(String.format("%.0f%%", status.getHumidity()));
            tvTargetTemp.setText(String.format("%.1f°C", status.getTargetTemp()));
            tvTargetHumidity.setText(String.format("%.0f%%", status.getTargetHumid()));
            tvDay.setText(String.format("%d/%d", status.getCurrentDay(), status.getTotalDays()));
            tvType.setText(status.getIncubationType());

            tvHeaterStatus.setText(status.isHeaterState() ? "AÇIK" : "KAPALI");
            tvHumidifierStatus.setText(status.isHumidifierState() ? "AÇIK" : "KAPALI");
            tvMotorStatus.setText(status.isMotorState() ? "AÇIK" : "KAPALI");

            // Durum renkleri
            int activeColor = getResources().getColor(R.color.colorActive);
            int inactiveColor = getResources().getColor(R.color.colorInactive);

            tvHeaterStatus.setTextColor(status.isHeaterState() ? activeColor : inactiveColor);
            tvHumidifierStatus.setTextColor(status.isHumidifierState() ? activeColor : inactiveColor);
            tvMotorStatus.setTextColor(status.isMotorState() ? activeColor : inactiveColor);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Fragment tekrar görünür olduğunda verileri güncelle
        ApiService.getInstance().refreshStatus();
    }
}