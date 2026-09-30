package app.model.entities.game;

import app.model.entities.physics.P2d;
import app.model.entities.physics.V2d;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Board {
        private final int NUM_BALLS = 10_000;
        private final double width;
        private final double height;
        private final List<Ball> balls;

        public Board(double width, double height) {
            this.width = width;
            this.height = height;
            this.balls = new ArrayList<>();

            Random rand = new Random();
            for (int i = 0; i < NUM_BALLS; i++) {
                this.balls.add(new Ball(
                        new P2d(rand.nextInt((int) width) + 50, rand.nextInt((int) height) + 50),
                        3, 1.0,
                        new V2d(0, 0)
                ));
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
