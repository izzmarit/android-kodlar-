package com.kulucka.mk.services;

import android.content.Context;
import android.util.Log;

import com.kulucka.mk.models.*;
import com.kulucka.mk.utils.Constants;
import com.kulucka.mk.utils.NetworkUtils;

import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.HashMap;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.*;

/**
 * ESP32 cihazı ile HTTP iletişimini sağlayan servis sınıfı
 * Retrofit kullanarak REST API çağrıları yapar
 */
public class ApiService {

    private static final String TAG = "ApiService";
    private static ApiService instance;

    private Context context;
    private String currentIpAddress;
    private Retrofit retrofit;
    private KuluckaApiInterface apiInterface;
    private boolean isConnected = false;

    // Singleton pattern
    public static synchronized ApiService getInstance(Context context) {
        if (instance == null) {
            instance = new ApiService(context.getApplicationContext());
        }
        return instance;
    }

    private ApiService(Context context) {
        this.context = context;
        setupRetrofit(Constants.ESP32_AP_IP); // Varsayılan AP IP'si ile başla
    }

    /**
     * Retrofit konfigürasyonu
     */
    private void setupRetrofit(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            Log.e(TAG, "IP adresi boş olamaz");
            return;
        }

        this.currentIpAddress = ipAddress;
        String baseUrl = Constants.getApiUrl(ipAddress);

        // HTTP logging interceptor (debug için)
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // OkHttp client konfigürasyonu
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                .connectTimeout(Constants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(Constants.READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(Constants.WRITE_TIMEOUT, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .retryOnConnectionFailure(true);

        // Retrofit instance oluştur
        retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(httpClient.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiInterface = retrofit.create(KuluckaApiInterface.class);

        Log.i(TAG, "Retrofit yapılandırıldı - Base URL: " + baseUrl);
    }

    /**
     * IP adresini güncelle ve Retrofit'i yeniden yapılandır
     */
    public void updateIpAddress(String ipAddress) {
        if (!ipAddress.equals(currentIpAddress)) {
            Log.i(TAG, "IP adresi güncelleniyor: " + currentIpAddress + " -> " + ipAddress);
            setupRetrofit(ipAddress);
        }
    }

    /**
     * Mevcut IP adresini al
     */
    public String getCurrentIpAddress() {
        return currentIpAddress;
    }

    /**
     * Bağlantı durumunu kontrol et
     */
    public boolean isConnected() {
        return isConnected && NetworkUtils.isNetworkAvailable(context);
    }

    /**
     * Sistem durumunu al - Ana veri endpoint'i
     */
    public void getSystemStatus(ApiCallback<SystemStatus> callback) {
        if (apiInterface == null) {
            callback.onError("API interface yapılandırılmamış");
            return;
        }

        Call<SystemStatus> call = apiInterface.getSystemStatus();
        call.enqueue(new Callback<SystemStatus>() {
            @Override
            public void onResponse(Call<SystemStatus> call, Response<SystemStatus> response) {
                if (response.isSuccessful() && response.body() != null) {
                    isConnected = true;
                    callback.onSuccess(response.body());
                } else {
                    isConnected = false;
                    callback.onError("Sistem durumu alınamadı - HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<SystemStatus> call, Throwable t) {
                isConnected = false;
                Log.e(TAG, "Sistem durumu hatası", t);
                callback.onError("Bağlantı hatası: " + t.getMessage());
            }
        });
    }

    /**
     * WiFi ağları listesini al
     */
    public void getWiFiNetworks(ApiCallback<WiFiNetworksResponse> callback) {
        if (apiInterface == null) {
            callback.onError("API interface yapılandırılmamış");
            return;
        }

        Call<WiFiNetworksResponse> call = apiInterface.getWiFiNetworks();
        call.enqueue(new Callback<WiFiNetworksResponse>() {
            @Override
            public void onResponse(Call<WiFiNetworksResponse> call, Response<WiFiNetworksResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("WiFi ağları alınamadı - HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<WiFiNetworksResponse> call, Throwable t) {
                Log.e(TAG, "WiFi ağları hatası", t);
                callback.onError("WiFi ağları yüklenemedi: " + t.getMessage());
            }
        });
    }

    /**
     * WiFi ağına bağlan
     */
    public void connectToWiFi(String ssid, String password, ApiCallback<ApiResponse> callback) {
        if (apiInterface == null) {
            callback.onError("API interface yapılandırılmamış");
            return;
        }

        Map<String, String> credentials = new HashMap<>();
        credentials.put("ssid", ssid);
        credentials.put("password", password);

        Call<ApiResponse> call = apiInterface.connectToWiFi(credentials);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("WiFi bağlantısı başarısız - HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e(TAG, "WiFi bağlantı hatası", t);
                callback.onError("WiFi bağlantısı kurulamadı: " + t.getMessage());
            }
        });
    }

    /**
     * AP moduna geç
     */
    public void switchToAPMode(ApiCallback<ApiResponse> callback) {
        if (apiInterface == null) {
            callback.onError("API interface yapılandırılmamış");
            return;
        }

        Call<ApiResponse> call = apiInterface.switchToAPMode();
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("AP modu geçişi başarısız - HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e(TAG, "AP modu hatası", t);
                callback.onError("AP moduna geçilemedi: " + t.getMessage());
            }
        });
    }

    /**
     * Hedef sıcaklığı ayarla
     */
    public void setTargetTemperature(double temperature, ApiCallback<ApiResponse> callback) {
        if (apiInterface == null) {
            callback.onError("API interface yapılandırılmamış");
            return;
        }

        Map<String, Double> data = new HashMap<>();
        data.put("targetTemp", temperature);

        Call<ApiResponse> call = apiInterface.setTemperature(data);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Sıcaklık ayarlanamadı - HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e(TAG, "Sıcaklık ayarlama hatası", t);
                callback.onError("Sıcaklık ayarı başarısız: " + t.getMessage());
            }
        });
    }

    /**
     * Hedef nem ayarla
     */
    public void setTargetHumidity(double humidity, ApiCallback<ApiResponse> callback) {
        if (apiInterface == null) {
            callback.onError("API interface yapılandırılmamış");
            return;
        }

        Map<String, Double> data = new HashMap<>();
        data.put("targetHumid", humidity);

        Call<ApiResponse> call = apiInterface.setHumidity(data);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Nem ayarlanamadı - HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e(TAG, "Nem ayarlama hatası", t);
                callback.onError("Nem ayarı başarısız: " + t.getMessage());
            }
        });
    }

    /**
     * PID parametrelerini ayarla
     */
    public void setPIDSettings(PIDSettings pidSettings, ApiCallback<ApiResponse> callback) {
        if (apiInterface == null) {
            callback.onError("API interface yapılandırılmamış");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("pidMode", pidSettings.getPidMode());
        data.put("kp", pidSettings.getPidKp());
        data.put("ki", pidSettings.getPidKi());
        data.put("kd", pidSettings.getPidKd());

        Call<ApiResponse> call = apiInterface.setPIDSettings(data);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("PID ayarları değiştirilemedi - HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e(TAG, "PID ayarlama hatası", t);
                callback.onError("PID ayarları başarısız: " + t.getMessage());
            }
        });
    }

    /**
     * Motor ayarlarını değiştir
     */
    public void setMotorSettings(int waitTime, int runTime, ApiCallback<ApiResponse> callback) {
        if (apiInterface == null) {
            callback.onError("API interface yapılandırılmamış");
            return;
        }

        Map<String, Integer> data = new HashMap<>();
        data.put("waitTime", waitTime);
        data.put("runTime", runTime);

        Call<ApiResponse> call = apiInterface.setMotorSettings(data);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Motor ayarları değiştirilemedi - HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e(TAG, "Motor ayarlama hatası", t);
                callback.onError("Motor ayarları başarısız: " + t.getMessage());
            }
        });
    }

    /**
     * Alarm ayarlarını değiştir
     */
    public void setAlarmSettings(AlarmSettings alarmSettings, ApiCallback<ApiResponse> callback) {
        if (apiInterface == null) {
            callback.onError("API interface yapılandırılmamış");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("alarmEnabled", alarmSettings.isAlarmEnabled());
        data.put("tempLowAlarm", alarmSettings.getTempLowAlarm());
        data.put("tempHighAlarm", alarmSettings.getTempHighAlarm());
        data.put("humidLowAlarm", alarmSettings.getHumidLowAlarm());
        data.put("humidHighAlarm", alarmSettings.getHumidHighAlarm());

        Call<ApiResponse> call = apiInterface.setAlarmSettings(data);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Alarm ayarları değiştirilemedi - HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e(TAG, "Alarm ayarlama hatası", t);
                callback.onError("Alarm ayarları başarısız: " + t.getMessage());
            }
        });
    }

    /**
     * Kalibrasyon ayarlarını değiştir
     */
    public void setCalibrationSettings(CalibrationData calibrationData, ApiCallback<ApiResponse> callback) {
        if (apiInterface == null) {
            callback.onError("API interface yapılandırılmamış");
            return;
        }

        Map<String, Double> data = new HashMap<>();
        data.put("tempCal1", calibrationData.getTempCalibration1());
        data.put("tempCal2", calibrationData.getTempCalibration2());
        data.put("humidCal1", calibrationData.getHumidCalibration1());
        data.put("humidCal2", calibrationData.getHumidCalibration2());

        Call<ApiResponse> call = apiInterface.setCalibrationSettings(data);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Kalibrasyon ayarları değiştirilemedi - HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e(TAG, "Kalibrasyon ayarlama hatası", t);
                callback.onError("Kalibrasyon ayarları başarısız: " + t.getMessage());
            }
        });
    }

    /**
     * Kuluçka ayarlarını değiştir
     */
    public void setIncubationSettings(IncubationSettings incubationSettings, ApiCallback<ApiResponse> callback) {
        if (apiInterface == null) {
            callback.onError("API interface yapılandırılmamış");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("incubationType", incubationSettings.getIncubationType());
        data.put("isIncubationRunning", incubationSettings.isIncubationRunning());

        if (incubationSettings.isManualType()) {
            data.put("manualDevTemp", incubationSettings.getManualDevTemp());
            data.put("manualHatchTemp", incubationSettings.getManualHatchTemp());
            data.put("manualDevHumid", incubationSettings.getManualDevHumid());
            data.put("manualHatchHumid", incubationSettings.getManualHatchHumid());
            data.put("manualDevDays", incubationSettings.getManualDevDays());
            data.put("manualHatchDays", incubationSettings.getManualHatchDays());
        }

        Call<ApiResponse> call = apiInterface.setIncubationSettings(data);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Kuluçka ayarları değiştirilemedi - HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e(TAG, "Kuluçka ayarlama hatası", t);
                callback.onError("Kuluçka ayarları başarısız: " + t.getMessage());
            }
        });
    }

    /**
     * Bağlantıyı test et
     */
    public void testConnection(ApiCallback<Boolean> callback) {
        getSystemStatus(new ApiCallback<SystemStatus>() {
            @Override
            public void onSuccess(SystemStatus data) {
                callback.onSuccess(true);
            }

            @Override
            public void onError(String error) {
                callback.onSuccess(false);
            }
        });
    }

    /**
     * API callback interface
     */
    public interface ApiCallback<T> {
        void onSuccess(T data);
        void onError(String error);
    }

    /**
     * Retrofit API Interface - ESP32 endpoint'lerini tanımlar
     */
    private interface KuluckaApiInterface {

        @GET(Constants.ENDPOINT_STATUS)
        Call<SystemStatus> getSystemStatus();

        @GET(Constants.ENDPOINT_WIFI_NETWORKS)
        Call<WiFiNetworksResponse> getWiFiNetworks();

        @POST(Constants.ENDPOINT_WIFI_CONNECT)
        Call<ApiResponse> connectToWiFi(@Body Map<String, String> credentials);

        @POST(Constants.ENDPOINT_WIFI_AP)
        Call<ApiResponse> switchToAPMode();

        @POST(Constants.ENDPOINT_TEMPERATURE)
        Call<ApiResponse> setTemperature(@Body Map<String, Double> data);

        @POST(Constants.ENDPOINT_HUMIDITY)
        Call<ApiResponse> setHumidity(@Body Map<String, Double> data);

        @POST(Constants.ENDPOINT_PID)
        Call<ApiResponse> setPIDSettings(@Body Map<String, Object> data);

        @POST(Constants.ENDPOINT_MOTOR)
        Call<ApiResponse> setMotorSettings(@Body Map<String, Integer> data);

        @POST(Constants.ENDPOINT_ALARM)
        Call<ApiResponse> setAlarmSettings(@Body Map<String, Object> data);

        @POST(Constants.ENDPOINT_CALIBRATION)
        Call<ApiResponse> setCalibrationSettings(@Body Map<String, Double> data);

        @POST(Constants.ENDPOINT_INCUBATION)
        Call<ApiResponse> setIncubationSettings(@Body Map<String, Object> data);
    }
}