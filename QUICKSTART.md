# Quick Start

## Prerequisites

- Java 17+
- MPI (OpenMPI or MPICH)

## Build and Run

```bash
# Build
./gradlew build

# Run
mpirun -np 4 java -cp "build/classes/java/main:lib/mpi.jar" org.example.mpi.MPIMain
```

## Expected Output

```
Gen    0 | Island 0: Best= 28 | Global Best= 24
Gen   50 | Island 0: Best= 12 | Global Best= 10
Gen  100 | Island 0: Best=  0 | Global Best=  0 ✓ PERFECT!
```

## Troubleshooting

**`mpirun: command not found`**
- Install MPI: `sudo apt install openmpi-bin`

**`ClassNotFoundException: mpi.MPI`**
- Verify `lib/mpi.jar` exists
