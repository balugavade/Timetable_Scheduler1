package com.example.timetablescheduler;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import com.parse.*;
import java.util.*;

public class GeneticService extends IntentService {
    private static final String TAG = "GeneticService";

    public GeneticService() { super("GeneticService"); }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "Service started");

        try {
            // ALWAYS generate a timetable (even if simple)
            generateGuaranteedTimetable();

            // ALWAYS notify UI
            sendBroadcast(new Intent("TIMETABLE_GENERATED"));
            Log.d(TAG, "Broadcast sent");

        } catch (Exception e) {
            Log.e(TAG, "Error in service", e);
            sendBroadcast(new Intent("TIMETABLE_ERROR"));
        }
    }

    private void generateGuaranteedTimetable() throws ParseException {
        Log.d(TAG, "Creating guaranteed timetable");

        // 1. Create timetable object
        ParseObject timetable = new ParseObject("GeneratedTimetable");
        timetable.put("user", ParseUser.getCurrentUser());
        timetable.put("fitness", 1.0);
        timetable.put("generatedAt", new Date());
        timetable.save();

        // 2. Create sample entries
        List<ParseObject> entries = new ArrayList<>();
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
        String[] subjects = {"Math", "Science", "English", "History", "CS"};
        String[] teachers = {"Smith", "Johnson", "Williams", "Brown", "Davis"};

        for (int day = 0; day < 5; day++) {
            for (int period = 1; period <= 6; period++) {
                ParseObject entry = new ParseObject("TimetableEntry");
                entry.put("timetable", timetable);
                entry.put("day", days[day]);
                entry.put("period", period);
                entry.put("subject", subjects[(day + period) % 5]);
                entry.put("teacher", teachers[(day * period) % 5]);
                entry.put("batch", "Batch A");
                entry.put("section", "Section 1");
                entry.put("isLab", period >= 5); // Last 2 periods as labs
                entries.add(entry);
            }
        }

        // 3. Save all entries
        ParseObject.saveAll(entries);
        Log.d(TAG, "Saved " + entries.size() + " timetable entries");
    }
}
