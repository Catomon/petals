package org.teavm.classlib.java.util.concurrent.locks;

public interface TCondition {
    void await() throws InterruptedException;

    void signal();

    void signalAll();
}
