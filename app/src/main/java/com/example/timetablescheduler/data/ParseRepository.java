package com.example.timetablescheduler.data;

import com.example.timetablescheduler.models.*;
import com.parse.*;
import java.util.*;

public class ParseRepository {

    /**
     * Save the generated timetable and its entries to Parse.
     * The 'day' field is saved as a string (e.g., "Monday") for correct display.
     */
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

    /**
     * Fetch the latest generated timetable entries for the current user.
     * Returns a list of TimetableEntry model objects.
     * Converts day string (e.g., "Monday") to int index using workingDays.
     */
    public List<TimetableEntry> fetchLatestTimetable(List<String> workingDays) throws ParseException {
        // Find the latest GeneratedTimetable for the user
        ParseQuery<ParseObject> query = ParseQuery.getQuery("GeneratedTimetable");
        query.whereEqualTo("user", ParseUser.getCurrentUser());
        query.orderByDescending("generatedAt");
        ParseObject timetable = query.getFirst();

        // Fetch all TimetableEntry objects for this timetable
        ParseQuery<ParseObject> entryQuery = ParseQuery.getQuery("TimetableEntry");
        entryQuery.whereEqualTo("timetable", timetable);
        List<ParseObject> entries = entryQuery.find();

        List<TimetableEntry> timetableEntries = new ArrayList<>();
        for (ParseObject entry : entries) {
            String dayStr = entry.getString("day");
            int dayIndex = workingDays.indexOf(dayStr); // Convert day string to index
            timetableEntries.add(new TimetableEntry(
                    entry.getString("subject"),
                    entry.getString("teacher"),
                    entry.getString("batch"),
                    entry.getString("section"),
                    dayIndex, // int day
                    entry.getInt("period"),
                    entry.getBoolean("isLab")
            ));
        }
        return timetableEntries;
    }

    /**
     * Fetch all teachers for the current user.
     */
    public List<Teacher> fetchTeachers() throws ParseException {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Teacher");
        query.whereEqualTo("user", ParseUser.getCurrentUser());
        List<ParseObject> results = query.find();

        List<Teacher> teachers = new ArrayList<>();
        for (ParseObject obj : results) {
            teachers.add(new Teacher(
                    obj.getString("name"),
                    obj.getString("position"),
                    obj.has("load") ? Integer.parseInt(obj.getString("load")) : 20,
                    obj.getString("department"),
                    Arrays.asList(obj.getString("subjects").split(","))
            ));
        }
        return teachers;
    }

    /**
     * Fetch all subjects for the current user.
     */
    public List<Subject> fetchSubjects() throws ParseException {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Subject");
        query.whereEqualTo("user", ParseUser.getCurrentUser());
        List<ParseObject> results = query.find();

        List<Subject> subjects = new ArrayList<>();
        for (ParseObject obj : results) {
            subjects.add(new Subject(
                    obj.getString("code"),
                    obj.getString("name"),
                    obj.has("isLab") && obj.getBoolean("isLab"),
                    obj.has("lecturesWeekly") ? obj.getInt("lecturesWeekly") : 0,
                    obj.has("labsWeekly") ? obj.getInt("labsWeekly") : 0,
                    obj.has("semester") ? obj.getInt("semester") : 1
            ));
        }
        return subjects;
    }

    /**
     * Fetch all batches for the current user.
     */
    public List<Batch> fetchBatches() throws ParseException {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Batch");
        query.whereEqualTo("user", ParseUser.getCurrentUser());
        List<ParseObject> results = query.find();

        List<Batch> batches = new ArrayList<>();
        for (ParseObject obj : results) {
            batches.add(new Batch(
                    obj.getString("name"),
                    obj.getString("department"),
                    obj.getString("academicYear"),
                    Arrays.asList("A", "B"), // Or fetch from Parse if you store sections
                    obj.getList("subjects")
            ));
        }
        return batches;
    }

    /**
     * Fetch the latest timetable configuration for the current user.
     */
    public ParseObject fetchLatestTimetableConfig() throws ParseException {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("TimetableConfig");
        query.whereEqualTo("user", ParseUser.getCurrentUser());
        query.orderByDescending("createdAt");
        return query.getFirst();
    }
}
