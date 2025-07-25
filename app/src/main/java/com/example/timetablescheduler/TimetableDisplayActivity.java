package com.example.timetablescheduler;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.print.PrintManager;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import com.example.timetablescheduler.models.TimetableEdit;
import com.parse.*;
import java.util.*;

public class TimetableDisplayActivity extends AppCompatActivity {
    private TableLayout tableLayout;
    private LinearLayout infoLayout;
    private Spinner batchSpinner;
    private Button printButton, exitButton, saveButton;
    private static final int CELL_HEIGHT = 120, CELL_WIDTH = 180;

    private List<ParseObject> timetableBatches = new ArrayList<>();
    private Map<String, TimetableEdit> manualEdits = new HashMap<>(); // Key: dayIdx_colIdx
    private List<String> workingDays;
    private List<ParseObject> periods, breaks;
    private String selectedBatch, selectedSection, selectedAcademicYear;
    private ParseObject selectedTimetableObj;
    private Map<String, ParseObject> backendEntryMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable_display);

        tableLayout = findViewById(R.id.timetableTable);
        infoLayout = findViewById(R.id.infoLayout);
        batchSpinner = findViewById(R.id.batchSpinner);
        printButton = findViewById(R.id.printButton);
        exitButton = findViewById(R.id.exitButton);
        saveButton = findViewById(R.id.saveButton);

        printButton.setOnClickListener(v -> printTimetable());
        exitButton.setOnClickListener(v -> finish());
        saveButton.setOnClickListener(v -> saveManualEdits());
        loadBatchListWithTimetables();
    }

    private void loadBatchListWithTimetables() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("GeneratedTimetable");
        query.whereEqualTo("user", ParseUser.getCurrentUser());
        query.orderByDescending("createdAt");
        query.findInBackground((timetables, e) -> {
            if (e != null || timetables == null || timetables.isEmpty()) {
                timetableBatches.clear();
                batchSpinner.setAdapter(null);
                clearTimetableUI();
                Toast.makeText(this, "No generated timetables found.", Toast.LENGTH_SHORT).show();
                return;
            }
            List<String> batchNames = new ArrayList<>();
            timetableBatches.clear();
            for (ParseObject timetable : timetables) {
                String batchName = timetable.getString("batch");
                String section = timetable.has("section") ? timetable.getString("section") : "";
                String year = timetable.has("academicYear") ? timetable.getString("academicYear") : "";
                batchNames.add(batchName + (section.isEmpty() ? "" : " - " + section) + (year.isEmpty() ? "" : " (" + year + ")"));
                timetableBatches.add(timetable);
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, batchNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            batchSpinner.setAdapter(adapter);
            batchSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    selectedTimetableObj = timetableBatches.get(pos);
                    selectedBatch = selectedTimetableObj.getString("batch");
                    selectedSection = selectedTimetableObj.has("section") ? selectedTimetableObj.getString("section") : "";
                    selectedAcademicYear = selectedTimetableObj.has("academicYear") ? selectedTimetableObj.getString("academicYear") : "";
                    fetchAndShowTimetable(selectedTimetableObj);
                }
                @Override public void onNothingSelected(AdapterView<?> parent) { clearTimetableUI(); }
            });
            if (!timetableBatches.isEmpty()) batchSpinner.setSelection(0);
        });
    }

    private void fetchAndShowTimetable(ParseObject timetable) {
        ParseQuery<ParseObject> configQuery = ParseQuery.getQuery("TimetableConfig");
        configQuery.whereEqualTo("user", ParseUser.getCurrentUser());
        configQuery.orderByDescending("createdAt");
        configQuery.setLimit(1);
        configQuery.include("breaks");
        configQuery.include("periods");
        configQuery.getFirstInBackground((config, configErr) -> {
            if (config == null || configErr != null) { clearTimetableUI(); return; }
            workingDays = config.getList("workingDays");
            periods = config.getList("periods");
            breaks = config.getList("breaks");
            int numDays = workingDays.size(), numPeriods = periods.size() + (breaks != null ? breaks.size() : 0);

            ParseQuery<ParseObject> entryQuery = ParseQuery.getQuery("TimetableEntry");
            entryQuery.whereEqualTo("timetable", timetable);
            entryQuery.findInBackground((entries, err) -> {
                backendEntryMap.clear();
                if (entries != null) for (ParseObject entry : entries) {
                    int dayIdx = workingDays.indexOf(entry.getString("day"));
                    int periodIdx = entry.getInt("period") - 1;
                    int colIdx = getPeriodColIndex(periodIdx, breaks);
                    backendEntryMap.put(dayIdx + "_" + colIdx, entry);
                }
                renderTimetable(tableLayout, workingDays, periods, breaks, backendEntryMap);
            });
        });
    }

    private void renderTimetable(
            TableLayout tableLayout, List<String> workingDays, List<ParseObject> periods, List<ParseObject> breaks,
            Map<String, ParseObject> entryMap
    ) {
        tableLayout.removeAllViews();
        infoLayout.removeAllViews();
        String info = "Batch: " + selectedBatch;
        if (!selectedSection.isEmpty()) info += " | Section: " + selectedSection;
        if (!selectedAcademicYear.isEmpty()) info += " | Academic Year: " + selectedAcademicYear;
        TextView infoText = new TextView(this);
        infoText.setText(info);
        infoText.setTextSize(20);
        infoText.setGravity(Gravity.CENTER);
        infoLayout.addView(infoText);
        int numPeriods = periods.size() + (breaks != null ? breaks.size() : 0);
        TableRow headerRow = new TableRow(this);
        headerRow.addView(createHeaderCell(""));

        int periodIdx, breakIdx;
        periodIdx = breakIdx = 0;
        for (int col = 0; col < numPeriods; col++) {
            boolean isBreak = isBreakCol(col, breaks);
            if (isBreak) {
                ParseObject br = breaks.get(breakIdx++);
                headerRow.addView(createBreakHeaderCell("Break\n" + br.getString("startTime") + "-" + br.getString("endTime")));
            } else {
                ParseObject period = periods.get(periodIdx++);
                headerRow.addView(createHeaderCell(period.getString("startTime") + "-" + period.getString("endTime")));
            }
        }
        tableLayout.addView(headerRow);

        for (int day = 0; day < workingDays.size(); day++) {
            TableRow row = new TableRow(this);
            row.addView(createDayCell(workingDays.get(day)));
            periodIdx = breakIdx = 0;
            for (int col = 0; col < numPeriods; col++) {
                boolean isBreak = isBreakCol(col, breaks);
                if (isBreak) {
                    ParseObject br = breaks.get(breakIdx++);
                    row.addView(createBreakCell(br.getString("startTime") + "-" + br.getString("endTime")));
                } else {
                    String key = day + "_" + col;
                    ParseObject entry = entryMap.get(key);
                    String objectId = (entry != null) ? entry.getObjectId() : null;
                    String subject = (entry != null) ? entry.getString("subject") : "";
                    String teacher = (entry != null) ? entry.getString("teacher") : "";
                    boolean isLab = (entry != null && entry.has("isLab") && entry.getBoolean("isLab"));
                    if (manualEdits.containsKey(key)) {
                        subject = manualEdits.get(key).getSubject();
                        teacher = manualEdits.get(key).getTeacher();
                        isLab = manualEdits.get(key).isLab();
                    }
                    final int finalDay = day, finalPeriod = periodIdx, finalCol = col;
                    final String finalObjectId = objectId, finalSubject = subject, finalTeacher = teacher;
                    final boolean finalIsLab = isLab;
                    if (finalIsLab && col + 1 < numPeriods && !isBreakCol(col + 1, breaks)) {
                        TableRow.LayoutParams params = new TableRow.LayoutParams();
                        params.span = 2;
                        AppCompatTextView labCell = new AppCompatTextView(this);
                        labCell.setLayoutParams(params);
                        labCell.setText(finalSubject.isEmpty() ? "—\n(—)" : finalSubject + "\n(" + finalTeacher + ") Lab");
                        labCell.setGravity(Gravity.CENTER);
                        labCell.setBackgroundResource(R.drawable.cell_box);
                        labCell.setTextSize(18);
                        labCell.setMinHeight(CELL_HEIGHT);
                        labCell.setMinWidth(CELL_WIDTH * 2);
                        labCell.setOnLongClickListener(v -> {
                            showEditDialog(finalDay, finalPeriod, finalCol, finalSubject, finalTeacher, true, finalObjectId);
                            return true;
                        });
                        row.addView(labCell);
                        col++;
                    } else {
                        AppCompatTextView classCell = createClassCell(finalSubject, finalTeacher);
                        classCell.setOnLongClickListener(v -> {
                            showEditDialog(finalDay, finalPeriod, finalCol, finalSubject, finalTeacher, false, finalObjectId);
                            return true;
                        });
                        row.addView(classCell);
                    }
                }
                periodIdx++;
            }
            tableLayout.addView(row);
        }
    }

    private void showEditDialog(
            int dayIdx, int periodIdx, int colIdx, String subject, String teacher, boolean isLab, String objectId
    ) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Timetable Entry");
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_timetable, null);
        EditText etSubject = view.findViewById(R.id.etSubject);
        EditText etTeacher = view.findViewById(R.id.etTeacher);
        CheckBox cbLab = view.findViewById(R.id.cbLab);
        etSubject.setText(subject); etTeacher.setText(teacher); cbLab.setChecked(isLab);
        builder.setView(view);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String newSubject = etSubject.getText().toString().trim(), newTeacher = etTeacher.getText().toString().trim();
            boolean newIsLab = cbLab.isChecked();
            String key = dayIdx + "_" + colIdx;
            manualEdits.put(key, new TimetableEdit(objectId, newSubject, newTeacher, newIsLab,
                    dayIdx, periodIdx, colIdx, selectedBatch, selectedSection, selectedAcademicYear));
            renderTimetable(tableLayout, workingDays, periods, breaks, backendEntryMap); // Refresh for instant view
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void saveManualEdits() {
        if (manualEdits.isEmpty()) {
            Toast.makeText(this, "No changes to save.", Toast.LENGTH_SHORT).show();
            return;
        }
        List<String> toDeleteIds = new ArrayList<>();
        for (TimetableEdit edit : manualEdits.values()) {
            if (edit.getObjectId() != null && edit.getSubject().isEmpty() && edit.getTeacher().isEmpty()) {
                toDeleteIds.add(edit.getObjectId());
            }
        }
        List<String> toUpdateIds = new ArrayList<>();
        Map<String, TimetableEdit> toUpdateEdits = new HashMap<>();
        for (TimetableEdit edit : manualEdits.values()) {
            if (edit.getObjectId() != null && (!edit.getSubject().isEmpty() || !edit.getTeacher().isEmpty())) {
                toUpdateIds.add(edit.getObjectId());
                toUpdateEdits.put(edit.getObjectId(), edit);
            }
        }
        List<TimetableEdit> toCreate = new ArrayList<>();
        for (TimetableEdit edit : manualEdits.values()) {
            if (edit.getObjectId() == null && (!edit.getSubject().isEmpty() || !edit.getTeacher().isEmpty())) {
                toCreate.add(edit);
            }
        }
        // --- 1. Delete ---
        if (!toDeleteIds.isEmpty()) {
            ParseQuery<ParseObject> deleteQuery = ParseQuery.getQuery("TimetableEntry");
            deleteQuery.whereContainedIn("objectId", toDeleteIds);
            deleteQuery.findInBackground((objects, e) -> {
                if (e == null && objects != null) ParseObject.deleteAllInBackground(objects);
            });
        }
        // --- 2. Update ---
        if (!toUpdateIds.isEmpty()) {
            ParseQuery<ParseObject> updateQuery = ParseQuery.getQuery("TimetableEntry");
            updateQuery.whereContainedIn("objectId", toUpdateIds);
            updateQuery.findInBackground((objects, e) -> {
                if (e != null || objects == null) return;
                List<ParseObject> toSave = new ArrayList<>();
                for (ParseObject entry : objects) {
                    TimetableEdit edit = toUpdateEdits.get(entry.getObjectId());
                    if (edit != null) {
                        entry.put("subject", edit.getSubject());
                        entry.put("teacher", edit.getTeacher());
                        entry.put("isLab", edit.isLab());
                        toSave.add(entry);
                    }
                }
                ParseObject.saveAllInBackground(toSave);
            });
        }
        // --- 3. Create ---
        if (!toCreate.isEmpty()) {
            for (TimetableEdit edit : toCreate) {
                ParseObject newEntry = new ParseObject("TimetableEntry");
                newEntry.put("subject", edit.getSubject());
                newEntry.put("teacher", edit.getTeacher());
                newEntry.put("isLab", edit.isLab());
                newEntry.put("day", workingDays.get(edit.getDayIdx()));
                newEntry.put("period", edit.getPeriodIdx() + 1); // 1-based
                newEntry.put("timetable", ParseObject.createWithoutData("GeneratedTimetable", selectedTimetableObj.getObjectId()));
                newEntry.put("batch", edit.getBatchName());
                if (!edit.getSection().isEmpty()) newEntry.put("section", edit.getSection());
                if (!edit.getAcademicYear().isEmpty()) newEntry.put("academicYear", edit.getAcademicYear());
                newEntry.saveInBackground();
            }
        }
        manualEdits.clear();
        fetchAndShowTimetable(selectedTimetableObj);
        Toast.makeText(this, "All changes saved & timetable reloaded.", Toast.LENGTH_SHORT).show();
    }

    private void clearTimetableUI() { tableLayout.removeAllViews(); infoLayout.removeAllViews(); }
    private int getPeriodColIndex(int periodIdx, List<ParseObject> breaks) {
        if (breaks == null) return periodIdx;
        int offset = 0;
        for (ParseObject br : breaks) {
            int breakAfter = br.getInt("breakAfterPeriod");
            if (periodIdx >= breakAfter) offset++;
        }
        return periodIdx + offset;
    }
    private boolean isBreakCol(int colIdx, List<ParseObject> breaks) {
        if (breaks == null) return false;
        int runningCol = 0;
        for (ParseObject br : breaks) {
            int breakAfter = br.getInt("breakAfterPeriod");
            int breakCol = breakAfter + runningCol;
            if (colIdx == breakCol) return true;
            runningCol++;
        }
        return false;
    }
    private AppCompatTextView createHeaderCell(String text) {
        AppCompatTextView tv = new AppCompatTextView(this);
        tv.setText(text); tv.setPadding(12,16,12,16); tv.setTextSize(16); tv.setGravity(Gravity.CENTER);
        tv.setBackgroundColor(ContextCompat.getColor(this, R.color.header_background));
        tv.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        tv.setMinWidth(CELL_WIDTH); tv.setMinHeight(CELL_HEIGHT);
        tv.setBackgroundResource(R.drawable.cell_box); return tv;
    }
    private AppCompatTextView createBreakHeaderCell(String text) {
        AppCompatTextView tv = createHeaderCell(text);
        tv.setBackgroundColor(ContextCompat.getColor(this, R.color.break_background)); return tv;
    }
    private AppCompatTextView createDayCell(String dayName) {
        AppCompatTextView tv = new AppCompatTextView(this);
        tv.setText(dayName); tv.setPadding(16,20,16,20); tv.setTextSize(16); tv.setGravity(Gravity.CENTER);
        tv.setBackgroundColor(ContextCompat.getColor(this, R.color.day_background));
        tv.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        tv.setMinWidth(CELL_WIDTH); tv.setMinHeight(CELL_HEIGHT);
        tv.setBackgroundResource(R.drawable.cell_box); return tv;
    }
    private AppCompatTextView createClassCell(String subject, String teacher) {
        AppCompatTextView tv = new AppCompatTextView(this);
        tv.setMinHeight(CELL_HEIGHT); tv.setMinWidth(CELL_WIDTH); tv.setPadding(16,16,16,16);
        tv.setGravity(Gravity.CENTER); tv.setBackgroundResource(R.drawable.cell_box); tv.setTextSize(18);
        if (subject.isEmpty() && teacher.isEmpty()) { tv.setText("—"); }
        else if (!subject.isEmpty() && !teacher.isEmpty()) { tv.setText(subject + "\n(" + teacher + ")"); }
        else if (!subject.isEmpty()) { tv.setText(subject); }
        else { tv.setText("—"); }
        return tv;
    }
    private AppCompatTextView createBreakCell(String label) {
        AppCompatTextView tv = new AppCompatTextView(this);
        tv.setText("Break\n" + label); tv.setMinHeight(CELL_HEIGHT); tv.setMinWidth(CELL_WIDTH);
        tv.setGravity(Gravity.CENTER); tv.setTextSize(16);
        tv.setBackgroundColor(ContextCompat.getColor(this, R.color.break_background));
        tv.setBackgroundResource(R.drawable.cell_box); return tv;
    }
    private void printTimetable() {
        PrintManager pm = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        pm.print("Timetable_" + selectedBatch, new TimetablePrintDocumentAdapter(this, tableLayout, selectedBatch, selectedSection, selectedAcademicYear), null);
    }
}
