package com.example.timetablescheduler;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.parse.ParseObject;
import com.parse.ParseUser;
import java.util.ArrayList;
import java.util.List;

public class DaysPeriodsActivity extends AppCompatActivity {

    private EditText etPeriodsPerDay, etBreakAfter, etDaysPerWeek;
    private Button btnGenerateRows, btnSaveTimetable;
    private LinearLayout layoutPeriodsContainer;
    private CheckBox cbMonday, cbTuesday, cbWednesday, cbThursday,
            cbFriday, cbSaturday, cbSunday;

    // For dynamic access
    private List<EditText> startEditTexts = new ArrayList<>();
    private List<EditText> endEditTexts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_days_periods);

        etPeriodsPerDay = findViewById(R.id.etPeriodsPerDay);
        etBreakAfter = findViewById(R.id.etBreakAfter);
        //etDaysPerWeek = findViewById(R.id.etDaysPerWeek);
        btnGenerateRows = findViewById(R.id.btnGenerateRows);
        btnSaveTimetable = findViewById(R.id.btnSaveTimetable);
        layoutPeriodsContainer = findViewById(R.id.layoutPeriodsContainer);

        cbMonday = findViewById(R.id.cbMonday);
        cbTuesday = findViewById(R.id.cbTuesday);
        cbWednesday = findViewById(R.id.cbWednesday);
        cbThursday = findViewById(R.id.cbThursday);
        cbFriday = findViewById(R.id.cbFriday);
        cbSaturday = findViewById(R.id.cbSaturday);
        cbSunday = findViewById(R.id.cbSunday);

        btnGenerateRows.setOnClickListener(v -> generatePeriodRows());
        btnSaveTimetable.setOnClickListener(v -> saveTimetableToBack4App());
    }

    private void generatePeriodRows() {
        layoutPeriodsContainer.removeAllViews();
        startEditTexts.clear();
        endEditTexts.clear();

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
            etStart.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            etStart.setHint("Start: hh:mm AM/PM");
            etStart.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);

            EditText etEnd = new EditText(this);
            etEnd.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            etEnd.setHint("End: hh:mm AM/PM");
            etEnd.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);

            startEditTexts.add(etStart);
            endEditTexts.add(etEnd);

            row.addView(etStart);
            row.addView(etEnd);

            layoutPeriodsContainer.addView(row);
        }
    }

    private void saveTimetableToBack4App() {
        try {
            int periodsPerDay = Integer.parseInt(etPeriodsPerDay.getText().toString().trim());
            int breakAfter = etBreakAfter.getText().toString().trim().isEmpty() ? 0 :
                    Integer.parseInt(etBreakAfter.getText().toString().trim());
            int daysPerWeek = Integer.parseInt(etDaysPerWeek.getText().toString().trim());

            // Collect period times
            List<String> startTimes = new ArrayList<>();
            List<String> endTimes = new ArrayList<>();
            for (int i = 0; i < startEditTexts.size(); i++) {
                String start = startEditTexts.get(i).getText().toString().trim();
                String end = endEditTexts.get(i).getText().toString().trim();
                if (!start.isEmpty() && !end.isEmpty()) {
                    startTimes.add(start);
                    endTimes.add(end);
                } else {
                    Toast.makeText(this, "Fill all start/end times", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            if (startTimes.isEmpty() || endTimes.isEmpty()) {
                Toast.makeText(this, "Please enter at least one period time.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Collect selected days
            List<String> selectedDays = new ArrayList<>();
            addDayIfChecked(selectedDays, cbMonday, "Monday");
            addDayIfChecked(selectedDays, cbTuesday, "Tuesday");
            addDayIfChecked(selectedDays, cbWednesday, "Wednesday");
            addDayIfChecked(selectedDays, cbThursday, "Thursday");
            addDayIfChecked(selectedDays, cbFriday, "Friday");
            addDayIfChecked(selectedDays, cbSaturday, "Saturday");
            addDayIfChecked(selectedDays, cbSunday, "Sunday");

            if (selectedDays.isEmpty()) {
                Toast.makeText(this, "Please select at least one day.", Toast.LENGTH_SHORT).show();
                return;
            }

            ParseObject timetable = new ParseObject("Timetable");
            timetable.put("user", ParseUser.getCurrentUser());
            timetable.put("periodsPerDay", periodsPerDay);
            timetable.put("breakAfter", breakAfter);
            timetable.put("daysPerWeek", daysPerWeek);
            timetable.put("startTimes", startTimes);
            timetable.put("endTimes", endTimes);
            timetable.put("selectedDays", selectedDays);

            timetable.saveInBackground(e -> {
                if (e == null) {
                    Toast.makeText(this, "Timetable saved!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Error saving timetable: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please fill all required numerical fields.", Toast.LENGTH_SHORT).show();
        }
    }

    private void addDayIfChecked(List<String> days, CheckBox checkBox, String dayName) {
        if (checkBox.isChecked()) {
            days.add(dayName);
        }
    }
}
