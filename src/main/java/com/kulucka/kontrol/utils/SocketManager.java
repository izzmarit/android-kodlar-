package com.kulucka.kontrol.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Iterator;

public class SocketManager {
    private static final String TAG = "SocketManager";
    private static final String ESP_IP = "192.168.4.1"; // ESP32 AP IP
    private static final int ESP_PORT = 80; // ESP32 TCP port
    private static final int CONNECTION_TIMEOUT = 5000; // 5 saniye bağlantı zaman aşımı

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private boolean isConnected = false;
    private ExecutorService executor;
    private Handler mainHandler;
    private OnMessageReceivedListener messageListener;
    private OnConnectionStatusListener connectionListener;

    public interface OnMessageReceivedListener {
        void onMessageReceived(String message);
    }

    public interface OnConnectionStatusListener {
        void onConnected();
        void onDisconnected();
        void onConnectionFailed(String reason);
    }

    public SocketManager() {
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public void setOnMessageReceivedListener(OnMessageReceivedListener listener) {
        this.messageListener = listener;
    }

    public void setOnConnectionStatusListener(OnConnectionStatusListener listener) {
        this.connectionListener = listener;
    }

    public boolean isConnected() {
        return isConnected && socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void connect() {
        if (isConnected()) {
            return;
        }

        executor.execute(() -> {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(ESP_IP, ESP_PORT), CONNECTION_TIMEOUT);

                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream())), true);

                isConnected = true;

                if (connectionListener != null) {
                    mainHandler.post(() -> connectionListener.onConnected());
                }

                // Start listening for incoming messages
                startListening();

                // Send a ping to verify connection
                sendPing();

            } catch (Exception e) {
                Log.e(TAG, "Connection error: " + e.getMessage());
                isConnected = false;

                if (connectionListener != null) {
                    mainHandler.post(() -> connectionListener.onConnectionFailed(e.getMessage()));
                }
            }
        });
    }

    private void startListening() {
        executor.execute(() -> {
            while (isConnected()) {
                try {
                    String message = input.readLine();
                    if (message != null) {
                        if (messageListener != null) {
                            mainHandler.post(() -> messageListener.onMessageReceived(message));
                        }
                    } else {
                        // Socket kapandı
                        disconnect();
                        break;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error reading from socket: " + e.getMessage());
                    disconnect();
                    break;
                }
            }
        });
    }

    public void disconnect() {
        executor.execute(() -> {
            try {
                isConnected = false;

                if (input != null) {
                    input.close();
                }

                if (output != null) {
                    output.close();
                }

                if (socket != null) {
                    socket.close();
                }

                if (connectionListener != null) {
                    mainHandler.post(() -> connectionListener.onDisconnected());
                }

            } catch (Exception e) {
                Log.e(TAG, "Disconnect error: " + e.getMessage());
            }
        });
    }

    public void sendMessage(String message) {
        if (!isConnected()) {
            Log.e(TAG, "Cannot send message, not connected");
            return;
        }

        executor.execute(() -> {
            try {
                output.println(message);
            } catch (Exception e) {
                Log.e(TAG, "Error sending message: " + e.getMessage());
                disconnect();
            }
        });
    }

    public void sendJsonCommand(String command, JSONObject params) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("cmd", command);

            if (params != null) {
                // params içindeki tüm key-value çiftlerini ekle
                for (Iterator<String> it = params.keys(); it.hasNext();) {
                    String key = it.next();
                    jsonObject.put(key, params.get(key));
                }
            }

            sendMessage(jsonObject.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error creating JSON command: " + e.getMessage());
        }
    }

    // Ping göndererek bağlantı kontrolü
    public void sendPing() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("cmd", "ping");
            sendMessage(jsonObject.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error sending ping: " + e.getMessage());
        }
    }

    // Sensör verilerini talep etme
    public void requestSensorData() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("cmd", "get_sensor_data");
            sendMessage(jsonObject.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error requesting sensor data: " + e.getMessage());
        }
    }

    // Ayarları talep etme
    public void requestSettings() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("cmd", "get_settings");
            sendMessage(jsonObject.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error requesting settings: " + e.getMessage());
        }
    }

    // Profil verilerini talep etme
    public void requestProfileData() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("cmd", "get_profile");
            sendMessage(jsonObject.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error requesting profile data: " + e.getMessage());
        }
    }

    // Alarm verilerini talep etme
    public void requestAlarmData() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("cmd", "get_alarm_data");
            sendMessage(jsonObject.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error requesting alarm data: " + e.getMessage());
        }
    }

    // Tüm profilleri talep etme
    public void requestAllProfiles() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("cmd", "get_all_profiles");
            sendMessage(jsonObject.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error requesting all profiles: " + e.getMessage());
        }
    }

    // Sistem durumunu talep etme
    public void requestSystemStatus() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("cmd", "get_system_status");
            sendMessage(jsonObject.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error requesting system status: " + e.getMessage());
        }
    }

    // Hedef sıcaklık ve nem ayarları
    public void setTargets(float temperature, float humidity) {
        try {
            JSONObject params = new JSONObject();
            params.put("cmd", "set_targets");
            params.put("temp", temperature);
            params.put("humidity", humidity);
            sendMessage(params.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error setting targets: " + e.getMessage());
        }
    }

    // Motor kontrolü
    public void controlMotor(boolean state) {
        try {
            JSONObject params = new JSONObject();
            params.put("cmd", "control_motor");
            params.put("state", state);
            sendMessage(params.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error controlling motor: " + e.getMessage());
        }
    }

    // Kuluçka başlatma
    public void startIncubation(int profileType) {
        try {
            JSONObject params = new JSONObject();
            params.put("cmd", "start_incubation");
            params.put("profile_type", profileType);
            sendMessage(params.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error starting incubation: " + e.getMessage());
        }
    }

    // Kuluçka durdurma
    public void stopIncubation() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("cmd", "stop_incubation");
            sendMessage(jsonObject.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error stopping incubation: " + e.getMessage());
        }
    }
}