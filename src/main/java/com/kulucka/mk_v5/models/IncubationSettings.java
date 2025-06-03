package com.kulucka.mk_v5.models;

public class IncubationSettings {
    private int incubationType;
    private float manualDevTemp;
    private float manualHatchTemp;
    private int manualDevHumid;
    private int manualHatchHumid;
    private int manualDevDays;
    private int manualHatchDays;
    private boolean isIncubationRunning;

    // Yapılandırıcı
    public IncubationSettings() {
    }

    // Getters ve Setters
    public int getIncubationType() {
        return incubationType;
    }

    public void setIncubationType(int incubationType) {
        this.incubationType = incubationType;
    }

    public float getManualDevTemp() {
        return manualDevTemp;
    }

    public void setManualDevTemp(float manualDevTemp) {
        this.manualDevTemp = manualDevTemp;
    }

    public float getManualHatchTemp() {
        return manualHatchTemp;
    }

    public void setManualHatchTemp(float manualHatchTemp) {
        this.manualHatchTemp = manualHatchTemp;
    }

    public int getManualDevHumid() {
        return manualDevHumid;
    }

    public void setManualDevHumid(int manualDevHumid) {
        this.manualDevHumid = manualDevHumid;
    }

    public int getManualHatchHumid() {
        return manualHatchHumid;
    }

    public void setManualHatchHumid(int manualHatchHumid) {
        this.manualHatchHumid = manualHatchHumid;
    }

    public int getManualDevDays() {
        return manualDevDays;
    }

    public void setManualDevDays(int manualDevDays) {
        this.manualDevDays = manualDevDays;
    }

    public int getManualHatchDays() {
        return manualHatchDays;
    }

    public void setManualHatchDays(int manualHatchDays) {
        this.manualHatchDays = manualHatchDays;
    }

    public boolean isIncubationRunning() {
        return isIncubationRunning;
    }

    public void setIncubationRunning(boolean incubationRunning) {
        isIncubationRunning = incubationRunning;
    }
}