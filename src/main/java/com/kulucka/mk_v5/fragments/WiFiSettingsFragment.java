package com.kulucka.mk_v5.fragments;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kulucka.mk_v5.R;
import com.kulucka.mk_v5.activities.MainActivity;
import com.kulucka.mk_v5.services.ApiService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

public class WiFiSettingsFragment extends Fragment {

    private static final String TAG = "WiFiSettings";
    private static final String PREF_NAME = "KuluckaPrefs";
    private static final String KEY_ESP32_IP = "esp32_ip";

    // UI Elements
    private EditText etEsp32Ip;
    private Button btnSaveIp;
    private Button btnTestConnection;
    private Button btnScanNetworks;
    private ListView lvWifiNetworks;
    private ProgressBar progressScan;
    private TextView tvConnectionStatus;
    private TextView tvCurrentWifi;

    // Data
    private List<String> wifiNetworks;
    private ArrayAdapter<String> wifiAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wifi_settings, container, false);

        initializeViews(view);
        setupClickListeners();
        loadSavedSettings();

        return view;
    }

    private void initializeViews(View view) {
        etEsp32Ip = view.findViewById(R.id.et_esp32_ip);
        btnSaveIp = view.findViewById(R.id.btn_save_ip);
        btnTestConnection = view.findViewById(R.id.btn_test_connection);
        btnScanNetworks = view.findViewById(R.id.btn_scan_networks);
        lvWifiNetworks = view.findViewById(R.id.lv_wifi_networks);
        progressScan = view.findViewById(R.id.progress_scan);
        tvConnectionStatus = view.findViewById(R.id.tv_connection_status);
        tvCurrentWifi = view.findViewById(R.id.tv_current_wifi);

        // Setup WiFi networks list
        wifiNetworks = new ArrayList<>();
        wifiAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, wifiNetworks);
        if (lvWifiNetworks != null) {
            lvWifiNetworks.setAdapter(wifiAdapter);
        }

        // Hide progress bar initially
        if (progressScan != null) {
            progressScan.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        if (btnSaveIp != null) {
            btnSaveIp.setOnClickListener(v -> saveIpAddress());
        }

        if (btnTestConnection != null) {
            btnTestConnection.setOnClickListener(v -> testConnection());
        }

        if (btnScanNetworks != null) {
            btnScanNetworks.setOnClickListener(v -> scanWifiNetworks());
        }

        if (lvWifiNetworks != null) {
            lvWifiNetworks.setOnItemClickListener((parent, view, position, id) -> {
                String selectedNetwork = wifiNetworks.get(position);
                showWifiPasswordDialog(selectedNetwork);
            });
        }
    }

    private void loadSavedSettings() {
        if (getContext() != null) {
            SharedPreferences prefs = getContext().getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            String savedIp = prefs.getString(KEY_ESP32_IP, "192.168.1.100");

            if (etEsp32Ip != null) {
                etEsp32Ip.setText(savedIp);
            }

            updateConnectionStatus("Bilinmiyor");
            testConnection(); // Auto-test on load
        }
    }

    private void saveIpAddress() {
        if (etEsp32Ip == null || TextUtils.isEmpty(etEsp32Ip.getText())) {
            showToast("IP adresi boş olamaz");
            return;
        }

        String ipAddress = etEsp32Ip.getText().toString().trim();

        if (!isValidIpAddress(ipAddress)) {
            showToast("Geçersiz IP adresi formatı");
            return;
        }

        // Save to SharedPreferences
        if (getContext() != null) {
            SharedPreferences prefs = getContext().getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            prefs.edit().putString(KEY_ESP32_IP, ipAddress).apply();
        }

        // Update ApiService
        ApiService.getInstance().setBaseUrl("http://" + ipAddress);

        // Update MainActivity
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateIpAddress(ipAddress);
        }

        showToast("IP adresi kaydedildi");
        testConnection();

        Log.d(TAG, "ESP32 IP address saved: " + ipAddress);
    }

    private boolean isValidIpAddress(String ip) {
        if (TextUtils.isEmpty(ip)) return false;

        String[] parts = ip.split("\\.");
        if (parts.length != 4) return false;

        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) return false;
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void testConnection() {
        updateConnectionStatus("Test ediliyor...");

        ApiService.getInstance().testConnection(new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateConnectionStatus("Bağlı");
                        showToast("ESP32 bağlantısı başarılı");
                        getCurrentWifiInfo();
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateConnectionStatus("Bağlantı Hatası");
                        showToast("Bağlantı başarısız: " + message);
                    });
                }
            }
        });
    }

    private void scanWifiNetworks() {
        if (progressScan != null) {
            progressScan.setVisibility(View.VISIBLE);
        }

        wifiNetworks.clear();
        wifiAdapter.notifyDataSetChanged();

        ApiService.getInstance().getWifiNetworks(new ApiService.DataCallback() {
            @Override
            public void onSuccess(JSONObject data) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        parseWifiNetworks(data);
                        if (progressScan != null) {
                            progressScan.setVisibility(View.GONE);
                        }
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("WiFi tarama başarısız: " + message);
                        if (progressScan != null) {
                            progressScan.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }

    private void parseWifiNetworks(JSONObject data) {
        try {
            JSONArray networks = data.optJSONArray("networks");
            if (networks != null) {
                wifiNetworks.clear();
                for (int i = 0; i < networks.length(); i++) {
                    JSONObject network = networks.getJSONObject(i);
                    String ssid = network.optString("ssid", "");
                    int rssi = network.optInt("rssi", 0);
                    String security = network.optString("security", "Open");

                    if (!TextUtils.isEmpty(ssid)) {
                        String networkInfo = ssid + " (" + rssi + "dBm, " + security + ")";
                        wifiNetworks.add(networkInfo);
                    }
                }
                wifiAdapter.notifyDataSetChanged();
                showToast("Bulunan ağ sayısı: " + wifiNetworks.size());
            } else {
                showToast("WiFi ağı bulunamadı");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing WiFi networks: " + e.getMessage());
            showToast("WiFi ağları ayrıştırılamadı");
        }
    }

    private void showWifiPasswordDialog(String networkInfo) {
        // Extract SSID from network info
        String ssid = networkInfo.split(" \\(")[0];

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_wifi_password, null);

        EditText etPassword = dialogView.findViewById(R.id.et_wifi_password);
        TextView tvSsid = dialogView.findViewById(R.id.tv_wifi_ssid);

        if (tvSsid != null) {
            tvSsid.setText("SSID: " + ssid);
        }

        builder.setView(dialogView)
                .setTitle("WiFi Şifresi")
                .setPositiveButton("Bağlan", (dialog, which) -> {
                    String password = "";
                    if (etPassword != null) {
                        password = etPassword.getText().toString();
                    }
                    connectToWifi(ssid, password);
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    private void connectToWifi(String ssid, String password) {
        showToast("WiFi'ye bağlanıyor...");

        ApiService.getInstance().connectToWifi(ssid, password, new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("WiFi bağlantısı başlatıldı");
                        // Wait a moment then refresh connection status
                        new android.os.Handler().postDelayed(() -> {
                            testConnection();
                            getCurrentWifiInfo();
                        }, 3000);
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("WiFi bağlantısı başarısız: " + message);
                    });
                }
            }
        });
    }

    private void getCurrentWifiInfo() {
        ApiService.getInstance().getStatus(new ApiService.DataCallback() {
            @Override
            public void onSuccess(JSONObject data) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        String currentSsid = data.optString("currentWifi", "Bilinmiyor");
                        String ipAddress = data.optString("ipAddress", "");

                        if (tvCurrentWifi != null) {
                            String wifiInfo = "Bağlı Ağ: " + currentSsid;
                            if (!TextUtils.isEmpty(ipAddress)) {
                                wifiInfo += "\nIP: " + ipAddress;
                            }
                            tvCurrentWifi.setText(wifiInfo);
                        }
                    });
                }
            }

            @Override
            public void onError(String message) {
                Log.w(TAG, "Failed to get current WiFi info: " + message);
            }
        });
    }

    private void updateConnectionStatus(String status) {
        if (tvConnectionStatus != null) {
            tvConnectionStatus.setText("Durum: " + status);

            int colorRes;
            switch (status) {
                case "Bağlı":
                    colorRes = android.R.color.holo_green_dark;
                    break;
                case "Bağlantı Hatası":
                    colorRes = android.R.color.holo_red_dark;
                    break;
                default:
                    colorRes = android.R.color.holo_orange_light;
                    break;
            }
            tvConnectionStatus.setTextColor(getResources().getColor(colorRes));
        }
    }

    private void refreshData() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).refreshDataNow();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        testConnection();
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}