# Distributed Genetic Algorithm for Timetable Scheduling

A parallel genetic algorithm using MPI's island model to solve the school timetabling problem.

## Quick Start

```bash
# Build
./gradlew build

# Run with 4 MPI processes
mpirun -np 4 java -cp "build/classes/java/main:lib/mpi.jar" org.example.mpi.MPIMain
```

## Overview

Solves school timetabling with hard constraints:
- No teacher, student, or room conflicts
- Room capacity requirements met

### Island Model

```
Rank 0 -> Rank 1 -> Rank 2 -> Rank 3 -> Rank 0
(Ring topology with periodic migration)
```

## Implementation

**Genetic Algorithm**:
- Tournament selection, one-point crossover, mutation
- Elitism preserves best solutions
- Fitness = constraint violations (goal: 0)

**MPI Communication**:
- `Bcast`: Distribute problem
- `Sendrecv`: Migrate best individuals
- `Reduce`: Find global best

## Configuration

Edit `MPIMain.java`:

```java
POPULATION_SIZE = 100
MUTATION_RATE = 0.1
MAX_GENERATIONS = 1000
MIGRATION_INTERVAL = 50
```

## Project Structure

```
src/main/java/org/example/
├── common/model/    # Data models
├── common/ga/       # GA operators
└── mpi/             # MPI coordination
```
