package com.kulucka.mk_v5.models;

public class PidSettings {
    private float pidKp;
    private float pidKi;
    private float pidKd;

    public PidSettings() {
        // ESP32'deki varsayılan PID değerleri
        pidKp = 10.0f;
        pidKi = 0.1f;
        pidKd = 5.0f;
    }

    public float getPidKp() {
        return pidKp;
    }

    public void setPidKp(float pidKp) {
        this.pidKp = pidKp;
    }

    public float getPidKi() {
        return pidKi;
    }

    public void setPidKi(float pidKi) {
        this.pidKi = pidKi;
    }

    public float getPidKd() {
        return pidKd;
    }

    public void setPidKd(float pidKd) {
        this.pidKd = pidKd;
    }
}