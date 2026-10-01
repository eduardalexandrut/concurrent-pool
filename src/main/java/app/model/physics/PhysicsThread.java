package app.model.physics;


import app.controller.Controller;
import app.model.AbstractPhysics;
import app.model.Physics;
import app.model.entities.physics.V2d;

import javax.swing.*;

public class PhysicsThread extends Thread {
    private final Controller controller;
    private final long period = 16;
    private long lastNpcKick = 0;
    private final long npcPeriod = 2000;

    public PhysicsThread(Controller controller) {
        this.controller = controller;
    }

    @Override
    public void run() {

        while (!isInterrupted() && this.controller.getModel().getGameState() == AbstractPhysics.GameState.RUNNING) {

            long start = System.currentTimeMillis();

            updateNpc();

            this.controller.getModel().computeState(period);

            checkEndGame();


            // Regulate the speed so it doesn't run too fast
            long used = System.currentTimeMillis() - start;
            long sleep = Math.max(0, period - used);

            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                break;
            }
        }

        System.out.println("Simulation Ended. Final State: " + controller.getModel().getGameState());
    }

    /**
     * Method that randomly moves the NPC's ball.
     */
    private void updateNpc() {
        long now = System.currentTimeMillis();

        if (now - lastNpcKick > npcPeriod) {

            V2d newVel = new V2d(
                    Math.random() * 200,
                    Math.random() * 200
            );

            controller.getModel().getNPCBall().kick(newVel);

            lastNpcKick = now;
        }
    }

    /**
     * Method to check and set the endgame state(user won, npc won, draw).
     */
    private void checkEndGame() {
        if (this.controller.getModel().getGameState() != AbstractPhysics.GameState.RUNNING) {
            return;
        }
        if (this.controller.getModel().getStateSnapshot().size() <= 2) {
            final int userScore = this.controller.getModel().getUserScore();
            final int npcScore = this.controller.getModel().getNPCScore();

            if (userScore > npcScore) {
                this.controller.getModel().setGameState(AbstractPhysics.GameState.USER_WON);
            } else if (userScore < npcScore) {
                this.controller.getModel().setGameState(AbstractPhysics.GameState.NPC_WON);
            } else {
                this.controller.getModel().setGameState(AbstractPhysics.GameState.DRAW);
            }
        }
    }
}
