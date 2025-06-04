package com.kulucka.mk_v5.models;

public class PidSettings {
    // Sadece sıcaklık PID parametreleri - ESP32'deki DEFAULT değerlerle uyumlu
    private float pidKp;
    private float pidKi;
    private float pidKd;

    // PID kontrol durumu
    private boolean pidEnabled;
    private boolean autoTuning;

    // Gelişmiş PID ayarları
    private int sampleTime;
    private float outputMin;
    private float outputMax;
    private boolean reverseDirection;

    // PID performans verileri
    private float tempError;
    private float tempOutput;
    private float tempSetpoint;
    private float tempCurrent;

    // Son ayarlama zamanı
    private long lastTuneTime;

    public PidSettings() {
        // ESP32'deki DEFAULT_TEMP_* değerleriyle uyumlu varsayılan değerler
        pidKp = 2.0f;
        pidKi = 0.1f;
        pidKd = 0.5f;

        // Varsayılan kontrol durumu
        pidEnabled = false;
        autoTuning = false;

        // Varsayılan gelişmiş ayarlar
        sampleTime = 1000; // 1 saniye
        outputMin = 0.0f;
        outputMax = 100.0f;
        reverseDirection = false;

        // Varsayılan performans değerleri
        tempError = 0.0f;
        tempOutput = 0.0f;
        tempSetpoint = 37.5f;
        tempCurrent = 0.0f;

        lastTuneTime = 0;
    }

    // Sıcaklık PID parametresi getter/setter metodları
    public float getPidKp() {
        return pidKp;
    }

    public void setPidKp(float pidKp) {
        this.pidKp = Math.max(0.0f, Math.min(10.0f, pidKp)); // 0-10 aralığında sınırla
    }

    public float getPidKi() {
        return pidKi;
    }

    public void setPidKi(float pidKi) {
        this.pidKi = Math.max(0.0f, Math.min(1.0f, pidKi)); // 0-1 aralığında sınırla
    }

    public float getPidKd() {
        return pidKd;
    }

    public void setPidKd(float pidKd) {
        this.pidKd = Math.max(0.0f, Math.min(5.0f, pidKd)); // 0-5 aralığında sınırla
    }

    // PID kontrol durumu getter/setter metodları
    public boolean isPidEnabled() {
        return pidEnabled;
    }

    public void setPidEnabled(boolean pidEnabled) {
        this.pidEnabled = pidEnabled;
    }

    public boolean isAutoTuning() {
        return autoTuning;
    }

    public void setAutoTuning(boolean autoTuning) {
        this.autoTuning = autoTuning;
    }

    // Gelişmiş PID ayarları getter/setter metodları
    public int getSampleTime() {
        return sampleTime;
    }

    public void setSampleTime(int sampleTime) {
        this.sampleTime = Math.max(100, Math.min(10000, sampleTime)); // 100ms-10s aralığında
    }

    public float getOutputMin() {
        return outputMin;
    }

    public void setOutputMin(float outputMin) {
        this.outputMin = Math.max(0.0f, Math.min(100.0f, outputMin));
    }

    public float getOutputMax() {
        return outputMax;
    }

    public void setOutputMax(float outputMax) {
        this.outputMax = Math.max(0.0f, Math.min(100.0f, outputMax));
    }

    public boolean isReverseDirection() {
        return reverseDirection;
    }

    public void setReverseDirection(boolean reverseDirection) {
        this.reverseDirection = reverseDirection;
    }

    // PID performans verileri getter/setter metodları
    public float getTempError() {
        return tempError;
    }

    public void setTempError(float tempError) {
        this.tempError = tempError;
    }

    public float getTempOutput() {
        return tempOutput;
    }

    public void setTempOutput(float tempOutput) {
        this.tempOutput = Math.max(0.0f, Math.min(100.0f, tempOutput));
    }

    public float getTempSetpoint() {
        return tempSetpoint;
    }

    public void setTempSetpoint(float tempSetpoint) {
        this.tempSetpoint = tempSetpoint;
    }

    public float getTempCurrent() {
        return tempCurrent;
    }

    public void setTempCurrent(float tempCurrent) {
        this.tempCurrent = tempCurrent;
    }

    public long getLastTuneTime() {
        return lastTuneTime;
    }

    public void setLastTuneTime(long lastTuneTime) {
        this.lastTuneTime = lastTuneTime;
    }

    // Yardımcı metodlar
    public void resetToDefaults() {
        setPidKp(2.0f);
        setPidKi(0.1f);
        setPidKd(0.5f);
        setPidEnabled(false);
        setAutoTuning(false);
        setSampleTime(1000);
        setOutputMin(0.0f);
        setOutputMax(100.0f);
        setReverseDirection(false);
        setLastTuneTime(0);
    }

    public boolean isValidConfiguration() {
        // Konfigürasyonun geçerliliğini kontrol et
        if (pidKp < 0 || pidKp > 10) return false;
        if (pidKi < 0 || pidKi > 1) return false;
        if (pidKd < 0 || pidKd > 5) return false;
        if (sampleTime < 100 || sampleTime > 10000) return false;
        if (outputMin < 0 || outputMin > 100) return false;
        if (outputMax < 0 || outputMax > 100) return false;
        if (outputMin >= outputMax) return false;

        return true;
    }

    public float calculatePerformanceScore() {
        // PID performans skoru hesapla (0-100 arasında)
        float absError = Math.abs(tempError);

        if (absError <= 0.2f) return 100.0f; // Mükemmel
        else if (absError <= 0.5f) return 80.0f; // İyi
        else if (absError <= 1.0f) return 60.0f; // Orta
        else if (absError <= 2.0f) return 40.0f; // Zayıf
        else return 20.0f; // Kötü
    }

    public String getPerformanceStatus() {
        // PID performans durumu metni
        float absError = Math.abs(tempError);

        if (absError <= 0.2f) return "Mükemmel";
        else if (absError <= 0.5f) return "İyi";
        else if (absError <= 1.0f) return "Orta";
        else if (absError <= 2.0f) return "Ayar Gerekli";
        else return "Kötü - Ayar Şart";
    }

    public boolean isStableOutput() {
        // Çıkışın kararlı olup olmadığını kontrol et
        return Math.abs(tempError) < 0.5f;
    }

    // ESP32'den gelen JSON verisini PidSettings nesnesine dönüştür
    public void updateFromJson(org.json.JSONObject jsonData) {
        try {
            // PID parametreleri
            if (jsonData.has("tempKp")) {
                setPidKp((float) jsonData.getDouble("tempKp"));
            }
            if (jsonData.has("tempKi")) {
                setPidKi((float) jsonData.getDouble("tempKi"));
            }
            if (jsonData.has("tempKd")) {
                setPidKd((float) jsonData.getDouble("tempKd"));
            }

            // PID durumu
            if (jsonData.has("pidEnabled")) {
                setPidEnabled(jsonData.getBoolean("pidEnabled"));
            }
            if (jsonData.has("autoTuning")) {
                setAutoTuning(jsonData.getBoolean("autoTuning"));
            }

            // Gelişmiş ayarlar
            if (jsonData.has("sampleTime")) {
                setSampleTime(jsonData.getInt("sampleTime"));
            }
            if (jsonData.has("outputMin")) {
                setOutputMin((float) jsonData.getDouble("outputMin"));
            }
            if (jsonData.has("outputMax")) {
                setOutputMax((float) jsonData.getDouble("outputMax"));
            }
            if (jsonData.has("reverseDirection")) {
                setReverseDirection(jsonData.getBoolean("reverseDirection"));
            }

            // Performans verileri
            if (jsonData.has("tempError")) {
                setTempError((float) jsonData.getDouble("tempError"));
            }
            if (jsonData.has("tempOutput")) {
                setTempOutput((float) jsonData.getDouble("tempOutput"));
            }
            if (jsonData.has("tempSetpoint")) {
                setTempSetpoint((float) jsonData.getDouble("tempSetpoint"));
            }
            if (jsonData.has("tempCurrent")) {
                setTempCurrent((float) jsonData.getDouble("tempCurrent"));
            }

            // Son ayarlama zamanı
            if (jsonData.has("lastTuneTime")) {
                setLastTuneTime(jsonData.getLong("lastTuneTime"));
            }

        } catch (org.json.JSONException e) {
            // JSON parse hatası - varsayılan değerleri koru
        }
    }

    // PidSettings nesnesini ESP32'ye gönderilecek JSON formatına dönüştür
    public org.json.JSONObject toJson() {
        try {
            org.json.JSONObject jsonObject = new org.json.JSONObject();

            // PID parametreleri
            jsonObject.put("tempKp", pidKp);
            jsonObject.put("tempKi", pidKi);
            jsonObject.put("tempKd", pidKd);

            // PID durumu
            jsonObject.put("pidEnabled", pidEnabled);
            jsonObject.put("autoTuning", autoTuning);

            // Gelişmiş ayarlar
            jsonObject.put("sampleTime", sampleTime);
            jsonObject.put("outputMin", outputMin);
            jsonObject.put("outputMax", outputMax);
            jsonObject.put("reverseDirection", reverseDirection);

            return jsonObject;
        } catch (org.json.JSONException e) {
            return new org.json.JSONObject();
        }
    }

    @Override
    public String toString() {
        return "PidSettings{" +
                "Kp=" + pidKp +
                ", Ki=" + pidKi +
                ", Kd=" + pidKd +
                ", enabled=" + pidEnabled +
                ", autoTuning=" + autoTuning +
                ", tempError=" + tempError +
                ", status='" + getPerformanceStatus() + '\'' +
                '}';
    }

    // PidSettings nesnesinin kopyasını oluştur
    public PidSettings copy() {
        PidSettings copy = new PidSettings();
        copy.setPidKp(this.pidKp);
        copy.setPidKi(this.pidKi);
        copy.setPidKd(this.pidKd);
        copy.setPidEnabled(this.pidEnabled);
        copy.setAutoTuning(this.autoTuning);
        copy.setSampleTime(this.sampleTime);
        copy.setOutputMin(this.outputMin);
        copy.setOutputMax(this.outputMax);
        copy.setReverseDirection(this.reverseDirection);
        copy.setTempError(this.tempError);
        copy.setTempOutput(this.tempOutput);
        copy.setTempSetpoint(this.tempSetpoint);
        copy.setTempCurrent(this.tempCurrent);
        copy.setLastTuneTime(this.lastTuneTime);
        return copy;
    }

    // İki PidSettings nesnesini karşılaştır
    public boolean equals(PidSettings other) {
        if (other == null) return false;

        return Float.compare(this.pidKp, other.pidKp) == 0 &&
                Float.compare(this.pidKi, other.pidKi) == 0 &&
                Float.compare(this.pidKd, other.pidKd) == 0 &&
                this.pidEnabled == other.pidEnabled &&
                this.autoTuning == other.autoTuning &&
                this.sampleTime == other.sampleTime &&
                Float.compare(this.outputMin, other.outputMin) == 0 &&
                Float.compare(this.outputMax, other.outputMax) == 0 &&
                this.reverseDirection == other.reverseDirection;
    }
}