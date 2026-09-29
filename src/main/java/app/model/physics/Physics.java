package app.model.physics;

import java.util.List;

public interface Physics {

    /**
     * Main method that calculates the balls position and handles the collisions
     * @param dt time
     */
    void computeState(long dt);

    /**
     * @param position The User's ball position.
     */
    void updateUserBall(P2d position);

    /**
     * @param ball The NPC's ball.
     */
    void updateNPCBall(Ball ball);

    /**
     * @return the current state of the User's ball.
     */
    BallState getUserBallState();

    /**
     * @return the current state of the NPC's ball.
     */
    BallState getNPCBallState();

    /**
     * @return a snapshot DTO of all the balls' state.
     */
    List<BallState> getStateSnapshot();

    void updateRowMovement(int r, long dt);

    void resolveRowCollisions(int r);

    void updateUserVelocity(double vx, double vy);

    void setFPS(int fps);

    int getCurrentFPS();

    Hole getLeftHole();

    Hole getRightHole();

    Board getBoard();

    double getCellH();

    double getCellW();

    void incrementUserScore();

    void incrementNpcScore();

    Ball getUserBall();

    Ball getNPCBall();

    int getUserScore();

    int getNPCScore();

    AbstractPhysics.GameState getGameState();

    void setGameState(AbstractPhysics.GameState gameState);

    void signalCollisionsDoneForRow(int r);
}
