package com.kulucka.kontrol.models;

import java.io.Serializable;

public class SensorData implements Serializable {
    // Mevcut kodun başına Serializable eklenmeli
    private static final long serialVersionUID = 1L;
    private float temperature;
    private float humidity;
    private float temperature1;
    private float humidity1;
    private float temperature2;
    private float humidity2;
    private boolean heaterActive;
    private boolean humidifierActive;
    private boolean motorActive;
    private int motorRemainingMinutes;
    private float pidOutput;
    private int currentDay;

    // Constructor, getters ve setters

    public SensorData() {
    }

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

    public float getTemperature1() {
        return temperature1;
    }

    public void setTemperature1(float temperature1) {
        this.temperature1 = temperature1;
    }

    public float getHumidity1() {
        return humidity1;
    }

    public void setHumidity1(float humidity1) {
        this.humidity1 = humidity1;
    }

    public float getTemperature2() {
        return temperature2;
    }

    public void setTemperature2(float temperature2) {
        this.temperature2 = temperature2;
    }

    public float getHumidity2() {
        return humidity2;
    }

    public void setHumidity2(float humidity2) {
        this.humidity2 = humidity2;
    }

    public boolean isHeaterActive() {
        return heaterActive;
    }

    public void setHeaterActive(boolean heaterActive) {
        this.heaterActive = heaterActive;
    }

    public boolean isHumidifierActive() {
        return humidifierActive;
    }

    public void setHumidifierActive(boolean humidifierActive) {
        this.humidifierActive = humidifierActive;
    }

    public boolean isMotorActive() {
        return motorActive;
    }

    public void setMotorActive(boolean motorActive) {
        this.motorActive = motorActive;
    }

    public int getMotorRemainingMinutes() {
        return motorRemainingMinutes;
    }

    public void setMotorRemainingMinutes(int motorRemainingMinutes) {
        this.motorRemainingMinutes = motorRemainingMinutes;
    }

    public float getPidOutput() {
        return pidOutput;
    }

    public void setPidOutput(float pidOutput) {
        this.pidOutput = pidOutput;
    }

    public int getCurrentDay() {
        return currentDay;
    }

    public void setCurrentDay(int currentDay) {
        this.currentDay = currentDay;
    }
}