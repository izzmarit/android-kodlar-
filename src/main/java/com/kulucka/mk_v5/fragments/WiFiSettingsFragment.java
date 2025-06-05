package com.kulucka.mk_v5.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kulucka.mk_v5.R;
import com.kulucka.mk_v5.activities.MainActivity;
import com.kulucka.mk_v5.services.ApiService;
import com.kulucka.mk_v5.utils.NetworkUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class WiFiSettingsFragment extends Fragment {

    private static final String TAG = "WiFiSettings";

    // UI Components - ESP32 WiFi Modu
    private RadioGroup rgWifiMode;
    private RadioButton rbStationMode;
    private RadioButton rbAPMode;
    private TextView tvCurrentMode;
    private TextView tvCurrentIP;

    // UI Components - Station Mode (Ev Ağına Bağlantı)
    private View stationModePanel;
    private ListView lvAvailableNetworks;
    private Button btnScanNetworks;
    private ProgressBar pbScanProgress;
    private EditText etSSID;
    private EditText etPassword;
    private CheckBox cbShowPassword;
    private Button btnConnectToNetwork;
    private TextView tvConnectionStatus;

    // UI Components - AP Mode Ayarları
    private View apModePanel;
    private EditText etAPSSID;
    private EditText etAPPassword;
    private TextView tvAPInfo;
    private Button btnUpdateAPSettings;
    private Button btnResetToDefaults;

    // UI Components - Bağlantı Durumu
    private TextView tvESP32Status;
    private TextView tvSignalStrength;
    private Button btnTestConnection;
    private Button btnForgetNetwork;

    // WiFi yönetimi
    private WifiManager wifiManager;
    private List<ScanResult> scanResults;
    private ArrayAdapter<String> networksAdapter;
    private Handler refreshHandler;
    private boolean isFragmentActive = false;
    private static final long REFRESH_INTERVAL = 10000; // 10 saniye

    // ESP32 varsayılan değerleri
    private static final String DEFAULT_AP_SSID = "KULUCKA_MK_v5";
    private static final String DEFAULT_AP_PASSWORD = "12345678";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wifi_settings, container, false);
        initializeViews(view);
        setupEventListeners();
        setupWiFiManager();
        setupRefreshHandler();
        loadCurrentWiFiStatus();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        isFragmentActive = true;
        startPeriodicRefresh();
    }

    @Override
    public void onPause() {
        super.onPause();
        isFragmentActive = false;
        stopPeriodicRefresh();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopPeriodicRefresh();
        if (refreshHandler != null) {
            refreshHandler.removeCallbacksAndMessages(null);
        }
    }

    private void initializeViews(View view) {
        // WiFi modu seçimi
        rgWifiMode = view.findViewById(R.id.rg_wifi_mode);
        rbStationMode = view.findViewById(R.id.rb_station_mode);
        rbAPMode = view.findViewById(R.id.rb_ap_mode);
        tvCurrentMode = view.findViewById(R.id.tv_current_mode);
        tvCurrentIP = view.findViewById(R.id.tv_current_ip);

        // Station mode paneli
        stationModePanel = view.findViewById(R.id.station_mode_panel);
        lvAvailableNetworks = view.findViewById(R.id.lv_available_networks);
        btnScanNetworks = view.findViewById(R.id.btn_scan_networks);
        pbScanProgress = view.findViewById(R.id.pb_scan_progress);
        etSSID = view.findViewById(R.id.et_ssid);
        etPassword = view.findViewById(R.id.et_password);
        cbShowPassword = view.findViewById(R.id.cb_show_password);
        btnConnectToNetwork = view.findViewById(R.id.btn_connect_to_network);
        tvConnectionStatus = view.findViewById(R.id.tv_connection_status);

        // AP mode paneli
        apModePanel = view.findViewById(R.id.ap_mode_panel);
        etAPSSID = view.findViewById(R.id.et_ap_ssid);
        etAPPassword = view.findViewById(R.id.et_ap_password);
        tvAPInfo = view.findViewById(R.id.tv_ap_info);
        btnUpdateAPSettings = view.findViewById(R.id.btn_update_ap_settings);
        btnResetToDefaults = view.findViewById(R.id.btn_reset_to_defaults);

        // Bağlantı durumu
        tvESP32Status = view.findViewById(R.id.tv_esp32_status);
        tvSignalStrength = view.findViewById(R.id.tv_signal_strength);
        btnTestConnection = view.findViewById(R.id.btn_test_connection);
        btnForgetNetwork = view.findViewById(R.id.btn_forget_network);

        setDefaultValues();
    }

    private void setDefaultValues() {
        if (etAPSSID != null) etAPSSID.setText(DEFAULT_AP_SSID);
        if (etAPPassword != null) etAPPassword.setText(DEFAULT_AP_PASSWORD);
        if (tvCurrentMode != null) tvCurrentMode.setText("Kontrol ediliyor...");
        if (tvCurrentIP != null) tvCurrentIP.setText("---.---.---.---");
        if (tvConnectionStatus != null) tvConnectionStatus.setText("Bağlantı durumu bilinmiyor");
        if (tvESP32Status != null) tvESP32Status.setText("ESP32 durumu kontrol ediliyor...");
    }

    private void setupEventListeners() {
        // WiFi modu değişikliği
        if (rgWifiMode != null) {
            rgWifiMode.setOnCheckedChangeListener((group, checkedId) -> {
                updateModePanel(checkedId);
            });
        }

        // Station mode işlemleri
        if (btnScanNetworks != null) {
            btnScanNetworks.setOnClickListener(v -> scanAvailableNetworks());
        }

        if (btnConnectToNetwork != null) {
            btnConnectToNetwork.setOnClickListener(v -> connectToSelectedNetwork());
        }

        if (lvAvailableNetworks != null) {
            lvAvailableNetworks.setOnItemClickListener((parent, view, position, id) -> {
                if (scanResults != null && position < scanResults.size()) {
                    ScanResult result = scanResults.get(position);
                    etSSID.setText(result.SSID);
                    etPassword.setText(""); // Şifreyi temizle
                    etPassword.requestFocus();
                }
            });
        }

        // Şifre göster/gizle
        if (cbShowPassword != null) {
            cbShowPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (etPassword != null) {
                    if (isChecked) {
                        etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    } else {
                        etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    }
                    etPassword.setSelection(etPassword.getText().length());
                }
            });
        }

        // SSID değişikliği kontrolü
        if (etSSID != null) {
            etSSID.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    validateNetworkCredentials();
                }
            });
        }

        // AP mode işlemleri
        if (btnUpdateAPSettings != null) {
            btnUpdateAPSettings.setOnClickListener(v -> updateAPSettings());
        }

        if (btnResetToDefaults != null) {
            btnResetToDefaults.setOnClickListener(v -> showResetDialog());
        }

        // Genel işlemler
        if (btnTestConnection != null) {
            btnTestConnection.setOnClickListener(v -> testESP32Connection());
        }

        if (btnForgetNetwork != null) {
            btnForgetNetwork.setOnClickListener(v -> showForgetNetworkDialog());
        }
    }

    private void updateModePanel(int checkedId) {
        if (checkedId == R.id.rb_station_mode) {
            if (stationModePanel != null) stationModePanel.setVisibility(View.VISIBLE);
            if (apModePanel != null) apModePanel.setVisibility(View.GONE);
        } else if (checkedId == R.id.rb_ap_mode) {
            if (stationModePanel != null) stationModePanel.setVisibility(View.GONE);
            if (apModePanel != null) apModePanel.setVisibility(View.VISIBLE);
        }
    }

    private void setupWiFiManager() {
        if (getContext() != null) {
            wifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        }

        // Ağ listesi adapter'ını kur
        if (lvAvailableNetworks != null) {
            networksAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1);
            lvAvailableNetworks.setAdapter(networksAdapter);
        }
    }

    private void setupRefreshHandler() {
        refreshHandler = new Handler();
    }

    private void startPeriodicRefresh() {
        if (refreshHandler != null && isFragmentActive) {
            refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
        }
    }

    private void stopPeriodicRefresh() {
        if (refreshHandler != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (isFragmentActive) {
                loadCurrentWiFiStatus();
                refreshHandler.postDelayed(this, REFRESH_INTERVAL);
            }
        }
    };

    private void loadCurrentWiFiStatus() {
        // ESP32'den güncel WiFi durumunu al - getStatus kullan
        ApiService.getInstance().getStatus(new ApiService.DataCallback() {
            @Override
            public void onSuccess(JSONObject data) {
                if (isFragmentActive && getActivity() != null) {
                    getActivity().runOnUiThread(() -> updateUIWithWiFiData(data));
                }
            }

            @Override
            public void onError(String message) {
                if (isFragmentActive && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Log.w(TAG, "WiFi durumu alınamadı: " + message);
                        updateConnectionStatus("Bağlantı hatası: " + message);
                    });
                }
            }
        });
    }

    private void updateUIWithWiFiData(JSONObject data) {
        try {
            // WiFi modu
            String mode = data.optString("mode", "unknown");
            boolean isConnected = data.optBoolean("connected", false);
            String ssid = data.optString("ssid", "");
            String ip = data.optString("ip", "");
            int rssi = data.optInt("rssi", 0);

            // Modu güncelle
            if (tvCurrentMode != null) {
                String modeText = "";
                switch (mode.toLowerCase()) {
                    case "ap":
                        modeText = "Access Point (Hotspot)";
                        if (rbAPMode != null) rbAPMode.setChecked(true);
                        updateModePanel(R.id.rb_ap_mode);
                        break;
                    case "sta":
                    case "station":
                        modeText = "Station (İstemci)";
                        if (rbStationMode != null) rbStationMode.setChecked(true);
                        updateModePanel(R.id.rb_station_mode);
                        break;
                    default:
                        modeText = "Bilinmiyor";
                        break;
                }
                tvCurrentMode.setText(modeText);
            }

            // IP adresi
            if (tvCurrentIP != null) {
                tvCurrentIP.setText(ip.isEmpty() ? "IP atanmamış" : ip);
            }

            // Bağlantı durumu
            String statusText;
            if (mode.equals("ap")) {
                statusText = "AP Modu - Cihazlar bağlanabilir";
            } else if (isConnected && !ssid.isEmpty()) {
                statusText = "Bağlı: " + ssid;
            } else {
                statusText = "Bağlantı yok";
            }
            updateConnectionStatus(statusText);

            // Sinyal gücü
            if (tvSignalStrength != null) {
                if (isConnected && rssi != 0) {
                    String signalText = getSignalStrengthText(rssi);
                    tvSignalStrength.setText("Sinyal: " + signalText + " (" + rssi + " dBm)");
                } else {
                    tvSignalStrength.setText("Sinyal: Bilinmiyor");
                }
            }

            // ESP32 durumu
            if (tvESP32Status != null) {
                tvESP32Status.setText("ESP32 Aktif - " + modeText);
                tvESP32Status.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            }

            // AP ayarlarını güncelle (eğer AP modundaysa)
            if (mode.equals("ap")) {
                updateAPInfo(data);
            }

        } catch (Exception e) {
            Log.e(TAG, "WiFi verileri işleme hatası: " + e.getMessage());
        }
    }

    private String getSignalStrengthText(int rssi) {
        if (rssi > -50) return "Mükemmel";
        else if (rssi > -60) return "İyi";
        else if (rssi > -70) return "Orta";
        else if (rssi > -80) return "Zayıf";
        else return "Çok Zayıf";
    }

    private void updateAPInfo(JSONObject data) {
        try {
            String apSSID = data.optString("apSSID", DEFAULT_AP_SSID);
            String apPassword = data.optString("apPassword", DEFAULT_AP_PASSWORD);
            int connectedClients = data.optInt("connectedClients", 0);

            if (etAPSSID != null) {
                etAPSSID.setText(apSSID);
            }
            if (etAPPassword != null) {
                etAPPassword.setText(apPassword);
            }
            if (tvAPInfo != null) {
                String infoText = String.format("Bağlı cihaz sayısı: %d\nIP Adresi: 192.168.4.1", connectedClients);
                tvAPInfo.setText(infoText);
            }
        } catch (Exception e) {
            Log.e(TAG, "AP bilgileri güncelleme hatası: " + e.getMessage());
        }
    }

    private void scanAvailableNetworks() {
        if (pbScanProgress != null) {
            pbScanProgress.setVisibility(View.VISIBLE);
        }
        if (btnScanNetworks != null) {
            btnScanNetworks.setEnabled(false);
            btnScanNetworks.setText("Taranıyor...");
        }

        // ESP32'den WiFi tarama isteği - doğru metodu kullan
        ApiService.getInstance().getWifiNetworks(new ApiService.DataCallback() {
            @Override
            public void onSuccess(JSONObject data) {
                if (isFragmentActive && getActivity() != null) {
                    getActivity().runOnUiThread(() -> processScanResults(data));
                }
            }

            @Override
            public void onError(String message) {
                if (isFragmentActive && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Ağ tarama hatası: " + message);
                        resetScanButton();
                    });
                }
            }
        });
    }

    private void processScanResults(JSONObject data) {
        try {
            JSONArray networks = data.optJSONArray("networks");
            if (networks != null && networksAdapter != null) {
                networksAdapter.clear();
                scanResults = new ArrayList<>();

                for (int i = 0; i < networks.length(); i++) {
                    JSONObject network = networks.getJSONObject(i);
                    String ssid = network.optString("ssid", "");
                    int rssi = network.optInt("rssi", 0);
                    String security = network.optString("security", "Open");

                    if (!ssid.isEmpty()) {
                        String displayText = String.format("%s (%s) - %s",
                                ssid, getSignalStrengthText(rssi), security);
                        networksAdapter.add(displayText);

                        // ScanResult mock objesi oluştur (gerçek tarama için)
                        // Burada ESP32'den gelen verilerle mock ScanResult oluşturabiliriz
                    }
                }
                networksAdapter.notifyDataSetChanged();

                if (networks.length() == 0) {
                    showToast("Hiç ağ bulunamadı");
                } else {
                    showToast(networks.length() + " ağ bulundu");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Tarama sonuçları işleme hatası: " + e.getMessage());
            showToast("Tarama sonuçları işlenemedi");
        }

        resetScanButton();
    }

    private void resetScanButton() {
        if (pbScanProgress != null) {
            pbScanProgress.setVisibility(View.GONE);
        }
        if (btnScanNetworks != null) {
            btnScanNetworks.setEnabled(true);
            btnScanNetworks.setText("Ağları Tara");
        }
    }

    private void connectToSelectedNetwork() {
        if (!validateNetworkCredentials()) {
            return;
        }

        String ssid = etSSID.getText().toString().trim();
        String password = etPassword.getText().toString();

        try {
            JSONObject params = new JSONObject();
            params.put("ssid", ssid);
            params.put("password", password);

            if (btnConnectToNetwork != null) {
                btnConnectToNetwork.setEnabled(false);
                btnConnectToNetwork.setText("Bağlanıyor...");
            }

            updateConnectionStatus("Ağa bağlanılıyor: " + ssid);

            // ESP32'ye WiFi bağlantı komutu gönder
            ApiService.getInstance().connectToWiFi(params, new ApiService.ParameterCallback() {
                @Override
                public void onSuccess() {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showToast("WiFi ağına bağlantı başlatıldı");
                            updateConnectionStatus("Bağlantı kuruluyor...");

                            // Bağlantı durumunu kontrol etmek için 5 saniye bekle
                            if (refreshHandler != null) {
                                refreshHandler.postDelayed(() -> {
                                    loadCurrentWiFiStatus();
                                    checkNewIPAddress(ssid);
                                }, 5000);
                            }

                            resetConnectButton();
                        });
                    }
                }

                @Override
                public void onError(String message) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showToast("Bağlantı hatası: " + message);
                            updateConnectionStatus("Bağlantı başarısız: " + message);
                            resetConnectButton();
                        });
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "WiFi bağlantı hatası: " + e.getMessage());
            showToast("Parametre hatası: " + e.getMessage());
            resetConnectButton();
        }
    }

    private void checkNewIPAddress(String ssid) {
        // ESP32'nin yeni IP adresini al ve MainActivity'ye bildir
        ApiService.getInstance().getWiFiStatus(new ApiService.DataCallback() {
            @Override
            public void onSuccess(JSONObject data) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        boolean isConnected = data.optBoolean("connected", false);
                        String newIP = data.optString("ip", "");

                        if (isConnected && !newIP.isEmpty() && !newIP.equals("192.168.4.1")) {
                            // Yeni IP adresini kaydet
                            if (getActivity() instanceof MainActivity) {
                                ((MainActivity) getActivity()).updateIpAddress(newIP);
                            }
                            showToast("WiFi bağlantısı başarılı! Yeni IP: " + newIP);
                            updateConnectionStatus("Bağlı: " + ssid + " (" + newIP + ")");
                        } else {
                            updateConnectionStatus("Bağlantı başarısız");
                        }
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateConnectionStatus("IP kontrolü başarısız: " + message);
                    });
                }
            }
        });
    }

    private void resetConnectButton() {
        if (btnConnectToNetwork != null) {
            btnConnectToNetwork.setEnabled(true);
            btnConnectToNetwork.setText("Ağa Bağlan");
        }
    }

    private boolean validateNetworkCredentials() {
        if (etSSID == null || etPassword == null) return false;

        String ssid = etSSID.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (ssid.isEmpty()) {
            etSSID.setError("SSID boş olamaz");
            return false;
        }

        if (ssid.length() > 32) {
            etSSID.setError("SSID 32 karakterden uzun olamaz");
            return false;
        }

        if (password.length() > 0 && password.length() < 8) {
            etPassword.setError("Şifre en az 8 karakter olmalı");
            return false;
        }

        if (password.length() > 63) {
            etPassword.setError("Şifre 63 karakterden uzun olamaz");
            return false;
        }

        return true;
    }

    private void updateAPSettings() {
        if (!validateAPCredentials()) {
            return;
        }

        String apSSID = etAPSSID.getText().toString().trim();
        String apPassword = etAPPassword.getText().toString();

        try {
            JSONObject params = new JSONObject();
            params.put("apSSID", apSSID);
            params.put("apPassword", apPassword);

            if (btnUpdateAPSettings != null) {
                btnUpdateAPSettings.setEnabled(false);
                btnUpdateAPSettings.setText("Güncelleniyor...");
            }

            // ESP32'ye AP ayarları gönder
            ApiService.getInstance().updateAPSettings(params, new ApiService.ParameterCallback() {
                @Override
                public void onSuccess() {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showToast("AP ayarları güncellendi");
                            resetAPButton();
                            loadCurrentWiFiStatus(); // Durumu yenile
                        });
                    }
                }

                @Override
                public void onError(String message) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showToast("AP ayarları güncellenemedi: " + message);
                            resetAPButton();
                        });
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "AP ayarları güncelleme hatası: " + e.getMessage());
            showToast("Parametre hatası: " + e.getMessage());
            resetAPButton();
        }
    }

    private void resetAPButton() {
        if (btnUpdateAPSettings != null) {
            btnUpdateAPSettings.setEnabled(true);
            btnUpdateAPSettings.setText("AP Ayarlarını Güncelle");
        }
    }

    private boolean validateAPCredentials() {
        if (etAPSSID == null || etAPPassword == null) return false;

        String ssid = etAPSSID.getText().toString().trim();
        String password = etAPPassword.getText().toString();

        if (ssid.isEmpty()) {
            etAPSSID.setError("AP SSID boş olamaz");
            return false;
        }

        if (ssid.length() > 32) {
            etAPSSID.setError("AP SSID 32 karakterden uzun olamaz");
            return false;
        }

        if (password.length() < 8) {
            etAPPassword.setError("AP şifresi en az 8 karakter olmalı");
            return false;
        }

        if (password.length() > 63) {
            etAPPassword.setError("AP şifresi 63 karakterden uzun olamaz");
            return false;
        }

        return true;
    }

    private void testESP32Connection() {
        if (btnTestConnection != null) {
            btnTestConnection.setEnabled(false);
            btnTestConnection.setText("Test ediliyor...");
        }

        ApiService.getInstance().testConnection(new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("ESP32 bağlantısı başarılı");
                        if (tvESP32Status != null) {
                            tvESP32Status.setText("ESP32 Bağlantısı: Başarılı");
                            tvESP32Status.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                        }
                        resetTestButton();
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("ESP32 bağlantı hatası: " + message);
                        if (tvESP32Status != null) {
                            tvESP32Status.setText("ESP32 Bağlantısı: Başarısız");
                            tvESP32Status.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        }
                        resetTestButton();
                    });
                }
            }
        });
    }

    private void resetTestButton() {
        if (btnTestConnection != null) {
            btnTestConnection.setEnabled(true);
            btnTestConnection.setText("Bağlantıyı Test Et");
        }
    }

    private void showForgetNetworkDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Ağı Unut")
                .setMessage("ESP32'nin kaydettiği WiFi bilgilerini silmek istediğinizden emin misiniz?\n\n" +
                        "Bu işlem ESP32'yi AP moduna geri döndürecektir.")
                .setPositiveButton("Unut", (dialog, which) -> forgetCurrentNetwork())
                .setNegativeButton("İptal", null)
                .show();
    }

    private void forgetCurrentNetwork() {
        // ESP32'den WiFi bilgilerini sil
        ApiService.getInstance().forgetWiFiNetwork(new ApiService.ParameterCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("WiFi ağı unutuldu. ESP32 AP moduna geçiyor.");
                        updateConnectionStatus("WiFi ağı unutuldu");

                        // 3 saniye sonra yeniden bağlantı kur (AP moduna geçiş için)
                        if (refreshHandler != null) {
                            refreshHandler.postDelayed(() -> {
                                loadCurrentWiFiStatus();
                                // MainActivity'ye AP IP'si bildir
                                if (getActivity() instanceof MainActivity) {
                                    ((MainActivity) getActivity()).updateIpAddress("192.168.4.1");
                                }
                            }, 3000);
                        }
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Ağ unutulamadı: " + message);
                    });
                }
            }
        });
    }

    private void showResetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Varsayılan AP Ayarları")
                .setMessage("AP ayarlarını varsayılan değerlere sıfırlamak istediğinizden emin misiniz?")
                .setPositiveButton("Sıfırla", (dialog, which) -> resetAPToDefaults())
                .setNegativeButton("İptal", null)
                .show();
    }

    private void resetAPToDefaults() {
        if (etAPSSID != null) {
            etAPSSID.setText(DEFAULT_AP_SSID);
        }
        if (etAPPassword != null) {
            etAPPassword.setText(DEFAULT_AP_PASSWORD);
        }
        showToast("Varsayılan AP ayarları yüklendi");
    }

    private void updateConnectionStatus(String status) {
        if (tvConnectionStatus != null) {
            tvConnectionStatus.setText(status);
        }
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}