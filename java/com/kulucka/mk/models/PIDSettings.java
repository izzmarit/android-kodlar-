package com.kulucka.mk.models;

import com.google.gson.annotations.SerializedName;
import com.kulucka.mk.utils.Constants;
import java.io.Serializable;

/**
 * PID kontrol parametreleri için veri modeli
 */
public class PIDSettings implements Serializable {

    @SerializedName("pidMode")
    private int pidMode;

    @SerializedName("pidKp")
    private double pidKp;

    @SerializedName("pidKi")
    private double pidKi;

    @SerializedName("pidKd")
    private double pidKd;

    public PIDSettings() {
        this.pidMode = Constants.PID_MODE_OFF;
        this.pidKp = 2.0;
        this.pidKi = 0.5;
        this.pidKd = 0.1;
    }

    public PIDSettings(int pidMode, double pidKp, double pidKi, double pidKd) {
        this.pidMode = pidMode;
        this.pidKp = pidKp;
        this.pidKi = pidKi;
        this.pidKd = pidKd;
    }

    // Getter metodları
    public int getPidMode() { return pidMode; }
    public double getPidKp() { return pidKp; }
    public double getPidKi() { return pidKi; }
    public double getPidKd() { return pidKd; }

    // Setter metodları
    public void setPidMode(int pidMode) { this.pidMode = pidMode; }
    public void setPidKp(double pidKp) { this.pidKp = pidKp; }
    public void setPidKi(double pidKi) { this.pidKi = pidKi; }
    public void setPidKd(double pidKd) { this.pidKd = pidKd; }

    // Yardımcı metodlar
    public String getPidModeName() {
        if (pidMode >= 0 && pidMode < Constants.PID_MODE_NAMES.length) {
            return Constants.PID_MODE_NAMES[pidMode];
        }
        return "Bilinmeyen";
    }

    public boolean isValid() {
        return pidMode >= 0 && pidMode < Constants.PID_MODE_NAMES.length &&
                pidKp >= Constants.MIN_PID_KP && pidKp <= Constants.MAX_PID_KP &&
                pidKi >= Constants.MIN_PID_KI && pidKi <= Constants.MAX_PID_KI &&
                pidKd >= Constants.MIN_PID_KD && pidKd <= Constants.MAX_PID_KD;
    }

    @Override
    public String toString() {
        return "PIDSettings{" +
                "pidMode=" + pidMode + " (" + getPidModeName() + ")" +
                ", pidKp=" + pidKp +
                ", pidKi=" + pidKi +
                ", pidKd=" + pidKd +
                '}';
    }
}