package app;

import app.controller.ControllerImpl;
import app.model.entities.game.Board;
import app.model.physics.multi_thread.MultiThreadPhysics;
import app.model.Physics;
import app.model.physics.PhysicsThread;
import app.view.View;

import javax.swing.*;

public class App {
    public static void main(String[] args) {
        Board board = new Board(1200, 1200);
        Physics physics = new MultiThreadPhysics(board, 20, 20);

        // Start the Engines
        ControllerImpl controller = new ControllerImpl(physics);

        // 1. Start the Physics Engine independent of the UI
        PhysicsThread physicsThread = new PhysicsThread(physics);
        physicsThread.start();

        // 2. Open the Window on the Swing Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            View view = new View(physics, controller, 1200, 1200);
            view.setVisible(true);
        });
    }
}
