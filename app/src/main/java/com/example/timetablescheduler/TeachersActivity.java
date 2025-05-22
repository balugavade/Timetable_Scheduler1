package com.example.timetablescheduler;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.parse.ParseObject;
import com.parse.ParseUser;
import java.util.ArrayList;
import java.util.List;

public class TeachersActivity extends AppCompatActivity {

    private EditText etNumTeachers;
    private Button btnGenerateTeacherFields, btnNext;
    private LinearLayout layoutTeachersContainer;

    // Lists to store dynamically created fields
    private List<EditText> teacherNameEditTexts = new ArrayList<>();
    private List<EditText> teacherSubjectEditTexts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teachers);

        etNumTeachers = findViewById(R.id.etNumTeachers);
        btnGenerateTeacherFields = findViewById(R.id.btnGenerateTeacherFields);
        btnNext = findViewById(R.id.btnNext);
        layoutTeachersContainer = findViewById(R.id.layoutTeachersContainer);

        btnGenerateTeacherFields.setOnClickListener(v -> generateTeacherFields());
        btnNext.setOnClickListener(v -> saveTeachersToBack4App());
    }

    private void generateTeacherFields() {
        layoutTeachersContainer.removeAllViews();
        teacherNameEditTexts.clear();
        teacherSubjectEditTexts.clear();

        String numTeachersStr = etNumTeachers.getText().toString().trim();
        if (numTeachersStr.isEmpty()) {
            Toast.makeText(this, "Enter number of teachers", Toast.LENGTH_SHORT).show();
            return;
        }
        int numTeachers = Integer.parseInt(numTeachersStr);

        // Create a TextView header for the table
        TextView headerTextView = new TextView(this);
        headerTextView.setText("Name                                Subject");
        headerTextView.setTextSize(16);
        headerTextView.setPadding(0, 10, 0, 10);
        layoutTeachersContainer.addView(headerTextView);

        for (int i = 0; i < numTeachers; i++) {
            // Create horizontal layout for each row
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            row.setPadding(0, 8, 0, 8);

            // Teacher name field
            EditText etTeacherName = new EditText(this);
            etTeacherName.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            etTeacherName.setHint("Name");
            etTeacherName.setInputType(InputType.TYPE_CLASS_TEXT);

            // Subject field
            EditText etSubject = new EditText(this);
            etSubject.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            etSubject.setHint("Subject");
            etSubject.setInputType(InputType.TYPE_CLASS_TEXT);

            // Add to tracking lists
            teacherNameEditTexts.add(etTeacherName);
            teacherSubjectEditTexts.add(etSubject);

            // Add to row
            row.addView(etTeacherName);
            row.addView(etSubject);

            // Add row to container
            layoutTeachersContainer.addView(row);
        }
    }

    private void saveTeachersToBack4App() {
        try {
            if (teacherNameEditTexts.isEmpty() || teacherSubjectEditTexts.isEmpty()) {
                Toast.makeText(this, "Please add teachers first", Toast.LENGTH_SHORT).show();
                return;
            }

            List<String> teacherNames = new ArrayList<>();
            List<String> teacherSubjects = new ArrayList<>();

            // Collect data from input fields
            for (int i = 0; i < teacherNameEditTexts.size(); i++) {
                String name = teacherNameEditTexts.get(i).getText().toString().trim();
                String subject = teacherSubjectEditTexts.get(i).getText().toString().trim();

                if (name.isEmpty() || subject.isEmpty()) {
                    Toast.makeText(this, "Please fill all teacher and subject fields",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                teacherNames.add(name);
                teacherSubjects.add(subject);

                // Create individual teacher objects
                ParseObject teacher = new ParseObject("Teacher");
                teacher.put("user", ParseUser.getCurrentUser());
                teacher.put("name", name);
                teacher.put("subject", subject);

                teacher.saveInBackground(e -> {
                    if (e != null) {
                        Log.e("TeacherSave", "Error saving teacher: " + e.getMessage());
                    }
                });
            }

            // Also save a summary object that contains all teachers and subjects
            ParseObject teacherSummary = new ParseObject("TeacherSummary");
            teacherSummary.put("user", ParseUser.getCurrentUser());
            teacherSummary.put("teacherNames", teacherNames);
            teacherSummary.put("teacherSubjects", teacherSubjects);

            teacherSummary.saveInBackground(e -> {
                if (e == null) {
                    Toast.makeText(TeachersActivity.this,
                            "Teachers saved successfully!", Toast.LENGTH_SHORT).show();

                    // Navigate to next activity - you'll need to change this
                    // to whatever your next activity should be
                    navigateToNextActivity();
                } else {
                    Toast.makeText(TeachersActivity.this,
                            "Error saving teachers: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToNextActivity() {
        // Change SubjectsActivity.class to whatever your next activity should be
        Intent intent = new Intent(TeachersActivity.this, WellcomeActivity.class);
        startActivity(intent);
        finish();
    }
}
