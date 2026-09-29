package app.model;

import app.model.active_components.Cell;
import app.model.entities.game.*;
import app.model.entities.physics.P2d;
import app.model.entities.physics.V2d;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractPhysics implements Physics {
    public enum GameState {
        RUNNING,
        USER_WON,
        NPC_WON,
        DRAW,
        STOPPED
    }
    protected final Board board;
    protected Cell[][] cells;
    protected UserBall userBall;
    protected Ball npcBall;
    protected final int rows, cols;
    protected final double cellWidth, cellHeight;
    protected final AtomicInteger userScore;
    protected final AtomicInteger npcScore;
    protected final Hole leftHole;
    protected final Hole rightHole;
    protected AtomicReference<AbstractPhysics.GameState> gameState = new AtomicReference<>();
    private volatile int currentFPS;

    public AbstractPhysics(Board board, int rows, int cols) {
        this.board = board;
        this.cells = new Cell[rows][cols];

        this.rows = rows;
        this.cols = cols;

        this.cellWidth = board.getWidth() / cols;
        this.cellHeight = board.getHeight() / rows;

        int cellId = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                this.cells[i][j] = new Cell(cellId, this);
                cellId = cellId + 1;
            }
        }

        this.userScore = new AtomicInteger(0);
        this.npcScore = new AtomicInteger(0);

        this.leftHole = new Hole(new P2d(0,0), 100);
        this.rightHole = new Hole(new P2d(board.getWidth(), 0), 100);

        this.npcBall = new Ball(
                new P2d(300, 300),
                30,
                10.0,
                new V2d(10, 10)
        );
        //this.transferToCorrectCell(this.npcBall);

        this.userBall = new UserBall(
                new P2d(300, 300),
                30,
                100.0,
                new V2d(10, 10)
        );
        //this.transferToCorrectCell(this.userBall);

        this.gameState.set(AbstractPhysics.GameState.RUNNING);

        //this.syncBoard(board);

    }



    @Override
    public final void computeState(long dt) {
        initFrame();
        runParallelStep(dt);
    }

    protected abstract void runParallelStep(long dt);

    private void initFrame() {
        // Initialize each cell's collision counter before spawning workers
        for (int r = 0; r < rows; r++) {
            int expected = (r == 0) ? 1 : 2;
            for (int c = 0; c < cols; c++) {
                cells[r][c].initFrame(expected);
            }
        }
    }

    protected void syncBoard(final Board board) {

        for (final Ball ball : board.getBalls()) {
            int r = (int) (ball.getPos().y() / this.cellHeight);
            int c = (int)  (ball.getPos().x() / this.cellWidth);

            // Boundary check
            r = Math.max(0, Math.min(r, rows - 1));
            c = Math.max(0, Math.min(c, cols - 1));

            this.cells[r][c].addBall(ball);
        }
    }

    protected void transferToCorrectCell(Ball b) {
        // 1. Calculate the indices based on the ball's current position
        int r = (int) (b.getPos().y() / this.cellHeight);
        int c = (int) (b.getPos().x() / this.cellWidth);

        // 2. Safety Clamp: Ensure the ball doesn't fly off the array indices
        // (e.g., if it hits a boundary exactly or slightly exceeds it)
        r = Math.max(0, Math.min(r, rows - 1));
        c = Math.max(0, Math.min(c, cols - 1));

        // 3. Call the Monitor method of the target cell
        // This method handles its own locking internally.
        this.cells[r][c].addBall(b);
    }

    @Override
    public void resolveRowCollisions(int r) {

        for (int c = 0; c < cols; c++) {

            final Cell current = this.cells[r][c];

            // Handle internal collisions
            current.resolveInternalCollisions();

            // Handle collision among balls of different cells at the borders
            int[][] directions = {{0, 1}, {1, 1}, {1, 0}, {1, -1}};

            for (int[] d:  directions) {
                int nr =  r + d[0];
                int nc = c + d[1];

                if (isValid(nr, nc)) {
                    multiLockResolver(r, c, nr, nc);
                }
            }
        }
    }

    private void multiLockResolver(int r, int c, int nr, int nc) {
        final Cell cell1 = this.cells[r][c];
        final Cell cell2 = this.cells[nr][nc];

        final Cell first = cell1.getId() > cell2.getId() ? cell1 : cell2;
        final Cell second = cell2.getId() > cell1.getId() ? cell2 : cell1;

        first.lock();
        second.lock();

        try {
            List<Ball> balls1 = new ArrayList<>(cell1.getBalls()); // snapshot
            List<Ball> balls2 = new ArrayList<>(cell2.getBalls()); // snapshot

            for (Ball ballA : balls1) {
                for (Ball ballB : balls2) {
                    Ball.resolveCollision(ballA, ballB);
                }
            }
        } finally {
            second.unlock();
            first.unlock();
        }
    }


    private boolean isValid(int r, int c) {
        return r >= 0 && r < this.rows && c >= 0 && c < this.cols;
    }

    @Override
    public void updateUserBall(P2d position) {

    }

    @Override
    public void updateNPCBall(Ball ball) {

    }

    @Override
    public BallState getUserBallState() {
        return BallState.fromBall(this.userBall);
    }

    @Override
    public BallState getNPCBallState() {
        return BallState.fromBall(this.npcBall);
    }

    @Override
    public List<BallState> getStateSnapshot() {
        List<BallState> snapshot = new ArrayList<>();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Cell cell = cells[i][j];

                cell.lock();
                try {
                    for (Ball b : cell.getBalls()) {
                        snapshot.add(BallState.fromBall(b));
                    }
                } finally {
                    cell.unlock();
                }
            }
        }
        return snapshot;
    }

    @Override
    public void updateRowMovement(int r, long dt) {
        for (int c = 0; c < cols; c++) {
            try {
                cells[r][c].awaitCollisionsComplete();
                List<Ball> leavers = cells[r][c].updateMovement(dt, r, c);

                for (Ball b : leavers) {
                    transferToCorrectCell(b);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void updateUserVelocity(double vx, double vy) {
        this.userBall.setVelocity(new V2d(vx, vy));
    }

    @Override
    public int getCurrentFPS() {
        return this.currentFPS;
    }

    @Override
    public Hole getLeftHole() {
        return this.leftHole;
    }

    @Override
    public Hole getRightHole() {
        return this.rightHole;
    }

    @Override
    public Board getBoard() {
        return this.board;
    }

    @Override
    public double getCellH() {
        return this.cellHeight;
    }

    @Override
    public double getCellW() {
        return this.cellWidth;
    }

    @Override
    public void incrementUserScore() {
        this.userScore.set(this.userScore.get() + 1);
    }

    @Override
    public void incrementNpcScore() {
        this.npcScore.set(this.npcScore.get() + 1);
    }

    @Override
    public Ball getUserBall() {
        return this.userBall;
    }

    @Override
    public Ball getNPCBall() {
        return this.npcBall;
    }

    @Override
    public int getUserScore() {
        return this.userScore.get();
    }

    @Override
    public int getNPCScore() {
        return this.npcScore.get();
    }

    @Override
    public AbstractPhysics.GameState getGameState() {
        return this.gameState.get();
    }

    @Override
    public void setGameState(AbstractPhysics.GameState gameState) {
        this.gameState.set(gameState);
    }

    @Override
    public void setFPS(int fps) {
        this.currentFPS = fps;
    }

    @Override
    public void signalCollisionsDoneForRow(int r) {
        // Signal all cells in my row
        for (int c = 0; c < cols; c++) {
            cells[r][c].signalCollisionsDone();
        }
        // Signal the boundary row below (if it exists)
        if (r + 1 < rows) {
            for (int c = 0; c < cols; c++) {
                cells[r + 1][c].signalCollisionsDone();
            }
        }
    }
}
