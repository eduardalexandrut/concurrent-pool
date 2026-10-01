package app;

import app.controller.ControllerImpl;
import app.model.Physics;
import app.model.entities.game.Ball;
import app.model.entities.game.Board;
import app.model.entities.physics.P2d;
import app.model.entities.physics.V2d;
import app.model.physics.PhysicsThread;
import app.model.physics.executor.ExecutorPhysics;
import app.model.physics.multi_thread.MultiThreadPhysics;
import app.model.physics.sequential.SequentialPhysics;
import app.view.View;

import javax.swing.*;
import java.awt.*;

public class GameLauncher extends JFrame {

    public GameLauncher() {
        setTitle("Concurrent Pool - Launcher");
        setSize(400, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new BorderLayout(10, 10));

        // Title
        JLabel titleLabel = new JLabel("Concurrent Pool", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        add(titleLabel, BorderLayout.NORTH);

        // Center Panel for Options
        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 30));

        // Physics Selector
        JLabel modeLabel = new JLabel("Select Physics Engine:");
        String[] modes = {"Sequential Physics", "MultiThread Physics", "Executor Physics"};
        JComboBox<String> modeCombo = new JComboBox<>(modes);

        // Description Area
        JTextArea descArea = new JTextArea(3, 20);
        descArea.setWrapStyleWord(true);
        descArea.setLineWrap(true);
        descArea.setEditable(false);
        descArea.setBackground(getBackground());
        descArea.setText("Standard sequential execution. Best for small numbers of balls.");

        modeCombo.addActionListener(e -> {
            int selected = modeCombo.getSelectedIndex();
            if (selected == 0) descArea.setText("Standard sequential execution. Best for small numbers of balls.");
            if (selected == 1) descArea.setText("Uses native Threads and Monitors for spatial partitioning. High concurrency.");
            if (selected == 2) descArea.setText("Task-based approach using Java Executor Framework for dynamic load balancing.");
        });

        // Ball Count Spinner (Range: 1 to 50,000, Default: 1000)
        JLabel ballsLabel = new JLabel("Number of balls (1 - 50,000):");
        JSpinner ballsSpinner = new JSpinner(new SpinnerNumberModel(1000, 1, 50000, 100));

        optionsPanel.add(modeLabel);
        optionsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        optionsPanel.add(modeCombo);
        optionsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        optionsPanel.add(descArea);
        optionsPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        optionsPanel.add(ballsLabel);
        optionsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        optionsPanel.add(ballsSpinner);

        add(optionsPanel, BorderLayout.CENTER);

        // Start Button
        JButton startButton = new JButton("Start Game");
        startButton.setFont(new Font("Arial", Font.BOLD, 16));
        startButton.addActionListener(e -> {
            int numBalls = (int) ballsSpinner.getValue();
            int selectedMode = modeCombo.getSelectedIndex();

            // Close launcher
            dispose();

            // Launch the actual game
            launchGame(selectedMode, numBalls);
        });

        JPanel bottomPanel = new JPanel();
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));
        bottomPanel.add(startButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void launchGame(int modeIndex, int numBalls) {
        // Build the core components
        int boardWidth = 1600;
        int boardHeight = 1200;

        Board board = new Board(boardWidth, boardHeight, numBalls);


        // Instantiate the correct physics engine
        Physics physics;
        if (modeIndex == 0) {
            physics = new SequentialPhysics(board, 20, 20);
        } else if (modeIndex == 1) {
            physics = new MultiThreadPhysics(board, 20, 20);
        } else {
            physics = new ExecutorPhysics(board, 20, 20);
        }

        // Wire MVC
        ControllerImpl controller = new ControllerImpl(physics);
        PhysicsThread physicsThread = new PhysicsThread(controller);
        physicsThread.start();

        // Start GUI on EDT
        SwingUtilities.invokeLater(() -> {
            View view = new View(controller, 1600, 1200);
            view.setVisible(true);
        });
    }
}