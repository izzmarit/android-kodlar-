package com.kulucka.mk_v5.services;

import android.content.Context;
import android.util.Log;

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

    // Test connection - ESP32 status endpoint'ini kullan
    public void testConnection(ParameterCallback callback) {
        String url = baseUrl + "/status";

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

        addToRequestQueue(request);
    }

    // Get sensor data ve status - ESP32 /status endpoint'ini kullan
    public void getSensorData(DataCallback callback) {
        String url = baseUrl + "/status";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    Log.d(TAG, "ESP32 sensor data received: " + response.toString());
                    if (callback != null) callback.onSuccess(response);
                },
                error -> {
                    Log.e(TAG, "Failed to get ESP32 sensor data: " + error.getMessage());
                    if (callback != null) callback.onError(getVolleyErrorMessage(error));
                }
        );

        addToRequestQueue(request);
    }

    // Get status (alias for getSensorData)
    public void getStatus(DataCallback callback) {
        getSensorData(callback);
    }

    // Get PID settings - ESP32'de ayrı endpoint yok, status'tan alınacak
    public void getPidSettings(DataCallback callback) {
        getSensorData(callback);
    }

    // Set temperature target - ESP32 /api/temperature endpoint'i
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

    // Set humidity target - ESP32 /api/humidity endpoint'i
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

    // Set PID parameters - ESP32 /api/pid endpoint'i
    public void setPidParameters(double kp, double ki, double kd, ParameterCallback callback) {
        String url = baseUrl + "/api/pid";

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("kp", kp);
            jsonBody.put("ki", ki);
            jsonBody.put("kd", kd);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST, url, jsonBody,
                    response -> {
                        Log.d(TAG, "PID parameters set successfully on ESP32");
                        if (callback != null) callback.onSuccess();
                    },
                    error -> {
                        Log.e(TAG, "Failed to set PID parameters on ESP32: " + error.getMessage());
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
    public void setAlarmSettings(float tempLowAlarm, float tempHighAlarm,
                                 float humidLowAlarm, float humidHighAlarm, ParameterCallback callback) {
        String url = baseUrl + "/api/alarm";

        try {
            JSONObject jsonBody = new JSONObject();
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

    // Start incubation program - basit versiyon
    public void startIncubation(int totalDays, ParameterCallback callback) {
        try {
            // Varsayılan manuel ayarlarla kuluçka başlat
            setIncubationSettings(3, true, 37.5f, 37.0f, 60, 70, totalDays-3, 3, callback);
        } catch (Exception e) {
            if (callback != null) callback.onError("Failed to start incubation: " + e.getMessage());
        }
    }

    // Stop incubation program
    public void stopIncubation(ParameterCallback callback) {
        try {
            // Kuluçkayı durdur
            setIncubationSettings(3, false, 37.5f, 37.0f, 60, 70, 18, 3, callback);
        } catch (Exception e) {
            if (callback != null) callback.onError("Failed to stop incubation: " + e.getMessage());
        }
    }

    // Generic parameter setting - ESP32'de desteklenen parametreler için
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

    // PID enable/disable (manuel olarak ESP32'ye gönderilecek)
    public void setPidEnabled(boolean enabled, ParameterCallback callback) {
        try {
            JSONObject jsonData = new JSONObject();
            jsonData.put("pidEnabled", enabled);
            setParameter("api/pid", jsonData.toString(), callback);
        } catch (JSONException e) {
            if (callback != null) callback.onError("JSON error: " + e.getMessage());
        }
    }

    // Turning enable/disable (motor ayarlarına dahil)
    public void setTurningEnabled(boolean enabled, ParameterCallback callback) {
        try {
            JSONObject jsonData = new JSONObject();
            jsonData.put("turningEnabled", enabled);
            setParameter("api/motor", jsonData.toString(), callback);
        } catch (JSONException e) {
            if (callback != null) callback.onError("JSON error: " + e.getMessage());
        }
    }

    // Turning interval setting
    public void setTurningInterval(int hours, ParameterCallback callback) {
        try {
            JSONObject jsonData = new JSONObject();
            jsonData.put("waitTime", hours * 60); // saatleri dakikaya çevir
            setParameter("api/motor", jsonData.toString(), callback);
        } catch (JSONException e) {
            if (callback != null) callback.onError("JSON error: " + e.getMessage());
        }
    }

    // Dismiss alarm (ESP32'de belki desteklenmez, ama deneyebiliriz)
    public void dismissAlarm(String alarmType, ParameterCallback callback) {
        try {
            JSONObject jsonData = new JSONObject();
            jsonData.put("dismissAlarm", alarmType);
            setParameter("api/alarm", jsonData.toString(), callback);
        } catch (JSONException e) {
            if (callback != null) callback.onError("JSON error: " + e.getMessage());
        }
    }

    // WiFi networks - ESP32 /api/wifi/networks endpoint'i
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

    // Get device info (basit string response)
    public void getDeviceInfo(StringCallback callback) {
        String url = baseUrl + "/status";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    Log.d(TAG, "ESP32 device info received");
                    if (callback != null) callback.onSuccess(response.toString());
                },
                error -> {
                    Log.e(TAG, "Failed to get ESP32 device info: " + error.getMessage());
                    if (callback != null) callback.onError(getVolleyErrorMessage(error));
                }
        );

        addToRequestQueue(request);
    }

    // Helper methods
    private void addToRequestQueue(Request<?> request) {
        if (requestQueue != null) {
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