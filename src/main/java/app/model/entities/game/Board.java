package app.model.entities.game;

import app.model.entities.physics.P2d;
import app.model.entities.physics.V2d;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Board {
        private final double width;
        private final double height;
        private final List<Ball> balls;

        public Board(double width, double height, int numBalls) {
            this.width = width;
            this.height = height;
            this.balls = new ArrayList<>();

            Random rand = new Random();
            for (int i = 0; i < numBalls; i++) {
                double x = 100 + Math.random() * (width - 50);
                double y = 100 + Math.random() * (height - 50);
                double vx = (Math.random() * 4) - 2;
                double vy = (Math.random() * 4) - 2;
                balls.add(new Ball(new P2d(x, y), 5, 1.0, new V2d(vx, vy)));
            }
        }

        public double getWidth() { return width; }
        public double getHeight() { return height; }

        // Returns a Boundary object that your Ball class expects
        public Boundary getBounds() {
            return new Boundary(0, 0, width, height);
        }

        public List<Ball> getBalls() {
            return this.balls.stream().toList();
        }

}
