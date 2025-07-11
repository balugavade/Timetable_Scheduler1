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
    private Button printButton, backButton, approveButton;
    private TextView messageText;
    private String selectedBatch, selectedSection, selectedAcademicYear;
    private List<ParseObject> generatedTimetableList = new ArrayList<>();
    private ParseObject selectedTimetable;
    private boolean isApproved = false;

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
        approveButton = findViewById(R.id.btnApprove);
        messageText = findViewById(R.id.messageText);

        loadGeneratedTimetables();

        printButton.setOnClickListener(v -> printTimetable());
        backButton.setOnClickListener(v -> {
            ParseUser.logOut();
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
        approveButton.setOnClickListener(v -> toggleApproval());
    }

    private void loadGeneratedTimetables() {
        ParseQuery<ParseObject> timetableQuery = ParseQuery.getQuery("GeneratedTimetable");
        // Optionally, show only unapproved timetables:
        // timetableQuery.whereEqualTo("isApproved", false);
        timetableQuery.orderByDescending("createdAt");
        timetableQuery.findInBackground((timetables, e) -> {
            if (e == null && timetables != null && !timetables.isEmpty()) {
                generatedTimetableList = timetables;
                List<String> batchNames = new ArrayList<>();
                for (ParseObject timetable : timetables) {
                    String name = timetable.getString("batch");
                    String section = timetable.has("section") ? timetable.getString("section") : "";
                    String year = timetable.has("academicYear") ? timetable.getString("academicYear") : "";
                    batchNames.add(name + (section.isEmpty() ? "" : " - " + section) + (year.isEmpty() ? "" : " (" + year + ")"));
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, batchNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                batchSpinner.setAdapter(adapter);
                batchSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        ParseObject timetable = generatedTimetableList.get(position);
                        selectedBatch = timetable.getString("batch");
                        selectedSection = timetable.has("section") ? timetable.getString("section") : "";
                        selectedAcademicYear = timetable.has("academicYear") ? timetable.getString("academicYear") : "";
                        selectedTimetable = timetable;
                        isApproved = timetable.has("isApproved") && timetable.getBoolean("isApproved");
                        fetchAndShowTimetable();
                    }
                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
                messageText.setVisibility(View.GONE);
            } else {
                showMessage("No generated timetables found.");
                clearTableAndInfo();
                approveButton.setVisibility(View.GONE);
            }
        });
    }

    private void fetchAndShowTimetable() {
        clearTableAndInfo();
        approveButton.setVisibility(View.GONE);

        ParseQuery<ParseObject> configQuery = ParseQuery.getQuery("TimetableConfig");
        configQuery.orderByDescending("createdAt");
        configQuery.setLimit(1);
        configQuery.include("breaks");
        configQuery.include("periods");

        configQuery.getFirstInBackground((config, configErr) -> {
            if (config == null || configErr != null) {
                showMessage("No timetable structure configured.\nAsk a user to configure periods, breaks, and days first.");
                return;
            }
            List<String> workingDays = config.getList("workingDays");
            List<ParseObject> periods = config.getList("periods");
            List<ParseObject> breaks = config.getList("breaks");

            if (workingDays == null || workingDays.isEmpty() ||
                    periods == null || periods.isEmpty()) {
                showMessage("No timetable structure configured.\nAsk a user to configure periods, breaks, and days first.");
                return;
            }

            int numDays = workingDays.size();
            int numPeriods = periods.size() + (breaks != null ? breaks.size() : 0);

            // Use the selectedTimetable directly (already loaded)
            ParseQuery<ParseObject> entryQuery = ParseQuery.getQuery("TimetableEntry");
            entryQuery.whereEqualTo("timetable", selectedTimetable);
            entryQuery.findInBackground((entries, err) -> {
                String[][][] timetableData = new String[numDays][numPeriods][3];
                if (entries != null && !entries.isEmpty()) {
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
                    runOnUiThread(() -> {
                        messageText.setVisibility(View.GONE);
                        renderTimetable(tableLayout, workingDays, periods, breaks, timetableData);
                        updateApproveButton();
                    });
                } else {
                    runOnUiThread(() -> {
                        showMessage("No timetable entries found for this batch.");
                        approveButton.setVisibility(View.GONE);
                    });
                }
            });
        });
    }

    private void toggleApproval() {
        if (selectedTimetable != null) {
            final boolean newApproval = !isApproved;
            selectedTimetable.put("isApproved", newApproval);
            selectedTimetable.saveInBackground(e -> {
                if (e == null) {
                    isApproved = newApproval;
                    runOnUiThread(this::updateApproveButton);
                    Toast.makeText(this, isApproved ? "Timetable approved!" : "Timetable disapproved!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Operation failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateApproveButton() {
        if (isApproved) {
            approveButton.setText("Disapprove");
            approveButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.red_500));
        } else {
            approveButton.setText("Approve");
            approveButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.green_500));
        }
        approveButton.setVisibility(View.VISIBLE);
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
            String[][][] timetableData
    ) {
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
                headerRow.addView(createHeaderCell(label));
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
            row.addView(createHeaderCell(workingDays.get(day)));
            periodIdx = 0;
            breakIdx = 0;
            for (int col = 0; col < numPeriods; col++) {
                boolean isBreak = isBreakCol(col, breaks);
                if (isBreak) {
                    ParseObject br = breaks.get(breakIdx++);
                    row.addView(createHeaderCell(br.getString("startTime") + "-" + br.getString("endTime")));
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
