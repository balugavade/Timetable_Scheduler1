package com.example.timetablescheduler;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.parse.ParseObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DaysPeriodsActivity extends AppCompatActivity {

    private EditText etPeriodsPerDay, etDaysPerWeek, etNumBreaks;
    private Button btnGenerateRows, btnSave, btnGenerateBreakRows;
    private LinearLayout layoutPeriodsContainer, layoutBreaksContainer;
    private List<EditText> startTimeFields = new ArrayList<>();
    private List<EditText> endTimeFields = new ArrayList<>();
    // Breaks
    private List<EditText> breakAfterFields = new ArrayList<>();
    private List<EditText> breakStartFields = new ArrayList<>();
    private List<EditText> breakEndFields = new ArrayList<>();

    private CheckBox cbMonday, cbTuesday, cbWednesday, cbThursday, cbFriday, cbSaturday, cbSunday;
    private List<CheckBox> dayCheckBoxes = new ArrayList<>();
    private final String[] dayNames = {"Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_days_periods);

        etPeriodsPerDay = findViewById(R.id.etPeriodsPerDay);
        btnGenerateRows = findViewById(R.id.btnGenerateRows);
        layoutPeriodsContainer = findViewById(R.id.layoutPeriodsContainer);

        etNumBreaks = findViewById(R.id.etNumBreaks);
        btnGenerateBreakRows = findViewById(R.id.btnGenerateBreakRows);
        layoutBreaksContainer = findViewById(R.id.layoutBreaksContainer);

        etDaysPerWeek = findViewById(R.id.etDaysPerWeek);
        btnSave = findViewById(R.id.btnSave);

        cbMonday = findViewById(R.id.cbMonday);
        cbTuesday = findViewById(R.id.cbTuesday);
        cbWednesday = findViewById(R.id.cbWednesday);
        cbThursday = findViewById(R.id.cbThursday);
        cbFriday = findViewById(R.id.cbFriday);
        cbSaturday = findViewById(R.id.cbSaturday);
        cbSunday = findViewById(R.id.cbSunday);

        dayCheckBoxes.clear();
        dayCheckBoxes.add(cbMonday);
        dayCheckBoxes.add(cbTuesday);
        dayCheckBoxes.add(cbWednesday);
        dayCheckBoxes.add(cbThursday);
        dayCheckBoxes.add(cbFriday);
        dayCheckBoxes.add(cbSaturday);
        dayCheckBoxes.add(cbSunday);

        btnGenerateRows.setOnClickListener(v -> generatePeriodRows());
        btnGenerateBreakRows.setOnClickListener(v -> generateBreakRows());
        btnSave.setOnClickListener(v -> saveAllData());

        // Auto-select days when number is entered
        etDaysPerWeek.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) updateDaySelection();
        });
        etDaysPerWeek.setOnEditorActionListener((v, actionId, event) -> {
            updateDaySelection();
            return false;
        });
    }

    private void generatePeriodRows() {
        layoutPeriodsContainer.removeAllViews();
        startTimeFields.clear();
        endTimeFields.clear();

        String periodsStr = etPeriodsPerDay.getText().toString().trim();
        if (periodsStr.isEmpty()) {
            Toast.makeText(this, "Enter number of periods", Toast.LENGTH_SHORT).show();
            return;
        }

        int periods = Integer.parseInt(periodsStr);

        for (int i = 0; i < periods; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            row.setPadding(0, 8, 0, 8);

            EditText etStart = new EditText(this);
            etStart.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f));
            etStart.setHint("Start time");
            etStart.setFocusable(false);
            etStart.setClickable(true);

            EditText etEnd = new EditText(this);
            etEnd.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f));
            etEnd.setHint("End time");
            etEnd.setFocusable(false);
            etEnd.setClickable(true);

            etStart.setOnClickListener(v -> showStartTimePicker(etStart, etEnd));
            etEnd.setOnClickListener(v -> showEndTimePicker(etEnd));

            startTimeFields.add(etStart);
            endTimeFields.add(etEnd);
            row.addView(etStart);
            row.addView(etEnd);
            layoutPeriodsContainer.addView(row);
        }
    }

    private void generateBreakRows() {
        layoutBreaksContainer.removeAllViews();
        breakAfterFields.clear();
        breakStartFields.clear();
        breakEndFields.clear();

        String breaksStr = etNumBreaks.getText().toString().trim();
        if (breaksStr.isEmpty()) {
            Toast.makeText(this, "Enter number of breaks", Toast.LENGTH_SHORT).show();
            return;
        }

        int numBreaks = Integer.parseInt(breaksStr);

        for (int i = 0; i < numBreaks; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            row.setPadding(0, 8, 0, 8);

            EditText etBreakAfter = new EditText(this);
            etBreakAfter.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            etBreakAfter.setHint("Break after period");
            etBreakAfter.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

            EditText etBreakStart = new EditText(this);
            etBreakStart.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            etBreakStart.setHint("Break start");
            etBreakStart.setFocusable(false);
            etBreakStart.setClickable(true);

            EditText etBreakEnd = new EditText(this);
            etBreakEnd.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            etBreakEnd.setHint("Break end");
            etBreakEnd.setFocusable(false);
            etBreakEnd.setClickable(true);

            etBreakStart.setOnClickListener(v -> showTimePicker12Hour(etBreakStart));
            etBreakEnd.setOnClickListener(v -> showTimePicker12Hour(etBreakEnd));

            breakAfterFields.add(etBreakAfter);
            breakStartFields.add(etBreakStart);
            breakEndFields.add(etBreakEnd);

            row.addView(etBreakAfter);
            row.addView(etBreakStart);
            row.addView(etBreakEnd);

            layoutBreaksContainer.addView(row);
        }
    }

    private void showStartTimePicker(EditText etStart, EditText etEnd) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePicker = new TimePickerDialog(this,
                (view, selectedHour, selectedMinute) -> {
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.HOUR_OF_DAY, selectedHour);
                    cal.set(Calendar.MINUTE, selectedMinute);

                    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);
                    String startTime = sdf.format(cal.getTime());
                    etStart.setText(startTime);

                    // Set end time to one hour later
                    cal.add(Calendar.HOUR_OF_DAY, 1);
                    String endTime = sdf.format(cal.getTime());
                    etEnd.setText(endTime);
                }, hour, minute, false);

        timePicker.setTitle("Select Start Time");
        timePicker.show();
    }

    private void showEndTimePicker(EditText etEnd) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePicker = new TimePickerDialog(this,
                (view, selectedHour, selectedMinute) -> {
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.HOUR_OF_DAY, selectedHour);
                    cal.set(Calendar.MINUTE, selectedMinute);

                    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);
                    String endTime = sdf.format(cal.getTime());
                    etEnd.setText(endTime);
                }, hour, minute, false);

        timePicker.setTitle("Select End Time");
        timePicker.show();
    }

    private void showTimePicker12Hour(EditText et) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePicker = new TimePickerDialog(this,
                (view, selectedHour, selectedMinute) -> {
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.HOUR_OF_DAY, selectedHour);
                    cal.set(Calendar.MINUTE, selectedMinute);

                    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);
                    String time = sdf.format(cal.getTime());
                    et.setText(time);
                }, hour, minute, false);

        timePicker.setTitle("Select Time");
        timePicker.show();
    }

    private void updateDaySelection() {
        String daysStr = etDaysPerWeek.getText().toString().trim();
        int numDays = 0;
        if (!daysStr.isEmpty()) {
            try {
                numDays = Integer.parseInt(daysStr);
            } catch (NumberFormatException ignored) {}
        }
        for (int i = 0; i < dayCheckBoxes.size(); i++) {
            dayCheckBoxes.get(i).setChecked(i < numDays);
            dayCheckBoxes.get(i).setEnabled(i < numDays);
        }
    }

    private void saveAllData() {
        List<String> startTimes = new ArrayList<>();
        List<String> endTimes = new ArrayList<>();
        for (int i = 0; i < startTimeFields.size(); i++) {
            String start = startTimeFields.get(i).getText().toString();
            String end = endTimeFields.get(i).getText().toString();
            if (start.isEmpty() || end.isEmpty()) {
                Toast.makeText(this, "Please fill all start and end times.", Toast.LENGTH_SHORT).show();
                return;
            }
            startTimes.add(start);
            endTimes.add(end);
        }

        // Breaks
        List<Integer> breakAfterPeriods = new ArrayList<>();
        List<String> breakStarts = new ArrayList<>();
        List<String> breakEnds = new ArrayList<>();
        for (int i = 0; i < breakAfterFields.size(); i++) {
            String afterStr = breakAfterFields.get(i).getText().toString().trim();
            String startStr = breakStartFields.get(i).getText().toString().trim();
            String endStr = breakEndFields.get(i).getText().toString().trim();
            if (afterStr.isEmpty() || startStr.isEmpty() || endStr.isEmpty()) {
                Toast.makeText(this, "Please fill all break fields.", Toast.LENGTH_SHORT).show();
                return;
            }
            breakAfterPeriods.add(Integer.parseInt(afterStr));
            breakStarts.add(startStr);
            breakEnds.add(endStr);
        }

        String daysStr = etDaysPerWeek.getText().toString().trim();
        int daysPerWeek = daysStr.isEmpty() ? 0 : Integer.parseInt(daysStr);

        List<String> selectedDays = new ArrayList<>();
        for (int i = 0; i < dayCheckBoxes.size(); i++) {
            if (dayCheckBoxes.get(i).isChecked()) {
                selectedDays.add(dayNames[i]);
            }
        }

        // Save to Parse/Back4App
        ParseObject timetable = new ParseObject("Timetable");
        timetable.put("periodsPerDay", startTimes.size());
        timetable.put("startTimes", startTimes);
        timetable.put("endTimes", endTimes);
        timetable.put("breakAfterPeriods", breakAfterPeriods);
        timetable.put("breakStarts", breakStarts);
        timetable.put("breakEnds", breakEnds);
        timetable.put("daysPerWeek", daysPerWeek);
        timetable.put("selectedDays", selectedDays);

        timetable.saveInBackground(e -> {
            if (e == null) {
                Toast.makeText(this, "Saved successfully!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(DaysPeriodsActivity.this, TeachersActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
