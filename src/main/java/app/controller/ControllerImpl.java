package app.controller;


import app.model.physics.BallState;
import app.model.physics.Hole;
import app.model.physics.Physics;

import java.util.List;

public class ControllerImpl implements Controller {
//    private final BoundedBuffer<Cmd> cmdBuffer;
    private final Physics model;

    public ControllerImpl(Physics model) {
        this.model = model;
//        this.cmdBuffer = new BoundedBufferImpl<>(100);
    }

//    @Override
//    public void run() {
//        System.out.println("Controller Thread started...");
//        while (!isInterrupted()) {
//            try {
//                Cmd cmd = cmdBuffer.get();
//                cmd.execute(model);
//
//            } catch (InterruptedException e) {
//                System.out.println("Controller interrupted.");
//                break;
//            }
//        }
//    }

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
