package com.example.timetablescheduler;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import com.example.timetablescheduler.algorithm.GeneticAlgorithm;
import com.example.timetablescheduler.models.*;
import com.example.timetablescheduler.data.ParseRepository;
import com.parse.*;
import java.util.*;

public class GeneticService extends IntentService {
    private static final String TAG = "GeneticService";

    public GeneticService() { super("GeneticService"); }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "Service started");
        try {
            ParseUser user = ParseUser.getCurrentUser();

            // 1. Fetch TimetableConfig
            ParseQuery<ParseObject> configQuery = ParseQuery.getQuery("TimetableConfig");
            configQuery.whereEqualTo("user", user);
            configQuery.orderByDescending("createdAt");
            configQuery.setLimit(1);
            configQuery.include("breaks");
            configQuery.include("periods");
            ParseObject config = configQuery.getFirst();

            if (config == null) throw new Exception("No timetable configuration found!");

            List<String> workingDays = config.getList("workingDays");
            List<ParseObject> periodObjs = config.getList("periods");
            List<ParseObject> breakObjs = config.getList("breaks");
            int periodsPerDay = periodObjs.size();

            // Build TimeSlot list (skip breaks for class assignment)
            List<TimeSlot> timeSlots = new ArrayList<>();
            for (int dayIdx = 0; dayIdx < workingDays.size(); dayIdx++) {
                for (int periodIdx = 0; periodIdx < periodsPerDay; periodIdx++) {
                    ParseObject p = periodObjs.get(periodIdx);
                    timeSlots.add(new TimeSlot(dayIdx, periodIdx+1, p.getString("startTime"), p.getString("endTime"), false));
                }
            }

            // For lab validation, build a set of break slots for quick lookup
            Set<String> breakSlotSet = new HashSet<>();
            if (breakObjs != null) {
                for (int dayIdx = 0; dayIdx < workingDays.size(); dayIdx++) {
                    for (ParseObject b : breakObjs) {
                        int breakAfter = b.getInt("breakAfterPeriod");
                        breakSlotSet.add(dayIdx + "_" + (breakAfter+1));
                    }
                }
            }

            // 2. Fetch Teachers
            ParseQuery<ParseObject> teacherQuery = ParseQuery.getQuery("Teacher");
            teacherQuery.whereEqualTo("user", user);
            List<ParseObject> teacherObjs = teacherQuery.find();

            Map<String, Integer> teacherLoads = new HashMap<>();
            Map<String, List<String>> teacherSubjects = new HashMap<>();
            List<String> teachers = new ArrayList<>();
            for (ParseObject t : teacherObjs) {
                String name = t.getString("name");
                teachers.add(name);
                int load = 20;
                try { load = Integer.parseInt(t.getString("load")); } catch (Exception ignore) {}
                teacherLoads.put(name, load);
                String subjectsStr = t.getString("subjects");
                List<String> subjects = Arrays.asList(subjectsStr.split(","));
                teacherSubjects.put(name, subjects);
            }

            // 3. Fetch Batches
            ParseQuery<ParseObject> batchQuery = ParseQuery.getQuery("Batch");
            batchQuery.whereEqualTo("user", user);
            List<ParseObject> batchObjs = batchQuery.find();

            // 4. Fetch Subjects
            ParseQuery<ParseObject> subjectQuery = ParseQuery.getQuery("Subject");
            subjectQuery.whereEqualTo("user", user);
            List<ParseObject> subjectObjs = subjectQuery.find();
            Map<String, ParseObject> subjectMap = new HashMap<>();
            for (ParseObject s : subjectObjs) {
                subjectMap.put(s.getString("name"), s);
            }

            // 5. Build TimetableClass list to schedule
            List<TimetableClass> classesToSchedule = new ArrayList<>();
            for (ParseObject batch : batchObjs) {
                String batchName = batch.getString("name");
                List<String> subjects = batch.getList("subjects");
                if (subjects == null) continue;
                for (String subjectName : subjects) {
                    ParseObject subjObj = subjectMap.get(subjectName);
                    if (subjObj == null) continue;
                    boolean isLab = subjObj.has("isLab") && subjObj.getBoolean("isLab");
                    int lecturesWeekly = subjObj.has("lecturesWeekly") ? subjObj.getInt("lecturesWeekly") : 0;
                    int labsWeekly = subjObj.has("labsWeekly") ? subjObj.getInt("labsWeekly") : 0;
                    String teacherName = "";
                    ParseObject teacherObj = subjObj.getParseObject("teacher");
                    if (teacherObj != null) teacherName = teacherObj.getString("name");
                    // Lectures
                    for (int l = 0; l < lecturesWeekly; l++) {
                        classesToSchedule.add(new TimetableClass(
                                subjectName, teacherName, batchName, "", null, false, 1
                        ));
                    }
                    // Labs (each lab is 2 periods, so labsWeekly is number of double slots)
                    for (int lab = 0; lab < labsWeekly; lab++) {
                        classesToSchedule.add(new TimetableClass(
                                subjectName, teacherName, batchName, "", null, true, 2
                        ));
                    }
                }
            }

            // 6. Run the Genetic Algorithm
            GeneticAlgorithm ga = new GeneticAlgorithm(
                    timeSlots, teacherLoads, teacherSubjects, teachers, periodsPerDay, breakSlotSet
            );
            Individual solution = ga.generateTimetable(classesToSchedule);

            // 7. Save the result (pass workingDays for day name mapping)
            new ParseRepository().saveTimetable(solution, workingDays);

            // 8. Notify UI
            sendBroadcast(new Intent("TIMETABLE_GENERATED"));
            Log.d(TAG, "Broadcast sent");

        } catch (Exception e) {
            Log.e(TAG, "Error in service", e);
            sendBroadcast(new Intent("TIMETABLE_ERROR"));
        }
    }
}
