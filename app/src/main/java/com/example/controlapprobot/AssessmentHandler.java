package com.example.controlapprobot;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class AssessmentHandler {
    public interface OnAssessmentCompleteListener {
        void onComplete(String result);
    }
    public interface OnAssessmentProgressListener {
        void onProgress(int current, int max);
    }
    private OnAssessmentProgressListener progressListener;
    public void setOnAssessmentProgressListener(OnAssessmentProgressListener listener) {
        this.progressListener = listener;
    }

    private final Context context;
    private final Logger logger;
    private int maxAssessmentCount;
    private final List<Float> accMagnitudes = new ArrayList<>();
    private boolean assessing = false;

    private OnAssessmentCompleteListener listener;

    public AssessmentHandler(Context context, int maxAssessmentCount, Logger logger, OnAssessmentCompleteListener listener) {
        this.context = context.getApplicationContext();
        this.maxAssessmentCount = maxAssessmentCount;
        this.logger = logger;
        this.listener = listener;
    }

    public OnAssessmentCompleteListener getListener() {
        return this.listener;
    }

    public void startAssessment() {
        accMagnitudes.clear();
        assessing = true;
    }

    public void cancelAssessment() {
        assessing = false;
        accMagnitudes.clear();
    }

    public boolean isAssessing() {
        return assessing;
    }


    public void setMaxAssessmentCount(int count) {
        this.maxAssessmentCount = count;
    }

    public void setAssessmentListener(OnAssessmentCompleteListener listener) {
        this.listener = listener;
    }

    public void saveAssessmentResult(Context context, String result) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString("latest_assessment_result", result).apply();
    }


    public String getSavedAssessmentResult() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("latest_assessment_result", null);
    }

    public void handleSensorData(float ax, float ay, float az) {
        if (!assessing) return;

        if (accMagnitudes.size() < maxAssessmentCount) {
            float magnitude = (float) Math.sqrt(ax * ax + ay * ay + az * az);
            accMagnitudes.add(magnitude);

            if (progressListener != null) {
                progressListener.onProgress(accMagnitudes.size(), maxAssessmentCount);
            }


            if (accMagnitudes.size() == maxAssessmentCount) {
                assessing = false;
                try {
                    float pitch = calculatePitch(ax, ay, az);
                    float roll = calculateRoll(ax, ay, az);
                    String terrain = getTerrainType(pitch, roll);
                    float roughness = calculateRoughness(accMagnitudes);
                    String roughnessCategory = getRoughnessCategory(roughness);

                    String result = String.format(
                            "Góc nghiêng dọc (Pitch): %.2f°\nGóc nghiêng ngang (Roll): %.2f°\n" +
                                    "Chỉ số gồ ghề: %.2f (%s)\nĐịa hình: %s",
                            pitch, roll, roughness, roughnessCategory, terrain
                    );

                    saveResult(result);

                    if (logger != null) {
                        logger.logAssessment(pitch, roll, terrain, roughness, roughnessCategory, maxAssessmentCount);
                    }

                    if (listener != null) {
                        listener.onComplete(result);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }


    private void saveResult(String result) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString("latest_assessment_result", result).apply();
    }

    private float calculatePitch(float ax, float ay, float az) {
        return (float) Math.toDegrees(Math.atan2(ax, Math.sqrt(ay * ay + az * az)));
    }

    private float calculateRoll(float ax, float ay, float az) {
        return (float) Math.toDegrees(Math.atan2(ay, Math.sqrt(ax * ax + az * az)));
    }

    private String getTerrainType(float pitch, float roll) {
        float maxAngle = Math.max(Math.abs(pitch), Math.abs(roll));
        if (maxAngle < 10) return "Bằng phẳng";
        else if (maxAngle < 15) return "Dốc nhẹ";
        else if (maxAngle < 25) return "Dốc vừa";
        else return "Dốc nhiều";
    }

    private float calculateRoughness(List<Float> values) {
        float mean = 0;
        for (float v : values) mean += v;
        mean /= values.size();

        float sumSqDiff = 0;
        for (float v : values) sumSqDiff += (v - mean) * (v - mean);

        return (float) Math.sqrt(sumSqDiff / values.size());
    }

    private String getRoughnessCategory(float index) {
        if (index < 0.2) return "Không gồ ghề";
        else if (index < 0.4) return "Gồ ghề nhẹ";
        else if (index < 0.7) return "Khá gồ ghề";
        else return "Rất gồ ghề";
    }

    public int getCurrentSampleCount() {
        return accMagnitudes.size();
    }
}
