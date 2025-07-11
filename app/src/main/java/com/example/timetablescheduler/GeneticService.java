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

            // Fetch config
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

            // Build TimeSlot list
            List<TimeSlot> timeSlots = new ArrayList<>();
            for (int dayIdx = 0; dayIdx < workingDays.size(); dayIdx++) {
                for (int periodIdx = 0; periodIdx < periodsPerDay; periodIdx++) {
                    ParseObject p = periodObjs.get(periodIdx);
                    timeSlots.add(new TimeSlot(dayIdx, periodIdx+1, p.getString("startTime"), p.getString("endTime"), false));
                }
            }

            // Build break slot set
            Set<String> breakSlotSet = new HashSet<>();
            if (breakObjs != null) {
                for (int dayIdx = 0; dayIdx < workingDays.size(); dayIdx++) {
                    for (ParseObject b : breakObjs) {
                        int breakAfter = b.getInt("breakAfterPeriod");
                        breakSlotSet.add(dayIdx + "_" + (breakAfter+1));
                    }
                }
            }

            // Fetch batches
            ParseQuery<ParseObject> batchQuery = ParseQuery.getQuery("Batch");
            batchQuery.whereEqualTo("user", user);
            List<ParseObject> batchObjs = batchQuery.find();

            // Fetch subjects
            ParseQuery<ParseObject> subjectQuery = ParseQuery.getQuery("Subject");
            subjectQuery.whereEqualTo("user", user);
            List<ParseObject> subjectObjs = subjectQuery.find();
            Map<String, ParseObject> subjectMap = new HashMap<>();
            for (ParseObject s : subjectObjs) {
                subjectMap.put(s.getString("name"), s);
            }

            // For each batch, generate and save the best timetable
            for (ParseObject batch : batchObjs) {
                String batchName = batch.getString("name");
                String section = batch.has("section") ? batch.getString("section") : "";
                String academicYear = batch.has("academicYear") ? batch.getString("academicYear") : "";

                List<Map<String, String>> subjectTeachers = batch.getList("subjectTeachers");
                if (subjectTeachers == null) continue;

                List<TimetableClass> classesToSchedule = new ArrayList<>();
                for (Map<String, String> st : subjectTeachers) {
                    String subjectName = st.get("subject");
                    String teacherName = st.get("teacher");
                    ParseObject subjObj = subjectMap.get(subjectName);
                    if (subjObj == null) continue;
                    int lecturesWeekly = subjObj.has("lecturesWeekly") ? subjObj.getInt("lecturesWeekly") : 0;
                    boolean isLab = subjObj.has("isLab") && subjObj.getBoolean("isLab");

                    // Lectures
                    for (int l = 0; l < lecturesWeekly; l++) {
                        classesToSchedule.add(new TimetableClass(
                                subjectName, teacherName, batchName, section, null, false, 1, lecturesWeekly, isLab ? 1 : 0
                        ));
                    }
                    // Labs
                    if (isLab) {
                        classesToSchedule.add(new TimetableClass(
                                subjectName, teacherName, batchName, section, null, true, 2, lecturesWeekly, 1
                        ));
                    }
                }

                // Run the Genetic Algorithm for this batch
                GeneticAlgorithm ga = new GeneticAlgorithm(
                        timeSlots, new HashMap<>(), new HashMap<>(), new ArrayList<>(), periodsPerDay, breakSlotSet
                );
                Individual solution = ga.generateTimetable(classesToSchedule);

                // Save the result (pass workingDays for day name mapping)
                new ParseRepository().saveTimetableForBatch(solution, workingDays, batchName, section, academicYear);
            }

            // Notify UI
            sendBroadcast(new Intent("TIMETABLE_GENERATED"));
            Log.d(TAG, "Broadcast sent");

        } catch (Exception e) {
            Log.e(TAG, "Error in service", e);
            sendBroadcast(new Intent("TIMETABLE_ERROR"));
        }
    }
}
