package com.example.timetablescheduler;

import android.app.ProgressDialog;
import android.content.*;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.parse.*;
import java.util.*;

public class TimetableGenerationActivity extends AppCompatActivity {
    private ProgressDialog progressDialog;
    private TextView tvStatus;
    private Button btnGenerateTimetable;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (progressDialog != null) progressDialog.dismiss();

            if ("TIMETABLE_GENERATED".equals(intent.getAction())) {
                tvStatus.setText("Timetable generated successfully!");
                startActivity(new Intent(TimetableGenerationActivity.this, TimetableDisplayActivity.class));
            } else if ("TIMETABLE_ERROR".equals(intent.getAction())) {
                tvStatus.setText("Error generating timetable. Please try again.");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable_generation);

        tvStatus = findViewById(R.id.tvStatus);
        btnGenerateTimetable = findViewById(R.id.btnGenerateTimetable);

        btnGenerateTimetable.setOnClickListener(v -> generateTimetable());

        // Display current configuration
        displayConfiguration();
    }

    private void displayConfiguration() {
        // Fetch and display current configuration summary
        ParseQuery<ParseObject> configQuery = ParseQuery.getQuery("TimetableConfig");
        configQuery.whereEqualTo("user", ParseUser.getCurrentUser());
        configQuery.getFirstInBackground((config, e) -> {
            if (e == null && config != null) {
                String summary = "Configuration Ready:\n" +
                        "Periods per day: " + config.getInt("periodsPerDay") + "\n" +
                        "Working days: " + config.getList("workingDays").size() + "\n" +
                        "Breaks: " + config.getInt("breaksPerDay");
                tvStatus.setText(summary);
            }
        });
    }

    private void generateTimetable() {
        // Check if all required data is available
        checkDataCompleteness((isComplete, message) -> {
            if (isComplete) {
                progressDialog = ProgressDialog.show(this,
                        "Generating Timetable",
                        "Processing genetic algorithm...", true);

                Intent serviceIntent = new Intent(this, GeneticService.class);
                startService(serviceIntent);
            } else {
                Toast.makeText(this, "Missing data: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void checkDataCompleteness(DataCallback callback) {
        // Check teachers
        ParseQuery<ParseObject> teacherQuery = ParseQuery.getQuery("Teacher");
        teacherQuery.whereEqualTo("user", ParseUser.getCurrentUser());
        teacherQuery.countInBackground((teacherCount, e1) -> {
            if (teacherCount == 0) {
                callback.onResult(false, "No teachers found");
                return;
            }

            // Check subjects
            ParseQuery<ParseObject> subjectQuery = ParseQuery.getQuery("Subject");
            subjectQuery.whereEqualTo("user", ParseUser.getCurrentUser());
            subjectQuery.countInBackground((subjectCount, e2) -> {
                if (subjectCount == 0) {
                    callback.onResult(false, "No subjects found");
                    return;
                }

                // Check batches
                ParseQuery<ParseObject> batchQuery = ParseQuery.getQuery("Batch");
                batchQuery.whereEqualTo("user", ParseUser.getCurrentUser());
                batchQuery.countInBackground((batchCount, e3) -> {
                    if (batchCount == 0) {
                        callback.onResult(false, "No batches found");
                        return;
                    }

                    callback.onResult(true, "All data available");
                });
            });
        });
    }

    interface DataCallback {
        void onResult(boolean isComplete, String message);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction("TIMETABLE_GENERATED");
        filter.addAction("TIMETABLE_ERROR");
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }
}
