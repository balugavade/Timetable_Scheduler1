package com.example.timetablescheduler.models;

public class TimetableClass implements Cloneable {
    private String subject;
    private String teacher;
    private String batch;
    private String section;
    private TimeSlot timeSlot;
    private boolean isLab;
    private int duration; // 1=lecture, 2=lab

    public TimetableClass(String subject, String teacher, String batch,
                          String section, TimeSlot timeSlot, boolean isLab, int duration) {
        this.subject = subject;
        this.teacher = teacher;
        this.batch = batch;
        this.section = section;
        this.timeSlot = timeSlot;
        this.isLab = isLab;
        this.duration = duration;
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

    @Override
    public TimetableClass clone() {
        try {
            TimetableClass copy = (TimetableClass) super.clone();
            copy.timeSlot = (this.timeSlot != null) ? this.timeSlot.clone() : null;
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
