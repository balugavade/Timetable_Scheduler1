package com.example.timetablescheduler.data;

import android.util.Log;
import com.example.timetablescheduler.models.*;
import com.parse.*;
import java.util.*;

public class ParseRepository {

    public void saveTimetableForBatch(
            Individual solution,
            List<String> workingDays,
            String batchName,
            String section,
            String academicYear
    ) {
        try {
            Log.d("ParseRepository", "saveTimetableForBatch called for " + batchName + " with " + solution.getClasses().size() + " classes");

            ParseObject timetable = new ParseObject("GeneratedTimetable");
            timetable.put("user", ParseUser.getCurrentUser());
            timetable.put("batch", batchName);
            timetable.put("section", section);
            timetable.put("academicYear", academicYear);
            timetable.put("fitness", solution.getFitness());
            timetable.put("generatedAt", new Date());
            timetable.save(); // Save before assigning entries to it

            List<ParseObject> entries = new ArrayList<>();

            for (TimetableClass cls : solution.getClasses()) {
                String teacherName = cls.getTeacher();
                TimeSlot slot = cls.getTimeSlot();
                int dayIndex = slot.getDay();

                for (int period = 0; period < cls.getDuration(); period++) {
                    ParseObject entry = new ParseObject("TimetableEntry");
                    entry.put("timetable", timetable);
                    entry.put("user", ParseUser.getCurrentUser());
                    entry.put("subject", cls.getSubject());
                    entry.put("teacher", teacherName);
                    entry.put("batch", batchName);
                    entry.put("section", section);
                    entry.put("academicYear", academicYear);
                    entry.put("day", workingDays.get(dayIndex));
                    entry.put("period", slot.getPeriod() + period);
                    entry.put("isLab", cls.isLab());
                    entry.put("duration", cls.getDuration());
                    entry.put("startTime", slot.getStartTime());
                    entry.put("endTime", slot.getEndTime());
                    entries.add(entry);
                }
            }

            ParseObject.saveAll(entries);
            Log.d("ParseRepository", "Saved " + entries.size() + " timetable entries for batch " + batchName);
        } catch (Exception e) {
            Log.e("ParseRepository", "Error saving entries: " + e.getMessage(), e);
        }
    }
}
