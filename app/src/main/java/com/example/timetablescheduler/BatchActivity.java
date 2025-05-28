package com.example.timetablescheduler;

import android.content.Intent;
import android.os.Bundle;
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
    private LinearLayout layoutBatchesContainer;
    private List<BatchContainer> batchContainers = new ArrayList<>();

    private static class BatchContainer {
        EditText etBatchName;
        EditText etNumSubjects;
        LinearLayout subjectsContainer;
        List<SubjectView> subjectViews = new ArrayList<>();
    }

    private static class SubjectView {
        EditText etSubject, etTeacher, etHours;
        CheckBox cbLab;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batch);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        etNumBatches = findViewById(R.id.etNumBatches);
        layoutBatchesContainer = findViewById(R.id.layoutBatchesContainer);
        Button btnSetBatches = findViewById(R.id.btnSetBatches);
        Button btnNext = findViewById(R.id.btnNext);

        btnSetBatches.setOnClickListener(v -> createBatchContainers());
        btnNext.setOnClickListener(v -> validateAndSave());
    }

    private void createBatchContainers() {
        int batchCount = Integer.parseInt(etNumBatches.getText().toString());
        layoutBatchesContainer.removeAllViews();
        batchContainers.clear();

        for(int i=0; i<batchCount; i++){
            LinearLayout batchLayout = new LinearLayout(this);
            batchLayout.setOrientation(LinearLayout.VERTICAL);

            // Batch name input
            EditText etBatchName = new EditText(this);
            etBatchName.setHint("Batch " + (i+1) + " Name");
            batchLayout.addView(etBatchName);

            // Subjects configuration
            EditText etNumSubjects = new EditText(this);
            etNumSubjects.setHint("Number of Subjects");
            etNumSubjects.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            batchLayout.addView(etNumSubjects);

            Button btnSetSubjects = new Button(this);
            btnSetSubjects.setText("Configure Subjects");
            batchLayout.addView(btnSetSubjects);

            LinearLayout subjectsContainer = new LinearLayout(this);
            subjectsContainer.setOrientation(LinearLayout.VERTICAL);
            batchLayout.addView(subjectsContainer);

            BatchContainer container = new BatchContainer();
            container.etBatchName = etBatchName;
            container.etNumSubjects = etNumSubjects;
            container.subjectsContainer = subjectsContainer;

            btnSetSubjects.setOnClickListener(v ->
                    generateSubjectFields(container));

            batchContainers.add(container);
            layoutBatchesContainer.addView(batchLayout);
        }
    }

    private void generateSubjectFields(BatchContainer container) {
        int subjectCount = Integer.parseInt(container.etNumSubjects.getText().toString());
        container.subjectsContainer.removeAllViews();
        container.subjectViews.clear();

        for(int i=0; i<subjectCount; i++){
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);

            SubjectView subjectView = new SubjectView();

            // Subject name
            subjectView.etSubject = new EditText(this);
            subjectView.etSubject.setHint("Subject Name");
            row.addView(subjectView.etSubject);

            // Teacher name
            subjectView.etTeacher = new EditText(this);
            subjectView.etTeacher.setHint("Teacher");
            row.addView(subjectView.etTeacher);

            // Hours
            subjectView.etHours = new EditText(this);
            subjectView.etHours.setHint("Hours");
            subjectView.etHours.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            row.addView(subjectView.etHours);

            // Lab checkbox
            subjectView.cbLab = new CheckBox(this);
            subjectView.cbLab.setText("Lab");
            row.addView(subjectView.cbLab);

            container.subjectsContainer.addView(row);
            container.subjectViews.add(subjectView);
        }
    }

    private void validateAndSave() {
        List<Map<String, Object>> batchData = new ArrayList<>();

        for(BatchContainer container : batchContainers){
            Map<String, Object> batch = new HashMap<>();
            batch.put("name", container.etBatchName.getText().toString());

            List<Map<String, Object>> subjects = new ArrayList<>();
            for(SubjectView view : container.subjectViews){
                Map<String, Object> subject = new HashMap<>();
                subject.put("name", view.etSubject.getText().toString());
                subject.put("teacher", view.etTeacher.getText().toString());
                subject.put("hours", Integer.parseInt(view.etHours.getText().toString()));
                subject.put("lab", view.cbLab.isChecked());
                subjects.add(subject);
            }

            batch.put("subjects", subjects);
            batchData.add(batch);
        }

        // Save to Parse
        ParseObject timetableConfig = new ParseObject("TimetableConfig");
        timetableConfig.put("user", ParseUser.getCurrentUser());
        timetableConfig.put("batches", batchData);
        timetableConfig.saveInBackground(e -> {
            if(e == null) {
                startActivity(new Intent(this, TimetableActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
