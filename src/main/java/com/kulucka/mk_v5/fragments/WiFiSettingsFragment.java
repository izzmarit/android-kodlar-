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
    private Button btnSwitchToAP;
    private ListView lvWifiNetworks;
    private ProgressBar progressScan;
    private TextView tvConnectionStatus;
    private TextView tvCurrentWifi;
    private TextView tvDeviceMode;

    // Data
    private List<WiFiNetworkInfo> wifiNetworks;
    private WiFiNetworkAdapter wifiAdapter;

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
        btnSwitchToAP = view.findViewById(R.id.btn_switch_to_ap);
        lvWifiNetworks = view.findViewById(R.id.lv_wifi_networks);
        progressScan = view.findViewById(R.id.progress_scan);
        tvConnectionStatus = view.findViewById(R.id.tv_connection_status);
        tvCurrentWifi = view.findViewById(R.id.tv_current_wifi);
        tvDeviceMode = view.findViewById(R.id.tv_device_mode);

        // Setup WiFi networks list
        wifiNetworks = new ArrayList<>();
        wifiAdapter = new WiFiNetworkAdapter(requireContext(), wifiNetworks);
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

        if (btnSwitchToAP != null) {
            btnSwitchToAP.setOnClickListener(v -> switchToAPMode());
        }

        if (lvWifiNetworks != null) {
            lvWifiNetworks.setOnItemClickListener((parent, view, position, id) -> {
                if (position < wifiNetworks.size()) {
                    WiFiNetworkInfo selectedNetwork = wifiNetworks.get(position);
                    showWifiPasswordDialog(selectedNetwork);
                }
            });
        }
    }

    private void loadSavedSettings() {
        if (getContext() != null) {
            SharedPreferences prefs = getContext().getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            String savedIp = prefs.getString(KEY_ESP32_IP, "192.168.4.1");

            if (etEsp32Ip != null) {
                etEsp32Ip.setText(savedIp);
            }

            updateConnectionStatus("Bilinmiyor");
            testConnection(); // Auto-test on load

            // ESP32'den durum bilgilerini al
            getCurrentDeviceStatus();
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
        updateDeviceMode("Kontrol ediliyor...");

        ApiService.getInstance().testConnection(new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateConnectionStatus("Bağlı");
                        showToast("ESP32 bağlantısı başarılı");
                        getCurrentDeviceStatus();
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateConnectionStatus("Bağlantı Hatası");
                        updateDeviceMode("Bağlantı Yok");
                        showToast("Bağlantı başarısız: " + message);
                    });
                }
            }
        });
    }

    private void getCurrentDeviceStatus() {
        ApiService.getInstance().getStatus(new ApiService.DataCallback() {
            @Override
            public void onSuccess(JSONObject data) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // ESP32'den gelen durum verilerine göre WiFi bilgilerini güncelle
                        updateDeviceInfoFromStatus(data);
                    });
                }
            }

            @Override
            public void onError(String message) {
                Log.w(TAG, "Failed to get ESP32 device status: " + message);
            }
        });
    }

    private void updateDeviceInfoFromStatus(JSONObject data) {
        try {
            // ESP32'den gelen verilerden WiFi durumu çıkarılabilir
            // ESP32 kodunda currentWifi, ipAddress gibi bilgiler varsa
            String currentSsid = data.optString("currentWifi", "ESP32 AP");
            String ipAddress = data.optString("ipAddress", "");
            boolean isAPMode = data.optBoolean("isAPMode", true);

            if (tvCurrentWifi != null) {
                String wifiInfo = "Aktif Ağ: " + currentSsid;
                if (!TextUtils.isEmpty(ipAddress)) {
                    wifiInfo += "\nIP: " + ipAddress;
                }
                tvCurrentWifi.setText(wifiInfo);
            }

            if (tvDeviceMode != null) {
                String mode = isAPMode ? "AP Modu (Hotspot)" : "Station Modu (WiFi Bağlı)";
                updateDeviceMode(mode);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error parsing device status: " + e.getMessage());
        }
    }

    private void scanWifiNetworks() {
        if (progressScan != null) {
            progressScan.setVisibility(View.VISIBLE);
        }

        wifiNetworks.clear();
        wifiAdapter.notifyDataSetChanged();

        showToast("WiFi ağları taranıyor...");

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
                    String encryption = network.optString("encryption", "open");

                    if (!TextUtils.isEmpty(ssid)) {
                        WiFiNetworkInfo networkInfo = new WiFiNetworkInfo();
                        networkInfo.ssid = ssid;
                        networkInfo.rssi = rssi;
                        networkInfo.isSecured = !"open".equals(encryption);
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

    private void showWifiPasswordDialog(WiFiNetworkInfo networkInfo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_wifi_password, null);

        EditText etPassword = dialogView.findViewById(R.id.et_wifi_password);
        TextView tvSsid = dialogView.findViewById(R.id.tv_wifi_ssid);

        if (tvSsid != null) {
            tvSsid.setText("SSID: " + networkInfo.ssid);
        }

        builder.setView(dialogView)
                .setTitle("WiFi Şifresi")
                .setPositiveButton("Bağlan", (dialog, which) -> {
                    String password = "";
                    if (etPassword != null) {
                        password = etPassword.getText().toString();
                    }
                    connectToWifi(networkInfo.ssid, password);
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    private void connectToWifi(String ssid, String password) {
        showToast("ESP32'yi WiFi'ye bağlanmaya çalışıyor...");

        ApiService.getInstance().connectToWifi(ssid, password, new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("WiFi bağlantısı başlatıldı. ESP32 yeniden başlatılıyor...");

                        // ESP32'nin yeniden başlamasını bekle
                        new android.os.Handler().postDelayed(() -> {
                            // Yeni IP adresini bulmaya çalış (genellikle router DHCP'den alınır)
                            updateIpToRouterRange();
                            testConnection();
                            getCurrentDeviceStatus();
                        }, 10000); // 10 saniye bekle
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

    private void switchToAPMode() {
        showToast("ESP32 AP moduna geçiriliyor...");

        ApiService.getInstance().switchToAPMode(new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("AP moduna geçiş başarılı");

                        // AP moduna geçtiğinde IP'yi eski haline getir
                        if (etEsp32Ip != null) {
                            etEsp32Ip.setText("192.168.4.1");
                            saveIpAddress();
                        }

                        // Durumu güncelle
                        new android.os.Handler().postDelayed(() -> {
                            testConnection();
                            getCurrentDeviceStatus();
                        }, 3000);
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("AP moduna geçiş başarısız: " + message);
                    });
                }
            }
        });
    }

    private void updateIpToRouterRange() {
        // ESP32 router'a bağlandığında genellikle 192.168.1.x aralığında IP alır
        // Burada basit bir tahmin yapıyoruz, gelişmiş versiyonda network tarama yapılabilir
        if (etEsp32Ip != null) {
            String currentIp = etEsp32Ip.getText().toString();
            if (currentIp.startsWith("192.168.4.")) {
                // AP modundan çıkıyoruz, muhtemelen router ağına geçtik
                etEsp32Ip.setText("192.168.1.100"); // Genel router IP aralığı
                // Not: Gerçek implementasyonda network scanning yapılmalı
            }
        }
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

    private void updateDeviceMode(String mode) {
        if (tvDeviceMode != null) {
            tvDeviceMode.setText("Mod: " + mode);
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
        getCurrentDeviceStatus();
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    // WiFi ağ bilgisi için basit sınıf
    private static class WiFiNetworkInfo {
        String ssid;
        int rssi;
        boolean isSecured;
    }

    // WiFi ağları için adapter
    private static class WiFiNetworkAdapter extends ArrayAdapter<WiFiNetworkInfo> {
        public WiFiNetworkAdapter(android.content.Context context, List<WiFiNetworkInfo> networks) {
            super(context, android.R.layout.simple_list_item_2, networks);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            }

            WiFiNetworkInfo network = getItem(position);
            if (network != null) {
                TextView text1 = convertView.findViewById(android.R.id.text1);
                TextView text2 = convertView.findViewById(android.R.id.text2);

                if (text1 != null) {
                    String title = network.ssid + (network.isSecured ? " 🔒" : "");
                    text1.setText(title);
                }

                if (text2 != null) {
                    text2.setText(network.rssi + " dBm");
                }
            }

            return convertView;
        }
    }
}