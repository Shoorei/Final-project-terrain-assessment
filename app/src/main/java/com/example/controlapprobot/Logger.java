package com.example.controlapprobot;

import android.util.Log;

import com.google.firebase.database.DatabaseReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Logger {
    private final DatabaseReference databaseReference;

    public Logger(DatabaseReference ref) {
        this.databaseReference = ref;
    }

    public void logSensorData(float accelX, float accelY, float accelZ, float gyroX, float gyroY, float gyroZ) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String formattedTime = sdf.format(new Date());
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("accelX", accelX);
        dataMap.put("accelY", accelY);
        dataMap.put("accelZ", accelZ);
        dataMap.put("gyroX", gyroX);
        dataMap.put("gyroY", gyroY);
        dataMap.put("gyroZ", gyroZ);
        dataMap.put("timestamp", formattedTime);

        databaseReference.push().setValue(dataMap)
                .addOnSuccessListener(aVoid -> Log.d("Firebase", "Sensor data logged successfully."))
                .addOnFailureListener(e -> Log.e("Firebase", "Failed to log sensor data.", e));
    }

    public void logAssessment(float pitch, float roll, String terrain, float roughness, String roughnessCategory, int count) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String formattedTime = sdf.format(new Date());
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("timestamp", formattedTime);
        dataMap.put("pitch", pitch);
        dataMap.put("roll", roll);
        dataMap.put("terrain", terrain);
        dataMap.put("roughness", roughness);
        dataMap.put("roughnessCategory", roughnessCategory);
        dataMap.put("sampleCount", count);

        databaseReference.push().setValue(dataMap)
                .addOnSuccessListener(aVoid -> Log.d("Firebase", "Assessment logged successfully."))
                .addOnFailureListener(e -> Log.e("Firebase", "Failed to log assessment.", e));
    }
}
