# School Timetable Generation: Distributed vs. Concurrent Genetic Algorithms

## 1. Project Overview
This project solves the **School Timetabling Problem** using a parallel **Genetic Algorithm (GA)**. The goal is to assign classes to time slots and rooms while minimizing constraint violations such as teacher conflicts, room double-booking, and capacity limits.

Two parallel implementations are provided to demonstrate different computing paradigms:
1.  **Distributed (MPI):** Uses the Message Passing Interface for distributed memory systems (clusters).
2.  **Concurrent (Threads):** Uses Java's `ExecutorService` for shared memory systems (multi-core single machine).

---

## 2. The Algorithm: Island Model GA
Both implementations utilize the **Island Model**. Instead of a single massive population, the population is split into smaller sub-populations ("islands").



### Core Mechanics
* **Evolution:** Each island evolves independently using standard GA operators:
    * **Selection:** Tournament selection (picking the best of $k$ random individuals).
    * **Crossover:** One-point crossover to combine "parent" schedules.
    * **Mutation:** Randomly changing time slots or rooms to maintain diversity.
    * **Elitism:** Automatically preserving the top $N$ individuals to the next generation.
* **Migration (Ring Topology):** Every `MIGRATION_INTERVAL` generations, islands exchange their best solutions.
    * Island $i$ sends its best individual to Island $(i+1)$.
    * Island $i$ receives an immigrant from Island $(i-1)$ and replaces its worst individual.

---

## 3. Implementation Comparison

### A. Distributed Implementation (`org.example.mpi.MPIMain`)
Designed for **distributed systems** where nodes do not share memory.
* **Technology:** OpenMPI / Java bindings for MPI.
* **Communication:** Explicit message passing via `MPI.COMM_WORLD`.
* **Data Transfer:** Objects must be **serialized** into primitive arrays (`int[]`) to be sent over the network or inter-process bus.
* **Synchronization:** Uses `MPI.COMM_WORLD.barrier()` and collective operations like `reduce` to find the global best fitness.

> **Key Difference:** High overhead due to manual serialization of the `Individual` class into a flat integer array.

### B. Concurrent Implementation (`org.example.regular.ConcurrentMain`)
Designed for a **single multi-core machine**.
* **Technology:** Java `java.util.concurrent` (ExecutorService, Callable).
* **Communication:** Shared memory access.
* **Data Transfer:** Direct object references. To prevent race conditions, we use `Individual.copy()` to pass deep-copied individuals between islands.
* **Synchronization:** Uses `executor.invokeAll()` which acts as a barrier, ensuring all threads complete a generation before migration begins.

---

## 4. Performance Analysis

| Feature | MPI Implementation | Concurrent Implementation |
| :--- | :--- | :--- |
| **Bottleneck** | **Serialization & IPC Latency.** Converting objects to `int[]` and inter-process communication. | **Thread Management.** OS-level context switching and thread scheduling. |
| **Scalability** | **High.** Can scale to hundreds of physical nodes in a cluster. | **Limited.** Bounded by the number of CPU cores on a single machine. |
| **Migration Cost**| **Expensive.** Requires serialization $\rightarrow$ transmission $\rightarrow$ deserialization. | **Cheap.** Requires only a memory allocation (deep copy). |
| **Setup Complexity**| High (Requires MPI environment and configuration). | Low (Standard Java SDK). |

### Performance Verdict
For the provided problem size (100 Classes, 4 Islands, 1000 Generations):
* **The Concurrent version is expected to be faster.** * **Reasoning:** On a single computer, the cost of MPI process synchronization and data serialization is much higher than the cost of simple memory copies and thread management. MPI's strengths only become apparent when the problem is too large for a single RAM/CPU complex.

---

## 5. How to Run

### Prerequisites
* Java 11 or higher
* Maven or Gradle
* (For MPI) OpenMPI installed and `mpi.jar` available on the classpath.

### Running the Concurrent Version
You can run this as a standard Java application from your IDE or the command line:
```bash
java -cp target/classes org.example.regular.ConcurrentMain
```

### Running the MPI Version
Use the mpirun command to launch the distributed processes (e.g., for 4 islands):

```bash
mpirun -np 4 java -cp target/classes:path/to/mpi.jar org.example.mpi.MPIMain
```
