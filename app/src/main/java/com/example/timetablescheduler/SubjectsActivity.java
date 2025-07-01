package com.example.timetablescheduler;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.parse.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SubjectsActivity extends AppCompatActivity {

    private Spinner spinnerSubjects, spinnerTeacher, spinnerSemester;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabAddSubject;
    private com.google.android.material.textfield.TextInputEditText etSubjectCode, etSubjectName, etLecturesWeekly, etLabsWeekly;
    private CheckBox cbLab;
    private Button btnSaveSubject, btnDeleteSubject;
    private androidx.cardview.widget.CardView cardSubjectDetail;

    private List<ParseObject> subjectList = new ArrayList<>();
    private List<ParseObject> teachersList = new ArrayList<>();
    private ArrayAdapter<String> subjectAdapter, teacherAdapter, semesterAdapter;
    private int selectedSubjectIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subjects);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        spinnerSubjects = findViewById(R.id.spinnerSubjects);
        fabAddSubject = findViewById(R.id.fabAddSubject);
        cardSubjectDetail = findViewById(R.id.cardSubjectDetail);

        etSubjectCode = findViewById(R.id.etSubjectCode);
        etSubjectName = findViewById(R.id.etSubjectName);
        spinnerTeacher = findViewById(R.id.spinnerTeacher);
        spinnerSemester = findViewById(R.id.spinnerSemester);
        cbLab = findViewById(R.id.cbLab);
        etLecturesWeekly = findViewById(R.id.etLecturesWeekly);
        etLabsWeekly = findViewById(R.id.etLabsWeekly);
        btnSaveSubject = findViewById(R.id.btnSaveSubject);
        btnDeleteSubject = findViewById(R.id.btnDeleteSubject);

        String[] semesters = {"I", "II", "III", "IV"};
        semesterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, semesters);
        semesterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSemester.setAdapter(semesterAdapter);

        cbLab.setOnCheckedChangeListener((buttonView, checked) -> etLabsWeekly.setEnabled(checked));

        fabAddSubject.setOnClickListener(v -> showAddSubject());
        btnSaveSubject.setOnClickListener(v -> saveOrUpdateSubject());
        btnDeleteSubject.setOnClickListener(v -> deleteSubject());

        spinnerSubjects.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                showSubjectDetails(position);
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
                teachersList.clear();
                teachersList.addAll(objects);
                updateTeacherSpinner();
                fetchSubjectsFromBack4App();
            } else {
                Toast.makeText(this, "Error fetching teachers: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateTeacherSpinner() {
        List<String> names = new ArrayList<>();
        for (ParseObject teacher : teachersList) {
            names.add(teacher.getString("name"));
        }
        teacherAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        teacherAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTeacher.setAdapter(teacherAdapter);
    }

    private void fetchSubjectsFromBack4App() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Subject");
        query.whereEqualTo("user", ParseUser.getCurrentUser());
        query.include("teacher");
        query.findInBackground((objects, e) -> {
            if (e == null) {
                subjectList.clear();
                subjectList.addAll(objects);
                updateSubjectSpinner();
                if (!subjectList.isEmpty()) {
                    showSubjectDetails(0);
                } else {
                    showAddSubject();
                }
            } else {
                Toast.makeText(this, "Error fetching subjects: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSubjectSpinner() {
        List<String> names = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();
        for (ParseObject subject : subjectList) {
            String name = subject.getString("name");
            if (name != null && !seen.contains(name)) {
                names.add(name);
                seen.add(name);
            }
        }
        subjectAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubjects.setAdapter(subjectAdapter);
    }

    private void showSubjectDetails(int position) {
        if (position < 0 || position >= subjectList.size()) return;
        selectedSubjectIndex = position;
        ParseObject subject = subjectList.get(position);

        etSubjectCode.setText(subject.has("code") ? subject.getString("code") : "");
        etSubjectName.setText(subject.getString("name"));
        cbLab.setChecked(subject.has("isLab") && subject.getBoolean("isLab"));
        etLecturesWeekly.setText(subject.has("lecturesWeekly") ? String.valueOf(subject.getNumber("lecturesWeekly")) : "");
        etLabsWeekly.setText(subject.has("labsWeekly") ? String.valueOf(subject.getNumber("labsWeekly")) : "");

        ParseObject teacher = subject.getParseObject("teacher");
        if (teacher != null) {
            for (int i = 0; i < teachersList.size(); i++) {
                if (teachersList.get(i).getObjectId().equals(teacher.getObjectId())) {
                    spinnerTeacher.setSelection(i);
                    break;
                }
            }
        } else if (!teachersList.isEmpty()) {
            spinnerTeacher.setSelection(0);
        }

        String semester = subject.getString("semester");
        if (semester != null) {
            int pos = semesterAdapter.getPosition(semester);
            if (pos >= 0) spinnerSemester.setSelection(pos);
        } else {
            spinnerSemester.setSelection(0);
        }

        btnSaveSubject.setText("Save");
        btnDeleteSubject.setVisibility(View.VISIBLE);
    }

    private void showAddSubject() {
        selectedSubjectIndex = -1;
        etSubjectCode.setText("");
        etSubjectName.setText("");
        cbLab.setChecked(false);
        etLecturesWeekly.setText("");
        etLabsWeekly.setText("");
        spinnerTeacher.setSelection(0);
        spinnerSemester.setSelection(0);
        btnSaveSubject.setText("Add");
        btnDeleteSubject.setVisibility(View.GONE);
    }

    private void saveOrUpdateSubject() {
        String code = etSubjectCode.getText().toString().trim();
        String name = etSubjectName.getText().toString().trim();
        int teacherPos = spinnerTeacher.getSelectedItemPosition();
        ParseObject teacher = teacherPos >= 0 && teacherPos < teachersList.size() ? teachersList.get(teacherPos) : null;
        String semester = spinnerSemester.getSelectedItem().toString();
        boolean isLab = cbLab.isChecked();
        String lecturesWeeklyStr = etLecturesWeekly.getText().toString().trim();
        String labsWeeklyStr = etLabsWeekly.getText().toString().trim();

        int lecturesWeekly = lecturesWeeklyStr.isEmpty() ? 0 : Integer.parseInt(lecturesWeeklyStr);
        int labsWeekly = labsWeeklyStr.isEmpty() ? 0 : Integer.parseInt(labsWeeklyStr);

        if (code.isEmpty() || name.isEmpty() || teacher == null) {
            Toast.makeText(this, "Please fill all fields and select a teacher", Toast.LENGTH_SHORT).show();
            return;
        }

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Subject");
        query.whereEqualTo("user", ParseUser.getCurrentUser());
        query.whereEqualTo("code", code);
        query.getFirstInBackground((existingSubject, e) -> {
            if (e == null && existingSubject != null) {
                // Update existing
                existingSubject.put("name", name);
                existingSubject.put("teacher", teacher);
                existingSubject.put("semester", semester);
                existingSubject.put("isLab", isLab);
                existingSubject.put("lecturesWeekly", lecturesWeekly);
                existingSubject.put("labsWeekly", labsWeekly);
                existingSubject.saveInBackground(err -> {
                    if (err == null) {
                        Toast.makeText(this, "Subject updated!", Toast.LENGTH_SHORT).show();
                        fetchSubjectsFromBack4App();
                    } else {
                        Toast.makeText(this, "Error: " + err.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // Create new subject
                ParseObject subject = new ParseObject("Subject");
                subject.put("user", ParseUser.getCurrentUser());
                subject.put("code", code);
                subject.put("name", name);
                subject.put("teacher", teacher);
                subject.put("semester", semester);
                subject.put("isLab", isLab);
                subject.put("lecturesWeekly", lecturesWeekly);
                subject.put("labsWeekly", labsWeekly);
                subject.saveInBackground(err -> {
                    if (err == null) {
                        Toast.makeText(this, "Subject added!", Toast.LENGTH_SHORT).show();
                        fetchSubjectsFromBack4App();
                    } else {
                        Toast.makeText(this, "Error: " + err.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void deleteSubject() {
        if (selectedSubjectIndex >= 0 && selectedSubjectIndex < subjectList.size()) {
            ParseObject subject = subjectList.get(selectedSubjectIndex);
            subject.deleteInBackground(e -> {
                if (e == null) {
                    Toast.makeText(this, "Subject deleted", Toast.LENGTH_SHORT).show();
                    fetchSubjectsFromBack4App();
                    showAddSubject();
                } else {
                    Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
