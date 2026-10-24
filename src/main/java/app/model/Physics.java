package app.model;

import app.model.entities.game.Ball;
import app.model.entities.game.BallState;
import app.model.entities.game.Hole;

import java.util.List;

/**
 * Public interface of the physics engine, used by the Controller and the game loop.
 * The per-row methods used by the workers are in {@link PhysicsKernel}.
 */
public interface Physics {

    /**
     * Main method that calculates the balls position and handles the collisions
     * @param dt time
     */
    void computeState(long dt);

    /**
     * Stops the engine: worker threads terminate. After this call computeState must not be called.
     */
    void shutdown();

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

    /**
     * Requests a new velocity for the user's ball. It is applied at the start of the next frame.
     */
    void updateUserVelocity(double vx, double vy);

    void setFPS(int fps);

    int getCurrentFPS();

    Hole getLeftHole();

    Hole getRightHole();

    Ball getNPCBall();

    int getUserScore();

    int getNPCScore();

    AbstractPhysics.GameState getGameState();

    void setGameState(AbstractPhysics.GameState gameState);
}
