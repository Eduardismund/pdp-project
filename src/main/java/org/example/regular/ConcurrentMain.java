package org.example.regular;

import org.example.common.ga.Individual;
import org.example.common.ga.Island;
import org.example.common.model.Gene;
import org.example.common.model.TimetableData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Concurrent (Shared Memory) Genetic Algorithm for timetable scheduling
 * Uses ExecutorService and Futures to run Islands in parallel.
 */
public class ConcurrentMain {

    // GA Parameters
    private static final int POPULATION_SIZE = 100;
    private static final double MUTATION_RATE = 0.1;
    private static final double CROSSOVER_RATE = 0.8;
    private static final int ELITE_COUNT = 5;
    private static final int MAX_GENERATIONS = 1000;
    private static final int MIGRATION_INTERVAL = 50;

    private static final int NUM_ISLANDS = 4;

    // Problem parameters
    private static final int NUM_CLASSES = 40;
    private static final int NUM_ROOMS = 8;
    private static final int NUM_TEACHERS = 10;
    private static final int NUM_GROUPS = 6;

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        long startTime = System.currentTimeMillis();

        System.out.println("=".repeat(70));
        System.out.println("CONCURRENT GENETIC ALGORITHM - TIMETABLE SCHEDULING");
        System.out.println("=".repeat(70));

        // 1. Generate Data
        TimetableData data = TimetableData.generateRandom(NUM_CLASSES, NUM_ROOMS, NUM_TEACHERS, NUM_GROUPS, 42);
        System.out.println("Problem: " + data);
        System.out.println("Threads (Islands): " + NUM_ISLANDS);
        System.out.println("Total Population: " + (POPULATION_SIZE * NUM_ISLANDS));

        // 2. Initialize Islands
        List<Island> islands = new ArrayList<>();
        for (int i = 0; i < NUM_ISLANDS; i++) {
            // Unique seed for each island
            long seed = 12345L + i * 1000;
            islands.add(new Island(data, POPULATION_SIZE, MUTATION_RATE, CROSSOVER_RATE, ELITE_COUNT, seed));
        }

        // 3. Create Thread Pool
        ExecutorService executor = Executors.newFixedThreadPool(NUM_ISLANDS);

        boolean foundPerfect = false;
        Individual globalBest = null;

        System.out.println("\nStarting evolution...\n");

        // 4. Evolution Loop
        for (int generation = 0; generation < MAX_GENERATIONS && !foundPerfect; generation++) {

            // Define the task: Each island runs one evolution step
            List<Callable<Void>> tasks = new ArrayList<>();
            for (Island island : islands) {
                tasks.add(() -> {
                    island.evolve();
                    return null;
                });
            }

            // Run all islands in parallel and wait for them to finish this generation
            executor.invokeAll(tasks);

            // Periodically migrate and report
            if (generation % MIGRATION_INTERVAL == 0) {
                // Migration: Ring topology in shared memory
                performMigration(islands);
            }

            // Check progress every 50 gens
            if (generation % 50 == 0) {
                globalBest = getGlobalBest(islands);
                double avgFitness = islands.stream().mapToDouble(Island::getAverageFitness).average().orElse(0);

                System.out.printf("Gen %4d | Global Best Fitness=%3d | Global Avg Fitness=%6.2f",
                        generation, globalBest.getFitness(), avgFitness);

                if (globalBest.isPerfect()) {
                    System.out.println(" ✓ PERFECT SOLUTION FOUND!");
                    foundPerfect = true;
                } else {
                    System.out.println();
                }
            }
        }

        // 5. Cleanup and Report
        executor.shutdown();
        long endTime = System.currentTimeMillis();

        System.out.println("\n" + "=".repeat(70));
        System.out.println("EVOLUTION COMPLETED");
        System.out.println("=".repeat(70));
        System.out.println("Total time: " + (endTime - startTime) + " ms");

        if (globalBest == null) globalBest = getGlobalBest(islands);

        System.out.println("Best solution fitness: " + globalBest.getFitness());

        if (globalBest.isPerfect()) {
            System.out.println("\n✓ Found valid timetable with no conflicts!");
            printTimetable(globalBest, data);
        } else {
            System.out.println("\nBest solution has " + globalBest.getFitness() + " violations.");
        }
    }

    /**
     * Performs migration between islands using a ring topology.
     * Island 0 -> Island 1 -> ... -> Island N -> Island 0
     */
    private static void performMigration(List<Island> islands) {
        // We capture the best individuals *before* we start modifying islands
        List<Individual> migrants = new ArrayList<>();
        for (Island island : islands) {
            // IMPORTANT: Create a copy, otherwise the gene references might be modified
            // by the source island while the destination island is using them.
            migrants.add(island.getBest().copy());
        }

        for (int i = 0; i < islands.size(); i++) {
            Island destination = islands.get((i + 1) % islands.size());
            Individual immigrant = migrants.get(i);
            destination.receiveImmigrant(immigrant);
        }
    }

    /**
     * Scan all islands to find the absolute best individual
     */
    private static Individual getGlobalBest(List<Island> islands) {
        Individual best = islands.get(0).getBest();
        for (Island island : islands) {
            Individual candidate = island.getBest();
            if (candidate.getFitness() < best.getFitness()) {
                best = candidate;
            }
        }
        return best;
    }

    /**
     * Helper to print the timetable (Identical to MPI version for consistency)
     */
    private static void printTimetable(Individual solution, TimetableData data) {
        System.out.println("\nTIMETABLE:");
        System.out.println("-".repeat(70));

        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
        Gene[] genes = solution.getGenes();

        for (int day = 0; day < 5; day++) {
            System.out.println("\n" + days[day].toUpperCase());
            for (int hour = 0; hour < 8; hour++) {
                System.out.printf("  %2d:00 - ", (8 + hour));

                boolean found = false;
                for (Gene gene : genes) {
                    if (gene.getTimeSlot().getDay() == day && gene.getTimeSlot().getHour() == hour) {
                        var cls = data.getClass(gene.getClassId());
                        System.out.printf("[%s, T%d, G%d, R%d] ",
                                cls.getSubject(), cls.getTeacherId(), cls.getStudentGroup(), gene.getRoomId());
                        found = true;
                    }
                }
                if (!found) {
                    System.out.print("-");
                }
                System.out.println();
            }
        }
    }
}