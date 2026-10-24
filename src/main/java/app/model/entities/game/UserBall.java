package app.model.entities.game;

import app.model.entities.physics.P2d;
import app.model.entities.physics.V2d;

/**
 * One of the two big balls (user or NPC).
 * Like every other ball, it is protected by the lock of the cell that contains it.
 */
public class UserBall extends Ball {

    public enum BALL_TYPE {
        HUMAN,
        BOT
    }

    private final BALL_TYPE type;

    public UserBall(P2d pos, double radius, double mass, V2d vel, BALL_TYPE type) {
        super(pos, radius, mass, vel);
        this.type = type;
    }

    public BALL_TYPE getType() {
        return this.type;
    }
}
