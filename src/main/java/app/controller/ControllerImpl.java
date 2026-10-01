package app.controller;


import app.model.entities.game.BallState;
import app.model.entities.game.Hole;
import app.model.Physics;

import java.util.List;

public class ControllerImpl implements Controller {
    private Physics model;

    public ControllerImpl(Physics model) {
        this.model = model;
    }

    @Override
    public Physics getModel() {
        return this.model;
    }

    @Override
    public void setModel(Physics model) {
        this.model = model;
    }

    @Override
    public void processInput(String key) {
        new MoveUserCmd(key).execute(model);
    }

    @Override
    public BallState getUserBallState() {
        return this.model.getUserBallState();
    }

    @Override
    public BallState getNPCBallState() {
        return this.model.getNPCBallState();
    }

    @Override
    public List<BallState> getStateSnapshot() {
        return this.model.getStateSnapshot();
    }

    @Override
    public String getCurrentFPS() {
        return String.valueOf(this.model.getCurrentFPS());
    }

    @Override
    public Hole getLeftHole() {
        return this.model.getLeftHole();
    }

    @Override
    public Hole getRightHole() {
        return this.model.getRightHole();
    }

    @Override
    public String getUserScore() {
        return String.valueOf(this.model.getUserScore());
    }

    @Override
    public String getNPCScore() {
        return String.valueOf(this.model.getNPCScore());
    }
}
