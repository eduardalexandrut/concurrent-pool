package app.model.entities.game;

import app.model.entities.physics.P2d;
import app.model.entities.physics.V2d;

import java.util.concurrent.locks.ReentrantLock;

public class UserBall extends Ball {

    public enum BALL_TYPE {
        HUMAN,
        BOT
    }

    private final ReentrantLock lock = new ReentrantLock();
    private final BALL_TYPE type;

    public UserBall(P2d pos, double radius, double mass, V2d vel, BALL_TYPE type) {
        super(pos, radius, mass, vel);
        this.type = type;
    }

    public void setPosition(P2d pos) {
        this.lock.lock();
        try {
            this.setPosition(pos);
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public P2d getPos() {
        this.lock.lock();
        try {
            P2d pos = super.getPos();
            return new P2d(pos.x(), pos.y());
        } finally { this.lock.unlock(); }
    }

    public void setVelocity(V2d vel) {
        if (this.lock.tryLock()) {
            try {
                super.kick(vel);
            } finally {
                this.lock.unlock();
            }
        } else {
            System.out.println("UserBall lock busy, dropping input frame");
        }
    }

    public BALL_TYPE getType() {
        this.lock.lock();
        try {
            BALL_TYPE type = this.type;
            return type;
        } finally {
            this.lock.unlock();
        }
    }
}
