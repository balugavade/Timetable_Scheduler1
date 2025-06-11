package com.example.timetablescheduler;

import java.util.*;

public class Timetable{

    // --- Data classes ---
    public static class Gene {
        public String subject;
        public String teacher;
        public String semester;
        public int timeSlot; // 0..N-1

        public Gene(String subject, String teacher, String semester, int timeSlot) {
            this.subject = subject;
            this.teacher = teacher;
            this.semester = semester;
            this.timeSlot = timeSlot;
        }

        public Gene(Gene g) {
            this.subject = g.subject;
            this.teacher = g.teacher;
            this.semester = g.semester;
            this.timeSlot = g.timeSlot;
        }
    }

    // --- Algorithm parameters ---
    private final int POP_SIZE = 100;
    private final int GENERATIONS = 500;
    private final double MUTATION_RATE = 0.1;

    // --- Data for timetable ---
    private List<String> allSubjects;
    private Map<String, List<String>> subjectTeachers; // subject -> list of teachers
    private List<String> allSemesters;
    private int timeSlotsPerWeek;

    // --- Constructor (pass your data here) ---
    public Timetable(List<String> allSubjects,
                                     Map<String, List<String>> subjectTeachers,
                                     List<String> allSemesters,
                                     int timeSlotsPerWeek) {
        this.allSubjects = allSubjects;
        this.subjectTeachers = subjectTeachers;
        this.allSemesters = allSemesters;
        this.timeSlotsPerWeek = timeSlotsPerWeek;
    }

    // --- Chromosome = List<Gene> ---
    private List<Gene> createRandomChromosome() {
        List<Gene> chromosome = new ArrayList<>();
        Random rand = new Random();
        for (String subject : allSubjects) {
            String semester = allSemesters.get(rand.nextInt(allSemesters.size()));
            List<String> teachers = subjectTeachers.get(subject);
            String teacher = teachers.get(rand.nextInt(teachers.size()));
            int timeSlot = rand.nextInt(timeSlotsPerWeek);
            chromosome.add(new Gene(subject, teacher, semester, timeSlot));
        }
        return chromosome;
    }

    // --- Fitness: lower is better, 0 is perfect ---
    private int fitness(List<Gene> chromosome) {
        int penalty = 0;
        // No teacher or semester should have two classes in the same time slot
        Map<String, Set<Integer>> teacherSlots = new HashMap<>();
        Map<String, Set<Integer>> semesterSlots = new HashMap<>();
        for (Gene g : chromosome) {
            teacherSlots.putIfAbsent(g.teacher, new HashSet<>());
            semesterSlots.putIfAbsent(g.semester, new HashSet<>());
            if (!teacherSlots.get(g.teacher).add(g.timeSlot)) penalty += 10;
            if (!semesterSlots.get(g.semester).add(g.timeSlot)) penalty += 10;
        }
        return penalty;
    }

    // --- Tournament selection ---
    private List<Gene> select(List<List<Gene>> population) {
        Random rand = new Random();
        List<Gene> best = null;
        int bestScore = Integer.MAX_VALUE;
        for (int i = 0; i < 5; i++) {
            List<Gene> candidate = population.get(rand.nextInt(population.size()));
            int score = fitness(candidate);
            if (score < bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        // Deep copy
        List<Gene> copy = new ArrayList<>();
        for (Gene g : best) copy.add(new Gene(g));
        return copy;
    }

    // --- Single-point crossover ---
    private List<Gene> crossover(List<Gene> p1, List<Gene> p2) {
        Random rand = new Random();
        int point = rand.nextInt(p1.size());
        List<Gene> child = new ArrayList<>();
        for (int i = 0; i < p1.size(); i++) {
            child.add(new Gene(i < point ? p1.get(i) : p2.get(i)));
        }
        return child;
    }

    // --- Mutation ---
    private void mutate(List<Gene> chromosome) {
        Random rand = new Random();
        for (Gene g : chromosome) {
            if (rand.nextDouble() < MUTATION_RATE) {
                g.timeSlot = rand.nextInt(timeSlotsPerWeek);
            }
        }
    }

    // --- Main algorithm ---
    public List<Gene> run() {
        // 1. Initialize population
        List<List<Gene>> population = new ArrayList<>();
        for (int i = 0; i < POP_SIZE; i++) {
            population.add(createRandomChromosome());
        }

        // 2. Evolve
        for (int gen = 0; gen < GENERATIONS; gen++) {
            // Sort by fitness
            population.sort(Comparator.comparingInt(this::fitness));
            // Early exit if perfect
            if (fitness(population.get(0)) == 0) break;

            List<List<Gene>> nextGen = new ArrayList<>();
            // Elitism: keep best 10%
            int elites = POP_SIZE / 10;
            for (int i = 0; i < elites; i++) nextGen.add(population.get(i));
            // Fill rest with offspring
            while (nextGen.size() < POP_SIZE) {
                List<Gene> p1 = select(population);
                List<Gene> p2 = select(population);
                List<Gene> child = crossover(p1, p2);
                mutate(child);
                nextGen.add(child);
            }
            population = nextGen;
        }
        // Return best
        return population.get(0);
    }

    // --- Utility: print timetable ---
    public static void printTimetable(List<Gene> timetable) {
        for (Gene g : timetable) {
            System.out.println("Subject: " + g.subject +
                    ", Teacher: " + g.teacher +
                    ", Semester: " + g.semester +
                    ", Slot: " + g.timeSlot);
        }
    }
}
