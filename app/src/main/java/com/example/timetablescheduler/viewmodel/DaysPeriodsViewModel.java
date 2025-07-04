package com.example.timetablescheduler.viewmodel;

import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.List;

public class DaysPeriodsViewModel extends ViewModel {
    public String periodsPerDay = "";
    public String numBreaks = "";
    public String daysPerWeek = "";
    public List<String[]> periodRows = new ArrayList<>(); // [start, end]
    public List<String[]> breakRows = new ArrayList<>();  // [after, start, end]
    public boolean[] dayChecks = new boolean[7]; // Monday-Sunday
}
