package com.kulucka.mkv5.activities;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WifiSettingsActivity extends AppCompatActivity {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private Button btnSwitchToAP;
    private Button btnRefresh;

    private WifiNetworkAdapter adapter;
    private NetworkManager networkManager;
    private List<WifiNetwork> wifiNetworks = new ArrayList<>();
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_settings);

        setupToolbar();
        initializeViews();
        setupRecyclerView();

        networkManager = NetworkManager.getInstance(this);

        // Load WiFi networks
        loadWifiNetworks();
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
        btnSwitchToAP = findViewById(R.id.btnSwitchToAP);
        btnRefresh = findViewById(R.id.btnRefresh);

        swipeRefresh.setOnRefreshListener(this::loadWifiNetworks);
        btnSwitchToAP.setOnClickListener(v -> switchToAPMode());
        btnRefresh.setOnClickListener(v -> loadWifiNetworks());
    }

    private void setupRecyclerView() {
        adapter = new WifiNetworkAdapter(wifiNetworks, this::showPasswordDialog);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadWifiNetworks() {
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
                    }
                    adapter.notifyDataSetChanged();

                    if (wifiNetworks.isEmpty()) {
                        Toast.makeText(WifiSettingsActivity.this,
                                "Hiç WiFi ağı bulunamadı", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    showError("WiFi ağları yüklenemedi");
                }
            }

            @Override
            public void onFailure(Call<ApiService.WifiNetworksResponse> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                showError("Bağlantı hatası: " + t.getMessage());
            }
        });
    }

    private void showPasswordDialog(WifiNetwork network) {
        if (network.isOpen()) {
            // Open network, connect directly
            connectToWifi(network.getSsid(), "");
            return;
        }

        // Show password dialog
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

        networkManager.connectToWifi(ssid, password, new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    showProgressDialog("Bağlantı başarılı! Cihaz yeniden başlatılıyor...");

                    // Wait for device to restart and connect
                    new Handler().postDelayed(() -> {
                        hideProgressDialog();
                        showSuccess("WiFi bağlantısı kuruldu!");
                        finish();
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

    private void switchToAPMode() {
        new AlertDialog.Builder(this)
                .setTitle("AP Moduna Geç")
                .setMessage("AP moduna geçmek istediğinizden emin misiniz?")
                .setPositiveButton("Evet", (dialog, which) -> {
                    showProgressDialog("AP moduna geçiliyor...");

                    networkManager.switchToAPMode(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            hideProgressDialog();

                            if (response.isSuccessful()) {
                                showSuccess("AP moduna geçildi!");
                                finish();
                            } else {
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