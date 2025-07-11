package com.example.timetablescheduler;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.print.PrintManager;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.parse.*;

import java.util.*;

public class FacultyDashboardActivity extends AppCompatActivity {

    private TableLayout tableLayout;
    private TextView tvNoClasses;
    private Button btnLogout, btnPrint;
    private Spinner spinnerTeachers;
    private List<String> teacherNames = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;
    private List<ParseObject> approvedTimetables = new ArrayList<>();
    private Set<String> approvedTimetableIds = new HashSet<>();

    // Weekday order
    private static final List<String> WEEK_ORDER = Arrays.asList(
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_dashboard);

        tableLayout = findViewById(R.id.tableFacultyClasses);
        tvNoClasses = findViewById(R.id.tvNoClasses);
        btnLogout = findViewById(R.id.btnLogout);
        btnPrint = findViewById(R.id.btnPrint);
        spinnerTeachers = findViewById(R.id.spinnerTeachers);

        setTitle("Faculty Dashboard");

        btnLogout.setOnClickListener(v -> {
            ParseUser.logOut();
            Intent intent = new Intent(FacultyDashboardActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        btnPrint.setOnClickListener(v -> printTable());

        fetchApprovedTimetablesAndTeachers();
    }

    // Step 1: Fetch all approved timetables and build a set of their IDs
    private void fetchApprovedTimetablesAndTeachers() {
        ParseQuery<ParseObject> timetableQuery = ParseQuery.getQuery("GeneratedTimetable");
        timetableQuery.whereEqualTo("isApproved", true);
        timetableQuery.findInBackground((timetables, e) -> {
            if (e == null && timetables != null && !timetables.isEmpty()) {
                approvedTimetables.clear();
                approvedTimetableIds.clear();
                for (ParseObject t : timetables) {
                    approvedTimetables.add(t);
                    approvedTimetableIds.add(t.getObjectId());
                }
                fetchTeacherNamesFromApprovedTimetables();
            } else {
                runOnUiThread(() -> Toast.makeText(this, "No approved timetables found", Toast.LENGTH_LONG).show());
            }
        });
    }

    // Step 2: Fetch all unique teacher names only from entries in approved timetables
    private void fetchTeacherNamesFromApprovedTimetables() {
        ParseQuery<ParseObject> entryQuery = ParseQuery.getQuery("TimetableEntry");
        entryQuery.whereContainedIn("timetable", approvedTimetables);
        entryQuery.selectKeys(Collections.singletonList("teacher"));
        entryQuery.findInBackground((entries, e) -> {
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
                loadFacultyClasses(selectedTeacher);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                tableLayout.removeAllViews();
                tvNoClasses.setVisibility(View.VISIBLE);
            }
        });
    }

    // Step 3: Fetch all classes for the selected teacher, only from approved timetables
    private void loadFacultyClasses(String teacherName) {
        ParseQuery<ParseObject> entryQuery = ParseQuery.getQuery("TimetableEntry");
        entryQuery.whereContainedIn("timetable", approvedTimetables);
        entryQuery.whereEqualTo("teacher", teacherName);
        entryQuery.findInBackground((entries, err) -> {
            if (err == null && entries != null && !entries.isEmpty()) {
                List<MergedEntry> merged = mergeLabPeriods(entries);
                // Sort by week order
                merged.sort(Comparator.comparingInt(e -> WEEK_ORDER.indexOf(e.day)));
                runOnUiThread(() -> {
                    showTable(merged);
                    tvNoClasses.setVisibility(View.GONE);
                });
            } else {
                runOnUiThread(this::showNoClasses);
            }
        });
    }

    // Helper class for merged rows
    static class MergedEntry {
        String day;
        int startPeriod;
        int endPeriod;
        String subject;
        String batch;
        String section;

        MergedEntry(String day, int startPeriod, int endPeriod, String subject, String batch, String section) {
            this.day = day;
            this.startPeriod = startPeriod;
            this.endPeriod = endPeriod;
            this.subject = subject;
            this.batch = batch;
            this.section = section;
        }
    }

    // Merge consecutive lab periods for the same subject, batch, section, and day
    private List<MergedEntry> mergeLabPeriods(List<ParseObject> entries) {
        // Sort entries by day (week order), period
        entries.sort(Comparator
                .comparing((ParseObject e) -> {
                    String d = e.getString("day");
                    int idx = WEEK_ORDER.indexOf(d);
                    return idx == -1 ? 100 : idx;
                })
                .thenComparingInt(e -> e.getInt("period")));

        List<MergedEntry> mergedList = new ArrayList<>();
        for (int i = 0; i < entries.size(); ) {
            ParseObject entry = entries.get(i);
            String day = entry.getString("day");
            String subject = entry.getString("subject");
            String batch = entry.getString("batch");
            String section = entry.getString("section");
            int period = entry.getInt("period");
            boolean isLab = entry.has("isLab") && entry.getBoolean("isLab");

            int endPeriod = period;
            // Merge consecutive lab periods
            while (isLab && i + 1 < entries.size()) {
                ParseObject next = entries.get(i + 1);
                if (next.getString("day").equals(day)
                        && next.getString("subject").equals(subject)
                        && next.getString("batch").equals(batch)
                        && next.getString("section").equals(section)
                        && next.getInt("period") == endPeriod + 1
                        && next.has("isLab") && next.getBoolean("isLab")) {
                    endPeriod++;
                    i++;
                } else {
                    break;
                }
            }
            mergedList.add(new MergedEntry(day, period, endPeriod, subject, batch, section));
            i++;
        }
        return mergedList;
    }

    // Show merged, sorted table
    private void showTable(List<MergedEntry> mergedEntries) {
        tableLayout.removeAllViews();

        TableRow header = new TableRow(this);
        addCell(header, "Day", true);
        addCell(header, "Timing", true);
        addCell(header, "Subject", true);
        addCell(header, "Batch/Class", true);
        addCell(header, "Section", true);
        tableLayout.addView(header);

        for (MergedEntry me : mergedEntries) {
            TableRow row = new TableRow(this);
            addCell(row, me.day, false);

            String periodText = me.startPeriod == me.endPeriod
                    ? "Period " + me.startPeriod
                    : "Period " + me.startPeriod + " - Period " + me.endPeriod;
            addCell(row, periodText, false);

            addCell(row, me.subject, false);
            addCell(row, me.batch, false);
            addCell(row, me.section, false);
            tableLayout.addView(row);
        }
    }

    private void addCell(TableRow row, String text, boolean isHeader) {
        TextView tv = new TextView(this);
        tv.setText(text != null ? text : "-");
        tv.setTextSize(isHeader ? 18 : 16);
        tv.setTypeface(null, isHeader ? Typeface.BOLD : Typeface.NORMAL);
        tv.setTextColor(isHeader ? getResources().getColor(R.color.purple_700) : getResources().getColor(android.R.color.black));
        tv.setPadding(32, 20, 32, 20);
        tv.setGravity(Gravity.CENTER);
        tv.setBackgroundResource(R.drawable.table_cell_border);
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

    // PRINT FUNCTIONALITY
    private void printTable() {
        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        printManager.print("Faculty_Timetable", new TimetablePrintDocumentAdapter(this, tableLayout, "Faculty Timetable", "", ""), null);
    }
}
