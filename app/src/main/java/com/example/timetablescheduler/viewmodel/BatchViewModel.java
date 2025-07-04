package com.example.timetablescheduler.viewmodel;

import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.List;

public class BatchViewModel extends ViewModel {
    public String department = "";
    public String totalBatches = "";
    public List<BatchData> batchDataList = new ArrayList<>();

    public static class BatchData {
        public String batchName = "";
        public boolean sectionA = false, sectionB = false;
        public String academicYear = "";
        public String totalSubjects = "";
        public List<SubjectTeacher> subjectTeachers = new ArrayList<>();
    }
    public static class SubjectTeacher {
        public String subject = "";
        public String teacher = "";
    }
}
