package com.example.timetablescheduler.models;

public class Subject {
    private String id;
    private String code;
    private String name;
    private boolean isLab;
    private int weeklyLectures;
    private int weeklyLabs;
    private int semester;

    // Constructors
    public Subject() {}

    public Subject(String code, String name, boolean isLab, int weeklyLectures, int weeklyLabs, int semester) {
        this.code = code;
        this.name = name;
        this.isLab = isLab;
        this.weeklyLectures = weeklyLectures;
        this.weeklyLabs = weeklyLabs;
        this.semester = semester;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isLab() { return isLab; }
    public void setLab(boolean lab) { isLab = lab; }

    public int getWeeklyLectures() { return weeklyLectures; }
    public void setWeeklyLectures(int weeklyLectures) { this.weeklyLectures = weeklyLectures; }

    public int getWeeklyLabs() { return weeklyLabs; }
    public void setWeeklyLabs(int weeklyLabs) { this.weeklyLabs = weeklyLabs; }

    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }
}
