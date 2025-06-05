package com.kulucka.mk_v5.services;

import android.content.Context;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ApiService {
    private static final String TAG = "ApiService";
    private static ApiService instance;
    private RequestQueue requestQueue;
    private Context context;
    private String baseUrl = "http://192.168.4.1"; // ESP32 AP modu varsayılan IP

    private ApiService() {
        // Private constructor
    }

    public static ApiService getInstance() {
        if (instance == null) {
            instance = new ApiService();
        }
        return instance;
    }

    public void initialize(Context context) {
        this.context = context.getApplicationContext();
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(this.context);
        }
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        Log.d(TAG, "Base URL set to: " + baseUrl);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    // Callback interfaces
    public interface DataCallback {
        void onSuccess(JSONObject data);
        void onError(String message);
    }

    public interface ParameterCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface StringCallback {
        void onSuccess(String response);
        void onError(String message);
    }

    // Test connection - ESP32 /api/status endpoint'ini kullan
    public void testConnection(ParameterCallback callback) {
        String url = baseUrl + "/api/status";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    Log.d(TAG, "ESP32 connection test successful");
                    if (callback != null) callback.onSuccess();
                },
                error -> {
                    Log.e(TAG, "ESP32 connection test failed: " + error.getMessage());
                    if (callback != null) callback.onError(getVolleyErrorMessage(error));
                }
        );

        request.setRetryPolicy(new DefaultRetryPolicy(
                5000, // 5 saniye timeout
                2,    // 2 deneme
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        addToRequestQueue(request);
    }

    // Get status - ESP32 /api/status endpoint'i
    public void getStatus(DataCallback callback) {
        String url = baseUrl + "/api/status";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    Log.d(TAG, "ESP32 status received: " + response.toString());
                    if (callback != null) callback.onSuccess(response);
                },
                error -> {
                    Log.e(TAG, "Failed to get ESP32 status: " + error.getMessage());
                    if (callback != null) callback.onError(getVolleyErrorMessage(error));
                }
        );

        addToRequestQueue(request);
    }

    // Get PID settings - ESP32'den PID ayarlarını al
    public void getPidSettings(DataCallback callback) {
        String url = baseUrl + "/api/status"; // Status endpoint'i PID bilgilerini de içeriyor

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    Log.d(TAG, "ESP32 PID settings received");
                    if (callback != null) callback.onSuccess(response);
                },
                error -> {
                    Log.e(TAG, "Failed to get ESP32 PID settings: " + error.getMessage());
                    if (callback != null) callback.onError(getVolleyErrorMessage(error));
                }
        );

        addToRequestQueue(request);
    }

    // Update PID settings - ESP32'ye PID ayarlarını gönder
    public void updatePidSettings(JSONObject pidParams, ParameterCallback callback) {
        String url = baseUrl + "/api/pid";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, url, pidParams,
                response -> {
                    Log.d(TAG, "ESP32 PID settings updated successfully");
                    if (callback != null) callback.onSuccess();
                },
                error -> {
                    Log.e(TAG, "Failed to update ESP32 PID settings: " + error.getMessage());
                    if (callback != null) callback.onError(getVolleyErrorMessage(error));
                }
        );

        addToRequestQueue(request);
    }

    // Start PID auto tune - Otomatik PID ayarlama başlat
    public void startPidAutoTune(ParameterCallback callback) {
        String url = baseUrl + "/api/pid";

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("autoTuning", true);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST, url, jsonBody,
                    response -> {
                        Log.d(TAG, "PID auto tuning started on ESP32");
                        if (callback != null) callback.onSuccess();
                    },
                    error -> {
                        Log.e(TAG, "Failed to start PID auto tuning on ESP32: " + error.getMessage());
                        if (callback != null) callback.onError(getVolleyErrorMessage(error));
                    }
            );

            request.setRetryPolicy(new DefaultRetryPolicy(
                    10000, // 10 saniye timeout
                    1,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            addToRequestQueue(request);
        } catch (JSONException e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
            if (callback != null) callback.onError("JSON error: " + e.getMessage());
        }
    }

    // Stop PID auto tune - Otomatik PID ayarlama durdur
    public void stopPidAutoTune(ParameterCallback callback) {
        String url = baseUrl + "/api/pid";

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("autoTuning", false);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST, url, jsonBody,
                    response -> {
                        Log.d(TAG, "PID auto tuning stopped on ESP32");
                        if (callback != null) callback.onSuccess();
                    },
                    error -> {
                        Log.e(TAG, "Failed to stop PID auto tuning on ESP32: " + error.getMessage());
                        if (callback != null) callback.onError(getVolleyErrorMessage(error));
                    }
            );

            addToRequestQueue(request);
        } catch (JSONException e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
            if (callback != null) callback.onError("JSON error: " + e.getMessage());
        }
    }

    // Test PID response - PID tepki testi
    public void testPidResponse(ParameterCallback callback) {
        String url = baseUrl + "/api/pid";

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("testResponse", true);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST, url, jsonBody,
                    response -> {
                        Log.d(TAG, "PID response test started on ESP32");
                        if (callback != null) callback.onSuccess();
                    },
                    error -> {
                        Log.e(TAG, "Failed to start PID response test on ESP32: " + error.getMessage());
                        if (callback != null) callback.onError(getVolleyErrorMessage(error));
                    }
            );

            addToRequestQueue(request);
        } catch (JSONException e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
            if (callback != null) callback.onError("JSON error: " + e.getMessage());
        }
    }

    // Get sensor data (alias for getStatus)
    public void getSensorData(DataCallback callback) {
        getStatus(callback);
    }

    // Get WiFi networks - ESP32 /api/wifi/networks endpoint'i
    public void getWifiNetworks(DataCallback callback) {
        String url = baseUrl + "/api/wifi/networks";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    Log.d(TAG, "ESP32 WiFi networks received");
                    if (callback != null) callback.onSuccess(response);
                },
                error -> {
                    Log.e(TAG, "Failed to get ESP32 WiFi networks: " + error.getMessage());
                    if (callback != null) callback.onError(getVolleyErrorMessage(error));
                }
        );

        request.setRetryPolicy(new DefaultRetryPolicy(
                10000, // 10 saniye timeout (tarama uzun sürebilir)
                1,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        addToRequestQueue(request);
    }

    // Connect to WiFi - ESP32 /api/wifi/connect endpoint'i
    public void connectToWifi(String ssid, String password, ParameterCallback callback) {
        String url = baseUrl + "/api/wifi/connect";

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("ssid", ssid);
            jsonBody.put("password", password);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST, url, jsonBody,
                    response -> {
                        Log.d(TAG, "ESP32 WiFi connection initiated");
                        if (callback != null) callback.onSuccess();
                    },
                    error -> {
                        Log.e(TAG, "Failed to connect ESP32 to WiFi: " + error.getMessage());
                        if (callback != null) callback.onError(getVolleyErrorMessage(error));
                    }
            );

            request.setRetryPolicy(new DefaultRetryPolicy(
                    15000, // 15 saniye timeout
                    1,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            addToRequestQueue(request);
        } catch (JSONException e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
            if (callback != null) callback.onError("JSON error: " + e.getMessage());
        }
    }

    // Switch to AP mode - ESP32 /api/wifi/ap endpoint'i
    public void switchToAPMode(ParameterCallback callback) {
        String url = baseUrl + "/api/wifi/ap";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, url, null,
                response -> {
                    Log.d(TAG, "ESP32 switched to AP mode");
                    if (callback != null) callback.onSuccess();
                },
                error -> {
                    Log.e(TAG, "Failed to switch ESP32 to AP mode: " + error.getMessage());
                    if (callback != null) callback.onError(getVolleyErrorMessage(error));
                }
        );

        addToRequestQueue(request);
    }

    // Set temperature - ESP32 /api/temperature endpoint'i
    public void setTemperature(double temperature, ParameterCallback callback) {
        String url = baseUrl + "/api/temperature";

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("targetTemp", temperature);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST, url, jsonBody,
                    response -> {
                        Log.d(TAG, "Temperature set successfully on ESP32");
                        if (callback != null) callback.onSuccess();
                    },
                    error -> {
                        Log.e(TAG, "Failed to set temperature on ESP32: " + error.getMessage());
                        if (callback != null) callback.onError(getVolleyErrorMessage(error));
                    }
            );

            addToRequestQueue(request);
        } catch (JSONException e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
            if (callback != null) callback.onError("JSON error: " + e.getMessage());
        }
    }

    // Set humidity - ESP32 /api/humidity endpoint'i
    public void setHumidity(double humidity, ParameterCallback callback) {
        String url = baseUrl + "/api/humidity";

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("targetHumid", humidity);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST, url, jsonBody,
                    response -> {
                        Log.d(TAG, "Humidity set successfully on ESP32");
                        if (callback != null) callback.onSuccess();
                    },
                    error -> {
                        Log.e(TAG, "Failed to set humidity on ESP32: " + error.getMessage());
                        if (callback != null) callback.onError(getVolleyErrorMessage(error));
                    }
            );

            addToRequestQueue(request);
        } catch (JSONException e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
            if (callback != null) callback.onError("JSON error: " + e.getMessage());
        }
    }

    // Set PID parameters - Sadece sıcaklık PID parametreleri için
    public void setPidParameters(double kp, double ki, double kd, ParameterCallback callback) {
        String url = baseUrl + "/api/pid";

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("kp", kp);  // ESP32'de tempKp olarak işlenecek
            jsonBody.put("ki", ki);  // ESP32'de tempKi olarak işlenecek
            jsonBody.put("kd", kd);  // ESP32'de tempKd olarak işlenecek

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST, url, jsonBody,
                    response -> {
                        Log.d(TAG, "Temperature PID parameters set successfully on ESP32");
                        if (callback != null) callback.onSuccess();
                    },
                    error -> {
                        Log.e(TAG, "Failed to set temperature PID parameters on ESP32: " + error.getMessage());
                        if (callback != null) callback.onError(getVolleyErrorMessage(error));
                    }
            );

            addToRequestQueue(request);
        } catch (JSONException e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
            if (callback != null) callback.onError("JSON error: " + e.getMessage());
        }
    }

    // Set PID mode - ESP32 /api/pid endpoint'i
    public void setPidMode(int mode, ParameterCallback callback) {
        String url = baseUrl + "/api/pid";

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("pidMode", mode);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST, url, jsonBody,
                    response -> {
                        Log.d(TAG, "PID mode set successfully on ESP32");
                        if (callback != null) callback.onSuccess();
                    },
                    error -> {
                        Log.e(TAG, "Failed to set PID mode on ESP32: " + error.getMessage());
                        if (callback != null) callback.onError(getVolleyErrorMessage(error));
                    }
            );

            addToRequestQueue(request);
        } catch (JSONException e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
            if (callback != null) callback.onError("JSON error: " + e.getMessage());
        }
    }

    // Motor settings - ESP32 /api/motor endpoint'i
    public void setMotorSettings(long waitTime, long runTime, ParameterCallback callback) {
        String url = baseUrl + "/api/motor";

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("waitTime", waitTime);
            jsonBody.put("runTime", runTime);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST, url, jsonBody,
                    response -> {
                        Log.d(TAG, "Motor settings set successfully on ESP32");
                        if (callback != null) callback.onSuccess();
                    },
                    error -> {
                        Log.e(TAG, "Failed to set motor settings on ESP32: " + error.getMessage());
                        if (callback != null) callback.onError(getVolleyErrorMessage(error));
                    }
            );

            addToRequestQueue(request);
        } catch (JSONException e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
            if (callback != null) callback.onError("JSON error: " + e.getMessage());
        }
    }

    // Alarm settings - ESP32 /api/alarm endpoint'i
    public void setAlarmSettings(boolean alarmEnabled, float tempLowAlarm, float tempHighAlarm,
                                 float humidLowAlarm, float humidHighAlarm, ParameterCallback callback) {
        String url = baseUrl + "/api/alarm";

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("alarmEnabled", alarmEnabled);
            jsonBody.put("tempLowAlarm", tempLowAlarm);
            jsonBody.put("tempHighAlarm", tempHighAlarm);
            jsonBody.put("humidLowAlarm", humidLowAlarm);
            jsonBody.put("humidHighAlarm", humidHighAlarm);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST, url, jsonBody,
                    response -> {
                        Log.d(TAG, "Alarm settings set successfully on ESP32");
                        if (callback != null) callback.onSuccess();
                    },
                    error -> {
                        Log.e(TAG, "Failed to set alarm settings on ESP32: " + error.getMessage());
                        if (callback != null) callback.onError(getVolleyErrorMessage(error));
                    }
            );

            addToRequestQueue(request);
        } catch (JSONException e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
            if (callback != null) callback.onError("JSON error: " + e.getMessage());
        }
    }

    // Calibration settings - ESP32 /api/calibration endpoint'i
    public void setCalibrationSettings(float tempCal1, float tempCal2,
                                       float humidCal1, float humidCal2, ParameterCallback callback) {
        String url = baseUrl + "/api/calibration";

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("tempCal1", tempCal1);
            jsonBody.put("tempCal2", tempCal2);
            jsonBody.put("humidCal1", humidCal1);
            jsonBody.put("humidCal2", humidCal2);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST, url, jsonBody,
                    response -> {
                        Log.d(TAG, "Calibration settings set successfully on ESP32");
                        if (callback != null) callback.onSuccess();
                    },
                    error -> {
                        Log.e(TAG, "Failed to set calibration settings on ESP32: " + error.getMessage());
                        if (callback != null) callback.onError(getVolleyErrorMessage(error));
                    }
            );

            addToRequestQueue(request);
        } catch (JSONException e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
            if (callback != null) callback.onError("JSON error: " + e.getMessage());
        }
    }

    // Incubation settings - ESP32 /api/incubation endpoint'i
    public void setIncubationSettings(int incubationType, boolean isRunning,
                                      float manualDevTemp, float manualHatchTemp,
                                      int manualDevHumid, int manualHatchHumid,
                                      int manualDevDays, int manualHatchDays, ParameterCallback callback) {
        String url = baseUrl + "/api/incubation";

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("incubationType", incubationType);
            jsonBody.put("isIncubationRunning", isRunning);
            jsonBody.put("manualDevTemp", manualDevTemp);
            jsonBody.put("manualHatchTemp", manualHatchTemp);
            jsonBody.put("manualDevHumid", manualDevHumid);
            jsonBody.put("manualHatchHumid", manualHatchHumid);
            jsonBody.put("manualDevDays", manualDevDays);
            jsonBody.put("manualHatchDays", manualHatchDays);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST, url, jsonBody,
                    response -> {
                        Log.d(TAG, "Incubation settings set successfully on ESP32");
                        if (callback != null) callback.onSuccess();
                    },
                    error -> {
                        Log.e(TAG, "Failed to set incubation settings on ESP32: " + error.getMessage());
                        if (callback != null) callback.onError(getVolleyErrorMessage(error));
                    }
            );

            addToRequestQueue(request);
        } catch (JSONException e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
            if (callback != null) callback.onError("JSON error: " + e.getMessage());
        }
    }

    // Generic parameter setting - Tüm endpoint'ler için
    public void setParameter(String endpoint, String jsonData, ParameterCallback callback) {
        String url = baseUrl + "/" + endpoint;

        try {
            JSONObject jsonBody = new JSONObject(jsonData);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST, url, jsonBody,
                    response -> {
                        Log.d(TAG, "Parameter set successfully for ESP32 endpoint: " + endpoint);
                        if (callback != null) callback.onSuccess();
                    },
                    error -> {
                        Log.e(TAG, "Failed to set parameter for ESP32 endpoint " + endpoint + ": " + error.getMessage());
                        if (callback != null) callback.onError(getVolleyErrorMessage(error));
                    }
            );

            addToRequestQueue(request);
        } catch (JSONException e) {
            Log.e(TAG, "JSON parsing error: " + e.getMessage());
            if (callback != null) callback.onError("JSON parsing error: " + e.getMessage());
        }
    }

    // Start incubation
    public void startIncubation(int totalDays, ParameterCallback callback) {
        try {
            JSONObject jsonData = new JSONObject();
            jsonData.put("isIncubationRunning", true);
            setParameter("api/incubation", jsonData.toString(), callback);
        } catch (JSONException e) {
            if (callback != null) callback.onError("Failed to start incubation: " + e.getMessage());
        }
    }

    // Stop incubation
    public void stopIncubation(ParameterCallback callback) {
        try {
            JSONObject jsonData = new JSONObject();
            jsonData.put("isIncubationRunning", false);
            setParameter("api/incubation", jsonData.toString(), callback);
        } catch (JSONException e) {
            if (callback != null) callback.onError("Failed to stop incubation: " + e.getMessage());
        }
    }

    // PID enable/disable
    public void setPidEnabled(boolean enabled, ParameterCallback callback) {
        setPidMode(enabled ? 1 : 0, callback); // 0: kapalı, 1: manuel, 2: otomatik
    }

    // Turning enable/disable
    public void setTurningEnabled(boolean enabled, ParameterCallback callback) {
        try {
            JSONObject jsonData = new JSONObject();
            jsonData.put("turningEnabled", enabled);
            setParameter("api/motor", jsonData.toString(), callback);
        } catch (JSONException e) {
            if (callback != null) callback.onError("JSON error: " + e.getMessage());
        }
    }

    // Turning interval
    public void setTurningInterval(int hours, ParameterCallback callback) {
        try {
            JSONObject jsonData = new JSONObject();
            jsonData.put("waitTime", hours * 60); // saatleri dakikaya çevir
            setParameter("api/motor", jsonData.toString(), callback);
        } catch (JSONException e) {
            if (callback != null) callback.onError("JSON error: " + e.getMessage());
        }
    }

    // Dismiss alarm
    public void dismissAlarm(String alarmType, ParameterCallback callback) {
        try {
            JSONObject jsonData = new JSONObject();
            jsonData.put("dismissAlarm", alarmType);
            setParameter("api/alarm", jsonData.toString(), callback);
        } catch (JSONException e) {
            if (callback != null) callback.onError("JSON error: " + e.getMessage());
        }
    }

    // Helper methods
    private void addToRequestQueue(Request<?> request) {
        if (requestQueue != null) {
            request.setTag(TAG);
            requestQueue.add(request);
        } else {
            Log.e(TAG, "RequestQueue is null. Make sure to call initialize() first.");
        }
    }

    private String getVolleyErrorMessage(VolleyError error) {
        if (error.networkResponse != null) {
            return "Network error: " + error.networkResponse.statusCode;
        } else if (error.getMessage() != null) {
            return error.getMessage();
        } else {
            return "Unknown network error";
        }
    }

    public void cancelAllRequests() {
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }
    }
}