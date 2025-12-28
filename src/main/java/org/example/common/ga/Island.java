package org.example.common.ga;

import lombok.Getter;
import org.example.common.model.TimetableData;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * An island maintains a population and evolves it using genetic operators
 * Each MPI rank runs one island independently
 */
public class Island {
    @Getter
    private List<Individual> population;
    private final TimetableData data;
    private final Random rand;
    private final int populationSize;
    private final double mutationRate;
    private final double crossoverRate;
    private final int eliteCount; // Number of best individuals to preserve

    public Island(TimetableData data, int populationSize, double mutationRate, double crossoverRate, int eliteCount, long seed) {
        this.data = data;
        this.populationSize = populationSize;
        this.mutationRate = mutationRate;
        this.crossoverRate = crossoverRate;
        this.eliteCount = eliteCount;
        this.rand = new Random(seed);
        this.population = new ArrayList<>(populationSize);
        initializePopulation();
    }

    /**
     * Create random initial population
     */
    private void initializePopulation() {
        for (int i = 0; i < populationSize; i++) {
            population.add(Individual.createRandom(data, rand));
        }
        evaluatePopulation();
    }

    /**
     * Calculate fitness for all individuals
     */
    private void evaluatePopulation() {
        for (Individual ind : population) {
            ind.calculateFitness();
        }
    }

    /**
     * Evolve population for one generation
     * Uses elitism + tournament selection + crossover + mutation
     */
    public synchronized void evolve() {
        List<Individual> newPopulation = new ArrayList<>(populationSize);

        // Sort population by fitness (best first)
        population.sort(Individual::compareTo);

        // ELITISM: Keep the best individuals unchanged
        for (int i = 0; i < eliteCount; i++) {
            newPopulation.add(population.get(i).copy());
        }

        // Fill rest of population with offspring
        while (newPopulation.size() < populationSize) {
            // SELECTION: Tournament selection
            Individual parent1 = tournamentSelection(5);
            Individual parent2 = tournamentSelection(5);

            Individual child;

            // CROSSOVER
            if (rand.nextDouble() < crossoverRate) {
                child = parent1.crossover(parent2, rand);
            } else {
                child = parent1.copy();
            }

            // MUTATION
            child.mutate(mutationRate, rand);

            newPopulation.add(child);
        }

        population = newPopulation;
        evaluatePopulation();
    }

    /**
     * Tournament selection: pick k random individuals, return the best
     */
    private Individual tournamentSelection(int tournamentSize) {
        Individual best = population.get(rand.nextInt(population.size()));

        for (int i = 1; i < tournamentSize; i++) {
            Individual competitor = population.get(rand.nextInt(population.size()));
            if (competitor.getFitness() < best.getFitness()) {
                best = competitor;
            }
        }

        return best;
    }

    /**
     * Get the best individual in this island
     */
    public synchronized Individual getBest() {
        return population.stream()
                .min(Individual::compareTo)
                .orElse(null);
    }

    /**
     * Get worst individual in this island
     */
    public synchronized Individual getWorst() {
        return population.stream()
                .max(Individual::compareTo)
                .orElse(null);
    }

    /**
     * Replace worst individual with immigrant from another island
     */
    public synchronized void receiveImmigrant(Individual immigrant) {
        // Find worst individual and replace it
        int worstIndex = 0;
        int worstFitness = population.get(0).getFitness();

        for (int i = 1; i < population.size(); i++) {
            if (population.get(i).getFitness() > worstFitness) {
                worstFitness = population.get(i).getFitness();
                worstIndex = i;
            }
        }

        // Only replace if immigrant is better
        if (immigrant.getFitness() < worstFitness) {
            population.set(worstIndex, immigrant);
        }
    }

    /**
     * Get average fitness of population
     */
    public synchronized double getAverageFitness() {
        return population.stream()
                .mapToInt(Individual::getFitness)
                .average()
                .orElse(0.0);
    }

    /**
     * Check if any individual is perfect (fitness = 0)
     */
    public synchronized boolean hasPerfectSolution() {
        return getBest().isPerfect();
    }
}
