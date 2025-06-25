package com.example.controlapprobot;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;

public class MainActivity extends AppCompatActivity {
    private UDPService udpService;
    private UIUpdater uiUpdater;
    private AssessmentHandler assessmentHandler;
    private ButtonController buttonController;
    private static final int UDP_PORT_SEND = 44086;
    private static final int UDP_PORT_RECEIVE = 35082;
    private static final int PACKET_SIZE = 2000;
    private static final int MAX_ASSESSMENT_COUNT = 10;
    private static final long UPDATE_INTERVAL_MS = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DatabaseReference sensorRef = FirebaseDatabase.getInstance("https://mpu6050-finalthesis-logger-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("sensorData");
        DatabaseReference assessmentRef = FirebaseDatabase.getInstance("https://mpu6050-finalthesis-logger-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("assessmentData");

        Logger sensorLogger = new Logger(sensorRef);
        Logger assessmentLogger = new Logger(assessmentRef);
        assessmentHandler = new AssessmentHandler(
                this,
                MAX_ASSESSMENT_COUNT,
                assessmentLogger,
                null
        );
        assessmentHandler.setOnAssessmentProgressListener((current, max) ->
                runOnUiThread(() -> uiUpdater.showAssessmentProgress(current, max))
        );

        // Find views
        TextView accelerationXTextView = findViewById(R.id.accelerationXTextView);
        TextView accelerationYTextView = findViewById(R.id.accelerationYTextView);
        TextView accelerationZTextView = findViewById(R.id.accelerationZTextView);
        TextView gyroXTextView = findViewById(R.id.gyroXTextView);
        TextView gyroYTextView = findViewById(R.id.gyroYTextView);
        TextView gyroZTextView = findViewById(R.id.gyroZTextView);
        TextView sensorDescriptionTextView = findViewById(R.id.sensorDescriptionTextView);
        LineChart accelerationChart = findViewById(R.id.accelerationChart);
        LineChart motionPathChart = findViewById(R.id.motionPathChart);
        TextView assessmentResultTextView = findViewById(R.id.assessmentResultTextView);
        Spinner spinnerAssessmentCount = findViewById(R.id.spinnerAssessmentCount);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.assessment_count_options,
                R.layout.spinner_item
        );
        adapter.setDropDownViewResource(R.layout.spinner_item);
        spinnerAssessmentCount.setAdapter(adapter);
        spinnerAssessmentCount.setSelection(0);

        spinnerAssessmentCount.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedValue = (String) parent.getItemAtPosition(position);
                int count = Integer.parseInt(selectedValue);
                assessmentHandler.setMaxAssessmentCount(count);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        uiUpdater = new UIUpdater(
                this,
                accelerationXTextView, accelerationYTextView, accelerationZTextView,
                gyroXTextView, gyroYTextView, gyroZTextView,
                sensorDescriptionTextView,
                accelerationChart, motionPathChart,
                UPDATE_INTERVAL_MS,
                assessmentResultTextView
        );

        udpService = new UDPService(UDP_PORT_SEND, UDP_PORT_RECEIVE, PACKET_SIZE);
        udpService.setListener(data -> {
            int[] indices = findIndices(data,"Acceleration: X=", " Y=", " Z="," Gyro X=", " Y=", " Z=");
            if (indices != null) {
                final String acceleration_x = getSubstring(data, indices[0] + 16, indices[1]);
                final String acceleration_y = getSubstring(data, indices[1] + 3, indices[2]);
                final String acceleration_z = getSubstring(data, indices[2] + 3, indices[3]);
                final String gyro_x = getSubstring(data, indices[3] + 8, indices[4]);
                final String gyro_y = getSubstring(data, indices[4] + 3, indices[5]);
                final String gyro_z = getSubstring(data, indices[5] + 3, data.length());
                runOnUiThread(() -> uiUpdater.update(
                        acceleration_x, acceleration_y, acceleration_z, gyro_x, gyro_y, gyro_z, () -> {
                            try {
                                sensorLogger.logSensorData(
                                        Float.parseFloat(acceleration_x),
                                        Float.parseFloat(acceleration_y),
                                        Float.parseFloat(acceleration_z),
                                        Float.parseFloat(gyro_x),
                                        Float.parseFloat(gyro_y),
                                        Float.parseFloat(gyro_z)
                                );
                            } catch (NumberFormatException ignored) {}
                        }
                ));
                assessmentHandler.handleSensorData(
                        Float.parseFloat(acceleration_x),
                        Float.parseFloat(acceleration_y),
                        Float.parseFloat(acceleration_z)
                );
            }
        });

        // Control buttons
        ImageButton btn_ahead = findViewById(R.id.tien);
        ImageButton btn_back = findViewById(R.id.lui);
        ImageButton btn_left = findViewById(R.id.trai);
        ImageButton btn_right = findViewById(R.id.phai);
        Button buttonSwitchScene = findViewById(R.id.buttonSwitchScene);
        Button btnStartAssessment = findViewById(R.id.btnStartAssessment);

        btnStartAssessment.setOnClickListener(v -> assessmentHandler.startAssessment());

        assessmentHandler.setAssessmentListener(result -> {
            runOnUiThread(() -> uiUpdater.showAssessmentResult(result));
        });

        buttonController = new ButtonController(
                this,
                assessmentHandler,
                btn_ahead, btn_back, btn_left, btn_right,
                buttonSwitchScene, btnStartAssessment,
                new ButtonController.ButtonListener() {
                    @Override
                    public void onCommand(String command) {
                        udpService.sendCommand(command);
                        Log.d("ButtonEvent", "Sending command: " + command);
                    }

                    @Override
                    public void onSwitchScene() {
                        udpService.stopReceiving();
                        Intent intent = new Intent(MainActivity.this, SensorLogActivity.class);
                        startActivity(intent);
                    }

                    @Override
                    public void onAssessmentCancelled() {
                        String lastResult = assessmentHandler.getSavedAssessmentResult();
                        runOnUiThread(() -> {
                            if (lastResult != null) {
                                uiUpdater.showAssessmentResult(lastResult);
                            } else {
                                uiUpdater.showAssessmentResult("Không có kết quả trước đó.");
                            }
                        });
                    }
                }
        );

        assessmentHandler.setMaxAssessmentCount(10);
        udpService.startReceiving();
        assessmentHandler.getSavedAssessmentResult();

        // Music service
        Intent musicIntent = new Intent(this, MusicService.class);
        startService(musicIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, MusicService.class));
        udpService.shutdown();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startService(new Intent(this, MusicService.class));
        udpService.startReceiving();
    }

    @Override
    protected void onPause() {
        super.onPause();
        udpService.stopReceiving();
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

    private int[] findIndices(String data, String... substrings) {
        int[] indices = new int[substrings.length];
        int startIndex = 0;
        for (int i = 0; i < substrings.length; i++) {
            indices[i] = data.indexOf(substrings[i], startIndex);
            if (indices[i] == -1) return null;
            startIndex = indices[i] + 1;
        }
        return indices;
    }

    private String getSubstring(String data, int startIndex, int endIndex) {
        if (startIndex >= 0 && endIndex >= 0 && endIndex <= data.length())
            return data.substring(startIndex, endIndex);
        else return "";
    }
}
