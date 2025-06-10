package com.kulucka.mk.activities;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

import com.kulucka.mk.R;
import com.kulucka.mk.adapters.WiFiNetworkAdapter;
import com.kulucka.mk.models.WiFiNetwork;
import com.kulucka.mk.models.WiFiNetworksResponse;
import com.kulucka.mk.services.ApiService;
import com.kulucka.mk.utils.Constants;
import com.kulucka.mk.utils.NetworkUtils;
import com.kulucka.mk.utils.SharedPreferencesManager;
import com.kulucka.mk.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * WiFi ayarları aktivitesi
 * WiFi ağlarını tarama, bağlanma ve AP/Station mod geçişi
 */
public class WiFiSettingsActivity extends AppCompatActivity implements WiFiNetworkAdapter.OnNetworkClickListener {

    private static final String TAG = "WiFiSettingsActivity";

    // UI Components
    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerViewNetworks;
    private MaterialTextView tvConnectionStatus;
    private MaterialTextView tvCurrentNetwork;
    private MaterialTextView tvIPAddress;
    private MaterialButton btnScanNetworks;
    private MaterialButton btnSwitchToAP;
    private MaterialButton btnSwitchToStation;

    // Adapters and Data
    private WiFiNetworkAdapter networkAdapter;
    private List<WiFiNetwork> networkList;

    // Services and Managers
    private ApiService apiService;
    private SharedPreferencesManager prefsManager;

    // State
    private boolean isScanning = false;
    private boolean isConnecting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_settings);

        Log.i(TAG, "WiFiSettingsActivity oluşturuluyor");

        // Initialize services
        apiService = ApiService.getInstance(this);
        prefsManager = SharedPreferencesManager.getInstance(this);

        // Initialize data
        networkList = new ArrayList<>();

        // Setup UI
        initializeUI();
        setupToolbar();
        setupRecyclerView();
        setupClickListeners();

        // Update initial status
        updateConnectionStatus();

        // Start initial scan
        scanNetworks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerBroadcastReceivers();
        updateConnectionStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterBroadcastReceivers();
    }

    /**
     * UI bileşenlerini başlatır
     */
    private void initializeUI() {
        toolbar = findViewById(R.id.toolbar);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        recyclerViewNetworks = findViewById(R.id.recyclerViewNetworks);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        tvCurrentNetwork = findViewById(R.id.tvCurrentNetwork);
        tvIPAddress = findViewById(R.id.tvIPAddress);
        btnScanNetworks = findViewById(R.id.btnScanNetworks);
        btnSwitchToAP = findViewById(R.id.btnSwitchToAP);
        btnSwitchToStation = findViewById(R.id.btnSwitchToStation);
    }

    /**
     * Toolbar'ı ayarlar
     */
    private void setupToolbar() {
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.wifi_settings_title);
        }
    }

    /**
     * RecyclerView'ı ayarlar
     */
    private void setupRecyclerView() {
        networkAdapter = new WiFiNetworkAdapter(networkList, this);
        recyclerViewNetworks.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewNetworks.setAdapter(networkAdapter);
    }

    /**
     * Click listener'ları ayarlar
     */
    private void setupClickListeners() {
        swipeRefreshLayout.setOnRefreshListener(this::scanNetworks);
        btnScanNetworks.setOnClickListener(v -> scanNetworks());
        btnSwitchToAP.setOnClickListener(v -> switchToAPMode());
        btnSwitchToStation.setOnClickListener(v -> showStationModeDialog());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * WiFi ağlarını tarar
     */
    private void scanNetworks() {
        if (isScanning) {
            Log.d(TAG, "Tarama zaten devam ediyor");
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(this)) {
            Utils.showToast(this, getString(R.string.error_network_unavailable));
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        isScanning = true;
        swipeRefreshLayout.setRefreshing(true);
        btnScanNetworks.setEnabled(false);
        btnScanNetworks.setText(R.string.scanning_networks);

        Log.d(TAG, "WiFi ağları taranıyor...");

        apiService.getWiFiNetworks(new ApiService.ApiCallback<WiFiNetworksResponse>() {
            @Override
            public void onSuccess(WiFiNetworksResponse response) {
                runOnUiThread(() -> handleScanSuccess(response));
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> handleScanError(error));
            }
        });
    }

    /**
     * Tarama başarısını işler
     */
    private void handleScanSuccess(WiFiNetworksResponse response) {
        isScanning = false;
        swipeRefreshLayout.setRefreshing(false);
        btnScanNetworks.setEnabled(true);
        btnScanNetworks.setText(R.string.scan_networks);

        networkList.clear();

        if (response != null && response.hasNetworks()) {
            for (WiFiNetwork network : response.getNetworks()) {
                if (!Utils.isEmpty(network.getSsid())) {
                    networkList.add(network);
                }
            }

            Log.d(TAG, "WiFi taraması tamamlandı - " + networkList.size() + " ağ bulundu");
            Utils.showToast(this, networkList.size() + " ağ bulundu");
        } else {
            Log.w(TAG, "WiFi taramasında ağ bulunamadı");
            Utils.showToast(this, getString(R.string.no_networks_found));
        }

        networkAdapter.notifyDataSetChanged();
    }

    /**
     * Tarama hatasını işler
     */
    private void handleScanError(String error) {
        isScanning = false;
        swipeRefreshLayout.setRefreshing(false);
        btnScanNetworks.setEnabled(true);
        btnScanNetworks.setText(R.string.scan_networks);

        Log.e(TAG, "WiFi tarama hatası: " + error);
        Utils.showToast(this, "Tarama hatası: " + error);
    }

    @Override
    public void onNetworkClick(WiFiNetwork network) {
        if (isConnecting) {
            Utils.showToast(this, "Bağlantı işlemi devam ediyor...");
            return;
        }

        Log.d(TAG, "Ağ seçildi: " + network.getSsid());

        if (network.isOpen()) {
            // Açık ağ - direkt bağlan
            connectToNetwork(network.getSsid(), "");
        } else {
            // Şifreli ağ - şifre iste
            showPasswordDialog(network);
        }
    }

    /**
     * Şifre girişi dialog'u gösterir
     */
    private void showPasswordDialog(WiFiNetwork network) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("WiFi Şifresi");
        builder.setMessage("\"" + network.getSsid() + "\" ağına bağlanmak için şifre girin:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("WiFi şifresi");

        LinearLayout container = new LinearLayout(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(50, 0, 50, 0);
        input.setLayoutParams(params);
        container.addView(input);

        builder.setView(container);

        builder.setPositiveButton(R.string.connect, (dialog, which) -> {
            String password = input.getText().toString().trim();
            connectToNetwork(network.getSsid(), password);
        });

        builder.setNegativeButton(R.string.cancel, null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Klavyeyi göster
        input.requestFocus();
        Utils.showKeyboard(this, input);
    }

    /**
     * Ağa bağlanma işlemini başlatır
     */
    private void connectToNetwork(String ssid, String password) {
        if (Utils.isEmpty(ssid)) {
            Utils.showToast(this, "Geçersiz ağ adı");
            return;
        }

        isConnecting = true;
        updateUIForConnecting(ssid);

        Log.i(TAG, "Ağa bağlanılıyor: " + ssid);

        apiService.connectToWiFi(ssid, password, new ApiService.ApiCallback<ApiResponse>() {
            @Override
            public void onSuccess(ApiResponse response) {
                runOnUiThread(() -> handleConnectionSuccess(ssid, response));
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> handleConnectionError(ssid, error));
            }
        });
    }

    /**
     * Bağlantı başarısını işler
     */
    private void handleConnectionSuccess(String ssid, ApiResponse response) {
        isConnecting = false;

        Log.i(TAG, "WiFi bağlantısı başarılı: " + ssid);
        Utils.showToast(this, "Bağlantı başarılı: " + ssid);

        // Ayarları kaydet
        prefsManager.saveLastConnectedWiFi(ssid, null);

        // Durum güncelle
        Utils.delay(2000, this::updateConnectionStatus);
    }

    /**
     * Bağlantı hatasını işler
     */
    private void handleConnectionError(String ssid, String error) {
        isConnecting = false;
        updateConnectionStatus();

        Log.e(TAG, "WiFi bağlantı hatası: " + error);
        Utils.showToast(this, "Bağlantı hatası: " + error);
    }

    /**
     * AP moduna geçiş
     */
    private void switchToAPMode() {
        if (isConnecting) {
            Utils.showToast(this, "İşlem devam ediyor...");
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("AP Moduna Geç")
                .setMessage("ESP32 cihazı AP moduna geçecek. Devam edilsin mi?")
                .setPositiveButton(R.string.yes, (dialog, which) -> performAPModeSwitch())
                .setNegativeButton(R.string.no, null)
                .show();
    }

    /**
     * AP modu geçişini gerçekleştirir
     */
    private void performAPModeSwitch() {
        isConnecting = true;
        btnSwitchToAP.setEnabled(false);
        btnSwitchToAP.setText("AP Moduna Geçiliyor...");

        apiService.switchToAPMode(new ApiService.ApiCallback<ApiResponse>() {
            @Override
            public void onSuccess(ApiResponse response) {
                runOnUiThread(() -> {
                    isConnecting = false;
                    btnSwitchToAP.setEnabled(true);
                    btnSwitchToAP.setText("AP Moduna Geç");

                    Utils.showToast(WiFiSettingsActivity.this, "AP moduna geçildi");

                    // IP'yi güncelle
                    prefsManager.setESP32IP(Constants.ESP32_AP_IP);
                    apiService.updateIpAddress(Constants.ESP32_AP_IP);

                    Utils.delay(3000, this::updateConnectionStatus);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    isConnecting = false;
                    btnSwitchToAP.setEnabled(true);
                    btnSwitchToAP.setText("AP Moduna Geç");

                    Utils.showToast(WiFiSettingsActivity.this, "AP modu geçişi başarısız: " + error);
                });
            }
        });
    }

    /**
     * Station modu dialog'u gösterir
     */
    private void showStationModeDialog() {
        // Daha sonra implementasyon yapılacak
        Utils.showToast(this, "Station modu geçişi için bir ağ seçin");
    }

    /**
     * Bağlantı durumunu günceller
     */
    private void updateConnectionStatus() {
        String currentIP = apiService.getCurrentIpAddress();

        tvIPAddress.setText("IP: " + (currentIP != null ? currentIP : "Bilinmiyor"));

        if (NetworkUtils.isNetworkAvailable(this)) {
            if (NetworkUtils.isESP32InAPMode(this)) {
                tvConnectionStatus.setText("ESP32 AP Modunda");
                tvConnectionStatus.setTextColor(Utils.getStatusColor(this, true));
                tvCurrentNetwork.setText("Ağ: " + Constants.ESP32_AP_SSID);

                btnSwitchToAP.setEnabled(false);
                btnSwitchToStation.setEnabled(true);
            } else {
                String currentSSID = NetworkUtils.getCurrentWiFiSSID(this);
                if (!Utils.isEmpty(currentSSID)) {
                    tvConnectionStatus.setText("Station Modunda");
                    tvConnectionStatus.setTextColor(Utils.getStatusColor(this, true));
                    tvCurrentNetwork.setText("Ağ: " + currentSSID);

                    btnSwitchToAP.setEnabled(true);
                    btnSwitchToStation.setEnabled(false);
                } else {
                    tvConnectionStatus.setText("Bağlantısız");
                    tvConnectionStatus.setTextColor(Utils.getStatusColor(this, false));
                    tvCurrentNetwork.setText("Ağ: Yok");

                    btnSwitchToAP.setEnabled(true);
                    btnSwitchToStation.setEnabled(true);
                }
            }
        } else {
            tvConnectionStatus.setText("İnternet Yok");
            tvConnectionStatus.setTextColor(Utils.getStatusColor(this, false));
            tvCurrentNetwork.setText("Ağ: Yok");

            btnSwitchToAP.setEnabled(false);
            btnSwitchToStation.setEnabled(false);
        }

        // Buton durumlarını güncelle
        btnScanNetworks.setEnabled(!isConnecting && !isScanning);
    }

    /**
     * Bağlanma sırasında UI'yi günceller
     */
    private void updateUIForConnecting(String ssid) {
        tvConnectionStatus.setText("Bağlanıyor...");
        tvConnectionStatus.setTextColor(Utils.getStatusColor(this, false));
        tvCurrentNetwork.setText("Ağ: " + ssid);

        btnScanNetworks.setEnabled(false);
        btnSwitchToAP.setEnabled(false);
        btnSwitchToStation.setEnabled(false);
    }

    /**
     * Broadcast receiver'ları kaydeder
     */
    private void registerBroadcastReceivers() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_CONNECTION_CHANGED);
        filter.addAction(Constants.ACTION_WIFI_CHANGED);

        lbm.registerReceiver(broadcastReceiver, filter);
    }

    /**
     * Broadcast receiver'ları kaldırır
     */
    private void unregisterBroadcastReceivers() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    /**
     * Broadcast receiver
     */
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            switch (action) {
                case Constants.ACTION_CONNECTION_CHANGED:
                    boolean connected = intent.getBooleanExtra(Constants.EXTRA_CONNECTION_STATUS, false);
                    String ipAddress = intent.getStringExtra(Constants.EXTRA_IP_ADDRESS);

                    if (connected && ipAddress != null) {
                        apiService.updateIpAddress(ipAddress);
                    }

                    updateConnectionStatus();
                    break;

                case Constants.ACTION_WIFI_CHANGED:
                    updateConnectionStatus();
                    break;
            }
        }
    };
}