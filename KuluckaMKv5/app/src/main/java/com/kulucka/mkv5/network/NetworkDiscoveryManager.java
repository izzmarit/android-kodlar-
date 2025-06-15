package com.kulucka.mkv5.network;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;

import com.kulucka.mkv5.utils.SharedPreferencesManager;
import com.kulucka.mkv5.utils.Constants;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NetworkDiscoveryManager {
    private static final String TAG = "NetworkDiscovery";
    private static final String SERVICE_TYPE = "_http._tcp.";
    private static final String SERVICE_NAME = "kulucka";
    private static final int DISCOVERY_PORT = 12345;
    private static final int TIMEOUT_MS = 5000;

    private boolean isDiscoveryActive = false;

    private Context context;
    private SharedPreferencesManager prefsManager;
    private NsdManager nsdManager;
    private NsdManager.DiscoveryListener discoveryListener;
    private ExecutorService executorService;
    private DiscoveryCallback callback;

    public interface DiscoveryCallback {
        void onDeviceFound(String ipAddress, int port);
        void onDiscoveryComplete();
        void onError(String error);
    }

    public NetworkDiscoveryManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefsManager = SharedPreferencesManager.getInstance(context);
        this.nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        this.executorService = Executors.newCachedThreadPool();
    }

    public void startDiscovery(DiscoveryCallback callback) {
        if (isDiscoveryActive) {
            Log.w(TAG, "Discovery already active, skipping...");
            return;
        }

        this.callback = callback;
        this.isDiscoveryActive = true;

        // Thread pool oluştur
        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newCachedThreadPool();
        }

        // Bağlantı moduna göre strateji belirle
        int connectionMode = prefsManager.getConnectionMode();

        if (connectionMode == Constants.MODE_AP) {
            // AP modunda önce varsayılan IP'yi dene
            tryDirectConnection(Constants.DEFAULT_AP_IP);
        } else {
            // Station modunda kayıtlı IP varsa önce onu dene
            String savedIP = prefsManager.getDeviceIp();
            if (!savedIP.equals(Constants.DEFAULT_AP_IP) && !savedIP.equals("0.0.0.0")) {
                tryDirectConnection(savedIP);
            }

            // Paralel olarak diğer yöntemleri de dene
            executorService.execute(this::tryMdnsConnection);
            executorService.execute(this::scanCurrentSubnet);
        }

        // Her durumda UDP broadcast dene
        executorService.execute(this::startUdpBroadcast);

        // Yaygın IP'leri son çare olarak dene
        executorService.execute(this::tryCommonIPs);
    }

    private void tryDirectConnection(String ip) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        String url = "http://" + ip + "/api/ping";
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "Direct IP connection failed: " + ip);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Direct IP connection successful: " + ip);
                    if (callback != null) {
                        callback.onDeviceFound(ip, 80);
                    }
                }
                response.close();
            }
        });
    }

    // Yeni metod ekleyin - mevcut subnet'i tara:
    private void scanCurrentSubnet() {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);

            if (wifiManager != null && wifiManager.isWifiEnabled()) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null) {
                    int ip = wifiInfo.getIpAddress();
                    if (ip != 0) {
                        String ipString = String.format("%d.%d.%d.",
                                (ip & 0xff),
                                (ip >> 8 & 0xff),
                                (ip >> 16 & 0xff));

                        Log.d(TAG, "Scanning subnet: " + ipString + "*");

                        // Öncelikli IP aralıklarını tara (ESP32'lerin genelde aldığı IP'ler)
                        int[] priorityRanges = {1, 2, 100, 101, 102, 103, 104, 105}; // Router'lar genelde bu IP'leri verir

                        // Önce öncelikli IP'leri tara
                        for (int i : priorityRanges) {
                            if (!isDiscoveryActive) break;
                            final String testIP = ipString + i;
                            executorService.execute(() -> testDeviceIP(testIP));
                            Thread.sleep(50);
                        }

                        // Sonra geri kalanları tara
                        for (int i = 1; i < 255; i++) {
                            if (!isDiscoveryActive) break;

                            // Öncelikli IP'leri atla
                            boolean skip = false;
                            for (int priority : priorityRanges) {
                                if (i == priority) {
                                    skip = true;
                                    break;
                                }
                            }
                            if (skip) continue;

                            final String testIP = ipString + i;
                            executorService.execute(() -> testDeviceIP(testIP));
                            Thread.sleep(50); // Rate limiting
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Subnet scan error: " + e.getMessage());
        }
    }

    // Yeni metod ekleyin:
    private void testDeviceIP(String ip) {
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(1, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(1, java.util.concurrent.TimeUnit.SECONDS)
                    .build();

            Request request = new Request.Builder()
                    .url("http://" + ip + "/api/discovery")
                    .build();

            Response response = client.newCall(request).execute();

            if (response.isSuccessful()) {
                String body = response.body().string();
                if (body.contains("KULUCKA_MK_v5")) {
                    Log.d(TAG, "Device found at: " + ip);
                    if (callback != null) {
                        callback.onDeviceFound(ip, 80);
                    }
                }
            }
            response.close();
        } catch (Exception e) {
            // Sessizce devam et
        }
    }

    private void tryMdnsConnection() {
        String[] mdnsAddresses = {
                "kulucka.local",
                "kulucka-mk.local"
        };

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(2, java.util.concurrent.TimeUnit.SECONDS) // 3'ten düşürüldü
                .readTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        for (String hostname : mdnsAddresses) {
            if (!isDiscoveryActive) break; // Discovery durdurulduysa çık

            try {
                // Önce ping endpoint'ini dene (daha hızlı)
                Request request = new Request.Builder()
                        .url("http://" + hostname + "/api/ping")
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    // mDNS çözümleme başarılı, IP adresini al
                    InetAddress address = InetAddress.getByName(hostname);
                    String ipAddress = address.getHostAddress();

                    Log.d(TAG, "mDNS ile cihaz bulundu: " + hostname + " -> " + ipAddress);

                    if (callback != null) {
                        callback.onDeviceFound(ipAddress, 80);
                    }

                    // Bulundu, diğerlerini denemeye gerek yok
                    stopDiscovery();
                    return;
                }

                response.close();
                Thread.sleep(500); // Bir sonraki deneme için kısa bekle

            } catch (Exception e) {
                Log.d(TAG, "mDNS bağlantı başarısız: " + hostname);
            }
        }
    }

    private void startUdpBroadcast() {
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true);
            socket.setSoTimeout(TIMEOUT_MS);

            String message = "KULUCKA_DISCOVERY";
            byte[] buffer = message.getBytes();

            // Subnet'teki tüm broadcast adreslerine gönder
            List<InetAddress> broadcastAddresses = getBroadcastAddresses();

            for (InetAddress broadcastAddr : broadcastAddresses) {
                try {
                    DatagramPacket packet = new DatagramPacket(
                            buffer, buffer.length, broadcastAddr, DISCOVERY_PORT);
                    socket.send(packet);
                    Log.d(TAG, "UDP broadcast gönderildi: " + broadcastAddr.getHostAddress());
                } catch (Exception e) {
                    Log.e(TAG, "UDP broadcast hatası: " + e.getMessage());
                }
            }

            // Yanıt dinle
            byte[] responseBuffer = new byte[1024];
            try {
                while (true) {
                    DatagramPacket responsePacket = new DatagramPacket(
                            responseBuffer, responseBuffer.length);
                    socket.receive(responsePacket);

                    String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
                    if (response.contains("KULUCKA_RESPONSE")) {
                        String deviceIP = responsePacket.getAddress().getHostAddress();
                        Log.d(TAG, "UDP ile cihaz bulundu: " + deviceIP);

                        if (callback != null) {
                            callback.onDeviceFound(deviceIP, 80);
                        }
                    }
                }
            } catch (SocketTimeoutException e) {
                Log.d(TAG, "UDP timeout");
            }

            socket.close();
        } catch (Exception e) {
            Log.e(TAG, "UDP broadcast hatası: " + e.getMessage());
        }
    }

    private void startNsdDiscovery() {
        if (nsdManager == null) return;

        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "NSD discovery başlatma başarısız: " + errorCode);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "NSD discovery durdurma başarısız: " + errorCode);
            }

            @Override
            public void onDiscoveryStarted(String serviceType) {
                Log.d(TAG, "NSD discovery başlatıldı");
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.d(TAG, "NSD discovery durduruldu");
            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                String serviceName = serviceInfo.getServiceName();
                if (serviceName.toLowerCase().contains("kulucka")) {
                    Log.d(TAG, "NSD ile servis bulundu: " + serviceName);
                    resolveService(serviceInfo);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "NSD servisi kayboldu: " + serviceInfo.getServiceName());
            }
        };

        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
        } catch (Exception e) {
            Log.e(TAG, "NSD discovery hatası: " + e.getMessage());
        }
    }

    private void resolveService(NsdServiceInfo serviceInfo) {
        NsdManager.ResolveListener resolveListener = new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "NSD resolve başarısız: " + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                String host = serviceInfo.getHost().getHostAddress();
                int port = serviceInfo.getPort();
                Log.d(TAG, "NSD ile cihaz çözümlendi: " + host + ":" + port);

                if (callback != null) {
                    callback.onDeviceFound(host, port);
                }
            }
        };

        try {
            nsdManager.resolveService(serviceInfo, resolveListener);
        } catch (Exception e) {
            Log.e(TAG, "NSD resolve hatası: " + e.getMessage());
        }
    }

    private void tryCommonIPs() {
        String[] commonIPs = {
                "192.168.4.1",    // AP mode default
                "192.168.1.1",    // Router default
                "192.168.0.1",    // Router default
                "10.0.0.1"        // Router default
        };

        // Aynı subnet'teki IP'leri de kontrol et
        List<String> subnetIPs = getSubnetIPs();

        List<String> allIPs = new ArrayList<>();
        Collections.addAll(allIPs, commonIPs);
        allIPs.addAll(subnetIPs);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        for (String ip : allIPs) {
            try {
                String url = "http://" + ip + "/api/discovery";
                Request request = new Request.Builder().url(url).build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        // Sessizce devam et
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            String responseBody = response.body().string();
                            if (responseBody.contains("KULUCKA_MK_v5")) {
                                Log.d(TAG, "HTTP ile cihaz bulundu: " + ip);
                                if (callback != null) {
                                    callback.onDeviceFound(ip, 80);
                                }
                            }
                        }
                        response.close();
                    }
                });

                Thread.sleep(100); // Rate limiting
            } catch (Exception e) {
                Log.e(TAG, "IP kontrol hatası " + ip + ": " + e.getMessage());
            }
        }
    }

    private List<InetAddress> getBroadcastAddresses() {
        List<InetAddress> broadcastList = new ArrayList<>();

        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : interfaces) {
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                List<InetAddress> addresses = Collections.list(networkInterface.getInetAddresses());
                for (InetAddress address : addresses) {
                    if (address.isSiteLocalAddress()) {
                        String hostAddress = address.getHostAddress();
                        String[] parts = hostAddress.split("\\.");
                        if (parts.length == 4) {
                            String broadcastAddress = parts[0] + "." + parts[1] + "." + parts[2] + ".255";
                            broadcastList.add(InetAddress.getByName(broadcastAddress));
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Broadcast adresleri alınamadı: " + e.getMessage());
        }

        return broadcastList;
    }

    private List<String> getSubnetIPs() {
        List<String> subnetIPs = new ArrayList<>();

        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : interfaces) {
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                List<InetAddress> addresses = Collections.list(networkInterface.getInetAddresses());
                for (InetAddress address : addresses) {
                    if (address.isSiteLocalAddress()) {
                        String hostAddress = address.getHostAddress();
                        String[] parts = hostAddress.split("\\.");
                        if (parts.length == 4) {
                            String subnet = parts[0] + "." + parts[1] + "." + parts[2] + ".";
                            // Subnet'teki muhtemel IP'leri ekle
                            for (int i = 1; i < 255; i++) {
                                if (i != Integer.parseInt(parts[3])) { // Kendi IP'mizi atla
                                    subnetIPs.add(subnet + i);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Subnet IP'leri alınamadı: " + e.getMessage());
        }

        return subnetIPs;
    }

    public void stopDiscovery() {
        isDiscoveryActive = false;

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
            try {
                if (!executorService.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
                    Log.w(TAG, "ExecutorService did not terminate");
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }
    }
}