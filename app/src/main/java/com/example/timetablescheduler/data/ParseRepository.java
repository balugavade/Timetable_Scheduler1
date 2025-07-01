package com.example.timetablescheduler.data;

import com.example.timetablescheduler.models.*;
import com.parse.*;
import java.util.*;

public class ParseRepository {

    public void saveTimetable(Individual solution) throws ParseException {
        ParseObject timetable = new ParseObject("GeneratedTimetable");
        timetable.put("user", ParseUser.getCurrentUser());
        timetable.put("fitness", solution.getFitness());
        timetable.put("generatedAt", new Date());

        List<ParseObject> entries = new ArrayList<>();
        for (TimetableClass cls : solution.getClasses()) {
            // Handle multi-period classes (labs)
            for (int period = 0; period < cls.getDuration(); period++) {
                ParseObject entry = new ParseObject("TimetableEntry");
                entry.put("timetable", timetable);
                entry.put("subject", cls.getSubject());
                entry.put("teacher", cls.getTeacher());
                entry.put("batch", cls.getBatch());
                entry.put("section", cls.getSection());
                entry.put("day", cls.getTimeSlot().getDay());
                entry.put("period", cls.getTimeSlot().getPeriod() + period);
                entry.put("isLab", cls.isLab());
                entry.put("duration", cls.getDuration());
                entries.add(entry);
            }
        }

        timetable.save();
        ParseObject.saveAll(entries);
    }

    public List<TimetableEntry> fetchLatestTimetable() throws ParseException {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("GeneratedTimetable");
        query.whereEqualTo("user", ParseUser.getCurrentUser());
        query.orderByDescending("generatedAt");
        ParseObject timetable = query.getFirst();

        ParseQuery<ParseObject> entryQuery = ParseQuery.getQuery("TimetableEntry");
        entryQuery.whereEqualTo("timetable", timetable);
        List<ParseObject> entries = entryQuery.find();

        List<TimetableEntry> timetableEntries = new ArrayList<>();
        for (ParseObject entry : entries) {
            timetableEntries.add(new TimetableEntry(
                    entry.getString("subject"),
                    entry.getString("teacher"),
                    entry.getString("batch"),
                    entry.getString("section"),
                    entry.getInt("day"),
                    entry.getInt("period"),
                    entry.getBoolean("isLab")
            ));
        }
        return timetableEntries;
    }
}
