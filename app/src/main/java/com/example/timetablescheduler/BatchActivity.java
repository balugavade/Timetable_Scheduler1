package com.example.timetablescheduler;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;
import com.example.timetablescheduler.viewmodel.BatchViewModel;
import com.google.android.material.textfield.TextInputEditText;
import com.parse.*;

import java.util.*;

public class BatchActivity extends AppCompatActivity {

    private TextInputEditText etDepartment, etTotalBatches;
    private Button btnGenerateBatches, btnNext, btnGenerateTimetable;
    private LinearLayout layoutBatchesContainer;
    private TextView tvBatchesHeader;

    private List<View> batchCardViews = new ArrayList<>();
    private List<String> allSubjects = new ArrayList<>();
    private List<String> allTeachers = new ArrayList<>();
    private BatchViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batch);

        viewModel = new ViewModelProvider(this).get(BatchViewModel.class);

        etDepartment = findViewById(R.id.etDepartment);
        etTotalBatches = findViewById(R.id.etTotalBatches);
        btnGenerateBatches = findViewById(R.id.btnGenerateBatches);
        btnNext = findViewById(R.id.btnNext);
        btnGenerateTimetable = findViewById(R.id.btnGenerateTimetable);
        layoutBatchesContainer = findViewById(R.id.layoutBatchesContainer);
        tvBatchesHeader = findViewById(R.id.tvBatchesHeader);

        restoreViewModel();
        fetchSubjectsAndTeachers();

        btnGenerateBatches.setOnClickListener(v -> {
            saveToViewModel();
            generateBatchCards();
        });

        btnNext.setOnClickListener(v -> {
            Intent intent = new Intent(BatchActivity.this, TimetableDisplayActivity.class);
            startActivity(intent);
        });

        btnGenerateTimetable.setOnClickListener(v -> {
            saveToViewModel();
            if (validateBatchData()) {
                saveBatchData(() -> {
                    Intent intent = new Intent(BatchActivity.this, TimetableGenerationActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                });
            } else {
                Toast.makeText(this, "Please fill all batch and subject-teacher info", Toast.LENGTH_SHORT).show();
            }
        });
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
            ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, allSubjects);
            subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerSubject.setAdapter(subjectAdapter);

            Spinner spinnerTeacher = subjectRow.findViewById(R.id.spinnerTeacher);
            ArrayAdapter<String> teacherAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, allTeachers);
            teacherAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerTeacher.setAdapter(teacherAdapter);

            container.addView(subjectRow);
        }
    }

    private void saveToViewModel() {
        if (viewModel == null) return;
        viewModel.department = etDepartment.getText().toString();
        viewModel.totalBatches = etTotalBatches.getText().toString();
        viewModel.batchDataList.clear();

        for (View batchCard : batchCardViews) {
            TextInputEditText etBatchName = batchCard.findViewById(R.id.etBatchName);
            RadioGroup rgSection = batchCard.findViewById(R.id.rgSection);
            TextInputEditText etAcademicYear = batchCard.findViewById(R.id.etAcademicYear);
            TextInputEditText etTotalSubjects = batchCard.findViewById(R.id.etTotalSubjects);
            LinearLayout layoutSubjectContainer = batchCard.findViewById(R.id.layoutSubjectContainer);

            BatchViewModel.BatchData batchData = new BatchViewModel.BatchData();
            batchData.batchName = etBatchName.getText().toString();

            int checkedId = rgSection.getCheckedRadioButtonId();
            if (checkedId == R.id.rbSectionA) {
                batchData.section = "A";
            } else if (checkedId == R.id.rbSectionB) {
                batchData.section = "B";
            } else {
                batchData.section = "";
            }

            batchData.academicYear = etAcademicYear.getText().toString();
            batchData.totalSubjects = etTotalSubjects.getText().toString();

            for (int i = 0; i < layoutSubjectContainer.getChildCount(); i++) {
                View subjectRow = layoutSubjectContainer.getChildAt(i);
                Spinner spinnerSubject = subjectRow.findViewById(R.id.spinnerSubject);
                Spinner spinnerTeacher = subjectRow.findViewById(R.id.spinnerTeacher);

                BatchViewModel.SubjectTeacher st = new BatchViewModel.SubjectTeacher();
                st.subject = spinnerSubject.getSelectedItem().toString();
                st.teacher = spinnerTeacher.getSelectedItem().toString();
                batchData.subjectTeachers.add(st);
            }
            viewModel.batchDataList.add(batchData);
        }
    }

    private void saveBatchData(Runnable onSuccess) {
        if (batchCardViews.isEmpty()) {
            Toast.makeText(this, "Please generate and fill at least one batch.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<ParseObject> batchObjects = new ArrayList<>();
        List<ParseObject> subjectTeacherMappings = new ArrayList<>();

        for (View batchCard : batchCardViews) {
            TextInputEditText etBatchName = batchCard.findViewById(R.id.etBatchName);
            RadioGroup rgSection = batchCard.findViewById(R.id.rgSection);
            TextInputEditText etAcademicYear = batchCard.findViewById(R.id.etAcademicYear);
            LinearLayout layoutSubjectContainer = batchCard.findViewById(R.id.layoutSubjectContainer);

            String batchName = etBatchName.getText().toString().trim();
            String academicYear = etAcademicYear.getText().toString().trim();

            int checkedId = rgSection.getCheckedRadioButtonId();
            String section = "";
            if (checkedId == R.id.rbSectionA) {
                section = "A";
            } else if (checkedId == R.id.rbSectionB) {
                section = "B";
            }

            if (batchName.isEmpty() || academicYear.isEmpty() || section.isEmpty()) {
                Toast.makeText(this, "Batch name, section, and year are required", Toast.LENGTH_SHORT).show();
                return;
            }

            List<String> subjects = new ArrayList<>();

            for (int i = 0; i < layoutSubjectContainer.getChildCount(); i++) {
                View subjectRow = layoutSubjectContainer.getChildAt(i);
                Spinner spinnerSubject = subjectRow.findViewById(R.id.spinnerSubject);
                Spinner spinnerTeacher = subjectRow.findViewById(R.id.spinnerTeacher);

                String subject = spinnerSubject.getSelectedItem().toString();
                String teacher = spinnerTeacher.getSelectedItem().toString();

                subjects.add(subject);

                // Save subject-teacher mapping to BatchSubjectTeacher class
                ParseObject mapping = new ParseObject("BatchSubjectTeacher");
                mapping.put("user", ParseUser.getCurrentUser());
                mapping.put("batchName", batchName);
                mapping.put("section", section);
                mapping.put("academicYear", academicYear);
                mapping.put("subject", subject);
                mapping.put("teacher", teacher);
                subjectTeacherMappings.add(mapping);
            }

            ParseObject batch = new ParseObject("Batch");
            batch.put("user", ParseUser.getCurrentUser());
            batch.put("name", batchName);
            batch.put("section", section);
            batch.put("academicYear", academicYear);
            batch.put("department", etDepartment.getText().toString());
            batch.put("subjects", subjects);
            batchObjects.add(batch);
        }

        ParseObject.saveAllInBackground(batchObjects, batchError -> {
            if (batchError == null) {
                ParseObject.saveAllInBackground(subjectTeacherMappings, mappingError -> {
                    if (mappingError == null) {
                        Toast.makeText(this, "Batch and mappings saved!", Toast.LENGTH_SHORT).show();
                        if (onSuccess != null) onSuccess.run();
                    } else {
                        Toast.makeText(this, "Mapping save error: " + mappingError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, "Batch save error: " + batchError.getMessage(), Toast.LENGTH_SHORT).show();
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

    @Override
    protected void onPause() {
        super.onPause();
        saveToViewModel();
    }

    private void restoreViewModel() {
        // Optional: implement as needed
    }
}
