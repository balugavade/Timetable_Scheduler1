package com.example.timetablescheduler;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.textfield.TextInputEditText;
import com.parse.*;
import java.util.*;

public class BatchActivity extends AppCompatActivity {

    private TextInputEditText etDepartment, etTotalBatches;
    private Button btnGenerateBatches, btnSave, btnNext;
    private LinearLayout layoutBatchesContainer;
    private TextView tvBatchesHeader;

    private List<View> batchCardViews = new ArrayList<>();
    private List<String> allSubjects = new ArrayList<>();
    private List<String> allTeachers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batch);

        etDepartment = findViewById(R.id.etDepartment);
        etTotalBatches = findViewById(R.id.etTotalBatches);
        btnGenerateBatches = findViewById(R.id.btnGenerateBatches);
        btnSave = findViewById(R.id.btnSave);
        btnNext = findViewById(R.id.btnNext);
        layoutBatchesContainer = findViewById(R.id.layoutBatchesContainer);
        tvBatchesHeader = findViewById(R.id.tvBatchesHeader);

        btnGenerateBatches.setOnClickListener(v -> generateBatchCards());
        btnSave.setOnClickListener(v -> saveBatchData());
        btnNext.setOnClickListener(v -> {
            saveBatchData();
            if (validateBatchData()) {
                Intent intent = new Intent(BatchActivity.this, TimetableGenerationActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Complete all batch configurations", Toast.LENGTH_SHORT).show();
            }
        });

        fetchSubjectsAndTeachers();
    }

    private void fetchSubjectsAndTeachers() {
        ParseQuery<ParseObject> subjectQuery = ParseQuery.getQuery("Subject");
        subjectQuery.whereEqualTo("user", ParseUser.getCurrentUser());
        subjectQuery.findInBackground((subjects, e1) -> {
            if (e1 == null) {
                allSubjects.clear();
                for (ParseObject subject : subjects) {
                    allSubjects.add(subject.getString("name"));
                }
            }
        });

        ParseQuery<ParseObject> teacherQuery = ParseQuery.getQuery("Teacher");
        teacherQuery.whereEqualTo("user", ParseUser.getCurrentUser());
        teacherQuery.findInBackground((teachers, e2) -> {
            if (e2 == null) {
                allTeachers.clear();
                for (ParseObject teacher : teachers) {
                    allTeachers.add(teacher.getString("name"));
                }
            }
        });
    }

    private void generateBatchCards() {
        String totalBatchesStr = etTotalBatches.getText().toString().trim();
        if (totalBatchesStr.isEmpty()) {
            Toast.makeText(this, "Enter number of batches", Toast.LENGTH_SHORT).show();
            return;
        }

        int totalBatches = Integer.parseInt(totalBatchesStr);
        layoutBatchesContainer.removeAllViews();
        batchCardViews.clear();
        tvBatchesHeader.setVisibility(View.VISIBLE);

        for (int i = 0; i < totalBatches; i++) {
            CardView batchCard = (CardView) LayoutInflater.from(this)
                    .inflate(R.layout.batch_item, layoutBatchesContainer, false);

            TextView tvBatchLabel = batchCard.findViewById(R.id.tvBatchLabel);
            tvBatchLabel.setText("Batch " + (i + 1));

            Button btnGenerateSubjects = batchCard.findViewById(R.id.btnGenerateSubjects);
            TextInputEditText etTotalSubjects = batchCard.findViewById(R.id.etTotalSubjects);
            LinearLayout layoutSubjectContainer = batchCard.findViewById(R.id.layoutSubjectContainer);

            btnGenerateSubjects.setOnClickListener(v -> {
                String numSubjects = etTotalSubjects.getText().toString().trim();
                if (!numSubjects.isEmpty()) {
                    generateSubjectFields(layoutSubjectContainer, Integer.parseInt(numSubjects));
                }
            });

            layoutBatchesContainer.addView(batchCard);
            batchCardViews.add(batchCard);
        }
    }

    private void generateSubjectFields(LinearLayout container, int numSubjects) {
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < numSubjects; i++) {
            View subjectRow = inflater.inflate(R.layout.subject_row_item, container, false);

            Spinner spinnerSubject = subjectRow.findViewById(R.id.spinnerSubject);
            ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, allSubjects);
            subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerSubject.setAdapter(subjectAdapter);

            Spinner spinnerTeacher = subjectRow.findViewById(R.id.spinnerTeacher);
            ArrayAdapter<String> teacherAdapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, allTeachers);
            teacherAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerTeacher.setAdapter(teacherAdapter);

            container.addView(subjectRow);
        }
    }

    private void saveBatchData() {
        if (batchCardViews.isEmpty()) {
            Toast.makeText(this, "No batches to save", Toast.LENGTH_SHORT).show();
            return;
        }

        List<ParseObject> batchObjects = new ArrayList<>();

        for (View batchCard : batchCardViews) {
            TextInputEditText etBatchName = batchCard.findViewById(R.id.etBatchName);
            CheckBox cbSectionA = batchCard.findViewById(R.id.cbSectionA);
            CheckBox cbSectionB = batchCard.findViewById(R.id.cbSectionB);
            TextInputEditText etAcademicYear = batchCard.findViewById(R.id.etAcademicYear);
            LinearLayout layoutSubjectContainer = batchCard.findViewById(R.id.layoutSubjectContainer);

            String batchName = etBatchName.getText().toString().trim();
            boolean sectionA = cbSectionA.isChecked();
            boolean sectionB = cbSectionB.isChecked();
            String academicYear = etAcademicYear.getText().toString().trim();

            if (batchName.isEmpty() || academicYear.isEmpty()) {
                Toast.makeText(this, "Batch name and year are required", Toast.LENGTH_SHORT).show();
                return;
            }

            List<String> subjects = new ArrayList<>();
            List<String> teachers = new ArrayList<>();

            for (int i = 0; i < layoutSubjectContainer.getChildCount(); i++) {
                View subjectRow = layoutSubjectContainer.getChildAt(i);
                Spinner spinnerSubject = subjectRow.findViewById(R.id.spinnerSubject);
                Spinner spinnerTeacher = subjectRow.findViewById(R.id.spinnerTeacher);

                subjects.add(spinnerSubject.getSelectedItem().toString());
                teachers.add(spinnerTeacher.getSelectedItem().toString());
            }

            ParseObject batch = new ParseObject("Batch");
            batch.put("user", ParseUser.getCurrentUser());
            batch.put("name", batchName);
            batch.put("department", etDepartment.getText().toString());
            batch.put("academicYear", academicYear);
            batch.put("sectionA", sectionA);
            batch.put("sectionB", sectionB);
            batch.put("subjects", subjects);
            batchObjects.add(batch);
        }

        ParseObject.saveAllInBackground(batchObjects, e -> {
            if (e == null) {
                Toast.makeText(this, "Batches saved!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Save error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean validateBatchData() {
        if (batchCardViews.isEmpty()) return false;

        for (View batchCard : batchCardViews) {
            LinearLayout layoutSubjectContainer = batchCard.findViewById(R.id.layoutSubjectContainer);
            if (layoutSubjectContainer.getChildCount() == 0) return false;
        }
        return true;
    }
}
