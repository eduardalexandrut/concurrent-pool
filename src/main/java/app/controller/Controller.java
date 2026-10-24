package app.controller;


import app.model.Physics;
import app.model.entities.game.BallState;
import app.model.entities.game.Hole;

import java.util.List;

public interface Controller {

    /**
     * Retrieves the current physics model.
     *
     * @return the current {@link BallState} of the User's ball
     */
    Physics getModel();


    /**
     * Sets the current physics model.
     *
     * @param model physics
     */
    void setModel(Physics model);

    /**
     * Processes the user's keyboard input to update the velocity of the human player's ball.
     *
     * @param key a string representation of the pressed key (e.g., "UP", "DOWN", "LEFT", "RIGHT")
     */
    void processInput(String key);

    /**
     * Retrieves the current state of the human player's ball.
     *
     * @return the current {@link BallState} of the User's ball
     */
    BallState getUserBallState();

    /**
     * Retrieves the current state of the computer-controlled (bot) ball.
     *
     * @return the current {@link BallState} of the NPC's ball
     */
    BallState getNPCBallState();

    /**
     * Captures and returns a thread-safe snapshot of all standard balls currently on the board.
     * Useful for asynchronously updating the view without locking the underlying physics model.
     *
     * @return a list of {@link BallState} data transfer objects representing all the small balls
     */
    List<BallState> getStateSnapshot();

    /**
     * Retrieves the current frames per second (FPS) at which the physics engine or game loop is running.
     *
     * @return a string representation of the current FPS
     */
    String getCurrentFPS();

    /**
     * Retrieves the entity representing the left hole located at the top-left corner of the game board.
     *
     * @return the left {@link Hole}
     */
    Hole getLeftHole();

    /**
     * Retrieves the entity representing the right hole located at the top-right corner of the game board.
     *
     * @return the right {@link Hole}
     */
    Hole getRightHole();

    /**
     * Retrieves the current accumulated score of the human player.
     *
     * @return a string representation of the user's score
     */
    String getUserScore();

    /**
     * Retrieves the current accumulated score of the non-playable character (bot).
     *
     * @return a string representation of the NPC's score
     */
    String getNPCScore();

    /**
     * Retrieves the current state of the game.
     *
     * @return the GameState enum as a String (e.g., "RUNNING", "USER_WON", etc.)
     */
    String getGameState();
}
