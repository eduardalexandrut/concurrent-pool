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

    // Called at frame start by the coordinator
    public void initFrame(int expectedWorkers) {
        lock.lock();
        try {
            this.pendingCollisionWorkers = expectedWorkers;
        } finally { lock.unlock(); }
    }

    // Called by a worker when it has finished all collision work touching this cell
    public void signalCollisionsDone() {
        lock.lock();
        try {
            pendingCollisionWorkers--;
            if (pendingCollisionWorkers == 0) {
                collisionsComplete.signalAll();
            }
        } finally { lock.unlock(); }
    }

    // Called by the owning worker before updating movement — blocks until safe
    public void awaitCollisionsComplete() throws InterruptedException {
        lock.lock();
        try {
            while (pendingCollisionWorkers > 0) {
                collisionsComplete.await();
            }
        } finally { lock.unlock(); }
    }

    /**
     * Entry point for the Monitor: Update movement.
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


    public void addBall(Ball b) {
        lock.lock();
        try { balls.add(b); } finally { lock.unlock(); }
    }


    public void lock() { lock.lock(); }
    public void unlock() { lock.unlock(); }
    public List<Ball> getBalls() { return balls; }
    public int getId() { return id; }
}
