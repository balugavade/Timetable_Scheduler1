package com.example.timetablescheduler;

import android.os.Bundle;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.parse.*;
import java.util.*;

public class TimetableDisplayActivity extends AppCompatActivity {
    private TableLayout tableLayout;
    private final String[] timePeriods = {
            "09:00-10:00", "10:00-11:00", "11:00-12:00", "Break", "13:00-14:00", "14:00-15:00", "15:00-16:00"
    };
    private final String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable_display);
        tableLayout = findViewById(R.id.timetableTable);
        fetchAndShowTimetable();
    }

    private void fetchAndShowTimetable() {
        // Fetch the latest timetable for the user
        ParseQuery<ParseObject> timetableQuery = ParseQuery.getQuery("GeneratedTimetable");
        timetableQuery.whereEqualTo("user", ParseUser.getCurrentUser());
        timetableQuery.orderByDescending("generatedAt");
        timetableQuery.setLimit(1);

        timetableQuery.getFirstInBackground((timetable, e) -> {
            if (timetable != null) {
                ParseQuery<ParseObject> entryQuery = ParseQuery.getQuery("TimetableEntry");
                entryQuery.whereEqualTo("timetable", timetable);
                entryQuery.findInBackground((entries, err) -> {
                    String[][][] data = new String[5][7][2]; // [day][period][subject/teacher]
                    for (ParseObject entry : entries) {
                        int dayIdx = getDayIndex(entry.getString("day"));
                        int periodIdx = entry.getInt("period") - 1; // assuming periods are 1-based
                        String subject = entry.getString("subject");
                        String teacher = entry.getString("teacher");
                        if (dayIdx >= 0 && dayIdx < 5 && periodIdx >= 0 && periodIdx < 7) {
                            data[dayIdx][periodIdx][0] = subject;
                            data[dayIdx][periodIdx][1] = teacher;
                        }
                    }
                    runOnUiThread(() -> showTimetableGrid(data));
                });
            } else {
                // No timetable found, show empty grid
                runOnUiThread(() -> showTimetableGrid(new String[5][7][2]));
            }
        });
    }

    private int getDayIndex(String day) {
        if (day == null) return -1;
        switch (day.trim().toLowerCase()) {
            case "monday": return 0;
            case "tuesday": return 1;
            case "wednesday": return 2;
            case "thursday": return 3;
            case "friday": return 4;
            default: return -1;
        }
    }

    private void showTimetableGrid(String[][][] timetableData) {
        tableLayout.removeAllViews();

        // Header row
        TableRow headerRow = new TableRow(this);
        headerRow.setBackgroundColor(ContextCompat.getColor(this, R.color.header_background));
        TextView emptyHeader = createHeaderCell(""); // Top-left empty cell
        headerRow.addView(emptyHeader);
        for (String period : timePeriods) {
            TextView periodHeader = createHeaderCell(period);
            headerRow.addView(periodHeader);
        }
        tableLayout.addView(headerRow);

        // Data rows
        for (int day = 0; day < days.length; day++) {
            TableRow row = new TableRow(this);
            TextView dayCell = createDayCell(days[day]);
            row.addView(dayCell);

            for (int period = 0; period < timePeriods.length; period++) {
                TextView cell;
                if ("Break".equals(timePeriods[period])) {
                    cell = createBreakCell();
                } else {
                    String[] classInfo = timetableData[day][period];
                    cell = createClassCell(classInfo);
                }
                row.addView(cell);
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
        if (classInfo != null && classInfo.length == 2 && classInfo[0] != null) {
            tv.setText(classInfo[0] + "\n" + classInfo[1]);
        } else {
            tv.setText("Free");
        }
        return tv;
    }

    private TextView createBreakCell() {
        TextView tv = new TextView(this);
        tv.setText("Break");
        tv.setMinHeight(100);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setTextSize(13);
        tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        tv.setBackgroundColor(ContextCompat.getColor(this, R.color.break_background));
        tv.setBackgroundResource(R.drawable.cell_box);
        return tv;
    }
}
