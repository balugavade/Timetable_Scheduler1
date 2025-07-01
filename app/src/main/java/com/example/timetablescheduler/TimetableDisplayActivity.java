package com.example.timetablescheduler;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.parse.*;
import java.util.*;

public class TimetableDisplayActivity extends AppCompatActivity {
    private TableLayout tableLayout;
    private List<String> workingDays;
    private List<String> periods;
    private List<ParseObject> timetableEntries;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable_display);

        tableLayout = findViewById(R.id.tableLayout);
        fetchAndDisplayTimetable();
    }

    private void fetchAndDisplayTimetable() {
        // Fetch latest generated timetable
        ParseQuery<ParseObject> query = ParseQuery.getQuery("GeneratedTimetable");
        query.whereEqualTo("user", ParseUser.getCurrentUser());
        query.orderByDescending("generatedAt");
        query.getFirstInBackground((timetable, e) -> {
            if (e == null && timetable != null) {
                double fitness = timetable.getDouble("fitness");
                Toast.makeText(this, String.format("Fitness Score: %.2f%%", fitness * 100),
                        Toast.LENGTH_LONG).show();

                fetchTimetableDetails(timetable);
            } else {
                Toast.makeText(this, "No timetable found. Please generate one first.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void fetchTimetableDetails(ParseObject timetable) {
        // Fetch timetable configuration
        ParseQuery<ParseObject> configQuery = ParseQuery.getQuery("TimetableConfig");
        configQuery.whereEqualTo("user", ParseUser.getCurrentUser());
        configQuery.getFirstInBackground((config, e1) -> {
            if (e1 == null && config != null) {
                workingDays = config.getList("workingDays");

                // Extract period times
                List<ParseObject> periodObjs = config.getList("periods");
                periods = new ArrayList<>();
                for (ParseObject period : periodObjs) {
                    periods.add(period.getString("startTime") + "-" + period.getString("endTime"));
                }

                // Fetch timetable entries
                ParseQuery<ParseObject> entriesQuery = ParseQuery.getQuery("TimetableEntry");
                entriesQuery.whereEqualTo("timetable", timetable);
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
        headerRow.addView(createHeaderCell("Time/Day"));
        for (String day : workingDays) {
            headerRow.addView(createHeaderCell(day));
        }
        tableLayout.addView(headerRow);

        // Create period rows
        for (int p = 0; p < periods.size(); p++) {
            TableRow row = new TableRow(this);
            row.addView(createHeaderCell(periods.get(p)));

            for (int d = 0; d < workingDays.size(); d++) {
                TextView cell = createDataCell(getCellContent(d, p));
                row.addView(cell);
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

    private TextView createHeaderCell(String text) {
        TextView tv = createBaseCell(text);
        tv.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_200));
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        return tv;
    }

    private TextView createDataCell(String text) {
        TextView tv = createBaseCell(text);
        if ("Free".equals(text)) {
            tv.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_50));
        } else {
            tv.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white));
        }
        return tv;
    }

    private TextView createBaseCell(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setPadding(16, 16, 16, 16);
        textView.setMinHeight(120);
        textView.setMinWidth(250);
        textView.setGravity(android.view.Gravity.CENTER);
        textView.setTextSize(12);
        textView.setBackground(ContextCompat.getDrawable(this, android.R.drawable.editbox_background));
        return textView;
    }
}
