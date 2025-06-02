package com.kulucka.mk_v5.models;

public class MotorSettings {
    private long motorWaitTime; // Dakika
    private long motorRunTime;  // Saniye
    private boolean turningEnabled;
    private String direction;

    public MotorSettings() {
        // Varsayılan değerler - ESP32'deki DEFAULT_MOTOR değerlerine uygun
        motorWaitTime = 120;
        motorRunTime = 14;
        turningEnabled = true;
        direction = "clockwise";
    }

    // Mevcut metodlar
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

    // SharedPrefsManager ile uyumluluk için alias metodlar
    public int getTurningInterval() {
        return (int) motorWaitTime;
    }

    public void setTurningInterval(int interval) {
        this.motorWaitTime = interval;
    }

    public int getTurningDuration() {
        return (int) motorRunTime;
    }

    public void setTurningDuration(int duration) {
        this.motorRunTime = duration;
    }

    public boolean isTurningEnabled() {
        return turningEnabled;
    }

    public void setTurningEnabled(boolean enabled) {
        this.turningEnabled = enabled;
    }

    public String getDirection() {
        return direction != null ? direction : "clockwise";
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }
}