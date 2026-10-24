package app.model.active_components;

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

    /**
     * Waits until all members have called await().
     *
     * @throws InterruptedException if the thread is interrupted while waiting. In that case the thread
     *                              withdraws its arrival, so the barrier stays consistent for the others.
     */
    public void await() throws InterruptedException {
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
                try {
                    allArrived.await();
                } catch (InterruptedException e) {
                    if (myGeneration == generation) {
                        arrivedMembers--;   // the barrier has not tripped yet: withdraw
                        throw e;
                    }
                    // the barrier tripped anyway: pass, but keep the interrupt for the caller
                    Thread.currentThread().interrupt();
                }
            }
        } finally {
            lock.unlock();
        }
    }
}
