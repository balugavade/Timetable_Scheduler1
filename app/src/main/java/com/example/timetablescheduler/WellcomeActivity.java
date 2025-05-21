package com.example.timetablescheduler;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.parse.ParseUser;

public class WellcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_wellcome);

        // Handle window insets for modern UI
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get current user
        ParseUser currentUser = ParseUser.getCurrentUser();

        TextView welcomeText = findViewById(R.id.textView);
        Button logoutButton = findViewById(R.id.button);

        if (currentUser != null) {
            String displayName = currentUser.getString("displayName");
            welcomeText.setText("Welcome, " + displayName + "!");
        } else {
            // No user, go back to login
            Intent intent = new Intent(WellcomeActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        logoutButton.setOnClickListener(v -> {
            ParseUser.logOutInBackground(e -> {
                if (e == null) {
                    Toast.makeText(WellcomeActivity.this, "Logged out successfully!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(WellcomeActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(WellcomeActivity.this, "Logout failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
