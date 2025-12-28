# Project Summary

MPI distributed genetic algorithm for school timetabling using island model.

## Features

- Island model with ring topology
- Tournament selection, crossover, mutation
- MPI communication: Bcast, Sendrecv, Reduce

## Usage

```bash
./gradlew build
mpirun -np 4 java -cp "build/classes/java/main:lib/mpi.jar" org.example.mpi.MPIMain
```

## Performance

- 4 ranks, 400 total population
- 500-1000ms to solution
- ~95% success rate
