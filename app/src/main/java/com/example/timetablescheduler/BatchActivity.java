package com.example.timetablescheduler;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BatchActivity extends AppCompatActivity {

    private RadioGroup rgCourseLevel;
    private EditText etDepartment, etTotalBatches;
    private Button btnGenerateBatches, btnSave, btnNext;
    private LinearLayout layoutBatchesContainer;
    private TextView tvBatchesHeader;
    private List<View> batchViews = new ArrayList<>();
    private List<HashMap<String, String>> subjectsList = new ArrayList<>();
    private List<HashMap<String, String>> teachersList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batch);

        rgCourseLevel = findViewById(R.id.rgCourseLevel);
        etDepartment = findViewById(R.id.etDepartment);
        etTotalBatches = findViewById(R.id.etTotalBatches);
        btnGenerateBatches = findViewById(R.id.btnGenerateBatches);
        layoutBatchesContainer = findViewById(R.id.layoutBatchesContainer);
        tvBatchesHeader = findViewById(R.id.tvBatchesHeader);
        btnSave = findViewById(R.id.btnSave);
        btnNext = findViewById(R.id.btnNext);

        btnGenerateBatches.setOnClickListener(v -> generateBatchFields());
        btnSave.setOnClickListener(v -> saveBatchesToBack4App());
        btnNext.setOnClickListener(v -> {
            Intent intent = new Intent(BatchActivity.this, Timetable.class);
            startActivity(intent);
            finish();
        });

        fetchSubjectsAndTeachers();
    }

    private void fetchSubjectsAndTeachers() {
        // Fetch subjects
        ParseQuery<ParseObject> subjectQuery = ParseQuery.getQuery("Subject");
        subjectQuery.whereEqualTo("user", ParseUser.getCurrentUser());
        subjectQuery.findInBackground((subjects, e) -> {
            subjectsList.clear();
            if (e == null) {
                for (ParseObject subject : subjects) {
                    HashMap<String, String> map = new HashMap<>();
                    map.put("objectId", subject.getObjectId());
                    map.put("name", subject.getString("name"));
                    subjectsList.add(map);
                }
            }
        });

        // Fetch teachers
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
        });
    }

    private void generateBatchFields() {
        String totalBatchesStr = etTotalBatches.getText().toString().trim();
        if (totalBatchesStr.isEmpty()) {
            Toast.makeText(this, "Enter total number of batches", Toast.LENGTH_SHORT).show();
            return;
        }

        int totalBatches = Integer.parseInt(totalBatchesStr);
        layoutBatchesContainer.removeAllViews();
        batchViews.clear();
        tvBatchesHeader.setVisibility(View.VISIBLE);

        for (int i = 0; i < totalBatches; i++) {
            addBatchField(i + 1);
        }
    }

    private void addBatchField(int batchNumber) {
        CardView batchCard = (CardView) LayoutInflater.from(this)
                .inflate(R.layout.batch_item, layoutBatchesContainer, false);

        TextView tvBatchNumber = batchCard.findViewById(R.id.tvBatchNumber);
        tvBatchNumber.setText("Batch " + batchNumber);

        Button btnGenerateSubjects = batchCard.findViewById(R.id.btnGenerateSubjects);
        EditText etTotalSubjects = batchCard.findViewById(R.id.etTotalSubjects);
        LinearLayout layoutSubjectsContainer = batchCard.findViewById(R.id.layoutSubjectsContainer);

        btnGenerateSubjects.setOnClickListener(v -> {
            String totalSubjectsStr = etTotalSubjects.getText().toString().trim();
            if (totalSubjectsStr.isEmpty()) {
                Toast.makeText(this, "Enter total subjects for batch " + batchNumber, Toast.LENGTH_SHORT).show();
                return;
            }
            int totalSubjects = Integer.parseInt(totalSubjectsStr);
            generateSubjectFields(layoutSubjectsContainer, totalSubjects);
        });

        layoutBatchesContainer.addView(batchCard);
        batchViews.add(batchCard);
    }

    private void generateSubjectFields(LinearLayout container, int totalSubjects) {
        container.removeAllViews();

        for (int i = 0; i < totalSubjects; i++) {
            LinearLayout subjectRow = new LinearLayout(this);
            subjectRow.setOrientation(LinearLayout.HORIZONTAL);
            subjectRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            subjectRow.setPadding(0, 8, 0, 8);

            // Subject Spinner
            Spinner spinnerSubject = new Spinner(this);
            spinnerSubject.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            List<String> subjectNames = new ArrayList<>();
            for (HashMap<String, String> subject : subjectsList) {
                subjectNames.add(subject.get("name"));
            }
            ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item, subjectNames);
            spinnerSubject.setAdapter(subjectAdapter);

            // Teacher Spinner
            Spinner spinnerTeacher = new Spinner(this);
            spinnerTeacher.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            List<String> teacherNames = new ArrayList<>();
            for (HashMap<String, String> teacher : teachersList) {
                teacherNames.add(teacher.get("name"));
            }
            ArrayAdapter<String> teacherAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item, teacherNames);
            spinnerTeacher.setAdapter(teacherAdapter);

            subjectRow.addView(spinnerSubject);
            subjectRow.addView(spinnerTeacher);
            container.addView(subjectRow);
        }
    }

    private void saveBatchesToBack4App() {
        String courseLevel = "";
        int selectedId = rgCourseLevel.getCheckedRadioButtonId();
        if (selectedId == R.id.rbUG) {
            courseLevel = "UG";
        } else if (selectedId == R.id.rbPG) {
            courseLevel = "PG";
        }

        String department = etDepartment.getText().toString().trim();

        if (courseLevel.isEmpty() || department.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        for (View batchView : batchViews) {
            EditText etBatchName = batchView.findViewById(R.id.etBatchName);
            CheckBox cbSectionA = batchView.findViewById(R.id.cbSectionA);
            CheckBox cbSectionB = batchView.findViewById(R.id.cbSectionB);
            EditText etAcademicYear = batchView.findViewById(R.id.etAcademicYear);

            String batchName = etBatchName.getText().toString().trim();
            String academicYear = etAcademicYear.getText().toString().trim();
            List<String> sections = new ArrayList<>();
            if (cbSectionA.isChecked()) sections.add("A");
            if (cbSectionB.isChecked()) sections.add("B");

            if (!batchName.isEmpty() && !academicYear.isEmpty()) {
                ParseObject batch = new ParseObject("Batch");
                batch.put("user", ParseUser.getCurrentUser());
                batch.put("courseLevel", courseLevel);
                batch.put("department", department);
                batch.put("batchName", batchName);
                batch.put("sections", sections);
                batch.put("academicYear", academicYear);
                batch.saveInBackground();
            }
        }

        Toast.makeText(this, "Batches saved successfully!", Toast.LENGTH_SHORT).show();
    }
}
