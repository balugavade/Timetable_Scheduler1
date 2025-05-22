package com.example.timetablescheduler;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.parse.ParseUser;

public class WellcomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wellcome);

        TextView tvWelcome = findViewById(R.id.textView);
        Button btnLogout = findViewById(R.id.button);
        Button btnTimetable = findViewById(R.id.button2);

        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser != null) {
            String displayName = currentUser.getString("displayName");
            tvWelcome.setText("Welcome, " + displayName + "!");
        }

        btnTimetable.setOnClickListener(v ->
                startActivity(new Intent(this, DaysPeriodsActivity.class))
        );

        btnLogout.setOnClickListener(v -> {
            ParseUser.logOut();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}