package com.example.timetablescheduler;

import java.util.*;

public class TimetableScheduler {
    // Constants for the Genetic Algorithm
    private static final int POPULATION_SIZE = 200;
    private static final int MAX_GENERATIONS = 1000;
    private static final double MUTATION_RATE = 0.15;
    private static final double CROSSOVER_RATE = 0.8;
    private static final int TOURNAMENT_SIZE = 5;

    // Data structures
    private final List<String> subjects;
    private final Map<String, List<String>> subjectTeachers;
    private final List<String> semesters;
    private final int timeSlotsPerWeek;
    private final Map<String, Integer> subjectHours; // Hours required per subject
    private final Map<String, List<Integer>> teacherAvailability; // Available time slots per teacher
    private final Map<String, List<Integer>> roomAvailability; // Available time slots per room
    private final Set<String> labSubjects; // Set of subjects that are labs
    private final Map<String, List<String>> labTeachers; // Map of lab subject to list of teacher pairs

    public TimetableScheduler(
            List<String> subjects,
            Map<String, List<String>> subjectTeachers,
            List<String> semesters,
            int timeSlotsPerWeek,
            Map<String, Integer> subjectHours,
            Map<String, List<Integer>> teacherAvailability,
            Map<String, List<Integer>> roomAvailability,
            Set<String> labSubjects,
            Map<String, List<String>> labTeachers) {
        this.subjects = subjects;
        this.subjectTeachers = subjectTeachers;
        this.semesters = semesters;
        this.timeSlotsPerWeek = timeSlotsPerWeek;
        this.subjectHours = subjectHours;
        this.teacherAvailability = teacherAvailability;
        this.roomAvailability = roomAvailability;
        this.labSubjects = labSubjects;
        this.labTeachers = labTeachers;
    }

    // Chromosome representation
    private static class Gene {
        String subject;
        List<String> teachers; // List of teachers (1 for regular, 2 for lab)
        String semester;
        String room;
        int timeSlot;
        boolean isLab;
        int duration; // Duration in hours (1 for regular, 2 for lab)

        Gene(String subject, List<String> teachers, String semester, String room, int timeSlot, boolean isLab) {
            this.subject = subject;
            this.teachers = new ArrayList<>(teachers);
            this.semester = semester;
            this.room = room;
            this.timeSlot = timeSlot;
            this.isLab = isLab;
            this.duration = isLab ? 2 : 1;
        }

        Gene(Gene other) {
            this.subject = other.subject;
            this.teachers = new ArrayList<>(other.teachers);
            this.semester = other.semester;
            this.room = other.room;
            this.timeSlot = other.timeSlot;
            this.isLab = other.isLab;
            this.duration = other.duration;
        }
    }

    // Create a random chromosome
    private List<Gene> createRandomChromosome() {
        List<Gene> chromosome = new ArrayList<>();
        Random random = new Random();

        for (String subject : subjects) {
            int hoursRequired = subjectHours.getOrDefault(subject, 1);
            boolean isLab = labSubjects.contains(subject);
            int sessionsNeeded = isLab ? hoursRequired / 2 : hoursRequired;

            for (int i = 0; i < sessionsNeeded; i++) {
                List<String> teachers;
                if (isLab) {
                    teachers = getRandomLabTeachers(subject);
                } else {
                    teachers = Collections.singletonList(getRandomTeacher(subject));
                }

                String semester = semesters.get(random.nextInt(semesters.size()));
                String room = getRandomAvailableRoom();
                int timeSlot = getRandomAvailableTimeSlot(teachers, room, isLab);
                
                chromosome.add(new Gene(subject, teachers, semester, room, timeSlot, isLab));
            }
        }
        return chromosome;
    }

    private List<String> getRandomLabTeachers(String subject) {
        List<String> teachers = labTeachers.get(subject);
        if (teachers == null || teachers.size() < 2) {
            throw new IllegalStateException("Lab subject " + subject + " must have at least 2 teachers");
        }
        // Randomly select 2 teachers from the list
        Collections.shuffle(teachers);
        return teachers.subList(0, 2);
    }

    private String getRandomTeacher(String subject) {
        List<String> teachers = subjectTeachers.get(subject);
        return teachers.get(new Random().nextInt(teachers.size()));
    }

    private String getRandomAvailableRoom() {
        List<String> rooms = new ArrayList<>(roomAvailability.keySet());
        return rooms.get(new Random().nextInt(rooms.size()));
    }

    private int getRandomAvailableTimeSlot(List<String> teachers, String room, boolean isLab) {
        List<Integer> availableSlots = new ArrayList<>();
        List<Integer> roomSlots = roomAvailability.get(room);
        
        // For each time slot, check if it's available for all teachers and the room
        for (int slot : roomSlots) {
            boolean allTeachersAvailable = true;
            for (String teacher : teachers) {
                if (!teacherAvailability.get(teacher).contains(slot) || 
                    (isLab && !teacherAvailability.get(teacher).contains(slot + 1))) {
                    allTeachersAvailable = false;
                    break;
                }
            }
            if (allTeachersAvailable) {
                availableSlots.add(slot);
            }
        }
        
        if (availableSlots.isEmpty()) {
            throw new IllegalStateException("No available time slots found for the given constraints");
        }
        
        return availableSlots.get(new Random().nextInt(availableSlots.size()));
    }

    // Calculate fitness of a chromosome (lower is better)
    private double calculateFitness(List<Gene> chromosome) {
        double penalty = 0.0;

        // Check teacher conflicts
        Map<String, Set<Integer>> teacherTimeSlots = new HashMap<>();
        // Check room conflicts
        Map<String, Set<Integer>> roomTimeSlots = new HashMap<>();
        // Check semester conflicts
        Map<String, Set<Integer>> semesterTimeSlots = new HashMap<>();

        for (Gene gene : chromosome) {
            // Check conflicts for each time slot in the session
            for (int i = 0; i < gene.duration; i++) {
                int currentSlot = gene.timeSlot + i;
                
                // Teacher conflicts
                for (String teacher : gene.teachers) {
                    teacherTimeSlots.putIfAbsent(teacher, new HashSet<>());
                    if (!teacherTimeSlots.get(teacher).add(currentSlot)) {
                        penalty += 10.0;
                    }
                }

                // Room conflicts
                roomTimeSlots.putIfAbsent(gene.room, new HashSet<>());
                if (!roomTimeSlots.get(gene.room).add(currentSlot)) {
                    penalty += 10.0;
                }

                // Semester conflicts
                semesterTimeSlots.putIfAbsent(gene.semester, new HashSet<>());
                if (!semesterTimeSlots.get(gene.semester).add(currentSlot)) {
                    penalty += 10.0;
                }

                // Check teacher availability
                for (String teacher : gene.teachers) {
                    if (!teacherAvailability.get(teacher).contains(currentSlot)) {
                        penalty += 5.0;
                    }
                }

                // Check room availability
                if (!roomAvailability.get(gene.room).contains(currentSlot)) {
                    penalty += 5.0;
                }
            }
        }

        // Check if all subjects have required hours
        Map<String, Integer> subjectHoursCount = new HashMap<>();
        for (Gene gene : chromosome) {
            subjectHoursCount.merge(gene.subject, gene.duration, Integer::sum);
        }
        for (Map.Entry<String, Integer> entry : subjectHours.entrySet()) {
            int required = entry.getValue();
            int actual = subjectHoursCount.getOrDefault(entry.getKey(), 0);
            penalty += Math.abs(required - actual) * 3.0;
        }

        return penalty;
    }

    // Tournament selection
    private List<Gene> tournamentSelection(List<List<Gene>> population) {
        Random random = new Random();
        List<Gene> best = null;
        double bestFitness = Double.MAX_VALUE;

        for (int i = 0; i < TOURNAMENT_SIZE; i++) {
            List<Gene> candidate = population.get(random.nextInt(population.size()));
            double fitness = calculateFitness(candidate);
            if (fitness < bestFitness) {
                best = candidate;
                bestFitness = fitness;
            }
        }

        // Deep copy
        List<Gene> copy = new ArrayList<>();
        for (Gene gene : best) {
            copy.add(new Gene(gene));
        }
        return copy;
    }

    // Crossover
    private List<Gene> crossover(List<Gene> parent1, List<Gene> parent2) {
        if (new Random().nextDouble() > CROSSOVER_RATE) {
            return new ArrayList<>(parent1);
        }

        List<Gene> child = new ArrayList<>();
        int crossoverPoint = new Random().nextInt(parent1.size());

        for (int i = 0; i < parent1.size(); i++) {
            if (i < crossoverPoint) {
                child.add(new Gene(parent1.get(i)));
            } else {
                child.add(new Gene(parent2.get(i)));
            }
        }
        return child;
    }

    // Mutation
    private void mutate(List<Gene> chromosome) {
        Random random = new Random();
        for (Gene gene : chromosome) {
            if (random.nextDouble() < MUTATION_RATE) {
                gene.timeSlot = getRandomAvailableTimeSlot(gene.teachers, gene.room, gene.isLab);
            }
        }
    }

    // Main Genetic Algorithm
    public List<Gene> generateTimetable() {
        // Initialize population
        List<List<Gene>> population = new ArrayList<>();
        for (int i = 0; i < POPULATION_SIZE; i++) {
            population.add(createRandomChromosome());
        }

        // Evolution loop
        for (int generation = 0; generation < MAX_GENERATIONS; generation++) {
            // Sort population by fitness
            population.sort(Comparator.comparingDouble(this::calculateFitness));

            // Check if we found a perfect solution
            if (calculateFitness(population.get(0)) == 0) {
                break;
            }

            // Create new population
            List<List<Gene>> newPopulation = new ArrayList<>();
            
            // Elitism: keep best 10%
            int eliteCount = POPULATION_SIZE / 10;
            for (int i = 0; i < eliteCount; i++) {
                newPopulation.add(population.get(i));
            }

            // Fill rest with offspring
            while (newPopulation.size() < POPULATION_SIZE) {
                List<Gene> parent1 = tournamentSelection(population);
                List<Gene> parent2 = tournamentSelection(population);
                List<Gene> child = crossover(parent1, parent2);
                mutate(child);
                newPopulation.add(child);
            }

            population = newPopulation;
        }

        // Return best solution
        population.sort(Comparator.comparingDouble(this::calculateFitness));
        return population.get(0);
    }

    // Utility method to print timetable
    public static void printTimetable(List<Gene> timetable) {
        System.out.println("\nGenerated Timetable:");
        System.out.println("===================");
        
        // Group by semester
        Map<String, List<Gene>> semesterGroups = new HashMap<>();
        for (Gene gene : timetable) {
            semesterGroups.computeIfAbsent(gene.semester, k -> new ArrayList<>()).add(gene);
        }

        // Print each semester's timetable
        for (Map.Entry<String, List<Gene>> entry : semesterGroups.entrySet()) {
            System.out.println("\nSemester: " + entry.getKey());
            System.out.println("-------------------");
            
            // Sort by time slot
            entry.getValue().sort(Comparator.comparingInt(g -> g.timeSlot));
            
            for (Gene gene : entry.getValue()) {
                String type = gene.isLab ? "LAB" : "Theory";
                String teachers = String.join(" & ", gene.teachers);
                System.out.printf("Time Slot %d-%d: %s (%s) - %s (Room: %s, Teachers: %s)%n",
                        gene.timeSlot, gene.timeSlot + gene.duration - 1,
                        gene.subject, type, gene.semester, gene.room, teachers);
            }
        }
    }
} 