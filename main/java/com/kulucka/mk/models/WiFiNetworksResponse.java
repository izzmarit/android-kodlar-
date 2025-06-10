package com.kulucka.mk.models;

import com.google.gson.annotations.SerializedName;
import com.kulucka.mk.utils.Constants;
import java.io.Serializable;

/**
 * WiFi ağları listesi response modeli
 */
class WiFiNetworksResponse implements Serializable {

    @SerializedName("networks")
    private WiFiNetwork[] networks;

    public WiFiNetworksResponse() {}

    public WiFiNetworksResponse(WiFiNetwork[] networks) {
        this.networks = networks;
    }

    public WiFiNetwork[] getNetworks() { return networks; }
    public void setNetworks(WiFiNetwork[] networks) { this.networks = networks; }

    public boolean hasNetworks() {
        return networks != null && networks.length > 0;
    }

    public int getNetworkCount() {
        return networks != null ? networks.length : 0;
    }

    @Override
    public String toString() {
        return "WiFiNetworksResponse{" +
                "networkCount=" + getNetworkCount() +
                '}';
    }
}