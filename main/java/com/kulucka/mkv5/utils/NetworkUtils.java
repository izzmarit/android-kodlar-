package com.kulucka.mkv5.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class NetworkUtils {

    /**
     * Cihazın internete bağlı olup olmadığını kontrol eder
     */
    public static boolean isInternetAvailable(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network network = connectivityManager.getActiveNetwork();
                if (network == null) return false;

                NetworkCapabilities capabilities =
                        connectivityManager.getNetworkCapabilities(network);
                return capabilities != null && (
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
            } else {
                NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            }
        }
        return false;
    }

    /**
     * WiFi bağlantısı olup olmadığını kontrol eder
     */
    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network network = connectivityManager.getActiveNetwork();
                if (network == null) return false;

                NetworkCapabilities capabilities =
                        connectivityManager.getNetworkCapabilities(network);
                return capabilities != null &&
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
            } else {
                NetworkInfo networkInfo =
                        connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                return networkInfo != null && networkInfo.isConnected();
            }
        }
        return false;
    }

    /**
     * Bağlı olan WiFi ağının SSID'sini döndürür
     */
    public static String getConnectedWifiSSID(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);

        if (wifiManager != null && wifiManager.isWifiEnabled()) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                String ssid = wifiInfo.getSSID();
                // Android, SSID'yi tırnak içinde döndürür
                if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                    ssid = ssid.substring(1, ssid.length() - 1);
                }
                return ssid;
            }
        }
        return "";
    }

    /**
     * Cihazın IP adresini döndürür
     */
    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces =
                    Collections.list(NetworkInterface.getNetworkInterfaces());

            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());

                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        boolean isIPv4 = sAddr.indexOf(':') < 0;

                        if (useIPv4) {
                            if (isIPv4) return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%');
                                return delim < 0 ? sAddr.toUpperCase() :
                                        sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) { }
        return "";
    }

    /**
     * WiFi sinyal gücünü yüzde olarak döndürür
     */
    public static int getWifiSignalStrengthInPercent(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);

        if (wifiManager != null && wifiManager.isWifiEnabled()) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                int level = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), 100);
                return level;
            }
        }
        return 0;
    }

    /**
     * Verilen IP adresine ping atar
     */
    public static boolean isReachable(String ipAddress, int timeout) {
        try {
            InetAddress address = InetAddress.getByName(ipAddress);
            return address.isReachable(timeout);
        } catch (Exception e) {
            return false;
        }
    }
}