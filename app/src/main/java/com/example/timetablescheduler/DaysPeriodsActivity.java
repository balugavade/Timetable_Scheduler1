package com.example.timetablescheduler;

import android.os.Bundle;
import android.text.InputType;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.parse.ParseObject;
import com.parse.ParseUser;
import java.util.ArrayList;
import java.util.List;

public class DaysPeriodsActivity extends AppCompatActivity {

    private EditText etPeriodsPerDay, etDaysPerWeek;
    private Button btnGenerateRows, btnSaveTimetable;
    private LinearLayout layoutPeriodsContainer;

    // Breaks
    private EditText etNumBreaks;
    private Button btnGenerateBreakRows;
    private LinearLayout layoutBreaksContainer;

    // Dynamic lists
    private List<EditText> startEditTexts = new ArrayList<>();
    private List<EditText> endEditTexts = new ArrayList<>();
    private List<EditText> breakAfterEditTexts = new ArrayList<>();
    private List<EditText> breakStartEditTexts = new ArrayList<>();
    private List<EditText> breakEndEditTexts = new ArrayList<>();

    private CheckBox cbMonday, cbTuesday, cbWednesday, cbThursday,
            cbFriday, cbSaturday, cbSunday;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_days_periods);

        etPeriodsPerDay = findViewById(R.id.etPeriodsPerDay);
        etDaysPerWeek = findViewById(R.id.etDaysPerWeek);
        btnGenerateRows = findViewById(R.id.btnGenerateRows);
        btnSaveTimetable = findViewById(R.id.btnSaveTimetable);
        layoutPeriodsContainer = findViewById(R.id.layoutPeriodsContainer);

        // Breaks
        etNumBreaks = findViewById(R.id.etNumBreaks);
        btnGenerateBreakRows = findViewById(R.id.btnGenerateBreakRows);
        layoutBreaksContainer = findViewById(R.id.layoutBreaksContainer);

        cbMonday = findViewById(R.id.cbMonday);
        cbTuesday = findViewById(R.id.cbTuesday);
        cbWednesday = findViewById(R.id.cbWednesday);
        cbThursday = findViewById(R.id.cbThursday);
        cbFriday = findViewById(R.id.cbFriday);
        cbSaturday = findViewById(R.id.cbSaturday);
        cbSunday = findViewById(R.id.cbSunday);

        btnGenerateRows.setOnClickListener(v -> generatePeriodRows());
        btnGenerateBreakRows.setOnClickListener(v -> generateBreakRows());
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
            etStart.setHint("Start: hh:mm");
            etStart.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);

            EditText etEnd = new EditText(this);
            etEnd.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            etEnd.setHint("End: hh:mm");
            etEnd.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);

            startEditTexts.add(etStart);
            endEditTexts.add(etEnd);

            row.addView(etStart);
            row.addView(etEnd);

            layoutPeriodsContainer.addView(row);
        }
    }

    private void generateBreakRows() {
        layoutBreaksContainer.removeAllViews();
        breakAfterEditTexts.clear();
        breakStartEditTexts.clear();
        breakEndEditTexts.clear();

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
            etBreakAfter.setInputType(InputType.TYPE_CLASS_NUMBER);

            EditText etBreakStart = new EditText(this);
            etBreakStart.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            etBreakStart.setHint("Break Start: hh:mm");
            etBreakStart.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);

            EditText etBreakEnd = new EditText(this);
            etBreakEnd.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            etBreakEnd.setHint("Break End: hh:mm");
            etBreakEnd.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);

            breakAfterEditTexts.add(etBreakAfter);
            breakStartEditTexts.add(etBreakStart);
            breakEndEditTexts.add(etBreakEnd);

            row.addView(etBreakAfter);
            row.addView(etBreakStart);
            row.addView(etBreakEnd);

            layoutBreaksContainer.addView(row);
        }
    }

    private void saveTimetableToBack4App() {
        try {
            int periodsPerDay = Integer.parseInt(etPeriodsPerDay.getText().toString().trim());
            int daysPerWeek = Integer.parseInt(etDaysPerWeek.getText().toString().trim());

            // Period times
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

            // Breaks
            List<Integer> breakAfterPeriods = new ArrayList<>();
            List<String> breakStarts = new ArrayList<>();
            List<String> breakEnds = new ArrayList<>();
            for (int i = 0; i < breakAfterEditTexts.size(); i++) {
                String afterStr = breakAfterEditTexts.get(i).getText().toString().trim();
                String startStr = breakStartEditTexts.get(i).getText().toString().trim();
                String endStr = breakEndEditTexts.get(i).getText().toString().trim();
                if (!afterStr.isEmpty() && !startStr.isEmpty() && !endStr.isEmpty()) {
                    breakAfterPeriods.add(Integer.parseInt(afterStr));
                    breakStarts.add(startStr);
                    breakEnds.add(endStr);
                } else {
                    Toast.makeText(this, "Fill all break fields", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // Days
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
            timetable.put("daysPerWeek", daysPerWeek);
            timetable.put("startTimes", startTimes);
            timetable.put("endTimes", endTimes);
            timetable.put("breakAfterPeriods", breakAfterPeriods);
            timetable.put("breakStarts", breakStarts);
            timetable.put("breakEnds", breakEnds);
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
