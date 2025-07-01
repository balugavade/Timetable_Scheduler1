package com.example.timetablescheduler.algorithm;

import com.example.timetablescheduler.models.*;
import java.util.*;

public class GeneticAlgorithm {
    // Configuration parameters
    private static final int POPULATION_SIZE = 100;
    private static final double CROSSOVER_RATE = 0.8;
    private static final double MUTATION_RATE = 0.05;
    private static final int MAX_GENERATIONS = 200;
    private static final double FITNESS_THRESHOLD = 0.95;
    private static final int ELITISM_COUNT = POPULATION_SIZE / 10; // Top 10%

    private final List<TimeSlot> timeSlots;
    private final Map<String, Integer> teacherLoads;
    private final Map<String, List<String>> teacherSubjects;
    private final List<String> teachers;

    public GeneticAlgorithm(List<TimeSlot> timeSlots, Map<String, Integer> teacherLoads,
                            Map<String, List<String>> teacherSubjects, List<String> teachers) {
        this.timeSlots = timeSlots;
        this.teacherLoads = teacherLoads;
        this.teacherSubjects = teacherSubjects;
        this.teachers = teachers;
    }

    public Individual generateTimetable(List<TimetableClass> classes) {
        List<Individual> population = initializePopulation(classes);
        Individual bestSolution = null;
        int generation = 0;

        while (generation < MAX_GENERATIONS) {
            // Evaluate fitness
            evaluatePopulation(population);
            Collections.sort(population, (i1, i2) ->
                    Double.compare(i2.getFitness(), i1.getFitness())); // Descending

            // Update best solution
            Individual currentBest = population.get(0);
            if (bestSolution == null || currentBest.getFitness() > bestSolution.getFitness()) {
                bestSolution = currentBest.clone();
            }

            // Check termination condition
            if (bestSolution.getFitness() >= FITNESS_THRESHOLD) {
                break;
            }

            // Create new population
            List<Individual> newPopulation = new ArrayList<>();

            // Elitism: Preserve top individuals
            for (int i = 0; i < ELITISM_COUNT; i++) {
                newPopulation.add(population.get(i).clone());
            }

            // Fill rest with offspring
            while (newPopulation.size() < POPULATION_SIZE) {
                Individual parent1 = selectParentRoulette(population);
                Individual parent2 = selectParentRoulette(population);

                Individual offspring;
                if (Math.random() < CROSSOVER_RATE) {
                    offspring = crossover(parent1, parent2);
                } else {
                    offspring = parent1.clone();
                }

                mutate(offspring);
                newPopulation.add(offspring);
            }

            population = newPopulation;
            generation++;
        }

        return bestSolution;
    }

    private List<Individual> initializePopulation(List<TimetableClass> classes) {
        List<Individual> population = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < POPULATION_SIZE; i++) {
            Individual individual = new Individual();
            List<TimetableClass> individualClasses = new ArrayList<>();

            for (TimetableClass cls : classes) {
                TimetableClass newCls = new TimetableClass(
                        cls.getSubject(),
                        cls.getTeacher(),
                        cls.getBatch(),
                        cls.getSection(),
                        timeSlots.get(random.nextInt(timeSlots.size())),
                        cls.isLab(),
                        cls.getDuration()
                );
                individualClasses.add(newCls);
            }
            individual.setClasses(individualClasses);
            population.add(individual);
        }
        return population;
    }

    private void evaluatePopulation(List<Individual> population) {
        for (Individual individual : population) {
            individual.setFitness(calculateFitness(individual));
        }
    }

    private double calculateFitness(Individual individual) {
        int conflicts = 0;
        int totalConstraints = 0;
        Map<String, Set<Integer>> teacherSchedule = new HashMap<>();
        Map<String, Set<Integer>> batchSchedule = new HashMap<>();

        for (TimetableClass cls : individual.getClasses()) {
            TimeSlot ts = cls.getTimeSlot();
            String teacherKey = cls.getTeacher();
            String batchKey = cls.getBatch() + "_" + cls.getSection();
            int timeKey = ts.getDay() * 100 + ts.getPeriod();

            // Teacher conflict check
            if (!teacherSchedule.computeIfAbsent(teacherKey, k -> new HashSet<>()).add(timeKey)) {
                conflicts++;
            }

            // Batch conflict check
            if (!batchSchedule.computeIfAbsent(batchKey, k -> new HashSet<>()).add(timeKey)) {
                conflicts++;
            }

            totalConstraints += 2;
        }

        // Teacher load check
        Map<String, Integer> teacherLoadCount = new HashMap<>();
        for (TimetableClass cls : individual.getClasses()) {
            teacherLoadCount.merge(cls.getTeacher(), cls.getDuration(), Integer::sum);
        }

        for (Map.Entry<String, Integer> entry : teacherLoadCount.entrySet()) {
            int assigned = entry.getValue();
            int maxAllowed = teacherLoads.getOrDefault(entry.getKey(), 20);
            if (assigned > maxAllowed) conflicts += (assigned - maxAllowed);
            totalConstraints++;
        }

        // Lab continuity penalty
        for (TimetableClass cls : individual.getClasses()) {
            if (cls.isLab() && cls.getDuration() == 2) {
                if (cls.getTimeSlot().getPeriod() + 1 > 6) { // Assuming 6 periods/day
                    conflicts++;
                }
            }
        }

        return (totalConstraints == 0) ? 1.0 : Math.max(0, 1.0 - (double) conflicts / totalConstraints);
    }

    private Individual selectParentRoulette(List<Individual> population) {
        double totalFitness = 0;
        for (Individual ind : population) {
            totalFitness += ind.getFitness();
        }

        double randomPoint = Math.random() * totalFitness;
        double currentSum = 0;

        for (Individual ind : population) {
            currentSum += ind.getFitness();
            if (currentSum >= randomPoint) {
                return ind;
            }
        }
        return population.get(0);
    }

    private Individual crossover(Individual parent1, Individual parent2) {
        Individual child = new Individual();
        List<TimetableClass> childClasses = new ArrayList<>();
        Random random = new Random();

        // Two-point crossover
        int size = parent1.getClasses().size();
        int point1 = random.nextInt(size);
        int point2 = random.nextInt(size - point1) + point1;

        // Take genes from parent1 between point1 and point2
        for (int i = 0; i < size; i++) {
            if (i >= point1 && i <= point2) {
                childClasses.add(parent1.getClasses().get(i).clone());
            } else {
                childClasses.add(parent2.getClasses().get(i).clone());
            }
        }

        child.setClasses(childClasses);
        return child;
    }

    private void mutate(Individual individual) {
        Random random = new Random();
        for (TimetableClass cls : individual.getClasses()) {
            if (random.nextDouble() < MUTATION_RATE) {
                // Time slot mutation
                if (cls.isLab() && cls.getDuration() == 2) {
                    // Find valid lab slots (consecutive periods)
                    List<TimeSlot> validLabSlots = new ArrayList<>();
                    for (TimeSlot slot : timeSlots) {
                        if (slot.getPeriod() < 6) { // Not last period
                            validLabSlots.add(slot);
                        }
                    }
                    if (!validLabSlots.isEmpty()) {
                        cls.setTimeSlot(validLabSlots.get(random.nextInt(validLabSlots.size())));
                    }
                } else {
                    cls.setTimeSlot(timeSlots.get(random.nextInt(timeSlots.size())));
                }

                // Teacher mutation
                if (random.nextDouble() < 0.3) {
                    List<String> eligibleTeachers = getEligibleTeachers(cls.getSubject());
                    if (!eligibleTeachers.isEmpty()) {
                        cls.setTeacher(eligibleTeachers.get(random.nextInt(eligibleTeachers.size())));
                    }
                }
            }
        }
    }

    private List<String> getEligibleTeachers(String subject) {
        List<String> eligible = new ArrayList<>();
        for (String teacher : teachers) {
            if (teacherSubjects.getOrDefault(teacher, Collections.emptyList()).contains(subject)) {
                eligible.add(teacher);
            }
        }
        return eligible;
    }
}
