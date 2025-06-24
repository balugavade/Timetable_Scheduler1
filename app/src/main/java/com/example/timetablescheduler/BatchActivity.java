package com.example.timetablescheduler;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
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

    private List<View> batchCards = new ArrayList<>();

    // These lists will be filled from your backend (Parse)
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

        btnGenerateBatches.setOnClickListener(v -> generateBatchFields());

        // Load subjects and teachers from Parse when activity starts
        loadSubjectsAndTeachers();
    }

    private void loadSubjectsAndTeachers() {
        // Load subjects
        ParseQuery<ParseObject> subjectQuery = ParseQuery.getQuery("Subject");
        subjectQuery.whereEqualTo("user", ParseUser.getCurrentUser());
        subjectQuery.findInBackground((subjects, e) -> {
            if (e == null) {
                allSubjects.clear();
                for (ParseObject obj : subjects) {
                    String name = obj.getString("name");
                    if (name != null) allSubjects.add(name);
                }
            }
        });

        // Load teachers
        ParseQuery<ParseObject> teacherQuery = ParseQuery.getQuery("Teacher");
        teacherQuery.whereEqualTo("user", ParseUser.getCurrentUser());
        teacherQuery.findInBackground((teachers, e) -> {
            if (e == null) {
                allTeachers.clear();
                for (ParseObject obj : teachers) {
                    String name = obj.getString("name");
                    if (name != null) allTeachers.add(name);
                }
            }
        });
    }

    private void generateBatchFields() {
        String totalBatchesText = etTotalBatches.getText().toString().trim();
        if (totalBatchesText.isEmpty()) {
            Toast.makeText(this, "Please enter total number of batches", Toast.LENGTH_SHORT).show();
            return;
        }

        int totalBatches = Integer.parseInt(totalBatchesText);
        layoutBatchesContainer.removeAllViews();
        batchCards.clear();
        tvBatchesHeader.setVisibility(View.VISIBLE);

        for (int i = 1; i <= totalBatches; i++) {
            View batchCard = createBatchCard(i);
            layoutBatchesContainer.addView(batchCard);
            batchCards.add(batchCard);
        }
    }

    private View createBatchCard(int batchNumber) {
        CardView cardView = new CardView(this);
        cardView.setCardElevation(8f);
        cardView.setRadius(16f);
        cardView.setUseCompatPadding(true);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 24);
        cardView.setLayoutParams(cardParams);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(24, 24, 24, 24);

        // Batch Title
        TextView tvBatchTitle = new TextView(this);
        tvBatchTitle.setText("Batch " + batchNumber);
        tvBatchTitle.setTextSize(20f);
        tvBatchTitle.setTextColor(getResources().getColor(R.color.purple_500));
        tvBatchTitle.setTypeface(tvBatchTitle.getTypeface(), android.graphics.Typeface.BOLD);
        tvBatchTitle.setPadding(0, 0, 0, 16);
        container.addView(tvBatchTitle);

        // Batch Name
        TextInputLayout tilBatchName = new TextInputLayout(this, null, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox);
        tilBatchName.setHint("Batch Name");
        TextInputEditText etBatchName = new TextInputEditText(this);
        tilBatchName.addView(etBatchName);
        tilBatchName.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        container.addView(tilBatchName);

        // Sections Label
        TextView tvSectionsLabel = new TextView(this);
        tvSectionsLabel.setText("Sections");
        tvSectionsLabel.setTextSize(16f);
        tvSectionsLabel.setTypeface(tvSectionsLabel.getTypeface(), android.graphics.Typeface.BOLD);
        tvSectionsLabel.setPadding(0, 24, 0, 8);
        container.addView(tvSectionsLabel);

        // Sections Checkboxes
        LinearLayout sectionsLayout = new LinearLayout(this);
        sectionsLayout.setOrientation(LinearLayout.HORIZONTAL);
        CheckBox cbSectionA = new CheckBox(this);
        cbSectionA.setText("Section A");
        sectionsLayout.addView(cbSectionA);
        CheckBox cbSectionB = new CheckBox(this);
        cbSectionB.setText("Section B");
        sectionsLayout.addView(cbSectionB);
        container.addView(sectionsLayout);

        // Academic Year
        TextInputLayout tilAcademicYear = new TextInputLayout(this, null, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox);
        tilAcademicYear.setHint("Academic Year (e.g., 2024-25)");
        TextInputEditText etAcademicYear = new TextInputEditText(this);
        tilAcademicYear.addView(etAcademicYear);
        tilAcademicYear.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        container.addView(tilAcademicYear);

        // Add extra space between Academic Year and Total Number of Subjects
        Space space = new Space(this);
        LinearLayout.LayoutParams spaceParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 24);
        space.setLayoutParams(spaceParams);
        container.addView(space);

        // Total Number of Subjects
        TextInputLayout tilTotalSubjects = new TextInputLayout(this, null, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox);
        tilTotalSubjects.setHint("Total Number of Subjects");
        TextInputEditText etTotalSubjects = new TextInputEditText(this);
        etTotalSubjects.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        tilTotalSubjects.addView(etTotalSubjects);
        tilTotalSubjects.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        container.addView(tilTotalSubjects);

        // Generate Subject Fields Button
        Button btnGenerateSubjects = new Button(this);
        btnGenerateSubjects.setText("Generate Subject Fields");
        btnGenerateSubjects.setBackgroundColor(getResources().getColor(R.color.purple_500));
        btnGenerateSubjects.setTextColor(getResources().getColor(android.R.color.white));
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(0, 24, 0, 0);
        btnGenerateSubjects.setLayoutParams(btnParams);
        container.addView(btnGenerateSubjects);

        // Subject fields container
        LinearLayout subjectContainer = new LinearLayout(this);
        subjectContainer.setOrientation(LinearLayout.VERTICAL);
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
            ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, allSubjects);
            subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerSubject.setAdapter(subjectAdapter);

            Spinner spinnerTeacher = new Spinner(this);
            spinnerTeacher.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            ArrayAdapter<String> teacherAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, allTeachers);
            teacherAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerTeacher.setAdapter(teacherAdapter);

            row.addView(spinnerSubject);
            row.addView(spinnerTeacher);

            subjectContainer.addView(row);
        }
    }
}
