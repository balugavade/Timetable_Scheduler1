package com.example.timetablescheduler.models;

public class TimeSlot implements Cloneable {
    private int day;
    private int period;
    private String startTime;
    private String endTime;

    public TimeSlot(int day, int period, String startTime, String endTime) {
        this.day = day;
        this.period = period;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public int getDay() { return day; }
    public int getPeriod() { return period; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }

    @Override
    public TimeSlot clone() {
        try {
            return (TimeSlot) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
