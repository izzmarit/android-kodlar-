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

    public static synchronized Retrofit getClient(String baseUrl) {
        // Eğer base URL değiştiyse, yeni bir Retrofit instance oluştur
        if (retrofit == null || !baseUrl.equals(currentBaseUrl)) {
            currentBaseUrl = baseUrl;

            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Cache-Control interceptor
            Interceptor cacheControlInterceptor = chain -> {
                Request original = chain.request();

                // Cache-Control header'ları ekle
                Request request = original.newBuilder()
                        .header("Cache-Control", "no-cache")
                        .header("Pragma", "no-cache")
                        .cacheControl(CacheControl.FORCE_NETWORK)
                        .method(original.method(), original.body())
                        .build();

                return chain.proceed(request);
            };

            // WiFi mod değişimleri için özel timeout interceptor
            Interceptor timeoutInterceptor = chain -> {
                Request request = chain.request();
                String url = request.url().toString();

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

                // Sistem doğrulama için orta timeout
                if (url.contains("/api/system/verify")) {
                    return chain.withConnectTimeout(8, TimeUnit.SECONDS)
                            .withReadTimeout(10, TimeUnit.SECONDS)
                            .withWriteTimeout(8, TimeUnit.SECONDS)
                            .proceed(request);
                }

                // Diğer endpoint'ler için normal timeout
                return chain.proceed(request);
            };

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .addInterceptor(cacheControlInterceptor)
                    .addInterceptor(timeoutInterceptor)
                    .addNetworkInterceptor(cacheControlInterceptor)
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