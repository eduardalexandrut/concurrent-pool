package app.model.physics;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SimpleBarrier {

    private final int members;
    private int arrivedMembers = 0;
    private int generation = 0;

    private final Lock lock = new ReentrantLock();
    private final Condition allArrived = lock.newCondition();

    public SimpleBarrier(int members) {
        this.members = members;
    }

    public void await() {
        lock.lock();
        try {
            int myGeneration = generation;

            arrivedMembers++;

            if (arrivedMembers == members) {
                generation++;          // next phase
                arrivedMembers = 0;
                allArrived.signalAll();
                return;
            }

            while (myGeneration == generation) {
                allArrived.await();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }
}
