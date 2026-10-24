package app.model;

import app.model.entities.game.Ball;
import app.model.entities.game.Board;
import app.model.entities.game.Hole;

/**
 * Internal interface of the physics engine, used by the cells and the worker threads.
 */
public interface PhysicsKernel {

    void resolveRowCollisions(int r);

    void signalCollisionsDoneForRow(int r);

    void updateRowMovement(int r, long dt);

    /**
     * @return false once shutdown() has been called: workers must terminate.
     */
    boolean isRunning();

    /**
     * Method to update the last toucher field of a ball, after a collision happened.
     * It is necessary in order to track the entity that scored a goal.
     * @param b1 first ball
     * @param b2 second ball
     */
    void updateToucher(Ball b1, Ball b2);

    Board getBoard();

    double getCellH();

    double getCellW();

    Hole getLeftHole();

    Hole getRightHole();

    Ball getUserBall();

    Ball getNPCBall();

    void incrementUserScore();

    void incrementNpcScore();

    void setGameState(AbstractPhysics.GameState gameState);
}
