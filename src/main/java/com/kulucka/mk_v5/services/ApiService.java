package com.kulucka.mk_v5.services;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.kulucka.mk_v5.models.IncubationStatus;
import com.kulucka.mk_v5.utils.Constants;
import com.kulucka.mk_v5.utils.SharedPrefsManager;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiService {
    private static final String TAG = "ApiService";
    private static ApiService instance;
    private KuluckaApi api;
    private OkHttpClient client;
    private MutableLiveData<IncubationStatus> statusLiveData = new MutableLiveData<>();
    private MutableLiveData<Boolean> connectionLiveData = new MutableLiveData<>();
    private Handler mainHandler;
    private int retryCount = 0;

    public interface KuluckaApi {
        @retrofit2.http.GET(Constants.STATUS_ENDPOINT)
        retrofit2.Call<IncubationStatus> getStatus();
    }

    public static ApiService getInstance() {
        if (instance == null) {
            instance = new ApiService();
        }
        return instance;
    }

    public static void init() {
        getInstance();
    }

    private ApiService() {
        connectionLiveData.setValue(false);
        mainHandler = new Handler(Looper.getMainLooper());
        setupApi();
    }

    public void setupApi() {
        try {
            String ipAddress = SharedPrefsManager.getInstance().getDeviceIp();
            String baseUrl = String.format(Constants.BASE_URL, ipAddress);

            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

            client = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .readTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .writeTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            api = retrofit.create(KuluckaApi.class);
            Log.d(TAG, "API initialized with base URL: " + baseUrl);
        } catch (Exception e) {
            Log.e(TAG, "API setup failed", e);
        }
    }

    public void refreshStatus() {
        if (api == null) {
            Log.w(TAG, "API not initialized, setting up again");
            setupApi();
            if (api == null) {
                connectionLiveData.postValue(false);
                return;
            }
        }

        try {
            api.getStatus().enqueue(new retrofit2.Callback<IncubationStatus>() {
                @Override
                public void onResponse(retrofit2.Call<IncubationStatus> call, retrofit2.Response<IncubationStatus> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            IncubationStatus status = response.body();

                            if (isValidStatus(status)) {
                                statusLiveData.postValue(status);
                                connectionLiveData.postValue(true);
                                retryCount = 0;
                                Log.d(TAG, "Status updated successfully");
                            } else {
                                Log.w(TAG, "Invalid status data received");
                                connectionLiveData.postValue(false);
                            }
                        } else {
                            Log.w(TAG, "Response not successful: " + response.code());
                            connectionLiveData.postValue(false);
                            handleRetry();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing response", e);
                        connectionLiveData.postValue(false);
                        handleRetry();
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<IncubationStatus> call, Throwable t) {
                    Log.e(TAG, "Network request failed", t);
                    connectionLiveData.postValue(false);
                    handleRetry();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error making status request", e);
            connectionLiveData.postValue(false);
        }
    }

    public void setParameter(String endpoint, String jsonData, ParameterCallback callback) {
        if (client == null) {
            setupApi();
            if (client == null) {
                callback.onError("API başlatılamadı");
                return;
            }
        }

        String ipAddress = SharedPrefsManager.getInstance().getDeviceIp();
        String url = String.format(Constants.BASE_URL, ipAddress) + "api/" + endpoint;

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON, jsonData);

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";

                mainHandler.post(() -> {
                    if (response.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onError("Request failed: " + response.code() + " - " + responseBody);
                    }
                });
            }
        });
    }

    private boolean isValidStatus(IncubationStatus status) {
        if (status == null) return false;
        float temp = status.getTemperature();
        float humid = status.getHumidity();
        return temp >= -50 && temp <= 100 && humid >= 0 && humid <= 100;
    }

    private void handleRetry() {
        if (retryCount < Constants.MAX_RETRY_COUNT) {
            retryCount++;
            Log.d(TAG, "Retrying request, attempt: " + retryCount);
            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    refreshStatus();
                }
            }, 2000);
        } else {
            Log.w(TAG, "Max retry count reached, giving up");
            retryCount = 0;
        }
    }

    public LiveData<IncubationStatus> getStatusLiveData() {
        return statusLiveData;
    }

    public LiveData<Boolean> getConnectionLiveData() {
        return connectionLiveData;
    }

    public interface ParameterCallback {
        void onSuccess();
        void onError(String message);
    }
}