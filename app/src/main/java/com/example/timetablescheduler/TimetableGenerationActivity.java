package com.example.timetablescheduler;

import android.content.Intent;
import android.os.Bundle;
import android.os.AsyncTask;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.parse.*;
import com.example.timetablescheduler.algorithm.GeneticAlgorithm;
import com.example.timetablescheduler.models.*;
import java.util.*;

public class TimetableGenerationActivity extends AppCompatActivity {

    private Button btnGenerateTimetable;
    private ProgressBar progressBar;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable_generation);

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        btnGenerateTimetable = findViewById(R.id.btnGenerateTimetable);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);

        progressBar.setVisibility(ProgressBar.GONE);
    }

    private void setupListeners() {
        btnGenerateTimetable.setOnClickListener(v -> generateTimetable());
    }

    private void generateTimetable() {
        btnGenerateTimetable.setEnabled(false);
        progressBar.setVisibility(ProgressBar.VISIBLE);
        tvStatus.setText("Fetching data...");

        new TimetableGenerationTask().execute();
    }

    private class TimetableGenerationTask extends AsyncTask<Void, String, Individual> {

        @Override
        protected Individual doInBackground(Void... voids) {
            try {
                publishProgress("Fetching teachers...");
                List<String> teachers = fetchTeachers();
                Map<String, Integer> teacherLoads = fetchTeacherLoads();
                Map<String, List<String>> teacherSubjects = fetchTeacherSubjects();

                publishProgress("Fetching subjects...");
                List<String> subjects = fetchSubjects();

                publishProgress("Fetching batches...");
                List<String> batches = fetchBatches();
                List<String> sections = Arrays.asList("A", "B");

                publishProgress("Fetching time configuration...");
                List<TimeSlot> timeSlots = fetchTimeSlots();

                publishProgress("Initializing genetic algorithm...");
                GeneticAlgorithm ga = new GeneticAlgorithm(teachers, subjects, batches, sections,
                        timeSlots, teacherLoads, teacherSubjects);

                publishProgress("Generating optimal timetable...");
                Individual bestTimetable = ga.generateTimetable();

                publishProgress("Saving timetable...");
                saveTimetable(bestTimetable);

                return bestTimetable;

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            tvStatus.setText(progress[0]);
        }

        @Override
        protected void onPostExecute(Individual result) {
            progressBar.setVisibility(ProgressBar.GONE);
            btnGenerateTimetable.setEnabled(true);

            if (result != null) {
                tvStatus.setText("Timetable generated successfully! Fitness: " +
                        String.format("%.2f%%", result.getFitness() * 100));

                Intent intent = new Intent(TimetableGenerationActivity.this, TimetableDisplayActivity.class);
                startActivity(intent);
            } else {
                tvStatus.setText("Failed to generate timetable. Please try again.");
                showToast("Timetable generation failed");
            }
        }
    }

    private List<String> fetchTeachers() throws Exception {
        List<String> teachers = new ArrayList<>();
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Teacher");
        query.whereEqualTo("user", ParseUser.getCurrentUser());
        List<ParseObject> results = query.find();

        for (ParseObject teacher : results) {
            teachers.add(teacher.getString("name"));
        }
        return teachers;
    }

    private Map<String, Integer> fetchTeacherLoads() throws Exception {
        Map<String, Integer> teacherLoads = new HashMap<>();
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Teacher");
        query.whereEqualTo("user", ParseUser.getCurrentUser());
        List<ParseObject> results = query.find();

        for (ParseObject teacher : results) {
            String name = teacher.getString("name");
            String loadStr = teacher.getString("load");
            int load = Integer.parseInt(loadStr != null ? loadStr : "20"); // Default load
            teacherLoads.put(name, load);
        }
        return teacherLoads;
    }

    private Map<String, List<String>> fetchTeacherSubjects() throws Exception {
        Map<String, List<String>> teacherSubjects = new HashMap<>();
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Teacher");
        query.whereEqualTo("user", ParseUser.getCurrentUser());
        List<ParseObject> results = query.find();

        for (ParseObject teacher : results) {
            String name = teacher.getString("name");
            List<String> subjects = teacher.getList("subjects");
            teacherSubjects.put(name, subjects != null ? subjects : new ArrayList<>());
        }
        return teacherSubjects;
    }

    private List<String> fetchSubjects() throws Exception {
        List<String> subjects = new ArrayList<>();
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Subject");
        query.whereEqualTo("user", ParseUser.getCurrentUser());
        List<ParseObject> results = query.find();

        for (ParseObject subject : results) {
            subjects.add(subject.getString("name"));
        }
        return subjects;
    }

    private List<String> fetchBatches() throws Exception {
        List<String> batches = new ArrayList<>();
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Batch");
        query.whereEqualTo("user", ParseUser.getCurrentUser());
        List<ParseObject> results = query.find();

        for (ParseObject batch : results) {
            batches.add(batch.getString("batchName"));
        }
        return batches;
    }

    private List<TimeSlot> fetchTimeSlots() throws Exception {
        List<TimeSlot> timeSlots = new ArrayList<>();
        ParseQuery<ParseObject> query = ParseQuery.getQuery("TimetableConfig");
        query.whereEqualTo("user", ParseUser.getCurrentUser());
        ParseObject config = query.getFirst();

        if (config != null) {
            List<String> workingDays = config.getList("workingDays");
            List<ParseObject> periods = config.getList("periods");

            for (int dayIndex = 0; dayIndex < workingDays.size(); dayIndex++) {
                for (int periodIndex = 0; periodIndex < periods.size(); periodIndex++) {
                    ParseObject period = periods.get(periodIndex);
                    TimeSlot timeSlot = new TimeSlot(dayIndex, periodIndex,
                            period.getString("startTime"),
                            period.getString("endTime"));
                    timeSlots.add(timeSlot);
                }
            }
        }

        return timeSlots;
    }

    private void saveTimetable(Individual timetable) {
        ParseObject timetableObj = new ParseObject("GeneratedTimetable");
        timetableObj.put("user", ParseUser.getCurrentUser());
        timetableObj.put("fitness", timetable.getFitness());
        timetableObj.put("generatedAt", new Date());

        List<ParseObject> classes = new ArrayList<>();
        for (TimetableClass tClass : timetable.getClasses()) {
            ParseObject classObj = new ParseObject("TimetableEntry");
            classObj.put("subject", tClass.getSubject());
            classObj.put("teacher", tClass.getTeacher());
            classObj.put("batch", tClass.getBatch());
            classObj.put("section", tClass.getSection());
            classObj.put("day", tClass.getTimeSlot().getDay());
            classObj.put("period", tClass.getTimeSlot().getPeriod());
            classObj.put("startTime", tClass.getTimeSlot().getStartTime());
            classObj.put("endTime", tClass.getTimeSlot().getEndTime());
            classes.add(classObj);
        }

        try {
            ParseObject.saveAll(classes);
            timetableObj.put("classes", classes);
            timetableObj.save();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
