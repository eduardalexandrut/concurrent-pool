package app.model.active_components;

import app.model.AbstractPhysics;
import app.model.Physics;
import app.model.entities.game.Ball;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Cell {
    private final int id;
    private final List<Ball> balls = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private int pendingCollisionWorkers;
    private final Condition collisionsComplete;
    private final Physics model;

    public Cell(int id, Physics model) {
        this.id = id;
        this.model = model;
        this.collisionsComplete = lock.newCondition();
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
        } finally { lock.unlock(); }
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
        } finally { lock.unlock(); }
    }

    /**
     * Blocks the current thread until all expected workers have signaled that they are
     * done with their collision resolution phase for this cell.
     * Uses a condition variable to wait safely, avoiding spurious wakeups.
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public void awaitCollisionsComplete() throws InterruptedException {
        lock.lock();
        try {
            while (pendingCollisionWorkers > 0) {
                collisionsComplete.await();
            }
        } finally { lock.unlock(); }
    }

    /**
     * Updates the movement and state of all balls currently within this cell based on the elapsed time.
     * It also handles game logic such as checking if a ball has fallen into a hole, updating scores,
     * and determining win conditions. Balls that have moved out of the cell's spatial boundaries
     * are removed from this cell's internal list and returned.
     *
     * @param dt the elapsed time (delta time) in milliseconds since the last frame
     * @param r  the row index of this cell
     * @param c  the column index of this cell
     * @return a list of balls that have exited this cell's boundaries during the update
     */
    public List<Ball> updateMovement(long dt, int r, int c) {
        lock.lock();
        try {

            List<Ball> exited = new ArrayList<>();
            Iterator<Ball> it = balls.iterator();
            while (it.hasNext()) {
                Ball b = it.next();
                b.updateState(dt, this.model.getBoard());

                // Check if the ball is inside a hole
                if (model.getLeftHole().contains(b) || model.getRightHole().contains(b)) {
                    if (b.equals(model.getUserBall())) {
                        model.setGameState(AbstractPhysics.GameState.NPC_WON);
                    } else if (b.equals(model.getNPCBall())) {
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
     * It safely acquires the cell's lock, iterates through all unique pairs of balls, and applies
     * physics and game logic (such as updating the last toucher) if a collision is detected.
     */
    public void resolveInternalCollisions() {
        this.lock();

        // Handle internal collisions
        try {
            for (int i = 0; i < this.balls.size(); i++) {
                final Ball b1 = this.balls.get(i);
                for (int j = i + 1; j < this.balls.size(); j++) {
                    final Ball b2 = this.balls.get(j);

                    if (Ball.areColliding(b1, b2)) {
                        this.model.updateToucher(b1, b2);
                        Ball.resolveCollision(b1, b2);
                    }
                }
            }
        } finally {
            this.unlock();
        }
    }


    /**
     * Safely adds a ball to this cell's internal collection.
     * This method acquires the cell's lock to ensure thread-safe insertion.
     *
     * @param b the ball to be added to this cell
     */
    public void addBall(Ball b) {
        lock.lock();
        try { balls.add(b); } finally { lock.unlock(); }
    }

    /**
     * Acquires the reentrant lock for this cell.
     * Should be used explicitly when multiple cells need to be locked simultaneously
     * (e.g., during boundary collision resolution).
     */
    public void lock() { lock.lock(); }

    /**
     * Releases the reentrant lock for this cell.
     * Must be called in a finally block after acquiring the lock.
     */
    public void unlock() { lock.unlock(); }

    /**
     * Retrieves the internal list of balls contained in this cell.
     * Note: External access and iteration over this list should be done while holding the cell's lock.
     *
     * @return the list of balls currently in this cell
     */
    public List<Ball> getBalls() { return balls; }

    /**
     * Returns the unique identifier of this cell.
     *
     * @return the cell ID
     */
    public int getId() { return id; }
}
