package app.controller;


import app.model.physics.BallState;
import app.model.physics.Hole;

import java.util.List;

public interface Controller {

    void processInput(String key);

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

    String getCurrentFPS();

    Hole getLeftHole();

    Hole getRightHole();

    String getUserScore();

    String getNPCScore();
}
