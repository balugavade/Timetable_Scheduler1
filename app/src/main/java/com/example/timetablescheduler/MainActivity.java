package com.example.timetablescheduler;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.parse.ParseUser;

public class MainActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etEmail = findViewById(R.id.editTextText);
        etPassword = findViewById(R.id.editTextText2);
        btnLogin = findViewById(R.id.button1);
        btnSignup = findViewById(R.id.btnSignup);

        btnLogin.setOnClickListener(v -> handleLogin());
        btnSignup.setOnClickListener(v -> {
            startActivity(new Intent(this, SignupActivity.class));
        });

        // Auto-login if user exists
        if (ParseUser.getCurrentUser() != null) {
            if (isAdmin(ParseUser.getCurrentUser().getUsername())) {
                navigateToAdmin();
            } else {
                navigateToWelcome();
            }
        }
    }

    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showToast("All fields are required");
            return;
        }

        // Regex to allow only rvce.edu.in emails
        if (!email.matches("^[A-Za-z0-9._%+-]+@rvce\\.edu\\.in$")) {
            showToast("Please enter a valid rvce.edu.in email address");
            return;
        }

        // Login with email by querying users
        ParseUser.logInInBackground(email, password, (user, e) -> {
            if (user != null) {
                if (isAdmin(email)) {
                    navigateToAdmin();
                } else {
                    navigateToWelcome();
                }
            } else {
                showToast("Login failed: " + (e != null ? e.getMessage() : "Invalid credentials"));
            }
        });
    }

    private boolean isAdmin(String email) {
        return "admin_mca@rvce.edu.in".equalsIgnoreCase(email);
    }

    private void navigateToWelcome() {
        startActivity(new Intent(this, WelcomeActivity.class));
        finish();
    }

    private void navigateToAdmin() {
        startActivity(new Intent(this, AdminApproveTimetableActivity.class));
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}