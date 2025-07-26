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
import androidx.appcompat.widget.Toolbar;
import com.parse.*;
import java.util.*;

public class FacultyDashboardActivity extends AppCompatActivity {

    private TableLayout tableLayout;
    private TextView tvNoClasses;
    private Button btnLogout, btnPrint;
    private Spinner spinnerTeachers;
    private List<String> teacherNames = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;
    private List<ParseObject> timetableList = new ArrayList<>();
    private String selectedTeacherName = "";

    private static final List<String> WEEK_ORDER = Arrays.asList(
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    );

    // Map: timetableId -> list of [startTime, endTime] for each period (1-based)
    private Map<String, List<String[]>> periodTimesMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_dashboard);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tableLayout = findViewById(R.id.tableFacultyClasses);
        tvNoClasses = findViewById(R.id.tvNoClasses);
        btnLogout = findViewById(R.id.btnLogout);
        btnPrint = findViewById(R.id.btnPrint);
        spinnerTeachers = findViewById(R.id.spinnerTeachers);

        btnLogout.setOnClickListener(v -> {
            ParseUser.logOut();
            Intent intent = new Intent(FacultyDashboardActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        btnPrint.setOnClickListener(v -> printTable());

        fetchTeacherNames();
    }

    // Step 1: Fetch all teacher names and period timings that appear in approved timetables
    private void fetchTeacherNames() {
        ParseQuery<ParseObject> timetableQuery = ParseQuery.getQuery("GeneratedTimetable");
        timetableQuery.whereEqualTo("isApprovedByAdmin", true);
        timetableQuery.whereEqualTo("isApprovedByDean", true);
        timetableQuery.whereEqualTo("isApprovedByPrincipal", true);
        timetableQuery.findInBackground((timetables, e) -> {
            if (e != null || timetables == null || timetables.isEmpty()) {
                runOnUiThread(() -> Toast.makeText(this, "No approved timetables found", Toast.LENGTH_LONG).show());
                return;
            }
            timetableList.clear();
            timetableList.addAll(timetables);
            periodTimesMap.clear();

            // --- Fetch period slots for all approved timetables ---
            // We'll count here and trigger next step once ALL configs fetched (async!)
            final int[] configsFetched = {0};
            for (ParseObject timetable : timetableList) {
                String timetableId = timetable.getObjectId();

                // You may need to adapt this query to match your schema - e.g., match on "user", "batch", or "config" pointer!
                ParseQuery<ParseObject> configQuery = ParseQuery.getQuery("TimetableConfig");
                configQuery.whereEqualTo("user", timetable.getParseUser("user")); // adapt if needed!
                configQuery.orderByDescending("createdAt");
                configQuery.setLimit(1);
                configQuery.include("periods");

                configQuery.findInBackground((configs, e2) -> {
                    configsFetched[0]++;
                    if (e2 == null && configs != null && !configs.isEmpty()) {
                        ParseObject config = configs.get(0);
                        List<ParseObject> periodObjs = config.getList("periods");
                        List<String[]> slots = new ArrayList<>();
                        if (periodObjs != null) {
                            for (ParseObject pObj : periodObjs) {
                                String st = pObj.getString("startTime");
                                String et = pObj.getString("endTime");
                                slots.add(new String[]{st, et});
                            }
                        }
                        periodTimesMap.put(timetableId, slots);
                    }
                    // after all configs fetched (or failed), now fetch classes
                    if (configsFetched[0] == timetableList.size()) {
                        fetchAndSetupTeacherSpinner(timetables); // only continue after periodTimesMap populated
                    }
                });
            }
        });
    }

    // Helper: Build unique teacher list after fetching periods/timings
    private void fetchAndSetupTeacherSpinner(List<ParseObject> timetables) {
        // Fetch teacher names from all TimetableEntry objects
        List<ParseQuery<ParseObject>> subQueries = new ArrayList<>();
        for (ParseObject timetable : timetables) {
            ParseQuery<ParseObject> entryQuery = ParseQuery.getQuery("TimetableEntry");
            entryQuery.whereEqualTo("timetable", timetable);
            subQueries.add(entryQuery);
        }
        ParseQuery<ParseObject> mainQuery = ParseQuery.or(subQueries);
        mainQuery.selectKeys(Collections.singletonList("teacher"));
        mainQuery.findInBackground((entries, ee) -> {
            if (ee != null) {
                runOnUiThread(() -> Toast.makeText(this, "Error fetching teachers", Toast.LENGTH_LONG).show());
                return;
            }
            Set<String> uniqueNames = new HashSet<>();
            for (ParseObject entry : entries) {
                String teacher = entry.getString("teacher");
                if (teacher != null && !teacher.trim().isEmpty())
                    uniqueNames.add(teacher);
            }
            teacherNames.clear();
            teacherNames.addAll(uniqueNames);
            Collections.sort(teacherNames);
            runOnUiThread(this::setupTeacherSpinner);
        });
    }

    // Step 2: Show spinner for teachers, on selection: show their classes from all approved timetables
    private void setupTeacherSpinner() {
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, teacherNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTeachers.setAdapter(spinnerAdapter);

        spinnerTeachers.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedTeacherName = teacherNames.get(position);
                loadFacultyClasses(selectedTeacherName);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                tableLayout.removeAllViews();
                tvNoClasses.setVisibility(View.VISIBLE);
            }
        });
    }

    // Step 3: Find timetable entries for selected teacher, from all approved timetables
    private void loadFacultyClasses(String teacherName) {
        List<ParseQuery<ParseObject>> queries = new ArrayList<>();
        for (ParseObject timetable : timetableList) {
            ParseQuery<ParseObject> entryQuery = ParseQuery.getQuery("TimetableEntry");
            entryQuery.whereEqualTo("timetable", timetable);
            entryQuery.whereEqualTo("teacher", teacherName);
            queries.add(entryQuery);
        }
        if (queries.isEmpty()) {
            showNoClasses();
            return;
        }
        ParseQuery<ParseObject> mainQuery = ParseQuery.or(queries);
        mainQuery.findInBackground((entries, err) -> {
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

    static class MergedEntry {
        String day;
        int startPeriod;
        int endPeriod;
        String subject;
        String batch;
        String section;
        String timetableId; // NEW

        MergedEntry(String day, int startPeriod, int endPeriod, String subject, String batch, String section, String timetableId) {
            this.day = day;
            this.startPeriod = startPeriod;
            this.endPeriod = endPeriod;
            this.subject = subject;
            this.batch = batch;
            this.section = section;
            this.timetableId = timetableId;
        }
    }

    private List<MergedEntry> mergeLabPeriods(List<ParseObject> entries) {
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
            String timetableId = "";
            if (entry.has("timetable") && entry.getParseObject("timetable") != null)
                timetableId = entry.getParseObject("timetable").getObjectId();
            boolean isLab = entry.has("isLab") && entry.getBoolean("isLab");

            int endPeriod = period;
            while (isLab && i + 1 < entries.size()) {
                ParseObject next = entries.get(i + 1);
                if (next.getString("day").equals(day)
                        && next.getString("subject").equals(subject)
                        && next.getString("batch").equals(batch)
                        && next.getString("section").equals(section)
                        && next.getInt("period") == endPeriod + 1
                        && next.has("isLab") && next.getBoolean("isLab")
                        && timetableId.equals(next.getParseObject("timetable") != null ? next.getParseObject("timetable").getObjectId() : "")) {
                    endPeriod++;
                    i++;
                } else {
                    break;
                }
            }
            mergedList.add(new MergedEntry(day, period, endPeriod, subject, batch, section, timetableId));
            i++;
        }
        return mergedList;
    }

    private void showTable(List<MergedEntry> entries) {
        tableLayout.removeAllViews();

        TableRow header = new TableRow(this);
        addCell(header, "Day", true);
        addCell(header, "Timing", true); // Will have start/end time!
        addCell(header, "Subject", true);
        addCell(header, "Batch/Class", true);
        addCell(header, "Section", true);
        tableLayout.addView(header);

        for (MergedEntry me : entries) {
            TableRow row = new TableRow(this);
            addCell(row, me.day, false);
            // Here’s the actual timing, not just "Period 1"
            String timingText = getTimingForEntry(me.timetableId, me.startPeriod, me.endPeriod);
            addCell(row, timingText, false);
            addCell(row, me.subject, false);
            addCell(row, me.batch, false);
            addCell(row, me.section, false);
            tableLayout.addView(row);
        }
    }

    private String getTimingForEntry(String timetableId, int startPeriod, int endPeriod) {
        List<String[]> slots = periodTimesMap.get(timetableId);
        if (slots == null || slots.isEmpty()) return "-";
        int startIdx = Math.max(0, startPeriod - 1);
        int endIdx = Math.max(0, endPeriod - 1);
        if (startIdx >= slots.size() || endIdx >= slots.size())
            return "-";
        String st = slots.get(startIdx)[0], et = slots.get(endIdx)[1];
        // You can prettify formatting here if needed
        return st + " - " + et;
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

    private void printTable() {
        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        printManager.print("Faculty_Timetable",
                new FacultyTimetablePrintAdapter(this, tableLayout, selectedTeacherName),
                null
        );
    }
}
