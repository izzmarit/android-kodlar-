package com.kulucka.kontrol.models;

import java.io.Serializable;
import java.util.List;

public class Profile implements Serializable {
    private static final long serialVersionUID = 1L;
    private int type;
    private String name;
    private int totalDays;
    private List<Stage> stages;

    public static class Stage implements Serializable {
        private static final long serialVersionUID = 1L;
        private float temperature;
        private float humidity;
        private boolean motorActive;
        private int startDay;
        private int endDay;

        public float getTemperature() {
            return temperature;
        }

        public void setTemperature(float temperature) {
            this.temperature = temperature;
        }

        public float getHumidity() {
            return humidity;
        }

        public void setHumidity(float humidity) {
            this.humidity = humidity;
        }

        public boolean isMotorActive() {
            return motorActive;
        }

        public void setMotorActive(boolean motorActive) {
            this.motorActive = motorActive;
        }

        public int getStartDay() {
            return startDay;
        }

        public void setStartDay(int startDay) {
            this.startDay = startDay;
        }

        public int getEndDay() {
            return endDay;
        }

        public void setEndDay(int endDay) {
            this.endDay = endDay;
        }
    }

    // Getters ve setters

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(int totalDays) {
        this.totalDays = totalDays;
    }

    public List<Stage> getStages() {
        return stages;
    }

    public void setStages(List<Stage> stages) {
        this.stages = stages;
    }
}