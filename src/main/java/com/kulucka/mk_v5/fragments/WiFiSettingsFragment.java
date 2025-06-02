package com.kulucka.mk_v5.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kulucka.mk_v5.R;
import com.kulucka.mk_v5.models.WiFiNetwork;
import com.kulucka.mk_v5.services.ApiService;
import com.kulucka.mk_v5.utils.Constants;
import com.kulucka.mk_v5.utils.NetworkUtils;
import com.kulucka.mk_v5.utils.SharedPrefsManager;

import java.util.ArrayList;
import java.util.List;

public class WiFiSettingsFragment extends Fragment {

    private TextView tvCurrentMode;
    private Switch swWiFiMode;
    private ListView lvAvailableNetworks;
    private EditText etSelectedSSID;
    private EditText etPassword;
    private Button btnScanNetworks;
    private Button btnConnect;
    private Button btnSwitchToAP;

    private List<WiFiNetwork> availableNetworks;
    private ArrayAdapter<String> networkAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wifi_settings, container, false);

        initializeViews(view);
        setupEventListeners();
        loadCurrentSettings();

        return view;
    }

    private void initializeViews(View view) {
        tvCurrentMode = view.findViewById(R.id.tv_current_mode);
        swWiFiMode = view.findViewById(R.id.sw_wifi_mode);
        lvAvailableNetworks = view.findViewById(R.id.lv_available_networks);
        etSelectedSSID = view.findViewById(R.id.et_selected_ssid);
        etPassword = view.findViewById(R.id.et_password);
        btnScanNetworks = view.findViewById(R.id.btn_scan_networks);
        btnConnect = view.findViewById(R.id.btn_connect);
        btnSwitchToAP = view.findViewById(R.id.btn_switch_to_ap);

        availableNetworks = new ArrayList<>();
        networkAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1);
        lvAvailableNetworks.setAdapter(networkAdapter);
    }

    private void setupEventListeners() {
        swWiFiMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // isChecked true = Station Mode, false = AP Mode
            switchWiFiMode(isChecked);
        });

        btnScanNetworks.setOnClickListener(v -> scanForNetworks());

        btnConnect.setOnClickListener(v -> connectToSelectedNetwork());

        btnSwitchToAP.setOnClickListener(v -> switchToAPMode());

        lvAvailableNetworks.setOnItemClickListener((parent, view, position, id) -> {
            if (position < availableNetworks.size()) {
                WiFiNetwork selectedNetwork = availableNetworks.get(position);
                etSelectedSSID.setText(selectedNetwork.getSsid());
            }
        });
    }

    private void loadCurrentSettings() {
        // ESP32'den mevcut WiFi durumunu al
        getCurrentWiFiStatus();
    }

    private void getCurrentWiFiStatus() {
        try {
            org.json.JSONObject jsonData = new org.json.JSONObject();
            jsonData.put("action", "getWiFiStatus");

            ApiService.getInstance().setParameter("wifi/status", jsonData.toString(), new ApiService.ParameterCallback() {
                @Override
                public void onSuccess() {
                    updateWiFiStatusDisplay();
                }

                @Override
                public void onError(String message) {
                    showToast("WiFi durumu alınamadı: " + message);
                }
            });
        } catch (org.json.JSONException e) {
            showToast("JSON oluşturma hatası: " + e.getMessage());
        }
    }

    private void updateWiFiStatusDisplay() {
        // Mevcut modu güncelle
        boolean isStationMode = NetworkUtils.getInstance(getContext()).isConnectedToDeviceWifi();
        swWiFiMode.setChecked(isStationMode);
        tvCurrentMode.setText(isStationMode ? "Station Modu (Ev WiFi)" : "AP Modu (Hotspot)");
    }

    private void switchWiFiMode(boolean toStationMode) {
        if (toStationMode) {
            // Station moda geçiş için SSID ve şifre gerekli
            String ssid = etSelectedSSID.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (ssid.isEmpty()) {
                showToast("Lütfen bir WiFi ağı seçin");
                swWiFiMode.setChecked(false);
                return;
            }

            connectToWiFiNetwork(ssid, password);
        } else {
            switchToAPMode();
        }
    }

    private void scanForNetworks() {
        try {
            org.json.JSONObject jsonData = new org.json.JSONObject();
            jsonData.put("action", "scanNetworks");

            ApiService.getInstance().setParameter("wifi/scan", jsonData.toString(), new ApiService.ParameterCallback() {
                @Override
                public void onSuccess() {
                    // Ağ listesini al
                    getNetworkList();
                }

                @Override
                public void onError(String message) {
                    showToast("Ağ taraması başarısız: " + message);
                }
            });
        } catch (org.json.JSONException e) {
            showToast("JSON oluşturma hatası: " + e.getMessage());
        }
    }

    private void getNetworkList() {
        // ESP32'den bulunan ağ listesini al
        ApiService.getInstance().setParameter("wifi/networks", "", new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                // Bu callback'te ağ listesi gelecek, GUI güncellemesi yapılacak
                // Gerçek implementasyonda response parsing gerekli
                showToast("Ağ listesi güncellendi");
            }

            @Override
            public void onError(String message) {
                showToast("Ağ listesi alınamadı: " + message);
            }
        });
    }

    private void connectToSelectedNetwork() {
        String ssid = etSelectedSSID.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (ssid.isEmpty()) {
            showToast("Lütfen bir WiFi ağı seçin");
            return;
        }

        connectToWiFiNetwork(ssid, password);
    }

    private void connectToWiFiNetwork(String ssid, String password) {
        try {
            org.json.JSONObject jsonData = new org.json.JSONObject();
            jsonData.put("wifiStationSSID", ssid);
            jsonData.put("wifiStationPassword", password);
            jsonData.put("wifiMode", "1"); // Station mode

            ApiService.getInstance().setParameter("wifi/connect", jsonData.toString(), new ApiService.ParameterCallback() {
                @Override
                public void onSuccess() {
                    // Bağlantı ayarlarını yerel olarak kaydet
                    SharedPrefsManager.getInstance().setApSsid(ssid);
                    SharedPrefsManager.getInstance().setApPassword(password);

                    showToast("WiFi bağlantısı başlatıldı. ESP32 yeniden bağlanacak...");

                    // Birkaç saniye sonra yeni IP ile bağlanmayı dene
                    updateConnectionAfterWiFiChange();
                }

                @Override
                public void onError(String message) {
                    showToast("WiFi bağlantısı başarısız: " + message);
                    swWiFiMode.setChecked(false);
                }
            });
        } catch (org.json.JSONException e) {
            showToast("JSON oluşturma hatası: " + e.getMessage());
        }
    }

    private void switchToAPMode() {
        try {
            org.json.JSONObject jsonData = new org.json.JSONObject();
            jsonData.put("wifiMode", "0"); // AP mode

            ApiService.getInstance().setParameter("wifi/mode", jsonData.toString(), new ApiService.ParameterCallback() {
                @Override
                public void onSuccess() {
                    showToast("AP moduna geçiş başarılı");
                    updateWiFiStatusDisplay();
                }

                @Override
                public void onError(String message) {
                    showToast("AP moduna geçiş başarısız: " + message);
                }
            });
        } catch (org.json.JSONException e) {
            showToast("JSON oluşturma hatası: " + e.getMessage());
        }
    }

    private void updateConnectionAfterWiFiChange() {
        // WiFi değişikliği sonrası API servisini yeniden yapılandır
        getActivity().runOnUiThread(() -> {
            new android.os.Handler().postDelayed(() -> {
                ApiService.getInstance().setupApi();
                ApiService.getInstance().refreshStatus();
            }, 5000); // 5 saniye bekle
        });
    }

    private void showToast(final String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
            });
        }
    }
}