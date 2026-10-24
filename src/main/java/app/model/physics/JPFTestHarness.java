package app.model.physics;


import app.model.Physics;
import app.model.entities.game.Board;
import app.model.entities.game.Ball;
import app.model.entities.physics.P2d;
import app.model.entities.physics.V2d;
import app.model.physics.multi_thread.MultiThreadPhysics;

public class JPFTestHarness {

    public static void main(String[] args) {
        // 1. Simulate a very small environment
        Board board = new Board(200, 200, 0);

        board.getBalls().clear();

        // 2. Ball setup on the borders
        Ball b1 = new Ball(new P2d(99, 99), 10, 1.0, new V2d(2, 2));
        Ball b2 = new Ball(new P2d(101, 101), 10, 1.0, new V2d(-2, -2));
        Ball b3 = new Ball(new P2d(20, 20), 10, 1.0, new V2d(0, 0));

        board.getBalls().add(b1);
        board.getBalls().add(b2);
        board.getBalls().add(b3);

        // 3. Init Physisics
        Physics physics = new MultiThreadPhysics(board, 2, 2, 2);

        // 4. Simulate only 2 frames
        long dt = 16;
        physics.computeState(dt);
        physics.computeState(dt);

        int totalBalls = physics.getStateSnapshot().size();

        // Check that no ball has been overwritten or lost due to race conditions
        assert totalBalls == 3 : "RACE CONDITION: Number of balls altered!";

        System.out.println("Test JPF completed.");
    }
}