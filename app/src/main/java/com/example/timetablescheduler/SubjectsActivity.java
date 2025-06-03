package com.example.timetablescheduler;

import android.os.Bundle;
import android.view.LayoutInflater;
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

public class SubjectsActivity extends AppCompatActivity {

    private LinearLayout layoutSubjectsContainer;
    private Button btnEditSubjects, btnSaveSubjects;
    private FloatingActionButton fabAddSubject;
    private boolean isEditMode = false;
    private List<View> subjectViews = new ArrayList<>();
    private List<ParseObject> teachersList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subjects);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        layoutSubjectsContainer = findViewById(R.id.layoutSubjectsContainer);
        btnEditSubjects = findViewById(R.id.btnEditSubjects);
        btnSaveSubjects = findViewById(R.id.btnSaveSubjects);
        fabAddSubject = findViewById(R.id.fabAddSubject);

        btnEditSubjects.setOnClickListener(v -> toggleEditMode());
        btnSaveSubjects.setOnClickListener(v -> saveSubjectsToBack4App());
        fabAddSubject.setOnClickListener(v -> addSubjectField(null, null, null, null, null, false, null));

        fetchTeachersFromBack4App();
    }

    private void fetchTeachersFromBack4App() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Teacher");
        query.whereEqualTo("user", ParseUser.getCurrentUser());
        query.findInBackground((objects, e) -> {
            if (e == null) {
                teachersList.clear();
                teachersList.addAll(objects);
                fetchSubjectsFromBack4App();
            } else {
                Toast.makeText(this, "Error fetching teachers: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchSubjectsFromBack4App() {
        layoutSubjectsContainer.removeAllViews();
        subjectViews.clear();

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Subject");
        query.whereEqualTo("user", ParseUser.getCurrentUser());
        query.findInBackground((objects, e) -> {
            if (e == null) {
                if (objects.isEmpty()) {
                    addSubjectField(null, null, null, null, null, false, null);
                } else {
                    for (ParseObject obj : objects) {
                        addSubjectField(
                                obj.getString("name"),
                                obj.getParseObject("teacher"),
                                obj.getInt("lecturesWeekly") == 0 ? "" : String.valueOf(obj.getInt("lecturesWeekly")),
                                obj.getString("semester"),
                                obj.getInt("labsWeekly") == 0 ? "" : String.valueOf(obj.getInt("labsWeekly")),
                                obj.getBoolean("isLab"),
                                obj.getObjectId()
                        );
                    }
                }
            } else {
                Toast.makeText(this, "Error fetching subjects: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addSubjectField(String name, ParseObject selectedTeacher, String lecturesWeekly, String semester, String labsWeekly, boolean isLab, String objectId) {
        CardView card = (CardView) LayoutInflater.from(this)
                .inflate(R.layout.subject_item, layoutSubjectsContainer, false);

        EditText etSubjectName = card.findViewById(R.id.etSubjectName);
        Spinner spinnerTeacher = card.findViewById(R.id.spinnerTeacher);
        Spinner spinnerSemester = card.findViewById(R.id.spinnerSemester);
        CheckBox cbLab = card.findViewById(R.id.cbLab);
        EditText etLecturesWeekly = card.findViewById(R.id.etLecturesWeekly);
        EditText etLabsWeekly = card.findViewById(R.id.etLabsWeekly);
        ImageButton btnDelete = card.findViewById(R.id.btnDeleteSubject);

        // Set values
        etSubjectName.setText(name != null ? name : "");
        etLecturesWeekly.setText(lecturesWeekly != null ? lecturesWeekly : "");
        etLabsWeekly.setText(labsWeekly != null ? labsWeekly : "");
        cbLab.setChecked(isLab);

        // Teacher spinner setup
        ArrayAdapter<String> teacherAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, getTeacherNames());
        teacherAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTeacher.setAdapter(teacherAdapter);

        // Set selected teacher if editing
        if (selectedTeacher != null) {
            for (int i = 0; i < teachersList.size(); i++) {
                if (teachersList.get(i).getObjectId().equals(selectedTeacher.getObjectId())) {
                    spinnerTeacher.setSelection(i);
                    break;
                }
            }
        }

        // Semester spinner setup (I–VIII)
        String[] semesters = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII"};
        ArrayAdapter<String> semesterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, semesters);
        semesterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSemester.setAdapter(semesterAdapter);

        // Set selected semester if editing
        if (semester != null) {
            int pos = semesterAdapter.getPosition(semester);
            if (pos >= 0) spinnerSemester.setSelection(pos);
        }

        // Show/hide labsWeekly field depending on isLab
        etLabsWeekly.setEnabled(isLab);
        cbLab.setOnCheckedChangeListener((buttonView, checked) -> etLabsWeekly.setEnabled(checked));

        btnDelete.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        btnDelete.setOnClickListener(v -> {
            layoutSubjectsContainer.removeView(card);
            subjectViews.remove(card);
            if (objectId != null) {
                ParseObject obj = ParseObject.createWithoutData("Subject", objectId);
                obj.deleteInBackground();
            }
        });

        card.setTag(objectId);
        layoutSubjectsContainer.addView(card);
        subjectViews.add(card);
    }

    private List<String> getTeacherNames() {
        List<String> names = new ArrayList<>();
        for (ParseObject teacher : teachersList) {
            names.add(teacher.getString("name"));
        }
        return names;
    }

    private void toggleEditMode() {
        isEditMode = !isEditMode;
        for (View card : subjectViews) {
            ImageButton btnDelete = card.findViewById(R.id.btnDeleteSubject);
            btnDelete.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        }
        btnEditSubjects.setText(isEditMode ? "Done Editing" : "Edit Subjects");
    }

    private void saveSubjectsToBack4App() {
        for (View card : subjectViews) {
            EditText etSubjectName = card.findViewById(R.id.etSubjectName);
            Spinner spinnerTeacher = card.findViewById(R.id.spinnerTeacher);
            Spinner spinnerSemester = card.findViewById(R.id.spinnerSemester);
            CheckBox cbLab = card.findViewById(R.id.cbLab);
            EditText etLecturesWeekly = card.findViewById(R.id.etLecturesWeekly);
            EditText etLabsWeekly = card.findViewById(R.id.etLabsWeekly);

            String name = etSubjectName.getText().toString().trim();
            int teacherPos = spinnerTeacher.getSelectedItemPosition();
            ParseObject teacher = teacherPos >= 0 && teacherPos < teachersList.size() ? teachersList.get(teacherPos) : null;
            String selectedSemester = spinnerSemester.getSelectedItem().toString();
            boolean isLab = cbLab.isChecked();
            String lecturesWeeklyStr = etLecturesWeekly.getText().toString().trim();
            String labsWeeklyStr = etLabsWeekly.getText().toString().trim();
            String objectId = (String) card.getTag();

            int lecturesWeekly = lecturesWeeklyStr.isEmpty() ? 0 : Integer.parseInt(lecturesWeeklyStr);
            int labsWeekly = labsWeeklyStr.isEmpty() ? 0 : Integer.parseInt(labsWeeklyStr);

            if (!name.isEmpty()) {
                if (objectId != null) {
                    // Update existing subject
                    ParseQuery<ParseObject> query = ParseQuery.getQuery("Subject");
                    query.getInBackground(objectId, (obj, e) -> {
                        if (e == null && obj != null) {
                            obj.put("name", name);
                            obj.put("teacher", teacher);
                            obj.put("semester", selectedSemester);
                            obj.put("isLab", isLab);
                            obj.put("lecturesWeekly", lecturesWeekly);
                            obj.put("labsWeekly", labsWeekly);
                            obj.saveInBackground();
                        }
                    });
                } else {
                    // Create new subject
                    ParseObject subject = new ParseObject("Subject");
                    subject.put("user", ParseUser.getCurrentUser());
                    subject.put("name", name);
                    subject.put("teacher", teacher);
                    subject.put("semester", selectedSemester);
                    subject.put("isLab", isLab);
                    subject.put("lecturesWeekly", lecturesWeekly);
                    subject.put("labsWeekly", labsWeekly);
                    subject.saveInBackground();
                }
            }
        }
        Toast.makeText(this, "Subjects saved!", Toast.LENGTH_SHORT).show();
    }
}
