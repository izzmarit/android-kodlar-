package com.kulucka.mkv5.models;

import com.google.gson.annotations.SerializedName;

public class DeviceStatus {
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

    @SerializedName("isIncubationRunning")
    private boolean isIncubationRunning;

    @SerializedName("isIncubationCompleted")
    private boolean isIncubationCompleted;

    @SerializedName("actualDay")
    private int actualDay;

    @SerializedName("displayDay")
    private int displayDay;

    @SerializedName("pidMode")
    private int pidMode;

    @SerializedName("pidKp")
    private float pidKp;

    @SerializedName("pidKi")
    private float pidKi;

    @SerializedName("pidKd")
    private float pidKd;

    @SerializedName("pidActive")
    private boolean pidActive;

    @SerializedName("pidAutoTuneActive")
    private boolean pidAutoTuneActive;

    @SerializedName("pidError")
    private float pidError;

    @SerializedName("pidOutput")
    private float pidOutput;

    @SerializedName("pidModeString")
    private String pidModeString;

    @SerializedName("alarmEnabled")
    private boolean alarmEnabled;

    @SerializedName("tempLowAlarm")
    private float tempLowAlarm;

    @SerializedName("tempHighAlarm")
    private float tempHighAlarm;

    @SerializedName("humidLowAlarm")
    private float humidLowAlarm;

    @SerializedName("humidHighAlarm")
    private float humidHighAlarm;

    @SerializedName("motorWaitTime")
    private int motorWaitTime;

    @SerializedName("motorRunTime")
    private int motorRunTime;

    @SerializedName("tempCalibration1")
    private float tempCalibration1;

    @SerializedName("tempCalibration2")
    private float tempCalibration2;

    @SerializedName("humidCalibration1")
    private float humidCalibration1;

    @SerializedName("humidCalibration2")
    private float humidCalibration2;

    @SerializedName("manualDevTemp")
    private float manualDevTemp;

    @SerializedName("manualHatchTemp")
    private float manualHatchTemp;

    @SerializedName("manualDevHumid")
    private int manualDevHumid;

    @SerializedName("manualHatchHumid")
    private int manualHatchHumid;

    @SerializedName("manualDevDays")
    private int manualDevDays;

    @SerializedName("manualHatchDays")
    private int manualHatchDays;

    @SerializedName("timestamp")
    private long timestamp;

    @SerializedName("freeHeap")
    private long freeHeap;

    @SerializedName("uptime")
    private long uptime;

    @SerializedName("motor")
    private MotorInfo motor;

    @SerializedName("reliability")
    private ReliabilityInfo reliability;

    // Motor bilgileri için sınıf
    public static class MotorInfo {
        @SerializedName("state")
        private boolean state;

        @SerializedName("waitTime")
        private int waitTime;

        @SerializedName("runTime")
        private int runTime;

        @SerializedName("testAvailable")
        private boolean testAvailable;

        public boolean isState() { return state; }
        public void setState(boolean state) { this.state = state; }

        public int getWaitTime() { return waitTime; }
        public void setWaitTime(int waitTime) { this.waitTime = waitTime; }

        public int getRunTime() { return runTime; }
        public void setRunTime(int runTime) { this.runTime = runTime; }

        public boolean isTestAvailable() { return testAvailable; }
        public void setTestAvailable(boolean testAvailable) { this.testAvailable = testAvailable; }
    }

    // Güvenilirlik bilgileri için sınıf
    public static class ReliabilityInfo {
        @SerializedName("lastSave")
        private long lastSave;

        @SerializedName("pendingChanges")
        private int pendingChanges;

        @SerializedName("autoSaveEnabled")
        private boolean autoSaveEnabled;

        @SerializedName("criticalParamsProtected")
        private boolean criticalParamsProtected;

        public long getLastSave() { return lastSave; }
        public void setLastSave(long lastSave) { this.lastSave = lastSave; }

        public int getPendingChanges() { return pendingChanges; }
        public void setPendingChanges(int pendingChanges) { this.pendingChanges = pendingChanges; }

        public boolean isAutoSaveEnabled() { return autoSaveEnabled; }
        public void setAutoSaveEnabled(boolean autoSaveEnabled) {
            this.autoSaveEnabled = autoSaveEnabled;
        }

        public boolean isCriticalParamsProtected() { return criticalParamsProtected; }
        public void setCriticalParamsProtected(boolean criticalParamsProtected) {
            this.criticalParamsProtected = criticalParamsProtected;
        }
    }

    // Getters
    public float getTemperature() { return temperature; }
    public float getHumidity() { return humidity; }
    public boolean isHeaterState() { return heaterState; }
    public boolean isHumidifierState() { return humidifierState; }
    public boolean isMotorState() { return motorState; }
    public int getCurrentDay() { return currentDay; }
    public int getTotalDays() { return totalDays; }
    public String getIncubationType() { return incubationType; }
    public float getTargetTemp() { return targetTemp; }
    public float getTargetHumid() { return targetHumid; }
    public boolean isIncubationRunning() { return isIncubationRunning; }
    public boolean isIncubationCompleted() { return isIncubationCompleted; }
    public int getActualDay() { return actualDay; }
    public int getDisplayDay() { return displayDay; }
    public int getPidMode() { return pidMode; }
    public float getPidKp() { return pidKp; }
    public float getPidKi() { return pidKi; }
    public float getPidKd() { return pidKd; }
    public boolean isPidActive() { return pidActive; }
    public boolean isPidAutoTuneActive() { return pidAutoTuneActive; }
    public float getPidError() { return pidError; }
    public float getPidOutput() { return pidOutput; }
    public String getPidModeString() { return pidModeString; }
    public boolean isAlarmEnabled() { return alarmEnabled; }
    public float getTempLowAlarm() { return tempLowAlarm; }
    public float getTempHighAlarm() { return tempHighAlarm; }
    public float getHumidLowAlarm() { return humidLowAlarm; }
    public float getHumidHighAlarm() { return humidHighAlarm; }
    public int getMotorWaitTime() { return motorWaitTime; }
    public int getMotorRunTime() { return motorRunTime; }
    public float getTempCalibration1() { return tempCalibration1; }
    public float getTempCalibration2() { return tempCalibration2; }
    public float getHumidCalibration1() { return humidCalibration1; }
    public float getHumidCalibration2() { return humidCalibration2; }
    public float getManualDevTemp() { return manualDevTemp; }
    public float getManualHatchTemp() { return manualHatchTemp; }
    public int getManualDevHumid() { return manualDevHumid; }
    public int getManualHatchHumid() { return manualHatchHumid; }
    public int getManualDevDays() { return manualDevDays; }
    public int getManualHatchDays() { return manualHatchDays; }
    public long getTimestamp() { return timestamp; }
    public long getFreeHeap() { return freeHeap; }
    public long getUptime() { return uptime; }
    public MotorInfo getMotor() { return motor; }
    public ReliabilityInfo getReliability() { return reliability; }

    // Setters
    public void setTemperature(float temperature) { this.temperature = temperature; }
    public void setHumidity(float humidity) { this.humidity = humidity; }
    public void setHeaterState(boolean heaterState) { this.heaterState = heaterState; }
    public void setHumidifierState(boolean humidifierState) { this.humidifierState = humidifierState; }
    public void setMotorState(boolean motorState) { this.motorState = motorState; }
    public void setCurrentDay(int currentDay) { this.currentDay = currentDay; }
    public void setTotalDays(int totalDays) { this.totalDays = totalDays; }
    public void setIncubationType(String incubationType) { this.incubationType = incubationType; }
    public void setTargetTemp(float targetTemp) { this.targetTemp = targetTemp; }
    public void setTargetHumid(float targetHumid) { this.targetHumid = targetHumid; }
    public void setIncubationRunning(boolean incubationRunning) { isIncubationRunning = incubationRunning; }
    public void setIncubationCompleted(boolean incubationCompleted) { isIncubationCompleted = incubationCompleted; }
    public void setActualDay(int actualDay) { this.actualDay = actualDay; }
    public void setDisplayDay(int displayDay) { this.displayDay = displayDay; }
    public void setPidMode(int pidMode) { this.pidMode = pidMode; }
    public void setPidKp(float pidKp) { this.pidKp = pidKp; }
    public void setPidKi(float pidKi) { this.pidKi = pidKi; }
    public void setPidKd(float pidKd) { this.pidKd = pidKd; }
    public void setPidActive(boolean pidActive) { this.pidActive = pidActive; }
    public void setPidAutoTuneActive(boolean pidAutoTuneActive) { this.pidAutoTuneActive = pidAutoTuneActive; }
    public void setPidError(float pidError) { this.pidError = pidError; }
    public void setPidOutput(float pidOutput) { this.pidOutput = pidOutput; }
    public void setPidModeString(String pidModeString) { this.pidModeString = pidModeString; }
    public void setAlarmEnabled(boolean alarmEnabled) { this.alarmEnabled = alarmEnabled; }
    public void setTempLowAlarm(float tempLowAlarm) { this.tempLowAlarm = tempLowAlarm; }
    public void setTempHighAlarm(float tempHighAlarm) { this.tempHighAlarm = tempHighAlarm; }
    public void setHumidLowAlarm(float humidLowAlarm) { this.humidLowAlarm = humidLowAlarm; }
    public void setHumidHighAlarm(float humidHighAlarm) { this.humidHighAlarm = humidHighAlarm; }
    public void setMotorWaitTime(int motorWaitTime) { this.motorWaitTime = motorWaitTime; }
    public void setMotorRunTime(int motorRunTime) { this.motorRunTime = motorRunTime; }
    public void setTempCalibration1(float tempCalibration1) { this.tempCalibration1 = tempCalibration1; }
    public void setTempCalibration2(float tempCalibration2) { this.tempCalibration2 = tempCalibration2; }
    public void setHumidCalibration1(float humidCalibration1) { this.humidCalibration1 = humidCalibration1; }
    public void setHumidCalibration2(float humidCalibration2) { this.humidCalibration2 = humidCalibration2; }
    public void setManualDevTemp(float manualDevTemp) { this.manualDevTemp = manualDevTemp; }
    public void setManualHatchTemp(float manualHatchTemp) { this.manualHatchTemp = manualHatchTemp; }
    public void setManualDevHumid(int manualDevHumid) { this.manualDevHumid = manualDevHumid; }
    public void setManualHatchHumid(int manualHatchHumid) { this.manualHatchHumid = manualHatchHumid; }
    public void setManualDevDays(int manualDevDays) { this.manualDevDays = manualDevDays; }
    public void setManualHatchDays(int manualHatchDays) { this.manualHatchDays = manualHatchDays; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setFreeHeap(long freeHeap) { this.freeHeap = freeHeap; }
    public void setUptime(long uptime) { this.uptime = uptime; }
    public void setMotor(MotorInfo motor) { this.motor = motor; }
    public void setReliability(ReliabilityInfo reliability) { this.reliability = reliability; }
}