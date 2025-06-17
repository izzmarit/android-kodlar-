package com.kulucka.mkv5.network;

import com.kulucka.mkv5.utils.Constants;

import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit = null;
    private static String currentBaseUrl = null;

    // Geliştirilmiş timeout interceptor
    private static Interceptor createTimeoutInterceptor() {
        return chain -> {
            Request request = chain.request();
            String url = request.url().toString();

            // Motor test endpoint'i için uzun timeout
            if (url.contains("/api/motor/test")) {
                return chain.withConnectTimeout(10, TimeUnit.SECONDS)
                        .withReadTimeout(Constants.MOTOR_TEST_TIMEOUT, TimeUnit.SECONDS)
                        .withWriteTimeout(10, TimeUnit.SECONDS)
                        .proceed(request);
            }

            // Sistem kaydetme için orta timeout
            if (url.contains("/api/system/save")) {
                return chain.withConnectTimeout(5, TimeUnit.SECONDS)
                        .withReadTimeout(Constants.SYSTEM_SAVE_TIMEOUT, TimeUnit.SECONDS)
                        .withWriteTimeout(5, TimeUnit.SECONDS)
                        .proceed(request);
            }

            // Sistem sağlık kontrolü için orta timeout
            if (url.contains("/api/system/health")) {
                return chain.withConnectTimeout(8, TimeUnit.SECONDS)
                        .withReadTimeout(Constants.HEALTH_CHECK_TIMEOUT, TimeUnit.SECONDS)
                        .withWriteTimeout(8, TimeUnit.SECONDS)
                        .proceed(request);
            }

            // Log getirme için uzun timeout
            if (url.contains("/api/system/logs")) {
                return chain.withConnectTimeout(10, TimeUnit.SECONDS)
                        .withReadTimeout(Constants.LOG_FETCH_TIMEOUT, TimeUnit.SECONDS)
                        .withWriteTimeout(10, TimeUnit.SECONDS)
                        .proceed(request);
            }

            // Sistem aksiyonları için çok uzun timeout
            if (url.contains("/api/system/action")) {
                return chain.withConnectTimeout(15, TimeUnit.SECONDS)
                        .withReadTimeout(Constants.SYSTEM_ACTION_TIMEOUT, TimeUnit.SECONDS)
                        .withWriteTimeout(15, TimeUnit.SECONDS)
                        .proceed(request);
            }

            // WiFi mod değişimi endpoint'leri için uzun timeout
            if (url.contains("/api/wifi/mode") || url.contains("/api/wifi/connect")) {
                return chain.withConnectTimeout(20, TimeUnit.SECONDS)
                        .withReadTimeout(25, TimeUnit.SECONDS)
                        .withWriteTimeout(20, TimeUnit.SECONDS)
                        .proceed(request);
            }

            // WiFi ağları tarama için uzun timeout
            if (url.contains("/api/wifi/networks")) {
                return chain.withConnectTimeout(15, TimeUnit.SECONDS)
                        .withReadTimeout(20, TimeUnit.SECONDS)
                        .withWriteTimeout(15, TimeUnit.SECONDS)
                        .proceed(request);
            }

            // WiFi credential kontrolü için kısa timeout
            if (url.contains("/api/wifi/credentials")) {
                return chain.withConnectTimeout(5, TimeUnit.SECONDS)
                        .withReadTimeout(8, TimeUnit.SECONDS)
                        .withWriteTimeout(5, TimeUnit.SECONDS)
                        .proceed(request);
            }

            // Sistem doğrulama için orta timeout
            if (url.contains("/api/system/verify")) {
                return chain.withConnectTimeout(8, TimeUnit.SECONDS)
                        .withReadTimeout(10, TimeUnit.SECONDS)
                        .withWriteTimeout(8, TimeUnit.SECONDS)
                        .proceed(request);
            }

            // Ağ test etme için orta timeout
            if (url.contains("/api/network/test")) {
                return chain.withConnectTimeout(10, TimeUnit.SECONDS)
                        .withReadTimeout(15, TimeUnit.SECONDS)
                        .withWriteTimeout(10, TimeUnit.SECONDS)
                        .proceed(request);
            }

            // Diğer endpoint'ler için normal timeout
            return chain.proceed(request);
        };
    }

    // Cache control interceptor
    private static Interceptor createCacheControlInterceptor() {
        return chain -> {
            Request original = chain.request();
            String url = original.url().toString();

            Request.Builder requestBuilder = original.newBuilder()
                    .header("Cache-Control", "no-cache")
                    .header("Pragma", "no-cache")
                    .cacheControl(CacheControl.FORCE_NETWORK);

            // Sistem durumu endpoint'leri için özel header'lar
            if (url.contains("/api/status") || url.contains("/api/system/")) {
                requestBuilder.header("X-Requested-With", "KuluckaMKv5")
                        .header("Accept", "application/json");
            }

            // WiFi işlemleri için özel header'lar
            if (url.contains("/api/wifi/")) {
                requestBuilder.header("X-WiFi-Operation", "true")
                        .header("Accept", "application/json");
            }

            Request request = requestBuilder.method(original.method(), original.body()).build();
            return chain.proceed(request);
        };
    }

    // Hata yeniden deneme interceptor'u
    private static Interceptor createRetryInterceptor() {
        return chain -> {
            Request request = chain.request();
            String url = request.url().toString();

            // Kritik olmayan endpoint'ler için retry logic
            if (url.contains("/api/status") || url.contains("/api/ping")) {
                int maxRetries = 2;
                int retryCount = 0;

                while (retryCount < maxRetries) {
                    try {
                        return chain.proceed(request);
                    } catch (Exception e) {
                        retryCount++;
                        if (retryCount >= maxRetries) {
                            throw e;
                        }

                        try {
                            Thread.sleep(1000 * retryCount); // Exponential backoff
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(ie);
                        }
                    }
                }
            }

            return chain.proceed(request);
        };
    }

    public static synchronized Retrofit getClient(String baseUrl) {
        // Eğer base URL değiştiyse, yeni bir Retrofit instance oluştur
        if (retrofit == null || !baseUrl.equals(currentBaseUrl)) {
            currentBaseUrl = baseUrl;

            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .addInterceptor(createCacheControlInterceptor())
                    .addInterceptor(createTimeoutInterceptor())
                    .addInterceptor(createRetryInterceptor())
                    .addNetworkInterceptor(createCacheControlInterceptor())
                    .connectTimeout(Constants.CONNECTION_TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(Constants.READ_TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(Constants.WRITE_TIMEOUT, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .cache(null)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }

        return retrofit;
    }

    public static void resetClient() {
        retrofit = null;
        currentBaseUrl = null;
    }
}