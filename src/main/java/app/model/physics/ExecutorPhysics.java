package app.model.physics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecutorPhysics extends AbstractPhysics implements Physics {
    private final ExecutorService executor;

    public ExecutorPhysics(Board board, int rows, int cols) {
        super(board, rows, cols);

        this.transferToCorrectCell(this.npcBall);
        this.transferToCorrectCell(this.userBall);

        this.syncBoard(board);

        int nThreads = Runtime.getRuntime().availableProcessors();
        this.executor = Executors.newFixedThreadPool(nThreads);
    }

    @Override
    protected void runParallelStep(long dt) {
        List<Callable<Void>> collisionTasks = new ArrayList<>();

        for (int r = 0; r <= rows; r++) {
            final int row = r;
            collisionTasks.add(() -> {
                // collisions
                resolveRowCollisions(row);
                signalCollisionsDoneForRow(row);
                return null;
            });
        }

        try {
            executor.invokeAll(collisionTasks); // WAIT ALL
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Phase 2: Movement

        List<Callable<Void>> movementTasks = new ArrayList<>();

        for (int r = 0; r <= rows; r++) {
            final int row = r;
            movementTasks.add(() -> {
                // movements
                updateRowMovement(row, dt);
                return null;
            });
        }

        try {
            executor.invokeAll(movementTasks); // WAIT ALL
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


}
