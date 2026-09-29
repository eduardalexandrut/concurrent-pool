package app.model.physics;

public class NpcThread extends Thread {
    private final Ball npcBall;

    public NpcThread(Ball npcBall) {
        this.npcBall = npcBall;
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            final V2d newVel = new V2d(Math.random() * 200, Math.random() * 200);

            this.npcBall.kick(newVel);

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) { break; }
        }
    }
}
