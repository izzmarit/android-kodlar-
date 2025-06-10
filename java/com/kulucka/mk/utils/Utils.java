package com.kulucka.mk.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.kulucka.mk.R;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Uygulama genelinde kullanılan yardımcı fonksiyonlar
 */
public class Utils {

    private static final String TAG = "Utils";

    // Date formatters
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());

    // Number formatters
    private static final DecimalFormat TEMPERATURE_FORMAT = new DecimalFormat("#0.0");
    private static final DecimalFormat HUMIDITY_FORMAT = new DecimalFormat("#0");
    private static final DecimalFormat PID_FORMAT = new DecimalFormat("#0.00");

    /**
     * Toast mesajı gösterir
     */
    public static void showToast(Context context, String message) {
        if (context != null && !TextUtils.isEmpty(message)) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Uzun toast mesajı gösterir
     */
    public static void showLongToast(Context context, String message) {
        if (context != null && !TextUtils.isEmpty(message)) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Klavyeyi gizler
     */
    public static void hideKeyboard(Activity activity) {
        if (activity == null) return;

        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    /**
     * Klavyeyi gösterir
     */
    public static void showKeyboard(Context context, View view) {
        if (context == null || view == null) return;

        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    // === Formatlamalar ===

    /**
     * Sıcaklığı formatlar
     */
    public static String formatTemperature(double temperature) {
        return TEMPERATURE_FORMAT.format(temperature) + "°C";
    }

    /**
     * Nem değerini formatlar
     */
    public static String formatHumidity(double humidity) {
        return HUMIDITY_FORMAT.format(humidity) + "%";
    }

    /**
     * PID değerini formatlar
     */
    public static String formatPIDValue(double value) {
        return PID_FORMAT.format(value);
    }

    /**
     * Gün sayısını formatlar
     */
    public static String formatDays(int currentDay, int totalDays) {
        return currentDay + "/" + totalDays + " gün";
    }

    /**
     * Zamanı formatlar (saniye cinsinden uptime)
     */
    public static String formatUptime(long uptimeSeconds) {
        long hours = TimeUnit.SECONDS.toHours(uptimeSeconds);
        long minutes = TimeUnit.SECONDS.toMinutes(uptimeSeconds) % 60;
        long seconds = uptimeSeconds % 60;

        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Tarihi formatlar
     */
    public static String formatDate(long timestamp) {
        return DATE_FORMAT.format(new Date(timestamp));
    }

    /**
     * Zamanı formatlar
     */
    public static String formatTime(long timestamp) {
        return TIME_FORMAT.format(new Date(timestamp));
    }

    /**
     * Tarih ve zamanı formatlar
     */
    public static String formatDateTime(long timestamp) {
        return DATETIME_FORMAT.format(new Date(timestamp));
    }

    /**
     * Dosya boyutunu formatlar
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format(Locale.getDefault(), "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * Motor ayarlarını formatlar
     */
    public static String formatMotorSettings(int waitTimeMinutes, int runTimeSeconds) {
        return waitTimeMinutes + "dk / " + runTimeSeconds + "s";
    }

    // === Renk Hesaplamaları ===

    /**
     * Sıcaklık değerine göre renk döndürür
     */
    public static int getTemperatureColor(Context context, double temperature, double target) {
        if (context == null) return Color.BLACK;

        double diff = Math.abs(temperature - target);

        if (diff <= 0.5) {
            return ContextCompat.getColor(context, R.color.temp_normal);
        } else if (diff <= 1.0) {
            return ContextCompat.getColor(context, R.color.temp_warning);
        } else {
            return ContextCompat.getColor(context, R.color.temp_critical);
        }
    }

    /**
     * Nem değerine göre renk döndürür
     */
    public static int getHumidityColor(Context context, double humidity, double target) {
        if (context == null) return Color.BLACK;

        double diff = Math.abs(humidity - target);

        if (diff <= 3.0) {
            return ContextCompat.getColor(context, R.color.humid_normal);
        } else if (diff <= 6.0) {
            return ContextCompat.getColor(context, R.color.humid_warning);
        } else {
            return ContextCompat.getColor(context, R.color.humid_critical);
        }
    }

    /**
     * WiFi sinyal gücüne göre renk döndürür
     */
    public static int getSignalStrengthColor(Context context, int rssi) {
        if (context == null) return Color.BLACK;

        if (rssi >= -50) {
            return ContextCompat.getColor(context, R.color.signal_excellent);
        } else if (rssi >= -60) {
            return ContextCompat.getColor(context, R.color.signal_good);
        } else if (rssi >= -70) {
            return ContextCompat.getColor(context, R.color.signal_fair);
        } else if (rssi >= -80) {
            return ContextCompat.getColor(context, R.color.signal_poor);
        } else {
            return ContextCompat.getColor(context, R.color.signal_none);
        }
    }

    /**
     * Boolean değere göre aktif/pasif renk döndürür
     */
    public static int getStatusColor(Context context, boolean isActive) {
        if (context == null) return Color.BLACK;

        return ContextCompat.getColor(context,
                isActive ? R.color.color_active : R.color.color_inactive);
    }

    /**
     * PID moduna göre renk döndürür
     */
    public static int getPIDModeColor(Context context, int pidMode) {
        if (context == null) return Color.BLACK;

        switch (pidMode) {
            case Constants.PID_MODE_OFF:
                return ContextCompat.getColor(context, R.color.pid_off);
            case Constants.PID_MODE_MANUAL:
                return ContextCompat.getColor(context, R.color.pid_manual);
            case Constants.PID_MODE_AUTO:
                return ContextCompat.getColor(context, R.color.pid_auto);
            default:
                return ContextCompat.getColor(context, R.color.color_unknown);
        }
    }

    // === Validasyonlar ===

    /**
     * Sıcaklık değeri geçerli mi kontrol eder
     */
    public static boolean isValidTemperature(double temperature) {
        return temperature >= Constants.MIN_TEMPERATURE && temperature <= Constants.MAX_TEMPERATURE;
    }

    /**
     * Nem değeri geçerli mi kontrol eder
     */
    public static boolean isValidHumidity(double humidity) {
        return humidity >= Constants.MIN_HUMIDITY && humidity <= Constants.MAX_HUMIDITY;
    }

    /**
     * PID parametresi geçerli mi kontrol eder
     */
    public static boolean isValidPIDValue(double value, String paramType) {
        switch (paramType.toLowerCase()) {
            case "kp":
                return value >= Constants.MIN_PID_KP && value <= Constants.MAX_PID_KP;
            case "ki":
                return value >= Constants.MIN_PID_KI && value <= Constants.MAX_PID_KI;
            case "kd":
                return value >= Constants.MIN_PID_KD && value <= Constants.MAX_PID_KD;
            default:
                return false;
        }
    }

    /**
     * Motor zamanı geçerli mi kontrol eder
     */
    public static boolean isValidMotorTime(int time, boolean isWaitTime) {
        if (isWaitTime) {
            return time >= Constants.MIN_MOTOR_WAIT_TIME && time <= Constants.MAX_MOTOR_WAIT_TIME;
        } else {
            return time >= Constants.MIN_MOTOR_RUN_TIME && time <= Constants.MAX_MOTOR_RUN_TIME;
        }
    }

    /**
     * Kalibrasyon değeri geçerli mi kontrol eder
     */
    public static boolean isValidCalibration(double calibration) {
        return calibration >= Constants.MIN_CALIBRATION && calibration <= Constants.MAX_CALIBRATION;
    }

    /**
     * Kuluçka günü geçerli mi kontrol eder
     */
    public static boolean isValidIncubationDays(int days) {
        return days >= Constants.MIN_INCUBATION_DAYS && days <= Constants.MAX_INCUBATION_DAYS;
    }

    // === String İşlemleri ===

    /**
     * String boş mu kontrol eder
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * String'i güvenli şekilde int'e çevirir
     */
    public static int safeParseInt(String str, int defaultValue) {
        if (isEmpty(str)) return defaultValue;

        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * String'i güvenli şekilde double'a çevirir
     */
    public static double safeParseDouble(String str, double defaultValue) {
        if (isEmpty(str)) return defaultValue;

        try {
            return Double.parseDouble(str.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * String'i güvenli şekilde float'a çevirir
     */
    public static float safeParseFloat(String str, float defaultValue) {
        if (isEmpty(str)) return defaultValue;

        try {
            return Float.parseFloat(str.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // === Hesaplamalar ===

    /**
     * İki değer arasındaki yüzde farkını hesaplar
     */
    public static double calculatePercentageDifference(double value1, double value2) {
        if (value2 == 0) return 0;
        return Math.abs((value1 - value2) / value2) * 100;
    }

    /**
     * Değerin min-max aralığında olup olmadığını kontrol eder
     */
    public static boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

    /**
     * Değeri belirtilen aralıkta sınırlar
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Değeri belirtilen aralıkta sınırlar (int)
     */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Sıcaklık alarm durumunu kontrol eder
     */
    public static boolean isTemperatureAlarmTriggered(double temperature, double lowAlarm, double highAlarm) {
        return temperature < lowAlarm || temperature > highAlarm;
    }

    /**
     * Nem alarm durumunu kontrol eder
     */
    public static boolean isHumidityAlarmTriggered(double humidity, double lowAlarm, double highAlarm) {
        return humidity < lowAlarm || humidity > highAlarm;
    }

    // === IP Adresi İşlemleri ===

    /**
     * IP adresinin aynı subnet'te olup olmadığını kontrol eder
     */
    public static boolean isInSameSubnet(String ip1, String ip2) {
        if (isEmpty(ip1) || isEmpty(ip2)) return false;

        String[] parts1 = ip1.split("\\.");
        String[] parts2 = ip2.split("\\.");

        if (parts1.length != 4 || parts2.length != 4) return false;

        // İlk 3 okteti karşılaştır
        for (int i = 0; i < 3; i++) {
            if (!parts1[i].equals(parts2[i])) {
                return false;
            }
        }

        return true;
    }

    /**
     * Subnet'in base IP'sini döndürür (örn: 192.168.1.0)
     */
    public static String getSubnetBase(String ipAddress) {
        if (isEmpty(ipAddress)) return null;

        String[] parts = ipAddress.split("\\.");
        if (parts.length != 4) return null;

        return parts[0] + "." + parts[1] + "." + parts[2] + ".0";
    }

    // === Delay İşlemleri ===

    /**
     * Ana thread'i bloke etmeden delay
     */
    public static void delay(long milliseconds, Runnable callback) {
        new android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed(callback, milliseconds);
    }

    // === Debug İşlemleri ===

    /**
     * Debug amaçlı array'i string'e çevirir
     */
    public static String arrayToString(Object[] array) {
        if (array == null) return "null";

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(array[i]);
        }
        sb.append("]");

        return sb.toString();
    }

    /**
     * Geçen zamanı hesaplar ve formatlar
     */
    public static String getElapsedTime(long startTime) {
        long elapsed = System.currentTimeMillis() - startTime;

        if (elapsed < 1000) {
            return elapsed + "ms";
        } else if (elapsed < 60000) {
            return String.format(Locale.getDefault(), "%.1fs", elapsed / 1000.0);
        } else {
            long seconds = elapsed / 1000;
            long minutes = seconds / 60;
            seconds = seconds % 60;
            return String.format(Locale.getDefault(), "%dm %ds", minutes, seconds);
        }
    }

    /**
     * Memory kullanımını formatlar
     */
    public static String getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        return String.format(Locale.getDefault(),
                "Used: %s / Total: %s",
                formatFileSize(usedMemory),
                formatFileSize(totalMemory));
    }

    // === Kuluçka Spesifik İşlemler ===

    /**
     * Kuluçka tipi ID'sinden isim döndürür
     */
    public static String getIncubationTypeName(int typeId) {
        if (typeId >= 0 && typeId < Constants.INCUBATION_TYPE_NAMES.length) {
            return Constants.INCUBATION_TYPE_NAMES[typeId];
        }
        return "Bilinmeyen";
    }

    /**
     * PID modu ID'sinden isim döndürür
     */
    public static String getPIDModeName(int modeId) {
        if (modeId >= 0 && modeId < Constants.PID_MODE_NAMES.length) {
            return Constants.PID_MODE_NAMES[modeId];
        }
        return "Bilinmeyen";
    }

    /**
     * Kuluçka için optimal sıcaklık aralığında mı kontrol eder
     */
    public static boolean isOptimalTemperature(double temperature, int incubationType) {
        switch (incubationType) {
            case Constants.INCUBATION_TYPE_CHICKEN:
                return isInRange(temperature, 37.0, 38.0);
            case Constants.INCUBATION_TYPE_QUAIL:
                return isInRange(temperature, 37.2, 37.8);
            case Constants.INCUBATION_TYPE_GOOSE:
                return isInRange(temperature, 37.0, 37.8);
            default:
                return isInRange(temperature, 36.5, 38.5);
        }
    }

    /**
     * Kuluçka için optimal nem aralığında mı kontrol eder
     */
    public static boolean isOptimalHumidity(double humidity, int incubationType, boolean isHatchingPhase) {
        switch (incubationType) {
            case Constants.INCUBATION_TYPE_CHICKEN:
                return isInRange(humidity, isHatchingPhase ? 65 : 55, isHatchingPhase ? 75 : 65);
            case Constants.INCUBATION_TYPE_QUAIL:
                return isInRange(humidity, isHatchingPhase ? 65 : 60, isHatchingPhase ? 80 : 70);
            case Constants.INCUBATION_TYPE_GOOSE:
                return isInRange(humidity, isHatchingPhase ? 70 : 50, isHatchingPhase ? 85 : 60);
            default:
                return isInRange(humidity, isHatchingPhase ? 65 : 55, isHatchingPhase ? 80 : 70);
        }
    }

    // Private constructor to prevent instantiation
    private Utils() {
        throw new UnsupportedOperationException("Utils sınıfından instance oluşturulamaz");
    }
}