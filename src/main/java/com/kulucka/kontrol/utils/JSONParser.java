package com.kulucka.kontrol.utils;

import android.util.Log;

import com.kulucka.kontrol.models.AlarmData;
import com.kulucka.kontrol.models.Profile;
import com.kulucka.kontrol.models.SensorData;
import com.kulucka.kontrol.models.AppSettings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class JSONParser {
    private static final String TAG = "JSONParser";

    public static SensorData parseSensorData(String jsonStr) {
        SensorData data = new SensorData();

        try {
            JSONObject json = new JSONObject(jsonStr);

            if (!json.getString("type").equals("sensor_data")) {
                return null;
            }

            if (json.has("temp")) data.setTemperature((float) json.getDouble("temp"));
            if (json.has("humidity")) data.setHumidity((float) json.getDouble("humidity"));
            if (json.has("temp1")) data.setTemperature1((float) json.getDouble("temp1"));
            if (json.has("hum1")) data.setHumidity1((float) json.getDouble("hum1"));
            if (json.has("temp2")) data.setTemperature2((float) json.getDouble("temp2"));
            if (json.has("hum2")) data.setHumidity2((float) json.getDouble("hum2"));
            if (json.has("heater")) data.setHeaterActive(json.getBoolean("heater"));
            if (json.has("humidifier")) data.setHumidifierActive(json.getBoolean("humidifier"));
            if (json.has("motor")) data.setMotorActive(json.getBoolean("motor"));
            if (json.has("motorRemaining")) data.setMotorRemainingMinutes(json.getInt("motorRemaining"));
            if (json.has("pidOutput")) data.setPidOutput((float) json.getDouble("pidOutput"));
            if (json.has("day")) data.setCurrentDay(json.getInt("day"));

            return data;
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing sensor data: " + e.getMessage());
            return null;
        }
    }

    public static AppSettings parseSettings(String jsonStr) {
        AppSettings settings = new AppSettings();

        try {
            JSONObject json = new JSONObject(jsonStr);

            if (!json.getString("type").equals("settings")) {
                return null;
            }

            if (json.has("profile_type")) settings.setProfileType(json.getInt("profile_type"));
            if (json.has("target_temp")) settings.setTargetTemperature((float) json.getDouble("target_temp"));
            if (json.has("target_humidity")) settings.setTargetHumidity((float) json.getDouble("target_humidity"));
            if (json.has("total_days")) settings.setTotalDays(json.getInt("total_days"));
            if (json.has("start_time")) settings.setStartTime(json.getLong("start_time"));
            if (json.has("motor_enabled")) settings.setMotorEnabled(json.getBoolean("motor_enabled"));
            if (json.has("current_day")) settings.setCurrentDay(json.getInt("current_day"));

            // Motor ayarları
            if (json.has("motor") && json.getJSONObject("motor") != null) {
                JSONObject motorJson = json.getJSONObject("motor");
                AppSettings.MotorSettings motorSettings = new AppSettings.MotorSettings();

                if (motorJson.has("duration")) motorSettings.setDuration(motorJson.getLong("duration"));
                if (motorJson.has("interval")) motorSettings.setInterval(motorJson.getLong("interval"));

                settings.setMotorSettings(motorSettings);
            }

            // PID ayarları
            if (json.has("pid") && json.getJSONObject("pid") != null) {
                JSONObject pidJson = json.getJSONObject("pid");

                if (pidJson.has("kp")) settings.setPidKp((float) pidJson.getDouble("kp"));
                if (pidJson.has("ki")) settings.setPidKi((float) pidJson.getDouble("ki"));
                if (pidJson.has("kd")) settings.setPidKd((float) pidJson.getDouble("kd"));
            }

            // Röle durumları
            if (json.has("relay") && json.getJSONObject("relay") != null) {
                JSONObject relayJson = json.getJSONObject("relay");
                AppSettings.RelayState relayState = new AppSettings.RelayState();

                if (relayJson.has("heater")) relayState.setHeater(relayJson.getBoolean("heater"));
                if (relayJson.has("humidifier")) relayState.setHumidifier(relayJson.getBoolean("humidifier"));
                if (relayJson.has("motor")) relayState.setMotor(relayJson.getBoolean("motor"));

                settings.setRelayState(relayState);
            }

            return settings;
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing settings: " + e.getMessage());
            return null;
        }
    }

    public static Profile parseProfile(String jsonStr) {
        Profile profile = new Profile();

        try {
            JSONObject json = new JSONObject(jsonStr);

            if (!json.getString("type").equals("profile")) {
                return null;
            }

            if (json.has("profile_type")) profile.setType(json.getInt("profile_type"));
            if (json.has("name")) profile.setName(json.getString("name"));
            if (json.has("total_days")) profile.setTotalDays(json.getInt("total_days"));

            // Profil aşamaları
            if (json.has("stages") && json.getJSONArray("stages") != null) {
                JSONArray stagesArray = json.getJSONArray("stages");
                List<Profile.Stage> stages = new ArrayList<>();

                for (int i = 0; i < stagesArray.length(); i++) {
                    JSONObject stageJson = stagesArray.getJSONObject(i);
                    Profile.Stage stage = new Profile.Stage();

                    if (stageJson.has("temperature")) stage.setTemperature((float) stageJson.getDouble("temperature"));
                    if (stageJson.has("humidity")) stage.setHumidity((float) stageJson.getDouble("humidity"));
                    if (stageJson.has("motor_active")) stage.setMotorActive(stageJson.getBoolean("motor_active"));
                    if (stageJson.has("start_day")) stage.setStartDay(stageJson.getInt("start_day"));
                    if (stageJson.has("end_day")) stage.setEndDay(stageJson.getInt("end_day"));

                    stages.add(stage);
                }

                profile.setStages(stages);
            }

            return profile;
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing profile: " + e.getMessage());
            return null;
        }
    }

    public static List<Profile> parseAllProfiles(String jsonStr) {
        List<Profile> profiles = new ArrayList<>();

        try {
            JSONObject json = new JSONObject(jsonStr);

            if (!json.getString("type").equals("all_profiles")) {
                return null;
            }

            if (json.has("profiles") && json.getJSONArray("profiles") != null) {
                JSONArray profilesArray = json.getJSONArray("profiles");

                for (int i = 0; i < profilesArray.length(); i++) {
                    JSONObject profileJson = profilesArray.getJSONObject(i);
                    Profile profile = new Profile();

                    if (profileJson.has("type")) profile.setType(profileJson.getInt("type"));
                    if (profileJson.has("name")) profile.setName(profileJson.getString("name"));
                    if (profileJson.has("total_days")) profile.setTotalDays(profileJson.getInt("total_days"));

                    // Profil aşamaları
                    if (profileJson.has("stages") && profileJson.getJSONArray("stages") != null) {
                        JSONArray stagesArray = profileJson.getJSONArray("stages");
                        List<Profile.Stage> stages = new ArrayList<>();

                        for (int j = 0; j < stagesArray.length(); j++) {
                            JSONObject stageJson = stagesArray.getJSONObject(j);
                            Profile.Stage stage = new Profile.Stage();

                            if (stageJson.has("temperature")) stage.setTemperature((float) stageJson.getDouble("temperature"));
                            if (stageJson.has("humidity")) stage.setHumidity((float) stageJson.getDouble("humidity"));
                            if (stageJson.has("motor_active")) stage.setMotorActive(stageJson.getBoolean("motor_active"));
                            if (stageJson.has("start_day")) stage.setStartDay(stageJson.getInt("start_day"));
                            if (stageJson.has("end_day")) stage.setEndDay(stageJson.getInt("end_day"));

                            stages.add(stage);
                        }

                        profile.setStages(stages);
                    }

                    profiles.add(profile);
                }
            }

            return profiles;
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing all profiles: " + e.getMessage());
            return null;
        }
    }

    public static AlarmData parseAlarmData(String jsonStr) {
        AlarmData alarmData = new AlarmData();

        try {
            JSONObject json = new JSONObject(jsonStr);

            if (!json.getString("type").equals("alarm_data")) {
                return null;
            }

            if (json.has("alarm_active")) alarmData.setAlarmActive(json.getBoolean("alarm_active"));
            if (json.has("alarm_type")) alarmData.setAlarmType(json.getInt("alarm_type"));
            if (json.has("alarm_message")) alarmData.setAlarmMessage(json.getString("alarm_message"));
            if (json.has("alarm_start_time")) alarmData.setAlarmStartTime(json.getLong("alarm_start_time"));

            // Eşik değerleri
            if (json.has("thresholds") && json.getJSONObject("thresholds") != null) {
                JSONObject thresholdsJson = json.getJSONObject("thresholds");
                AlarmData.Thresholds thresholds = new AlarmData.Thresholds();

                if (thresholdsJson.has("enabled")) thresholds.setEnabled(thresholdsJson.getBoolean("enabled"));
                if (thresholdsJson.has("high_temp")) thresholds.setHighTemp((float) thresholdsJson.getDouble("high_temp"));
                if (thresholdsJson.has("low_temp")) thresholds.setLowTemp((float) thresholdsJson.getDouble("low_temp"));
                if (thresholdsJson.has("high_hum")) thresholds.setHighHum((float) thresholdsJson.getDouble("high_hum"));
                if (thresholdsJson.has("low_hum")) thresholds.setLowHum((float) thresholdsJson.getDouble("low_hum"));
                if (thresholdsJson.has("temp_diff")) thresholds.setTempDiff((float) thresholdsJson.getDouble("temp_diff"));
                if (thresholdsJson.has("hum_diff")) thresholds.setHumDiff((float) thresholdsJson.getDouble("hum_diff"));

                alarmData.setThresholds(thresholds);
            }

            // Alarm geçmişi
            if (json.has("history") && json.getJSONArray("history") != null) {
                JSONArray historyArray = json.getJSONArray("history");
                List<AlarmData.AlarmHistory> historyList = new ArrayList<>();

                for (int i = 0; i < historyArray.length(); i++) {
                    JSONObject historyJson = historyArray.getJSONObject(i);
                    AlarmData.AlarmHistory history = new AlarmData.AlarmHistory();

                    if (historyJson.has("type")) history.setType(historyJson.getInt("type"));
                    if (historyJson.has("message")) history.setMessage(historyJson.getString("message"));
                    if (historyJson.has("time")) history.setTime(historyJson.getLong("time"));

                    historyList.add(history);
                }

                alarmData.setHistory(historyList);
            }

            return alarmData;
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing alarm data: " + e.getMessage());
            return null;
        }
    }
}