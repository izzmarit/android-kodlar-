package com.kulucka.mk.models;

import com.google.gson.annotations.SerializedName;
import com.kulucka.mk.utils.Constants;
import java.io.Serializable;

/**
 * WiFi ağ bilgilerini tutan model
 */
class WiFiNetwork implements Serializable {

    @SerializedName("ssid")
    private String ssid;

    @SerializedName("rssi")
    private int rssi;

    @SerializedName("encryption")
    private String encryption;

    public WiFiNetwork() {}

    public WiFiNetwork(String ssid, int rssi, String encryption) {
        this.ssid = ssid;
        this.rssi = rssi;
        this.encryption = encryption;
    }

    // Getter metodları
    public String getSsid() { return ssid; }
    public int getRssi() { return rssi; }
    public String getEncryption() { return encryption; }

    // Setter metodları
    public void setSsid(String ssid) { this.ssid = ssid; }
    public void setRssi(int rssi) { this.rssi = rssi; }
    public void setEncryption(String encryption) { this.encryption = encryption; }

    // Yardımcı metodlar
    public boolean isOpen() {
        return "open".equals(encryption);
    }

    public String getSignalStrengthDescription() {
        if (rssi >= -50) return "Mükemmel";
        if (rssi >= -60) return "İyi";
        if (rssi >= -70) return "Orta";
        if (rssi >= -80) return "Zayıf";
        return "Çok Zayıf";
    }

    public int getSignalStrengthPercentage() {
        return Math.max(0, Math.min(100, (rssi + 100) * 2));
    }

    @Override
    public String toString() {
        return "WiFiNetwork{" +
                "ssid='" + ssid + '\'' +
                ", rssi=" + rssi +
                ", encryption='" + encryption + '\'' +
                '}';
    }
}