package com.example.timetablescheduler;

import android.content.Context;
import android.print.PrintManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import com.parse.*;
import java.util.*;

public class TimetableDisplayActivity extends AppCompatActivity {
    private TableLayout tableLayout;
    private LinearLayout infoLayout;
    private Spinner batchSpinner;
    private Button printButton;
    private String selectedBatch, selectedSection, selectedAcademicYear;
    private static final String TAG = "TimetableDisplay";

    // Cell size constants
    private static final int CELL_HEIGHT = 120;
    private static final int CELL_WIDTH = 180;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable_display);
        tableLayout = findViewById(R.id.timetableTable);
        infoLayout = findViewById(R.id.infoLayout);
        batchSpinner = findViewById(R.id.batchSpinner);
        printButton = findViewById(R.id.printButton);

        loadBatchList();
        printButton.setOnClickListener(v -> printTimetable());
    }

    private void loadBatchList() {
        ParseQuery<ParseObject> batchQuery = ParseQuery.getQuery("Batch");
        batchQuery.whereEqualTo("user", ParseUser.getCurrentUser());
        batchQuery.findInBackground((batches, e) -> {
            if (e == null && batches != null) {
                List<String> batchNames = new ArrayList<>();
                for (ParseObject batch : batches) {
                    String name = batch.getString("name");
                    String section = batch.has("section") ? batch.getString("section") : "";
                    String year = batch.has("academicYear") ? batch.getString("academicYear") : "";
                    batchNames.add(name + (section.isEmpty() ? "" : " - " + section) + (year.isEmpty() ? "" : " (" + year + ")"));
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, batchNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                batchSpinner.setAdapter(adapter);
                batchSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        ParseObject batch = batches.get(position);
                        selectedBatch = batch.getString("name");
                        selectedSection = batch.has("section") ? batch.getString("section") : "";
                        selectedAcademicYear = batch.has("academicYear") ? batch.getString("academicYear") : "";
                        fetchAndShowTimetable();
                    }
                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
            }
        });
    }

    private void fetchAndShowTimetable() {
        ParseQuery<ParseObject> configQuery = ParseQuery.getQuery("TimetableConfig");
        configQuery.whereEqualTo("user", ParseUser.getCurrentUser());
        configQuery.orderByDescending("createdAt");
        configQuery.setLimit(1);
        configQuery.include("breaks");
        configQuery.include("periods");

        configQuery.getFirstInBackground((config, configErr) -> {
            if (config == null || configErr != null) {
                Log.e(TAG, "No config found or error: " + (configErr != null ? configErr.getMessage() : "null"));
                return;
            }
            List<String> workingDays = config.getList("workingDays");
            List<ParseObject> periods = config.getList("periods");
            List<ParseObject> breaks = config.getList("breaks");
            int numDays = workingDays.size();
            int numPeriods = periods.size() + (breaks != null ? breaks.size() : 0);

            ParseQuery<ParseObject> timetableQuery = ParseQuery.getQuery("GeneratedTimetable");
            timetableQuery.whereEqualTo("user", ParseUser.getCurrentUser());
            timetableQuery.whereEqualTo("batch", selectedBatch);
            if (!selectedSection.isEmpty()) timetableQuery.whereEqualTo("section", selectedSection);
            if (!selectedAcademicYear.isEmpty()) timetableQuery.whereEqualTo("academicYear", selectedAcademicYear);
            timetableQuery.orderByDescending("generatedAt");
            timetableQuery.setLimit(1);

            timetableQuery.getFirstInBackground((timetable, e) -> {
                if (timetable != null) {
                    ParseQuery<ParseObject> entryQuery = ParseQuery.getQuery("TimetableEntry");
                    entryQuery.whereEqualTo("timetable", timetable);
                    entryQuery.findInBackground((entries, err) -> {
                        Log.d(TAG, "Fetched entries: " + entries.size());
                        // Build a map: [day][periodCol] -> [subject, teacher, isLab]
                        String[][][] timetableData = new String[numDays][numPeriods][3];
                        for (ParseObject entry : entries) {
                            String dayStr = entry.getString("day");
                            int dayIdx = workingDays.indexOf(dayStr);
                            int periodIdx = entry.getInt("period") - 1;
                            int colIdx = getPeriodColIndex(periodIdx, breaks);
                            String subject = entry.getString("subject");
                            String teacher = entry.getString("teacher");
                            boolean isLab = entry.has("isLab") && entry.getBoolean("isLab");
                            if (dayIdx >= 0 && dayIdx < numDays && colIdx >= 0 && colIdx < numPeriods) {
                                timetableData[dayIdx][colIdx][0] = subject;
                                timetableData[dayIdx][colIdx][1] = teacher;
                                timetableData[dayIdx][colIdx][2] = String.valueOf(isLab);
                            }
                        }
                        runOnUiThread(() -> renderTimetable(tableLayout, workingDays, periods, breaks, timetableData));
                    });
                } else {
                    Log.w(TAG, "No GeneratedTimetable found for batch.");
                }
            });
        });
    }

    // Map period index to column index, skipping breaks
    private int getPeriodColIndex(int periodIdx, List<ParseObject> breaks) {
        int offset = 0;
        if (breaks != null) {
            for (ParseObject br : breaks) {
                int breakAfter = br.has("breakAfterPeriod") ? br.getInt("breakAfterPeriod") : -1;
                if (breakAfter != -1 && periodIdx >= breakAfter) offset++;
            }
        }
        return periodIdx + offset;
    }

    private boolean isBreakCol(int colIdx, List<ParseObject> breaks) {
        if (breaks == null) return false;
        int runningCol = 0;
        for (ParseObject br : breaks) {
            int breakAfter = br.has("breakAfterPeriod") ? br.getInt("breakAfterPeriod") : -1;
            int breakCol = breakAfter + runningCol;
            if (colIdx == breakCol) return true;
            runningCol++;
        }
        return false;
    }

    private void renderTimetable(
            TableLayout tableLayout,
            List<String> workingDays,
            List<ParseObject> periods,
            List<ParseObject> breaks,
            String[][][] timetableData // [day][col][subject, teacher, isLab]
    ) {
        tableLayout.removeAllViews();
        infoLayout.removeAllViews();

        // Show batch info above timetable
        String info = "Batch: " + selectedBatch;
        if (!selectedSection.isEmpty()) info += " | Section: " + selectedSection;
        if (!selectedAcademicYear.isEmpty()) info += " | Academic Year: " + selectedAcademicYear;
        TextView infoText = new TextView(this);
        infoText.setText(info);
        infoText.setTextSize(20);
        infoText.setGravity(Gravity.CENTER);
        infoLayout.addView(infoText);

        int numPeriods = periods.size() + (breaks != null ? breaks.size() : 0);

        // 1. Header Row
        TableRow headerRow = new TableRow(this);
        headerRow.addView(createHeaderCell(""));
        int periodIdx = 0, breakIdx = 0;
        for (int col = 0; col < numPeriods; col++) {
            boolean isBreak = isBreakCol(col, breaks);
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

        // 2. Data Rows
        for (int day = 0; day < workingDays.size(); day++) {
            TableRow row = new TableRow(this);
            row.addView(createDayCell(workingDays.get(day)));
            periodIdx = 0;
            breakIdx = 0;
            for (int col = 0; col < numPeriods; col++) {
                boolean isBreak = isBreakCol(col, breaks);
                if (isBreak) {
                    ParseObject br = breaks.get(breakIdx++);
                    row.addView(createBreakCell(br.getString("startTime") + "-" + br.getString("endTime")));
                } else {
                    String[] classInfo = timetableData[day][col];
                    // Defensive: never display a class in a break cell
                    if (classInfo == null || classInfo[0] == null) {
                        row.addView(createEmptyCell());
                    } else {
                        boolean isLab = classInfo[2] != null && Boolean.parseBoolean(classInfo[2]);
                        // Labs: merged cells only if next cell is not a break
                        if (isLab && col + 1 < numPeriods && !isBreakCol(col + 1, breaks)) {
                            TableRow.LayoutParams params = new TableRow.LayoutParams();
                            params.span = 2;
                            AppCompatTextView labCell = new AppCompatTextView(this);
                            labCell.setLayoutParams(params);
                            labCell.setText(classInfo[0] + "\nLab");
                            labCell.setGravity(Gravity.CENTER);
                            labCell.setBackgroundResource(R.drawable.cell_box);
                            labCell.setTextSize(18);
                            labCell.setMinHeight(CELL_HEIGHT);
                            labCell.setMinWidth(CELL_WIDTH * 2);
                            row.addView(labCell);
                            col++; // Skip next period
                        } else {
                            row.addView(createClassCell(classInfo));
                        }
                    }
                }
            }
            tableLayout.addView(row);
        }
    }

    private AppCompatTextView createHeaderCell(String text) {
        AppCompatTextView tv = new AppCompatTextView(this);
        tv.setText(text);
        tv.setPadding(12, 16, 12, 16);
        tv.setTextSize(16);
        tv.setGravity(Gravity.CENTER);
        tv.setBackgroundColor(ContextCompat.getColor(this, R.color.header_background));
        tv.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        tv.setMinWidth(CELL_WIDTH);
        tv.setMinHeight(CELL_HEIGHT);
        tv.setBackgroundResource(R.drawable.cell_box);
        return tv;
    }

    private AppCompatTextView createBreakHeaderCell(String text) {
        AppCompatTextView tv = createHeaderCell(text);
        tv.setBackgroundColor(ContextCompat.getColor(this, R.color.break_background));
        return tv;
    }

    private AppCompatTextView createDayCell(String dayName) {
        AppCompatTextView tv = new AppCompatTextView(this);
        tv.setText(dayName);
        tv.setPadding(16, 20, 16, 20);
        tv.setTextSize(16);
        tv.setGravity(Gravity.CENTER);
        tv.setBackgroundColor(ContextCompat.getColor(this, R.color.day_background));
        tv.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        tv.setMinWidth(CELL_WIDTH);
        tv.setMinHeight(CELL_HEIGHT);
        tv.setBackgroundResource(R.drawable.cell_box);
        return tv;
    }

    private AppCompatTextView createClassCell(String[] classInfo) {
        AppCompatTextView tv = new AppCompatTextView(this);
        tv.setMinHeight(CELL_HEIGHT);
        tv.setMinWidth(CELL_WIDTH);
        tv.setPadding(16, 16, 16, 16);
        tv.setGravity(Gravity.CENTER);
        tv.setBackgroundResource(R.drawable.cell_box);
        tv.setTextSize(18);
        if (classInfo != null && classInfo.length >= 2 && classInfo[0] != null && classInfo[1] != null) {
            tv.setText(classInfo[0] + "\n" + classInfo[1]);
        } else {
            tv.setText("");
        }
        return tv;
    }

    private AppCompatTextView createBreakCell(String label) {
        AppCompatTextView tv = new AppCompatTextView(this);
        tv.setText("Break\n" + label);
        tv.setMinHeight(CELL_HEIGHT);
        tv.setMinWidth(CELL_WIDTH);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(16);
        tv.setBackgroundColor(ContextCompat.getColor(this, R.color.break_background));
        tv.setBackgroundResource(R.drawable.cell_box);
        return tv;
    }

    private AppCompatTextView createEmptyCell() {
        AppCompatTextView tv = new AppCompatTextView(this);
        tv.setText("");
        tv.setMinHeight(CELL_HEIGHT);
        tv.setMinWidth(CELL_WIDTH);
        tv.setGravity(Gravity.CENTER);
        tv.setBackgroundResource(R.drawable.cell_box);
        return tv;
    }

    // Print/Download as PDF
    private void printTimetable() {
        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        printManager.print("Timetable_" + selectedBatch,
                new TimetablePrintDocumentAdapter(this, tableLayout, selectedBatch, selectedSection, selectedAcademicYear),
                null
        );
    }
}
