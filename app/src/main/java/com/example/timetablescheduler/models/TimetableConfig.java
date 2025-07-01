package com.example.timetablescheduler.models;

import com.parse.ParseObject;
import java.util.List;

public class TimetableConfig {
    private int periodsPerDay;
    private int breaksPerDay;
    private List<String> workingDays;
    private List<ParseObject> periods;
    private List<ParseObject> breaks;

    // Constructors
    public TimetableConfig() {}

    // Getters and Setters
    public int getPeriodsPerDay() { return periodsPerDay; }
    public void setPeriodsPerDay(int periodsPerDay) { this.periodsPerDay = periodsPerDay; }

    public int getBreaksPerDay() { return breaksPerDay; }
    public void setBreaksPerDay(int breaksPerDay) { this.breaksPerDay = breaksPerDay; }

    public List<String> getWorkingDays() { return workingDays; }
    public void setWorkingDays(List<String> workingDays) { this.workingDays = workingDays; }

    public List<ParseObject> getPeriods() { return periods; }
    public void setPeriods(List<ParseObject> periods) { this.periods = periods; }

    public List<ParseObject> getBreaks() { return breaks; }
    public void setBreaks(List<ParseObject> breaks) { this.breaks = breaks; }
}
