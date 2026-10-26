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

## 3. Test machine
| | |
|---|---|
| CPU | Intel Xeon @ 2.80 GHz, **2 virtual cores** (Ncpu = 2), virtual machine |
| Memory | 8 GB, JVM heap fixed with `-Xms2g -Xmx2g` |
| JVM | OpenJDK 21.0.12, HotSpot 64-bit Server VM |
| OS | Linux 6.18 |

With only two cores the maximum theoretical speedup is 2. On a machine with more cores the absolute values and the
best number of threads change; the benchmark can be repeated there with the same commands (section 8).

## 4. Use cases
The measurements use the real engine (the two player balls, the holes and the scores are included), without the GUI
and without the 16 ms sleep of the game loop, on the default board (1600x1200, grid 20x20).

| Use case | Balls | Why |
|---|---|---|
| Default game | 1,000 | default value of the launcher |
| Heavy game | 5,000 | crowded table |
| Stress | 1,000 to 40,000 | to find the limit of each version |

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

E1 and E2 use 300 warm-up frames and 1,000 measured frames, repeated **5 times**; E3 and E4 use 200 + 500 frames,
repeated **3 times** (the runs with 40,000 balls take minutes). **Every repetition runs in a new JVM**, and the tables
report the mean of the repetitions. The variation between repetitions (coefficient of variation of the mean frame
time) is below 5% for every configuration with at least 2,500 balls, and around 10% with 1,000 balls, where a frame
takes less than half a millisecond.

## 6. Pitfalls
| Pitfall | How it is handled |
|---|---|
| Garbage collection | long runs (500 to 1,000 frames), so that the GC runs several times during the measure and its cost is part of the mean; fixed heap (`-Xms2g -Xmx2g`) |
| Dynamic compilation | warm-up frames that are not measured; each repetition in a new JVM |
| Unrealistic code paths | the measured code is the real engine, called in the same way as the game loop |
| Unrealistic contention | real board, real ball distribution, real locks |
| Dead code elimination | the result of the simulation (number of balls left) is printed at the end |

The maximum frame time is sensitive to GC pauses and to the scheduler of the virtual machine: it is reported but not
used to compare the versions.

## 7. Results
### 7.1 Use cases (E1)
Threads = Ncpu = 2, grid 20x20, mean of 5 runs. Times in milliseconds.

| Version | Balls | Mean | SD | p95 | p99 | Max | FPS | Frames ≤ 16.6 ms |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Sequential | 1,000 | 0.40 | 0.18 | 0.57 | 0.83 | 3.91 | 2,490 | 100% |
| MultiThread | 1,000 | 0.43 | 0.35 | 0.69 | 2.04 | 5.63 | 2,343 | 100% |
| Executor | 1,000 | 0.72 | 0.46 | 1.18 | 2.97 | 6.38 | 1,405 | 100% |
| Sequential | 5,000 | 5.89 | 1.15 | 8.01 | 9.81 | 16.98 | 170 | 99.9% |
| MultiThread | 5,000 | **3.59** | 1.02 | 4.87 | 7.30 | 19.84 | 279 | 99.9% |
| Executor | 5,000 | 5.68 | 0.92 | 7.33 | 8.66 | 12.54 | 176 | 100% |

With **1,000 balls** (the default game) concurrency is useless: a frame takes 0.4 ms, forty times less than the budget
of 16.6 ms, and the threads only add overhead. The multithreaded version is as fast as the sequential one but with
twice the variance (p99 2.0 ms against 0.8 ms); the executor version is almost twice as slow.

With **5,000 balls** the multithreaded version is clearly the fastest (speedup 1.64), while the executor version is
only as fast as the sequential one.

### 7.2 Number of threads (E2)
MultiThread version, 10,000 balls, grid 20x20, mean of 5 runs. T(sequential) = 21.78 ms.

| Threads | Mean (ms) | p99 (ms) | Frames ≤ 16.6 ms | Speedup | Efficiency |
|---:|---:|---:|---:|---:|---:|
| 1 | 21.34 | 32.10 | 0% | 1.02 | 1.02 |
| **2 (Ncpu)** | **12.23** | 19.30 | 96.9% | **1.78** | 0.89 |
| 3 (Ncpu + 1) | 13.99 | 21.53 | 87.3% | 1.56 | 0.52 |
| 4 (2 x Ncpu) | 12.91 | 19.70 | 93.8% | 1.69 | 0.42 |

- **1 thread** is as fast as the sequential version: the cost of the barrier (two `await` per frame) and of the cell
  counters is negligible compared with the work of a frame.
- **2 threads** give the best result, a speedup of 1.78 on 2 cores (efficiency 0.89).
- **Ncpu + 1 = 3 threads is worse than Ncpu.** The rows are split 7/7/6 on 2 cores: one core has to run two workers,
  and since the end barrier waits for the slowest worker, that core decides the frame time. The rule Ncpu + 1 is meant
  for tasks that sometimes block (page faults, I/O): here the workers only block on the barrier and on the cells, and
  those waits are caused by the threads themselves, so an extra thread does not fill idle time.
- **4 threads** are again close to 2, because 4 workers divide the 20 rows evenly between the two cores (5 + 5 each),
  but they add context switches and are never better than 2.

The number of threads that should be used is therefore **Ncpu**, the default of the `MultiThreadPhysics` constructor.

### 7.3 Number of balls (E3)
Threads = 2, grid 20x20, mean of 3 runs. Mean frame time in milliseconds, speedup against the sequential version in
brackets.

| Balls | Sequential | MultiThread | Executor |
|---:|---:|---:|---:|
| 1,000 | 0.44 | 0.47 (0.94) | 0.70 (0.63) |
| 2,500 | 1.61 | 1.10 (1.46) | 2.09 (0.77) |
| 5,000 | 5.37 | 3.49 (1.54) | 5.79 (0.93) |
| 10,000 | 22.23 | **11.34 (1.96)** | 18.30 (1.21) |
| 20,000 | 79.88 | 44.03 (1.81) | 54.70 (1.46) |
| 40,000 | 281.58 | 153.82 (1.83) | 183.91 (1.53) |

- The frame time grows faster than linearly with the number of balls: the collisions inside a cell are quadratic in
  the number of its balls (twice the balls, about four times the time with a fixed grid).
- **Limit of 60 FPS (mean ≤ 16.6 ms):** the sequential and the executor versions stay under the limit up to between
  5,000 and 10,000 balls; the multithreaded version still does at 10,000 balls (11.3 ms, 98.5% of frames under
  16.6 ms).
- **Under about 1,000 balls** the sequential version is the fastest: the work of a frame is too small to pay for the
  synchronisation. The multithreaded version is faster from 2,500 balls; the executor version only from 10,000 balls.
- The speedup of the multithreaded version settles around 1.8–1.9 on 2 cores.

### 7.4 Grid size (E4)
10,000 balls, mean of 3 runs. MultiThread with 2 threads, sequential as reference.

| Grid | Cells | Balls per cell | Sequential (ms) | MultiThread (ms) | Speedup |
|---|---:|---:|---:|---:|---:|
| 5x5 | 25 | 400 | 253.81 | 189.77 | 1.34 |
| 10x10 | 100 | 100 | 69.61 | 37.87 | 1.84 |
| 15x15 | 225 | 44 | 36.37 | 21.58 | 1.69 |
| 20x20 | 400 | 25 | 21.05 | 11.97 | 1.76 |

The grid is a form of **lock striping**: one lock per cell instead of one for the whole board. More cells mean fewer
balls per cell, so fewer pairs to check (the time drops from 254 ms to 21 ms in the sequential version) and shorter
critical sections. With the 5x5 grid there are only 5 rows for 2 workers (3 + 2), so the work is unbalanced and the
speedup is only 1.34. 20x20 is the finest grid allowed on this board (cells must be at least 60 px high, the
diameter of the player balls) and it is the fastest.

### 7.5 Sources of serialisation
- **Work done only by the game loop.** `initFrame` on all the 400 cells and the user input are executed by one
  thread before the workers start (Amdahl's law). The result of E2 with 1 thread (same time as the sequential version)
  shows that this part, together with the barrier, is small.
- **Load balance.** The frame ends when the slowest worker reaches the end barrier: with few rows (E4, 5x5) or a
  number of workers that does not divide the rows evenly between the cores (E2, 3 threads) the speedup drops.
- **Executor.** `ExecutorPhysics` waits for *all* collision tasks with `invokeAll` before starting the movement tasks.
  This is a global barrier between the two phases, while the multithreaded version only waits on the counter of each
  cell, so a worker can start moving its rows while another is still colliding. In addition it creates and submits
  2 x 20 = 40 tasks per frame through the shared queue of the pool. This explains why it is always slower than the
  multithreaded version, and slower than the sequential one up to about 5,000 balls.

### 7.6 Conclusion
On the test machine (2 cores):

- for the **default game (1,000 balls)** all three versions run at well over 60 FPS, and the sequential version is the
  fastest;
- **from about 2,500 balls** the **multithreaded version with Ncpu threads** is the fastest, with a speedup of
  1.5–1.96 on 2 cores, and it is the only version that keeps 60 FPS with 10,000 balls;
- the **executor version** is always slower than the multithreaded one, because of the global wait between the
  phases and the task overhead;
- **Ncpu + 1** threads do not help, because the workers never block on I/O.

## 8. How to repeat the measurements
```bash
javac -encoding UTF-8 -d out $(find src/main/java -name "*.java")
# java app.bench.Benchmark <seq|mt|exec> <balls> <threads> <grid> [warmup] [frames]
java -Xms2g -Xmx2g -cp out app.bench.Benchmark mt 10000 2 20
```
Each call prints one CSV line:
`engine,balls,threads,grid,mean,sd,p95,p99,max,fps,%frames≤16.6ms,balls left`.