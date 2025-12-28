package org.example.mpi;

import mpi.MPI;
import org.example.common.ga.Individual;
import org.example.common.ga.Island;
import org.example.common.model.Gene;
import org.example.common.model.TimeSlot;
import org.example.common.model.TimetableData;

/**
 * MPI-based distributed genetic algorithm for timetable scheduling
 *
 * ISLAND MODEL:
 * - Each MPI rank = one island with its own population
 * - Islands evolve independently
 * - Periodic migration: best individual sent to next rank in ring topology
 * - Rank 0 collects global best and prints progress
 */
public class MPIMain {
    private static final int TAG_MIGRATE_FITNESS = 1;
    private static final int TAG_MIGRATE_GENES = 2;

    // GA Parameters
    private static final int POPULATION_SIZE = 100;
    private static final double MUTATION_RATE = 0.1;
    private static final double CROSSOVER_RATE = 0.8;
    private static final int ELITE_COUNT = 5;
    private static final int MAX_GENERATIONS = 1000;
    private static final int MIGRATION_INTERVAL = 50; // Migrate every N generations

    // Problem parameters
    private static final int NUM_CLASSES = 40;
    private static final int NUM_ROOMS = 8;
    private static final int NUM_TEACHERS = 10;
    private static final int NUM_GROUPS = 6;

    public static void main(String[] args) throws mpi.MPIException {
        // Initialize MPI
        MPI.Init(args);

        int rank = MPI.COMM_WORLD.getRank();
        int size = MPI.COMM_WORLD.getSize();

        long startTime = System.currentTimeMillis();

        // Step 1: Rank 0 creates problem instance and broadcasts to all
        TimetableData data = broadcastProblemData(rank, size);

        if (rank == 0) {
            System.out.println("=".repeat(70));
            System.out.println("DISTRIBUTED GENETIC ALGORITHM - TIMETABLE SCHEDULING");
            System.out.println("=".repeat(70));
            System.out.println("Problem: " + data);
            System.out.println("MPI Ranks (Islands): " + size);
            System.out.println("Population per island: " + POPULATION_SIZE);
            System.out.println("Total individuals: " + (POPULATION_SIZE * size));
            System.out.println("Migration interval: " + MIGRATION_INTERVAL + " generations");
            System.out.println("=".repeat(70));
        }

        // Step 2: Each rank creates its own island with unique seed
        long seed = 12345L + rank * 1000;
        Island myIsland = new Island(data, POPULATION_SIZE, MUTATION_RATE, CROSSOVER_RATE, ELITE_COUNT, seed);

        if (rank == 0) {
            System.out.println("\nStarting evolution...\n");
        }

        // Step 3: Evolution loop
        boolean foundPerfect = false;

        for (int generation = 0; generation < MAX_GENERATIONS && !foundPerfect; generation++) {
            // Evolve local island
            myIsland.evolve();

            // Migration: send best to next rank, receive from previous rank
            if (generation > 0 && generation % MIGRATION_INTERVAL == 0) {
                migrateBestIndividuals(rank, size, myIsland, data);
            }

            // Every 50 generations: rank 0 collects and prints global best
            if (generation % 50 == 0) {
                int globalBestFitness = collectGlobalBest(rank, size, myIsland);

                if (rank == 0) {
                    Individual localBest = myIsland.getBest();
                    double avgFitness = myIsland.getAverageFitness();

                    System.out.printf("Gen %4d | Island 0: Best=%3d Avg=%6.2f | Global Best=%3d",
                            generation, localBest.getFitness(), avgFitness, globalBestFitness);

                    if (globalBestFitness == 0) {
                        System.out.println(" ✓ PERFECT SOLUTION FOUND!");
                        foundPerfect = true;
                    } else {
                        System.out.println();
                    }
                }

                // Broadcast if perfect solution found
                int[] foundFlag = new int[]{foundPerfect ? 1 : 0};
                MPI.COMM_WORLD.bcast(foundFlag, 1, MPI.INT, 0);
                foundPerfect = foundFlag[0] == 1;
            }
        }

        // Step 4: Final report
        MPI.COMM_WORLD.barrier();

        if (rank == 0) {
            long endTime = System.currentTimeMillis();
            System.out.println("\n" + "=".repeat(70));
            System.out.println("EVOLUTION COMPLETED");
            System.out.println("=".repeat(70));
            System.out.println("Total time: " + (endTime - startTime) + " ms");

            Individual finalBest = myIsland.getBest();
            System.out.println("Best solution fitness: " + finalBest.getFitness());

            if (finalBest.isPerfect()) {
                System.out.println("\n✓ Found valid timetable with no conflicts!");
                printTimetable(finalBest, data);
            } else {
                System.out.println("\nBest solution has " + finalBest.getFitness() + " constraint violations.");
                System.out.println("Try running longer or increasing population size.");
            }
        }

        // Finalize MPI
        MPI.Finalize();
    }

    /**
     * Rank 0 creates problem and broadcasts to all ranks
     * Since MPI.OBJECT is not supported, we just create the same data on each rank
     */
    private static TimetableData broadcastProblemData(int rank, int size) throws mpi.MPIException {
        // All ranks create identical problem with same seed
        // This avoids need for object serialization
        return TimetableData.generateRandom(NUM_CLASSES, NUM_ROOMS, NUM_TEACHERS, NUM_GROUPS, 42);
    }

    /**
     * Ring topology migration: each rank sends best to (rank+1) % size
     * and receives from (rank-1+size) % size
     *
     * We send Individual as serialized genes (int arrays)
     */
    private static void migrateBestIndividuals(int rank, int size, Island myIsland, TimetableData data) throws mpi.MPIException {
        int nextRank = (rank + 1) % size;
        int prevRank = (rank - 1 + size) % size;

        Individual myBest = myIsland.getBest();

        // Serialize individual to int arrays:
        // Format: [fitness, classId0, day0, hour0, roomId0, classId1, day1, hour1, roomId1, ...]
        // Size: 1 + NUM_CLASSES * 4
        int[] sendData = serializeIndividual(myBest);
        int[] recvData = new int[sendData.length];

        // Use separate send and recv to avoid deadlock in ring topology
        // Even ranks send first, odd ranks receive first
        if (rank % 2 == 0) {
            MPI.COMM_WORLD.send(sendData, sendData.length, MPI.INT, nextRank, TAG_MIGRATE_GENES);
            MPI.COMM_WORLD.recv(recvData, recvData.length, MPI.INT, prevRank, TAG_MIGRATE_GENES);
        } else {
            MPI.COMM_WORLD.recv(recvData, recvData.length, MPI.INT, prevRank, TAG_MIGRATE_GENES);
            MPI.COMM_WORLD.send(sendData, sendData.length, MPI.INT, nextRank, TAG_MIGRATE_GENES);
        }

        // Deserialize and integrate immigrant
        Individual immigrant = deserializeIndividual(recvData, data);
        myIsland.receiveImmigrant(immigrant);
    }

    /**
     * Serialize individual to int array
     */
    private static int[] serializeIndividual(Individual ind) {
        Gene[] genes = ind.getGenes();
        // Format: [fitness, gene0_classId, gene0_day, gene0_hour, gene0_room, ...]
        int[] data = new int[1 + genes.length * 4];
        data[0] = ind.getFitness();

        for (int i = 0; i < genes.length; i++) {
            Gene gene = genes[i];
            data[1 + i * 4] = gene.getClassId();
            data[1 + i * 4 + 1] = gene.getTimeSlot().getDay();
            data[1 + i * 4 + 2] = gene.getTimeSlot().getHour();
            data[1 + i * 4 + 3] = gene.getRoomId();
        }

        return data;
    }

    /**
     * Deserialize int array to individual
     */
    private static Individual deserializeIndividual(int[] data, TimetableData timetableData) {
        int numGenes = (data.length - 1) / 4;
        Gene[] genes = new Gene[numGenes];

        for (int i = 0; i < numGenes; i++) {
            int classId = data[1 + i * 4];
            int day = data[1 + i * 4 + 1];
            int hour = data[1 + i * 4 + 2];
            int roomId = data[1 + i * 4 + 3];

            genes[i] = new Gene(classId, new TimeSlot(day, hour), roomId);
        }

        Individual ind = new Individual(genes, timetableData);
        ind.calculateFitness(); // Recalculate fitness
        return ind;
    }

    /**
     * Collect global best fitness using MPI_Reduce
     */
    private static int collectGlobalBest(int rank, int size, Island myIsland) throws mpi.MPIException {
        int localBestFitness = myIsland.getBest().getFitness();
        int[] localBest = new int[]{localBestFitness};
        int[] globalBest = new int[1];

        // Reduce to find minimum fitness across all ranks
        MPI.COMM_WORLD.reduce(localBest, globalBest, 1, MPI.INT, MPI.MIN, 0);

        return rank == 0 ? globalBest[0] : localBestFitness;
    }

    /**
     * Print the timetable in a readable format
     */
    private static void printTimetable(Individual solution, TimetableData data) {
        System.out.println("\nTIMETABLE:");
        System.out.println("-".repeat(70));

        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
        Gene[] genes = solution.getGenes();

        // Group by day and hour
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
