package com.example.timetablescheduler;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.parse.ParseUser;

public class SignupActivity extends AppCompatActivity {

    private EditText etUsername, etEmail, etPassword, etConfirmPassword;
    private Button btnFinish;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        etUsername = findViewById(R.id.editTextText3);
        etEmail = findViewById(R.id.editTextText4);
        etPassword = findViewById(R.id.editTextText5);
        etConfirmPassword = findViewById(R.id.editTextText6);
        btnFinish = findViewById(R.id.button3);

        btnFinish.setOnClickListener(v -> handleSignup());
    }

    private void handleSignup() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showToast("All fields are required");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showToast("Passwords do not match");
            return;
        }

        ParseUser user = new ParseUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);

        user.signUpInBackground(e -> {
            if (e == null) {
                // Log out the user immediately after signup
                ParseUser.logOut();

                showToast("Signup successful! Please login");
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
