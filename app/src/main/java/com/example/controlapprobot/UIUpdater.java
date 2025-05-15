package com.example.controlapprobot;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.widget.TextView;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.Entry;

import java.util.ArrayList;

public class UIUpdater {
    private final TextView accelerationXTextView, accelerationYTextView, accelerationZTextView;
    private final TextView gyroXTextView, gyroYTextView, gyroZTextView;
    private final TextView sensorDescriptionTextView;
    private final LineChart accelerationChart, motionPathChart;
    private final LineDataSet accelerationDataSet, motionPathDataSet;
    private final float[] pos = new float[2];
    private TextView assessmentResultTextView;
    private int timeIndex = 0;
    private long lastUpdateTime = 0;
    private final long updateIntervalMs;

    public UIUpdater(Context context,
                     TextView ax, TextView ay, TextView az,
                     TextView gx, TextView gy, TextView gz,
                     TextView desc,
                     LineChart accelChart, LineChart motionChart,
                     long updateIntervalMs,
                     TextView assessmentTextView) {

        this.accelerationXTextView = ax;
        this.accelerationYTextView = ay;
        this.accelerationZTextView = az;
        this.gyroXTextView = gx;
        this.gyroYTextView = gy;
        this.gyroZTextView = gz;
        this.sensorDescriptionTextView = desc;
        this.accelerationChart = accelChart;
        this.motionPathChart = motionChart;
        this.updateIntervalMs = updateIntervalMs;
        this.assessmentResultTextView = assessmentTextView;

        accelerationDataSet = new LineDataSet(new ArrayList<>(), "Độ lớn gia tốc");
        accelerationDataSet.setColor(Color.CYAN);
        accelerationDataSet.setDrawCircles(false);
        accelerationDataSet.setLineWidth(2f);
        accelerationChart.setData(new LineData(accelerationDataSet));
        accelerationChart.getDescription().setText("Độ lớn gia tốc (y) so với Thời gian (x)");
        accelerationChart.getAxisLeft().setAxisMinimum(0f);
        accelerationChart.getAxisLeft().setAxisMaximum(20f);
        accelerationChart.getAxisRight().setEnabled(false);
        accelerationChart.getXAxis().setTextColor(Color.WHITE);
        accelerationChart.getAxisLeft().setTextColor(Color.WHITE);
        accelerationChart.getAxisRight().setTextColor(Color.WHITE);
        accelerationChart.getLegend().setTextColor(Color.WHITE);
        accelerationChart.getDescription().setTextColor(Color.WHITE);

        motionPathDataSet = new LineDataSet(new ArrayList<>(), "Đường chuyển động");
        motionPathDataSet.setColor(Color.MAGENTA);
        motionPathDataSet.setDrawCircles(true);
        motionPathDataSet.setCircleRadius(2f);
        motionPathChart.setData(new LineData(motionPathDataSet));
        motionPathChart.getDescription().setText("Đường chuyển động 2D");
        motionPathChart.getAxisLeft().setAxisMinimum(-20f);
        motionPathChart.getAxisLeft().setAxisMaximum(20f);
        motionPathChart.getAxisRight().setEnabled(false);
        motionPathChart.getXAxis().setTextColor(Color.WHITE);
        motionPathChart.getAxisLeft().setTextColor(Color.WHITE);
        motionPathChart.getAxisRight().setTextColor(Color.WHITE);
        motionPathChart.getLegend().setTextColor(Color.WHITE);
        motionPathChart.getDescription().setTextColor(Color.WHITE);

    }

    public void update(final String axStr, final String ayStr, final String azStr,
                       final String gxStr, final String gyStr, final String gzStr, final Runnable postUi) {
        try {
            float ax = Float.parseFloat(axStr);
            float ay = Float.parseFloat(ayStr);
            float az = Float.parseFloat(azStr);
            float gx = Float.parseFloat(gxStr);
            float gy = Float.parseFloat(gyStr);
            float gz = Float.parseFloat(gzStr);

            accelerationXTextView.setText("Accel X: " + axStr);
            accelerationYTextView.setText("Accel Y: " + ayStr);
            accelerationZTextView.setText("Accel Z: " + azStr);
            gyroXTextView.setText("Gyro X: " + gxStr);
            gyroYTextView.setText("Gyro Y: " + gyStr);
            gyroZTextView.setText("Gyro Z: " + gzStr);

            String description = describeSensorData(ax, ay, az, gx, gy, gz);
            sensorDescriptionTextView.setText("Trạng thái: " + description);

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUpdateTime > updateIntervalMs) {
                lastUpdateTime = currentTime;

                float magnitude = (float) Math.sqrt(ax * ax + ay * ay + az * az);
                if (!Float.isNaN(magnitude) && !Float.isInfinite(magnitude)) {
                    accelerationDataSet.addEntry(new Entry(timeIndex++, magnitude));
                    if (accelerationDataSet.getEntryCount() > 20) accelerationDataSet.removeFirst();
                    accelerationChart.getData().notifyDataChanged();
                    accelerationChart.notifyDataSetChanged();
                    accelerationChart.invalidate();
                    accelerationChart.getData().setDrawValues(false);
                    accelerationDataSet.setDrawValues(false);
                }

                pos[0] += ax * 0.1f;
                pos[1] -= ay * 0.1f;
                if (!Float.isNaN(pos[0]) && !Float.isNaN(pos[1]) &&
                        !Float.isInfinite(pos[0]) && !Float.isInfinite(pos[1])) {
                    motionPathDataSet.addEntry(new Entry(pos[0], pos[1]));
                    if (motionPathDataSet.getEntryCount() > 20) motionPathDataSet.removeFirst();
                    motionPathChart.getData().notifyDataChanged();
                    motionPathChart.notifyDataSetChanged();
                    motionPathChart.invalidate();
                } else {
                    Log.w("MotionChart", "Skipped invalid motion path point: " + pos[0] + ", " + pos[1]);
                }
            }
        } catch (NumberFormatException e) {
            Log.e("UIUpdater", "Failed to parse sensor data", e);
            sensorDescriptionTextView.setText("Status: Error parsing sensor values.");
        }
        postUi.run();
    }

    private String describeSensorData(float ax, float ay, float az, float gx, float gy, float gz) {
        StringBuilder description = new StringBuilder();
        if (ax > 5) description.append("Nghiêng phải. ");
        else if (ax < -5) description.append("Nghiêng trái. ");

        if (ay > 5) description.append("Nghiêng trước. ");
        else if (ay < -5) description.append("Nghiêng sau. ");

        if (az < 5) description.append("Nguy hiểm! ");
        else if (az > 12) description.append("Đứng yên trên mặt phẳng. ");

        if (Math.abs(gx) > 10) description.append("Đang xoay quanh trục X. ");
        if (Math.abs(gy) > 10) description.append("Đang xoay quanh trục Y. ");
        if (Math.abs(gz) > 10) description.append("Đang xoay quanh trục z. ");

        if (description.length() == 0) description.append("Đang cân bằng.");
        return description.toString();
    }

    public void showAssessmentProgress(int current, int max) {
        assessmentResultTextView.setText("Đang đánh giá... (" + current + "/" + max + ")");
    }

    public void showAssessmentResult(String result) {
        Log.d("UIUpdater", "Final result received:\n" + result);
        if (assessmentResultTextView == null) {
            Log.e("UIUpdater", "assessmentResultTextView IS NULL!");
        } else {
            assessmentResultTextView.setText(result);
        }
    }
}
