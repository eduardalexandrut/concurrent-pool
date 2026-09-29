package app;

import app.controller.ControllerImpl;
import app.model.physics.Board;
import app.model.physics.MultiThreadPhysics;
import app.model.physics.Physics;
import app.model.physics.PhysicsThread;
import app.view.View;

import javax.swing.*;

public class App {
    public static void main(String[] args) {
        Board board = new Board(1200, 1200);
        Physics physics = new MultiThreadPhysics(board, 20, 20);


        // Start the Engines
        ControllerImpl controller = new ControllerImpl(physics);
        //controller.start();

        // Open the Window
        SwingUtilities.invokeLater(() -> {
            View view = new View(physics, controller, 1200, 1200);
            view.setVisible(true);

            PhysicsThread physicsThread = new PhysicsThread(physics, view);
            physicsThread.start();
        });
    }
}
