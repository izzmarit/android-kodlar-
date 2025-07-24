package com.kulucka.mkv5.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.kulucka.mkv5.R;
import com.kulucka.mkv5.network.ApiService;
import com.kulucka.mkv5.network.NetworkManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OTAUpdateActivity extends AppCompatActivity {
    private static final String TAG = "OTAUpdateActivity";
    private static final int FILE_SELECT_CODE = 1001;

    private TextView tvCurrentVersion;
    private TextView tvBuildDate;
    private TextView tvChipModel;
    private TextView tvFreeSpace;
    private TextView tvSelectedFile;
    private Button btnSelectFile;
    private Button btnStartUpdate;

    private NetworkManager networkManager;
    private File selectedFile;
    private ProgressDialog progressDialog;
    private Handler progressHandler;
    private Runnable progressRunnable;
    private boolean isUpdating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ota_update);

        setupToolbar();
        initializeViews();

        networkManager = NetworkManager.getInstance(this);
        progressHandler = new Handler();

        loadDeviceInfo();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Firmware Güncelleme");
        }
    }

    private void initializeViews() {
        tvCurrentVersion = findViewById(R.id.tvCurrentVersion);
        tvBuildDate = findViewById(R.id.tvBuildDate);
        tvChipModel = findViewById(R.id.tvChipModel);
        tvFreeSpace = findViewById(R.id.tvFreeSpace);
        tvSelectedFile = findViewById(R.id.tvSelectedFile);
        btnSelectFile = findViewById(R.id.btnSelectFile);
        btnStartUpdate = findViewById(R.id.btnStartUpdate);

        btnSelectFile.setOnClickListener(v -> selectFirmwareFile());
        btnStartUpdate.setOnClickListener(v -> startUpdate());
        btnStartUpdate.setEnabled(false);
    }

    private void loadDeviceInfo() {
        networkManager.getOTAInfo(new Callback<ApiService.OTAInfoResponse>() {
            @Override
            public void onResponse(Call<ApiService.OTAInfoResponse> call,
                                   Response<ApiService.OTAInfoResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiService.OTAInfoResponse info = response.body();

                    tvCurrentVersion.setText("Mevcut Versiyon: " + info.getCurrentVersion());
                    tvBuildDate.setText("Derleme Tarihi: " + info.getBuildDate());
                    tvChipModel.setText("Chip Modeli: " + info.getChipModel());
                    tvFreeSpace.setText("Boş Alan: " + formatBytes(info.getFreeSpace()));

                    if (info.isUpdateInProgress()) {
                        showError("Güncelleme devam ediyor!");
                        btnSelectFile.setEnabled(false);
                    }
                } else {
                    showError("Cihaz bilgileri alınamadı");
                }
            }

            @Override
            public void onFailure(Call<ApiService.OTAInfoResponse> call, Throwable t) {
                showError("Bağlantı hatası: " + t.getMessage());
            }
        });
    }

    private void selectFirmwareFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Firmware dosyasını seçin"),
                    FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            showError("Dosya yöneticisi bulunamadı");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_SELECT_CODE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                handleSelectedFile(uri);
            }
        }
    }

    private void handleSelectedFile(Uri uri) {
        try {
            // Dosya adını al
            String fileName = getFileName(uri);

            // Geçici dosya oluştur
            selectedFile = new File(getCacheDir(), fileName);

            // Dosyayı kopyala
            InputStream inputStream = getContentResolver().openInputStream(uri);
            OutputStream output = new FileOutputStream(selectedFile);

            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }

            output.flush();
            output.close();
            inputStream.close();

            // UI güncelle
            tvSelectedFile.setText("Seçilen: " + fileName +
                    "\nBoyut: " + formatBytes(selectedFile.length()));
            btnStartUpdate.setEnabled(true);

        } catch (Exception e) {
            showError("Dosya okuma hatası: " + e.getMessage());
            Log.e(TAG, "File handling error", e);
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void startUpdate() {
        if (selectedFile == null || !selectedFile.exists()) {
            showError("Lütfen firmware dosyası seçin");
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Firmware Güncelleme")
                .setMessage("Güncelleme başlatılacak. Cihaz güncelleme sonrası " +
                        "otomatik olarak yeniden başlayacaktır.\n\n" +
                        "Devam etmek istiyor musunuz?")
                .setPositiveButton("Evet", (dialog, which) -> performUpdate())
                .setNegativeButton("İptal", null)
                .show();
    }

    private void performUpdate() {
        isUpdating = true;
        showProgressDialog("Güncelleme hazırlanıyor...");

        // MD5 checksum hesapla
        String md5 = calculateMD5(selectedFile);

        // Güncellemeyi başlat
        networkManager.uploadFirmware(selectedFile, md5, new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    // İlerleme takibi başlat
                    startProgressMonitoring();
                } else {
                    hideProgressDialog();
                    isUpdating = false;
                    showError("Güncelleme başlatılamadı: HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                hideProgressDialog();
                isUpdating = false;
                showError("Güncelleme hatası: " + t.getMessage());
            }
        });
    }

    private void startProgressMonitoring() {
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isUpdating) return;

                networkManager.getOTAProgress(new Callback<ApiService.OTAProgressResponse>() {
                    @Override
                    public void onResponse(Call<ApiService.OTAProgressResponse> call,
                                           Response<ApiService.OTAProgressResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ApiService.OTAProgressResponse progress = response.body();
                            updateProgress(progress);

                            // Devam eden güncelleme varsa tekrar kontrol et
                            if (progress.getState() == 1) { // OTA_IN_PROGRESS
                                progressHandler.postDelayed(progressRunnable, 1000);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiService.OTAProgressResponse> call, Throwable t) {
                        // Hata durumunda da kontrol etmeye devam et
                        progressHandler.postDelayed(progressRunnable, 2000);
                    }
                });
            }
        };

        progressHandler.post(progressRunnable);
    }

    private void updateProgress(ApiService.OTAProgressResponse progress) {
        int percentage = progress.getProgress();
        String message = String.format("Güncelleniyor... %%%d\n%s / %s",
                percentage,
                formatBytes(progress.getWrittenSize()),
                formatBytes(progress.getTotalSize()));

        updateProgressDialog(message, percentage);

        // Güncelleme durumunu kontrol et
        switch (progress.getState()) {
            case 0: // OTA_IDLE
                hideProgressDialog();
                isUpdating = false;
                break;

            case 2: // OTA_SUCCESS
                hideProgressDialog();
                isUpdating = false;
                showUpdateSuccess();
                break;

            case 3: // OTA_ERROR
                hideProgressDialog();
                isUpdating = false;
                showError("Güncelleme hatası: " + progress.getError());
                break;
        }
    }

    private void showUpdateSuccess() {
        new AlertDialog.Builder(this)
                .setTitle("Güncelleme Başarılı")
                .setMessage("Firmware güncellemesi başarıyla tamamlandı.\n" +
                        "Cihaz yeniden başlatılıyor...")
                .setPositiveButton("Tamam", (dialog, which) -> {
                    setResult(RESULT_OK);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private String calculateMD5(File file) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            InputStream is = new java.io.FileInputStream(file);
            byte[] buffer = new byte[8192];
            int read;

            while ((read = is.read(buffer)) > 0) {
                md.update(buffer, 0, read);
            }
            is.close();

            byte[] md5sum = md.digest();
            StringBuilder result = new StringBuilder();
            for (byte b : md5sum) {
                result.append(String.format("%02x", b));
            }

            return result.toString();
        } catch (Exception e) {
            Log.e(TAG, "MD5 calculation error", e);
            return "";
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setMax(100);
        }
        progressDialog.setMessage(message);
        progressDialog.setProgress(0);
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    private void updateProgressDialog(String message, int progress) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.setMessage(message);
            progressDialog.setProgress(progress);
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

        if (progressHandler != null && progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
        }

        // Geçici dosyayı sil
        if (selectedFile != null && selectedFile.exists()) {
            selectedFile.delete();
        }
    }
}