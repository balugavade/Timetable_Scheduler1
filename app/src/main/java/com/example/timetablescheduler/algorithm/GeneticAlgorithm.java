package com.example.timetablescheduler.algorithm;

import com.example.timetablescheduler.models.*;
import java.util.*;

public class GeneticAlgorithm {
    private static final int POPULATION_SIZE = 100;
    private static final double MUTATION_RATE = 0.05;
    private static final int MAX_GENERATIONS = 1000;
    private static final int PERIODS_PER_DAY = 6;

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

        for (int gen = 0; gen < MAX_GENERATIONS; gen++) {
            evaluatePopulation(population);  // Fixed: Method now implemented
            Individual currentBest = getBestIndividual(population);

            if (bestSolution == null || currentBest.getFitness() > bestSolution.getFitness()) {
                bestSolution = currentBest.clone();
            }

            if (bestSolution.getFitness() >= 0.95) break;
            population = evolvePopulation(population);
        }
        return bestSolution;
    }

    // ADDED MISSING METHOD
    private void evaluatePopulation(List<Individual> population) {
        for (Individual individual : population) {
            individual.setFitness(calculateFitness(individual));
        }
    }

    private List<Individual> initializePopulation(List<TimetableClass> classes) {
        List<Individual> population = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < POPULATION_SIZE; i++) {
            Individual individual = new Individual();
            List<TimetableClass> individualClasses = new ArrayList<>();

            for (TimetableClass cls : classes) {
                TimeSlot selectedSlot;

                if (cls.isLab() && cls.getDuration() == 2) {
                    List<TimeSlot> validLabSlots = getConsecutiveSlots();
                    selectedSlot = validLabSlots.isEmpty() ?
                            timeSlots.get(random.nextInt(timeSlots.size())) :
                            validLabSlots.get(random.nextInt(validLabSlots.size()));
                } else {
                    selectedSlot = timeSlots.get(random.nextInt(timeSlots.size()));
                }

                TimetableClass newCls = new TimetableClass(
                        cls.getSubject(), cls.getTeacher(), cls.getBatch(),
                        cls.getSection(), selectedSlot, cls.isLab(), cls.getDuration()
                );
                individualClasses.add(newCls);
            }
            individual.setClasses(individualClasses);
            population.add(individual);
        }
        return population;
    }

    private List<TimeSlot> getConsecutiveSlots() {
        List<TimeSlot> validSlots = new ArrayList<>();
        for (TimeSlot slot : timeSlots) {
            int nextPeriod = slot.getPeriod() + 1;
            if (nextPeriod <= PERIODS_PER_DAY) {
                boolean hasNext = timeSlots.stream().anyMatch(s ->
                        s.getDay() == slot.getDay() && s.getPeriod() == nextPeriod);
                if (hasNext) validSlots.add(slot);
            }
        }
        return validSlots;
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

            for (int period = 0; period < cls.getDuration(); period++) {
                int timeKey = ts.getDay() * 100 + (ts.getPeriod() + period);

                if (!teacherSchedule.computeIfAbsent(teacherKey, k -> new HashSet<>()).add(timeKey)) {
                    conflicts++;
                }
                if (!batchSchedule.computeIfAbsent(batchKey, k -> new HashSet<>()).add(timeKey)) {
                    conflicts++;
                }
                totalConstraints += 2;
            }

            if (cls.isLab() && cls.getDuration() == 2) {
                int nextPeriod = ts.getPeriod() + 1;
                if (nextPeriod > PERIODS_PER_DAY) conflicts += 3;
            }
        }

        Map<String, Integer> teacherLoadCount = new HashMap<>();
        for (TimetableClass cls : individual.getClasses()) {
            teacherLoadCount.merge(cls.getTeacher(), cls.getDuration(), Integer::sum);
        }

        for (Map.Entry<String, Integer> entry : teacherLoadCount.entrySet()) {
            int assigned = entry.getValue();
            int maxAllowed = teacherLoads.getOrDefault(entry.getKey(), Integer.MAX_VALUE);
            if (assigned > maxAllowed) conflicts += (assigned - maxAllowed);
            totalConstraints++;
        }

        return (totalConstraints == 0) ? 1.0 : Math.max(0, 1.0 - (double) conflicts / totalConstraints);
    }

    private Individual getBestIndividual(List<Individual> population) {
        return Collections.max(population, Comparator.comparingDouble(Individual::getFitness));
    }

    private List<Individual> evolvePopulation(List<Individual> population) {
        List<Individual> newPopulation = new ArrayList<>();
        Random random = new Random();
        int eliteSize = (int) (POPULATION_SIZE * 0.1);
        population.sort(Comparator.comparingDouble(Individual::getFitness).reversed());
        for (int i = 0; i < eliteSize; i++) {
            newPopulation.add(population.get(i).clone());
        }
        while (newPopulation.size() < POPULATION_SIZE) {
            Individual parent1 = tournamentSelection(population);
            Individual parent2 = tournamentSelection(population);
            Individual child = crossover(parent1, parent2);
            mutate(child);
            newPopulation.add(child);
        }
        return newPopulation;
    }

    private Individual tournamentSelection(List<Individual> population) {
        Random random = new Random();
        List<Individual> tournament = new ArrayList<>();
        int tournamentSize = 5;
        for (int i = 0; i < tournamentSize; i++) {
            tournament.add(population.get(random.nextInt(population.size())));
        }
        return getBestIndividual(tournament);
    }

    private Individual crossover(Individual parent1, Individual parent2) {
        Individual child = new Individual();
        List<TimetableClass> childClasses = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < parent1.getClasses().size(); i++) {
            if (random.nextDouble() < 0.5) {
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
                if (random.nextDouble() < 0.8) {
                    if (cls.isLab() && cls.getDuration() == 2) {
                        List<TimeSlot> validLabSlots = getConsecutiveSlots();
                        if (!validLabSlots.isEmpty()) {
                            cls.setTimeSlot(validLabSlots.get(random.nextInt(validLabSlots.size())));
                        }
                    } else {
                        cls.setTimeSlot(timeSlots.get(random.nextInt(timeSlots.size())));
                    }
                } else {
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
