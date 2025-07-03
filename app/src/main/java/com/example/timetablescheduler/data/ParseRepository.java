package com.example.timetablescheduler.data;

import android.util.Log;
import com.example.timetablescheduler.models.*;
import com.parse.*;
import java.util.*;

public class ParseRepository {

    public void saveTimetable(Individual solution, List<String> workingDays) {
        try {
            Log.d("ParseRepository", "saveTimetable called with " + solution.getClasses().size() + " classes");
            ParseObject timetable = new ParseObject("GeneratedTimetable");
            timetable.put("user", ParseUser.getCurrentUser());
            timetable.put("fitness", solution.getFitness());
            timetable.put("generatedAt", new Date());

            List<ParseObject> entries = new ArrayList<>();
            for (TimetableClass cls : solution.getClasses()) {
                for (int period = 0; period < cls.getDuration(); period++) {
                    ParseObject entry = new ParseObject("TimetableEntry");
                    entry.put("timetable", timetable);
                    entry.put("subject", cls.getSubject());
                    entry.put("teacher", cls.getTeacher());
                    entry.put("batch", cls.getBatch());
                    entry.put("section", cls.getSection());
                    int dayIndex = cls.getTimeSlot().getDay();
                    entry.put("day", workingDays.get(dayIndex)); // Save as string!
                    entry.put("period", cls.getTimeSlot().getPeriod() + period);
                    entry.put("isLab", cls.isLab());
                    entry.put("duration", cls.getDuration());
                    Log.d("ParseRepository", "Preparing entry: subject=" + cls.getSubject() + ", teacher=" + cls.getTeacher() + ", day=" + workingDays.get(dayIndex));
                    entries.add(entry);
                }
            }

            timetable.save();
            ParseObject.saveAll(entries);
            Log.d("ParseRepository", "Saved " + entries.size() + " timetable entries.");
        } catch (Exception e) {
            Log.e("ParseRepository", "Error saving entries: " + e.getMessage(), e);
        }
    }
}
