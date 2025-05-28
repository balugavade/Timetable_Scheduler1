package com.example.timetablescheduler;

import static kotlin.text.Typography.section;

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
    private List<HashMap<String, String>> teachersList = new ArrayList<>(); // Each: {objectId, name}

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

        findViewById(R.id.fabAddSubject).setOnClickListener(v -> addSubjectField(null, null, false, null, null, null, null));

        fetchTeachersAndSubjects();
    }

    private void fetchTeachersAndSubjects() {
        // Fetch teachers for spinner
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Teacher");
        query.whereEqualTo("user", ParseUser.getCurrentUser());
        query.findInBackground((objects, e) -> {
            teachersList.clear();
            if (e == null && objects != null) {
                for (ParseObject obj : objects) {
                    HashMap<String, String> map = new HashMap<>();
                    map.put("objectId", obj.getObjectId());
                    map.put("name", obj.getString("name"));
                    teachersList.add(map);
                }
            }
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
                if (objects.isEmpty()) {
                    addSubjectField(null, null, false, null, null, null, null);
                } else {
                    for (ParseObject obj : objects) {
                        ParseObject teacherObj = obj.getParseObject("teacher");
                        String teacherId = teacherObj != null ? teacherObj.getObjectId() : null;
                        addSubjectField(
                                obj.getString("name"),
                                teacherId,
                                obj.getBoolean("lab"),
                                obj.getString("hours"),
                                obj.getString("semester"),
                                obj.getString("section"),
                                obj.getObjectId()
                        );
                    }
                }
            } else {
                Toast.makeText(this, "Error fetching subjects: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addSubjectField(String name, String teacherId, boolean lab, String hours, String semester, String section, String objectId) {
        CardView card = (CardView) LayoutInflater.from(this)
                .inflate(R.layout.subject_item, layoutSubjectsContainer, false);

        EditText etName = card.findViewById(R.id.etSubjectName);
        Spinner spinnerTeacher = card.findViewById(R.id.spinnerTeacher);
        CheckBox cbLab = card.findViewById(R.id.cbLab);
        EditText etHours = card.findViewById(R.id.etHours);
        EditText etSemester = card.findViewById(R.id.etSemester);
        //EditText etSection = card.findViewById(R.id.etSection);
        ImageButton btnDelete = card.findViewById(R.id.btnDeleteSubject);

        etName.setText(name != null ? name : "");
        cbLab.setChecked(lab);
        etHours.setText(hours != null ? hours : "");
        etSemester.setText(semester != null ? semester : "");
        //etSection.setText(section != null ? section : "");

        // Setup spinner
        List<String> teacherNames = new ArrayList<>();
        int selectedIdx = 0;
        for (int i = 0; i < teachersList.size(); i++) {
            teacherNames.add(teachersList.get(i).get("name"));
            if (teacherId != null && teacherId.equals(teachersList.get(i).get("objectId"))) {
                selectedIdx = i;
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, teacherNames);
        spinnerTeacher.setAdapter(adapter);
        if (!teacherNames.isEmpty()) spinnerTeacher.setSelection(selectedIdx);

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
            EditText etHours = card.findViewById(R.id.etHours);
            EditText etSemester = card.findViewById(R.id.etSemester);
            //EditText etSection = card.findViewById(R.id.etSection);

            String name = etName.getText().toString().trim();
            int selectedTeacher = spinnerTeacher.getSelectedItemPosition();
            String teacherId = selectedTeacher >= 0 && selectedTeacher < teachersList.size()
                    ? teachersList.get(selectedTeacher).get("objectId") : null;
            boolean lab = cbLab.isChecked();
            String hours = etHours.getText().toString().trim();
            String semester = etSemester.getText().toString().trim();
            //String section = etSection.getText().toString().trim();
            String objectId = (String) card.getTag();

            if (!name.isEmpty() && teacherId != null && !hours.isEmpty()) {
                if (objectId != null) {
                    // Update existing subject
                    ParseQuery<ParseObject> query = ParseQuery.getQuery("Subject");
                    query.getInBackground(objectId, (obj, e) -> {
                        if (e == null && obj != null) {
                            obj.put("name", name);
                            obj.put("teacher", ParseObject.createWithoutData("Teacher", teacherId));
                            obj.put("lab", lab);
                            obj.put("hours", hours);
                            obj.put("semester", semester);
                            //obj.put("section", section);
                            obj.saveInBackground();
                        }
                    });
                } else {
                    // Create new subject
                    ParseObject subject = new ParseObject("Subject");
                    subject.put("user", ParseUser.getCurrentUser());
                    subject.put("name", name);
                    subject.put("teacher", ParseObject.createWithoutData("Teacher", teacherId));
                    subject.put("lab", lab);
                    subject.put("hours", hours);
                    subject.put("semester", semester);
                   // subject.put("section", section);
                    subject.saveInBackground();
                }
            }
        }
        Toast.makeText(this, "Subjects saved!", Toast.LENGTH_SHORT).show();
    }
}
