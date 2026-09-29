package app.model.physics;

public record Hole(P2d position, int radius) {

    public boolean contains(final Ball ball) {
        double dx = ball.getPos().x() - position.x();
        double dy = ball.getPos().y() - position.y();

        double distanceSquared = (dx * dx) + (dy * dy);
        double radiusSquared = (double) radius * radius;

        return distanceSquared <= radiusSquared;
    }
}

