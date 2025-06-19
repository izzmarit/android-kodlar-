package com.kulucka.mkv5.network;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

public class NetworkDiscoveryManager {
    private static final String TAG = "NetworkDiscoveryManager";
    private static final int DISCOVERY_PORT = 8266;
    private static final String DISCOVERY_REQUEST = "KULUCKA_DISCOVERY";
    private static final String DISCOVERY_RESPONSE_PREFIX = "KULUCKA_DEVICE:";
    private static final int TIMEOUT_MS = 5000;

    private Context context;
    private DiscoveryTask discoveryTask;
    private DiscoveryCallback callback;

    public interface DiscoveryCallback {
        void onDeviceFound(String ipAddress, int port);
        void onDiscoveryComplete();
        void onError(String error);
    }

    public NetworkDiscoveryManager(Context context) {
        this.context = context;
    }

    public void startDiscovery(DiscoveryCallback callback) {
        this.callback = callback;

        if (discoveryTask != null && !discoveryTask.isCancelled()) {
            discoveryTask.cancel(true);
        }

        discoveryTask = new DiscoveryTask();
        discoveryTask.execute();
    }

    public void stopDiscovery() {
        if (discoveryTask != null && !discoveryTask.isCancelled()) {
            discoveryTask.cancel(true);
            discoveryTask = null;
        }
    }

    private class DiscoveryTask extends AsyncTask<Void, Void, String> {
        private DatagramSocket socket;
        private boolean deviceFound = false;

        @Override
        protected String doInBackground(Void... voids) {
            try {
                // UDP broadcast socket oluştur
                socket = new DatagramSocket();
                socket.setBroadcast(true);
                socket.setSoTimeout(TIMEOUT_MS);

                // Broadcast mesajını hazırla
                byte[] sendData = DISCOVERY_REQUEST.getBytes();

                // Farklı broadcast adreslerini dene
                tryBroadcast(sendData, "255.255.255.255");
                tryBroadcast(sendData, "192.168.4.255");  // AP modu için
                tryBroadcast(sendData, "192.168.1.255");  // Yaygın ev ağları
                tryBroadcast(sendData, "192.168.0.255");  // Yaygın ev ağları

                // Yanıt bekle
                byte[] recvBuf = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);

                long startTime = System.currentTimeMillis();
                while (!isCancelled() && (System.currentTimeMillis() - startTime) < TIMEOUT_MS) {
                    try {
                        socket.receive(receivePacket);
                        String response = new String(receivePacket.getData(), 0, receivePacket.getLength());

                        Log.d(TAG, "Yanıt alındı: " + response + " from " + receivePacket.getAddress().getHostAddress());

                        if (response.startsWith(DISCOVERY_RESPONSE_PREFIX)) {
                            String deviceInfo = response.substring(DISCOVERY_RESPONSE_PREFIX.length());
                            String[] parts = deviceInfo.split(":");

                            if (parts.length >= 2) {
                                String ip = receivePacket.getAddress().getHostAddress();
                                int port = Integer.parseInt(parts[1]);

                                deviceFound = true;

                                // UI thread'de callback'i çağır
                                publishProgress();

                                return ip + ":" + port;
                            }
                        }
                    } catch (Exception e) {
                        // Timeout veya diğer hatalar, devam et
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Discovery hatası: " + e.getMessage());
                return "error:" + e.getMessage();
            } finally {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            }

            return null;
        }

        private void tryBroadcast(byte[] data, String broadcastAddress) {
            try {
                InetAddress address = InetAddress.getByName(broadcastAddress);
                DatagramPacket packet = new DatagramPacket(data, data.length, address, DISCOVERY_PORT);
                socket.send(packet);
                Log.d(TAG, "Broadcast gönderildi: " + broadcastAddress);
            } catch (Exception e) {
                Log.e(TAG, "Broadcast hatası (" + broadcastAddress + "): " + e.getMessage());
            }
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            // Cihaz bulunduğunda çağrılır
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                if (result.startsWith("error:")) {
                    if (callback != null) {
                        callback.onError(result.substring(6));
                    }
                } else {
                    String[] parts = result.split(":");
                    if (parts.length >= 2 && callback != null) {
                        callback.onDeviceFound(parts[0], Integer.parseInt(parts[1]));
                    }
                }
            }

            if (callback != null) {
                callback.onDiscoveryComplete();
            }
        }

        @Override
        protected void onCancelled() {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

            if (callback != null) {
                callback.onDiscoveryComplete();
            }
        }
    }
}