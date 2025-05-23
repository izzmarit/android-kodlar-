package com.kulucka.mk_v5.services;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.kulucka.mk_v5.models.IncubationStatus;
import com.kulucka.mk_v5.utils.Constants;
import com.kulucka.mk_v5.utils.SharedPrefsManager;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;

public class ApiService {
    private static final String TAG = "ApiService";
    private static ApiService instance;
    private KuluckaApi api;
    private MutableLiveData<IncubationStatus> statusLiveData = new MutableLiveData<>();
    private MutableLiveData<Boolean> connectionLiveData = new MutableLiveData<>();
    private Handler mainHandler;
    private int retryCount = 0;

    public interface KuluckaApi {
        @GET(Constants.STATUS_ENDPOINT)
        Call<IncubationStatus> getStatus();

        @FormUrlEncoded
        @POST(Constants.SET_PARAM_ENDPOINT)
        Call<ResponseBody> setParameter(
                @Field("param") String param,
                @Field("value") String value
        );
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
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC); // BODY yerine BASIC

            OkHttpClient client = new OkHttpClient.Builder()
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
            api.getStatus().enqueue(new Callback<IncubationStatus>() {
                @Override
                public void onResponse(Call<IncubationStatus> call, Response<IncubationStatus> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            IncubationStatus status = response.body();

                            // Veri doğrulama
                            if (isValidStatus(status)) {
                                statusLiveData.postValue(status);
                                connectionLiveData.postValue(true);
                                retryCount = 0; // Başarılı olduğunda retry sayacını sıfırla
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
                public void onFailure(Call<IncubationStatus> call, Throwable t) {
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

    private boolean isValidStatus(IncubationStatus status) {
        if (status == null) return false;

        // Temel veri doğrulama
        float temp = status.getTemperature();
        float humid = status.getHumidity();

        return temp >= -50 && temp <= 100 && humid >= 0 && humid <= 100;
    }

    private void handleRetry() {
        if (retryCount < Constants.MAX_RETRY_COUNT) {
            retryCount++;
            Log.d(TAG, "Retrying request, attempt: " + retryCount);

            // 2 saniye sonra tekrar dene
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

    public void setParameter(String parameter, String value, final ParameterCallback callback) {
        if (api == null) {
            setupApi();
            if (api == null) {
                callback.onError("API başlatılamadı");
                return;
            }
        }

        if (parameter == null || parameter.isEmpty() || value == null) {
            callback.onError("Geçersiz parametre veya değer");
            return;
        }

        try {
            api.setParameter(parameter, value).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "Parameter set successfully: " + parameter + "=" + value);
                            callback.onSuccess();
                        } else {
                            String error = "İstek başarısız oldu: " + response.code();
                            Log.w(TAG, error);
                            callback.onError(error);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing parameter response", e);
                        callback.onError("Yanıt işlenirken hata oluştu");
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    String error = "Bağlantı hatası: " + t.getMessage();
                    Log.e(TAG, error, t);
                    callback.onError(error);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error making parameter request", e);
            callback.onError("İstek gönderilirken hata oluştu");
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