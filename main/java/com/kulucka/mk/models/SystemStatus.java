package com.kulucka.mk.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * ESP32'den gelen sistem durumu verilerini tutan ana model sınıfı
 * /api/status endpoint'inden dönen JSON verilerini karşılar
 */
public class SystemStatus implements Serializable {

    // Temel sensör verileri
    @SerializedName("temperature")
    private double temperature;

    @SerializedName("humidity")
    private double humidity;

    @SerializedName("targetTemp")
    private double targetTemp;

    @SerializedName("targetHumid")
    private double targetHumid;

    // Kontrol durumları
    @SerializedName("heaterState")
    private boolean heaterState;

    @SerializedName("humidifierState")
    private boolean humidifierState;

    @SerializedName("motorState")
    private boolean motorState;

    // Kuluçka bilgileri
    @SerializedName("currentDay")
    private int currentDay;

    @SerializedName("totalDays")
    private int totalDays;

    @SerializedName("actualDay")
    private int actualDay;

    @SerializedName("displayDay")
    private int displayDay;

    @SerializedName("incubationType")
    private String incubationType;

    @SerializedName("isIncubationRunning")
    private boolean isIncubationRunning;

    @SerializedName("isIncubationCompleted")
    private boolean isIncubationCompleted;

    // PID kontrol bilgileri
    @SerializedName("pidMode")
    private int pidMode;

    @SerializedName("pidKp")
    private double pidKp;

    @SerializedName("pidKi")
    private double pidKi;

    @SerializedName("pidKd")
    private double pidKd;

    @SerializedName("pidActive")
    private boolean pidActive;

    @SerializedName("pidAutoTuneActive")
    private boolean pidAutoTuneActive;

    @SerializedName("pidError")
    private double pidError;

    @SerializedName("pidOutput")
    private double pidOutput;

    @SerializedName("pidModeString")
    private String pidModeString;

    @SerializedName("autoTuneProgress")
    private double autoTuneProgress;

    @SerializedName("autoTuneFinished")
    private boolean autoTuneFinished;

    // Alarm ayarları
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

    // Motor ayarları
    @SerializedName("motorWaitTime")
    private int motorWaitTime;

    @SerializedName("motorRunTime")
    private int motorRunTime;

    // Kalibrasyon verileri
    @SerializedName("tempCalibration1")
    private double tempCalibration1;

    @SerializedName("tempCalibration2")
    private double tempCalibration2;

    @SerializedName("humidCalibration1")
    private double humidCalibration1;

    @SerializedName("humidCalibration2")
    private double humidCalibration2;

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

    // WiFi ve sistem bilgileri
    @SerializedName("wifiStatus")
    private String wifiStatus;

    @SerializedName("ipAddress")
    private String ipAddress;

    @SerializedName("wifiMode")
    private String wifiMode;

    @SerializedName("ssid")
    private String ssid;

    @SerializedName("signalStrength")
    private int signalStrength;

    // Sistem performans bilgileri
    @SerializedName("timestamp")
    private long timestamp;

    @SerializedName("freeHeap")
    private long freeHeap;

    @SerializedName("uptime")
    private long uptime;

    // Varsayılan constructor
    public SystemStatus() {}

    // Getter metodları
    public double getTemperature() { return temperature; }
    public double getHumidity() { return humidity; }
    public double getTargetTemp() { return targetTemp; }
    public double getTargetHumid() { return targetHumid; }

    public boolean isHeaterState() { return heaterState; }
    public boolean isHumidifierState() { return humidifierState; }
    public boolean isMotorState() { return motorState; }

    public int getCurrentDay() { return currentDay; }
    public int getTotalDays() { return totalDays; }
    public int getActualDay() { return actualDay; }
    public int getDisplayDay() { return displayDay; }
    public String getIncubationType() { return incubationType; }
    public boolean isIncubationRunning() { return isIncubationRunning; }
    public boolean isIncubationCompleted() { return isIncubationCompleted; }

    public int getPidMode() { return pidMode; }
    public double getPidKp() { return pidKp; }
    public double getPidKi() { return pidKi; }
    public double getPidKd() { return pidKd; }
    public boolean isPidActive() { return pidActive; }
    public boolean isPidAutoTuneActive() { return pidAutoTuneActive; }
    public double getPidError() { return pidError; }
    public double getPidOutput() { return pidOutput; }
    public String getPidModeString() { return pidModeString; }
    public double getAutoTuneProgress() { return autoTuneProgress; }
    public boolean isAutoTuneFinished() { return autoTuneFinished; }

    public boolean isAlarmEnabled() { return alarmEnabled; }
    public double getTempLowAlarm() { return tempLowAlarm; }
    public double getTempHighAlarm() { return tempHighAlarm; }
    public double getHumidLowAlarm() { return humidLowAlarm; }
    public double getHumidHighAlarm() { return humidHighAlarm; }

    public int getMotorWaitTime() { return motorWaitTime; }
    public int getMotorRunTime() { return motorRunTime; }

    public double getTempCalibration1() { return tempCalibration1; }
    public double getTempCalibration2() { return tempCalibration2; }
    public double getHumidCalibration1() { return humidCalibration1; }
    public double getHumidCalibration2() { return humidCalibration2; }

    public double getManualDevTemp() { return manualDevTemp; }
    public double getManualHatchTemp() { return manualHatchTemp; }
    public int getManualDevHumid() { return manualDevHumid; }
    public int getManualHatchHumid() { return manualHatchHumid; }
    public int getManualDevDays() { return manualDevDays; }
    public int getManualHatchDays() { return manualHatchDays; }

    public String getWifiStatus() { return wifiStatus; }
    public String getIpAddress() { return ipAddress; }
    public String getWifiMode() { return wifiMode; }
    public String getSsid() { return ssid; }
    public int getSignalStrength() { return signalStrength; }

    public long getTimestamp() { return timestamp; }
    public long getFreeHeap() { return freeHeap; }
    public long getUptime() { return uptime; }

    // Setter metodları
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public void setHumidity(double humidity) { this.humidity = humidity; }
    public void setTargetTemp(double targetTemp) { this.targetTemp = targetTemp; }
    public void setTargetHumid(double targetHumid) { this.targetHumid = targetHumid; }

    public void setHeaterState(boolean heaterState) { this.heaterState = heaterState; }
    public void setHumidifierState(boolean humidifierState) { this.humidifierState = humidifierState; }
    public void setMotorState(boolean motorState) { this.motorState = motorState; }

    public void setCurrentDay(int currentDay) { this.currentDay = currentDay; }
    public void setTotalDays(int totalDays) { this.totalDays = totalDays; }
    public void setActualDay(int actualDay) { this.actualDay = actualDay; }
    public void setDisplayDay(int displayDay) { this.displayDay = displayDay; }
    public void setIncubationType(String incubationType) { this.incubationType = incubationType; }
    public void setIncubationRunning(boolean incubationRunning) { this.isIncubationRunning = incubationRunning; }
    public void setIncubationCompleted(boolean incubationCompleted) { this.isIncubationCompleted = incubationCompleted; }

    public void setPidMode(int pidMode) { this.pidMode = pidMode; }
    public void setPidKp(double pidKp) { this.pidKp = pidKp; }
    public void setPidKi(double pidKi) { this.pidKi = pidKi; }
    public void setPidKd(double pidKd) { this.pidKd = pidKd; }
    public void setPidActive(boolean pidActive) { this.pidActive = pidActive; }
    public void setPidAutoTuneActive(boolean pidAutoTuneActive) { this.pidAutoTuneActive = pidAutoTuneActive; }
    public void setPidError(double pidError) { this.pidError = pidError; }
    public void setPidOutput(double pidOutput) { this.pidOutput = pidOutput; }
    public void setPidModeString(String pidModeString) { this.pidModeString = pidModeString; }
    public void setAutoTuneProgress(double autoTuneProgress) { this.autoTuneProgress = autoTuneProgress; }
    public void setAutoTuneFinished(boolean autoTuneFinished) { this.autoTuneFinished = autoTuneFinished; }

    public void setAlarmEnabled(boolean alarmEnabled) { this.alarmEnabled = alarmEnabled; }
    public void setTempLowAlarm(double tempLowAlarm) { this.tempLowAlarm = tempLowAlarm; }
    public void setTempHighAlarm(double tempHighAlarm) { this.tempHighAlarm = tempHighAlarm; }
    public void setHumidLowAlarm(double humidLowAlarm) { this.humidLowAlarm = humidLowAlarm; }
    public void setHumidHighAlarm(double humidHighAlarm) { this.humidHighAlarm = humidHighAlarm; }

    public void setMotorWaitTime(int motorWaitTime) { this.motorWaitTime = motorWaitTime; }
    public void setMotorRunTime(int motorRunTime) { this.motorRunTime = motorRunTime; }

    public void setTempCalibration1(double tempCalibration1) { this.tempCalibration1 = tempCalibration1; }
    public void setTempCalibration2(double tempCalibration2) { this.tempCalibration2 = tempCalibration2; }
    public void setHumidCalibration1(double humidCalibration1) { this.humidCalibration1 = humidCalibration1; }
    public void setHumidCalibration2(double humidCalibration2) { this.humidCalibration2 = humidCalibration2; }

    public void setManualDevTemp(double manualDevTemp) { this.manualDevTemp = manualDevTemp; }
    public void setManualHatchTemp(double manualHatchTemp) { this.manualHatchTemp = manualHatchTemp; }
    public void setManualDevHumid(int manualDevHumid) { this.manualDevHumid = manualDevHumid; }
    public void setManualHatchHumid(int manualHatchHumid) { this.manualHatchHumid = manualHatchHumid; }
    public void setManualDevDays(int manualDevDays) { this.manualDevDays = manualDevDays; }
    public void setManualHatchDays(int manualHatchDays) { this.manualHatchDays = manualHatchDays; }

    public void setWifiStatus(String wifiStatus) { this.wifiStatus = wifiStatus; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public void setWifiMode(String wifiMode) { this.wifiMode = wifiMode; }
    public void setSsid(String ssid) { this.ssid = ssid; }
    public void setSignalStrength(int signalStrength) { this.signalStrength = signalStrength; }

    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setFreeHeap(long freeHeap) { this.freeHeap = freeHeap; }
    public void setUptime(long uptime) { this.uptime = uptime; }

    // Yardımcı metodlar
    public boolean isConnected() {
        return wifiStatus != null &&
                (wifiStatus.contains("Bağlı") || wifiStatus.contains("AP Modu"));
    }

    public boolean isAPMode() {
        return "AP".equals(wifiMode);
    }

    public boolean isStationMode() {
        return "Station".equals(wifiMode);
    }

    public String getFormattedTemperature() {
        return String.format("%.1f°C", temperature);
    }

    public String getFormattedHumidity() {
        return String.format("%.0f%%", humidity);
    }

    public String getFormattedTargetTemp() {
        return String.format("%.1f°C", targetTemp);
    }

    public String getFormattedTargetHumid() {
        return String.format("%.0f%%", targetHumid);
    }

    public String getFormattedDays() {
        return String.format("%d/%d gün", displayDay, totalDays);
    }

    public String getFormattedMotorWaitTime() {
        return String.format("%d dakika", motorWaitTime);
    }

    public String getFormattedMotorRunTime() {
        return String.format("%d saniye", motorRunTime);
    }

    public String getFormattedUptime() {
        long hours = uptime / 3600;
        long minutes = (uptime % 3600) / 60;
        long seconds = uptime % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public String getFormattedFreeHeap() {
        if (freeHeap < 1024) {
            return freeHeap + " B";
        } else if (freeHeap < 1024 * 1024) {
            return String.format("%.1f KB", freeHeap / 1024.0);
        } else {
            return String.format("%.1f MB", freeHeap / (1024.0 * 1024.0));
        }
    }

    public boolean hasAlarmCondition() {
        if (!alarmEnabled) return false;

        return (temperature < tempLowAlarm || temperature > tempHighAlarm ||
                humidity < humidLowAlarm || humidity > humidHighAlarm);
    }

    public String getAlarmMessage() {
        if (!hasAlarmCondition()) return null;

        StringBuilder message = new StringBuilder("ALARM: ");
        if (temperature < tempLowAlarm) {
            message.append("Düşük sıcaklık (").append(getFormattedTemperature()).append(") ");
        }
        if (temperature > tempHighAlarm) {
            message.append("Yüksek sıcaklık (").append(getFormattedTemperature()).append(") ");
        }
        if (humidity < humidLowAlarm) {
            message.append("Düşük nem (").append(getFormattedHumidity()).append(") ");
        }
        if (humidity > humidHighAlarm) {
            message.append("Yüksek nem (").append(getFormattedHumidity()).append(") ");
        }

        return message.toString().trim();
    }

    @Override
    public String toString() {
        return "SystemStatus{" +
                "temperature=" + temperature +
                ", humidity=" + humidity +
                ", targetTemp=" + targetTemp +
                ", targetHumid=" + targetHumid +
                ", heaterState=" + heaterState +
                ", humidifierState=" + humidifierState +
                ", motorState=" + motorState +
                ", currentDay=" + currentDay +
                ", totalDays=" + totalDays +
                ", incubationType='" + incubationType + '\'' +
                ", isIncubationRunning=" + isIncubationRunning +
                ", wifiStatus='" + wifiStatus + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                '}';
    }
}