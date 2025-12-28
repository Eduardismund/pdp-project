package org.example.common.ga;

import lombok.Getter;
import org.example.common.model.*;

import java.io.Serializable;
import java.util.*;

/**
 * Represents one complete timetable (a candidate solution)
 * An individual is an array of genes, one gene per class to schedule
 */
public class Individual implements Serializable, Comparable<Individual> {
    private static final long serialVersionUID = 1L;

    @Getter
    private final Gene[] genes;
    private int fitness = -1; // -1 means not calculated yet
    private final TimetableData data;

    /**
     * Create individual with given genes
     */
    public Individual(Gene[] genes, TimetableData data) {
        this.genes = genes;
        this.data = data;
    }

    /**
     * Create random individual
     */
    public static Individual createRandom(TimetableData data, Random rand) {
        List<SchoolClass> classes = data.getClasses();
        Gene[] genes = new Gene[classes.size()];

        for (int i = 0; i < classes.size(); i++) {
            // Random timeslot
            int day = rand.nextInt(data.getDaysPerWeek());
            int hour = rand.nextInt(data.getHoursPerDay());
            TimeSlot timeSlot = new TimeSlot(day, hour);

            // Random room
            int roomId = rand.nextInt(data.getRooms().size());

            genes[i] = new Gene(i, timeSlot, roomId);
        }

        return new Individual(genes, data);
    }

    /**
     * Calculate fitness (lower is better, 0 = perfect solution)
     * Fitness = number of constraint violations
     */
    public int calculateFitness() {
        int violations = 0;

        // Constraint 1: Teacher conflict (teacher in two places at same time)
        violations += countTeacherConflicts();

        // Constraint 2: Student group conflict (group in two places at same time)
        violations += countStudentConflicts();

        // Constraint 3: Room conflict (two classes in same room at same time)
        violations += countRoomConflicts();

        // Constraint 4: Room capacity (class size > room capacity)
        violations += countCapacityViolations();

        this.fitness = violations;
        return violations;
    }

    private int countTeacherConflicts() {
        int conflicts = 0;
        // For each timeslot, check if any teacher teaches multiple classes
        Map<Integer, Set<Integer>> teacherAtTime = new HashMap<>();

        for (Gene gene : genes) {
            SchoolClass cls = data.getClass(gene.getClassId());
            int timeSlotId = gene.getTimeSlot().getAbsoluteSlot();

            teacherAtTime.putIfAbsent(timeSlotId, new HashSet<>());

            if (teacherAtTime.get(timeSlotId).contains(cls.getTeacherId())) {
                conflicts++; // Teacher already teaching at this time
            }
            teacherAtTime.get(timeSlotId).add(cls.getTeacherId());
        }

        return conflicts;
    }

    private int countStudentConflicts() {
        int conflicts = 0;
        // For each timeslot, check if any student group has multiple classes
        Map<Integer, Set<Integer>> groupAtTime = new HashMap<>();

        for (Gene gene : genes) {
            SchoolClass cls = data.getClass(gene.getClassId());
            int timeSlotId = gene.getTimeSlot().getAbsoluteSlot();

            groupAtTime.putIfAbsent(timeSlotId, new HashSet<>());

            if (groupAtTime.get(timeSlotId).contains(cls.getStudentGroup())) {
                conflicts++; // Group already has class at this time
            }
            groupAtTime.get(timeSlotId).add(cls.getStudentGroup());
        }

        return conflicts;
    }

    private int countRoomConflicts() {
        int conflicts = 0;
        // For each timeslot, check if any room is used by multiple classes
        Map<Integer, Set<Integer>> roomAtTime = new HashMap<>();

        for (Gene gene : genes) {
            int timeSlotId = gene.getTimeSlot().getAbsoluteSlot();
            int roomId = gene.getRoomId();

            roomAtTime.putIfAbsent(timeSlotId, new HashSet<>());

            if (roomAtTime.get(timeSlotId).contains(roomId)) {
                conflicts++; // Room already occupied at this time
            }
            roomAtTime.get(timeSlotId).add(roomId);
        }

        return conflicts;
    }

    private int countCapacityViolations() {
        int violations = 0;

        for (Gene gene : genes) {
            SchoolClass cls = data.getClass(gene.getClassId());
            Room room = data.getRoom(gene.getRoomId());

            if (cls.getRequiredCapacity() > room.getCapacity()) {
                violations++; // Class too big for room
            }
        }

        return violations;
    }

    /**
     * One-point crossover with another individual
     */
    public Individual crossover(Individual other, Random rand) {
        int crossoverPoint = rand.nextInt(genes.length);
        Gene[] childGenes = new Gene[genes.length];

        for (int i = 0; i < genes.length; i++) {
            childGenes[i] = i < crossoverPoint ? this.genes[i] : other.genes[i];
        }

        return new Individual(childGenes, data);
    }

    /**
     * Mutate this individual by randomly changing some genes
     */
    public void mutate(double mutationRate, Random rand) {
        for (int i = 0; i < genes.length; i++) {
            if (rand.nextDouble() < mutationRate) {
                // Mutate this gene: change timeslot or room
                if (rand.nextBoolean()) {
                    // Change timeslot
                    int day = rand.nextInt(data.getDaysPerWeek());
                    int hour = rand.nextInt(data.getHoursPerDay());
                    TimeSlot newTimeSlot = new TimeSlot(day, hour);
                    genes[i] = new Gene(genes[i].getClassId(), newTimeSlot, genes[i].getRoomId());
                } else {
                    // Change room
                    int newRoom = rand.nextInt(data.getRooms().size());
                    genes[i] = new Gene(genes[i].getClassId(), genes[i].getTimeSlot(), newRoom);
                }
                this.fitness = -1; // Invalidate fitness
            }
        }
    }

    public int getFitness() {
        if (fitness == -1) {
            calculateFitness();
        }
        return fitness;
    }

    public boolean isPerfect() {
        return getFitness() == 0;
    }

    @Override
    public int compareTo(Individual other) {
        return Integer.compare(this.getFitness(), other.getFitness());
    }

    @Override
    public String toString() {
        return "Individual{fitness=" + getFitness() + "}";
    }

    /**
     * Deep copy
     */
    public Individual copy() {
        Gene[] newGenes = new Gene[genes.length];
        System.arraycopy(genes, 0, newGenes, 0, genes.length);
        Individual copy = new Individual(newGenes, data);
        copy.fitness = this.fitness;
        return copy;
    }
}
