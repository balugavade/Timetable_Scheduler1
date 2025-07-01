package com.example.timetablescheduler;

import android.app.IntentService;
import android.content.Intent;
import com.example.timetablescheduler.algorithm.GeneticAlgorithm;
import com.example.timetablescheduler.models.*;
import com.parse.*;
import java.util.*;

public class GeneticService extends IntentService {
    public GeneticService() { super("GeneticService"); }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            // 1. Fetch all required data
            List<Teacher> teachers = fetchTeachers();
            List<Subject> subjects = fetchSubjects();
            List<Batch> batches = fetchBatches();
            TimetableConfig config = fetchTimetableConfig();

            // 2. Generate timetable classes from real data
            List<TimetableClass> classes = generateTimetableClasses(teachers, subjects, batches);

            // 3. Create time slots from configuration
            List<TimeSlot> timeSlots = generateTimeSlots(config);

            // 4. Prepare GA parameters
            Map<String, Integer> teacherLoads = new HashMap<>();
            Map<String, List<String>> teacherSubjects = new HashMap<>();
            List<String> teacherNames = new ArrayList<>();

            for (Teacher teacher : teachers) {
                teacherLoads.put(teacher.getName(), teacher.getMaxHours());
                teacherSubjects.put(teacher.getName(), teacher.getSubjects());
                teacherNames.add(teacher.getName());
            }

            // 5. Initialize and run GA
            GeneticAlgorithm ga = new GeneticAlgorithm(timeSlots, teacherLoads, teacherSubjects, teacherNames);
            Individual solution = ga.generateTimetable(classes);

            // 6. Save solution to Parse
            saveTimetableToDatabase(solution, config);

            // 7. Notify UI
            sendBroadcast(new Intent("TIMETABLE_GENERATED"));

        } catch (Exception e) {
            e.printStackTrace();
            sendBroadcast(new Intent("TIMETABLE_ERROR"));
        }
    }

    private List<Teacher> fetchTeachers() throws ParseException {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Teacher");
        query.whereEqualTo("user", ParseUser.getCurrentUser());
        List<ParseObject> results = query.find();

        List<Teacher> teachers = new ArrayList<>();
        for (ParseObject obj : results) {
            Teacher teacher = new Teacher();
            teacher.setName(obj.getString("name"));
            teacher.setMaxHours(obj.getInt("maxHours"));
            teacher.setSubjects(obj.getList("subjects"));
            teachers.add(teacher);
        }
        return teachers;
    }

    private List<Subject> fetchSubjects() throws ParseException {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Subject");
        query.whereEqualTo("user", ParseUser.getCurrentUser());
        List<ParseObject> results = query.find();

        List<Subject> subjects = new ArrayList<>();
        for (ParseObject obj : results) {
            Subject subject = new Subject();
            subject.setName(obj.getString("name"));
            subject.setLab(obj.getBoolean("isLab"));
            subject.setWeeklyLectures(obj.getInt("lecturesWeekly"));
            subject.setWeeklyLabs(obj.getInt("labsWeekly"));
            subjects.add(subject);
        }
        return subjects;
    }

    private List<Batch> fetchBatches() throws ParseException {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Batch");
        query.whereEqualTo("user", ParseUser.getCurrentUser());
        List<ParseObject> results = query.find();

        List<Batch> batches = new ArrayList<>();
        for (ParseObject obj : results) {
            Batch batch = new Batch();
            batch.setName(obj.getString("name"));
            batch.setDepartment(obj.getString("department"));
            batch.setAcademicYear(obj.getString("academicYear"));
            batch.setSections(obj.getList("sections"));
            batch.setSubjects(obj.getList("subjects"));
            batches.add(batch);
        }
        return batches;
    }

    private TimetableConfig fetchTimetableConfig() throws ParseException {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("TimetableConfig");
        query.whereEqualTo("user", ParseUser.getCurrentUser());
        ParseObject configObj = query.getFirst();

        TimetableConfig config = new TimetableConfig();
        config.setPeriodsPerDay(configObj.getInt("periodsPerDay"));
        config.setBreaksPerDay(configObj.getInt("breaksPerDay"));
        config.setWorkingDays(configObj.getList("workingDays"));
        config.setPeriods(configObj.getList("periods"));
        config.setBreaks(configObj.getList("breaks"));

        return config;
    }

    private List<TimeSlot> generateTimeSlots(TimetableConfig config) {
        List<TimeSlot> timeSlots = new ArrayList<>();
        List<String> workingDays = config.getWorkingDays();
        List<ParseObject> periods = config.getPeriods();

        for (int dayIndex = 0; dayIndex < workingDays.size(); dayIndex++) {
            for (int periodIndex = 0; periodIndex < periods.size(); periodIndex++) {
                ParseObject period = periods.get(periodIndex);
                TimeSlot slot = new TimeSlot(
                        dayIndex,
                        periodIndex + 1, // assuming period numbers start at 1
                        period.getString("startTime"),
                        period.getString("endTime")
                );
                timeSlots.add(slot);
            }
        }
        return timeSlots;
    }

    private List<TimetableClass> generateTimetableClasses(List<Teacher> teachers,
                                                          List<Subject> subjects,
                                                          List<Batch> batches) {
        List<TimetableClass> classes = new ArrayList<>();

        for (Batch batch : batches) {
            for (String section : batch.getSections()) {
                for (String subjectName : batch.getSubjects()) {
                    Subject subject = findSubjectByName(subjects, subjectName);
                    if (subject == null) continue;

                    Teacher teacher = findTeacherForSubject(teachers, subjectName);
                    if (teacher == null) continue;

                    // Lectures
                    for (int i = 0; i < subject.getWeeklyLectures(); i++) {
                        classes.add(new TimetableClass(
                                subjectName,
                                teacher.getName(),
                                batch.getName(),
                                section,
                                null, // Timeslot assigned by GA
                                false,
                                1 // 1-hour duration
                        ));
                    }

                    // Labs
                    if (subject.isLab()) {
                        for (int i = 0; i < subject.getWeeklyLabs(); i++) {
                            classes.add(new TimetableClass(
                                    subjectName + " Lab",
                                    teacher.getName(),
                                    batch.getName(),
                                    section,
                                    null,
                                    true,
                                    2 // 2-hour duration
                            ));
                        }
                    }
                }
            }
        }
        return classes;
    }

    private Subject findSubjectByName(List<Subject> subjects, String name) {
        for (Subject subject : subjects) {
            if (subject.getName().equals(name)) {
                return subject;
            }
        }
        return null;
    }

    private Teacher findTeacherForSubject(List<Teacher> teachers, String subjectName) {
        for (Teacher teacher : teachers) {
            if (teacher.getSubjects() != null && teacher.getSubjects().contains(subjectName)) {
                return teacher;
            }
        }
        return teachers.isEmpty() ? null : teachers.get(0); // fallback
    }

    private void saveTimetableToDatabase(Individual solution, TimetableConfig config) throws ParseException {
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
}
