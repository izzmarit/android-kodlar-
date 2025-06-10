package com.kulucka.mk.models;

import com.google.gson.annotations.SerializedName;
import com.kulucka.mk.utils.Constants;
import java.io.Serializable;

/**
 * Motor ayarları için veri modeli
 */
public class MotorSettings implements Serializable {

    @SerializedName("motorWaitTime")
    private int motorWaitTime; // dakika cinsinden

    @SerializedName("motorRunTime")
    private int motorRunTime; // saniye cinsinden

    @SerializedName("motorState")
    private boolean motorState;

    @SerializedName("motorEnabled")
    private boolean motorEnabled;

    @SerializedName("manualOverride")
    private boolean manualOverride;

    public MotorSettings() {
        this.motorWaitTime = Constants.DEFAULT_MOTOR_WAIT_TIME;
        this.motorRunTime = Constants.DEFAULT_MOTOR_RUN_TIME;
        this.motorState = false;
        this.motorEnabled = true;
        this.manualOverride = false;
    }

    public MotorSettings(int motorWaitTime, int motorRunTime, boolean motorState,
                         boolean motorEnabled, boolean manualOverride) {
        this.motorWaitTime = motorWaitTime;
        this.motorRunTime = motorRunTime;
        this.motorState = motorState;
        this.motorEnabled = motorEnabled;
        this.manualOverride = manualOverride;
    }

    // Getter metodları
    public int getMotorWaitTime() { return motorWaitTime; }
    public int getMotorRunTime() { return motorRunTime; }
    public boolean isMotorState() { return motorState; }
    public boolean isMotorEnabled() { return motorEnabled; }
    public boolean isManualOverride() { return manualOverride; }

    // Setter metodları
    public void setMotorWaitTime(int motorWaitTime) { this.motorWaitTime = motorWaitTime; }
    public void setMotorRunTime(int motorRunTime) { this.motorRunTime = motorRunTime; }
    public void setMotorState(boolean motorState) { this.motorState = motorState; }
    public void setMotorEnabled(boolean motorEnabled) { this.motorEnabled = motorEnabled; }
    public void setManualOverride(boolean manualOverride) { this.manualOverride = manualOverride; }

    // Validation metodları
    public boolean isValid() {
        return motorWaitTime >= Constants.MIN_MOTOR_WAIT_TIME &&
                motorWaitTime <= Constants.MAX_MOTOR_WAIT_TIME &&
                motorRunTime >= Constants.MIN_MOTOR_RUN_TIME &&
                motorRunTime <= Constants.MAX_MOTOR_RUN_TIME;
    }

    public String getValidationMessage() {
        if (!isValid()) {
            if (motorWaitTime < Constants.MIN_MOTOR_WAIT_TIME || motorWaitTime > Constants.MAX_MOTOR_WAIT_TIME) {
                return String.format("Motor bekleme süresi %d-%d dakika arasında olmalı",
                        Constants.MIN_MOTOR_WAIT_TIME, Constants.MAX_MOTOR_WAIT_TIME);
            }
            if (motorRunTime < Constants.MIN_MOTOR_RUN_TIME || motorRunTime > Constants.MAX_MOTOR_RUN_TIME) {
                return String.format("Motor çalışma süresi %d-%d saniye arasında olmalı",
                        Constants.MIN_MOTOR_RUN_TIME, Constants.MAX_MOTOR_RUN_TIME);
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "MotorSettings{" +
                "motorWaitTime=" + motorWaitTime +
                " dk, motorRunTime=" + motorRunTime +
                " sn, motorState=" + motorState +
                ", motorEnabled=" + motorEnabled +
                ", manualOverride=" + manualOverride +
                '}';
    }
}