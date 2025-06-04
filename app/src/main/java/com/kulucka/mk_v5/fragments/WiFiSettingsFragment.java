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
    private Button btnRefreshStatus;
    private ListView lvWifiNetworks;
    private ProgressBar progressScan;
    private TextView tvConnectionStatus;
    private TextView tvCurrentWifi;
    private TextView tvDeviceMode;
    private EditText etWifiSsid;
    private EditText etWifiPassword;
    private Button btnConnectWifi;

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
        btnRefreshStatus = view.findViewById(R.id.btn_refresh_status);
        lvWifiNetworks = view.findViewById(R.id.lv_wifi_networks);
        progressScan = view.findViewById(R.id.progress_scan);
        tvConnectionStatus = view.findViewById(R.id.tv_connection_status);
        tvCurrentWifi = view.findViewById(R.id.tv_current_wifi);
        tvDeviceMode = view.findViewById(R.id.tv_device_mode);
        etWifiSsid = view.findViewById(R.id.et_wifi_ssid);
        etWifiPassword = view.findViewById(R.id.et_wifi_password);
        btnConnectWifi = view.findViewById(R.id.btn_connect_wifi);

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

        if (btnRefreshStatus != null) {
            btnRefreshStatus.setOnClickListener(v -> refreshDeviceStatus());
        }

        if (btnConnectWifi != null) {
            btnConnectWifi.setOnClickListener(v -> connectToWifiManual());
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

            updateConnectionStatus("Kontrol ediliyor...");
            testConnection();
            refreshDeviceStatus();
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
                        refreshDeviceStatus();
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

    private void refreshDeviceStatus() {
        // ESP32'den genişletilmiş durum bilgilerini al (createAppData fonksiyonu)
        ApiService.getInstance().getAppData(new ApiService.DataCallback() {
            @Override
            public void onSuccess(JSONObject data) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateDeviceInfoFromAppData(data);
                    });
                }
            }

            @Override
            public void onError(String message) {
                Log.w(TAG, "Failed to get ESP32 app data: " + message);
            }
        });
    }

    private void updateDeviceInfoFromAppData(JSONObject data) {
        try {
            // ESP32'nin createAppData fonksiyonundan gelen veriler
            String wifiMode = data.optString("wifiMode", "AP");
            String ipAddress = data.optString("ipAddress", "192.168.4.1");
            String ssid = data.optString("ssid", "KULUCKA_MK_v5");
            int signalStrength = data.optInt("signalStrength", 0);
            String wifiStatus = data.optString("wifiStatus", "Bilinmiyor");
            long timestamp = data.optLong("timestamp", 0);
            int freeHeap = data.optInt("freeHeap", 0);
            long uptime = data.optLong("uptime", 0);

            if (tvCurrentWifi != null) {
                String wifiInfo = "Aktif Ağ: " + ssid;
                if (!TextUtils.isEmpty(ipAddress)) {
                    wifiInfo += "\nIP: " + ipAddress;
                }
                if (signalStrength != 0) {
                    wifiInfo += "\nSinyal Gücü: " + signalStrength + " dBm";
                }
                tvCurrentWifi.setText(wifiInfo);
            }

            if (tvDeviceMode != null) {
                String modeText;
                if ("AP".equals(wifiMode)) {
                    modeText = "AP Modu (Hotspot)";
                } else {
                    modeText = "Station Modu (WiFi Bağlı)";
                }

                // Sistem bilgilerini ekle
                if (freeHeap > 0) {
                    modeText += "\nBoş Hafıza: " + (freeHeap / 1024) + " KB";
                }
                if (uptime > 0) {
                    modeText += "\nÇalışma Süresi: " + (uptime / 3600) + " saat";
                }

                updateDeviceMode(modeText);
            }

            updateConnectionStatus(wifiStatus);

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

        // ESP32'nin /api/wifi/networks endpoint'ini kullan
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
            // ESP32'nin _getWiFiNetworksJson fonksiyonundan gelen format
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

        // SSID'yi manuel giriş alanına da doldur
        if (etWifiSsid != null) {
            etWifiSsid.setText(networkInfo.ssid);
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

    private void connectToWifiManual() {
        if (etWifiSsid == null || etWifiPassword == null) {
            showToast("SSID ve şifre alanları bulunamadı");
            return;
        }

        String ssid = etWifiSsid.getText().toString().trim();
        String password = etWifiPassword.getText().toString();

        if (TextUtils.isEmpty(ssid)) {
            showToast("SSID boş olamaz");
            return;
        }

        connectToWifi(ssid, password);
    }

    private void connectToWifi(String ssid, String password) {
        showToast("ESP32'ye WiFi bağlantı komutu gönderiliyor...");

        // ESP32'nin /api/wifi/connect endpoint'ini kullan
        ApiService.getInstance().connectToWifi(ssid, password, new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("WiFi bağlantısı başlatıldı. ESP32 yeniden başlatılıyor...");

                        // ESP32'nin yeniden başlamasını bekle ve durumu kontrol et
                        new android.os.Handler().postDelayed(() -> {
                            updateIpToRouterRange();
                            testConnection();
                            refreshDeviceStatus();
                        }, 15000); // 15 saniye bekle
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

        // ESP32'nin /api/wifi/ap endpoint'ini kullan
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
                            refreshDeviceStatus();
                        }, 5000);
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
        if (etEsp32Ip != null) {
            String currentIp = etEsp32Ip.getText().toString();
            if (currentIp.startsWith("192.168.4.")) {
                // AP modundan çıkıyoruz, muhtemelen router ağına geçtik
                etEsp32Ip.setText("192.168.1.100"); // Genel router IP aralığı
                showToast("IP adresi router ağı için güncellendi. Bağlantıyı test edin.");
            }
        }
    }

    private void updateConnectionStatus(String status) {
        if (tvConnectionStatus != null) {
            tvConnectionStatus.setText("Durum: " + status);

            int colorRes;
            switch (status) {
                case "Bağlı":
                case "Bağlı (KULUCKA_MK_v5)":
                case "Bağlı (Station)":
                    colorRes = android.R.color.holo_green_dark;
                    break;
                case "Bağlantı Hatası":
                case "Bağlantı Başarısız":
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
        refreshDeviceStatus();
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    // WiFi ağ bilgisi için sınıf
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
                    String signalText = network.rssi + " dBm";
                    if (network.rssi > -50) {
                        signalText += " (Mükemmel)";
                    } else if (network.rssi > -70) {
                        signalText += " (İyi)";
                    } else if (network.rssi > -80) {
                        signalText += " (Orta)";
                    } else {
                        signalText += " (Zayıf)";
                    }
                    text2.setText(signalText);
                }
            }

            return convertView;
        }
    }
}