package com.example.timetablescheduler.data;

import android.util.Log;
import com.example.timetablescheduler.models.*;
import com.parse.*;
import java.util.*;

public class ParseRepository {

    public void saveTimetableForBatch(Individual solution, List<String> workingDays, String batchName, String section, String academicYear) {
        try {
            Log.d("ParseRepository", "saveTimetableForBatch called for " + batchName + " with " + solution.getClasses().size() + " classes");
            ParseObject timetable = new ParseObject("GeneratedTimetable");
            timetable.put("user", ParseUser.getCurrentUser());
            timetable.put("batch", batchName);
            timetable.put("section", section);
            timetable.put("academicYear", academicYear);
            timetable.put("fitness", solution.getFitness());
            timetable.put("generatedAt", new Date());

            List<ParseObject> entries = new ArrayList<>();
            for (TimetableClass cls : solution.getClasses()) {
                for (int period = 0; period < cls.getDuration(); period++) {
                    ParseObject entry = new ParseObject("TimetableEntry");
                    entry.put("timetable", timetable);
                    entry.put("subject", cls.getSubject());
                    entry.put("teacher", cls.isLab() ? "Lab" : cls.getTeacher());
                    entry.put("batch", batchName);
                    entry.put("section", section);
                    entry.put("academicYear", academicYear);
                    int dayIndex = cls.getTimeSlot().getDay();
                    entry.put("day", workingDays.get(dayIndex));
                    entry.put("period", cls.getTimeSlot().getPeriod() + period);
                    entry.put("isLab", cls.isLab());
                    entry.put("duration", cls.getDuration());
                    entries.add(entry);
                }
            }

            timetable.save();
            ParseObject.saveAll(entries);
            Log.d("ParseRepository", "Saved " + entries.size() + " timetable entries for batch " + batchName);
        } catch (Exception e) {
            Log.e("ParseRepository", "Error saving entries: " + e.getMessage(), e);
        }
    }
}
