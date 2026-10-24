---
layout: default
title: Home
---

# Concurrent Pool Project

Welcome to the documentation and technical report for **Concurrent Pool**.

Use the navigation below or explore the sections to learn more:

* [Analysis](analysis.md)
* [Design](design.md)
* [Behaviour](behaviour.md)
* [Model Checking](model-checking.md)
* [Performance Evaluation](performance-evaluation.md)

## Abstract
A multi-threaded physics simulation of a pool game developed in Java 21. The board is divided into a grid of cells,
and each cell is a **monitor** (`ReentrantLock` + `Condition`) that owns the balls inside it. Every frame is computed
in two phases, collisions and movement, by a set of worker threads that meet on a custom reusable barrier at the
start and at the end of the frame. Between the two phases there is no global barrier: a cell waits only for the
workers that actually touch it.

The physics engine has three implementations: sequential, multithreaded with platform threads, and task-based with
an `ExecutorService`. The multithreaded version was verified with **Java PathFinder** (no deadlocks, no data races,
no balls lost) and the three versions are compared in the performance evaluation.
