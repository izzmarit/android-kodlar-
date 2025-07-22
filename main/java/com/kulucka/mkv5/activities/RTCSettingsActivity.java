package com.kulucka.mkv5.activities;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.kulucka.mkv5.R;
import com.kulucka.mkv5.network.ApiService;
import com.kulucka.mkv5.network.NetworkManager;

import java.util.Calendar;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RTCSettingsActivity extends AppCompatActivity {

    private TextView tvCurrentTime, tvCurrentDate, tvRTCStatus, tvErrorCount;
    private Button btnSetTime, btnSetDate, btnRefresh;

    private NetworkManager networkManager;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rtc_settings);

        setupToolbar();
        initializeViews();
        setupListeners();

        networkManager = NetworkManager.getInstance(this);

        loadRTCStatus();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("RTC Saat Ayarları");
        }
    }

    private void initializeViews() {
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvCurrentDate = findViewById(R.id.tvCurrentDate);
        tvRTCStatus = findViewById(R.id.tvRTCStatus);
        tvErrorCount = findViewById(R.id.tvErrorCount);

        btnSetTime = findViewById(R.id.btnSetTime);
        btnSetDate = findViewById(R.id.btnSetDate);
        btnRefresh = findViewById(R.id.btnRefresh);
    }

    private void setupListeners() {
        btnSetTime.setOnClickListener(v -> showTimePicker());
        btnSetDate.setOnClickListener(v -> showDatePicker());
        btnRefresh.setOnClickListener(v -> loadRTCStatus());
    }

    private void loadRTCStatus() {
        showProgressDialog("RTC durumu yükleniyor...");

        networkManager.getRTCStatus(new Callback<ApiService.RTCStatusResponse>() {
            @Override
            public void onResponse(Call<ApiService.RTCStatusResponse> call,
                                   Response<ApiService.RTCStatusResponse> response) {
                hideProgressDialog();

                if (response.isSuccessful() && response.body() != null) {
                    updateUI(response.body());
                } else {
                    showError("RTC durumu alınamadı");
                }
            }

            @Override
            public void onFailure(Call<ApiService.RTCStatusResponse> call, Throwable t) {
                hideProgressDialog();
                showError("Bağlantı hatası: " + t.getMessage());
            }
        });
    }

    private void updateUI(ApiService.RTCStatusResponse status) {
        tvCurrentTime.setText(status.getTime());
        tvCurrentDate.setText(status.getDate());

        if ("working".equals(status.getStatus())) {
            tvRTCStatus.setText("RTC Çalışıyor");
            tvRTCStatus.setTextColor(getColor(R.color.success));
        } else {
            tvRTCStatus.setText("RTC Hatası");
            tvRTCStatus.setTextColor(getColor(R.color.error));
        }

        tvErrorCount.setText("Hata Sayısı: " + status.getErrorCount());
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, selectedHour, selectedMinute) -> {
                    setRTCTime(selectedHour, selectedMinute);
                }, hour, minute, true);

        timePickerDialog.show();
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    setRTCDate(selectedDay, selectedMonth + 1, selectedYear);
                }, year, month, day);

        datePickerDialog.show();
    }

    private void setRTCTime(int hour, int minute) {
        showProgressDialog("Saat ayarlanıyor...");

        networkManager.setRTCTime(hour, minute, new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                hideProgressDialog();

                if (response.isSuccessful()) {
                    showSuccess("Saat başarıyla ayarlandı");
                    loadRTCStatus();
                } else {
                    showError("Saat ayarlanamadı");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                hideProgressDialog();
                showError("Bağlantı hatası: " + t.getMessage());
            }
        });
    }

    private void setRTCDate(int day, int month, int year) {
        showProgressDialog("Tarih ayarlanıyor...");

        networkManager.setRTCDate(day, month, year, new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                hideProgressDialog();

                if (response.isSuccessful()) {
                    showSuccess("Tarih başarıyla ayarlandı");
                    loadRTCStatus();
                } else {
                    showError("Tarih ayarlanamadı");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                hideProgressDialog();
                showError("Bağlantı hatası: " + t.getMessage());
            }
        });
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