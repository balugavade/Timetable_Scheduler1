package com.example.timetablescheduler;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.parse.ParseObject;
import com.parse.ParseUser;
import java.util.ArrayList;
import java.util.List;

public class TeachersActivity extends AppCompatActivity {

    private Button btnAddTeacher, btnEditTeachers, btnNext;
    private LinearLayout layoutTeachersContainer;
    private List<View> teacherViews = new ArrayList<>();
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teachers);

        btnAddTeacher = findViewById(R.id.btnAddTeacher);
        btnEditTeachers = findViewById(R.id.btnEditTeachers);
        btnNext = findViewById(R.id.btnNext);
        layoutTeachersContainer = findViewById(R.id.layoutTeachersContainer);

        btnAddTeacher.setOnClickListener(v -> addTeacherField(null, null));
        btnEditTeachers.setOnClickListener(v -> toggleEditMode());
        btnNext.setOnClickListener(v -> saveTeachersToBack4App());
    }

    private void addTeacherField(String name, String subject) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setPadding(0, 8, 0, 8);

        EditText etName = new EditText(this);
        etName.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        etName.setHint("Teacher Name");
        etName.setInputType(InputType.TYPE_CLASS_TEXT);
        if (name != null) etName.setText(name);

        EditText etSubject = new EditText(this);
        etSubject.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        etSubject.setHint("Subject");
        etSubject.setInputType(InputType.TYPE_CLASS_TEXT);
        if (subject != null) etSubject.setText(subject);

        ImageButton btnDelete = new ImageButton(this);
        btnDelete.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        btnDelete.setImageResource(android.R.drawable.ic_delete);
        btnDelete.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        btnDelete.setOnClickListener(v -> {
            layoutTeachersContainer.removeView(row);
            teacherViews.remove(row);
        });

        row.addView(etName);
        row.addView(etSubject);
        row.addView(btnDelete);

        layoutTeachersContainer.addView(row);
        teacherViews.add(row);
    }

    private void toggleEditMode() {
        isEditMode = !isEditMode;
        for (View view : teacherViews) {
            ImageButton btnDelete = (ImageButton) ((LinearLayout) view).getChildAt(2);
            btnDelete.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        }
        btnEditTeachers.setText(isEditMode ? "Done Editing" : "Edit Teachers");
    }

    private void saveTeachersToBack4App() {
        List<String> teacherNames = new ArrayList<>();
        List<String> teacherSubjects = new ArrayList<>();

        for (View view : teacherViews) {
            EditText etName = (EditText) ((LinearLayout) view).getChildAt(0);
            EditText etSubject = (EditText) ((LinearLayout) view).getChildAt(1);

            String name = etName.getText().toString().trim();
            String subject = etSubject.getText().toString().trim();

            if (!name.isEmpty() && !subject.isEmpty()) {
                teacherNames.add(name);
                teacherSubjects.add(subject);
            }
        }

        if (teacherNames.isEmpty()) {
            Toast.makeText(this, "Please add at least one teacher.", Toast.LENGTH_SHORT).show();
            return;
        }

        ParseObject teachers = new ParseObject("Teachers");
        teachers.put("user", ParseUser.getCurrentUser());
        teachers.put("names", teacherNames);
        teachers.put("subjects", teacherSubjects);
        teachers.saveInBackground(e -> {
            if (e == null) {
                Toast.makeText(this, "Teachers saved!", Toast.LENGTH_SHORT).show();
                navigateToNextActivity();
            } else {
                Toast.makeText(this, "Save error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToNextActivity() {
        Intent intent = new Intent(TeachersActivity.this, BatchActivity.class);
        startActivity(intent);
        finish();
    }
}
