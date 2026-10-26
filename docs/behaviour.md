# Behaviour

To model the synchronisation of the physics engine I used **Petri nets**. Each net shows one mechanism: the mutual
exclusion of a cell, the lock ordering, the counter between the two phases, a whole frame, and the termination.

Mermaid does not have Petri nets, so they are drawn as graphs with this convention:

- **places** are circles; `●` is a token in the initial marking;
- **transitions** are dark rectangles;
- an arrow with a number has that **weight** (default 1);
- a double arrow `<-->` is a **read arc**: the transition needs the token but puts it back;
- an arrow ending with a circle `--o` is an **inhibitor arc**: the transition can fire only if the place is empty.

`M(p)` is the number of tokens in the place `p`.

### 1. Mutual exclusion on a Cell
Two threads that use the same cell, for example the worker of row `r-1` (collisions across the border) and the worker
of row `r` (internal collisions). Every method of `Cell` has the shape `lock.lock()`, work, `lock.unlock()`.

```mermaid
flowchart LR
    A0(("A outside ●"))
    A1(("A inside cell"))
    B0(("B outside ●"))
    B1(("B inside cell"))
    L(("lock free ●"))

    tAe["A: lock()"]:::tr
    tAx["A: unlock()"]:::tr
    tBe["B: lock()"]:::tr
    tBx["B: unlock()"]:::tr

    A0 --> tAe
    L --> tAe
    tAe --> A1
    A1 --> tAx
    tAx --> A0
    tAx --> L

    B0 --> tBe
    L --> tBe
    tBe --> B1
    B1 --> tBx
    tBx --> B0
    tBx --> L

    classDef tr fill:#263238,color:#ffffff,stroke:#263238
```

**Properties.** `M(lock free) + M(A inside) + M(B inside) = 1` in every reachable marking: at most one thread is
inside the cell (**mutual exclusion**). The net is live: each transition can always fire again.

### 2. Two locks: lock ordering
`resolveCollisionsWith` needs the locks of two cells. Here two threads X and Y both need the cells `c1` (id 1) and
`c2` (id 2). On the left, each thread takes the locks in its own order; on the right, as in the code, both take the
lock with the higher id first.

```mermaid
flowchart TB
%%    subgraph U["Without ordering: X takes c1 then c2, Y takes c2 then c1"]
%%        direction LR
%%        uX0(("X idle ●")) --> uXa["X takes c1"]:::tr
%%        uC1(("c1 free ●")) --> uXa
%%        uXa --> uX1(("X holds c1"))
%%        uX1 --> uXb["X takes c2"]:::tr
%%        uC2(("c2 free ●")) --> uXb
%%        uXb --> uX2(("X holds both"))
%%        uX2 --> uXr["X releases"]:::tr
%%        uXr --> uX0
%%        uXr --> uC1
%%        uXr --> uC2
%%
%%        uY0(("Y idle ●")) --> uYa["Y takes c2"]:::tr
%%        uC2 --> uYa
%%        uYa --> uY1(("Y holds c2"))
%%        uY1 --> uYb["Y takes c1"]:::tr
%%        uC1 --> uYb
%%        uYb --> uY2(("Y holds both"))
%%        uY2 --> uYr["Y releases"]:::tr
%%        uYr --> uY0
%%        uYr --> uC1
%%        uYr --> uC2
%%    end

    subgraph O["With ordering (implementation): both take c2 first"]
        direction LR
        oX0(("X idle ●")) --> oXa["X takes c2"]:::tr
        oC2(("c2 free ●")) --> oXa
        oXa --> oX1(("X holds c2"))
        oX1 --> oXb["X takes c1"]:::tr
        oC1(("c1 free ●")) --> oXb
        oXb --> oX2(("X holds both"))
        oX2 --> oXr["X releases"]:::tr
        oXr --> oX0
        oXr --> oC1
        oXr --> oC2

        oY0(("Y idle ●")) --> oYa["Y takes c2"]:::tr
        oC2 --> oYa
        oYa --> oY1(("Y holds c2"))
        oY1 --> oYb["Y takes c1"]:::tr
        oC1 --> oYb
        oYb --> oY2(("Y holds both"))
        oY2 --> oYr["Y releases"]:::tr
        oYr --> oY0
        oYr --> oC1
        oYr --> oC2
    end

    classDef tr fill:#263238,color:#ffffff,stroke:#263238
```

**Properties.** Without ordering, the marking {X holds c1, Y holds c2} is reachable and no transition is enabled:
it is a **deadlock**. With ordering, the thread that takes `c2` first is the only one that can take `c1`, and the
other is still idle; every reachable marking enables at least one transition, so there is **no deadlock**. The same
argument works with any number of cells, because the ids give a total order on all the locks.

[//]: # (### 3. The counter between the two phases)

[//]: # (This net models one cell of a row `r ≥ 1` during one frame. The collision work of rows `r-1` &#40;job 1&#41; and `r` &#40;job 2&#41;)

[//]: # (must signal the cell before the worker of row `r` can move its balls.)

[//]: # ()
[//]: # (```mermaid)

[//]: # (flowchart LR)

[//]: # (    D&#40;&#40;"game loop ●"&#41;&#41;)

[//]: # (    P&#40;&#40;"pending"&#41;&#41;)

[//]: # (    J1w&#40;&#40;"job 1 working"&#41;&#41;)

[//]: # (    J1d&#40;&#40;"job 1 done"&#41;&#41;)

[//]: # (    J2w&#40;&#40;"job 2 working"&#41;&#41;)

[//]: # (    J2d&#40;&#40;"job 2 done"&#41;&#41;)

[//]: # (    Mw&#40;&#40;"mover waiting"&#41;&#41;)

[//]: # (    Mm&#40;&#40;"mover moving"&#41;&#41;)

[//]: # ()
[//]: # (    tB["initFrame&#40;2&#41;"]:::tr)

[//]: # (    tS1["job 1: signalCollisionsDone&#40;&#41;"]:::tr)

[//]: # (    tS2["job 2: signalCollisionsDone&#40;&#41;"]:::tr)

[//]: # (    tP["mover: await returns"]:::tr)

[//]: # ()
[//]: # (    D --> tB)

[//]: # (    tB -- "2" --> P)

[//]: # (    tB --> J1w)

[//]: # (    tB --> J2w)

[//]: # (    tB --> Mw)

[//]: # ()
[//]: # (    J1w --> tS1)

[//]: # (    P --> tS1)

[//]: # (    tS1 --> J1d)

[//]: # ()
[//]: # (    J2w --> tS2)

[//]: # (    P --> tS2)

[//]: # (    tS2 --> J2d)

[//]: # ()
[//]: # (    Mw --> tP)

[//]: # (    P --o tP)

[//]: # (    tP --> Mm)

[//]: # ()
[//]: # (    classDef tr fill:#263238,color:#ffffff,stroke:#263238)

[//]: # (```)

[//]: # ()
[//]: # (`initFrame&#40;2&#41;` puts two tokens in `pending` &#40;the counter `pendingCollisionWorkers`&#41;. Each signal removes one. The)

[//]: # (transition `await returns` is the exit from `while &#40;pendingCollisionWorkers > 0&#41; collisionsComplete.await&#40;&#41;;`: the)

[//]: # (inhibitor arc means it can fire only when `pending` is empty.)

[//]: # ()
[//]: # (**Properties.**)

[//]: # ()
[//]: # (- `M&#40;pending&#41; + M&#40;job 1 done&#41; + M&#40;job 2 done&#41; = 2`: the counter is never negative and never larger than 2.)

[//]: # (- `mover moving` can be marked only after both signals: the balls of a cell never move while a collision job that)

[//]: # (  touches that cell is still running.)

[//]: # (- From every reachable marking the mover can eventually move, because the jobs never wait before signalling: there is)

[//]: # (  no deadlock between the two phases.)

[//]: # ()
[//]: # (The same cell as a state diagram:)

[//]: # ()
[//]: # (```mermaid)

[//]: # (stateDiagram-v2)

[//]: # (    [*] --> Colliding: initFrame&#40;k&#41;, pending = k)

[//]: # (    Colliding --> Colliding: signalCollisionsDone&#40;&#41; [pending > 1])

[//]: # (    Colliding --> Movable: signalCollisionsDone&#40;&#41; [pending = 1] / signalAll&#40;&#41;)

[//]: # (    Movable --> Moved: updateMovement&#40;&#41;)

[//]: # (    Moved --> Colliding: initFrame&#40;k&#41; of the next frame)

[//]: # (```)

### 4. A whole frame
The full protocol for the smallest interesting case: the game loop, two workers (W1 owns row 0, W2 owns row 1) and a
cell `c` of row 1, touched by the collision work of both workers. The barrier is a transition with one input arc from
each member: it fires only when all three are waiting. The collision work on `c` is drawn as one step that uses the
lock of `c` and produces one token in `done c`.

```mermaid
flowchart LR
    D0(("loop: start of frame ●"))
    D1(("loop: at barrier 2"))
    W1i(("W1 at barrier 1 ●"))
    W2i(("W2 at barrier 1 ●"))
    W1c(("W1 colliding"))
    W2c(("W2 colliding"))
    W1s(("W1 signalled"))
    W2s(("W2 signalled"))
    W2m(("W2 moving c"))
    W1e(("W1 at barrier 2"))
    W2e(("W2 at barrier 2"))
    MX(("lock c free ●"))
    DN(("done c"))

    TS["barrier 1"]:::tr
    C1["W1: collisions with c, signal"]:::tr
    C2["W2: collisions of c, signal"]:::tr
    M1["W1: move row 0"]:::tr
    MA["W2: await c, lock"]:::tr
    MB["W2: move c, unlock"]:::tr
    TE["barrier 2"]:::tr

    D0 --> TS
    W1i --> TS
    W2i --> TS
    TS --> D1
    TS --> W1c
    TS --> W2c

    W1c --> C1
    MX --> C1
    C1 --> W1s
    C1 --> MX
    C1 --> DN

    W2c --> C2
    MX --> C2
    C2 --> W2s
    C2 --> MX
    C2 --> DN

    W1s --> M1
    M1 --> W1e

    W2s --> MA
    DN -- "2" --> MA
    MX --> MA
    MA --> W2m
    W2m --> MB
    MB --> MX
    MB --> W2e

    D1 --> TE
    W1e --> TE
    W2e --> TE
    TE --> D0
    TE --> W1i
    TE --> W2i

    classDef tr fill:#263238,color:#ffffff,stroke:#263238
```

**Properties.**

- `M(lock c free) + M(W2 moving c) = 1`: mutual exclusion on `c`.
- `M(done c) ≤ 2`, and the arc of weight 2 empties it, so the counter starts from zero at every frame.
- W2 can move `c` only after both collision steps on `c`. W1 can already move row 0 while W2 is still colliding:
  there is no global barrier between the two phases.
- From every reachable marking the initial marking can be reached again: every frame completes and the cycle repeats
  (**liveness**, no deadlock).

### 5. Termination
The game loop and one worker. The place `running` models the `running` flag. The barrier transitions read it to decide
whether the worker starts a new frame or terminates.

```mermaid
flowchart LR
    R(("running ●"))
    S(("stopped"))
    D0(("loop idle ●"))
    D1(("loop in frame"))
    Ds(("loop: shutdown"))
    Dj(("loop: join"))
    DT(("loop done"))
    W0(("worker at barrier 1 ●"))
    W1(("worker working"))
    W2(("worker at barrier 2"))
    Wd(("worker terminated"))

    tStart["barrier 1, running"]:::tr
    tWork["worker: rows done"]:::tr
    tEnd["barrier 2"]:::tr
    tStop["running = false"]:::tr
    tRel["barrier 1, stopped:<br/>worker returns"]:::tr
    tJoin["join()"]:::tr

    D0 --> tStart
    W0 --> tStart
    R <--> tStart
    tStart --> D1
    tStart --> W1

    W1 --> tWork
    tWork --> W2
    D1 --> tEnd
    W2 --> tEnd
    tEnd --> D0
    tEnd --> W0

    D0 --> tStop
    R --> tStop
    tStop --> S
    tStop --> Ds
    Ds --> tRel
    W0 --> tRel
    S <--> tRel
    tRel --> Dj
    tRel --> Wd
    Dj --> tJoin
    Wd <--> tJoin
    tJoin --> DT

    classDef tr fill:#263238,color:#ffffff,stroke:#263238
```

**Properties.** After `running = false`, the only possible sequence is release, return of the worker, join, ending in
{loop done, worker terminated, stopped}. This final marking is the expected end of the program, not a deadlock. The
worker can no longer start a frame, because `barrier 1, running` needs the token in `running`. Without the flag and
the last release, the worker would stay in `worker at barrier 1` forever after the last frame, which is exactly what
JPF reported for the first test harness (see [Model Checking](model-checking.md)).

The life of a worker as a state diagram:

```mermaid
stateDiagram-v2
    [*] --> AtStartBarrier
    AtStartBarrier --> Colliding: released [running]
    AtStartBarrier --> Terminated: released [not running]
    Colliding --> Moving: my rows collided and signalled
    Moving --> WaitingOnCell: awaitCollisionsComplete [pending > 0]
    WaitingOnCell --> Moving: signalAll [pending = 0]
    Moving --> AtEndBarrier: my rows moved
    AtEndBarrier --> AtStartBarrier: released
    AtStartBarrier --> Terminated: InterruptedException
    AtEndBarrier --> Terminated: InterruptedException
    Terminated --> [*]
```
