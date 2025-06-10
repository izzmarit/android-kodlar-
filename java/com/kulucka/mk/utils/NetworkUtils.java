package com.kulucka.mk.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Ağ bağlantısı ve IP tarama işlemleri için yardımcı sınıf
 */
public class NetworkUtils {

    private static final String TAG = "NetworkUtils";
    private static final int PING_TIMEOUT = 1000; // 1 saniye
    private static final int SOCKET_TIMEOUT = 2000; // 2 saniye

    /**
     * Ağ bağlantısının mevcut olup olmadığını kontrol eder
     */
    public static boolean isNetworkAvailable(Context context) {
        if (context == null) return false;

        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return false;

        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * WiFi bağlantısının aktif olup olmadığını kontrol eder
     */
    public static boolean isWiFiConnected(Context context) {
        if (context == null) return false;

        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return false;

        NetworkInfo wifiInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return wifiInfo != null && wifiInfo.isConnected();
    }

    /**
     * Mevcut WiFi ağının IP adresini alır
     */
    public static String getCurrentWiFiIP(Context context) {
        if (context == null) return null;

        WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);

        if (wifiManager == null) return null;

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null) return null;

        int ipAddress = wifiInfo.getIpAddress();

        // IP adresini string formatına çevir
        return String.format("%d.%d.%d.%d",
                (ipAddress & 0xff),
                (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff),
                (ipAddress >> 24 & 0xff));
    }

    /**
     * Mevcut ağın subnet'ini belirler
     */
    public static String getCurrentSubnet(Context context) {
        String currentIP = getCurrentWiFiIP(context);
        if (currentIP == null) return null;

        String[] parts = currentIP.split("\\.");
        if (parts.length < 3) return null;

        return parts[0] + "." + parts[1] + "." + parts[2];
    }

    /**
     * Belirtilen IP adresinin erişilebilir olup olmadığını kontrol eder
     */
    public static boolean isHostReachable(String ipAddress, int port) {
        if (ipAddress == null || ipAddress.isEmpty()) return false;

        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ipAddress, port), SOCKET_TIMEOUT);
            socket.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * ESP32 cihazını bulmak için IP aralığını tarar
     */
    public static void scanForESP32(Context context, ScanCallback callback) {
        if (!isWiFiConnected(context)) {
            callback.onScanCompleted(new ArrayList<>());
            return;
        }

        String subnet = getCurrentSubnet(context);
        if (subnet == null) {
            callback.onScanCompleted(new ArrayList<>());
            return;
        }

        Log.i(TAG, "ESP32 taraması başlatıldı - Subnet: " + subnet);

        ExecutorService executor = Executors.newFixedThreadPool(50);
        List<Future<String>> futures = new ArrayList<>();

        // 1-254 aralığında IP'leri tara
        for (int i = 1; i <= 254; i++) {
            final String ipToScan = subnet + "." + i;

            Future<String> future = executor.submit(() -> {
                if (isHostReachable(ipToScan, Constants.ESP32_PORT)) {
                    Log.d(TAG, "Erişilebilir IP bulundu: " + ipToScan);
                    return ipToScan;
                }
                return null;
            });

            futures.add(future);
        }

        // Sonuçları topla
        executor.submit(() -> {
            List<String> reachableIPs = new ArrayList<>();

            for (Future<String> future : futures) {
                try {
                    String result = future.get(3, TimeUnit.SECONDS);
                    if (result != null) {
                        reachableIPs.add(result);
                    }
                } catch (Exception e) {
                    // Timeout veya diğer hatalar - devam et
                }
            }

            executor.shutdown();
            Log.i(TAG, "Tarama tamamlandı - Bulunan IP sayısı: " + reachableIPs.size());
            callback.onScanCompleted(reachableIPs);
        });
    }

    /**
     * ESP32 varsayılan IP adreslerini kontrol eder
     */
    public static void checkDefaultESP32IPs(DefaultIPCallback callback) {
        ExecutorService executor = Executors.newFixedThreadPool(3);

        String[] defaultIPs = {
                Constants.ESP32_AP_IP,      // AP mode IP
                "192.168.1.1",             // Router default
                "192.168.0.1"               // Alternative router default
        };

        List<Future<String>> futures = new ArrayList<>();

        for (String ip : defaultIPs) {
            Future<String> future = executor.submit(() -> {
                if (isHostReachable(ip, Constants.ESP32_PORT)) {
                    return ip;
                }
                return null;
            });
            futures.add(future);
        }

        executor.submit(() -> {
            String foundIP = null;

            for (Future<String> future : futures) {
                try {
                    String result = future.get(5, TimeUnit.SECONDS);
                    if (result != null) {
                        foundIP = result;
                        break; // İlk bulduğunu kullan
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }

            executor.shutdown();
            callback.onResult(foundIP);
        });
    }

    /**
     * IP adresinin geçerli formatta olup olmadığını kontrol eder
     */
    public static boolean isValidIPAddress(String ip) {
        if (ip == null || ip.isEmpty()) return false;

        String[] parts = ip.split("\\.");
        if (parts.length != 4) return false;

        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) return false;
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * mDNS kullanarak ESP32'yi bulmaya çalışır
     */
    public static void resolveMDNS(String hostname, MDNSCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.submit(() -> {
            try {
                InetAddress address = InetAddress.getByName(hostname);
                String ipAddress = address.getHostAddress();

                if (isHostReachable(ipAddress, Constants.ESP32_PORT)) {
                    callback.onResolved(ipAddress);
                } else {
                    callback.onFailed("mDNS çözüldü ancak erişilemez: " + ipAddress);
                }
            } catch (UnknownHostException e) {
                callback.onFailed("mDNS çözümlenemedi: " + e.getMessage());
            }

            executor.shutdown();
        });
    }

    /**
     * WiFi sinyal gücünü yüzde olarak hesaplar
     */
    public static int calculateSignalStrength(int rssi) {
        if (rssi <= -100) {
            return 0;
        } else if (rssi >= -50) {
            return 100;
        } else {
            return 2 * (rssi + 100);
        }
    }

    /**
     * Sinyal gücü açıklaması
     */
    public static String getSignalStrengthDescription(int rssi) {
        if (rssi >= -50) return "Mükemmel";
        if (rssi >= -60) return "İyi";
        if (rssi >= -70) return "Orta";
        if (rssi >= -80) return "Zayıf";
        return "Çok Zayıf";
    }

    /**
     * Ağ tipini belirler
     */
    public static String getNetworkType(Context context) {
        if (context == null) return "Bilinmeyen";

        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return "Bilinmeyen";

        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork == null) return "Bağlı Değil";

        if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
            return "WiFi";
        } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
            return "Mobil Veri";
        } else if (activeNetwork.getType() == ConnectivityManager.TYPE_ETHERNET) {
            return "Ethernet";
        }

        return "Diğer";
    }

    /**
     * Bağlantı hızını test eder
     */
    public static void testConnectionSpeed(String ipAddress, ConnectionSpeedCallback callback) {
        if (!isValidIPAddress(ipAddress)) {
            callback.onResult(0, "Geçersiz IP adresi");
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.submit(() -> {
            long startTime = System.currentTimeMillis();

            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(ipAddress, Constants.ESP32_PORT), 5000);
                socket.close();

                long endTime = System.currentTimeMillis();
                long connectionTime = endTime - startTime;

                callback.onResult(connectionTime, "Bağlantı süresi: " + connectionTime + "ms");

            } catch (IOException e) {
                callback.onResult(0, "Bağlantı hatası: " + e.getMessage());
            }

            executor.shutdown();
        });
    }

    /**
     * Mevcut WiFi ağının SSID'sini alır
     */
    public static String getCurrentWiFiSSID(Context context) {
        if (context == null) return null;

        WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);

        if (wifiManager == null) return null;

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null) return null;

        String ssid = wifiInfo.getSSID();
        if (ssid != null && ssid.startsWith("\"") && ssid.endsWith("\"")) {
            ssid = ssid.substring(1, ssid.length() - 1);
        }

        return ssid;
    }

    /**
     * ESP32 cihazının AP mode'da olup olmadığını kontrol eder
     */
    public static boolean isESP32InAPMode(Context context) {
        String currentSSID = getCurrentWiFiSSID(context);
        return currentSSID != null && currentSSID.equals(Constants.ESP32_AP_SSID);
    }

    // Callback interface'leri
    public interface ScanCallback {
        void onScanCompleted(List<String> foundIPs);
    }

    public interface DefaultIPCallback {
        void onResult(String ipAddress);
    }

    public interface MDNSCallback {
        void onResolved(String ipAddress);
        void onFailed(String error);
    }

    public interface ConnectionSpeedCallback {
        void onResult(long connectionTimeMs, String message);
    }

    /**
     * Debug amaçlı ağ bilgilerini loglar
     */
    public static void logNetworkInfo(Context context) {
        Log.d(TAG, "=== Network Info ===");
        Log.d(TAG, "Network Available: " + isNetworkAvailable(context));
        Log.d(TAG, "WiFi Connected: " + isWiFiConnected(context));
        Log.d(TAG, "Current IP: " + getCurrentWiFiIP(context));
        Log.d(TAG, "Current SSID: " + getCurrentWiFiSSID(context));
        Log.d(TAG, "Current Subnet: " + getCurrentSubnet(context));
        Log.d(TAG, "Network Type: " + getNetworkType(context));
        Log.d(TAG, "ESP32 AP Mode: " + isESP32InAPMode(context));
        Log.d(TAG, "==================");
    }
}