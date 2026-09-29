package app.model.physics;

public class SequentialPhysics extends AbstractPhysics implements Physics {

    public SequentialPhysics(Board board, int rows, int cols) {
        super(board, rows, cols);
        this.transferToCorrectCell(this.npcBall);
        this.transferToCorrectCell(this.userBall);

        this.syncBoard(board);
    }

    @Override
    protected void runParallelStep(long dt) {
        for (int r = 0; r < this.rows; r++) {
            this.resolveRowCollisions(r);
            this.signalCollisionsDoneForRow(r);
        }

        for (int r = 0; r < rows; r++) {
            updateRowMovement(r, dt);
        }
    }
}
