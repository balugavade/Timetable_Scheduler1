package com.example.timetablescheduler.utils;

import com.example.timetablescheduler.models.*;
import java.util.*;

public class ConstraintUtils {

    public static boolean validateTimetable(Individual individual) {
        Map<String, Set<Integer>> teacherSchedule = new HashMap<>();
        Map<String, Set<Integer>> batchSchedule = new HashMap<>();

        for (TimetableClass cls : individual.getClasses()) {
            for (int period = 0; period < cls.getDuration(); period++) {
                int timeKey = cls.getTimeSlot().getDay() * 100 +
                        (cls.getTimeSlot().getPeriod() + period);

                String teacherKey = cls.getTeacher();
                String batchKey = cls.getBatch() + "_" + cls.getSection();

                if (!teacherSchedule.computeIfAbsent(teacherKey, k -> new HashSet<>()).add(timeKey)) {
                    return false; // Teacher conflict
                }

                if (!batchSchedule.computeIfAbsent(batchKey, k -> new HashSet<>()).add(timeKey)) {
                    return false; // Batch conflict
                }
            }
        }
        return true;
    }

    public static int countConflicts(Individual individual) {
        // Implementation similar to validateTimetable but returns count
        return 0; // Simplified
    }
}
