package com.example.timetablescheduler;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.parse.ParseUser;

public class WelcomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wellcome);

        TextView tvWelcome = findViewById(R.id.textView);
        Button btnLogout = findViewById(R.id.button);

        // CardViews for tiles
        CardView cardTeacher = findViewById(R.id.cardTeacher);
        CardView cardSubject = findViewById(R.id.cardSubject);
        CardView cardTimetable = findViewById(R.id.cardTimetable);

        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser != null) {
            String displayName = currentUser.getString("displayName");
            tvWelcome.setText("Welcome, " + displayName + "!");
        }

        cardTeacher.setOnClickListener(v ->
                startActivity(new Intent(this, TeachersActivity.class))
        );

        cardSubject.setOnClickListener(v ->
                startActivity(new Intent(this, SubjectsActivity.class))
        );

        cardTimetable.setOnClickListener(v ->
                startActivity(new Intent(this, TimetableActivity.class))
        );

        btnLogout.setOnClickListener(v -> {
            ParseUser.logOut();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}
