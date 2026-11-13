package org.teavm.classlib.java.util.concurrent.locks;

/** There is no contention in the browser, so locking only tracks the owner. */
public class TReentrantLock implements TLock {
    private Thread owner;
    private int count;

    @Override
    public void lock() {
        owner = Thread.currentThread();
        count++;
    }

    @Override
    public void unlock() {
        if (count > 0) count--;
    }

    @Override
    public boolean tryLock() {
        lock();
        return true;
    }

    public boolean isLocked() {
        return count > 0;
    }

    public int getHoldCount() {
        return count;
    }

    @Override
    public java.util.concurrent.locks.Condition newCondition() {
        throw new UnsupportedOperationException("Conditions are not available in the browser.");
    }
}
