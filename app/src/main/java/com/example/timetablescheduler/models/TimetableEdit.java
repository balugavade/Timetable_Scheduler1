package com.example.timetablescheduler.models;

public class TimetableEdit {
    private String objectId;
    private String subject;
    private String teacher;
    private boolean isLab;
    private int dayIdx;
    private int periodIdx;
    private int colIdx;
    private String batchName;
    private String section;
    private String academicYear;

    public TimetableEdit(String objectId, String subject, String teacher, boolean isLab,
                         int dayIdx, int periodIdx, int colIdx,
                         String batchName, String section, String academicYear) {
        this.objectId = objectId;
        this.subject = subject;
        this.teacher = teacher;
        this.isLab = isLab;
        this.dayIdx = dayIdx;
        this.periodIdx = periodIdx;
        this.colIdx = colIdx;
        this.batchName = batchName;
        this.section = section;
        this.academicYear = academicYear;
    }

    // Getters and setters
    public String getObjectId() { return objectId; }
    public String getSubject() { return subject; }
    public String getTeacher() { return teacher; }
    public boolean isLab() { return isLab; }
    public int getDayIdx() { return dayIdx; }
    public int getPeriodIdx() { return periodIdx; }
    public int getColIdx() { return colIdx; }
    public String getBatchName() { return batchName; }
    public String getSection() { return section; }
    public String getAcademicYear() { return academicYear; }
    public void setSubject(String subject) { this.subject = subject; }
    public void setTeacher(String teacher) { this.teacher = teacher; }
    public void setLab(boolean lab) { isLab = lab; }
}
