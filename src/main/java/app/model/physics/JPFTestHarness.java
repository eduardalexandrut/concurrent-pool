package app.model.physics;


import app.model.entities.game.Ball;
import app.model.entities.game.Board;
import app.model.entities.physics.P2d;
import app.model.entities.physics.V2d;
import app.model.physics.multi_thread.MultiThreadPhysics;

import java.util.ArrayList;
import java.util.List;

public class JPFTestHarness {

    public static void main(String[] args) {
        int frames = args.length > 0 ? Integer.parseInt(args[0]) : 1;

        //Ball setup: two balls on the border between the two rows, moving towards each other,
        //    and a third one far from the others
        List<Ball> balls = new ArrayList<>();
        balls.add(new Ball(new P2d(200, 197), 5, 1.0, new V2d(0, 50)));
        balls.add(new Ball(new P2d(200, 204), 5, 1.0, new V2d(0, -50)));
        balls.add(new Ball(new P2d(200, 300), 5, 1.0, new V2d(0, 0)));

        // Very small environment: 2 rows x 1 column, 2 workers (one row each)
        Board board = new Board(400, 400, balls);
        MultiThreadPhysics physics = new MultiThreadPhysics(board, 2, 1, 2);

        // Simulate a few frames, then stop the workers
        for (int i = 0; i < frames; i++) {
            physics.computeState(16);
        }
        int totalBalls = physics.getStateSnapshot().size();
        physics.shutdown();

        // Check that no ball has been lost or duplicated due to race conditions
        if (totalBalls != 3) {
            throw new AssertionError("RACE CONDITION: Number of balls altered: " + totalBalls);
        }

        System.out.println("Test JPF completed.");
    }
}
