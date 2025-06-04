package com.kulucka.mk_v5.models;

public class AlarmSettings {
    private float tempLowAlarm;
    private float tempHighAlarm;
    private float humidLowAlarm;
    private float humidHighAlarm;

    public AlarmSettings() {
        // ESP32'deki varsayılan alarm değerleri
        tempLowAlarm = 1.0f;
        tempHighAlarm = 1.0f;
        humidLowAlarm = 10.0f;
        humidHighAlarm = 10.0f;
    }

    public float getTempLowAlarm() {
        return tempLowAlarm;
    }

    public void setTempLowAlarm(float tempLowAlarm) {
        this.tempLowAlarm = tempLowAlarm;
    }

    public float getTempHighAlarm() {
        return tempHighAlarm;
    }

    public void setTempHighAlarm(float tempHighAlarm) {
        this.tempHighAlarm = tempHighAlarm;
    }

    public float getHumidLowAlarm() {
        return humidLowAlarm;
    }

    public void setHumidLowAlarm(float humidLowAlarm) {
        this.humidLowAlarm = humidLowAlarm;
    }

    public float getHumidHighAlarm() {
        return humidHighAlarm;
    }

    public void setHumidHighAlarm(float humidHighAlarm) {
        this.humidHighAlarm = humidHighAlarm;
    }
}