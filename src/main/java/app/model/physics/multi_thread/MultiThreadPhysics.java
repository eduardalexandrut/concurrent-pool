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
        final int baseRowsPerThread = this.rows / nThreads;
        final int remainder = this.rows % nThreads;

        this.barrier = new SimpleBarrier(nThreads + 1);

        int currentRow = 0;

        for (int i = 0; i < nThreads; i++) {
            int start = currentRow;

            // Add an extra row for the first remainder threads
            int rowsForThisThread = baseRowsPerThread + (i < remainder ? 1 : 0);
            int end = start + rowsForThisThread - 1;

            workers[i] = new PhysicsWorker(this, start, end, barrier);
            workers[i].start();

            currentRow = end + 1;
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
