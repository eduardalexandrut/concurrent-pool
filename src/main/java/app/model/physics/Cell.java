package app.model.physics;

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
                        this.updateToucher(b1, b2);
                        Ball.resolveCollision(b1, b2);
                    }
                }
            }
        } finally {
            this.unlock();
        }
    }

    private void updateToucher(Ball b1, Ball b2) {
        // If b1 is the Human Player, b2 is now "Touched by Human"
        if (b1.equals(model.getUserBall())) {
            b2.setLastToucher(Ball.CHARACTERS.HUMAN);
            b2.setRemainingBounces(1);
        }
        // If b2 is the Human Player, b1 is now "Touched by Human"
        else if (b2.equals(model.getUserBall())) {
            b1.setLastToucher(Ball.CHARACTERS.HUMAN);
            b1.setRemainingBounces(1);
        }
        else if (b1.equals(model.getNPCBall())) {
            b2.setLastToucher(Ball.CHARACTERS.NPC);
            b2.setRemainingBounces(1);
        }
        // If b2 is the Human Player, b1 is now "Touched by Human"
        else if (b2.equals(model.getNPCBall())) {
            b1.setLastToucher(Ball.CHARACTERS.NPC);
            b1.setRemainingBounces(1);
        }
        // If two normal balls hit each other, they BOTH consume their "Direct Hit" status
        else {
            b1.consumeRemainingBounce();
            b2.consumeRemainingBounce();
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
