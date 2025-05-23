package com.kulucka.mk_v5.models;

import com.google.gson.annotations.SerializedName;

public class IncubationStatus {
    @SerializedName("temperature")
    private float temperature;

    @SerializedName("humidity")
    private float humidity;

    @SerializedName("heaterState")
    private boolean heaterState;

    @SerializedName("humidifierState")
    private boolean humidifierState;

    @SerializedName("motorState")
    private boolean motorState;

    @SerializedName("currentDay")
    private int currentDay;

    @SerializedName("totalDays")
    private int totalDays;

    @SerializedName("incubationType")
    private String incubationType;

    @SerializedName("targetTemp")
    private float targetTemp;

    @SerializedName("targetHumid")
    private float targetHumid;

    // Getters ve Setters
    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public float getHumidity() {
        return humidity;
    }

    public void setHumidity(float humidity) {
        this.humidity = humidity;
    }

    public boolean isHeaterState() {
        return heaterState;
    }

    public void setHeaterState(boolean heaterState) {
        this.heaterState = heaterState;
    }

    public boolean isHumidifierState() {
        return humidifierState;
    }

    public void setHumidifierState(boolean humidifierState) {
        this.humidifierState = humidifierState;
    }

    public boolean isMotorState() {
        return motorState;
    }

    public void setMotorState(boolean motorState) {
        this.motorState = motorState;
    }

    public int getCurrentDay() {
        return currentDay;
    }

    public void setCurrentDay(int currentDay) {
        this.currentDay = currentDay;
    }

    public int getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(int totalDays) {
        this.totalDays = totalDays;
    }

    public String getIncubationType() {
        return incubationType;
    }

    public void setIncubationType(String incubationType) {
        this.incubationType = incubationType;
    }

    public float getTargetTemp() {
        return targetTemp;
    }

    public void setTargetTemp(float targetTemp) {
        this.targetTemp = targetTemp;
    }

    public float getTargetHumid() {
        return targetHumid;
    }

    public void setTargetHumid(float targetHumid) {
        this.targetHumid = targetHumid;
    }
}