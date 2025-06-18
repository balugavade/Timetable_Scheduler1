package com.example.timetablescheduler;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.parse.*;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;

public class TeachersActivity extends AppCompatActivity {

    private Spinner spinnerTeachers;
    private TextInputEditText etTeacherName, etTeacherPosition, etTeacherLoad, etSubjects, etTeacherDepartment;
    private Button btnDeleteTeacher;
    private List<ParseObject> teacherList = new ArrayList<>();
    private ParseObject selectedTeacher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teachers);

        // Initialize views
        spinnerTeachers = findViewById(R.id.spinnerTeachers);
        etTeacherName = findViewById(R.id.etTeacherName);
        etTeacherPosition = findViewById(R.id.etTeacherPosition);
        etTeacherLoad = findViewById(R.id.etTeacherLoad);
        etSubjects = findViewById(R.id.etSubjects);
        etTeacherDepartment = findViewById(R.id.etTeacherDepartment);
        btnDeleteTeacher = findViewById(R.id.btnDeleteTeacher);

        // Setup listeners
        setupSpinner();
        setupButtonListeners();
        fetchTeachers();
    }

    private void setupSpinner() {
        spinnerTeachers.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < teacherList.size()) {
                    selectedTeacher = teacherList.get(position);
                    populateTeacherData(selectedTeacher);
                    btnDeleteTeacher.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                clearForm();
                btnDeleteTeacher.setVisibility(View.GONE);
            }
        });
    }

    private void populateTeacherData(ParseObject teacher) {
        etTeacherName.setText(teacher.getString("name"));
        etTeacherPosition.setText(teacher.getString("position"));
        etTeacherLoad.setText(teacher.getString("load"));
        etTeacherDepartment.setText(teacher.getString("department"));

        List<String> subjects = teacher.getList("subjects");
        if (subjects != null) {
            etSubjects.setText(TextUtils.join(", ", subjects));
        }
    }

    private void clearForm() {
        etTeacherName.setText("");
        etTeacherPosition.setText("");
        etTeacherLoad.setText("");
        etSubjects.setText("");
        etTeacherDepartment.setText("");
        selectedTeacher = null;
        btnDeleteTeacher.setVisibility(View.GONE);
    }

    private void setupButtonListeners() {
        findViewById(R.id.btnSaveTeacher).setOnClickListener(v -> saveTeacher());
        findViewById(R.id.btnDeleteTeacher).setOnClickListener(v -> deleteTeacher());
        findViewById(R.id.fabAddTeacher).setOnClickListener(v -> clearForm());
    }

    private void saveTeacher() {
        String name = etTeacherName.getText().toString().trim();
        String position = etTeacherPosition.getText().toString().trim();
        String load = etTeacherLoad.getText().toString().trim();
        String department = etTeacherDepartment.getText().toString().trim();
        String subjects = etSubjects.getText().toString().trim();

        if (name.isEmpty() || position.isEmpty() || load.isEmpty()) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        ParseObject teacher = (selectedTeacher != null) ? selectedTeacher : new ParseObject("Teacher");
        teacher.put("user", ParseUser.getCurrentUser());
        teacher.put("name", name);
        teacher.put("position", position);
        teacher.put("load", load);
        teacher.put("department", department);

        // Process subjects
        List<String> subjectsList = new ArrayList<>();
        if (!subjects.isEmpty()) {
            String[] subjectsArray = subjects.split(",");
            for (String subject : subjectsArray) {
                String trimmed = subject.trim();
                if (!trimmed.isEmpty()) {
                    subjectsList.add(trimmed);
                }
            }
        }
        teacher.put("subjects", subjectsList);

        teacher.saveInBackground(e -> {
            if (e == null) {
                Toast.makeText(this, "Teacher saved!", Toast.LENGTH_SHORT).show();
                fetchTeachers();
                clearForm();
            } else {
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteTeacher() {
        if (selectedTeacher != null) {
            selectedTeacher.deleteInBackground(e -> {
                if (e == null) {
                    Toast.makeText(this, "Teacher deleted", Toast.LENGTH_SHORT).show();
                    fetchTeachers();
                    clearForm();
                } else {
                    Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void fetchTeachers() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Teacher");
        query.whereEqualTo("user", ParseUser.getCurrentUser());
        query.findInBackground((teachers, e) -> {
            if (e == null) {
                teacherList = teachers;
                updateTeacherSpinner();
            } else {
                Toast.makeText(this, "Error loading teachers", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateTeacherSpinner() {
        List<String> teacherNames = new ArrayList<>();
        for (ParseObject teacher : teacherList) {
            teacherNames.add(teacher.getString("name"));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, teacherNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTeachers.setAdapter(adapter);
    }
}
