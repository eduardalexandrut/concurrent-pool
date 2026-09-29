package app.model.physics;

public class PhysicsWorker extends Thread {
    private final Physics physics;
    private final SimpleBarrier barrier;
    private final int startRow, endRow;
    private volatile long dt;

    public PhysicsWorker(Physics physics, int startRow, int endRow, SimpleBarrier barrier) {
        this.physics = physics;
        this.startRow = startRow;
        this.endRow = endRow;
        this.barrier = barrier;
    }

    public void setDt(long dt) {
        this.dt = dt;
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            barrier.await();

            // Phase 1: resolve collisions for my rows
            for (int r = startRow; r <= endRow; r++) {
                physics.resolveRowCollisions(r);
                // Signal all cells in row r AND row r+1 boundary that I'm done touching them
                physics.signalCollisionsDoneForRow(r);
            }

            // Phase 2: update movement — each cell blocks until all collision workers are done
            for (int r = startRow; r <= endRow; r++) {
                physics.updateRowMovement(r, dt);
            }

            barrier.await();
        }

    }

}
