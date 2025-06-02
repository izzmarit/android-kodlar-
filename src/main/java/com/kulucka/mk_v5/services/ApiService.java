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
    private String baseUrl = "http://192.168.1.100"; // Default IP

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

    // Test connection
    public void testConnection(ParameterCallback callback) {
        String url = baseUrl + "/status";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    Log.d(TAG, "Connection test successful");
                    if (callback != null) callback.onSuccess();
                },
                error -> {
                    Log.e(TAG, "Connection test failed: " + error.getMessage());
                    if (callback != null) callback.onError(getVolleyErrorMessage(error));
                }
        );

        addToRequestQueue(request);
    }

    // Get sensor data
    public void getSensorData(DataCallback callback) {
        String url = baseUrl + "/sensor-data";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    Log.d(TAG, "Sensor data received: " + response.toString());
                    if (callback != null) callback.onSuccess(response);
                },
                error -> {
                    Log.e(TAG, "Failed to get sensor data: " + error.getMessage());
                    if (callback != null) callback.onError(getVolleyErrorMessage(error));
                }
        );

        addToRequestQueue(request);
    }

    // Get status
    public void getStatus(DataCallback callback) {
        String url = baseUrl + "/status";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    Log.d(TAG, "Status received: " + response.toString());
                    if (callback != null) callback.onSuccess(response);
                },
                error -> {
                    Log.e(TAG, "Failed to get status: " + error.getMessage());
                    if (callback != null) callback.onError(getVolleyErrorMessage(error));
                }
        );

        addToRequestQueue(request);
    }

    // Get PID settings - Eksik olan method
    public void getPidSettings(DataCallback callback) {
        String url = baseUrl + "/pid-settings";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    Log.d(TAG, "PID settings received: " + response.toString());
                    if (callback != null) callback.onSuccess(response);
                },
                error -> {
                    Log.e(TAG, "Failed to get PID settings: " + error.getMessage());
                    if (callback != null) callback.onError(getVolleyErrorMessage(error));
                }
        );

        addToRequestQueue(request);
    }

    // Generic setParameter method - Eksik olan method
    public void setParameter(String endpoint, String jsonData, ParameterCallback callback) {
        String url = baseUrl + "/" + endpoint;

        try {
            JSONObject jsonBody = new JSONObject(jsonData);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST, url, jsonBody,
                    response -> {
                        Log.d(TAG, "Parameter set successfully for endpoint: " + endpoint);
                        if (callback != null) callback.onSuccess();
                    },
                    error -> {
                        Log.e(TAG, "Failed to set parameter for endpoint " + endpoint + ": " + error.getMessage());
                        if (callback != null) callback.onError(getVolleyErrorMessage(error));
                    }
            );

            addToRequestQueue(request);
        } catch (JSONException e) {
            Log.e(TAG, "JSON parsing error: " + e.getMessage());
            if (callback != null) callback.onError("JSON parsing error: " + e.getMessage());
        }
    }

    // Set temperature target
    public void setTemperature(double temperature, ParameterCallback callback) {
        String url = baseUrl + "/set-temperature";

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("temperature", temperature);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST, url, jsonBody,
                    response -> {
                        Log.d(TAG, "Temperature set successfully");
                        if (callback != null) callback.onSuccess();
                    },
                    error -> {
                        Log.e(TAG, "Failed to set temperature: " + error.getMessage());
                        if (callback != null) callback.onError(getVolleyErrorMessage(error));
                    }
            );

            addToRequestQueue(request);
        } catch (JSONException e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
            if (callback != null) callback.onError("JSON error: " + e.getMessage());
        }
    }

    // Set humidity target
    public void setHumidity(double humidity, ParameterCallback callback) {
        String url = baseUrl + "/set-humidity";

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("humidity", humidity);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST, url, jsonBody,
                    response -> {
                        Log.d(TAG, "Humidity set successfully");
                        if (callback != null) callback.onSuccess();
                    },
                    error -> {
                        Log.e(TAG, "Failed to set humidity: " + error.getMessage());
                        if (callback != null) callback.onError(getVolleyErrorMessage(error));
                    }
            );

            addToRequestQueue(request);
        } catch (JSONException e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
            if (callback != null) callback.onError("JSON error: " + e.getMessage());
        }
    }

    // Set PID parameters
    public void setPidParameters(double kp, double ki, double kd, ParameterCallback callback) {
        String url = baseUrl + "/set-pid";

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("kp", kp);
            jsonBody.put("ki", ki);
            jsonBody.put("kd", kd);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST, url, jsonBody,
                    response -> {
                        Log.d(TAG, "PID parameters set successfully");
                        if (callback != null) callback.onSuccess();
                    },
                    error -> {
                        Log.e(TAG, "Failed to set PID parameters: " + error.getMessage());
                        if (callback != null) callback.onError(getVolleyErrorMessage(error));
                    }
            );

            addToRequestQueue(request);
        } catch (JSONException e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
            if (callback != null) callback.onError("JSON error: " + e.getMessage());
        }
    }

    // Start incubation program
    public void startIncubation(int totalDays, ParameterCallback callback) {
        String url = baseUrl + "/start-incubation";

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("totalDays", totalDays);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST, url, jsonBody,
                    response -> {
                        Log.d(TAG, "Incubation started successfully");
                        if (callback != null) callback.onSuccess();
                    },
                    error -> {
                        Log.e(TAG, "Failed to start incubation: " + error.getMessage());
                        if (callback != null) callback.onError(getVolleyErrorMessage(error));
                    }
            );

            addToRequestQueue(request);
        } catch (JSONException e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
            if (callback != null) callback.onError("JSON error: " + e.getMessage());
        }
    }

    // Stop incubation program
    public void stopIncubation(ParameterCallback callback) {
        String url = baseUrl + "/stop-incubation";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, url, null,
                response -> {
                    Log.d(TAG, "Incubation stopped successfully");
                    if (callback != null) callback.onSuccess();
                },
                error -> {
                    Log.e(TAG, "Failed to stop incubation: " + error.getMessage());
                    if (callback != null) callback.onError(getVolleyErrorMessage(error));
                }
        );

        addToRequestQueue(request);
    }

    // Enable/Disable PID
    public void setPidEnabled(boolean enabled, ParameterCallback callback) {
        String url = baseUrl + "/set-pid-enabled";

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("enabled", enabled);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST, url, jsonBody,
                    response -> {
                        Log.d(TAG, "PID enabled/disabled successfully");
                        if (callback != null) callback.onSuccess();
                    },
                    error -> {
                        Log.e(TAG, "Failed to set PID enabled: " + error.getMessage());
                        if (callback != null) callback.onError(getVolleyErrorMessage(error));
                    }
            );

            addToRequestQueue(request);
        } catch (JSONException e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
            if (callback != null) callback.onError("JSON error: " + e.getMessage());
        }
    }

    // Enable/Disable turning
    public void setTurningEnabled(boolean enabled, ParameterCallback callback) {
        String url = baseUrl + "/set-turning-enabled";

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("enabled", enabled);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST, url, jsonBody,
                    response -> {
                        Log.d(TAG, "Turning enabled/disabled successfully");
                        if (callback != null) callback.onSuccess();
                    },
                    error -> {
                        Log.e(TAG, "Failed to set turning enabled: " + error.getMessage());
                        if (callback != null) callback.onError(getVolleyErrorMessage(error));
                    }
            );

            addToRequestQueue(request);
        } catch (JSONException e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
            if (callback != null) callback.onError("JSON error: " + e.getMessage());
        }
    }

    // Set turning interval
    public void setTurningInterval(int hours, ParameterCallback callback) {
        String url = baseUrl + "/set-turning-interval";

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("interval", hours);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST, url, jsonBody,
                    response -> {
                        Log.d(TAG, "Turning interval set successfully");
                        if (callback != null) callback.onSuccess();
                    },
                    error -> {
                        Log.e(TAG, "Failed to set turning interval: " + error.getMessage());
                        if (callback != null) callback.onError(getVolleyErrorMessage(error));
                    }
            );

            addToRequestQueue(request);
        } catch (JSONException e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
            if (callback != null) callback.onError("JSON error: " + e.getMessage());
        }
    }

    // Dismiss alarm
    public void dismissAlarm(String alarmType, ParameterCallback callback) {
        String url = baseUrl + "/dismiss-alarm";

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("alarmType", alarmType);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST, url, jsonBody,
                    response -> {
                        Log.d(TAG, "Alarm dismissed successfully");
                        if (callback != null) callback.onSuccess();
                    },
                    error -> {
                        Log.e(TAG, "Failed to dismiss alarm: " + error.getMessage());
                        if (callback != null) callback.onError(getVolleyErrorMessage(error));
                    }
            );

            addToRequestQueue(request);
        } catch (JSONException e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
            if (callback != null) callback.onError("JSON error: " + e.getMessage());
        }
    }

    // Get WiFi networks
    public void getWifiNetworks(DataCallback callback) {
        String url = baseUrl + "/wifi-scan";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    Log.d(TAG, "WiFi networks received");
                    if (callback != null) callback.onSuccess(response);
                },
                error -> {
                    Log.e(TAG, "Failed to get WiFi networks: " + error.getMessage());
                    if (callback != null) callback.onError(getVolleyErrorMessage(error));
                }
        );

        addToRequestQueue(request);
    }

    // Connect to WiFi
    public void connectToWifi(String ssid, String password, ParameterCallback callback) {
        String url = baseUrl + "/wifi-connect";

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("ssid", ssid);
            jsonBody.put("password", password);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST, url, jsonBody,
                    response -> {
                        Log.d(TAG, "WiFi connection initiated");
                        if (callback != null) callback.onSuccess();
                    },
                    error -> {
                        Log.e(TAG, "Failed to connect to WiFi: " + error.getMessage());
                        if (callback != null) callback.onError(getVolleyErrorMessage(error));
                    }
            );

            addToRequestQueue(request);
        } catch (JSONException e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
            if (callback != null) callback.onError("JSON error: " + e.getMessage());
        }
    }

    // Get device info
    public void getDeviceInfo(StringCallback callback) {
        String url = baseUrl + "/info";

        StringRequest request = new StringRequest(
                Request.Method.GET, url,
                response -> {
                    Log.d(TAG, "Device info received");
                    if (callback != null) callback.onSuccess(response);
                },
                error -> {
                    Log.e(TAG, "Failed to get device info: " + error.getMessage());
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