package com.kulucka.mkv5.models;

import com.google.gson.annotations.SerializedName;

public class IncubationSettings {

    @SerializedName("type")
    private int type;

    @SerializedName("developmentTemperature")
    private float developmentTemperature;

    @SerializedName("hatchingTemperature")
    private float hatchingTemperature;

    @SerializedName("developmentHumidity")
    private int developmentHumidity;

    @SerializedName("hatchingHumidity")
    private int hatchingHumidity;

    @SerializedName("developmentDays")
    private int developmentDays;

    @SerializedName("hatchingDays")
    private int hatchingDays;

    @SerializedName("totalDays")
    private int totalDays;

    @SerializedName("motorWaitTime")
    private int motorWaitTime;

    @SerializedName("motorRunTime")
    private int motorRunTime;

    // Kuluçka tipleri için sabitler
    public static final int TYPE_CHICKEN = 0;
    public static final int TYPE_QUAIL = 1;
    public static final int TYPE_GOOSE = 2;
    public static final int TYPE_MANUAL = 3;

    // Varsayılan değerler
    public static class Defaults {
        // Tavuk
        public static final float CHICKEN_DEV_TEMP = 37.8f;
        public static final float CHICKEN_HATCH_TEMP = 37.5f;
        public static final int CHICKEN_DEV_HUMID = 60;
        public static final int CHICKEN_HATCH_HUMID = 70;
        public static final int CHICKEN_DEV_DAYS = 18;
        public static final int CHICKEN_HATCH_DAYS = 3;
        public static final int CHICKEN_MOTOR_WAIT = 120;
        public static final int CHICKEN_MOTOR_RUN = 14;

        // Bıldırcın
        public static final float QUAIL_DEV_TEMP = 37.8f;
        public static final float QUAIL_HATCH_TEMP = 37.5f;
        public static final int QUAIL_DEV_HUMID = 55;
        public static final int QUAIL_HATCH_HUMID = 65;
        public static final int QUAIL_DEV_DAYS = 14;
        public static final int QUAIL_HATCH_DAYS = 2;
        public static final int QUAIL_MOTOR_WAIT = 90;
        public static final int QUAIL_MOTOR_RUN = 10;

        // Kaz
        public static final float GOOSE_DEV_TEMP = 37.6f;
        public static final float GOOSE_HATCH_TEMP = 37.3f;
        public static final int GOOSE_DEV_HUMID = 65;
        public static final int GOOSE_HATCH_HUMID = 75;
        public static final int GOOSE_DEV_DAYS = 25;
        public static final int GOOSE_HATCH_DAYS = 3;
        public static final int GOOSE_MOTOR_WAIT = 180;
        public static final int GOOSE_MOTOR_RUN = 20;
    }

    // Constructors
    public IncubationSettings() {
        // Varsayılan değerler
        this.type = TYPE_CHICKEN;
        setDefaultsForType(TYPE_CHICKEN);
    }

    public IncubationSettings(int type) {
        this.type = type;
        setDefaultsForType(type);
    }

    // Tip için varsayılan değerleri ayarla
    public void setDefaultsForType(int type) {
        switch (type) {
            case TYPE_CHICKEN:
                this.developmentTemperature = Defaults.CHICKEN_DEV_TEMP;
                this.hatchingTemperature = Defaults.CHICKEN_HATCH_TEMP;
                this.developmentHumidity = Defaults.CHICKEN_DEV_HUMID;
                this.hatchingHumidity = Defaults.CHICKEN_HATCH_HUMID;
                this.developmentDays = Defaults.CHICKEN_DEV_DAYS;
                this.hatchingDays = Defaults.CHICKEN_HATCH_DAYS;
                this.motorWaitTime = Defaults.CHICKEN_MOTOR_WAIT;
                this.motorRunTime = Defaults.CHICKEN_MOTOR_RUN;
                break;

            case TYPE_QUAIL:
                this.developmentTemperature = Defaults.QUAIL_DEV_TEMP;
                this.hatchingTemperature = Defaults.QUAIL_HATCH_TEMP;
                this.developmentHumidity = Defaults.QUAIL_DEV_HUMID;
                this.hatchingHumidity = Defaults.QUAIL_HATCH_HUMID;
                this.developmentDays = Defaults.QUAIL_DEV_DAYS;
                this.hatchingDays = Defaults.QUAIL_HATCH_DAYS;
                this.motorWaitTime = Defaults.QUAIL_MOTOR_WAIT;
                this.motorRunTime = Defaults.QUAIL_MOTOR_RUN;
                break;

            case TYPE_GOOSE:
                this.developmentTemperature = Defaults.GOOSE_DEV_TEMP;
                this.hatchingTemperature = Defaults.GOOSE_HATCH_TEMP;
                this.developmentHumidity = Defaults.GOOSE_DEV_HUMID;
                this.hatchingHumidity = Defaults.GOOSE_HATCH_HUMID;
                this.developmentDays = Defaults.GOOSE_DEV_DAYS;
                this.hatchingDays = Defaults.GOOSE_HATCH_DAYS;
                this.motorWaitTime = Defaults.GOOSE_MOTOR_WAIT;
                this.motorRunTime = Defaults.GOOSE_MOTOR_RUN;
                break;

            case TYPE_MANUAL:
                // Manuel için varsayılan tavuk değerlerini kullan
                this.developmentTemperature = Defaults.CHICKEN_DEV_TEMP;
                this.hatchingTemperature = Defaults.CHICKEN_HATCH_TEMP;
                this.developmentHumidity = Defaults.CHICKEN_DEV_HUMID;
                this.hatchingHumidity = Defaults.CHICKEN_HATCH_HUMID;
                this.developmentDays = Defaults.CHICKEN_DEV_DAYS;
                this.hatchingDays = Defaults.CHICKEN_HATCH_DAYS;
                this.motorWaitTime = Defaults.CHICKEN_MOTOR_WAIT;
                this.motorRunTime = Defaults.CHICKEN_MOTOR_RUN;
                break;
        }

        this.totalDays = this.developmentDays + this.hatchingDays;
    }

    // Getters and Setters
    public int getType() { return type; }
    public void setType(int type) {
        this.type = type;
        setDefaultsForType(type);
    }

    public float getDevelopmentTemperature() { return developmentTemperature; }
    public void setDevelopmentTemperature(float developmentTemperature) {
        this.developmentTemperature = developmentTemperature;
    }

    public float getHatchingTemperature() { return hatchingTemperature; }
    public void setHatchingTemperature(float hatchingTemperature) {
        this.hatchingTemperature = hatchingTemperature;
    }

    public int getDevelopmentHumidity() { return developmentHumidity; }
    public void setDevelopmentHumidity(int developmentHumidity) {
        this.developmentHumidity = developmentHumidity;
    }

    public int getHatchingHumidity() { return hatchingHumidity; }
    public void setHatchingHumidity(int hatchingHumidity) {
        this.hatchingHumidity = hatchingHumidity;
    }

    public int getDevelopmentDays() { return developmentDays; }
    public void setDevelopmentDays(int developmentDays) {
        this.developmentDays = developmentDays;
        this.totalDays = this.developmentDays + this.hatchingDays;
    }

    public int getHatchingDays() { return hatchingDays; }
    public void setHatchingDays(int hatchingDays) {
        this.hatchingDays = hatchingDays;
        this.totalDays = this.developmentDays + this.hatchingDays;
    }

    public int getTotalDays() { return totalDays; }

    public int getMotorWaitTime() { return motorWaitTime; }
    public void setMotorWaitTime(int motorWaitTime) {
        this.motorWaitTime = motorWaitTime;
    }

    public int getMotorRunTime() { return motorRunTime; }
    public void setMotorRunTime(int motorRunTime) {
        this.motorRunTime = motorRunTime;
    }

    public String getTypeName() {
        switch (type) {
            case TYPE_CHICKEN: return "Tavuk";
            case TYPE_QUAIL: return "Bıldırcın";
            case TYPE_GOOSE: return "Kaz";
            case TYPE_MANUAL: return "Manuel";
            default: return "Bilinmeyen";
        }
    }
}