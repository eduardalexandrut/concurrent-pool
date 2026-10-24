# Analysis

The goal of the assignment is to design and develop a concurrent implementation of the
game Poool. The game consists of a bidimensional board containing a large number
of small balls and two larger balls controlled respectively by the human player and a
bot. Balls move according to simple physical rules, including friction and elastic collisions with other balls and
with the board boundaries. Two holes are located at the top corners of the board, and players score points by pushing
small balls into these holes. A player whose own ball falls into a hole loses; when no small ball is left, the player
with the higher score wins.

The simulation must support a potentially large number of balls (thousands or more),
all moving simultaneously and interacting through collisions. In particular, the following
operations must be performed repeatedly, at every frame:

- detection of collisions among balls
- resolution of elastic collisions
- update of ball positions
- application of friction
- transfer of balls across the board
- detection of balls entering holes
- score update and game termination checks

At the same time the user must be able to move their ball with the keyboard, and the board must be drawn on screen.

## Spatial decomposition

A simple implementation would require checking collisions between every pair of balls,
resulting in quadratic complexity with respect to the number of balls. This approach quickly becomes impractical
when the number of balls grows large.

To address this issue, I've decided to use **spatial decomposition**. The board is partitioned into a grid of cells,
and each ball belongs to exactly one cell according to its position. Collision detection is then restricted to:

- balls inside the same cell
- balls in adjacent cells

This drastically reduces the number of collision checks and enables parallel computation, since many cells can be
processed independently.

Checking only adjacent cells is correct as long as two touching balls are never more than one cell apart. Two balls
touch when the distance between their centres is smaller than the sum of their radii, so every side of a cell must be
at least as long as the diameter of the biggest ball. With the default configuration (1600x1200 board, 20x20 grid) the
cells are 80x60 and the player balls have a diameter of 60, so the condition holds.

## Concurrency problems

Processing the cells in parallel introduces some problems that the design must solve:

1. **Shared state between cells.** A collision between two balls in adjacent cells changes the balls of both cells,
   and a ball that moves out of a cell is removed from one list and added to another. Two threads could work on the
   same cell at the same time, so every cell needs **mutual exclusion**.
2. **Deadlock.** A collision across a border needs the locks of two cells. If two threads take the same two locks in
   opposite order, they wait for each other forever.
3. **Order between the phases.** The balls of a cell must not move while another thread is still computing collisions
   that involve that cell. In addition, a ball must move exactly once per frame, even if it changes cell during the
   movement phase.
4. **Frame synchronisation.** The game loop must not start a new frame (or reset the per-frame state) while the
   workers are still working on the previous one.
5. **Input and rendering.** The Swing thread (EDT) reads the balls to draw them and receives the key presses, while
   the workers are updating the same balls.
6. **Shared counters.** The scores can be updated by several workers at the same time.
7. **Termination.** When the game ends, the worker threads must stop.

The properties that the solution must guarantee are therefore:

- **Safety:** mutual exclusion on each cell, no ball lost or duplicated, every ball moved once per frame, no lost
  score update.
- **Liveness:** no deadlock, every frame completes, and the worker threads terminate when the engine is shut down.
