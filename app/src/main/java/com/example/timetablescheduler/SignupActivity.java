package com.example.timetablescheduler;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.parse.ParseUser;

public class SignupActivity extends AppCompatActivity {

    private EditText etDisplayName, etEmail, etPassword, etConfirmPassword;
    private Button btnFinish;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        etDisplayName = findViewById(R.id.editTextText3);
        etEmail = findViewById(R.id.editTextText4);
        etPassword = findViewById(R.id.editTextText5);
        etConfirmPassword = findViewById(R.id.editTextText6);
        btnFinish = findViewById(R.id.button3);

        btnFinish.setOnClickListener(v -> handleSignup());
    }

    private void handleSignup() {
        String displayName = etDisplayName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (displayName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showToast("All fields are required");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showToast("Passwords do not match");
            return;
        }

        // Optional: Only allow rvce.edu.in emails
        // if (!email.matches("^[A-Za-z0-9._%+-]+@rvce\\.edu\\.in$")) {
        //     showToast("Please use your rvce.edu.in email address");
        //     return;
        // }

        ParseUser user = new ParseUser();
        user.setUsername(email); // Use email as unique username
        user.setEmail(email);
        user.setPassword(password);
        user.put("displayName", displayName); // Custom field for display name

        user.signUpInBackground(e -> {
            if (e == null) {
                ParseUser.logOut(); // Log out after signup so user can login manually
                showToast("Signup successful! Please login.");
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                showToast("Signup failed: " + e.getMessage());
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
