package com.example.timetablescheduler.algorithm;

import com.example.timetablescheduler.models.*;
import java.util.*;

public class GeneticAlgorithm {
    private static final int POPULATION_SIZE = 120;
    private static final double CROSSOVER_RATE = 0.8;
    private static final double MUTATION_RATE = 0.10;
    private static final int MAX_GENERATIONS = 300;
    private static final double FITNESS_THRESHOLD = 0.995;
    private static final int ELITISM_COUNT = POPULATION_SIZE / 10;

    private final List<TimeSlot> timeSlots;
    private final Map<String, Integer> teacherLoads;
    private final Map<String, List<String>> teacherSubjects;
    private final List<String> teachers;
    private final int periodsPerDay;
    private final Set<String> breakSlotSet;

    public GeneticAlgorithm(List<TimeSlot> timeSlots, Map<String, Integer> teacherLoads,
                            Map<String, List<String>> teacherSubjects, List<String> teachers,
                            int periodsPerDay, Set<String> breakSlotSet) {
        this.timeSlots = timeSlots;
        this.teacherLoads = teacherLoads;
        this.teacherSubjects = teacherSubjects;
        this.teachers = teachers;
        this.periodsPerDay = periodsPerDay;
        this.breakSlotSet = breakSlotSet;
    }

    public Individual generateTimetable(List<TimetableClass> classes) {
        List<Individual> population = initializePopulation(classes);
        Individual bestSolution = null;
        int generation = 0;

        while (generation < MAX_GENERATIONS) {
            evaluatePopulation(population);
            Collections.sort(population, (i1, i2) -> Double.compare(i2.getFitness(), i1.getFitness()));
            Individual currentBest = population.get(0);
            if (bestSolution == null || currentBest.getFitness() > bestSolution.getFitness()) {
                bestSolution = currentBest.clone();
            }
            if (bestSolution.getFitness() >= FITNESS_THRESHOLD) break;

            List<Individual> newPopulation = new ArrayList<>();
            for (int i = 0; i < ELITISM_COUNT; i++) {
                newPopulation.add(population.get(i).clone());
            }
            while (newPopulation.size() < POPULATION_SIZE) {
                Individual parent1 = selectParentRoulette(population);
                Individual parent2 = selectParentRoulette(population);
                Individual offspring;
                if (Math.random() < CROSSOVER_RATE) {
                    offspring = crossover(parent1, parent2);
                } else {
                    offspring = parent1.clone();
                }
                mutate(offspring, classes);
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
                        getRandomValidSlot(cls, random),
                        cls.isLab(),
                        cls.getDuration(),
                        cls.getWeeklyLectures(),
                        cls.getWeeklyLabs()
                );
                individualClasses.add(newCls);
            }
            individual.setClasses(individualClasses);
            population.add(individual);
        }
        return population;
    }

    // Only assign labs to valid double-period slots (never last period, never across break)
    private TimeSlot getRandomValidSlot(TimetableClass cls, Random random) {
        if (cls.isLab() && cls.getDuration() == 2) {
            List<TimeSlot> validLabSlots = new ArrayList<>();
            for (TimeSlot slot : timeSlots) {
                if (slot.isBreak()) continue;
                if (slot.getPeriod() >= periodsPerDay) continue; // Can't start at last period
                String nextKey = slot.getDay() + "_" + (slot.getPeriod() + 1);
                if (breakSlotSet.contains(nextKey)) continue; // Next period is a break
                validLabSlots.add(slot);
            }
            if (validLabSlots.isEmpty()) return timeSlots.get(0); // Fallback
            return validLabSlots.get(random.nextInt(validLabSlots.size()));
        } else {
            List<TimeSlot> validSlots = new ArrayList<>();
            for (TimeSlot slot : timeSlots) {
                if (!slot.isBreak()) validSlots.add(slot);
            }
            return validSlots.get(random.nextInt(validSlots.size()));
        }
    }

    private void evaluatePopulation(List<Individual> population) {
        for (Individual individual : population) {
            individual.setFitness(calculateFitness(individual));
        }
    }

    private double calculateFitness(Individual individual) {
        int conflicts = 0;
        int totalConstraints = 0;
        Map<String, Set<String>> teacherSchedule = new HashMap<>();
        Map<String, Set<String>> batchSchedule = new HashMap<>();
        Map<String, Integer> subjectClassCount = new HashMap<>();

        for (TimetableClass cls : individual.getClasses()) {
            TimeSlot ts = cls.getTimeSlot();
            String teacherKey = cls.getTeacher();
            String batchKey = cls.getBatch() + "_" + cls.getSection();
            String timeKey = ts.getDay() + "_" + ts.getPeriod();

            // Teacher and batch conflict check
            if (!teacherSchedule.computeIfAbsent(teacherKey, k -> new HashSet<>()).add(timeKey)) conflicts++;
            if (!batchSchedule.computeIfAbsent(batchKey, k -> new HashSet<>()).add(timeKey)) conflicts++;

            // Count scheduled classes per subject per batch
            String subjBatchKey = cls.getSubject() + "_" + cls.getBatch() + "_" + (cls.isLab() ? "lab" : "lec");
            subjectClassCount.put(subjBatchKey, subjectClassCount.getOrDefault(subjBatchKey, 0) + 1);

            totalConstraints += 2;

            // Labs: Ensure double period and not across breaks
            if (cls.isLab() && cls.getDuration() == 2) {
                if (ts.getPeriod() >= periodsPerDay) conflicts++;
                String nextKey = ts.getDay() + "_" + (ts.getPeriod() + 1);
                if (breakSlotSet.contains(nextKey)) conflicts++;
                if (!teacherSchedule.get(teacherKey).add(nextKey)) conflicts++;
                if (!batchSchedule.get(batchKey).add(nextKey)) conflicts++;
                totalConstraints += 3;
            }
        }

        // Check for over/under scheduling of classes/labs per week
        Set<String> checked = new HashSet<>();
        for (TimetableClass cls : individual.getClasses()) {
            String subjBatchKey = cls.getSubject() + "_" + cls.getBatch() + "_" + (cls.isLab() ? "lab" : "lec");
            if (checked.contains(subjBatchKey)) continue;
            checked.add(subjBatchKey);
            int count = subjectClassCount.get(subjBatchKey);
            int required = cls.isLab() ? cls.getWeeklyLabs() : cls.getWeeklyLectures();
            if (count > required) conflicts += (count - required);
            if (count < required) conflicts += (required - count);
            totalConstraints++;
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

        // Subject-teacher eligibility
        for (TimetableClass cls : individual.getClasses()) {
            List<String> eligible = teacherSubjects.getOrDefault(cls.getTeacher(), Collections.emptyList());
            if (!eligible.contains(cls.getSubject())) {
                conflicts++;
                totalConstraints++;
            }
        }

        return (totalConstraints == 0) ? 1.0 : Math.max(0, 1.0 - (double) conflicts / totalConstraints);
    }

    private Individual selectParentRoulette(List<Individual> population) {
        double totalFitness = 0;
        for (Individual ind : population) totalFitness += ind.getFitness();
        double randomPoint = Math.random() * totalFitness;
        double currentSum = 0;
        for (Individual ind : population) {
            currentSum += ind.getFitness();
            if (currentSum >= randomPoint) return ind;
        }
        return population.get(0);
    }

    private Individual crossover(Individual parent1, Individual parent2) {
        Individual child = new Individual();
        List<TimetableClass> childClasses = new ArrayList<>();
        Random random = new Random();
        int size = parent1.getClasses().size();
        int point1 = random.nextInt(size);
        int point2 = random.nextInt(size - point1) + point1;
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

    private void mutate(Individual individual, List<TimetableClass> originalClasses) {
        Random random = new Random();
        for (int i = 0; i < individual.getClasses().size(); i++) {
            TimetableClass cls = individual.getClasses().get(i);
            if (random.nextDouble() < MUTATION_RATE) {
                cls.setTimeSlot(getRandomValidSlot(cls, random));
                // Teacher mutation
                if (random.nextDouble() < 0.3) {
                    List<String> eligibleTeachers = getEligibleTeachers(cls.getSubject());
                    if (!eligibleTeachers.isEmpty()) {
                        cls.setTeacher(eligibleTeachers.get(random.nextInt(eligibleTeachers.size())));
                    }
                }
            }
            // Ensure no duplicate or extra classes: match original subject/batch/section/lab
            TimetableClass orig = originalClasses.get(i);
            cls.setSubject(orig.getSubject());
            cls.setBatch(orig.getBatch());
            cls.setSection(orig.getSection());
            cls.setLab(orig.isLab());
            cls.setDuration(orig.getDuration());
            cls.setWeeklyLectures(orig.getWeeklyLectures());
            cls.setWeeklyLabs(orig.getWeeklyLabs());
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
