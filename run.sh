#!/bin/bash
# Simple run script for MPI Genetic Algorithm
# Usage: ./run.sh [num_processes]

NUM_PROCS=${1:-4}  # Default to 4 processes if not specified

echo "Building project..."
./gradlew build

if [ $? -eq 0 ]; then
    echo "Running with $NUM_PROCS MPI processes..."
    mpirun -np $NUM_PROCS java -cp "build/classes/java/main:lib/mpi.jar" org.example.mpi.MPIMain
else
    echo "Build failed!"
    exit 1
fi
