package app;

import app.controller.ControllerImpl;
import app.model.entities.game.Board;
import app.model.physics.executor.ExecutorPhysics;
import app.model.physics.multi_thread.MultiThreadPhysics;
import app.model.Physics;
import app.model.physics.PhysicsThread;
import app.view.View;

import javax.swing.*;

public class App {
    public static void main(String[] args) {
        Board board = new Board(1200, 1200);
        Physics physics = new ExecutorPhysics(board, 20, 20);

        // Start the Engines
        ControllerImpl controller = new ControllerImpl(physics);

        PhysicsThread physicsThread = new PhysicsThread(controller);
        physicsThread.start();

        SwingUtilities.invokeLater(() -> {
            View view = new View(controller, 1200, 1200);
            view.setVisible(true);
        });
    }
}
