# Performance Evaluation

The method follows the lab notes *Performance of Concurrent Programs*: first make the program correct, then measure
(not guess) on representative use cases, looking both at the average (throughput) and at the variance
(responsiveness).

## 1. What is measured
The activity to measure is the computation of one frame, `computeState(dt)`. It is a CPU-bound task (no I/O), so the
goal is to keep the CPUs busy with useful work.

The questions are:

1. **How fast** is a frame on average, and how many frames stay within the 16.6 ms needed for 60 FPS?
2. **Scalability:** how does the speedup change when threads are added? Which number of threads is the best?
3. **How much:** how does the frame time grow with the number of balls? With few balls, is the concurrent version
   still faster than the sequential one?
4. **Which version** is better: platform threads with the barrier, or the `ExecutorService`?

## 2. Metrics
| Metric | Definition |
|---|---|
| Frame time | time of one `computeState(16)`, measured with `System.nanoTime()` |
| Throughput | mean frame time and frames per second (1000 / mean) |
| Responsiveness | standard deviation, 95th and 99th percentile, maximum, and the percentage of frames under 16.6 ms |
| Speedup | S(p) = T(sequential) / T(p), where T(sequential) is measured with `SequentialPhysics` |
| Efficiency | E(p) = S(p) / p |

## 3. Test machine
| | |
|---|---|
| CPU | AMD Ryzen 7 3700U (laptop, 15 W): **4 physical cores, 8 logical processors** with SMT (Ncpu = 8, `availableProcessors()`) |
| JVM | Java HotSpot 64-Bit Server VM 21.0.7, heap fixed with `-Xms2g -Xmx2g` |
| OS | Windows |

Two properties of this CPU matter for the results:

- **SMT (simultaneous multithreading):** each physical core runs two hardware threads that share its execution units.
  Two threads on the same core are faster than one, but far from twice as fast, so the ideal speedup on this machine is
  between 4 (physical cores) and 8 (logical processors).
- **Turbo frequency:** a laptop CPU runs at a higher clock when few cores are busy and lowers it when all of them are,
  to stay within its power limit. A single thread therefore runs faster than each of eight parallel threads, which
  lowers the measured efficiency.

## 4. Use cases
The measurements use the real engine (the two player balls, the holes and the scores are included), without the GUI
and without the 16 ms sleep of the game loop, on the default board (1600x1200).

| Use case | Balls | Why |
|---|---|---|
| Default game | 1,000 | default value of the launcher |
| Heavy game | 5,000 | crowded table |
| Stress | 10,000 | more than the sequential version can compute at 60 FPS |

The balls are created from a `Random` with a fixed seed (42) and passed to `new Board(width, height, balls)`, so every
version simulates exactly the same table. The executor version always uses `availableProcessors()` threads.

## 5. Measurement loop
The benchmark is the class `app.bench.Benchmark`. Its core:

```java
for (int i = 0; i < warmup; i++) {          // warm-up: let the JIT compile the code
    physics.computeState(16);
}
long[] times = new long[frames];
for (int i = 0; i < frames; i++) {
    long start = System.nanoTime();
    physics.computeState(16);
    times[i] = System.nanoTime() - start;
}
int remaining = physics.getStateSnapshot().size();   // use the result
physics.shutdown();
```

All the experiments are run by `scripts/run-benchmarks.ps1`. **Every repetition runs in a new JVM**, and the tables
report the mean of the repetitions.

| Experiment | Warm-up + measured frames | Repetitions |
|---|---|---|
| E1 | 300 + 1,000 | 5 |
| E2, E3 | 200 + 500 | 3 |
| E4 | 100 + 300 | 3 |

## 6. Pitfalls
| Pitfall | How it is handled |
|---|---|
| Garbage collection | long runs (300 to 1,000 frames), so that the GC runs several times during the measure and its cost is part of the mean; fixed heap (`-Xms2g -Xmx2g`) |
| Dynamic compilation | warm-up frames that are not measured; each repetition in a new JVM |
| Unrealistic code paths | the measured code is the real engine, called in the same way as the game loop |
| Unrealistic contention | real board, real ball distribution, real locks |
| Dead code elimination | the result of the simulation (number of balls left) is printed at the end |

**Outliers.** In 6 of the 111 runs a few frames took between 160 and 450 ms, while the 99th percentile of the same
runs is below 60 ms (for example run 2 of the sequential version with 5,000 balls: maximum 446 ms). They appear in a single run of a configuration and not in the others, so they
come from outside the engine (a GC pause or another process of the operating system), not from the synchronisation.
They raise the mean, the standard deviation and the maximum of that run; the median of the runs is given where it
changes the result. The maximum frame time is reported but not used to compare the versions.

## 7. Results
### 7.1 Use cases (E1)
Threads = Ncpu = 8, grid 20x20, mean of 5 runs. Times in milliseconds.

| Version | Balls | Mean | SD | p95 | p99 | Max | FPS | Frames ≤ 16.6 ms |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Sequential | 1,000 | 0.38 | 0.15 | 0.57 | 0.79 | 3.84 | 2,632 | 100% |
| MultiThread | 1,000 | **0.32** | 0.12 | 0.44 | 0.63 | 3.25 | 3,190 | 100% |
| Executor | 1,000 | 0.39 | 0.14 | 0.53 | 0.71 | 3.91 | 2,571 | 100% |
| Sequential | 5,000 | 6.24 | 6.89 | 8.75 | 12.03 | 189.61 | 162 | 99.6% |
| MultiThread | 5,000 | **2.11** | 2.23 | 2.91 | 5.67 | 57.26 | 496 | 99.8% |
| Executor | 5,000 | 2.16 | 0.20 | 2.48 | 2.71 | 4.61 | 463 | 100% |

With **1,000 balls** (the default game) every version computes a frame in less than 0.4 ms, forty times less than the
budget of 16.6 ms: the game runs at 60 FPS with any of them, and concurrency gives only a small advantage
(speedup 1.19 for the multithreaded version, 0.97 for the executor).

With **5,000 balls** both concurrent versions are about three times faster than the sequential one (speedup 2.96 and
2.89). The standard deviation and the maximum of the sequential and multithreaded versions are raised by the outliers
of a few runs (section 6): without them the multithreaded version has the lowest times (median of the runs 1.85 ms,
against 2.15 ms for the executor), while the executor has the most regular ones.

### 7.2 Number of threads (E2)
MultiThread version, 10,000 balls, grid 20x20, mean of 3 runs. T(sequential) = 21.40 ms.

| Threads | Mean (ms) | p99 (ms) | Frames ≤ 16.6 ms | Speedup | Efficiency |
|---:|---:|---:|---:|---:|---:|
| 1 | 23.78 | 30.21 | 0% | 0.90 | 0.90 |
| 2 | 15.29 | 21.00 | 80.1% | 1.40 | 0.70 |
| 4 | 8.16 | 23.41 | 99.1% | 2.62 | 0.66 |
| 8 (Ncpu) | 6.62 | 9.73 | 99.9% | 3.23 | 0.40 |
| 9 (Ncpu + 1) | 7.06 | 10.20 | 99.9% | 3.03 | 0.34 |
| **16 (2 x Ncpu)** | **5.79** | 7.14 | 100% | **3.70** | 0.23 |

- **1 thread** is about 10% slower than the sequential version: this is the cost of the barrier, of the locks of the
  cells and of the counters when there is no parallelism to pay for them.
- The speedup grows up to 8 threads, but the efficiency drops. Measured against the 4 physical cores, the speedup of
  3.23 with 8 threads is an efficiency of 0.81; the step from 4 to 8 threads (2.62 → 3.23) is the gain of SMT, the
  second hardware thread of each core (section 3).
- **Ncpu + 1 = 9 threads is worse than Ncpu.** The 20 rows are split as 3 + 3 + 2 + … among 9 workers, and the end
  barrier waits for the workers with 3 rows. The rule Ncpu + 1 is meant for tasks that sometimes block for reasons
  outside the program (page faults, I/O); here the workers never do.
- **16 threads give the best result** (5.79 ms). Two explanations are compatible with the data:
    - *load balance*: with 8 workers some have 3 rows and some 2, and the frame lasts as long as the workers with 3
      rows; with 16 workers no one has more than 2 rows, and the operating system can move the short workers to the
      cores that become free;
    - *waiting time*: a worker can block on the counter of a cell (waiting for the collisions of the row above) and at
      the end barrier. While it waits, its core is free, and an extra thread can use it. This is the case of the
      formula Nthreads = Ncpu x Ucpu x (1 + W/C) with W > 0.

  The two effects cannot be separated with these measurements; a profiler (VisualVM or Java Mission Control) showing
  how long the workers stay in `awaitCollisionsComplete` would measure W/C.

### 7.3 Number of balls (E3)
Threads = Ncpu = 8, grid 20x20, mean of 3 runs. Mean frame time in milliseconds, speedup against the sequential
version in brackets.

| Balls | Sequential | MultiThread | Executor |
|---:|---:|---:|---:|
| 1,000 | 0.38 | 0.38 (1.00) | 0.41 (0.93) |
| 2,500 | 1.58 | 0.70 (2.26) | 0.81 (1.95) |
| 5,000 | 5.25 | 1.82 (2.88) | 2.11 (2.49) |
| 10,000 | 19.71 | **6.59 (2.99)** | **6.57 (3.00)** |

- The frame time grows faster than linearly with the number of balls: the collisions inside a cell are quadratic in
  the number of its balls (twice the balls, about four times the time with a fixed grid).
- **Limit of 60 FPS (mean ≤ 16.6 ms):** the sequential version stays under the limit up to between 5,000 and 10,000
  balls (19.7 ms at 10,000); both concurrent versions are still at 6.6 ms with 10,000 balls, so they can handle more.
- **With 1,000 balls** the three versions are equivalent: the work of a frame is too small to pay for the
  synchronisation.
- The speedup grows with the number of balls: the more work in a frame, the smaller the fixed cost of the
  synchronisation compared with it. The multithreaded version is faster than the executor up to 5,000 balls; at
  10,000 they are the same.

### 7.4 Grid size (E4)
5,000 balls, mean of 3 runs. MultiThread with 8 threads, sequential as reference.

| Grid | Cells | Balls per cell | Sequential (ms) | MultiThread (ms) | Speedup |
|---|---:|---:|---:|---:|---:|
| 5x5 | 25 | 200 | 60.31 | 29.10 | 2.07 |
| 10x10 | 100 | 50 | 17.92 | 6.77 | 2.65 |
| 15x15 | 225 | 22 | 9.05 | 2.71 | 3.34 |
| 20x20 | 400 | 12 | 6.38 | 2.11 | 3.02 |

The grid is a form of **lock striping**: one lock per cell instead of one for the whole board. More cells mean fewer
balls per cell, so fewer pairs to check (the sequential time drops from 60 ms to 6 ms) and shorter critical sections.
The 20x20 grid is the fastest in absolute terms; it is the finest grid allowed on this board (cells must be at least
60 px high, the diameter of the player balls).

The speedup also depends on the number of rows, because the rows are the unit of work of a worker: with the 5x5 grid
there are only 5 rows for 8 workers, so 3 workers have nothing to do and the speedup stops at 2.07.

### 7.5 Sources of serialisation
- **Work done only by the game loop.** `initFrame` on all the cells and the user input are executed by one thread
  before the workers start (Amdahl's law). Together with the cost of the barrier and of the locks, it is the reason
  why 1 worker is 10% slower than the sequential version (E2).
- **Load balance.** The frame ends when the slowest worker reaches the end barrier, and the work is divided by rows:
  when the rows are not a multiple of the workers (9 threads in E2) or are fewer than the workers (5x5 in E4), the
  speedup drops.
- **Executor.** `ExecutorPhysics` waits for *all* collision tasks with `invokeAll` before starting the movement tasks.
  This is a global barrier between the two phases, while the multithreaded version only waits on the counter of each
  cell. In addition it creates and submits 2 x 20 = 40 tasks per frame through the shared queue of the pool. This cost
  is visible with medium loads (2,500 – 5,000 balls); at 10,000 balls it is small compared with the work of a frame,
  and the two versions are equivalent.

### 7.6 Conclusion
On the test machine (4 physical cores, 8 logical processors):

- for the **default game (1,000 balls)** all three versions run at well over 60 FPS, with a negligible difference;
- **from 2,500 balls** the concurrent versions are clearly faster than the sequential one, with a speedup of about 3
  at 5,000 – 10,000 balls; with 10,000 balls they are the only ones that keep 60 FPS;
- the **multithreaded version** is the fastest up to 5,000 balls; at 10,000 balls the executor version is equivalent;
- the best number of threads for the multithreaded version is not Ncpu + 1 but **2 x Ncpu**, because of the division
  of the work by rows and of the time the workers spend waiting on the cells and on the barrier.

## 8. How to repeat the measurements
All the experiments, from the project folder (Windows PowerShell):

```powershell
powershell -ExecutionPolicy Bypass -File scripts\run-benchmarks.ps1
```

A single configuration:

```powershell
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse src\main\java -Filter *.java).FullName
# java app.bench.Benchmark <seq|mt|exec> <balls> <threads> <grid> [warmup] [frames]
java -Xms2g -Xmx2g -cp out app.bench.Benchmark mt 10000 8 20
```

Each run prints one CSV line: `engine,balls,threads,grid,mean,sd,p95,p99,max,fps,%frames≤16.6ms,balls left`. The raw
results of the tables are in `docs/data/benchmark-results.csv`.