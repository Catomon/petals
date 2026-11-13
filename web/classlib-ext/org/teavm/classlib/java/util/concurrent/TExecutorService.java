package org.teavm.classlib.java.util.concurrent;

public interface TExecutorService extends TExecutor {
    void shutdown();

    java.util.List<Runnable> shutdownNow();

    boolean isShutdown();

    boolean isTerminated();

    boolean awaitTermination(long timeout, java.util.concurrent.TimeUnit unit) throws InterruptedException;
}
