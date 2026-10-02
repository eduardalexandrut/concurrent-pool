# Analysis

The goal of the assignment is to design and develop a concurrent implementation of the
game Poool. The game consists of a bidimensional board containing a large number
of small balls and two larger balls controlled respectively by the human player and a
bot. Balls move according to simple physical rules, including friction and elastic collisions with other balls and with the board boundaries. Two holes are located at the
top corners of the board, and players score points by pushing small balls into these holes.

The simulation must support a potentially large number of balls (thousands or more),
all moving simultaneously and interacting through collisions. In particular, the following
operations must be performed repeatedly:

- detection of collisions among balls
- resolution of elastic collisions
- update of ball positions
- application of friction
- transfer of balls across the board
- detection of balls entering holes
- score update and game termination checks

A simple implementation would require checking collisions between every pair of balls,
resulting in quadratic complexity with respect to the number of balls. This approach quickly becomes impractical when the number of balls grows large.

To address this issue, I've decided to use **spatial decomposition**. The board is partitioned into a grid of cells,
and each ball belongs to exactly one cell according to its position. Collision detection is then restricted to:

- balls inside the same cell
- balls in adjacent cells

This drastically reduces the number of collision checks and enables parallel computation, since many cells can be processed independently.