package app.model.physics;

public record BallState(P2d pos, V2d vel, double radius) {

    public static BallState fromBall(final Ball ball){
        return new BallState(ball.getPos(), ball.getVel(), ball.getRadius());
    }
}
