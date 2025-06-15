package com.kulucka.mkv5.activities;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.kulucka.mkv5.R;
import com.kulucka.mkv5.adapters.WifiNetworkAdapter;
import com.kulucka.mkv5.models.WifiNetwork;
import com.kulucka.mkv5.network.ApiService;
import com.kulucka.mkv5.network.NetworkManager;
import com.kulucka.mkv5.utils.Constants;
import com.kulucka.mkv5.utils.SharedPreferencesManager;
import com.kulucka.mkv5.network.NetworkDiscoveryManager;
import com.kulucka.mkv5.MainActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import org.json.JSONObject;
import org.json.JSONException;

public class WifiSettingsActivity extends AppCompatActivity {
    private static final String TAG = "WifiSettingsActivity";

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private Button btnSwitchMode;
    private Button btnRefresh;
    private TextView tvCurrentConnection;

    private WifiNetworkAdapter adapter;
    private NetworkManager networkManager;
    private SharedPreferencesManager prefsManager;
    private List<WifiNetwork> wifiNetworks = new ArrayList<>();
    private ProgressDialog progressDialog;
    private int currentMode = Constants.MODE_AP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_settings);

        setupToolbar();
        initializeViews();
        setupRecyclerView();

        networkManager = NetworkManager.getInstance(this);
        prefsManager = SharedPreferencesManager.getInstance(this);

        // WiFi durumunu kontrol et
        checkWifiStatus();

        // WiFi ağlarını yükle
        loadWifiNetworks();
    }

    private void checkWifiStatus() {
        networkManager.getWifiStatus(new Callback<ApiService.WifiStatusResponse>() {
            @Override
            public void onResponse(Call<ApiService.WifiStatusResponse> call,
                                   Response<ApiService.WifiStatusResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiService.WifiStatusResponse status = response.body();

                    currentMode = status.getMode().equals("AP") ?
                            Constants.MODE_AP : Constants.MODE_STATION;

                    String connectionInfo = status.getMode() + " Modu: " +
                            status.getSsid() + " (" + status.getIp() + ")";
                    tvCurrentConnection.setText(connectionInfo);

                    updateConnectionInfo();
                }
            }

            @Override
            public void onFailure(Call<ApiService.WifiStatusResponse> call, Throwable t) {
                Log.e(TAG, "WiFi durumu alınamadı: " + t.getMessage());
            }
        });
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("WiFi Ayarları");
        }
    }

    private void initializeViews() {
        swipeRefresh = findViewById(R.id.swipeRefresh);
        recyclerView = findViewById(R.id.recyclerView);
        btnSwitchMode = findViewById(R.id.btnSwitchToAP);
        btnRefresh = findViewById(R.id.btnRefresh);
        tvCurrentConnection = findViewById(R.id.tvCurrentConnection);

        swipeRefresh.setOnRefreshListener(this::loadWifiNetworks);
        btnSwitchMode.setOnClickListener(v -> switchMode());
        btnRefresh.setOnClickListener(v -> loadWifiNetworks());
    }

    private void setupRecyclerView() {
        adapter = new WifiNetworkAdapter(wifiNetworks, this::showPasswordDialog);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void updateConnectionInfo() {
        if (currentMode == Constants.MODE_AP) {
            tvCurrentConnection.setText("AP Modu: " + prefsManager.getAPSSID());
            btnSwitchMode.setText("Station Moduna Geç");
            btnSwitchMode.setEnabled(false); // WiFi seçilene kadar devre dışı
        } else {
            tvCurrentConnection.setText("Station Modu: Bağlı");
            btnSwitchMode.setText("AP Moduna Geç");
            btnSwitchMode.setEnabled(true);
        }
    }

    private void loadWifiNetworks() {
        Log.d(TAG, "WiFi ağları yükleniyor...");
        swipeRefresh.setRefreshing(true);


        networkManager.getWifiNetworks(new Callback<ApiService.WifiNetworksResponse>() {
            @Override
            public void onResponse(Call<ApiService.WifiNetworksResponse> call,
                                   Response<ApiService.WifiNetworksResponse> response) {
                swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    wifiNetworks.clear();
                    List<WifiNetwork> networks = response.body().getNetworks();
                    if (networks != null) {
                        wifiNetworks.addAll(networks);
                        Log.d(TAG, "Bulunan ağ sayısı: " + networks.size());
                    }
                    adapter.notifyDataSetChanged();

                    if (wifiNetworks.isEmpty()) {
                        Toast.makeText(WifiSettingsActivity.this,
                                "Hiç WiFi ağı bulunamadı", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "WiFi ağları yüklenemedi. Response code: " + response.code());
                    showError("WiFi ağları yüklenemedi");
                }
            }

            @Override
            public void onFailure(Call<ApiService.WifiNetworksResponse> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Log.e(TAG, "WiFi ağları yükleme hatası: " + t.getMessage());
                showError("Bağlantı hatası: " + t.getMessage());
            }
        });
    }

    private void showPasswordDialog(WifiNetwork network) {
        // Önce kaydedilmiş şifre var mı kontrol et
        String savedPassword = prefsManager.getWifiPassword(network.getSsid());

        if (network.isOpen()) {
            // Açık ağ, direkt bağlan
            connectToWifi(network.getSsid(), "");
            return;
        } else if (!savedPassword.isEmpty()) {
            // Kaydedilmiş şifre var, direkt bağlan
            connectToWifi(network.getSsid(), savedPassword);
            return;
        }

        // Şifre dialogu göster
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_wifi_password, null);
        EditText etPassword = dialogView.findViewById(R.id.etPassword);

        new AlertDialog.Builder(this)
                .setTitle(network.getSsid())
                .setMessage("WiFi şifresini girin")
                .setView(dialogView)
                .setPositiveButton("Bağlan", (dialog, which) -> {
                    String password = etPassword.getText().toString();
                    connectToWifi(network.getSsid(), password);
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    private void connectToWifi(String ssid, String password) {
        showProgressDialog("Bağlanıyor...");

        Map<String, Object> params = new HashMap<>();
        params.put("ssid", ssid);
        params.put("password", password);

        networkManager.connectToWifi(ssid, password, new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    // Bağlantı başarılı, şifreyi kaydet
                    prefsManager.saveWifiNetwork(ssid, password);

                    // Station moduna geçiyoruz
                    prefsManager.saveConnectionMode(Constants.MODE_STATION);
                    currentMode = Constants.MODE_STATION;
                    updateConnectionInfo();

                    showProgressDialog("Bağlantı başarılı! Yeni IP adresi aranıyor...");

                    // Biraz bekle ve yeni IP'yi keşfet
                    new Handler().postDelayed(() -> {
                        hideProgressDialog();

                        // Discovery başlat
                        NetworkDiscoveryManager discoveryManager = new NetworkDiscoveryManager(WifiSettingsActivity.this);
                        discoveryManager.startDiscovery(new NetworkDiscoveryManager.DiscoveryCallback() {
                            @Override
                            public void onDeviceFound(String ipAddress, int port) {
                                runOnUiThread(() -> {
                                    // Yeni IP'yi kaydet
                                    prefsManager.saveDeviceIp(ipAddress);
                                    prefsManager.saveDevicePort(port);

                                    // NetworkManager'ı sıfırla
                                    networkManager.resetConnection();

                                    new AlertDialog.Builder(WifiSettingsActivity.this)
                                            .setTitle("Bağlantı Başarılı")
                                            .setMessage("Cihaz station moduna geçti.\nYeni IP: " + ipAddress)
                                            .setPositiveButton("Tamam", (dialog, which) -> {
                                                setResult(RESULT_OK);
                                                finish();
                                            })
                                            .setCancelable(false)
                                            .show();

                                    discoveryManager.stopDiscovery();
                                });
                            }

                            @Override
                            public void onDiscoveryComplete() {
                                runOnUiThread(() -> {
                                    hideProgressDialog();
                                    showError("Yeni IP adresi bulunamadı. Manuel olarak girmeniz gerekebilir.");
                                });
                            }

                            @Override
                            public void onError(String error) {
                                runOnUiThread(() -> {
                                    hideProgressDialog();
                                    showError("Discovery hatası: " + error);
                                });
                            }
                        });

                        // 30 saniye sonra discovery'yi durdur
                        new Handler().postDelayed(() -> {
                            discoveryManager.stopDiscovery();
                        }, 30000);

                    }, 5000);
                } else {
                    hideProgressDialog();
                    showError("Bağlantı başarısız");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                hideProgressDialog();
                showError("Bağlantı hatası: " + t.getMessage());
            }
        });
    }

    private void switchMode() {
        if (currentMode == Constants.MODE_STATION) {
            // Station'dan AP moduna geç
            switchToAPMode();
        } else {
            // AP'den Station moduna geç - önce WiFi seçilmeli
            Toast.makeText(this, "Önce bir WiFi ağı seçin", Toast.LENGTH_SHORT).show();
        }
    }

    private void switchToAPMode() {
        new AlertDialog.Builder(this)
                .setTitle("AP Moduna Geç")
                .setMessage("AP moduna geçmek istediğinizden emin misiniz?\n\nBağlantı 192.168.4.1 adresine dönecektir.")
                .setPositiveButton("Evet", (dialog, which) -> {
                    showProgressDialog("AP moduna geçiliyor...");

                    networkManager.switchToAPMode(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful()) {
                                // IP ve port bilgilerini güncelle
                                prefsManager.saveDeviceIp(Constants.DEFAULT_AP_IP);
                                prefsManager.saveDevicePort(Constants.DEFAULT_PORT);
                                prefsManager.saveConnectionMode(Constants.MODE_AP);

                                // NetworkManager'ı AP modu için sıfırla
                                networkManager.resetConnection();

                                // Kısa bir süre bekle ve durumu güncelle
                                new Handler().postDelayed(() -> {
                                    hideProgressDialog();

                                    // Mevcut aktivitede kalarak durumu güncelle
                                    currentMode = Constants.MODE_AP;
                                    updateConnectionInfo();
                                    checkWifiStatus();

                                    showSuccess("AP moduna başarıyla geçildi!");

                                    // WiFi ayarlarına yönlendirme mesajı
                                    new AlertDialog.Builder(WifiSettingsActivity.this)
                                            .setTitle("AP Modu Aktif")
                                            .setMessage("Lütfen telefon WiFi ayarlarından KuluckaMK ağına bağlanın.")
                                            .setPositiveButton("Tamam", (d, w) -> {
                                                setResult(RESULT_OK);
                                                finish();
                                            })
                                            .setCancelable(false)
                                            .show();
                                }, 2000);
                            } else {
                                hideProgressDialog();
                                showError("AP moduna geçilemedi");
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            hideProgressDialog();
                            showError("Bağlantı hatası: " + t.getMessage());
                        }
                    });
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideProgressDialog();
    }
}