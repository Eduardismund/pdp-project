# MPI Communication Patterns

## Overview

| Primitive | Purpose | When |
|-----------|---------|------|
| `Bcast` | Distribute problem data | Program start |
| `Sendrecv` | Ring migration | Every 50 generations |
| `Reduce(MIN)` | Find global best | Every 50 generations |
| `Barrier` | Synchronize completion | Before final report |

## 1. Broadcast Problem Data

**Pattern**: One-to-all distribution from rank 0

```java
if (rank == 0) {
    dataArray[0] = TimetableData.generateRandom(...);
}
MPI.COMM_WORLD.bcast(dataArray, 1, MPI.OBJECT, 0);
```

**Complexity**: O(log P) via tree-based broadcast

## 2. Ring Migration

**Pattern**: Ring topology exchange

```
Rank 0 -> Rank 1 -> Rank 2 -> Rank 3 -> Rank 0
```

```java
int nextRank = (rank + 1) % size;
int prevRank = (rank - 1 + size) % size;

MPI.COMM_WORLD.sendrecv(
    sendData, sendData.length, MPI.INT, nextRank, TAG,
    recvData, recvData.length, MPI.INT, prevRank, TAG
);
```

**Why Sendrecv**: Avoids deadlock in circular communication

**Data format**: Serialized as int[] array
- `[fitness, class0_id, class0_day, class0_hour, class0_room, ...]`
- Size: ~644 bytes for 40 classes

## 3. Global Best Reduction

**Pattern**: All-to-one reduction to rank 0

```java
MPI.COMM_WORLD.reduce(localBest, globalBest, 1, MPI.INT, MPI.MIN, 0);
```

**Complexity**: O(log P) via reduction tree

## 4. Barrier Synchronization

**Pattern**: Wait for all ranks

```java
MPI.COMM_WORLD.barrier();
if (rank == 0) {
    printResults();
}
```

## Communication Overhead

### Total data (1000 generations, 4 ranks)

| Operation | Frequency | Data | Total |
|-----------|-----------|------|-------|
| Broadcast | 1 | 8 KB | 8 KB |
| Migration | 20 × 4 | 644 B | 52 KB |
| Reduce | 20 | 4 B | 80 B |
| **Total** | - | - | **~60 KB** |

**Communication time**: ~5ms
**Compute time**: ~7000ms
**Overhead**: <1% (compute-bound)

## Deadlock Avoidance

**Problem**: Ring send/recv can deadlock
```java
// BAD - all ranks block on send
send(..., nextRank);
recv(..., prevRank);
```

**Solution**: Use sendrecv
```java
// GOOD - MPI handles synchronization
sendrecv(..., nextRank, ..., prevRank);
```

## Topology Comparison

| Topology | Messages/rank | Scalability |
|----------|---------------|-------------|
| Ring | 2 | O(P) |
| Complete | 2×(P-1) | O(P²) |
| Star | Varies | Bottleneck at master |

**Ring chosen for**: Scalability + load balance + no bottleneck
