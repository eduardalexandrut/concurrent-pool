package app.model.physics;


import app.model.AbstractPhysics;
import app.model.Physics;
import app.model.entities.physics.V2d;
import app.view.View;

import javax.swing.*;

public class PhysicsThread extends Thread {
    private final Physics model;
    private final View view;
    private final long period = 15; // 20ms = 50 FPS
    private long lastNpcKick = 0;
    private final long npcPeriod = 2000;

    public PhysicsThread(Physics model,  View view) {
        this.model = model;
        this.view = view;
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double fps = 0;

        while (!isInterrupted() && this.model.getGameState() == AbstractPhysics.GameState.RUNNING) {
            long now = System.nanoTime();
            // Calculate the time elapsed in nanoseconds
            long updateTime = now - lastTime;
            lastTime = now;

            // Convert nanoseconds to seconds, then invert for FPS
            // We use a simple Alpha-Smoothing filter to stop the number from flickering
            double currentFps = 1_000_000_000.0 / updateTime;
            fps = (fps * 0.9) + (currentFps * 0.1);

            long start = System.currentTimeMillis();

            // NPC update
            updateNpc();

            // 1. Update the math (Move balls, check collisions)
            model.computeState(period);

            checkEndGame();

            model.setFPS((int) fps);

            // 2. repaint UI (SAFE)
            SwingUtilities.invokeLater(() -> view.repaint());

            // 3. Regulate the speed so it doesn't run too fast
            long used = System.currentTimeMillis() - start;
            long sleep = Math.max(0, period - used);

            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                break;
            }
        }

        System.out.println("Simulation Ended. Final State: " + model.getGameState());
    }

    private void updateNpc() {
        long now = System.currentTimeMillis();

        if (now - lastNpcKick > npcPeriod) {

            V2d newVel = new V2d(
                    Math.random() * 200,
                    Math.random() * 200
            );

            model.getNPCBall().kick(newVel);

            lastNpcKick = now;
        }
    }

    private void checkEndGame() {
        if (this.model.getGameState() != AbstractPhysics.GameState.RUNNING) {
            return;
        }
        if (this.model.getStateSnapshot().size() <= 2) {
            final int userScore = this.model.getUserScore();
            final int npcScore = this.model.getNPCScore();

            if (userScore > npcScore) {
                this.model.setGameState(AbstractPhysics.GameState.USER_WON);
            } else if (userScore < npcScore) {
                this.model.setGameState(AbstractPhysics.GameState.NPC_WON);
            } else {
                this.model.setGameState(AbstractPhysics.GameState.DRAW);
            }
        }
    }
}
