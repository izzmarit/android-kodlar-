package com.kulucka.controller.api;

import com.kulucka.controller.services.ApiService;
import com.kulucka.controller.utils.Constants;
import com.kulucka.controller.utils.SharedPreferencesManager;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit istemci sınıfı
 * API bağlantılarını yönetir ve ApiService instance'ını sağlar
 */
public class RetrofitClient {
    private static RetrofitClient instance;
    private final ApiService apiService;
    private Retrofit retrofit;

    private RetrofitClient(String baseUrl) {
        // HTTP loglama interceptor'ı oluştur
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // OkHttpClient yapılandırması
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(Constants.CONNECTION_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(Constants.READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(Constants.WRITE_TIMEOUT, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .addInterceptor(chain -> {
                    // Her istek için özel header'lar eklenebilir
                    return chain.proceed(chain.request().newBuilder()
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Accept", "application/json")
                            .build());
                })
                .build();

        // Retrofit instance oluştur
        retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // ApiService oluştur
        apiService = retrofit.create(ApiService.class);
    }

    /**
     * Singleton instance'ı al
     * @param baseUrl API base URL
     * @return RetrofitClient instance
     */
    public static synchronized RetrofitClient getInstance(String baseUrl) {
        if (instance == null || !baseUrl.equals(instance.getBaseUrl())) {
            instance = new RetrofitClient(baseUrl);
        }
        return instance;
    }

    /**
     * Varsayılan base URL ile instance al
     * @param preferencesManager SharedPreferences yöneticisi
     * @return RetrofitClient instance
     */
    public static synchronized RetrofitClient getInstance(SharedPreferencesManager preferencesManager) {
        String deviceIp = preferencesManager.getDeviceIp();
        String baseUrl = "http://" + deviceIp + ":" + Constants.DEFAULT_PORT;
        return getInstance(baseUrl);
    }

    /**
     * ApiService instance'ını al
     * @return ApiService
     */
    public ApiService getApiService() {
        return apiService;
    }

    /**
     * Mevcut base URL'i al
     * @return Base URL
     */
    private String getBaseUrl() {
        return retrofit.baseUrl().toString();
    }

    /**
     * Retrofit client'ı yeniden başlat
     * @param newBaseUrl Yeni base URL
     */
    public static void resetClient(String newBaseUrl) {
        instance = null;
        getInstance(newBaseUrl);
    }
}