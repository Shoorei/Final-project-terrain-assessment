package com.example.controlapprobot;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SensorLogActivity extends AppCompatActivity {

    private TableLayout tableData;
    private DatabaseReference databaseReference;
    private boolean showingSensorLog = true;
    private FirebaseDatabase database;
    private TableLayout tableHeaderSensor;
    private TableLayout tableHeaderAssessment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        tableData = findViewById(R.id.tableData);
        tableHeaderSensor = findViewById(R.id.tableHeaderSensor);
        tableHeaderAssessment = findViewById(R.id.tableHeaderAssessment);

        Button buttonReturn = findViewById(R.id.buttonReturn);
        Button buttonDeleteAll = findViewById(R.id.buttonDeleteAll);
        Button buttonToggleLog = findViewById(R.id.buttonToggleLog);

        database = FirebaseDatabase.getInstance("https://mpu6050-finalthesis-logger-default-rtdb.asia-southeast1.firebasedatabase.app/");
        databaseReference = database.getReference("sensorData");

        buttonToggleLog.setOnClickListener(v -> {
            showingSensorLog = !showingSensorLog;

            if (showingSensorLog) {
                databaseReference = database.getReference("sensorData");
                buttonToggleLog.setText("Xem đánh giá");
                buttonDeleteAll.setText("Xóa hết");

                tableHeaderSensor.setVisibility(View.VISIBLE);
                tableHeaderAssessment.setVisibility(View.GONE);
            } else {
                databaseReference = database.getReference("assessmentData");
                buttonToggleLog.setText("Xem dữ liệu đo");
                buttonDeleteAll.setText("Xóa hết");

                tableHeaderSensor.setVisibility(View.GONE);
                tableHeaderAssessment.setVisibility(View.VISIBLE);
            }

            loadData();
        });

        buttonReturn.setOnClickListener(v -> finish());

        buttonDeleteAll.setOnClickListener(v -> {

            new AlertDialog.Builder(SensorLogActivity.this)
                    .setTitle("Xác nhận xóa")
                    .setMessage("Bạn có chắc là muốn xóa toàn bộ data của bảng này không?")
                    .setPositiveButton("Xác nhận", (dialog, which) -> {
                        databaseReference.removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    tableData.removeAllViews();
                                    Log.d("Firebase", "All data deleted.");
                                })
                                .addOnFailureListener(e -> Log.e("Firebase", "Delete failed", e));
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        loadData();
    }

    private void loadData() {
        tableData.removeAllViews();
        databaseReference.orderByKey().limitToLast(100).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<DataSnapshot> logs = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    logs.add(child);
                }
                Collections.reverse(logs);

                for (DataSnapshot child : logs) {
                    if (showingSensorLog) {
                        String timestamp = child.child("timestamp").getValue(String.class);
                        Double accelX = child.child("accelX").getValue(Double.class);
                        Double accelY = child.child("accelY").getValue(Double.class);
                        Double accelZ = child.child("accelZ").getValue(Double.class);
                        Double gyroX = child.child("gyroX").getValue(Double.class);
                        Double gyroY = child.child("gyroY").getValue(Double.class);
                        Double gyroZ = child.child("gyroZ").getValue(Double.class);

                        addSensorTableRow(
                                timestamp,
                                accelX != null ? accelX : 0.0,
                                accelY != null ? accelY : 0.0,
                                accelZ != null ? accelZ : 0.0,
                                gyroX != null ? gyroX : 0.0,
                                gyroY != null ? gyroY : 0.0,
                                gyroZ != null ? gyroZ : 0.0
                        );
                    } else {
                        String timestamp = child.child("timestamp").getValue(String.class);
                        Double pitch = child.child("pitch").getValue(Double.class);
                        Double roll = child.child("roll").getValue(Double.class);
                        String terrain = child.child("terrain").getValue(String.class);
                        Double roughness = child.child("roughness").getValue(Double.class);
                        String category = child.child("roughnessCategory").getValue(String.class);
                        Long count = child.child("sampleCount").getValue(Long.class);

                        addAssessmentTableRow(
                                timestamp,
                                pitch != null ? pitch : 0.0,
                                roll != null ? roll : 0.0,
                                terrain != null ? terrain : "N/A",
                                roughness != null ? roughness : 0.0,
                                category != null ? category : "N/A",
                                count != null ? count : 0
                        );
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Error: " + error.getMessage());
            }
        });
    }

    private void addSensorTableRow(String timestamp, Double ax, Double ay, Double az,
                                   Double gx, Double gy, Double gz) {
        TableRow row = new TableRow(this);
        row.addView(createTextView(timestamp, 2f));
        row.addView(createTextView(String.format("%.2f", ax), 1f));
        row.addView(createTextView(String.format("%.2f", ay), 1f));
        row.addView(createTextView(String.format("%.2f", az), 1f));
        row.addView(createTextView(String.format("%.2f", gx), 1f));
        row.addView(createTextView(String.format("%.2f", gy), 1f));
        row.addView(createTextView(String.format("%.2f", gz), 1f));
        tableData.addView(row);
    }

    private void addAssessmentTableRow(String timestamp, Double pitch, Double roll,
                                       String terrain, Double roughness, String category, long count) {
        TableRow row = new TableRow(this);
        row.addView(createTextView(timestamp, 2f));
        row.addView(createTextView(String.format("%.2f", pitch), 1f));
        row.addView(createTextView(String.format("%.2f", roll), 1f));
        row.addView(createTextView(String.format("%.2f", roughness), 1f));
        row.addView(createTextView(category, 1f));
        row.addView(createTextView(terrain, 1f));
        row.addView(createTextView(String.valueOf(count), 1f));
        tableData.addView(row);
    }



//    private void addTableRow(String timestamp, Double accelX, Double accelY, Double accelZ,
//                             Double gyroX, Double gyroY, Double gyroZ) {
//
//        TableRow row = new TableRow(this);
//
//        row.addView(createTextView(timestamp, 2f)); // Timestamp gets more space
//        row.addView(createTextView(String.format("%.2f", accelX), 1f));
//        row.addView(createTextView(String.format("%.2f", accelY), 1f));
//        row.addView(createTextView(String.format("%.2f", accelZ), 1f));
//        row.addView(createTextView(String.format("%.2f", gyroX), 1f));
//        row.addView(createTextView(String.format("%.2f", gyroY), 1f));
//        row.addView(createTextView(String.format("%.2f", gyroZ), 1f));
//
//        tableData.addView(row);
//
//        TableRow separator = new TableRow(this);
//        TextView separatorView = new TextView(this);
//        separatorView.setBackgroundColor(getResources().getColor(android.R.color.white));
//        separatorView.setHeight(1); // 1dp height
//        TableRow.LayoutParams params = new TableRow.LayoutParams(
//                TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
//        params.span = row.getChildCount();
//        separator.addView(separatorView, params);
//        tableData.addView(separator);
//    }


    private TextView createTextView(String text, float weight) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(getResources().getColor(android.R.color.white));
        tv.setBackgroundResource(R.drawable.data_cell_border);
        tv.setPadding(8, 8, 8, 8);
        tv.setGravity(Gravity.LEFT);

        TableRow.LayoutParams params = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, weight);
        tv.setLayoutParams(params);
        return tv;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        }
    }

}