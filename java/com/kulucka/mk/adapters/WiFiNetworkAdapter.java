package com.kulucka.mk.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.kulucka.mk.R;
import com.kulucka.mk.models.WiFiNetwork;
import com.kulucka.mk.utils.NetworkUtils;
import com.kulucka.mk.utils.Utils;

import java.util.List;

/**
 * WiFi ağları listesi için RecyclerView Adapter
 */
public class WiFiNetworkAdapter extends RecyclerView.Adapter<WiFiNetworkAdapter.ViewHolder> {

    private List<WiFiNetwork> networks;
    private OnNetworkClickListener listener;

    public interface OnNetworkClickListener {
        void onNetworkClick(WiFiNetwork network);
    }

    public WiFiNetworkAdapter(List<WiFiNetwork> networks, OnNetworkClickListener listener) {
        this.networks = networks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_wifi_network, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WiFiNetwork network = networks.get(position);
        holder.bind(network);
    }

    @Override
    public int getItemCount() {
        return networks.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvSSID;
        private TextView tvSecurity;
        private TextView tvSignalStrength;
        private ImageView ivSignalIcon;
        private ImageView ivSecurityIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvSSID = itemView.findViewById(R.id.tvSSID);
            tvSecurity = itemView.findViewById(R.id.tvSecurity);
            tvSignalStrength = itemView.findViewById(R.id.tvSignalStrength);
            ivSignalIcon = itemView.findViewById(R.id.ivSignalIcon);
            ivSecurityIcon = itemView.findViewById(R.id.ivSecurityIcon);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onNetworkClick(networks.get(position));
                    }
                }
            });
        }

        public void bind(WiFiNetwork network) {
            // SSID
            tvSSID.setText(network.getSsid());

            // Güvenlik durumu
            if (network.isOpen()) {
                tvSecurity.setText("Açık");
                tvSecurity.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.color_success));
                ivSecurityIcon.setVisibility(View.GONE);
            } else {
                tvSecurity.setText("Şifreli");
                tvSecurity.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.color_warning));
                ivSecurityIcon.setVisibility(View.VISIBLE);
                ivSecurityIcon.setImageResource(R.drawable.ic_lock);
                ivSecurityIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.color_warning));
            }

            // Sinyal gücü
            int rssi = network.getRssi();
            tvSignalStrength.setText(rssi + " dBm (" + NetworkUtils.getSignalStrengthDescription(rssi) + ")");

            // Sinyal gücü rengi
            int signalColor = Utils.getSignalStrengthColor(itemView.getContext(), rssi);
            tvSignalStrength.setTextColor(signalColor);

            // Sinyal ikonu
            ivSignalIcon.setImageResource(getSignalIcon(rssi));
            ivSignalIcon.setColorFilter(signalColor);
        }

        private int getSignalIcon(int rssi) {
            if (rssi >= -50) {
                return R.drawable.ic_signal_4;
            } else if (rssi >= -60) {
                return R.drawable.ic_signal_3;
            } else if (rssi >= -70) {
                return R.drawable.ic_signal_2;
            } else if (rssi >= -80) {
                return R.drawable.ic_signal_1;
            } else {
                return R.drawable.ic_signal_0;
            }
        }
    }
}