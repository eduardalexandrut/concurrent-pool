package app.model.entities.game;

import app.model.entities.physics.P2d;

public record Hole(P2d position, int radius) {

    public boolean contains(final Ball ball) {
        double dx = ball.getPos().x() - position.x();
        double dy = ball.getPos().y() - position.y();

        double distanceSquared = (dx * dx) + (dy * dy);
        double radiusSquared = (double) radius * radius;

        return distanceSquared <= radiusSquared;
    }
}

