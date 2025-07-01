package com.example.timetablescheduler.models;

import java.util.List;

public class Teacher {
    private String id;
    private String name;
    private String position;
    private int maxHours;
    private String department;
    private List<String> subjects;

    // Constructors
    public Teacher() {}

    public Teacher(String name, String position, int maxHours, String department, List<String> subjects) {
        this.name = name;
        this.position = position;
        this.maxHours = maxHours;
        this.department = department;
        this.subjects = subjects;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public int getMaxHours() { return maxHours; }
    public void setMaxHours(int maxHours) { this.maxHours = maxHours; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public List<String> getSubjects() { return subjects; }
    public void setSubjects(List<String> subjects) { this.subjects = subjects; }
}
