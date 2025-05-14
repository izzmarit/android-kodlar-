package com.kulucka.kontrol.models;

import java.io.Serializable;
import java.util.List;

public class AlarmData implements Serializable {
    private static final long serialVersionUID = 1L;
    private boolean alarmActive;
    private int alarmType;
    private String alarmMessage;
    private long alarmStartTime;
    private Thresholds thresholds;
    private List<AlarmHistory> history;

    public static class Thresholds implements Serializable {
        private static final long serialVersionUID = 1L;
        private boolean enabled;
        private float highTemp;
        private float lowTemp;
        private float highHum;
        private float lowHum;
        private float tempDiff;
        private float humDiff;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public float getHighTemp() {
            return highTemp;
        }

        public void setHighTemp(float highTemp) {
            this.highTemp = highTemp;
        }

        public float getLowTemp() {
            return lowTemp;
        }

        public void setLowTemp(float lowTemp) {
            this.lowTemp = lowTemp;
        }

        public float getHighHum() {
            return highHum;
        }

        public void setHighHum(float highHum) {
            this.highHum = highHum;
        }

        public float getLowHum() {
            return lowHum;
        }

        public void setLowHum(float lowHum) {
            this.lowHum = lowHum;
        }

        public float getTempDiff() {
            return tempDiff;
        }

        public void setTempDiff(float tempDiff) {
            this.tempDiff = tempDiff;
        }

        public float getHumDiff() {
            return humDiff;
        }

        public void setHumDiff(float humDiff) {
            this.humDiff = humDiff;
        }
    }

    public static class AlarmHistory implements Serializable {
        private static final long serialVersionUID = 1L;
        private int type;
        private String message;
        private long time;

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public long getTime() {
            return time;
        }

        public void setTime(long time) {
            this.time = time;
        }
    }

    // Alarm türleri (ESP32 kodundaki enum AlarmType ile uyumlu)
    public static final int ALARM_NONE = 0;
    public static final int ALARM_HIGH_TEMP = 1;
    public static final int ALARM_LOW_TEMP = 2;
    public static final int ALARM_HIGH_HUMIDITY = 3;
    public static final int ALARM_LOW_HUMIDITY = 4;
    public static final int ALARM_SENSOR_ERROR = 5;
    public static final int ALARM_POWER_OUTAGE = 6;
    public static final int ALARM_TEMP_DIFF = 7;
    public static final int ALARM_HUM_DIFF = 8;

    // Getters ve setters

    public boolean isAlarmActive() {
        return alarmActive;
    }

    public void setAlarmActive(boolean alarmActive) {
        this.alarmActive = alarmActive;
    }

    public int getAlarmType() {
        return alarmType;
    }

    public void setAlarmType(int alarmType) {
        this.alarmType = alarmType;
    }

    public String getAlarmMessage() {
        return alarmMessage;
    }

    public void setAlarmMessage(String alarmMessage) {
        this.alarmMessage = alarmMessage;
    }

    public long getAlarmStartTime() {
        return alarmStartTime;
    }

    public void setAlarmStartTime(long alarmStartTime) {
        this.alarmStartTime = alarmStartTime;
    }

    public Thresholds getThresholds() {
        return thresholds;
    }

    public void setThresholds(Thresholds thresholds) {
        this.thresholds = thresholds;
    }

    public List<AlarmHistory> getHistory() {
        return history;
    }

    public void setHistory(List<AlarmHistory> history) {
        this.history = history;
    }

    public String getAlarmTypeString() {
        switch (alarmType) {
            case ALARM_NONE: return "Yok";
            case ALARM_HIGH_TEMP: return "Yüksek Sıcaklık";
            case ALARM_LOW_TEMP: return "Düşük Sıcaklık";
            case ALARM_HIGH_HUMIDITY: return "Yüksek Nem";
            case ALARM_LOW_HUMIDITY: return "Düşük Nem";
            case ALARM_SENSOR_ERROR: return "Sensör Hatası";
            case ALARM_POWER_OUTAGE: return "Elektrik Kesintisi";
            case ALARM_TEMP_DIFF: return "Sıcaklık Farkı";
            case ALARM_HUM_DIFF: return "Nem Farkı";
            default: return "Bilinmeyen";
        }
    }
}