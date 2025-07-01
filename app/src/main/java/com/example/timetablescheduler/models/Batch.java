package com.example.timetablescheduler.models;

import java.util.List;

public class Batch {
    private String id;
    private String name;
    private String department;
    private String academicYear;
    private List<String> sections;
    private List<String> subjects;

    // Constructors
    public Batch() {}

    public Batch(String name, String department, String academicYear, List<String> sections, List<String> subjects) {
        this.name = name;
        this.department = department;
        this.academicYear = academicYear;
        this.sections = sections;
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

    public List<String> getSections() { return sections; }
    public void setSections(List<String> sections) { this.sections = sections; }

    public List<String> getSubjects() { return subjects; }
    public void setSubjects(List<String> subjects) { this.subjects = subjects; }
}
