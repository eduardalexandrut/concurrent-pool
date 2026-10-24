# Model-Checking
To verify the correctness of the multithreaded engine, the absence of deadlocks and of race conditions on the cells,
I used **Java PathFinder (JPF)**. JPF executes the program on its own virtual machine and explores all the possible
interleavings of its threads. The verification is limited to the physics engine (`MultiThreadPhysics`), without the
GUI.

## Environment Configuration
JPF (`jpf-core`, branch `master`) runs on **JDK 11** and executes Java 11 bytecode. The code was adapted once, so that
the same sources compile both for the game (Java 21) and for JPF (Java 11):

- **No records.** `P2d`, `V2d`, `BallState`, `Hole` and `Boundary` are normal final classes with the same accessor
  methods (`x()`, `y()`, `pos()`, ...).
- **No Java 12+ syntax.** `stream().toList()` was replaced with `Collections.unmodifiableList(...)`, and the
  `switch` with arrows in `MoveUserCmd` with a classic `switch`.
- **Maven profile.** `mvn -P jpf clean compile` produces Java 11 bytecode in `target/classes`.

JPF executes `ReentrantLock` and `Condition` directly (their implementation in the JDK 11 library), so the `Cell` and
`SimpleBarrier` monitors are verified exactly as they are used in the game.

## First Attempt
The first version of the test harness ran out of memory after about 15 minutes (`OutOfMemoryError`, 285,637 new
states, maximum depth 86). The problem was not in the monitors but in the configuration and in the harness itself:

- **Breadth-first search.** `search.class = BFSHeuristic` keeps in memory all the states of the current level of the
  search. The default depth-first search only keeps the current path. With a maximum depth of 86, no execution was
  ever explored until the end.
- **`cg.threads.break_arrays = true`.** It adds a thread switch at every access to a shared array. The array
  `cells[r][c]` is read all the time, so the number of interleavings exploded.
- **The balls never reached the engine.** `board.getBalls()` returned a new (or unmodifiable) list, so
  `board.getBalls().add(b1)` did not add anything to the board: the engine ran with no small balls, and the final
  check would have failed.
- **The workers never terminated.** After `main` ended, the workers stayed blocked on the barrier forever. JPF reports
  this situation as a deadlock, so the harness could never pass, even with infinite memory. Running the new harness
  without the call to `shutdown()` confirms it: JPF finds the deadlock immediately (264 states, less than a second):

```text
gov.nasa.jpf.vm.NotDeadlockedProperty
deadlock encountered:
  thread app.model.physics.executor.PhysicsWorker:{id:1,name:Thread-1,status:WAITING,...}
  thread app.model.physics.executor.PhysicsWorker:{id:2,name:Thread-2,status:WAITING,...}
	at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2081)
	at app.model.active_components.SimpleBarrier.await(SimpleBarrier.java:42)
```

## Test Harness Design
The new harness (`JPFTestHarness`) uses the smallest configuration that still exercises every synchronisation
mechanism:

- a 400x400 board with a grid of **2 rows x 1 column** and **2 workers**, one row each;
- **two small balls overlapping on the border between the rows** (y = 197 and y = 204) and moving towards each other,
  plus a third ball in row 1 away from them. The two player balls are also on the board, in row 1;
- **1 frame** (the number of frames can be passed as an argument), then `shutdown()`.

With this set-up, in a single frame:

- worker 0 computes the collision across the border, holding the locks of both cells (lock ordering), while worker 1
  computes the collisions inside the cell of row 1 (contention on the same lock);
- the movement of the cell of row 1 must wait on its counter until worker 0 has signalled it (the `Condition`);
- the bounce moves a ball across the border, so it is transferred to the other cell, where the frame stamp prevents it
  from being moved twice;
- `shutdown()` releases the workers from the barrier and joins them.

The properties checked are:

- **no deadlock** (`NotDeadlockedProperty`), including at the end of the program: the workers must terminate;
- **no uncaught exception** (`NoUncaughtExceptionsProperty`). At the end the harness checks that the number of small
  balls is still 3 and throws an `AssertionError` otherwise, so a lost or duplicated ball is reported as an error;
- **no data race** on shared fields (`PreciseRaceDetector` listener): two threads accessing the same field, at least
  one writing, without a lock in common.

## Configuration
`poool.jpf`:

```properties
target = app.model.physics.JPFTestHarness
target.args = 1
classpath = target/classes

search.class = gov.nasa.jpf.search.DFSearch
search.properties = gov.nasa.jpf.vm.NotDeadlockedProperty,gov.nasa.jpf.vm.NoUncaughtExceptionsProperty
listener = gov.nasa.jpf.listener.PreciseRaceDetector

report.console.property_violation = error,trace
```

## Execution and Results
Environment: `jpf-core` branch `master`, Temurin JDK 11.0.24, `JVM_FLAGS=-Xmx4g`.

| Result | New states | Visited states | Backtracked | Max depth | Time | Memory |
|---|---:|---:|---:|---:|---:|---:|
| no errors detected | 4,055,363 | 7,738,505 | 11,793,868 | 600 | 19 min 08 s | 342 MB |

The search **completed**: all the interleavings of this configuration were explored, and in none of them there is a
deadlock, a data race, or a lost or duplicated ball, and the workers always terminate after `shutdown()`.

The number of states is large compared to the size of the program because `ReentrantLock` and `Condition` are
implemented in Java with atomic operations, and every one of them is a point where JPF tries all the possible thread
switches. Memory is not a problem any more thanks to the depth-first search (342 MB); the limit is time. More frames
can be verified with `+target.args=2`, but the time grows a lot.

## Limitations
- The verification is exhaustive only for the configuration of the harness (2 workers, 2 rows, 1 frame). The
  arguments for the general case are the ones in [Behaviour](behaviour.md): the lock ordering on the cell ids and
  the counters work in the same way for any number of rows and workers.
- The executor version is not verified with JPF, because the thread pool would add many more states. It uses the same
  per-row code, and `invokeAll` acts as a barrier between the phases.
- The GUI is not part of the verification.

## How to Run
1. Install a **JDK 11** and build `jpf-core`:
   ```bash
   git clone https://github.com/javapathfinder/jpf-core.git
   cd jpf-core
   ./gradlew buildJars        # with JAVA_HOME pointing to the JDK 11
   ```
2. Compile the project for JPF, from the project folder:
   ```bash
   mvn -P jpf clean compile
   ```
3. Run JPF with the JDK 11 on the `PATH`:
   ```bash
   export JVM_FLAGS="-Xmx4g"
   /path/to/jpf-core/bin/jpf poool.jpf
   ```
4. `no errors detected` at the end of the output means that the search finished without finding errors. In case of
   an error, the `trace` section shows the sequence of thread switches that leads to it.
