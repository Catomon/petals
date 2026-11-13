package org.teavm.classlib.java.util.concurrent;

public class TSemaphore {
    private int permits;

    public TSemaphore(int permits) {
        this.permits = permits;
    }

    public void acquire() throws InterruptedException {
        permits--;
    }

    public void acquire(int n) throws InterruptedException {
        permits -= n;
    }

    public void release() {
        permits++;
    }

    public void release(int n) {
        permits += n;
    }

    public boolean tryAcquire() {
        if (permits <= 0) return false;
        permits--;
        return true;
    }

    public int availablePermits() {
        return permits;
    }
}
