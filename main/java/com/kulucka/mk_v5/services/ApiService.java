package com.kulucka.mk_v5.services;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.kulucka.mk_v5.models.IncubationStatus;
import com.kulucka.mk_v5.utils.Constants;
import com.kulucka.mk_v5.utils.SharedPrefsManager;

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
    private static ApiService instance;
    private KuluckaApi api;
    private MutableLiveData<IncubationStatus> statusLiveData = new MutableLiveData<>();
    private MutableLiveData<Boolean> connectionLiveData = new MutableLiveData<>();

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

    // Singleton örneğini döndür
    public static ApiService getInstance() {
        if (instance == null) {
            instance = new ApiService();
        }
        return instance;
    }

    // İlk kurulum
    public static void init() {
        getInstance();
    }

    private ApiService() {
        connectionLiveData.setValue(false);
        setupApi();
    }

    // API ayarları
    public void setupApi() {
        String ipAddress = SharedPrefsManager.getInstance().getDeviceIp();
        String baseUrl = String.format(Constants.BASE_URL, ipAddress);

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(KuluckaApi.class);
    }

    // Durumu güncelle
    public void refreshStatus() {
        if (api == null) {
            setupApi();
        }

        api.getStatus().enqueue(new Callback<IncubationStatus>() {
            @Override
            public void onResponse(Call<IncubationStatus> call, Response<IncubationStatus> response) {
                if (response.isSuccessful() && response.body() != null) {
                    statusLiveData.postValue(response.body());
                    connectionLiveData.postValue(true);
                } else {
                    connectionLiveData.postValue(false);
                }
            }

            @Override
            public void onFailure(Call<IncubationStatus> call, Throwable t) {
                connectionLiveData.postValue(false);
            }
        });
    }

    // Parametre değiştirme
    public void setParameter(String parameter, String value, final ParameterCallback callback) {
        if (api == null) {
            setupApi();
        }

        api.setParameter(parameter, value).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onError("İstek başarısız oldu: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                callback.onError("Bağlantı hatası: " + t.getMessage());
            }
        });
    }

    // Durum live data'sını döndür
    public LiveData<IncubationStatus> getStatusLiveData() {
        return statusLiveData;
    }

    // Bağlantı live data'sını döndür
    public LiveData<Boolean> getConnectionLiveData() {
        return connectionLiveData;
    }

    // Parametre değiştirme geri çağırma arayüzü
    public interface ParameterCallback {
        void onSuccess();
        void onError(String message);
    }
}