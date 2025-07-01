package com.example.timetablescheduler.models;

public class TimetableEntry {
    private String subject;
    private String teacher;
    private String batch;
    private String section;
    private int day;
    private int period;
    private boolean isLab;

    public TimetableEntry(String subject, String teacher, String batch,
                          String section, int day, int period, boolean isLab) {
        this.subject = subject;
        this.teacher = teacher;
        this.batch = batch;
        this.section = section;
        this.day = day;
        this.period = period;
        this.isLab = isLab;
    }

    // Getters
    public String getSubject() { return subject; }
    public String getTeacher() { return teacher; }
    public String getBatch() { return batch; }
    public String getSection() { return section; }
    public int getDay() { return day; }
    public int getPeriod() { return period; }
    public boolean isLab() { return isLab; }
}
