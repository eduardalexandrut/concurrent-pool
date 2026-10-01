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

        this.npcBall = new UserBall(
                new P2d(300, 300),
                30,
                10.0,
                new V2d(10, 10),
                UserBall.BALL_TYPE.HUMAN
        );

        this.userBall = new UserBall(
                new P2d(300, 300),
                30,
                100.0,
                new V2d(10, 10),
                UserBall.BALL_TYPE.BOT
        );

        this.gameState.set(AbstractPhysics.GameState.RUNNING);
    }


    /**
     * Method that at each tick advances the game.
     * @param dt time
     */
    @Override
    public final void computeState(long dt) {
        initFrame();
        runParallelStep(dt);
    }

    /**
     * Executes a single physics simulation step for the specified time delta.
     * Subclasses must implement this method to define their specific concurrency model
     * and execution strategy (e.g., sequential, multi-threaded via default threads,
     * or task-based via Executor Framework).
     *
     * @param dt the elapsed time (delta time) in milliseconds since the last frame
     */
    protected abstract void runParallelStep(long dt);


    public void updateToucher(Ball b1, Ball b2) {
        // If b1 is the Human Player, b2 is now "Touched by Human"
        if (b1.equals(this.userBall)) {
            b2.setLastToucher(Ball.CHARACTERS.HUMAN);
            b2.setRemainingBounces(1);
        }
        // If b2 is the Human Player, b1 is now "Touched by Human"
        else if (b2.equals(this.userBall)) {
            b1.setLastToucher(Ball.CHARACTERS.HUMAN);
            b1.setRemainingBounces(1);
        }
        else if (b1.equals(this.npcBall)) {
            b2.setLastToucher(Ball.CHARACTERS.NPC);
            b2.setRemainingBounces(1);
        }
        // If b2 is the Human Player, b1 is now "Touched by Human"
        else if (b2.equals(this.npcBall)) {
            b1.setLastToucher(Ball.CHARACTERS.NPC);
            b1.setRemainingBounces(1);
        }
        // If two normal balls hit each other, they BOTH consume their "Direct Hit" status
        else {
            b1.consumeRemainingBounce();
            b2.consumeRemainingBounce();
        }
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
                        // Ignore User and NPC
                        if (b == this.userBall || b == this.npcBall) {
                            continue;
                        }
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

    /**
     * Initializes the synchronization state for all cells in the grid at the beginning of a physics frame.
     * Sets the expected number of pending collision workers that must finish before a cell can proceed
     * to the movement phase. The first row expects 1 worker, while all subsequent rows expect 2
     * (the worker for the current row and the worker for the row immediately above).
     */
    private void initFrame() {
        // Initialize each cell's collision counter before spawning workers
        for (int r = 0; r < rows; r++) {
            int expected = (r == 0) ? 1 : 2;
            for (int c = 0; c < cols; c++) {
                cells[r][c].initFrame(expected);
            }
        }
    }

    /**
     * Synchronizes the internal spatial grid with the provided game board by distributing all active balls
     * into their appropriate grid cells. The target cell is calculated based on the ball's spatial coordinates
     * and clamped to ensure it remains within the valid array boundaries.
     *
     * @param board the game board containing the list of balls to be distributed
     */
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

    /**
     * Calculates the target cell for a given ball based on its current spatial position and safely
     * adds it to that cell. The calculated grid coordinates are safely clamped to prevent out-of-bounds
     * access, and the thread-safe monitor method of the target cell is invoked.
     *
     * @param b the ball to be transferred to its correct spatial cell
     */
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

    /**
     * Resolves all physical collisions for the cells located in the specified row.
     * This process handles both internal collisions among balls within the same cell and boundary
     * collisions between balls in the current cell and those in adjacent cells. To prevent redundant
     * calculations, adjacent checks are strictly limited to four forward-facing directions
     * (Right, Bottom-Right, Bottom, and Bottom-Left).
     *
     * @param r the index of the row for which collisions should be resolved
     */
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

    /**
     * Safely resolves collisions between balls located in two distinct, adjacent cells.
     * Acquires the monitor locks of both cells simultaneously using a hierarchical lock-ordering
     * strategy (based on the cell ID) to prevent circular deadlocks. It operates on a snapshot
     * of the balls to avoid concurrent modification issues during the collision resolution.
     *
     * @param r  the row index of the first cell
     * @param c  the column index of the first cell
     * @param nr the row index of the second (adjacent) cell
     * @param nc the column index of the second (adjacent) cell
     */
    private void multiLockResolver(int r, int c, int nr, int nc) {
        final Cell cell1 = this.cells[r][c];
        final Cell cell2 = this.cells[nr][nc];

        final Cell first = cell1.getId() > cell2.getId() ? cell1 : cell2;
        final Cell second = cell1.getId() > cell2.getId() ? cell2 : cell1;

        first.lock();
        second.lock();

        try {
            List<Ball> balls1 = new ArrayList<>(cell1.getBalls()); // snapshot
            List<Ball> balls2 = new ArrayList<>(cell2.getBalls()); // snapshot

            for (Ball ballA : balls1) {
                for (Ball ballB : balls2) {
                    this.updateToucher(ballA, ballB);
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

}
