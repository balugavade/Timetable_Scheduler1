package com.example.timetablescheduler.models;

import java.util.List;

public class Batch {
    private String id;
    private String name;
    private String department;
    private String academicYear;
    private String section; // Changed from List<String> to String
    private List<String> subjects;

    // Constructors
    public Batch() {}

    public Batch(String name, String department, String academicYear, String section, List<String> subjects) {
        this.name = name;
        this.department = department;
        this.academicYear = academicYear;
        this.section = section;
        this.subjects = subjects;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public List<String> getSubjects() { return subjects; }
    public void setSubjects(List<String> subjects) { this.subjects = subjects; }
}
