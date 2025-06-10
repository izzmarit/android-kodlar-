package com.kulucka.mk.models;

import com.google.gson.annotations.SerializedName;
import com.kulucka.mk.utils.Constants;
import java.io.Serializable;

/**
 * Kuluçka ayarları için veri modeli
 * ESP32'ye kuluçka parametrelerini göndermek için kullanılır
 */
public class IncubationSettings implements Serializable {

    @SerializedName("incubationType")
    private int incubationType;

    @SerializedName("isIncubationRunning")
    private boolean isIncubationRunning;

    // Manuel kuluçka parametreleri
    @SerializedName("manualDevTemp")
    private double manualDevTemp;

    @SerializedName("manualHatchTemp")
    private double manualHatchTemp;

    @SerializedName("manualDevHumid")
    private int manualDevHumid;

    @SerializedName("manualHatchHumid")
    private int manualHatchHumid;

    @SerializedName("manualDevDays")
    private int manualDevDays;

    @SerializedName("manualHatchDays")
    private int manualHatchDays;

    // Varsayılan constructor
    public IncubationSettings() {
        // Varsayılan değerler
        this.incubationType = Constants.INCUBATION_TYPE_CHICKEN;
        this.isIncubationRunning = false;
        this.manualDevTemp = 37.5;
        this.manualHatchTemp = 37.0;
        this.manualDevHumid = 60;
        this.manualHatchHumid = 70;
        this.manualDevDays = 18;
        this.manualHatchDays = 3;
    }

    // Parametreli constructor
    public IncubationSettings(int incubationType, boolean isIncubationRunning,
                              double manualDevTemp, double manualHatchTemp,
                              int manualDevHumid, int manualHatchHumid,
                              int manualDevDays, int manualHatchDays) {
        this.incubationType = incubationType;
        this.isIncubationRunning = isIncubationRunning;
        this.manualDevTemp = manualDevTemp;
        this.manualHatchTemp = manualHatchTemp;
        this.manualDevHumid = manualDevHumid;
        this.manualHatchHumid = manualHatchHumid;
        this.manualDevDays = manualDevDays;
        this.manualHatchDays = manualHatchDays;
    }

    // Getter metodları
    public int getIncubationType() { return incubationType; }
    public boolean isIncubationRunning() { return isIncubationRunning; }
    public double getManualDevTemp() { return manualDevTemp; }
    public double getManualHatchTemp() { return manualHatchTemp; }
    public int getManualDevHumid() { return manualDevHumid; }
    public int getManualHatchHumid() { return manualHatchHumid; }
    public int getManualDevDays() { return manualDevDays; }
    public int getManualHatchDays() { return manualHatchDays; }

    // Setter metodları
    public void setIncubationType(int incubationType) { this.incubationType = incubationType; }
    public void setIncubationRunning(boolean incubationRunning) { this.isIncubationRunning = incubationRunning; }
    public void setManualDevTemp(double manualDevTemp) { this.manualDevTemp = manualDevTemp; }
    public void setManualHatchTemp(double manualHatchTemp) { this.manualHatchTemp = manualHatchTemp; }
    public void setManualDevHumid(int manualDevHumid) { this.manualDevHumid = manualDevHumid; }
    public void setManualHatchHumid(int manualHatchHumid) { this.manualHatchHumid = manualHatchHumid; }
    public void setManualDevDays(int manualDevDays) { this.manualDevDays = manualDevDays; }
    public void setManualHatchDays(int manualHatchDays) { this.manualHatchDays = manualHatchDays; }

    // Yardımcı metodlar
    public String getIncubationTypeName() {
        if (incubationType >= 0 && incubationType < Constants.INCUBATION_TYPE_NAMES.length) {
            return Constants.INCUBATION_TYPE_NAMES[incubationType];
        }
        return "Bilinmeyen";
    }

    public boolean isManualType() {
        return incubationType == Constants.INCUBATION_TYPE_MANUAL;
    }

    public int getTotalDays() {
        if (isManualType()) {
            return manualDevDays + manualHatchDays;
        } else {
            // Ön tanımlı kuluçka tiplerinin gün sayıları
            switch (incubationType) {
                case Constants.INCUBATION_TYPE_CHICKEN:
                    return 21;
                case Constants.INCUBATION_TYPE_QUAIL:
                    return 17;
                case Constants.INCUBATION_TYPE_GOOSE:
                    return 28;
                default:
                    return 21;
            }
        }
    }

    public double getCurrentTargetTemp(int currentDay) {
        if (isManualType()) {
            return currentDay <= manualDevDays ? manualDevTemp : manualHatchTemp;
        } else {
            // Ön tanımlı kuluçka tiplerinin sıcaklık değerleri
            switch (incubationType) {
                case Constants.INCUBATION_TYPE_CHICKEN:
                    return currentDay <= 18 ? 37.5 : 37.2;
                case Constants.INCUBATION_TYPE_QUAIL:
                    return currentDay <= 14 ? 37.7 : 37.2;
                case Constants.INCUBATION_TYPE_GOOSE:
                    return currentDay <= 25 ? 37.4 : 37.0;
                default:
                    return 37.5;
            }
        }
    }

    public double getCurrentTargetHumidity(int currentDay) {
        if (isManualType()) {
            return currentDay <= manualDevDays ? manualDevHumid : manualHatchHumid;
        } else {
            // Ön tanımlı kuluçka tiplerinin nem değerleri
            switch (incubationType) {
                case Constants.INCUBATION_TYPE_CHICKEN:
                    return currentDay <= 18 ? 60 : 65;
                case Constants.INCUBATION_TYPE_QUAIL:
                    return currentDay <= 14 ? 60 : 70;
                case Constants.INCUBATION_TYPE_GOOSE:
                    return currentDay <= 25 ? 55 : 75;
                default:
                    return 60;
            }
        }
    }

    public boolean isValidConfiguration() {
        // Kuluçka tipi geçerli mi?
        if (incubationType < 0 || incubationType >= Constants.INCUBATION_TYPE_NAMES.length) {
            return false;
        }

        // Manuel tip ise parametreleri kontrol et
        if (isManualType()) {
            if (manualDevTemp < Constants.MIN_TEMPERATURE || manualDevTemp > Constants.MAX_TEMPERATURE) {
                return false;
            }
            if (manualHatchTemp < Constants.MIN_TEMPERATURE || manualHatchTemp > Constants.MAX_TEMPERATURE) {
                return false;
            }
            if (manualDevHumid < Constants.MIN_HUMIDITY || manualDevHumid > Constants.MAX_HUMIDITY) {
                return false;
            }
            if (manualHatchHumid < Constants.MIN_HUMIDITY || manualHatchHumid > Constants.MAX_HUMIDITY) {
                return false;
            }
            if (manualDevDays < 1 || manualDevDays > Constants.MAX_INCUBATION_DAYS) {
                return false;
            }
            if (manualHatchDays < 1 || manualHatchDays > Constants.MAX_INCUBATION_DAYS) {
                return false;
            }
            if (getTotalDays() > Constants.MAX_INCUBATION_DAYS) {
                return false;
            }
        }

        return true;
    }

    public String getValidationMessage() {
        if (isValidConfiguration()) {
            return null;
        }

        if (incubationType < 0 || incubationType >= Constants.INCUBATION_TYPE_NAMES.length) {
            return "Geçersiz kuluçka tipi seçildi";
        }

        if (isManualType()) {
            if (manualDevTemp < Constants.MIN_TEMPERATURE || manualDevTemp > Constants.MAX_TEMPERATURE) {
                return String.format("Gelişim sıcaklığı %.1f-%.1f°C arasında olmalı",
                        Constants.MIN_TEMPERATURE, Constants.MAX_TEMPERATURE);
            }
            if (manualHatchTemp < Constants.MIN_TEMPERATURE || manualHatchTemp > Constants.MAX_TEMPERATURE) {
                return String.format("Çıkım sıcaklığı %.1f-%.1f°C arasında olmalı",
                        Constants.MIN_TEMPERATURE, Constants.MAX_TEMPERATURE);
            }
            if (manualDevHumid < Constants.MIN_HUMIDITY || manualDevHumid > Constants.MAX_HUMIDITY) {
                return String.format("Gelişim nemi %.0f-%.0f%% arasında olmalı",
                        Constants.MIN_HUMIDITY, Constants.MAX_HUMIDITY);
            }
            if (manualHatchHumid < Constants.MIN_HUMIDITY || manualHatchHumid > Constants.MAX_HUMIDITY) {
                return String.format("Çıkım nemi %.0f-%.0f%% arasında olmalı",
                        Constants.MIN_HUMIDITY, Constants.MAX_HUMIDITY);
            }
            if (manualDevDays < 1 || manualDevDays > Constants.MAX_INCUBATION_DAYS) {
                return String.format("Gelişim günü 1-%d arasında olmalı", Constants.MAX_INCUBATION_DAYS);
            }
            if (manualHatchDays < 1 || manualHatchDays > Constants.MAX_INCUBATION_DAYS) {
                return String.format("Çıkım günü 1-%d arasında olmalı", Constants.MAX_INCUBATION_DAYS);
            }
            if (getTotalDays() > Constants.MAX_INCUBATION_DAYS) {
                return String.format("Toplam kuluçka süresi %d günü geçemez", Constants.MAX_INCUBATION_DAYS);
            }
        }

        return "Bilinmeyen hata";
    }

    @Override
    public String toString() {
        return "IncubationSettings{" +
                "incubationType=" + incubationType +
                " (" + getIncubationTypeName() + ")" +
                ", isIncubationRunning=" + isIncubationRunning +
                ", manualDevTemp=" + manualDevTemp +
                ", manualHatchTemp=" + manualHatchTemp +
                ", manualDevHumid=" + manualDevHumid +
                ", manualHatchHumid=" + manualHatchHumid +
                ", manualDevDays=" + manualDevDays +
                ", manualHatchDays=" + manualHatchDays +
                ", totalDays=" + getTotalDays() +
                '}';
    }
}