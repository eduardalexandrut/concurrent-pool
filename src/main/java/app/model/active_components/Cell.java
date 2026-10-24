package app.model.active_components;

import app.model.AbstractPhysics;
import app.model.PhysicsKernel;
import app.model.entities.game.Ball;
import app.model.entities.game.BallState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A cell of the grid, implemented as a monitor.
 * The lock, the list of balls and the counter are private: every access goes through a method of this class.
 */
public class Cell {
    private final int id;
    private final List<Ball> balls = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition collisionsComplete = lock.newCondition();
    private int pendingCollisionWorkers;
    private final PhysicsKernel model;

    public Cell(int id, PhysicsKernel model) {
        this.id = id;
        this.model = model;
    }

    /**
     * Initializes the state of the cell for a new physics frame.
     * Sets the number of expected worker threads that must finish their collision
     * resolution phase before this cell can safely proceed to the movement phase.
     *
     * @param expectedWorkers the number of workers that will interact with this cell
     */
    public void initFrame(int expectedWorkers) {
        lock.lock();
        try {
            this.pendingCollisionWorkers = expectedWorkers;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Signals that a worker thread has finished its collision resolution work touching this cell.
     * Decrements the internal counter of pending workers. If the counter reaches zero,
     * it wakes up all threads waiting on the collisions complete condition.
     */
    public void signalCollisionsDone() {
        lock.lock();
        try {
            pendingCollisionWorkers--;
            if (pendingCollisionWorkers == 0) {
                collisionsComplete.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Blocks the current thread until all expected workers have signaled that they are
     * done with their collision resolution phase for this cell.
     * The condition is checked in a loop to handle spurious wakeups.
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public void awaitCollisionsComplete() throws InterruptedException {
        lock.lock();
        try {
            while (pendingCollisionWorkers > 0) {
                collisionsComplete.await();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Updates the movement and state of all balls currently within this cell based on the elapsed time.
     * It also handles game logic such as checking if a ball has fallen into a hole, updating scores,
     * and determining win conditions. Balls that have moved out of the cell's spatial boundaries
     * are removed from this cell's internal list and returned.
     *
     * <p>A ball that was already moved in this frame (it arrived from a cell processed earlier)
     * is skipped, so that every ball moves exactly once per frame.</p>
     *
     * @param dt    the elapsed time (delta time) in milliseconds since the last frame
     * @param frame the number of the current frame
     * @param r     the row index of this cell
     * @param c     the column index of this cell
     * @return a list of balls that have exited this cell's boundaries during the update
     */
    public List<Ball> updateMovement(long dt, long frame, int r, int c) {
        lock.lock();
        try {
            List<Ball> exited = new ArrayList<>();
            Iterator<Ball> it = balls.iterator();
            while (it.hasNext()) {
                Ball b = it.next();

                // Already moved in this frame by the cell it came from
                if (b.getLastMovedFrame() == frame) {
                    continue;
                }
                b.setLastMovedFrame(frame);
                b.updateState(dt, this.model.getBoard());

                // Check if the ball is inside a hole
                if (model.getLeftHole().contains(b) || model.getRightHole().contains(b)) {
                    if (b == model.getUserBall()) {
                        model.setGameState(AbstractPhysics.GameState.NPC_WON);
                    } else if (b == model.getNPCBall()) {
                        model.setGameState(AbstractPhysics.GameState.USER_WON);
                    } else {
                        if (b.isScorableBy(Ball.CHARACTERS.HUMAN)) {
                            model.incrementUserScore();
                        } else if (b.isScorableBy(Ball.CHARACTERS.NPC)) {
                            model.incrementNpcScore();
                        }
                    }
                    it.remove(); // Ball is gone
                    continue;
                }

                int nr = (int) (b.getPos().y() / model.getCellH());
                int nc = (int) (b.getPos().x() / model.getCellW());

                if (nr != r || nc != c) {
                    exited.add(b);
                    it.remove();
                }
            }
            return exited;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Resolves physical collisions between all pairs of balls currently residing inside this cell.
     */
    public void resolveInternalCollisions() {
        lock.lock();
        try {
            for (int i = 0; i < balls.size(); i++) {
                Ball b1 = balls.get(i);
                for (int j = i + 1; j < balls.size(); j++) {
                    collide(b1, balls.get(j));
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Resolves collisions between the balls of this cell and the balls of an adjacent cell.
     * Both locks are needed: they are always acquired in decreasing id order, so two threads
     * can never wait for each other in a cycle (no deadlock).
     *
     * @param other the adjacent cell
     */
    public void resolveCollisionsWith(Cell other) {
        Cell first = this.id > other.id ? this : other;
        Cell second = (first == this) ? other : this;

        first.lock.lock();
        try {
            second.lock.lock();
            try {
                for (Ball a : this.balls) {
                    for (Ball b : other.balls) {
                        collide(a, b);
                    }
                }
            } finally {
                second.lock.unlock();
            }
        } finally {
            first.lock.unlock();
        }
    }

    /**
     * Applies a collision only if the two balls are actually touching.
     * Must be called while holding the locks of the cells that own the two balls.
     */
    private void collide(Ball a, Ball b) {
        if (Ball.areColliding(a, b)) {
            model.updateToucher(a, b);
            Ball.resolveCollision(a, b);
        }
    }

    /**
     * Safely adds a ball to this cell.
     *
     * @param b the ball to be added to this cell
     */
    public void addBall(Ball b) {
        lock.lock();
        try {
            balls.add(b);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Adds the state of every ball of this cell to {@code out}, except the two given balls
     * (used to exclude the user and NPC balls from the snapshot).
     */
    public void collectStates(List<BallState> out, Ball skip1, Ball skip2) {
        lock.lock();
        try {
            for (Ball b : balls) {
                if (b != skip1 && b != skip2) {
                    out.add(BallState.fromBall(b));
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the unique identifier of this cell.
     *
     * @return the cell ID
     */
    public int getId() {
        return id;
    }
}
