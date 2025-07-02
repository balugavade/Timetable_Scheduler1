package com.example.timetablescheduler.data;

import com.example.timetablescheduler.models.*;
import com.parse.*;
import java.util.*;

public class ParseRepository {

    // Save timetable and entries, using day names from workingDays list
    public void saveTimetable(Individual solution, List<String> workingDays) throws ParseException {
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
                entries.add(entry);
            }
        }

        timetable.save();
        ParseObject.saveAll(entries);
    }
}
