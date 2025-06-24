package com.example.timetablescheduler;

import android.content.Intent;
import android.os.Bundle;
import android.app.TimePickerDialog;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.parse.*;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;

public class DaysPeriodsActivity extends AppCompatActivity {

    private TextInputEditText etPeriodsPerDay, etNumBreaks, etDaysPerWeek;
    private Button btnGenerateRows, btnGenerateBreakRows, btnSave, btnNext;
    private LinearLayout layoutPeriodsContainer, layoutBreaksContainer;
    private CheckBox cbMonday, cbTuesday, cbWednesday, cbThursday, cbFriday, cbSaturday, cbSunday;

    private List<View> periodRows = new ArrayList<>();
    private List<View> breakRows = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_days_periods);

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        etPeriodsPerDay = findViewById(R.id.etPeriodsPerDay);
        etNumBreaks = findViewById(R.id.etNumBreaks);
        etDaysPerWeek = findViewById(R.id.etDaysPerWeek);
        btnGenerateRows = findViewById(R.id.btnGenerateRows);
        btnGenerateBreakRows = findViewById(R.id.btnGenerateBreakRows);
        btnSave = findViewById(R.id.btnSave);
        btnNext = findViewById(R.id.btnNext);
        layoutPeriodsContainer = findViewById(R.id.layoutPeriodsContainer);
        layoutBreaksContainer = findViewById(R.id.layoutBreaksContainer);

        cbMonday = findViewById(R.id.cbMonday);
        cbTuesday = findViewById(R.id.cbTuesday);
        cbWednesday = findViewById(R.id.cbWednesday);
        cbThursday = findViewById(R.id.cbThursday);
        cbFriday = findViewById(R.id.cbFriday);
        cbSaturday = findViewById(R.id.cbSaturday);
        cbSunday = findViewById(R.id.cbSunday);
    }

    private void setupListeners() {
        btnGenerateRows.setOnClickListener(v -> generatePeriodRows());
        btnGenerateBreakRows.setOnClickListener(v -> generateBreakRows());
        btnSave.setOnClickListener(v -> saveTimetableConfig());
        btnNext.setOnClickListener(v -> navigateToBatchActivity());

        etDaysPerWeek.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                updateDayCheckboxes();
            }
        });
    }

    private void generatePeriodRows() {
        String periodsText = etPeriodsPerDay.getText().toString().trim();
        if (periodsText.isEmpty()) {
            showToast("Please enter number of periods");
            return;
        }

        int periods = Integer.parseInt(periodsText);
        layoutPeriodsContainer.removeAllViews();
        periodRows.clear();

        for (int i = 1; i <= periods; i++) {
            View periodRow = createPeriodRow(i);
            layoutPeriodsContainer.addView(periodRow);
            periodRows.add(periodRow);
        }
    }

    private View createPeriodRow(int periodNumber) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 8);

        TextView tvPeriod = new TextView(this);
        tvPeriod.setText("Period " + periodNumber + ":");
        tvPeriod.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        EditText etStartTime = new EditText(this);
        etStartTime.setHint("Start Time");
        etStartTime.setFocusable(false);
        etStartTime.setClickable(true);
        etStartTime.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        etStartTime.setOnClickListener(v -> showTimePicker(etStartTime, true));

        EditText etEndTime = new EditText(this);
        etEndTime.setHint("End Time");
        etEndTime.setFocusable(false);
        etEndTime.setClickable(true);
        etEndTime.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        etEndTime.setOnClickListener(v -> showTimePicker(etEndTime, false));

        row.addView(tvPeriod);
        row.addView(etStartTime);
        row.addView(etEndTime);

        return row;
    }

    private void generateBreakRows() {
        String breaksText = etNumBreaks.getText().toString().trim();
        if (breaksText.isEmpty()) {
            showToast("Please enter number of breaks");
            return;
        }
        int breaks = Integer.parseInt(breaksText);
        layoutBreaksContainer.removeAllViews();
        breakRows.clear();

        for (int i = 1; i <= breaks; i++) {
            View breakRow = createBreakRow(i);
            layoutBreaksContainer.addView(breakRow);
            breakRows.add(breakRow);
        }
    }

    private View createBreakRow(int breakNumber) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 8);

        EditText etBreakAfter = new EditText(this);
        etBreakAfter.setHint("Break after period");
        etBreakAfter.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etBreakAfter.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        EditText etBreakStart = new EditText(this);
        etBreakStart.setHint("Break start");
        etBreakStart.setFocusable(false);
        etBreakStart.setClickable(true);
        etBreakStart.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        etBreakStart.setOnClickListener(v -> showTimePicker(etBreakStart, false));

        EditText etBreakEnd = new EditText(this);
        etBreakEnd.setHint("Break end");
        etBreakEnd.setFocusable(false);
        etBreakEnd.setClickable(true);
        etBreakEnd.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        etBreakEnd.setOnClickListener(v -> showTimePicker(etBreakEnd, false));

        row.addView(etBreakAfter);
        row.addView(etBreakStart);
        row.addView(etBreakEnd);

        return row;
    }

    // 12-hour format time picker with AM/PM
    private void showTimePicker(EditText editText, boolean autoFillEndTime) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, selectedHour, selectedMinute) -> {
                    String time = formatTime12Hour(selectedHour, selectedMinute);
                    editText.setText(time);

                    if (autoFillEndTime) {
                        // Auto-fill end time (1 hour later)
                        LinearLayout parent = (LinearLayout) editText.getParent();
                        EditText etEndTime = (EditText) parent.getChildAt(2);
                        int endHour = (selectedHour + 1) % 24;
                        String endTime = formatTime12Hour(endHour, selectedMinute);
                        etEndTime.setText(endTime);
                    }
                }, hour, minute, false); // false for 12-hour format with AM/PM

        timePickerDialog.show();
    }

    // Helper to format time in 12-hour format with AM/PM
    private String formatTime12Hour(int hourOfDay, int minute) {
        String amPm = (hourOfDay >= 12) ? "PM" : "AM";
        int hour = hourOfDay % 12;
        if (hour == 0) hour = 12;
        return String.format("%02d:%02d %s", hour, minute, amPm);
    }

    private void updateDayCheckboxes() {
        String daysText = etDaysPerWeek.getText().toString().trim();
        if (daysText.isEmpty()) return;

        int daysCount = Integer.parseInt(daysText);
        CheckBox[] checkboxes = {cbMonday, cbTuesday, cbWednesday, cbThursday, cbFriday, cbSaturday, cbSunday};

        for (int i = 0; i < checkboxes.length; i++) {
            if (i < daysCount) {
                checkboxes[i].setEnabled(true);
                checkboxes[i].setChecked(true);
            } else {
                checkboxes[i].setEnabled(false);
                checkboxes[i].setChecked(false);
            }
        }
    }

    private void saveTimetableConfig() {
        if (!validateInputs()) return;

        ParseObject timetableConfig = new ParseObject("TimetableConfig");
        timetableConfig.put("user", ParseUser.getCurrentUser());
        timetableConfig.put("periodsPerDay", Integer.parseInt(etPeriodsPerDay.getText().toString()));
        timetableConfig.put("breaksPerDay", Integer.parseInt(etNumBreaks.getText().toString()));
        timetableConfig.put("daysPerWeek", Integer.parseInt(etDaysPerWeek.getText().toString()));

        // Save period timings
        List<ParseObject> periods = new ArrayList<>();
        for (int i = 0; i < periodRows.size(); i++) {
            LinearLayout row = (LinearLayout) periodRows.get(i);
            EditText etStart = (EditText) row.getChildAt(1);
            EditText etEnd = (EditText) row.getChildAt(2);

            ParseObject period = new ParseObject("Period");
            period.put("periodNumber", i + 1);
            period.put("startTime", etStart.getText().toString());
            period.put("endTime", etEnd.getText().toString());
            periods.add(period);
        }
        timetableConfig.put("periods", periods);

        // Save break timings
        List<ParseObject> breaks = new ArrayList<>();
        for (int i = 0; i < breakRows.size(); i++) {
            LinearLayout row = (LinearLayout) breakRows.get(i);
            EditText etAfter = (EditText) row.getChildAt(0);
            EditText etStart = (EditText) row.getChildAt(1);
            EditText etEnd = (EditText) row.getChildAt(2);

            ParseObject breakObj = new ParseObject("Break");
            breakObj.put("breakAfterPeriod", Integer.parseInt(etAfter.getText().toString()));
            breakObj.put("startTime", etStart.getText().toString());
            breakObj.put("endTime", etEnd.getText().toString());
            breaks.add(breakObj);
        }
        timetableConfig.put("breaks", breaks);

        // Save selected days
        List<String> selectedDays = new ArrayList<>();
        if (cbMonday.isChecked()) selectedDays.add("Monday");
        if (cbTuesday.isChecked()) selectedDays.add("Tuesday");
        if (cbWednesday.isChecked()) selectedDays.add("Wednesday");
        if (cbThursday.isChecked()) selectedDays.add("Thursday");
        if (cbFriday.isChecked()) selectedDays.add("Friday");
        if (cbSaturday.isChecked()) selectedDays.add("Saturday");
        if (cbSunday.isChecked()) selectedDays.add("Sunday");
        timetableConfig.put("workingDays", selectedDays);

        timetableConfig.saveInBackground(e -> {
            if (e == null) {
                showToast("Configuration saved successfully!");
            } else {
                showToast("Error saving configuration: " + e.getMessage());
            }
        });
    }

    private boolean validateInputs() {
        if (etPeriodsPerDay.getText().toString().trim().isEmpty()) {
            showToast("Please enter number of periods per day");
            return false;
        }
        if (etNumBreaks.getText().toString().trim().isEmpty()) {
            showToast("Please enter number of breaks per day");
            return false;
        }
        if (etDaysPerWeek.getText().toString().trim().isEmpty()) {
            showToast("Please enter number of days per week");
            return false;
        }
        if (periodRows.isEmpty()) {
            showToast("Please generate period rows first");
            return false;
        }
        return true;
    }

    private void navigateToBatchActivity() {
        saveTimetableConfig();
        startActivity(new Intent(this, BatchActivity.class));
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
