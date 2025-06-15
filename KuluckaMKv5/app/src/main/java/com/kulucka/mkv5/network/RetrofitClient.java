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

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .addInterceptor(cacheControlInterceptor)
                    .addNetworkInterceptor(cacheControlInterceptor) // Network interceptor olarak da ekle
                    .connectTimeout(Constants.CONNECTION_TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(Constants.WRITE_TIMEOUT, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .cache(null) // Cache'i tamamen devre dışı bırak
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