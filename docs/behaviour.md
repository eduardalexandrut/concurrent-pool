# Behaviour

To model the concurrent behavior and synchronization flow of the physics engine—specifically the two-phase barrier 
coordination between the master thread and worker threads—we can utilize a **Petri Net** abstraction.

### Petri Net Model of the Two-Phase Barrier Cycle
In this model, places represent the operational states of the system or workers, while transitions represent 
synchronization events (such as entering the barrier, waiting, or releasing threads).

```mermaid
graph LR
    %% Places
    P1((Idle / Ready))
    P2((Resolving Collisions))
    P3((Waiting at Barrier 1))
    P4((Updating Movement))
    P5((Waiting at Barrier 2))

    %% Transitions
    T1[Start Frame]
    T2[Collisions Done]
    T3[All Workers Arrived]
    T4[Movement Done]
    T5[All Workers Arrived]

    %% Arcs
    P1 --> T1
    T1 --> P2
    P2 --> T2
    T2 --> P3
    P3 --> T3
    T3 --> P4
    P4 --> T4
    T4 --> P5
    P5 --> T5
    T5 --> P1

    style P1 fill:#e1f5fe,stroke:#0288d1
    style P3 fill:#fff9c4,stroke:#fbc02d
    style P5 fill:#fff9c4,stroke:#fbc02d
    style P2 fill:#c8e6c9,stroke:#388e3c
    style P4 fill:#c8e6c9,stroke:#388e3c
```

### Behavioral Description
- **Phase Initialization ($T_1$):** The master thread triggers a simulation tick, transitioning workers from idle/ready state
($P_1$) into the parallel collision resolution phase ($P_2$).
- **First Synchronization Barrier ($P_3 \rightarrow T_3$):** Once a worker completes its assigned rows, it reaches the 
SimpleBarrier ($P_3$). It suspends execution via condition variables until all peer worker threads arrive, avoiding premature state modifications.
- **Movement Phase ($P_4$):** Upon barrier release, workers transition to updating entity positions and velocities ($P_4$).
- **Second Barrier & Reset ($P_5 \rightarrow P_1$):** A second synchronization barrier ($P_5$) guarantees that spatial 
transfers and boundary checks are globally finalized before the frame counter increments and the cycle loops back to idle.