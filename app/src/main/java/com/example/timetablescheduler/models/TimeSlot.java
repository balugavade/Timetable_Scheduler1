package com.example.timetablescheduler.models;

public class TimeSlot {
    private int day; // 0=Monday, 1=Tuesday, etc.
    private int period; // 1-based period number
    private String startTime;
    private String endTime;
    private boolean isBreak;

    public TimeSlot(int day, int period, String startTime, String endTime, boolean isBreak) {
        this.day = day;
        this.period = period;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isBreak = isBreak;
    }

    // Getters and setters
    public int getDay() { return day; }
    public void setDay(int day) { this.day = day; }

    public int getPeriod() { return period; }
    public void setPeriod(int period) { this.period = period; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public boolean isBreak() { return isBreak; }
    public void setBreak(boolean isBreak) { this.isBreak = isBreak; }
}
