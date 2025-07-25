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
import com.kulucka.mkv5.models.DeviceStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.InetAddress;

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
    private Button btnResetWifi;
    private TextView tvCurrentConnection;

    private WifiNetworkAdapter adapter;
    private NetworkManager networkManager;
    private SharedPreferencesManager prefsManager;
    private List<WifiNetwork> wifiNetworks = new ArrayList<>();
    private ProgressDialog progressDialog;
    private int currentMode = Constants.MODE_AP;
    private boolean isRefreshing = false;

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
                tvCurrentConnection.setText("Bağlantı durumu bilinmiyor");
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
        btnResetWifi = findViewById(R.id.btnResetWifi);
        tvCurrentConnection = findViewById(R.id.tvCurrentConnection);

        swipeRefresh.setOnRefreshListener(this::loadWifiNetworks);
        btnSwitchMode.setOnClickListener(v -> switchMode());
        btnRefresh.setOnClickListener(v -> {
            if (!isRefreshing) {
                loadWifiNetworks();
            }
        });

        // WiFi Sıfırlama butonu
        btnResetWifi.setOnClickListener(v -> showResetWifiDialog());
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
        if (isRefreshing) {
            Log.d(TAG, "WiFi taraması zaten devam ediyor");
            return;
        }

        Log.d(TAG, "WiFi ağları yükleniyor...");
        isRefreshing = true;
        swipeRefresh.setRefreshing(true);
        btnRefresh.setEnabled(false);

        networkManager.getWifiNetworks(new Callback<ApiService.WifiNetworksResponse>() {
            @Override
            public void onResponse(Call<ApiService.WifiNetworksResponse> call,
                                   Response<ApiService.WifiNetworksResponse> response) {
                swipeRefresh.setRefreshing(false);
                isRefreshing = false;
                btnRefresh.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    wifiNetworks.clear();
                    List<WifiNetwork> networks = response.body().getNetworks();
                    if (networks != null) {
                        wifiNetworks.addAll(networks);
                        Log.d(TAG, "Bulunan ağ sayısı: " + networks.size());

                        // Station moduna geçiş butonunu etkinleştir
                        if (currentMode == Constants.MODE_AP && !wifiNetworks.isEmpty()) {
                            btnSwitchMode.setEnabled(true);
                        }
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
                isRefreshing = false;
                btnRefresh.setEnabled(true);
                Log.e(TAG, "WiFi ağları yükleme hatası: " + t.getMessage());
                showError("Bağlantı hatası: " + t.getMessage());
            }
        });
    }

    private void showResetWifiDialog() {
        new AlertDialog.Builder(this)
                .setTitle("WiFi Ayarlarını Sıfırla")
                .setMessage("WiFi ayarları sıfırlanacak ve cihaz AP moduna dönecektir.\n\n" +
                        "Bu işlem:\n" +
                        "• Kayıtlı WiFi şifrelerini silecek\n" +
                        "• Cihazı AP moduna döndürecek\n" +
                        "• Varsayılan IP adresini kullanacak (192.168.4.1)\n\n" +
                        "Devam etmek istiyor musunuz?")
                .setPositiveButton("Evet, Sıfırla", (dialog, which) -> resetWifiSettings())
                .setNegativeButton("İptal", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void resetWifiSettings() {
        showProgressDialog("WiFi ayarları sıfırlanıyor...");

        // Önce AP moduna geç
        Map<String, Object> params = new HashMap<>();
        params.put("mode", "ap");

        networkManager.changeWifiMode(params, new Callback<ApiService.WifiModeChangeResponse>() {
            @Override
            public void onResponse(Call<ApiService.WifiModeChangeResponse> call,
                                   Response<ApiService.WifiModeChangeResponse> response) {
                // Local ayarları sıfırla
                prefsManager.saveDeviceIp(Constants.DEFAULT_AP_IP);
                prefsManager.saveDevicePort(Constants.DEFAULT_PORT);
                prefsManager.saveConnectionMode(Constants.MODE_AP);
                prefsManager.saveAPCredentials(Constants.DEFAULT_AP_SSID, Constants.DEFAULT_AP_PASSWORD);

                // Kayıtlı WiFi şifrelerini temizle
                clearSavedWifiPasswords();

                // NetworkManager'ı sıfırla
                networkManager.resetConnection();

                hideProgressDialog();

                new AlertDialog.Builder(WifiSettingsActivity.this)
                        .setTitle("WiFi Ayarları Sıfırlandı")
                        .setMessage("WiFi ayarları başarıyla sıfırlandı.\n\n" +
                                "Cihaz şimdi AP modunda:\n" +
                                "SSID: " + Constants.DEFAULT_AP_SSID + "\n" +
                                "Şifre: " + Constants.DEFAULT_AP_PASSWORD + "\n" +
                                "IP: " + Constants.DEFAULT_AP_IP)
                        .setPositiveButton("Tamam", (d, w) -> {
                            setResult(RESULT_OK);
                            finish();
                        })
                        .setCancelable(false)
                        .show();
            }

            @Override
            public void onFailure(Call<ApiService.WifiModeChangeResponse> call, Throwable t) {
                hideProgressDialog();
                showError("WiFi sıfırlama başarısız: " + t.getMessage());
            }
        });
    }

    private void clearSavedWifiPasswords() {
        // SharedPreferences'tan tüm WiFi şifrelerini temizle
        for (WifiNetwork network : wifiNetworks) {
            prefsManager.removeWifiNetwork(network.getSsid());
        }

        // Son bağlanılan ağ bilgilerini de temizle
        prefsManager.saveWifiNetwork("", "");
    }

    private void showPasswordDialog(WifiNetwork network) {
        // Önce kaydedilmiş şifre var mı kontrol et
        String savedPassword = prefsManager.getWifiPassword(network.getSsid());

        if (network.isOpen()) {
            // Açık ağ, direkt bağlan
            connectToWifi(network.getSsid(), "");
            return;
        } else if (!savedPassword.isEmpty()) {
            // Kaydedilmiş şifre var, kullanıcıya sor
            new AlertDialog.Builder(this)
                    .setTitle(network.getSsid())
                    .setMessage("Bu ağ için kayıtlı şifre bulundu. Kullanmak ister misiniz?")
                    .setPositiveButton("Evet", (dialog, which) -> {
                        connectToWifi(network.getSsid(), savedPassword);
                    })
                    .setNegativeButton("Yeni Şifre Gir", (dialog, which) -> {
                        showNewPasswordDialog(network);
                    })
                    .setNeutralButton("İptal", null)
                    .show();
            return;
        }

        // Şifre dialogu göster
        showNewPasswordDialog(network);
    }

    private void showNewPasswordDialog(WifiNetwork network) {
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
        showProgressDialog("WiFi ağına bağlanıyor...");

        // Şifreyi kaydet (sadece boş değilse)
        if (!password.isEmpty()) {
            prefsManager.saveWifiNetwork(ssid, password);
            Log.d(TAG, "WiFi credential'ları kaydedildi: " + ssid);
        }

        networkManager.connectToWifi(ssid, password, new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);

                        if ("success".equals(jsonResponse.optString("status"))) {
                            handleSuccessfulConnection(ssid, jsonResponse);
                        } else {
                            hideProgressDialog();
                            String errorMessage = jsonResponse.optString("message", "Bilinmeyen hata");
                            showError("Bağlantı başarısız: " + errorMessage);

                            // Başarısız bağlantı durumunda kaydedilmiş şifreyi sil
                            if (!password.isEmpty()) {
                                prefsManager.removeWifiNetwork(ssid);
                                Log.d(TAG, "Başarısız bağlantı nedeniyle şifre silindi: " + ssid);
                            }
                        }
                    } catch (Exception e) {
                        hideProgressDialog();
                        showError("Yanıt işlenemedi: " + e.getMessage());
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                    }
                } else {
                    hideProgressDialog();
                    showError("Bağlantı başarısız - HTTP: " + response.code());
                    Log.e(TAG, "HTTP error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                hideProgressDialog();
                showError("Bağlantı hatası: " + t.getMessage());
                Log.e(TAG, "Network error: " + t.getMessage());
            }
        });
    }

    private void handleSuccessfulConnection(String ssid, JSONObject response) {
        updateProgressDialog("Bağlantı başarılı! Sistem durumu kontrol ediliyor...");

        // Station moduna geçiyoruz
        prefsManager.saveConnectionMode(Constants.MODE_STATION);
        currentMode = Constants.MODE_STATION;

        // Sistem durumunu kaydetmek için mevcut API'yi kullan
        networkManager.saveSystem(new Callback<ApiService.SystemSaveResponse>() {
            @Override
            public void onResponse(Call<ApiService.SystemSaveResponse> call,
                                   Response<ApiService.SystemSaveResponse> response) {
                // ESP32'nin mod değişimini tamamlaması için bekle
                new Handler().postDelayed(() -> {
                    performSystemVerification();
                }, 3000);
            }

            @Override
            public void onFailure(Call<ApiService.SystemSaveResponse> call, Throwable t) {
                // Kaydetme başarısız olsa bile devam et
                new Handler().postDelayed(() -> {
                    performSystemVerification();
                }, 3000);
            }
        });
    }

    private void performSystemVerification() {
        updateProgressDialog("Sistem doğrulaması yapılıyor...");

        final int[] retryCount = {0};
        final int maxRetries = 5;
        final Handler retryHandler = new Handler();

        final Runnable[] verificationRunnable = {null};

        verificationRunnable[0] = new Runnable() {
            @Override
            public void run() {
                // İlk önce mDNS'i dene
                if (retryCount[0] == 2) {
                    tryMDNSVerification();
                    return;
                }

                networkManager.getSystemVerification(new Callback<ApiService.SystemVerificationResponse>() {
                    @Override
                    public void onResponse(Call<ApiService.SystemVerificationResponse> call,
                                           Response<ApiService.SystemVerificationResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ApiService.SystemVerificationResponse verification = response.body();

                            // Bağlantı doğrulandı
                            String newIP = verification.getWifi().getIp();
                            if (newIP != null && !newIP.isEmpty() && !newIP.equals("0.0.0.0")) {
                                prefsManager.saveDeviceIp(newIP);
                                networkManager.resetConnection();

                                // Sistem durumunu tekrar yükle
                                networkManager.getDeviceStatus(new Callback<DeviceStatus>() {
                                    @Override
                                    public void onResponse(Call<DeviceStatus> call2,
                                                           Response<DeviceStatus> response2) {
                                        hideProgressDialog();
                                        showConnectionSuccessDialog(newIP, verification.getWifi().getMode());
                                    }

                                    @Override
                                    public void onFailure(Call<DeviceStatus> call2, Throwable t) {
                                        hideProgressDialog();
                                        showConnectionSuccessDialog(newIP, verification.getWifi().getMode());
                                    }
                                });
                            } else {
                                retryVerification();
                            }
                        } else {
                            retryVerification();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiService.SystemVerificationResponse> call, Throwable t) {
                        retryVerification();
                    }

                    private void retryVerification() {
                        retryCount[0]++;
                        if (retryCount[0] < maxRetries) {
                            updateProgressDialog("Sistem doğrulaması yapılıyor... (Deneme " +
                                    (retryCount[0] + 1) + "/" + maxRetries + ")");
                            int delay = Math.min(3000 * retryCount[0], 10000); // Exponential backoff
                            retryHandler.postDelayed(verificationRunnable[0], delay);
                        } else {
                            hideProgressDialog();
                            showError("Sistem doğrulaması başarısız oldu");
                        }
                    }
                });
            }
        };

        // İlk doğrulama denemesini başlat
        retryHandler.post(verificationRunnable[0]);
    }

    private void showConnectionSuccessDialog(String ip, String mode) {
        new AlertDialog.Builder(WifiSettingsActivity.this)
                .setTitle("Bağlantı Başarılı")
                .setMessage("WiFi bağlantısı kuruldu.\nYeni IP: " + ip +
                        "\nMod: " + mode + "\n\nSistem durumu korundu.")
                .setPositiveButton("Tamam", (dialog, which) -> {
                    updateConnectionInfo();
                    checkWifiStatus();
                    setResult(RESULT_OK);
                })
                .setCancelable(false)
                .show();
    }

    private void tryMDNSVerification() {
        updateProgressDialog("mDNS adresi kontrol ediliyor...");

        new Thread(() -> {
            try {
                // mDNS hostname'i IP adresine çözümle
                InetAddress address = InetAddress.getByName(Constants.MDNS_HOSTNAME);
                String resolvedIP = address.getHostAddress();

                if (resolvedIP != null && !resolvedIP.isEmpty()) {
                    runOnUiThread(() -> {
                        prefsManager.saveDeviceIp(resolvedIP);
                        networkManager.resetConnection();
                        verifySystemStatus();
                    });
                } else {
                    runOnUiThread(() -> {
                        // mDNS çözümlenemedi, doğrudan sistem durumunu kontrol et
                        verifySystemStatus();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "mDNS çözümleme hatası: " + e.getMessage());
                runOnUiThread(() -> {
                    // mDNS başarısız, normal doğrulama yap
                    verifySystemStatus();
                });
            }
        }).start();
    }

    private void verifySystemStatus() {
        updateProgressDialog("Sistem durumu doğrulanıyor...");

        networkManager.getSystemVerification(new Callback<ApiService.SystemVerificationResponse>() {
            @Override
            public void onResponse(Call<ApiService.SystemVerificationResponse> call,
                                   Response<ApiService.SystemVerificationResponse> response) {
                hideProgressDialog();

                if (response.isSuccessful() && response.body() != null) {
                    ApiService.SystemVerificationResponse verification = response.body();

                    // WiFi bilgilerini güncelle
                    String newIP = verification.getWifi().getIp();
                    if (newIP != null && !newIP.isEmpty() && !newIP.equals("0.0.0.0")) {
                        prefsManager.saveDeviceIp(newIP);
                        networkManager.resetConnection();
                    }

                    // Başarı mesajı göster
                    new AlertDialog.Builder(WifiSettingsActivity.this)
                            .setTitle("Bağlantı Başarılı")
                            .setMessage("WiFi bağlantısı kuruldu.\nYeni IP: " + newIP +
                                    "\nMod: " + verification.getWifi().getMode())
                            .setPositiveButton("Tamam", (dialog, which) -> {
                                updateConnectionInfo();
                                checkWifiStatus();
                                setResult(RESULT_OK);
                            })
                            .setCancelable(false)
                            .show();
                } else {
                    showError("Sistem durumu doğrulanamadı");
                }
            }

            @Override
            public void onFailure(Call<ApiService.SystemVerificationResponse> call, Throwable t) {
                hideProgressDialog();
                // Hata olsa bile bağlantı başarılı olabilir
                updateConnectionInfo();
                checkWifiStatus();
                showSuccess("WiFi bağlantısı kuruldu (doğrulama hatası: " + t.getMessage() + ")");
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

                    // Yeni mod değişim API'sini kullan
                    Map<String, Object> params = new HashMap<>();
                    params.put("mode", "ap");

                    networkManager.changeWifiMode(params, new Callback<ApiService.WifiModeChangeResponse>() {
                        @Override
                        public void onResponse(Call<ApiService.WifiModeChangeResponse> call,
                                               Response<ApiService.WifiModeChangeResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                ApiService.WifiModeChangeResponse modeResponse = response.body();

                                if ("success".equals(modeResponse.getStatus())) {
                                    handleAPModeSwitch(modeResponse);
                                } else {
                                    hideProgressDialog();
                                    showError("AP moduna geçilemedi: " + modeResponse.getMessage());
                                }
                            } else {
                                hideProgressDialog();
                                showError("AP moduna geçilemedi - HTTP: " + response.code());
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiService.WifiModeChangeResponse> call, Throwable t) {
                            hideProgressDialog();
                            showError("Bağlantı hatası: " + t.getMessage());
                        }
                    });
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    private void handleAPModeSwitch(ApiService.WifiModeChangeResponse response) {
        updateProgressDialog("AP modu aktif. Yeni IP adresi kontrol ediliyor...");

        // IP ve port bilgilerini güncelle
        String newIP = response.getIpAddress();
        if (newIP != null && !newIP.isEmpty()) {
            prefsManager.saveDeviceIp(newIP);
        } else {
            prefsManager.saveDeviceIp(Constants.DEFAULT_AP_IP);
        }

        prefsManager.saveDevicePort(Constants.DEFAULT_PORT);
        prefsManager.saveConnectionMode(Constants.MODE_AP);

        // NetworkManager'ı AP modu için sıfırla
        networkManager.resetConnection();

        // Kısa bir süre bekle ve durumu güncelle
        new Handler().postDelayed(() -> {
            hideProgressDialog();

            currentMode = Constants.MODE_AP;
            updateConnectionInfo();
            checkWifiStatus();

            showSuccess("AP moduna başarıyla geçildi!");

            // WiFi ayarlarına yönlendirme mesajı
            new AlertDialog.Builder(WifiSettingsActivity.this)
                    .setTitle("AP Modu Aktif")
                    .setMessage("Cihaz artık AP modunda çalışıyor.\nTelefon WiFi ayarlarından KuluckaMK ağına bağlanabilirsiniz.")
                    .setPositiveButton("Tamam", (d, w) -> {
                        setResult(RESULT_OK);
                    })
                    .setCancelable(false)
                    .show();
        }, 2000);
    }

    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    private void updateProgressDialog(String message) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.setMessage(message);
        }
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