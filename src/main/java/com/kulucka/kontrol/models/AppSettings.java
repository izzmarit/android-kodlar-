package com.kulucka.kontrol.models;

import java.io.Serializable;

public class AppSettings implements Serializable {
    private static final long serialVersionUID = 1L;
    private int profileType;
    private float targetTemperature;
    private float targetHumidity;
    private int totalDays;
    private long startTime;
    private boolean motorEnabled;
    private float pidKp;
    private float pidKi;
    private float pidKd;
    private int currentDay;
    private MotorSettings motorSettings;
    private RelayState relayState;

    public static class MotorSettings implements Serializable {
        private static final long serialVersionUID = 1L;
        private long duration;
        private long interval;

        public long getDuration() {
            return duration;
        }

        public void setDuration(long duration) {
            this.duration = duration;
        }

        public long getInterval() {
            return interval;
        }

        public void setInterval(long interval) {
            this.interval = interval;
        }
    }

    public static class RelayState implements Serializable {
        private static final long serialVersionUID = 1L;
        private boolean heater;
        private boolean humidifier;
        private boolean motor;

        public boolean isHeater() {
            return heater;
        }

        public void setHeater(boolean heater) {
            this.heater = heater;
        }

        public boolean isHumidifier() {
            return humidifier;
        }

        public void setHumidifier(boolean humidifier) {
            this.humidifier = humidifier;
        }

        public boolean isMotor() {
            return motor;
        }

        public void setMotor(boolean motor) {
            this.motor = motor;
        }
    }

    // Getters ve setters

    public int getProfileType() {
        return profileType;
    }

    public void setProfileType(int profileType) {
        this.profileType = profileType;
    }

    public float getTargetTemperature() {
        return targetTemperature;
    }

    public void setTargetTemperature(float targetTemperature) {
        this.targetTemperature = targetTemperature;
    }

    public float getTargetHumidity() {
        return targetHumidity;
    }

    public void setTargetHumidity(float targetHumidity) {
        this.targetHumidity = targetHumidity;
    }

    public int getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(int totalDays) {
        this.totalDays = totalDays;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public boolean isMotorEnabled() {
        return motorEnabled;
    }

    public void setMotorEnabled(boolean motorEnabled) {
        this.motorEnabled = motorEnabled;
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

    public int getCurrentDay() {
        return currentDay;
    }

    public void setCurrentDay(int currentDay) {
        this.currentDay = currentDay;
    }

    public MotorSettings getMotorSettings() {
        return motorSettings;
    }

    public void setMotorSettings(MotorSettings motorSettings) {
        this.motorSettings = motorSettings;
    }

    public RelayState getRelayState() {
        return relayState;
    }

    public void setRelayState(RelayState relayState) {
        this.relayState = relayState;
    }
}