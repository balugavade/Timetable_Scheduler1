package com.example.timetablescheduler;

import android.content.Context;
import android.content.Intent;
import android.print.PrintManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import com.parse.*;
import java.util.*;

public class AdminApproveTimetableActivity extends AppCompatActivity {

    private Spinner batchSpinner;
    private LinearLayout infoLayout;
    private TableLayout tableLayout;
    private Button printButton, backButton;
    private TextView messageText;
    private String selectedBatch, selectedSection, selectedAcademicYear;
    private List<ParseObject> batchList = new ArrayList<>();
    private ParseObject selectedTimetable;

    // Cell size constants
    private static final int CELL_HEIGHT = 120;
    private static final int CELL_WIDTH = 180;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_approve_timetable);

        batchSpinner = findViewById(R.id.batchSpinner);
        infoLayout = findViewById(R.id.infoLayout);
        tableLayout = findViewById(R.id.timetableTable);
        printButton = findViewById(R.id.btnPrint);
        backButton = findViewById(R.id.btnBack);
        messageText = findViewById(R.id.messageText);

        loadBatchList();

        printButton.setOnClickListener(v -> printTimetable());

        backButton.setOnClickListener(v -> {
            ParseUser.logOut();
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadBatchList() {
        ParseQuery<ParseObject> batchQuery = ParseQuery.getQuery("Batch");
        batchQuery.findInBackground((batches, e) -> {
            if (e == null && batches != null && !batches.isEmpty()) {
                batchList = batches;
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
                        ParseObject batch = batchList.get(position);
                        selectedBatch = batch.getString("name");
                        selectedSection = batch.has("section") ? batch.getString("section") : "";
                        selectedAcademicYear = batch.has("academicYear") ? batch.getString("academicYear") : "";
                        fetchAndShowTimetable();
                    }
                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
                messageText.setVisibility(View.GONE);
            } else {
                showMessage("No batches found. Please ensure batches are configured.");
                clearTableAndInfo();
            }
        });
    }

    private void fetchAndShowTimetable() {
        ParseQuery<ParseObject> configQuery = ParseQuery.getQuery("TimetableConfig");
        configQuery.orderByDescending("createdAt");
        configQuery.setLimit(1);
        configQuery.include("breaks");
        configQuery.include("periods");

        configQuery.getFirstInBackground((config, configErr) -> {
            if (config == null || configErr != null) {
                showMessage("No timetable structure configured.\nAsk a user to configure periods, breaks, and days first.");
                clearTableAndInfo();
                return;
            }
            List<String> workingDays = config.getList("workingDays");
            List<ParseObject> periods = config.getList("periods");
            List<ParseObject> breaks = config.getList("breaks");

            if (workingDays == null || workingDays.isEmpty() ||
                    periods == null || periods.isEmpty()) {
                showMessage("No timetable structure configured.\nAsk a user to configure periods, breaks, and days first.");
                clearTableAndInfo();
                return;
            }

            int numDays = workingDays.size();
            int numPeriods = periods.size() + (breaks != null ? breaks.size() : 0);

            ParseQuery<ParseObject> timetableQuery = ParseQuery.getQuery("GeneratedTimetable");
            timetableQuery.whereEqualTo("batch", selectedBatch);
            if (!selectedSection.isEmpty()) timetableQuery.whereEqualTo("section", selectedSection);
            if (!selectedAcademicYear.isEmpty()) timetableQuery.whereEqualTo("academicYear", selectedAcademicYear);
            timetableQuery.orderByDescending("generatedAt");
            timetableQuery.setLimit(1);

            timetableQuery.getFirstInBackground((timetable, e) -> {
                if (timetable != null) {
                    selectedTimetable = timetable;
                    ParseQuery<ParseObject> entryQuery = ParseQuery.getQuery("TimetableEntry");
                    entryQuery.whereEqualTo("timetable", timetable);
                    entryQuery.findInBackground((entries, err) -> {
                        String[][][] timetableData = new String[numDays][numPeriods][3];
                        if (entries != null) {
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
                        }
                        runOnUiThread(() -> {
                            if (timetableData.length == 0 || timetableData[0].length == 0) {
                                showMessage("No timetable entries found for this batch.");
                                clearTableAndInfo();
                            } else {
                                messageText.setVisibility(View.GONE);
                                renderTimetable(tableLayout, workingDays, periods, breaks, timetableData);
                            }
                        });
                    });
                } else {
                    runOnUiThread(() -> {
                        showMessage("No timetable found for this batch.");
                        clearTableAndInfo();
                    });
                }
            });
        });
    }

    private void showMessage(String msg) {
        messageText.setText(msg);
        messageText.setVisibility(View.VISIBLE);
    }

    private void clearTableAndInfo() {
        tableLayout.removeAllViews();
        infoLayout.removeAllViews();
    }

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
        if (tableLayout == null || infoLayout == null ||
                workingDays == null || workingDays.isEmpty() ||
                periods == null || periods.isEmpty() ||
                timetableData == null || timetableData.length == 0 ||
                timetableData[0].length == 0) {
            clearTableAndInfo();
            showMessage("No timetable data to display.");
            return;
        }

        tableLayout.removeAllViews();
        infoLayout.removeAllViews();

        String info = "Batch: " + selectedBatch;
        if (selectedSection != null && !selectedSection.isEmpty()) info += " | Section: " + selectedSection;
        if (selectedAcademicYear != null && !selectedAcademicYear.isEmpty()) info += " | Academic Year: " + selectedAcademicYear;
        TextView infoText = new TextView(this);
        infoText.setText(info);
        infoText.setTextSize(20);
        infoText.setGravity(Gravity.CENTER);
        infoLayout.addView(infoText);

        int numPeriods = periods.size() + (breaks != null ? breaks.size() : 0);

        // Header Row
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

        // Data Rows
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
                    if (classInfo == null || classInfo[0] == null) {
                        row.addView(createEmptyCell());
                    } else {
                        boolean isLab = classInfo[2] != null && Boolean.parseBoolean(classInfo[2]);
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

    private void printTimetable() {
        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        printManager.print("Timetable_" + selectedBatch,
                new TimetablePrintDocumentAdapter(this, tableLayout, selectedBatch, selectedSection, selectedAcademicYear),
                null
        );
    }
}
