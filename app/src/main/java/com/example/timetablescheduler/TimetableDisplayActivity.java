package com.example.timetablescheduler;

import android.os.Bundle;
import android.util.Log;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.parse.*;
import java.util.*;

public class TimetableDisplayActivity extends AppCompatActivity {
    private TableLayout tableLayout;
    private static final String TAG = "TimetableDisplay";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable_display);
        tableLayout = findViewById(R.id.timetableTable);
        fetchAndShowTimetable();
    }

    private void fetchAndShowTimetable() {
        // 1. Fetch TimetableConfig for days, periods, breaks
        ParseQuery<ParseObject> configQuery = ParseQuery.getQuery("TimetableConfig");
        configQuery.whereEqualTo("user", ParseUser.getCurrentUser());
        configQuery.orderByDescending("createdAt");
        configQuery.setLimit(1);
        configQuery.include("breaks");
        configQuery.include("periods");

        configQuery.getFirstInBackground((config, configErr) -> {
            if (config == null || configErr != null) {
                Log.e(TAG, "No config found or error: " + (configErr != null ? configErr.getMessage() : "null"));
                runOnUiThread(() -> showTimetableGrid(new String[5][7][2], Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday"), getDefaultPeriods(), new ArrayList<>()));
                return;
            }
            List<String> workingDays = config.getList("workingDays");
            List<ParseObject> periods = config.getList("periods");
            List<ParseObject> breaks = config.getList("breaks");
            int numDays = workingDays.size();
            int numPeriods = periods.size() + (breaks != null ? breaks.size() : 0);

            // 2. Fetch latest GeneratedTimetable
            ParseQuery<ParseObject> timetableQuery = ParseQuery.getQuery("GeneratedTimetable");
            timetableQuery.whereEqualTo("user", ParseUser.getCurrentUser());
            timetableQuery.orderByDescending("generatedAt");
            timetableQuery.setLimit(1);

            timetableQuery.getFirstInBackground((timetable, e) -> {
                if (timetable != null) {
                    // 3. Fetch TimetableEntry objects for this timetable
                    ParseQuery<ParseObject> entryQuery = ParseQuery.getQuery("TimetableEntry");
                    entryQuery.whereEqualTo("timetable", timetable);
                    entryQuery.findInBackground((entries, err) -> {
                        // Debug: Print workingDays and all entry days
                        Log.d(TAG, "WorkingDays: " + workingDays);
                        for (ParseObject entry : entries) {
                            Log.d(TAG, "Entry: day=" + entry.getString("day") +
                                    ", period=" + entry.getInt("period") +
                                    ", subject=" + entry.getString("subject") +
                                    ", teacher=" + entry.getString("teacher"));
                        }

                        // Map entries to grid: [day][period][subject, teacher]
                        String[][][] data = new String[numDays][numPeriods][2];
                        for (ParseObject entry : entries) {
                            String dayStr = entry.getString("day");
                            int dayIdx = workingDays.indexOf(dayStr);
                            int periodIdx = entry.getInt("period") - 1; // 1-based to 0-based

                            // Adjust periodIdx if breaks inserted (see below)
                            if (breaks != null && !breaks.isEmpty()) {
                                periodIdx = getPeriodIndexWithBreaks(periodIdx, breaks);
                            }
                            String subject = entry.getString("subject");
                            String teacher = entry.getString("teacher");

                            if (dayIdx >= 0 && dayIdx < numDays && periodIdx >= 0 && periodIdx < numPeriods) {
                                data[dayIdx][periodIdx][0] = subject;
                                data[dayIdx][periodIdx][1] = teacher;
                            } else {
                                Log.w(TAG, "Entry mapping out of bounds: dayIdx=" + dayIdx + ", periodIdx=" + periodIdx + ", dayStr=" + dayStr);
                            }
                        }
                        runOnUiThread(() -> showTimetableGrid(data, workingDays, periods, breaks));
                    });
                } else {
                    Log.w(TAG, "No GeneratedTimetable found for user.");
                    runOnUiThread(() -> showTimetableGrid(new String[numDays][numPeriods][2], workingDays, periods, breaks));
                }
            });
        });
    }

    // Helper for fallback periods if config is missing
    private List<ParseObject> getDefaultPeriods() {
        List<ParseObject> periods = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            ParseObject period = new ParseObject("Period");
            period.put("periodNumber", i);
            period.put("startTime", (8 + i) + ":00");
            period.put("endTime", (9 + i) + ":00");
            periods.add(period);
        }
        return periods;
    }

    // Helper to adjust period index if breaks are present
    private int getPeriodIndexWithBreaks(int periodIdx, List<ParseObject> breaks) {
        int offset = 0;
        for (ParseObject br : breaks) {
            // Defensive: ensure break object is fully fetched
            if (!br.isDataAvailable()) {
                try { br.fetchIfNeeded(); } catch (Exception e) { Log.e(TAG, "Error fetching break: " + e.getMessage()); }
            }
            int breakAfter = br.has("breakAfterPeriod") ? br.getInt("breakAfterPeriod") : -1;
            if (breakAfter != -1 && periodIdx >= breakAfter) offset++;
        }
        return periodIdx + offset;
    }

    private void showTimetableGrid(String[][][] timetableData, List<String> workingDays, List<ParseObject> periods, List<ParseObject> breaks) {
        tableLayout.removeAllViews();

        // Build header row (periods + breaks)
        TableRow headerRow = new TableRow(this);
        headerRow.setBackgroundColor(ContextCompat.getColor(this, R.color.header_background));
        TextView emptyHeader = createHeaderCell("");
        headerRow.addView(emptyHeader);

        int periodIdx = 0, breakIdx = 0;
        int totalColumns = periods.size() + (breaks != null ? breaks.size() : 0);
        for (int col = 0; col < totalColumns; col++) {
            boolean isBreak = false;
            if (breaks != null && breakIdx < breaks.size()) {
                ParseObject br = breaks.get(breakIdx);
                if (!br.isDataAvailable()) {
                    try { br.fetchIfNeeded(); } catch (Exception e) { Log.e(TAG, "Error fetching break: " + e.getMessage()); }
                }
                int breakAfter = br.has("breakAfterPeriod") ? br.getInt("breakAfterPeriod") : -1;
                isBreak = (col == breakAfter);
            }
            if (isBreak) {
                ParseObject br = breaks.get(breakIdx++);
                String label = "Break\n" + br.getString("startTime") + "-" + br.getString("endTime");
                headerRow.addView(createBreakHeaderCell(label));
            } else {
                ParseObject period = periods.get(periodIdx++);
                String label = period.getString("startTime") + "-" + period.getString("endTime");
                headerRow.addView(createHeaderCell(label));
            }
        }
        tableLayout.addView(headerRow);

        // Data rows
        for (int day = 0; day < workingDays.size(); day++) {
            TableRow row = new TableRow(this);
            TextView dayCell = createDayCell(workingDays.get(day));
            row.addView(dayCell);

            periodIdx = 0;
            breakIdx = 0;
            for (int col = 0; col < totalColumns; col++) {
                boolean isBreak = false;
                if (breaks != null && breakIdx < breaks.size()) {
                    ParseObject br = breaks.get(breakIdx);
                    if (!br.isDataAvailable()) {
                        try { br.fetchIfNeeded(); } catch (Exception e) { Log.e(TAG, "Error fetching break: " + e.getMessage()); }
                    }
                    int breakAfter = br.has("breakAfterPeriod") ? br.getInt("breakAfterPeriod") : -1;
                    isBreak = (col == breakAfter);
                }
                if (isBreak) {
                    ParseObject br = breaks.get(breakIdx++);
                    row.addView(createBreakCell(br.getString("startTime") + "-" + br.getString("endTime")));
                } else {
                    String[] classInfo = timetableData[day][col];
                    row.addView(createClassCell(classInfo));
                    periodIdx++;
                }
            }
            tableLayout.addView(row);
        }
    }

    private TextView createHeaderCell(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(12, 16, 12, 16);
        tv.setTextSize(14);
        tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        tv.setBackgroundColor(ContextCompat.getColor(this, R.color.header_background));
        tv.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        tv.setGravity(android.view.Gravity.CENTER);
        return tv;
    }

    private TextView createBreakHeaderCell(String text) {
        TextView tv = createHeaderCell(text);
        tv.setBackgroundColor(ContextCompat.getColor(this, R.color.break_background));
        return tv;
    }

    private TextView createDayCell(String dayName) {
        TextView tv = new TextView(this);
        tv.setText(dayName);
        tv.setPadding(16, 20, 16, 20);
        tv.setTextSize(14);
        tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        tv.setBackgroundColor(ContextCompat.getColor(this, R.color.day_background));
        tv.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        tv.setGravity(android.view.Gravity.CENTER);
        return tv;
    }

    private TextView createClassCell(String[] classInfo) {
        TextView tv = new TextView(this);
        tv.setMinHeight(100);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setBackgroundResource(R.drawable.cell_box);
        tv.setTextSize(13);
        if (classInfo != null && classInfo.length == 2 && classInfo[0] != null && classInfo[1] != null) {
            tv.setText(classInfo[0] + "\n" + classInfo[1]);
        } else {
            tv.setText(""); // Show empty if no class
        }
        return tv;
    }

    private TextView createBreakCell(String label) {
        TextView tv = new TextView(this);
        tv.setText("Break\n" + label);
        tv.setMinHeight(100);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setTextSize(13);
        tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        tv.setBackgroundColor(ContextCompat.getColor(this, R.color.break_background));
        tv.setBackgroundResource(R.drawable.cell_box);
        return tv;
    }
}
