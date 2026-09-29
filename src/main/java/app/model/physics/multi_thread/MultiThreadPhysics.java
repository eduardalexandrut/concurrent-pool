package app.model.physics.multi_thread;

import app.model.AbstractPhysics;
import app.model.Physics;
import app.model.active_components.SimpleBarrier;
import app.model.entities.game.Board;
import app.model.physics.executor.PhysicsWorker;

public class MultiThreadPhysics extends AbstractPhysics implements Physics {

    private SimpleBarrier barrier;
    private PhysicsWorker[] workers;

    public MultiThreadPhysics(Board board, int rows, int cols) {
        super(board, rows, cols);

        this.transferToCorrectCell(this.npcBall);
        this.transferToCorrectCell(this.userBall);

        this.syncBoard(board);

        int nThreads = Runtime.getRuntime().availableProcessors();
        this.workers = new PhysicsWorker[nThreads];
        final int rowsPerThread = this.rows / nThreads;

        this.barrier = new SimpleBarrier(nThreads + 1);

        // Init worker threads
        for (int i = 0; i < nThreads; i++) {
            int start = i * rowsPerThread;
            int end = (i == nThreads - 1) ? rows - 1 : (start + rowsPerThread - 1);

            workers[i] = new PhysicsWorker(this, start, end, barrier);
            workers[i].start();
        }

    }

    @Override
    protected void runParallelStep(long dt) {
        for (PhysicsWorker w : workers) {
            w.setDt(dt);
        }

        barrier.await();
        barrier.await();
    }

}
