package com.example.timetablescheduler;

import android.app.ProgressDialog;
import android.content.*;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class TimetableGenerationActivity extends AppCompatActivity {
    private static final String TAG = "TimetableGenActivity";
    private ProgressDialog progressDialog;
    private final Handler timeoutHandler = new Handler();
    private boolean broadcastReceived = false;

    // Timeout after 10 seconds
    private final Runnable timeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (!broadcastReceived && progressDialog != null && progressDialog.isShowing()) {
                Log.w(TAG, "Timeout - proceeding anyway");
                progressDialog.dismiss();
                startTimetableDisplay();
            }
        }
    };

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            broadcastReceived = true;
            timeoutHandler.removeCallbacks(timeoutRunnable);

            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            if ("TIMETABLE_GENERATED".equals(intent.getAction())) {
                Log.d(TAG, "Received TIMETABLE_GENERATED");
                startTimetableDisplay();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable_generation);

        Button btnGenerate = findViewById(R.id.btnGenerateTimetable);
        btnGenerate.setOnClickListener(v -> startGeneration());
    }

    private void startGeneration() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Generating");
        progressDialog.setMessage("Creating optimal timetable...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Start timeout countdown
        timeoutHandler.postDelayed(timeoutRunnable, 10000); // 10 seconds
        broadcastReceived = false;

        // Start service
        Intent serviceIntent = new Intent(this, GeneticService.class);
        startService(serviceIntent);
        Log.d(TAG, "Service started");
    }

    private void startTimetableDisplay() {
        startActivity(new Intent(this, TimetableDisplayActivity.class));
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction("TIMETABLE_GENERATED");
        filter.addAction("TIMETABLE_ERROR");

        ContextCompat.registerReceiver(
                this,
                receiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(receiver);
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering receiver", e);
        }
        timeoutHandler.removeCallbacks(timeoutRunnable);
    }
}