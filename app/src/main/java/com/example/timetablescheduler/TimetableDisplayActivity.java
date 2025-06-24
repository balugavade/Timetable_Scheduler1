package com.example.timetablescheduler;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.parse.*;
import java.util.*;

public class TimetableDisplayActivity extends AppCompatActivity {

    private TableLayout tableLayout;
    private List<ParseObject> timetableEntries;
    private List<String> workingDays;
    private List<String> periods;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable_display);

        tableLayout = findViewById(R.id.tableLayout);
        fetchAndDisplayTimetable();
    }

    private void fetchAndDisplayTimetable() {
        // Fetch the latest generated timetable
        ParseQuery<ParseObject> query = ParseQuery.getQuery("GeneratedTimetable");
        query.whereEqualTo("user", ParseUser.getCurrentUser());
        query.orderByDescending("generatedAt");
        query.getFirstInBackground((timetable, e) -> {
            if (e == null && timetable != null) {
                fetchTimetableDetails(timetable);
            } else {
                showToast("No timetable found");
            }
        });
    }

    private void fetchTimetableDetails(ParseObject timetable) {
        // Fetch working days and periods configuration
        ParseQuery<ParseObject> configQuery = ParseQuery.getQuery("TimetableConfig");
        configQuery.whereEqualTo("user", ParseUser.getCurrentUser());
        configQuery.getFirstInBackground((config, e1) -> {
            if (e1 == null && config != null) {
                workingDays = config.getList("workingDays");
                List<ParseObject> periodObjs = config.getList("periods");
                periods = new ArrayList<>();
                for (ParseObject period : periodObjs) {
                    periods.add(period.getString("startTime") + "-" + period.getString("endTime"));
                }

                // Fetch timetable entries
                ParseQuery<ParseObject> entriesQuery = ParseQuery.getQuery("TimetableEntry");
                entriesQuery.findInBackground((entries, e2) -> {
                    if (e2 == null) {
                        timetableEntries = entries;
                        displayTimetable();
                    }
                });
            }
        });
    }

    private void displayTimetable() {
        if (workingDays == null || periods == null || timetableEntries == null) return;

        tableLayout.removeAllViews();

        // Create header row
        TableRow headerRow = new TableRow(this);
        headerRow.addView(createTextView("Time/Day", true));
        for (String day : workingDays) {
            headerRow.addView(createTextView(day, true));
        }
        tableLayout.addView(headerRow);

        // Create period rows
        for (int periodIndex = 0; periodIndex < periods.size(); periodIndex++) {
            TableRow row = new TableRow(this);
            row.addView(createTextView(periods.get(periodIndex), true));

            for (int dayIndex = 0; dayIndex < workingDays.size(); dayIndex++) {
                String cellContent = getCellContent(dayIndex, periodIndex);
                row.addView(createTextView(cellContent, false));
            }
            tableLayout.addView(row);
        }
    }

    private String getCellContent(int day, int period) {
        for (ParseObject entry : timetableEntries) {
            if (entry.getInt("day") == day && entry.getInt("period") == period) {
                return entry.getString("subject") + "\n" +
                        entry.getString("teacher") + "\n" +
                        entry.getString("batch") + "-" + entry.getString("section");
            }
        }
        return "Free";
    }

    private TextView createTextView(String text, boolean isHeader) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setPadding(12, 12, 12, 12);
        textView.setBackground(getResources().getDrawable(android.R.drawable.editbox_background));

        if (isHeader) {
            textView.setTypeface(null, android.graphics.Typeface.BOLD); // Corrected line
            textView.setBackgroundColor(getResources().getColor(R.color.purple_200));
        }

        TableRow.LayoutParams params = new TableRow.LayoutParams();
        params.weight = 1;
        textView.setLayoutParams(params);

        return textView;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
