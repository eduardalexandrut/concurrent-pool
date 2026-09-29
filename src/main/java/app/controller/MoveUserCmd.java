package app.controller;


import app.model.physics.Physics;

public class MoveUserCmd implements Cmd {
    private String key;

    public MoveUserCmd(String key) {
        this.key = key;
    }

    @Override
    public void execute(Physics model) {

        double speed = 200.0;
        double vx = 0, vy = 0;

        if (!key.startsWith("STOP_")) {
            switch (key.toUpperCase()) {
                case "W" -> vy = -speed;
                case "S" -> vy = speed;
                case "A" -> vx = -speed;
                case "D" -> vx = speed;
            }
            model.updateUserVelocity(vx, vy);
        }
    }
}
