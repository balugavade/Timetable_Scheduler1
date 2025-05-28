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
import java.util.List;

public class TeachersActivity extends AppCompatActivity {

    private LinearLayout layoutTeachersContainer;
    private Button btnEditTeachers, btnSaveTeachers;
    private boolean isEditMode = false;
    private List<View> teacherViews = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teachers);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        layoutTeachersContainer = findViewById(R.id.layoutTeachersContainer);
        btnEditTeachers = findViewById(R.id.btnEditTeachers);
        btnSaveTeachers = findViewById(R.id.btnSaveTeachers);

        btnEditTeachers.setOnClickListener(v -> toggleEditMode());
        btnSaveTeachers.setOnClickListener(v -> saveTeachersToBack4App());

        findViewById(R.id.fabAddTeacher).setOnClickListener(v -> addTeacherField(null, null, null, null, null, null));

        fetchTeachersFromBack4App();
    }

    private void fetchTeachersFromBack4App() {
        layoutTeachersContainer.removeAllViews();
        teacherViews.clear();

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Teacher");
        query.whereEqualTo("user", ParseUser.getCurrentUser());
        query.findInBackground((objects, e) -> {
            if (e == null) {
                if (objects.isEmpty()) {
                    addTeacherField(null, null, null, null, null, null);
                } else {
                    for (ParseObject obj : objects) {
                        addTeacherField(
                                obj.getString("name"),
                                obj.getString("position"),
                                obj.getString("load"),
                                obj.getString("subjects"),
                                obj.getString("department"),
                                obj.getObjectId()
                        );
                    }
                }
            } else {
                Toast.makeText(this, "Error fetching teachers: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addTeacherField(String name, String position, String load, String subjects, String department, String objectId) {
        CardView card = (CardView) LayoutInflater.from(this)
                .inflate(R.layout.teacher_item, layoutTeachersContainer, false);

        EditText etName = card.findViewById(R.id.etTeacherName);
        EditText etPosition = card.findViewById(R.id.etTeacherPosition);
        EditText etLoad = card.findViewById(R.id.etTeacherLoad);
        EditText etSubjects = card.findViewById(R.id.etTeacherSubjects);
        EditText etDepartment = card.findViewById(R.id.etTeacherDepartment);
        ImageButton btnDelete = card.findViewById(R.id.btnDeleteTeacher);

        etName.setText(name != null ? name : "");
        etPosition.setText(position != null ? position : "");
        etLoad.setText(load != null ? load : "");
        etSubjects.setText(subjects != null ? subjects : "");
        etDepartment.setText(department != null ? department : "");

        btnDelete.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        btnDelete.setOnClickListener(v -> {
            layoutTeachersContainer.removeView(card);
            teacherViews.remove(card);
            if (objectId != null) {
                ParseObject obj = ParseObject.createWithoutData("Teacher", objectId);
                obj.deleteInBackground();
            }
        });

        card.setTag(objectId);
        layoutTeachersContainer.addView(card);
        teacherViews.add(card);
    }

    private void toggleEditMode() {
        isEditMode = !isEditMode;
        for (View card : teacherViews) {
            ImageButton btnDelete = card.findViewById(R.id.btnDeleteTeacher);
            btnDelete.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        }
        btnEditTeachers.setText(isEditMode ? "Done Editing" : "Edit Teachers");
    }

    private void saveTeachersToBack4App() {
        for (View card : teacherViews) {
            EditText etName = card.findViewById(R.id.etTeacherName);
            EditText etPosition = card.findViewById(R.id.etTeacherPosition);
            EditText etLoad = card.findViewById(R.id.etTeacherLoad);
            EditText etSubjects = card.findViewById(R.id.etTeacherSubjects);
            EditText etDepartment = card.findViewById(R.id.etTeacherDepartment);

            String name = etName.getText().toString().trim();
            String position = etPosition.getText().toString().trim();
            String load = etLoad.getText().toString().trim();
            String subjects = etSubjects.getText().toString().trim();
            String department = etDepartment.getText().toString().trim();
            String objectId = (String) card.getTag();

            if (!name.isEmpty()) {
                if (objectId != null) {
                    // Update existing teacher
                    ParseQuery<ParseObject> query = ParseQuery.getQuery("Teacher");
                    query.getInBackground(objectId, (obj, e) -> {
                        if (e == null && obj != null) {
                            obj.put("name", name);
                            obj.put("position", position);
                            obj.put("load", load);
                            obj.put("subjects", subjects);
                            obj.put("department", department);
                            obj.saveInBackground();
                        }
                    });
                } else {
                    // Create new teacher
                    ParseObject teacher = new ParseObject("Teacher");
                    teacher.put("user", ParseUser.getCurrentUser());
                    teacher.put("name", name);
                    teacher.put("position", position);
                    teacher.put("load", load);
                    teacher.put("subjects", subjects);
                    teacher.put("department", department);
                    teacher.saveInBackground();
                }
            }
        }
        Toast.makeText(this, "Teachers saved!", Toast.LENGTH_SHORT).show();
    }
}
