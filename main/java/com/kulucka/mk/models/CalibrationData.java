package com.kulucka.mk.models;

import com.google.gson.annotations.SerializedName;
import com.kulucka.mk.utils.Constants;
import java.io.Serializable;

/**
 * Sensor kalibrasyon verileri için model
 */
class CalibrationData implements Serializable {

    @SerializedName("tempCalibration1")
    private double tempCalibration1;

    @SerializedName("tempCalibration2")
    private double tempCalibration2;

    @SerializedName("humidCalibration1")
    private double humidCalibration1;

    @SerializedName("humidCalibration2")
    private double humidCalibration2;

    public CalibrationData() {
        this.tempCalibration1 = 0.0;
        this.tempCalibration2 = 0.0;
        this.humidCalibration1 = 0.0;
        this.humidCalibration2 = 0.0;
    }

    public CalibrationData(double tempCalibration1, double tempCalibration2,
                           double humidCalibration1, double humidCalibration2) {
        this.tempCalibration1 = tempCalibration1;
        this.tempCalibration2 = tempCalibration2;
        this.humidCalibration1 = humidCalibration1;
        this.humidCalibration2 = humidCalibration2;
    }

    // Getter metodları
    public double getTempCalibration1() { return tempCalibration1; }
    public double getTempCalibration2() { return tempCalibration2; }
    public double getHumidCalibration1() { return humidCalibration1; }
    public double getHumidCalibration2() { return humidCalibration2; }

    // Setter metodları
    public void setTempCalibration1(double tempCalibration1) { this.tempCalibration1 = tempCalibration1; }
    public void setTempCalibration2(double tempCalibration2) { this.tempCalibration2 = tempCalibration2; }
    public void setHumidCalibration1(double humidCalibration1) { this.humidCalibration1 = humidCalibration1; }
    public void setHumidCalibration2(double humidCalibration2) { this.humidCalibration2 = humidCalibration2; }

    // Validation metodları
    public boolean isValid() {
        return tempCalibration1 >= Constants.MIN_CALIBRATION && tempCalibration1 <= Constants.MAX_CALIBRATION &&
                tempCalibration2 >= Constants.MIN_CALIBRATION && tempCalibration2 <= Constants.MAX_CALIBRATION &&
                humidCalibration1 >= Constants.MIN_CALIBRATION && humidCalibration1 <= Constants.MAX_CALIBRATION &&
                humidCalibration2 >= Constants.MIN_CALIBRATION && humidCalibration2 <= Constants.MAX_CALIBRATION;
    }

    @Override
    public String toString() {
        return "CalibrationData{" +
                "tempCalibration1=" + tempCalibration1 +
                ", tempCalibration2=" + tempCalibration2 +
                ", humidCalibration1=" + humidCalibration1 +
                ", humidCalibration2=" + humidCalibration2 +
                '}';
    }
}