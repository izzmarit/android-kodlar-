package com.kulucka.mkv5.models;

import com.google.gson.annotations.SerializedName;

public class WifiNetwork {
    @SerializedName("ssid")
    private String ssid;

    @SerializedName("rssi")
    private int rssi;

    @SerializedName("encryption")
    private String encryption;

    public WifiNetwork() {
    }

    public WifiNetwork(String ssid, int rssi, String encryption) {
        this.ssid = ssid;
        this.rssi = rssi;
        this.encryption = encryption;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public String getEncryption() {
        return encryption;
    }

    public void setEncryption(String encryption) {
        this.encryption = encryption;
    }

    public boolean isOpen() {
        return "open".equals(encryption);
    }

    public String getSignalStrength() {
        if (rssi >= -50) {
            return "Çok İyi";
        } else if (rssi >= -60) {
            return "İyi";
        } else if (rssi >= -70) {
            return "Orta";
        } else {
            return "Zayıf";
        }
    }
}