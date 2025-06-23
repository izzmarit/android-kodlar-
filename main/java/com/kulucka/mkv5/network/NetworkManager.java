package com.kulucka.mkv5.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.kulucka.mkv5.models.DeviceStatus;
import com.kulucka.mkv5.utils.Constants;
import com.kulucka.mkv5.utils.SharedPreferencesManager;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.ResponseBody;

public class NetworkManager {
    private static final String TAG = "NetworkManager";
    private static NetworkManager instance;
    private final Context context;
    private ApiService apiService;
    private String baseUrl;
    private SharedPreferencesManager prefsManager;

    public void testMotor(int duration, Callback<ApiService.MotorTestResponse> callback) {
        Map<String, Object> params = new HashMap<>();
        if (duration > 0) {
            params.put("duration", duration);
        }
        apiService.testMotor(params).enqueue(callback);
    }

    // YENİ: Sistem kaydetme metodu
    public void saveSystem(Callback<ApiService.SystemSaveResponse> callback) {
        apiService.saveSystem().enqueue(callback);
    }

    // YENİ: WiFi credential kontrolü
    public void getWifiCredentials(Callback<ApiService.WifiCredentialsResponse> callback) {
        if (!isNetworkAvailable()) {
            callback.onFailure(null, new Exception("Ağ bağlantısı yok"));
            return;
        }
        apiService.getWifiCredentials().enqueue(callback);
    }

    // YENİ: Sistem sağlık kontrolü
    public void getSystemHealth(Callback<ApiService.SystemHealthResponse> callback) {
        if (!isNetworkAvailable()) {
            callback.onFailure(null, new Exception("Ağ bağlantısı yok"));
            return;
        }
        apiService.getSystemHealth().enqueue(callback);
    }

    private NetworkManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefsManager = SharedPreferencesManager.getInstance(context);
        updateBaseUrl();
    }

    public static synchronized NetworkManager getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkManager(context);
        }
        return instance;
    }

    private void updateBaseUrl() {
        String ip = prefsManager.getDeviceIp();
        int port = prefsManager.getDevicePort();
        baseUrl = "http://" + ip + ":" + port;
        apiService = RetrofitClient.getClient(baseUrl).create(ApiService.class);
        Log.d(TAG, "Base URL updated: " + baseUrl);
    }

    public void resetConnection() {
        RetrofitClient.resetClient();
        updateBaseUrl();
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }

    public void getDeviceStatus(Callback<DeviceStatus> callback) {
        if (!isNetworkAvailable()) {
            callback.onFailure(null, new Exception("Ağ bağlantısı yok"));
            return;
        }

        apiService.getStatus().enqueue(callback);
    }

    public void setTemperature(float temperature, Callback<okhttp3.ResponseBody> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("targetTemp", temperature);
        apiService.setTemperature(params).enqueue(callback);
    }

    public void setHumidity(float humidity, Callback<okhttp3.ResponseBody> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("targetHumid", humidity);
        apiService.setHumidity(params).enqueue(callback);
    }

    public void setPidMode(int mode, Callback<okhttp3.ResponseBody> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("pidMode", mode);
        apiService.setPidParameters(params).enqueue(callback);
    }

    public void setPidParameters(float kp, float ki, float kd, Callback<okhttp3.ResponseBody> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("kp", kp);  // ESP32 direkt kp, ki, kd bekliyor
        params.put("ki", ki);
        params.put("kd", kd);
        apiService.setPidParameters(params).enqueue(callback);
    }

    public void setMotorSettings(int waitTime, int runTime, Callback<okhttp3.ResponseBody> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("motorWaitTime", waitTime);  // ESP32 bu isimleri bekliyor
        params.put("motorRunTime", runTime);
        apiService.setMotorSettings(params).enqueue(callback);
    }

    public void setAlarmEnabled(boolean enabled, Callback<okhttp3.ResponseBody> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("alarmEnabled", enabled);
        apiService.setAlarmSettings(params).enqueue(callback);
    }

    public void setAlarmLimits(float tempLow, float tempHigh, float humidLow, float humidHigh,
                               Callback<okhttp3.ResponseBody> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("tempLowAlarm", tempLow);
        params.put("tempHighAlarm", tempHigh);
        params.put("humidLowAlarm", humidLow);
        params.put("humidHighAlarm", humidHigh);
        apiService.setAlarmSettings(params).enqueue(callback);
    }

    public void setCalibration(int sensor, float tempCal, float humidCal,
                               Callback<okhttp3.ResponseBody> callback) {
        Map<String, Object> params = new HashMap<>();
        if (sensor == 1) {
            params.put("tempCalibration1", tempCal);
            params.put("humidCalibration1", humidCal);
        } else {
            params.put("tempCalibration2", tempCal);
            params.put("humidCalibration2", humidCal);
        }
        apiService.setCalibration(params).enqueue(callback);
    }

    public void setIncubationType(int type, Callback<okhttp3.ResponseBody> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("incubationType", type);
        apiService.setIncubationSettings(params).enqueue(callback);
    }

    public void startIncubation(Callback<okhttp3.ResponseBody> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("isIncubationRunning", true);
        apiService.setIncubationSettings(params).enqueue(callback);
    }

    public void stopIncubation(Callback<okhttp3.ResponseBody> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("isIncubationRunning", false);
        apiService.setIncubationSettings(params).enqueue(callback);
    }

    public void setManualIncubationParams(float devTemp, float hatchTemp, int devHumid,
                                          int hatchHumid, int devDays, int hatchDays,
                                          Callback<okhttp3.ResponseBody> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("manualDevTemp", devTemp);
        params.put("manualHatchTemp", hatchTemp);
        params.put("manualDevHumid", devHumid);
        params.put("manualHatchHumid", hatchHumid);
        params.put("manualDevDays", devDays);
        params.put("manualHatchDays", hatchDays);
        apiService.setIncubationSettings(params).enqueue(callback);
    }

    public void getWifiNetworks(Callback<ApiService.WifiNetworksResponse> callback) {
        if (!isNetworkAvailable()) {
            callback.onFailure(null, new Exception("Ağ bağlantısı yok"));
            return;
        }

        // Timeout'u artır çünkü WiFi taraması zaman alır
        Call<ApiService.WifiNetworksResponse> call = apiService.getWifiNetworks();
        call.enqueue(new Callback<ApiService.WifiNetworksResponse>() {
            @Override
            public void onResponse(Call<ApiService.WifiNetworksResponse> call, Response<ApiService.WifiNetworksResponse> response) {
                Log.d(TAG, "WiFi networks response code: " + response.code());
                if (response.body() != null) {
                    Log.d(TAG, "Response body not null");
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<ApiService.WifiNetworksResponse> call, Throwable t) {
                Log.e(TAG, "WiFi networks request failed", t);
                callback.onFailure(call, t);
            }
        });
    }

    public void connectToWifi(String ssid, String password, Callback<okhttp3.ResponseBody> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("ssid", ssid);
        params.put("password", password);
        apiService.connectToWifi(params).enqueue(callback);
    }

    public void switchToAPMode(Callback<okhttp3.ResponseBody> callback) {
        apiService.switchToAPMode().enqueue(callback);
    }

    public void getWifiStatus(Callback<ApiService.WifiStatusResponse> callback) {
        if (!isNetworkAvailable()) {
            callback.onFailure(null, new Exception("Ağ bağlantısı yok"));
            return;
        }

        apiService.getWifiStatus().enqueue(callback);
    }

    public void saveWifiSettings(Callback<okhttp3.ResponseBody> callback) {
        apiService.saveWifiSettings().enqueue(callback);
    }

    // Ping metodu
    public void ping(Callback<okhttp3.ResponseBody> callback) {
        if (!isNetworkAvailable()) {
            callback.onFailure(null, new Exception("Ağ bağlantısı yok"));
            return;
        }

        apiService.ping().enqueue(callback);
    }

    // Discovery info metodu
    public void getDiscoveryInfo(Callback<ApiService.DiscoveryResponse> callback) {
        if (!isNetworkAvailable()) {
            callback.onFailure(null, new Exception("Ağ bağlantısı yok"));
            return;
        }

        apiService.getDiscoveryInfo().enqueue(callback);
    }

    // YENİ: WiFi mod değişimi metodu
    public void changeWifiMode(Map<String, Object> params, Callback<ApiService.WifiModeChangeResponse> callback) {
        if (!isNetworkAvailable()) {
            callback.onFailure(null, new Exception("Ağ bağlantısı yok"));
            return;
        }

        Call<ApiService.WifiModeChangeResponse> call = apiService.changeWifiMode(params);
        call.enqueue(new Callback<ApiService.WifiModeChangeResponse>() {
            @Override
            public void onResponse(Call<ApiService.WifiModeChangeResponse> call,
                                   Response<ApiService.WifiModeChangeResponse> response) {
                Log.d(TAG, "WiFi mode change response code: " + response.code());
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<ApiService.WifiModeChangeResponse> call, Throwable t) {
                Log.e(TAG, "WiFi mode change request failed", t);
                callback.onFailure(call, t);
            }
        });
    }

    // YENİ: Sistem doğrulama metodu
    public void getSystemVerification(Callback<ApiService.SystemVerificationResponse> callback) {
        if (!isNetworkAvailable()) {
            callback.onFailure(null, new Exception("Ağ bağlantısı yok"));
            return;
        }

        Call<ApiService.SystemVerificationResponse> call = apiService.getSystemVerification();
        call.enqueue(new Callback<ApiService.SystemVerificationResponse>() {
            @Override
            public void onResponse(Call<ApiService.SystemVerificationResponse> call,
                                   Response<ApiService.SystemVerificationResponse> response) {
                Log.d(TAG, "System verification response code: " + response.code());
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<ApiService.SystemVerificationResponse> call, Throwable t) {
                Log.e(TAG, "System verification request failed", t);
                callback.onFailure(call, t);
            }
        });
    }

    // YENİ: Toplu parametre güncelleme metodu
    public void updateMultipleParameters(Map<String, Object> parameters,
                                         Callback<ResponseBody> callback) {
        // Parametrelerin türüne göre uygun endpoint'i belirle
        if (parameters.containsKey("targetTemp") || parameters.containsKey("targetHumid")) {
            if (parameters.containsKey("targetTemp")) {
                apiService.setTemperature(parameters).enqueue(callback);
            } else {
                apiService.setHumidity(parameters).enqueue(callback);
            }
        } else if (parameters.containsKey("pidKp") || parameters.containsKey("pidKi") ||
                parameters.containsKey("pidKd") || parameters.containsKey("pidMode")) {
            apiService.setPidParameters(parameters).enqueue(callback);
        } else if (parameters.containsKey("alarmEnabled") ||
                parameters.containsKey("tempLowAlarm")) {
            apiService.setAlarmSettings(parameters).enqueue(callback);
        } else if (parameters.containsKey("incubationType") ||
                parameters.containsKey("isIncubationRunning")) {
            apiService.setIncubationSettings(parameters).enqueue(callback);
        }
    }

    // YENİ: Geliştirilmiş WiFi bağlantı metodu (detaylı response ile)
    public void connectToWifiAdvanced(String ssid, String password,
                                      Callback<ApiService.WifiModeChangeResponse> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("ssid", ssid);
        params.put("password", password);

        // Düzeltme: retrofitService yerine apiService kullanın
        Call<ApiService.WifiModeChangeResponse> call =
                apiService.connectToWifiWithDetails(params);
        call.enqueue(callback);
    }

    // YENİ: Sistem durumu toplu sorgulama
    public void getCompleteSystemStatus(
            Callback<ApiService.CompleteSystemStatusResponse> callback) {
        if (!isNetworkAvailable()) {
            callback.onFailure(null, new Exception("Ağ bağlantısı yok"));
            return;
        }
        apiService.getCompleteSystemStatus().enqueue(callback);
    }

    // YENİ: Motor durumu ve ayarlarını getir
    public void getMotorStatus(Callback<ApiService.MotorStatusResponse> callback) {
        if (!isNetworkAvailable()) {
            callback.onFailure(null, new Exception("Ağ bağlantısı yok"));
            return;
        }
        apiService.getMotorStatus().enqueue(callback);
    }

    // YENİ: PID durumu ve ayarlarını getir
    public void getPidStatus(Callback<ApiService.PidStatusResponse> callback) {
        if (!isNetworkAvailable()) {
            callback.onFailure(null, new Exception("Ağ bağlantısı yok"));
            return;
        }
        apiService.getPidStatus().enqueue(callback);
    }

    // YENİ: Alarm ayarlarını toplu güncelleme
    public void updateAlarmSettings(boolean enabled, float tempLow, float tempHigh,
                                    float humidLow, float humidHigh,
                                    Callback<ResponseBody> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("alarmEnabled", enabled);

        if (enabled) {
            params.put("tempLowAlarm", tempLow);
            params.put("tempHighAlarm", tempHigh);
            params.put("humidLowAlarm", humidLow);
            params.put("humidHighAlarm", humidHigh);
        }

        apiService.setAlarmSettings(params).enqueue(callback);
    }

    // YENİ: Kalibrasyon ayarlarını toplu güncelleme
    public void updateAllCalibrationSettings(float temp1Cal, float humid1Cal,
                                             float temp2Cal, float humid2Cal,
                                             Callback<ResponseBody> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("tempCalibration1", temp1Cal);
        params.put("humidCalibration1", humid1Cal);
        params.put("tempCalibration2", temp2Cal);
        params.put("humidCalibration2", humid2Cal);

        apiService.setCalibration(params).enqueue(callback);
    }

    // YENİ: WiFi mod geçiş durumu kontrolü
    public void checkWifiModeChangeStatus(Callback<ApiService.WifiModeChangeStatusResponse> callback) {
        apiService.getWifiModeChangeStatus().enqueue(callback);
    }

    // YENİ: Sistem yeniden başlatma
    public void restartSystem(Callback<ResponseBody> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("action", "restart");
        apiService.systemAction(params).enqueue(callback);
    }

    // YENİ: Fabrika ayarlarına sıfırlama
    public void resetToFactoryDefaults(Callback<ResponseBody> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("action", "factory_reset");
        apiService.systemAction(params).enqueue(callback);
    }

    // YENİ: Ağ bağlantısı test etme
    public void testNetworkConnection(String targetIP, Callback<ApiService.NetworkTestResponse> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("targetIP", targetIP);
        apiService.testNetworkConnection(params).enqueue(callback);
    }

    // YENİ: Hata loglarını getir
    public void getSystemLogs(Callback<ApiService.SystemLogsResponse> callback) {
        apiService.getSystemLogs().enqueue(callback);
    }

    // Geliştirilmiş timeout ayarları için özel metot
    private void setCustomTimeout(String endpoint) {
        // RetrofitClient'ta endpoint'e özel timeout ayarları
        switch (endpoint) {
            case "/api/wifi/mode":
            case "/api/wifi/connect":
                // 25 saniye timeout
                break;
            case "/api/wifi/networks":
                // 20 saniye timeout
                break;
            case "/api/system/verify":
                // 10 saniye timeout
                break;
            default:
                // Normal timeout
                break;
        }
    }

    // Manuel kuluçka parametrelerini toplu güncelleme
    public void setManualIncubationParametersAdvanced(
            float devTemp, float hatchTemp, int devHumid, int hatchHumid,
            int devDays, int hatchDays, Callback<ResponseBody> callback) {

        Map<String, Object> params = new HashMap<>();

        Map<String, Object> development = new HashMap<>();
        development.put("temperature", devTemp);
        development.put("humidity", devHumid);
        development.put("days", devDays);
        params.put("development", development);

        Map<String, Object> hatching = new HashMap<>();
        hatching.put("temperature", hatchTemp);
        hatching.put("humidity", hatchHumid);
        hatching.put("days", hatchDays);
        params.put("hatching", hatching);

        apiService.setManualIncubationParameters(params).enqueue(callback);
    }

    // Sensör detaylarını getir
    public void getSensorDetails(Callback<ApiService.SensorDetailsResponse> callback) {
        if (!isNetworkAvailable()) {
            callback.onFailure(null, new Exception("Ağ bağlantısı yok"));
            return;
        }
        apiService.getSensorDetails().enqueue(callback);
    }

    // RTC durumunu getir
    public void getRTCStatus(Callback<ApiService.RTCStatusResponse> callback) {
        if (!isNetworkAvailable()) {
            callback.onFailure(null, new Exception("Ağ bağlantısı yok"));
            return;
        }
        apiService.getRTCStatus().enqueue(callback);
    }

    // RTC saatini ayarla
    public void setRTCTime(int hour, int minute, Callback<ResponseBody> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("hour", hour);
        params.put("minute", minute);
        apiService.setRTCTime(params).enqueue(callback);
    }

    // RTC tarihini ayarla
    public void setRTCDate(int day, int month, int year, Callback<ResponseBody> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("day", day);
        params.put("month", month);
        params.put("year", year);
        apiService.setRTCDate(params).enqueue(callback);
    }
}