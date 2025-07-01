package com.example.timetablescheduler.models;

import java.util.*;

public class Individual {
    private List<TimetableClass> classes;
    private double fitness;

    public Individual() {
        this.classes = new ArrayList<>();
        this.fitness = 0.0;
    }

    public List<TimetableClass> getClasses() { return classes; }
    public void setClasses(List<TimetableClass> classes) { this.classes = classes; }
    public double getFitness() { return fitness; }
    public void setFitness(double fitness) { this.fitness = fitness; }

    public Individual clone() {
        Individual clone = new Individual();
        List<TimetableClass> clonedClasses = new ArrayList<>();
        for (TimetableClass cls : this.classes) {
            clonedClasses.add(cls.clone());
        }
        clone.setClasses(clonedClasses);
        clone.setFitness(this.fitness);
        return clone;
    }
}
