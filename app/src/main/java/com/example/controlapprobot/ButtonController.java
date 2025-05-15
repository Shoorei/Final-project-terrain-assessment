package com.example.controlapprobot;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.core.content.ContextCompat;

public class ButtonController {
    public interface ButtonListener {
        void onCommand(String command);
        void onSwitchScene();
        void onAssessmentCancelled();
    }

    private final Context context;

    public ButtonController(
            Context context,
            AssessmentHandler assessmentHandler,
            ImageButton btnAhead, ImageButton btnBack, ImageButton btnLeft, ImageButton btnRight,
            Button btnSwitchScene, Button btnStartAssessment,
            ButtonListener listener
    ) {
        this.context = context;

        btnAhead.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) listener.onCommand("F");
            else if (event.getAction() == MotionEvent.ACTION_UP) listener.onCommand("S");
            return true;
        });
        btnBack.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) listener.onCommand("B");
            else if (event.getAction() == MotionEvent.ACTION_UP) listener.onCommand("S");
            return true;
        });
        btnLeft.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) listener.onCommand("L");
            else if (event.getAction() == MotionEvent.ACTION_UP) listener.onCommand("S");
            return true;
        });
        btnRight.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) listener.onCommand("R");
            else if (event.getAction() == MotionEvent.ACTION_UP) listener.onCommand("S");
            return true;
        });

        btnSwitchScene.setOnClickListener(v -> listener.onSwitchScene());

        btnStartAssessment.setOnClickListener(v -> {
            if (assessmentHandler.isAssessing()) {
                // Cancel assessment
                assessmentHandler.cancelAssessment();
                btnStartAssessment.setText("Bắt đầu đánh giá");
                int startColor = ContextCompat.getColor(context, R.color.assessment_start_color);
                btnStartAssessment.setBackgroundTintList(ColorStateList.valueOf(startColor));

                // Notify MainActivity to restore previous result
                listener.onAssessmentCancelled();
            } else {
                // Start new assessment
                assessmentHandler.startAssessment();
                btnStartAssessment.setText("Hủy");
                int stopColor = ContextCompat.getColor(context, R.color.assessment_stop_color);
                btnStartAssessment.setBackgroundTintList(ColorStateList.valueOf(stopColor));

                // ✅ Wrap the original listener to avoid overwriting MainActivity’s logic
                AssessmentHandler.OnAssessmentCompleteListener originalListener = assessmentHandler.getListener();

                assessmentHandler.setAssessmentListener(result -> {
                    // Save result and update button appearance
                    assessmentHandler.saveAssessmentResult(context, result);

                    ((Activity) context).runOnUiThread(() -> {
                        btnStartAssessment.setText("Bắt đầu đánh giá");
                        int startColor = ContextCompat.getColor(context, R.color.assessment_start_color);
                        btnStartAssessment.setBackgroundTintList(ColorStateList.valueOf(startColor));
                    });

                    // ✅ Forward the result to the original listener (e.g., MainActivity)
                    if (originalListener != null) {
                        originalListener.onComplete(result);
                    }
                });
            }
        });
    }
}
