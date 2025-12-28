# Algorithms

## Problem Definition

**Input**: N classes, R rooms, T teachers, G student groups

**Output**: Timetable assigning each class to a timeslot and room

**Constraints**:
- No teacher/student/room conflicts
- Room capacity requirements met

## Genetic Algorithm

**Representation**: Individual = array of Genes
```
Gene = (classId, timeSlot, roomId)
```

**Operators**:
- **Selection**: Tournament (k=5)
- **Crossover**: One-point (80% rate)
- **Mutation**: Random timeslot/room (10% rate)
- **Elitism**: Preserve top 5

**Fitness**: Count of constraint violations (minimize to 0)

## Island Model

**Architecture**: Each MPI rank = independent population

**Migration**: Ring topology every 50 generations
```python
next_rank = (rank + 1) % num_ranks
prev_rank = (rank - 1 + num_ranks) % num_ranks

immigrant = MPI.Sendrecv(
    send_data=my_best,
    dest=next_rank,
    recv_source=prev_rank
)
```

## MPI Communication

| Operation | Primitive | Purpose |
|-----------|-----------|---------|
| Problem distribution | `Bcast` | Share problem data |
| Ring migration | `Sendrecv` | Exchange individuals |
| Global best | `Reduce(MIN)` | Find best fitness |
| Synchronization | `Barrier` | Coordinate completion |

## Complexity

- **Fitness**: O(N) per individual
- **Generation**: O(P × N) where P = population size
- **Migration**: O(N) + O(log R) where R = ranks
- **Total**: O(G × P × N) where G = generations
