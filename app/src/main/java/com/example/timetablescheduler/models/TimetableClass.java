package com.example.timetablescheduler.models;

public class TimetableClass implements Cloneable {
    private String subject;
    private String teacher;
    private String batch;
    private String section;
    private TimeSlot timeSlot;
    private boolean isLab;
    private int duration;
    private int weeklyLectures;
    private int weeklyLabs;

    public TimetableClass(String subject, String teacher, String batch,
                          String section, TimeSlot timeSlot, boolean isLab, int duration,
                          int weeklyLectures, int weeklyLabs) {
        this.subject = subject;
        this.teacher = teacher;
        this.batch = batch;
        this.section = section;
        this.timeSlot = timeSlot;
        this.isLab = isLab;
        this.duration = duration;
        this.weeklyLectures = weeklyLectures;
        this.weeklyLabs = weeklyLabs;
    }

    // Getters and setters
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getTeacher() { return teacher; }
    public void setTeacher(String teacher) { this.teacher = teacher; }

    public String getBatch() { return batch; }
    public void setBatch(String batch) { this.batch = batch; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public TimeSlot getTimeSlot() { return timeSlot; }
    public void setTimeSlot(TimeSlot timeSlot) { this.timeSlot = timeSlot; }

    public boolean isLab() { return isLab; }
    public void setLab(boolean lab) { isLab = lab; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public int getWeeklyLectures() { return weeklyLectures; }
    public void setWeeklyLectures(int weeklyLectures) { this.weeklyLectures = weeklyLectures; }

    public int getWeeklyLabs() { return weeklyLabs; }
    public void setWeeklyLabs(int weeklyLabs) { this.weeklyLabs = weeklyLabs; }

    @Override
    public TimetableClass clone() {
        try {
            return (TimetableClass) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
