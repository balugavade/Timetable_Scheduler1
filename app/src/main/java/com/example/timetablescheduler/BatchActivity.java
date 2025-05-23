package com.example.timetablescheduler;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.parse.ParseObject;
import com.parse.ParseUser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BatchActivity extends AppCompatActivity {

    private EditText etNumBatches;
    private Button btnSetBatches, btnNext;
    private LinearLayout layoutBatchesContainer;
    private List<BatchContainer> batchContainers = new ArrayList<>();

    private static class BatchContainer {
        EditText etBatchName;
        EditText etNumSubjects;
        Button btnSetSubjects;
        LinearLayout subjectsContainer;
        List<SubjectTimeView> subjectViews = new ArrayList<>();
    }

    private static class SubjectTimeView {
        EditText etSubject;
        EditText etTimeRequired;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batch);

        // Toolbar setup
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Batch Management");
        }

        etNumBatches = findViewById(R.id.etNumBatches);
        btnSetBatches = findViewById(R.id.btnSetBatches);
        btnNext = findViewById(R.id.btnNext);
        layoutBatchesContainer = findViewById(R.id.layoutBatchesContainer);

        btnSetBatches.setOnClickListener(v -> generateBatchFields());
        btnNext.setOnClickListener(v -> saveBatchDataAndProceed());
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Go back to TeachersActivity
        Intent intent = new Intent(BatchActivity.this, TeachersActivity.class);
        startActivity(intent);
        finish();
        return true;
    }

    private void generateBatchFields() {
        layoutBatchesContainer.removeAllViews();
        batchContainers.clear();

        String numBatchesStr = etNumBatches.getText().toString().trim();
        if (numBatchesStr.isEmpty()) {
            Toast.makeText(this, "Enter number of batches", Toast.LENGTH_SHORT).show();
            return;
        }

        int numBatches = Integer.parseInt(numBatchesStr);

        for (int i = 0; i < numBatches; i++) {
            createBatchContainer(i + 1);
        }
    }

    private void createBatchContainer(int batchNumber) {
        BatchContainer batchContainer = new BatchContainer();

        LinearLayout batchLayout = new LinearLayout(this);
        batchLayout.setOrientation(LinearLayout.VERTICAL);
        batchLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        batchLayout.setPadding(16, 16, 16, 16);
        batchLayout.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);

        TextView batchLabel = new TextView(this);
        batchLabel.setText("Batch " + batchNumber);
        batchLabel.setTextSize(18);
        batchLabel.setTypeface(batchLabel.getTypeface(), Typeface.BOLD);
        batchLayout.addView(batchLabel);

        batchContainer.etBatchName = new EditText(this);
        batchContainer.etBatchName.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        batchContainer.etBatchName.setHint("Batch Name (e.g., B.Tech CSE Sem 1)");
        batchLayout.addView(batchContainer.etBatchName);

        batchContainer.etNumSubjects = new EditText(this);
        batchContainer.etNumSubjects.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        batchContainer.etNumSubjects.setHint("No of subjects");
        batchContainer.etNumSubjects.setInputType(InputType.TYPE_CLASS_NUMBER);
        batchLayout.addView(batchContainer.etNumSubjects);

        batchContainer.btnSetSubjects = new Button(this);
        batchContainer.btnSetSubjects.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        batchContainer.btnSetSubjects.setText("Set Subjects");
        batchLayout.addView(batchContainer.btnSetSubjects);

        batchContainer.subjectsContainer = new LinearLayout(this);
        batchContainer.subjectsContainer.setOrientation(LinearLayout.VERTICAL);
        batchContainer.subjectsContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        batchLayout.addView(batchContainer.subjectsContainer);

        batchContainer.btnSetSubjects.setOnClickListener(v ->
                generateSubjectFields(batchContainer));

        layoutBatchesContainer.addView(batchLayout);
        batchContainers.add(batchContainer);
    }

    private void generateSubjectFields(BatchContainer batchContainer) {
        batchContainer.subjectsContainer.removeAllViews();
        batchContainer.subjectViews.clear();

        String numSubjectsStr = batchContainer.etNumSubjects.getText().toString().trim();
        if (numSubjectsStr.isEmpty()) {
            Toast.makeText(this, "Enter number of subjects", Toast.LENGTH_SHORT).show();
            return;
        }

        int numSubjects = Integer.parseInt(numSubjectsStr);

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView subjectHeader = new TextView(this);
        subjectHeader.setText("Subject");
        subjectHeader.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        subjectHeader.setTypeface(subjectHeader.getTypeface(), Typeface.BOLD);

        TextView timeHeader = new TextView(this);
        timeHeader.setText("Time Required (hours)");
        timeHeader.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        timeHeader.setTypeface(timeHeader.getTypeface(), Typeface.BOLD);

        headerRow.addView(subjectHeader);
        headerRow.addView(timeHeader);
        batchContainer.subjectsContainer.addView(headerRow);

        for (int i = 0; i < numSubjects; i++) {
            SubjectTimeView subjectView = new SubjectTimeView();

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            row.setPadding(0, 8, 0, 8);

            subjectView.etSubject = new EditText(this);
            subjectView.etSubject.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            subjectView.etSubject.setHint("Subject name");

            subjectView.etTimeRequired = new EditText(this);
            subjectView.etTimeRequired.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            subjectView.etTimeRequired.setHint("Hours");
            subjectView.etTimeRequired.setInputType(InputType.TYPE_CLASS_NUMBER);

            row.addView(subjectView.etSubject);
            row.addView(subjectView.etTimeRequired);

            batchContainer.subjectsContainer.addView(row);
            batchContainer.subjectViews.add(subjectView);
        }
    }

    private void saveBatchDataAndProceed() {
        List<Map<String, Object>> batchesData = new ArrayList<>();

        for (BatchContainer container : batchContainers) {
            String batchName = container.etBatchName.getText().toString().trim();
            if (batchName.isEmpty()) {
                Toast.makeText(this, "Please fill all batch names", Toast.LENGTH_SHORT).show();
                return;
            }

            List<String> subjects = new ArrayList<>();
            List<Integer> timeRequirements = new ArrayList<>();

            for (SubjectTimeView subjectView : container.subjectViews) {
                String subject = subjectView.etSubject.getText().toString().trim();
                String timeStr = subjectView.etTimeRequired.getText().toString().trim();

                if (subject.isEmpty() || timeStr.isEmpty()) {
                    Toast.makeText(this, "Please fill all subject and time fields",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                subjects.add(subject);
                timeRequirements.add(Integer.parseInt(timeStr));
            }

            Map<String, Object> batchData = new HashMap<>();
            batchData.put("batchName", batchName);
            batchData.put("subjects", subjects);
            batchData.put("timeRequirements", timeRequirements);
            batchesData.add(batchData);
        }

        ParseObject batchInfo = new ParseObject("BatchInfo");
        batchInfo.put("user", ParseUser.getCurrentUser());
        batchInfo.put("numBatches", batchContainers.size());
        batchInfo.put("batchesData", batchesData);

        batchInfo.saveInBackground(e -> {
            if (e == null) {
                Toast.makeText(this, "Batch data saved!", Toast.LENGTH_SHORT).show();
                navigateToTimetableGenerator();
            } else {
                Toast.makeText(this, "Error saving batch data: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToTimetableGenerator() {
        Intent intent = new Intent(BatchActivity.this, TimetableGeneratorActivity.class);
        startActivity(intent);
        finish();
    }
}
