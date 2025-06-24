package com.example.timetablescheduler;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.textfield.TextInputEditText;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import java.util.ArrayList;
import java.util.List;

public class BatchActivity extends AppCompatActivity {

    private TextInputEditText etDepartment, etTotalBatches;
    private Button btnGenerateBatches, btnSave, btnNext;
    private LinearLayout layoutBatchesContainer;
    private TextView tvBatchesHeader;

    private List<View> batchContainers = new ArrayList<>();

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

        btnGenerateBatches.setOnClickListener(v -> generateBatchFields());
        btnSave.setOnClickListener(v -> saveBatchConfiguration());
        btnNext.setOnClickListener(v -> navigateToTimetableGeneration());
    }

    private void generateBatchFields() {
        String totalBatchesText = etTotalBatches.getText().toString().trim();
        if (totalBatchesText.isEmpty()) {
            showToast("Please enter total number of batches");
            return;
        }

        int totalBatches = Integer.parseInt(totalBatchesText);
        layoutBatchesContainer.removeAllViews();
        batchContainers.clear();
        tvBatchesHeader.setVisibility(View.VISIBLE);

        for (int i = 1; i <= totalBatches; i++) {
            View batchContainer = createBatchContainer(i);
            layoutBatchesContainer.addView(batchContainer);
            batchContainers.add(batchContainer);
        }
    }

    private View createBatchContainer(int batchNumber) {
        CardView cardView = new CardView(this);
        cardView.setCardElevation(8f);
        cardView.setRadius(18f);
        cardView.setUseCompatPadding(true);
        cardView.setCardBackgroundColor(getResources().getColor(android.R.color.white));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 16, 0, 16);
        cardView.setLayoutParams(cardParams);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(24, 24, 24, 24);

        // Batch Title
        TextView tvBatchTitle = new TextView(this);
        tvBatchTitle.setText("Batch " + batchNumber);
        tvBatchTitle.setTextSize(20f);
        tvBatchTitle.setTextColor(getResources().getColor(R.color.purple_500));
        tvBatchTitle.setPadding(0, 0, 0, 16);
        container.addView(tvBatchTitle);

        // Batch Name Input
        TextInputEditText etBatchName = new TextInputEditText(this);
        etBatchName.setHint("Batch Name");
        etBatchName.setId(View.generateViewId());
        container.addView(etBatchName);

        // Sections Label
        TextView tvSectionsLabel = new TextView(this);
        tvSectionsLabel.setText("Sections");
        tvSectionsLabel.setPadding(0, 24, 0, 8);
        tvSectionsLabel.setTextSize(16f);
        container.addView(tvSectionsLabel);

        // Sections Checkboxes
        LinearLayout sectionsLayout = new LinearLayout(this);
        sectionsLayout.setOrientation(LinearLayout.HORIZONTAL);

        CheckBox cbSectionA = new CheckBox(this);
        cbSectionA.setText("Section A");
        cbSectionA.setId(View.generateViewId());
        sectionsLayout.addView(cbSectionA);

        CheckBox cbSectionB = new CheckBox(this);
        cbSectionB.setText("Section B");
        cbSectionB.setId(View.generateViewId());
        sectionsLayout.addView(cbSectionB);

        container.addView(sectionsLayout);

        // Academic Year Input
        TextInputEditText etAcademicYear = new TextInputEditText(this);
        etAcademicYear.setHint("Academic Year (e.g., 2024-25)");
        etAcademicYear.setId(View.generateViewId());
        etAcademicYear.setPadding(0, 24, 0, 0);
        container.addView(etAcademicYear);

        // Total Subjects Input
        TextInputEditText etTotalSubjects = new TextInputEditText(this);
        etTotalSubjects.setHint("Total Number of Subjects");
        etTotalSubjects.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etTotalSubjects.setId(View.generateViewId());
        etTotalSubjects.setPadding(0, 24, 0, 0);
        container.addView(etTotalSubjects);

        // Generate Subject Fields Button
        Button btnGenerateSubjects = new Button(this);
        btnGenerateSubjects.setText("Generate Subject Fields");
        btnGenerateSubjects.setId(View.generateViewId());
        btnGenerateSubjects.setPadding(0, 16, 0, 0);
        container.addView(btnGenerateSubjects);

        // Subject Container
        LinearLayout subjectContainer = new LinearLayout(this);
        subjectContainer.setOrientation(LinearLayout.VERTICAL);
        subjectContainer.setId(View.generateViewId());
        container.addView(subjectContainer);

        btnGenerateSubjects.setOnClickListener(v -> {
            String subjectsText = etTotalSubjects.getText().toString().trim();
            if (!subjectsText.isEmpty()) {
                generateSubjectFields(subjectContainer, Integer.parseInt(subjectsText));
            }
        });

        cardView.addView(container);

        return cardView;
    }

    private void generateSubjectFields(LinearLayout subjectContainer, int numSubjects) {
        subjectContainer.removeAllViews();

        for (int i = 1; i <= numSubjects; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 8, 0, 8);

            Spinner spinnerSubject = new Spinner(this);
            spinnerSubject.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            Spinner spinnerTeacher = new Spinner(this);
            spinnerTeacher.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            // TODO: Populate spinners with actual data (subjects and teachers)

            row.addView(spinnerSubject);
            row.addView(spinnerTeacher);

            subjectContainer.addView(row);
        }
    }

    private void saveBatchConfiguration() {
        String department = etDepartment.getText().toString().trim();
        if (department.isEmpty()) {
            showToast("Please enter department");
            return;
        }
        if (batchContainers.isEmpty()) {
            showToast("Please generate batch fields");
            return;
        }

        List<ParseObject> batches = new ArrayList<>();

        for (View batchView : batchContainers) {
            LinearLayout container = (LinearLayout) ((CardView) batchView).getChildAt(0);

            TextInputEditText etBatchName = (TextInputEditText) container.getChildAt(1);
            String batchName = etBatchName.getText().toString().trim();
            if (batchName.isEmpty()) {
                showToast("Please enter batch name");
                return;
            }

            LinearLayout sectionsLayout = (LinearLayout) container.getChildAt(2);
            CheckBox cbSectionA = (CheckBox) sectionsLayout.getChildAt(0);
            CheckBox cbSectionB = (CheckBox) sectionsLayout.getChildAt(1);

            List<String> sections = new ArrayList<>();
            if (cbSectionA.isChecked()) sections.add("A");
            if (cbSectionB.isChecked()) sections.add("B");
            if (sections.isEmpty()) {
                showToast("Please select at least one section");
                return;
            }

            TextInputEditText etAcademicYear = (TextInputEditText) container.getChildAt(3);
            String academicYear = etAcademicYear.getText().toString().trim();

            ParseObject batch = new ParseObject("Batch");
            batch.put("user", ParseUser.getCurrentUser());
            batch.put("department", department);
            batch.put("batchName", batchName);
            batch.put("sections", sections);
            batch.put("academicYear", academicYear);

            // TODO: Save subjects and teachers assigned to this batch if needed

            batches.add(batch);
        }

        ParseObject.saveAllInBackground(batches, e -> {
            if (e == null) {
                showToast("Batch configuration saved successfully!");
            } else {
                showToast("Error saving batches: " + e.getMessage());
            }
        });
    }

    private void navigateToTimetableGeneration() {
        saveBatchConfiguration();
        Intent intent = new Intent(this, TimetableGenerationActivity.class);
        startActivity(intent);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
