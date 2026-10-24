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
        SwingUtilities.invokeLater(() -> {
            GameLauncher launcher = new GameLauncher();
            launcher.setVisible(true);
        });
    }
}
