package com.kulucka.mk_v5.models;

public class WiFiNetwork {
    private String ssid;
    private int signalStrength;
    private boolean isSecured;

    public WiFiNetwork() {}

    public WiFiNetwork(String ssid, int signalStrength, boolean isSecured) {
        this.ssid = ssid;
        this.signalStrength = signalStrength;
        this.isSecured = isSecured;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public int getSignalStrength() {
        return signalStrength;
    }

    public void setSignalStrength(int signalStrength) {
        this.signalStrength = signalStrength;
    }

    public boolean isSecured() {
        return isSecured;
    }

    public void setSecured(boolean secured) {
        isSecured = secured;
    }

    @Override
    public String toString() {
        return ssid + " (" + signalStrength + " dBm) " + (isSecured ? "🔒" : "");
    }
}