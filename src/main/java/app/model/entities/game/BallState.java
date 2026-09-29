package app.model.entities.game;

import app.model.entities.physics.P2d;
import app.model.entities.physics.V2d;

public record BallState(P2d pos, V2d vel, double radius) {

    public static BallState fromBall(final Ball ball){
        return new BallState(ball.getPos(), ball.getVel(), ball.getRadius());
    }
}
