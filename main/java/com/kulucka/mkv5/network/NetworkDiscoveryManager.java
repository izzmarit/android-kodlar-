package com.kulucka.mkv5.network;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class NetworkDiscoveryManager {
    private static final String TAG = "NetworkDiscoveryManager";
    private static final int DISCOVERY_PORT = 8266;
    private static final String DISCOVERY_REQUEST = "KULUCKA_DISCOVERY";
    private static final String DISCOVERY_RESPONSE_PREFIX = "KULUCKA_DEVICE:";
    private static final int TIMEOUT_MS = 3000; // 3 saniye timeout
    private static final int MAX_DISCOVERY_TIME = 15000; // 15 saniye maksimum

    private Context context;
    private DiscoveryTask discoveryTask;
    private DiscoveryCallback callback;
    private boolean isDiscoveryActive = false;

    public interface DiscoveryCallback {
        void onDeviceFound(String ipAddress, int port);
        void onDiscoveryComplete();
        void onError(String error);
    }

    public NetworkDiscoveryManager(Context context) {
        this.context = context;
    }

    public void startDiscovery(DiscoveryCallback callback) {
        if (isDiscoveryActive) {
            Log.w(TAG, "Discovery zaten aktif, yeni discovery başlatılmıyor");
            return;
        }

        this.callback = callback;
        this.isDiscoveryActive = true;

        if (discoveryTask != null && !discoveryTask.isCancelled()) {
            discoveryTask.cancel(true);
        }

        discoveryTask = new DiscoveryTask();
        discoveryTask.execute();
    }

    public void stopDiscovery() {
        isDiscoveryActive = false;
        if (discoveryTask != null && !discoveryTask.isCancelled()) {
            discoveryTask.cancel(true);
            discoveryTask = null;
        }
    }

    private class DiscoveryTask extends AsyncTask<Void, String, String> {
        private DatagramSocket socket;
        private boolean deviceFound = false;

        @Override
        protected String doInBackground(Void... voids) {
            try {
                // UDP broadcast socket oluştur
                socket = new DatagramSocket();
                socket.setBroadcast(true);
                socket.setSoTimeout(TIMEOUT_MS);
                socket.setReuseAddress(true);

                // Broadcast mesajını hazırla
                byte[] sendData = DISCOVERY_REQUEST.getBytes();

                // Farklı broadcast adreslerini dene
                String[] broadcastAddresses = {
                        "255.255.255.255",    // Genel broadcast
                        "192.168.4.255",      // AP modu için
                        "192.168.1.255",      // Yaygın ev ağları
                        "192.168.0.255",      // Yaygın ev ağları
                        "10.0.0.255",         // Bazı kurumsal ağlar
                        "172.16.0.255"        // Bazı özel ağlar
                };

                // Aynı anda tüm adreslere broadcast gönder
                for (String address : broadcastAddresses) {
                    if (isCancelled()) break;
                    tryBroadcast(sendData, address);
                }

                // Yanıt bekle
                byte[] recvBuf = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);

                long startTime = System.currentTimeMillis();
                while (!isCancelled() && !deviceFound &&
                        (System.currentTimeMillis() - startTime) < MAX_DISCOVERY_TIME) {
                    try {
                        socket.receive(receivePacket);
                        String response = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();

                        Log.d(TAG, "Yanıt alındı: " + response + " from " +
                                receivePacket.getAddress().getHostAddress());

                        if (response.startsWith(DISCOVERY_RESPONSE_PREFIX)) {
                            String deviceInfo = response.substring(DISCOVERY_RESPONSE_PREFIX.length());
                            String[] parts = deviceInfo.split(":");

                            if (parts.length >= 2) {
                                String ip = receivePacket.getAddress().getHostAddress();
                                int port = 80; // Varsayılan port

                                try {
                                    port = Integer.parseInt(parts[1].trim());
                                } catch (NumberFormatException e) {
                                    Log.w(TAG, "Port parse hatası, varsayılan kullanılıyor: " + e.getMessage());
                                }

                                deviceFound = true;
                                publishProgress(ip + ":" + port);

                                Log.d(TAG, "Cihaz bulundu: " + ip + ":" + port);
                                return ip + ":" + port;
                            }
                        }
                    } catch (SocketTimeoutException e) {
                        // Timeout normal, devam et

                        // Her 3 saniyede bir yeniden broadcast gönder
                        if ((System.currentTimeMillis() - startTime) % 3000 < 100) {
                            for (String address : broadcastAddresses) {
                                if (isCancelled()) break;
                                tryBroadcast(sendData, address);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Receive hatası: " + e.getMessage());
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Discovery hatası: " + e.getMessage());
                return "error:" + e.getMessage();
            } finally {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
                isDiscoveryActive = false;
            }

            return null;
        }

        private void tryBroadcast(byte[] data, String broadcastAddress) {
            try {
                InetAddress address = InetAddress.getByName(broadcastAddress);
                DatagramPacket packet = new DatagramPacket(data, data.length, address, DISCOVERY_PORT);

                // Birden fazla kez gönder güvenilirlik için
                for (int i = 0; i < 3; i++) {
                    if (!isCancelled()) {
                        socket.send(packet);
                        Thread.sleep(50); // 50ms bekle
                    }
                }

                Log.d(TAG, "Broadcast gönderildi: " + broadcastAddress);
            } catch (Exception e) {
                Log.w(TAG, "Broadcast hatası (" + broadcastAddress + "): " + e.getMessage());
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            // Cihaz bulunduğunda UI thread'e bildir
            if (values.length > 0 && callback != null) {
                String result = values[0];
                String[] parts = result.split(":");
                if (parts.length >= 2) {
                    try {
                        String ip = parts[0];
                        int port = Integer.parseInt(parts[1]);
                        callback.onDeviceFound(ip, port);
                    } catch (Exception e) {
                        Log.e(TAG, "Progress update hatası: " + e.getMessage());
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(String result) {
            isDiscoveryActive = false;

            if (result != null) {
                if (result.startsWith("error:")) {
                    if (callback != null) {
                        callback.onError(result.substring(6));
                    }
                } else if (!deviceFound) {
                    // Bu durumda zaten onProgressUpdate'te bildirildi
                    String[] parts = result.split(":");
                    if (parts.length >= 2 && callback != null) {
                        try {
                            callback.onDeviceFound(parts[0], Integer.parseInt(parts[1]));
                        } catch (Exception e) {
                            callback.onError("Port parse hatası: " + e.getMessage());
                        }
                    }
                }
            }

            if (callback != null) {
                callback.onDiscoveryComplete();
            }
        }

        @Override
        protected void onCancelled() {
            isDiscoveryActive = false;

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

            if (callback != null) {
                callback.onDiscoveryComplete();
            }
        }
    }

    // Utility metod - Mevcut ağ arayüzlerini kontrol et
    private String getLocalBroadcastAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : interfaces) {
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                for (InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
                    if (!address.isLoopbackAddress() && address.isSiteLocalAddress()) {
                        String hostAddress = address.getHostAddress();
                        if (hostAddress != null && hostAddress.indexOf(':') < 0) { // IPv4
                            // Broadcast adresini hesapla
                            String[] parts = hostAddress.split("\\.");
                            if (parts.length == 4) {
                                return parts[0] + "." + parts[1] + "." + parts[2] + ".255";
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Broadcast adresi alınamadı: " + e.getMessage());
        }
        return "255.255.255.255"; // Varsayılan
    }
}