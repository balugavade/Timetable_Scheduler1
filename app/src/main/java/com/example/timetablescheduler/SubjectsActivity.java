package com.example.timetablescheduler;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SubjectsActivity extends AppCompatActivity {

    private LinearLayout layoutSubjectsContainer;
    private Button btnEditSubjects, btnSaveSubjects;
    private boolean isEditMode = false;
    private List<View> subjectViews = new ArrayList<>();
    private List<HashMap<String, String>> teachersList = new ArrayList<>();

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

        btnEditSubjects.setOnClickListener(v -> toggleEditMode());
        btnSaveSubjects.setOnClickListener(v -> saveSubjectsToBack4App());

        findViewById(R.id.fabAddSubject).setOnClickListener(v ->
                addSubjectField(null, null, false, null, null, null, null));

        fetchTeachersAndSubjects();
    }

    private void fetchTeachersAndSubjects() {
        // Fetch teachers first
        ParseQuery<ParseObject> teacherQuery = ParseQuery.getQuery("Teacher");
        teacherQuery.whereEqualTo("user", ParseUser.getCurrentUser());
        teacherQuery.findInBackground((teachers, e) -> {
            teachersList.clear();
            if (e == null) {
                for (ParseObject teacher : teachers) {
                    HashMap<String, String> map = new HashMap<>();
                    map.put("objectId", teacher.getObjectId());
                    map.put("name", teacher.getString("name"));
                    teachersList.add(map);
                }
            }
            // Now fetch subjects
            fetchSubjectsFromBack4App();
        });
    }

    private void fetchSubjectsFromBack4App() {
        layoutSubjectsContainer.removeAllViews();
        subjectViews.clear();

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Subject");
        query.whereEqualTo("user", ParseUser.getCurrentUser());
        query.findInBackground((objects, e) -> {
            if (e == null) {
                for (ParseObject obj : objects) {
                    addSubjectField(
                            obj.getString("name"),
                            obj.getParseObject("teacher").getObjectId(),
                            obj.getBoolean("lab"),
                            obj.getString("lecturesWeekly"),
                            obj.getString("semester"),
                            obj.getString("labsWeekly"),
                            obj.getObjectId()
                    );
                }
            }
        });
    }

    private void addSubjectField(String name, String teacherId, boolean lab,
                                 String lectures, String semester, String labs, String objectId) {
        CardView card = (CardView) LayoutInflater.from(this)
                .inflate(R.layout.subject_item, layoutSubjectsContainer, false);

        EditText etName = card.findViewById(R.id.etSubjectName);
        Spinner spinnerTeacher = card.findViewById(R.id.spinnerTeacher);
        CheckBox cbLab = card.findViewById(R.id.cbLab);
        EditText etLectures = card.findViewById(R.id.etLecturesWeekly);
        EditText etSemester = card.findViewById(R.id.etSemester);
        EditText etLabs = card.findViewById(R.id.etLabsWeekly);
        ImageButton btnDelete = card.findViewById(R.id.btnDeleteSubject);

        // Populate fields
        etName.setText(name != null ? name : "");
        cbLab.setChecked(lab);
        etLectures.setText(lectures != null ? lectures : "");
        etSemester.setText(semester != null ? semester : "");
        etLabs.setText(labs != null ? labs : "");

        // Setup teacher spinner
        List<String> teacherNames = new ArrayList<>();
        int selectedIndex = 0;
        for (int i = 0; i < teachersList.size(); i++) {
            teacherNames.add(teachersList.get(i).get("name"));
            if (teachersList.get(i).get("objectId").equals(teacherId)) {
                selectedIndex = i;
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, teacherNames);
        spinnerTeacher.setAdapter(adapter);
        if (!teacherNames.isEmpty()) spinnerTeacher.setSelection(selectedIndex);

        // Delete button handling
        btnDelete.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        btnDelete.setOnClickListener(v -> {
            layoutSubjectsContainer.removeView(card);
            subjectViews.remove(card);
            if (objectId != null) {
                ParseObject.createWithoutData("Subject", objectId).deleteInBackground();
            }
        });

        card.setTag(objectId);
        layoutSubjectsContainer.addView(card);
        subjectViews.add(card);
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
            EditText etName = card.findViewById(R.id.etSubjectName);
            Spinner spinnerTeacher = card.findViewById(R.id.spinnerTeacher);
            CheckBox cbLab = card.findViewById(R.id.cbLab);
            EditText etLectures = card.findViewById(R.id.etLecturesWeekly);
            EditText etSemester = card.findViewById(R.id.etSemester);
            EditText etLabs = card.findViewById(R.id.etLabsWeekly);

            String objectId = (String) card.getTag();
            String name = etName.getText().toString().trim();
            String teacherId = teachersList.get(spinnerTeacher.getSelectedItemPosition()).get("objectId");
            boolean lab = cbLab.isChecked();
            String lectures = etLectures.getText().toString().trim();
            String semester = etSemester.getText().toString().trim();
            String labs = etLabs.getText().toString().trim();

            if (objectId != null) {
                // Update existing
                ParseObject subject = ParseObject.createWithoutData("Subject", objectId);
                subject.put("name", name);
                subject.put("teacher", ParseObject.createWithoutData("Teacher", teacherId));
                subject.put("lab", lab);
                subject.put("lecturesWeekly", lectures);
                subject.put("semester", semester);
                subject.put("labsWeekly", labs);
                subject.saveInBackground();
            } else {
                // Create new
                ParseObject subject = new ParseObject("Subject");
                subject.put("user", ParseUser.getCurrentUser());
                subject.put("name", name);
                subject.put("teacher", ParseObject.createWithoutData("Teacher", teacherId));
                subject.put("lab", lab);
                subject.put("lecturesWeekly", lectures);
                subject.put("semester", semester);
                subject.put("labsWeekly", labs);
                subject.saveInBackground();
            }
        }
        Toast.makeText(this, "Subjects saved!", Toast.LENGTH_SHORT).show();
    }
}
