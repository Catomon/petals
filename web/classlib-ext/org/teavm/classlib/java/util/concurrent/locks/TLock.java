package org.teavm.classlib.java.util.concurrent.locks;

public interface TLock {
    void lock();

    void unlock();

    boolean tryLock();

    java.util.concurrent.locks.Condition newCondition();
}
