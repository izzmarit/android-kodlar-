package com.kulucka.mk.models;

import com.google.gson.annotations.SerializedName;
import com.kulucka.mk.utils.Constants;
import java.io.Serializable;

/**
 * Alarm ayarları için veri modeli
 */
class AlarmSettings implements Serializable {

    @SerializedName("alarmEnabled")
    private boolean alarmEnabled;

    @SerializedName("tempLowAlarm")
    private double tempLowAlarm;

    @SerializedName("tempHighAlarm")
    private double tempHighAlarm;

    @SerializedName("humidLowAlarm")
    private double humidLowAlarm;

    @SerializedName("humidHighAlarm")
    private double humidHighAlarm;

    public AlarmSettings() {
        this.alarmEnabled = true;
        this.tempLowAlarm = 36.0;
        this.tempHighAlarm = 39.0;
        this.humidLowAlarm = 50.0;
        this.humidHighAlarm = 80.0;
    }

    public AlarmSettings(boolean alarmEnabled, double tempLowAlarm, double tempHighAlarm,
                         double humidLowAlarm, double humidHighAlarm) {
        this.alarmEnabled = alarmEnabled;
        this.tempLowAlarm = tempLowAlarm;
        this.tempHighAlarm = tempHighAlarm;
        this.humidLowAlarm = humidLowAlarm;
        this.humidHighAlarm = humidHighAlarm;
    }

    // Getter metodları
    public boolean isAlarmEnabled() { return alarmEnabled; }
    public double getTempLowAlarm() { return tempLowAlarm; }
    public double getTempHighAlarm() { return tempHighAlarm; }
    public double getHumidLowAlarm() { return humidLowAlarm; }
    public double getHumidHighAlarm() { return humidHighAlarm; }

    // Setter metodları
    public void setAlarmEnabled(boolean alarmEnabled) { this.alarmEnabled = alarmEnabled; }
    public void setTempLowAlarm(double tempLowAlarm) { this.tempLowAlarm = tempLowAlarm; }
    public void setTempHighAlarm(double tempHighAlarm) { this.tempHighAlarm = tempHighAlarm; }
    public void setHumidLowAlarm(double humidLowAlarm) { this.humidLowAlarm = humidLowAlarm; }
    public void setHumidHighAlarm(double humidHighAlarm) { this.humidHighAlarm = humidHighAlarm; }

    // Validation metodları
    public boolean isValid() {
        return tempLowAlarm < tempHighAlarm &&
                humidLowAlarm < humidHighAlarm &&
                tempLowAlarm >= Constants.MIN_TEMPERATURE &&
                tempHighAlarm <= Constants.MAX_TEMPERATURE &&
                humidLowAlarm >= Constants.MIN_HUMIDITY &&
                humidHighAlarm <= Constants.MAX_HUMIDITY;
    }

    public boolean checkTemperatureAlarm(double temperature) {
        return alarmEnabled && (temperature < tempLowAlarm || temperature > tempHighAlarm);
    }

    public boolean checkHumidityAlarm(double humidity) {
        return alarmEnabled && (humidity < humidLowAlarm || humidity > humidHighAlarm);
    }

    @Override
    public String toString() {
        return "AlarmSettings{" +
                "alarmEnabled=" + alarmEnabled +
                ", tempLowAlarm=" + tempLowAlarm +
                ", tempHighAlarm=" + tempHighAlarm +
                ", humidLowAlarm=" + humidLowAlarm +
                ", humidHighAlarm=" + humidHighAlarm +
                '}';
    }
}