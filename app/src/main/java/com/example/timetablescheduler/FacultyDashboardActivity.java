package com.example.timetablescheduler;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.parse.*;

import java.util.*;

public class FacultyDashboardActivity extends AppCompatActivity {

    private TableLayout tableLayout;
    private TextView tvNoClasses;
    private Button btnLogout;
    private Spinner spinnerTeachers, spinnerTimetables;
    private List<String> teacherNames = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;
    private List<ParseObject> timetableList = new ArrayList<>();
    private ArrayAdapter<String> timetableAdapter;
    private String selectedTimetableId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_dashboard);

        tableLayout = findViewById(R.id.tableFacultyClasses);
        tvNoClasses = findViewById(R.id.tvNoClasses);
        btnLogout = findViewById(R.id.btnLogout);
        spinnerTeachers = findViewById(R.id.spinnerTeachers);
        spinnerTimetables = findViewById(R.id.spinnerTimetables);

        setTitle("Faculty Dashboard");

        btnLogout.setOnClickListener(v -> {
            ParseUser.logOut();
            Intent intent = new Intent(FacultyDashboardActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        fetchTimetables();
    }

    private void fetchTimetables() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("GeneratedTimetable");
        query.whereEqualTo("isApproved", true);
        query.findInBackground((timetables, e) -> {
            if (e == null && timetables != null && !timetables.isEmpty()) {
                timetableList.clear();
                timetableList.addAll(timetables);
                runOnUiThread(this::setupTimetableSpinner);
            } else {
                runOnUiThread(() -> Toast.makeText(this, "No approved timetables found", Toast.LENGTH_LONG).show());
            }
        });
    }

    private void setupTimetableSpinner() {
        List<String> timetableNames = new ArrayList<>();
        for (ParseObject timetable : timetableList) {
            String name = timetable.has("name") ? timetable.getString("name") : timetable.getObjectId();
            timetableNames.add(name);
        }
        timetableAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, timetableNames);
        timetableAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTimetables.setAdapter(timetableAdapter);

        spinnerTimetables.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedTimetableId = timetableList.get(position).getObjectId();
                fetchTeacherNames(selectedTimetableId);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                tableLayout.removeAllViews();
                tvNoClasses.setVisibility(View.VISIBLE);
            }
        });
    }

    private void fetchTeacherNames(String timetableId) {
        ParseObject selectedTimetable = ParseObject.createWithoutData("GeneratedTimetable", timetableId);
        ParseQuery<ParseObject> query = ParseQuery.getQuery("TimetableEntry");
        query.whereEqualTo("timetable", selectedTimetable);
        query.selectKeys(Collections.singletonList("teacher"));
        query.findInBackground((entries, e) -> {
            if (e == null && entries != null) {
                Set<String> uniqueNames = new HashSet<>();
                for (ParseObject entry : entries) {
                    String name = entry.getString("teacher");
                    if (name != null && !name.trim().isEmpty()) {
                        uniqueNames.add(name);
                    }
                }
                teacherNames.clear();
                teacherNames.addAll(uniqueNames);
                Collections.sort(teacherNames);
                runOnUiThread(this::setupTeacherSpinner);
            } else {
                runOnUiThread(() -> Toast.makeText(this, "Failed to load teacher names", Toast.LENGTH_LONG).show());
            }
        });
    }

    private void setupTeacherSpinner() {
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, teacherNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTeachers.setAdapter(spinnerAdapter);

        spinnerTeachers.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedTeacher = teacherNames.get(position);
                loadFacultyClasses(selectedTeacher, selectedTimetableId);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                tableLayout.removeAllViews();
                tvNoClasses.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadFacultyClasses(String teacherName, String timetableId) {
        ParseObject selectedTimetable = ParseObject.createWithoutData("GeneratedTimetable", timetableId);
        ParseQuery<ParseObject> entryQuery = ParseQuery.getQuery("TimetableEntry");
        entryQuery.whereEqualTo("timetable", selectedTimetable);
        entryQuery.whereEqualTo("teacher", teacherName);
        entryQuery.findInBackground((entries, err) -> {
            if (err == null && entries != null && !entries.isEmpty()) {
                runOnUiThread(() -> {
                    showTable(entries);
                    tvNoClasses.setVisibility(View.GONE);
                });
            } else {
                runOnUiThread(this::showNoClasses);
            }
        });
    }

    private void showTable(List<ParseObject> entries) {
        tableLayout.removeAllViews();

        TableRow header = new TableRow(this);
        addCell(header, "Day", true);
        addCell(header, "Timing", true);
        addCell(header, "Subject", true);
        addCell(header, "Class/Batch", true);
        addCell(header, "Section", true);
        tableLayout.addView(header);

        for (ParseObject entry : entries) {
            TableRow row = new TableRow(this);
            addCell(row, entry.getString("day"), false);
            String period = entry.has("period") ? "Period " + entry.getInt("period") : "-";
            addCell(row, period, false);
            addCell(row, entry.getString("subject"), false);
            addCell(row, entry.getString("batch"), false);
            addCell(row, entry.getString("section"), false);
            tableLayout.addView(row);
        }
    }

    private void addCell(TableRow row, String text, boolean isHeader) {
        TextView tv = new TextView(this);
        tv.setText(text != null ? text : "-");
        tv.setTextSize(14);
        tv.setTypeface(null, isHeader ? Typeface.BOLD : Typeface.NORMAL);
        tv.setPadding(24, 16, 24, 16);
        tv.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        tv.setBackgroundResource(R.drawable.table_cell_border);
        tv.setMaxLines(3);
        tv.setEllipsize(null);
        tv.setSingleLine(false);
        tv.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
        ));
        row.addView(tv);
    }

    private void showNoClasses() {
        tableLayout.removeAllViews();
        tvNoClasses.setVisibility(View.VISIBLE);
    }
}
