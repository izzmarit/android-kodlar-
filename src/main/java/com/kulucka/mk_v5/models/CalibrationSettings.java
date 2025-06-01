package com.kulucka.mk_v5.models;

public class CalibrationSettings {
    private float tempCalibration1;
    private float tempCalibration2;
    private float humidCalibration1;
    private float humidCalibration2;

    public CalibrationSettings() {
        tempCalibration1 = 0.0f;
        tempCalibration2 = 0.0f;
        humidCalibration1 = 0.0f;
        humidCalibration2 = 0.0f;
    }

    public float getTempCalibration1() {
        return tempCalibration1;
    }

    public void setTempCalibration1(float tempCalibration1) {
        this.tempCalibration1 = tempCalibration1;
    }

    public float getTempCalibration2() {
        return tempCalibration2;
    }

    public void setTempCalibration2(float tempCalibration2) {
        this.tempCalibration2 = tempCalibration2;
    }

    public float getHumidCalibration1() {
        return humidCalibration1;
    }

    public void setHumidCalibration1(float humidCalibration1) {
        this.humidCalibration1 = humidCalibration1;
    }

    public float getHumidCalibration2() {
        return humidCalibration2;
    }

    public void setHumidCalibration2(float humidCalibration2) {
        this.humidCalibration2 = humidCalibration2;
    }
}