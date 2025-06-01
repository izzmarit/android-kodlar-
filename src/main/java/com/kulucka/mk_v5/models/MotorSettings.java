package com.kulucka.mk_v5.models;

public class MotorSettings {
    private long motorWaitTime; // Dakika
    private long motorRunTime;  // Saniye

    public MotorSettings() {
        // Varsayılan değerler - ESP32'deki DEFAULT_MOTOR değerlerine uygun
        motorWaitTime = 120;
        motorRunTime = 14;
    }

    public long getMotorWaitTime() {
        return motorWaitTime;
    }

    public void setMotorWaitTime(long motorWaitTime) {
        this.motorWaitTime = motorWaitTime;
    }

    public long getMotorRunTime() {
        return motorRunTime;
    }

    public void setMotorRunTime(long motorRunTime) {
        this.motorRunTime = motorRunTime;
    }
}