package com.kulucka.mkv5.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kulucka.mkv5.R;
import com.kulucka.mkv5.models.WifiNetwork;

import java.util.List;

public class WifiNetworkAdapter extends RecyclerView.Adapter<WifiNetworkAdapter.ViewHolder> {

    private final List<WifiNetwork> networks;
    private final OnNetworkClickListener listener;

    public interface OnNetworkClickListener {
        void onNetworkClick(WifiNetwork network);
    }

    public WifiNetworkAdapter(List<WifiNetwork> networks, OnNetworkClickListener listener) {
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
        WifiNetwork network = networks.get(position);
        holder.bind(network);
    }

    @Override
    public int getItemCount() {
        return networks.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvSsid;
        private final TextView tvSignalStrength;
        private final ImageView ivLock;
        private final ImageView ivSignal;

        ViewHolder(View itemView) {
            super(itemView);
            tvSsid = itemView.findViewById(R.id.tvSsid);
            tvSignalStrength = itemView.findViewById(R.id.tvSignalStrength);
            ivLock = itemView.findViewById(R.id.ivLock);
            ivSignal = itemView.findViewById(R.id.ivSignal);
        }

        void bind(WifiNetwork network) {
            tvSsid.setText(network.getSsid());
            tvSignalStrength.setText(String.format("%s (%d dBm)",
                    network.getSignalStrength(), network.getRssi()));

            // Set lock icon visibility
            ivLock.setVisibility(network.isOpen() ? View.GONE : View.VISIBLE);

            // Set signal strength icon
            int signalLevel = getSignalLevel(network.getRssi());
            ivSignal.setImageResource(getSignalDrawable(signalLevel));

            // Set click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNetworkClick(network);
                }
            });
        }

        private int getSignalLevel(int rssi) {
            if (rssi >= -50) return 4;
            if (rssi >= -60) return 3;
            if (rssi >= -70) return 2;
            return 1;
        }

        private int getSignalDrawable(int level) {
            switch (level) {
                case 4:
                    return R.drawable.ic_signal_4;
                case 3:
                    return R.drawable.ic_signal_3;
                case 2:
                    return R.drawable.ic_signal_2;
                default:
                    return R.drawable.ic_signal_1;
            }
        }
    }
}