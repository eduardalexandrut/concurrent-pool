# Performance Evaluation

!!! note "To be completed"
    This chapter describes how the performance is measured and how the results are presented. The parts marked
    *TODO* must be filled with the measurements taken on the test machine.

The method follows the lab notes *Performance of Concurrent Programs*: first make the program correct, then measure
(not guess) on representative use cases, looking both at the average (throughput) and at the variance
(responsiveness).

## 1. What is measured
The activity to measure is the computation of one frame, `computeState(dt)`. It is a CPU-bound task (no I/O), so the
goal is to keep the CPUs busy with useful work.

The questions are:

1. **How fast** is a frame on average, and how many frames stay within the 16.6 ms needed for 60 FPS?
2. **Scalability:** how does the speedup change when threads are added? Which number of threads is the best?
3. **How much:** what is the largest number of balls that each version can handle within 16.6 ms? With few balls, is
   the concurrent version still faster than the sequential one?
4. **Which version** is better: platform threads with the barrier, or the `ExecutorService`?

## 2. Metrics
| Metric | Definition |
|---|---|
| Frame time | time of one `computeState(16)`, measured with `System.nanoTime()` |
| Throughput | mean frame time and frames per second (1000 / mean) |
| Responsiveness | standard deviation, 95th and 99th percentile, maximum, and the percentage of frames under 16.6 ms |
| Speedup | S(p) = T(sequential) / T(p), where T(sequential) is measured with `SequentialPhysics` |
| Efficiency | E(p) = S(p) / p |

## 3. Use cases
The measurements use the real engine (the two player balls, the holes and the scores are included), without the GUI
and without the 16 ms sleep of the game loop, on the default board (1600x1200, grid 20x20).

| Use case | Balls | Why |
|---|---|---|
| Default game | 1,000 | default value of the launcher |
| Heavy game | 5,000 | crowded table |
| Stress | 1,000 to 40,000 | to find the limit of each version |

The balls are created from a `Random` with a fixed seed and passed to `new Board(width, height, balls)`, so every
version simulates exactly the same table. The default number of threads is `Runtime.getRuntime().availableProcessors()`
(Ncpu), not a hard-coded value.

## 4. Measurement loop
```java
Random rand = new Random(42);
List<Ball> balls = new ArrayList<>();
for (int i = 0; i < numBalls; i++) {
    double x = 20 + rand.nextDouble() * 1560;
    double y = 20 + rand.nextDouble() * 1160;
    balls.add(new Ball(new P2d(x, y), 5, 1.0, new V2d(rand.nextDouble() * 4 - 2, rand.nextDouble() * 4 - 2)));
}
Physics physics = new MultiThreadPhysics(new Board(1600, 1200, balls), 20, 20, nThreads);

for (int i = 0; i < 300; i++) {          // warm-up: let the JIT compile the code
    physics.computeState(16);
}
long[] times = new long[1000];
for (int i = 0; i < times.length; i++) {
    long start = System.nanoTime();
    physics.computeState(16);
    times[i] = System.nanoTime() - start;
}
System.out.println(physics.getStateSnapshot().size());   // use the result
physics.shutdown();
```

Each configuration is run **5 times, each time in a new JVM**, and the tables report the mean of the 5 runs.

## 5. Pitfalls
| Pitfall | How it is handled |
|---|---|
| Garbage collection | long runs (1,000 frames) so that the GC runs several times; fixed heap (`-Xms2g -Xmx2g`); GC activity checked with `-Xlog:gc` |
| Dynamic compilation | 300 warm-up frames that are not measured; checked with `-XX:+PrintCompilation` that almost no method is compiled during the measured frames |
| Unrealistic code paths | the measured code is the real engine, called in the same way as the game loop |
| Unrealistic contention | real board, real ball distribution, real locks |
| Dead code elimination | the result of the simulation (number of balls) is printed at the end |

## 6. Experiments
| Experiment | What changes | What is fixed |
|---|---|---|
| E1 Use cases | version (sequential, multithreaded, executor), 1,000 and 5,000 balls | threads = Ncpu |
| E2 Number of threads | threads = 1, 2, 4, ..., Ncpu, **Ncpu + 1**, 2 x Ncpu (multithreaded version) | 10,000 balls |
| E3 Number of balls | 1,000 to 40,000 balls, all three versions | threads = Ncpu |
| E4 Grid size | 5x5, 10x10, 15x15, 20x20 | 10,000 balls, threads = Ncpu |

In E4, 20 rows is the maximum for this board: the cells must be at least 60 px high (the diameter of the player
balls).

During one run of E2 I also use a profiler (VisualVM or Java Mission Control) to see the state of the threads (running,
waiting on the barrier, waiting on a cell) and where they block on locks, and `vmstat 1` to count the context
switches.

## 7. Results
### 7.1 Use cases (E1)
*TODO: table with version, balls, mean, standard deviation, p95, p99, max, frames per second, % of frames under 16.6 ms.*

*TODO: chart of the distribution of the frame times (percentage of frames under x ms) for each version.*

To discuss: with 1,000 balls, is the concurrent version useful at all? If every version is far under 16.6 ms, the
threads only add overhead.

### 7.2 Number of threads (E2)
*TODO: table with threads, mean frame time, speedup, efficiency.*

*TODO: chart of the speedup compared to the ideal line S = p.*

To discuss:

- where the speedup stops growing, compared with the number of cores;
- whether **Ncpu + 1** threads is better than Ncpu, as the rule for CPU-bound tasks suggests;
- what happens with more threads than cores: more context switches, and at every frame the barrier waits for the
  slowest worker, so a worker that is descheduled slows down all the others;
- the ratio W/C (waiting time / computing time) of the workers seen in the profiler. Here the waits are not I/O but
  synchronisation (barrier and cells), and they grow with the number of threads, so the formula
  Nthreads = Ncpu x Ucpu x (1 + W/C) for blocking tasks should not suggest more threads than Ncpu + 1.

### 7.3 Number of balls (E3)
*TODO: chart of the mean frame time against the number of balls for the three versions, with the 16.6 ms line.*

To discuss: the largest number of balls that each version handles within 16.6 ms, and below which number of balls the
sequential version is faster than the concurrent ones.

### 7.4 Grid size (E4)
*TODO: chart of the frame time against the number of cells.*

To discuss: the grid is a form of **lock striping** (one lock per cell instead of one for the whole board). With few
big cells each lock is held for a long time (the collisions inside a cell are quadratic in the number of its balls);
with many small cells there are more lock operations per frame.

### 7.5 Sources of serialisation
To discuss with the profiler results:

- the part of the frame executed only by the game loop (`initFrame` on every cell) limits the speedup (Amdahl's law);
- the `ExecutorService` uses a shared queue for the tasks, a serialisation hidden in the library; it creates
  2 x rows tasks per frame, while the multithreaded version only uses the barrier twice per frame;
- whether the cell locks are actually contended (threads blocked on them in the profiler) or almost always free. An
  uncontended lock is cheap; optimisation is useful only where contention actually happens.

### 7.6 Conclusion
*TODO: which version is the fastest, under which load, with how many threads.*
