package com.example.timetablescheduler;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.ParseObject;

public class MainActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnSignup;

    // Role emails
    private static final String ADMIN_EMAIL      = "admin_mca@rvce.edu.in";
    private static final String DEAN_EMAIL       = "dean_timetable@rvce.edu.in";
    private static final String PRINCIPAL_EMAIL  = "principal_rvce@rvce.edu.in";

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

        // Auto-login if already logged in user
        if (ParseUser.getCurrentUser() != null) {
            String currentEmail = ParseUser.getCurrentUser().getUsername();
            navigateByRole(currentEmail);
        }
    }

    private void handleLogin() {
        String email = etEmail.getText().toString().trim().toLowerCase();
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

        ParseUser.logInInBackground(email, password, (user, e) -> {
            if (user != null) {
                navigateByRole(email);
            } else {
                showToast("Login failed: " + (e != null ? e.getMessage() : "Invalid credentials"));
            }
        });
    }

    private void navigateByRole(String email) {
        if (email.equalsIgnoreCase(ADMIN_EMAIL)) {
            navigateToAdmin();
        } else if (email.equalsIgnoreCase(DEAN_EMAIL)) {
            navigateToDean();
        } else if (email.equalsIgnoreCase(PRINCIPAL_EMAIL)) {
            navigateToPrincipal();
        } else {
            // Check Teacher table for faculty, else Welcome
            checkIfFacultyAndNavigate(email);
        }
    }

    private void checkIfFacultyAndNavigate(String email) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Teacher");
        query.whereEqualTo("email", email);
        query.getFirstInBackground((teacher, ex) -> {
            if (teacher != null) {
                // Open faculty dashboard
                Intent intent = new Intent(MainActivity.this, FacultyDashboardActivity.class);
                intent.putExtra("teacherEmail", email);
                startActivity(intent);
                finish();
            } else {
                // Open student/general dashboard (Welcome screen)
                navigateToWelcome();
            }
        });
    }

    private void navigateToAdmin() {
        startActivity(new Intent(this, AdminApproveTimetableActivity.class));
        finish();
    }

    private void navigateToDean() {
        startActivity(new Intent(this, DeanApproveTimetableActivity.class));
        finish();
    }

    private void navigateToPrincipal() {
        startActivity(new Intent(this, PrincipalApproveTimetableActivity.class));
        finish();
    }

    private void navigateToWelcome() {
        startActivity(new Intent(this, WelcomeActivity.class));
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
