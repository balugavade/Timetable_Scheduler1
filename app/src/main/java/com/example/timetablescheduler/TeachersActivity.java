package com.example.timetablescheduler;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import java.util.ArrayList;
import java.util.List;

public class TeachersActivity extends AppCompatActivity {

    private Spinner spinnerTeachers;
    private FloatingActionButton fabAddTeacher;
    private EditText etTeacherName, etTeacherPosition, etTeacherLoad, etTeacherSubjects, etTeacherDepartment;
    private Button btnSaveTeacher;
    private CardView cardTeacherDetail;

    private List<ParseObject> teacherList = new ArrayList<>();
    private ArrayAdapter<String> teacherAdapter;
    private int selectedTeacherIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teachers);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        spinnerTeachers = findViewById(R.id.spinnerTeachers);
        fabAddTeacher = findViewById(R.id.fabAddTeacher);
        cardTeacherDetail = findViewById(R.id.cardTeacherDetail);

        etTeacherName = findViewById(R.id.etTeacherName);
        etTeacherPosition = findViewById(R.id.etTeacherPosition);
        etTeacherLoad = findViewById(R.id.etTeacherLoad);
        etTeacherSubjects = findViewById(R.id.etTeacherSubjects);
        etTeacherDepartment = findViewById(R.id.etTeacherDepartment);
        btnSaveTeacher = findViewById(R.id.btnSaveTeacher);

        fabAddTeacher.setOnClickListener(v -> showAddTeacher());

        btnSaveTeacher.setOnClickListener(v -> saveTeacher());

        spinnerTeachers.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                showTeacherDetails(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        fetchTeachersFromBack4App();
    }

    private void fetchTeachersFromBack4App() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Teacher");
        query.whereEqualTo("user", ParseUser.getCurrentUser());
        query.findInBackground((objects, e) -> {
            if (e == null) {
                teacherList.clear();
                teacherList.addAll(objects);
                updateTeacherSpinner();
                if (!teacherList.isEmpty()) {
                    showTeacherDetails(0);
                } else {
                    showAddTeacher();
                }
            } else {
                Toast.makeText(this, "Error fetching teachers: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateTeacherSpinner() {
        List<String> names = new ArrayList<>();
        for (ParseObject teacher : teacherList) {
            names.add(teacher.getString("name"));
        }
        teacherAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        teacherAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTeachers.setAdapter(teacherAdapter);
    }

    private void showTeacherDetails(int position) {
        if (position < 0 || position >= teacherList.size()) return;
        selectedTeacherIndex = position;
        ParseObject teacher = teacherList.get(position);

        etTeacherName.setText(teacher.getString("name"));
        etTeacherPosition.setText(teacher.getString("position"));
        etTeacherLoad.setText(teacher.getString("load"));
        etTeacherSubjects.setText(teacher.getString("subjects"));
        etTeacherDepartment.setText(teacher.getString("department"));
        btnSaveTeacher.setText("Save");
    }

    private void showAddTeacher() {
        selectedTeacherIndex = -1;
        etTeacherName.setText("");
        etTeacherPosition.setText("");
        etTeacherLoad.setText("");
        etTeacherSubjects.setText("");
        etTeacherDepartment.setText("");
        btnSaveTeacher.setText("Add");
        spinnerTeachers.setSelection(-1);
    }

    private void saveTeacher() {
        String name = etTeacherName.getText().toString().trim();
        String position = etTeacherPosition.getText().toString().trim();
        String load = etTeacherLoad.getText().toString().trim();
        String subjects = etTeacherSubjects.getText().toString().trim();
        String department = etTeacherDepartment.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedTeacherIndex >= 0 && selectedTeacherIndex < teacherList.size()) {
            // Update existing teacher
            ParseObject teacher = teacherList.get(selectedTeacherIndex);
            teacher.put("name", name);
            teacher.put("position", position);
            teacher.put("load", load);
            teacher.put("subjects", subjects);
            teacher.put("department", department);
            teacher.saveInBackground(e -> {
                if (e == null) {
                    Toast.makeText(this, "Teacher updated!", Toast.LENGTH_SHORT).show();
                    fetchTeachersFromBack4App();
                } else {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Add new teacher
            ParseObject teacher = new ParseObject("Teacher");
            teacher.put("user", ParseUser.getCurrentUser());
            teacher.put("name", name);
            teacher.put("position", position);
            teacher.put("load", load);
            teacher.put("subjects", subjects);
            teacher.put("department", department);
            teacher.saveInBackground(e -> {
                if (e == null) {
                    Toast.makeText(this, "Teacher added!", Toast.LENGTH_SHORT).show();
                    fetchTeachersFromBack4App();
                } else {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
