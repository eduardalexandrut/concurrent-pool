package app.model;

import app.model.entities.game.Ball;
import app.model.entities.game.BallState;
import app.model.entities.game.Board;
import app.model.entities.game.Hole;
import app.model.entities.physics.P2d;

import java.util.List;

public interface Physics {

    /**
     * Main method that calculates the balls position and handles the collisions
     * @param dt time
     */
    void computeState(long dt);


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

    /**
     * Method to update the last toucher field of a ball, after a collision happened.
     * It is necessary in order to track the entity that scored a goal.
     * @param b1 first ball
     * @param b2 second ball
     */
    void updateToucher(Ball b1, Ball b2);
}
