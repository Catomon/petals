package org.teavm.classlib.java.util.concurrent;

/**
 * Single thread executor used by desktop libraries. The browser has no threads, so a task runs
 * when it is submitted.
 */
public class TExecutors {

    public static TThreadFactory defaultThreadFactory() {
        return new TThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                return new Thread(runnable);
            }
        };
    }

    public static TExecutorService newSingleThreadExecutor(TThreadFactory factory) {
        return new SimpleExecutor();
    }

    public static TExecutorService newSingleThreadExecutor() {
        return new SimpleExecutor();
    }

    public static TExecutorService newFixedThreadPool(int count) {
        return new SimpleExecutor();
    }

    public static TExecutorService newCachedThreadPool() {
        return new SimpleExecutor();
    }

    private static class SimpleExecutor implements TExecutorService {
        private boolean shutdown;

        @Override
        public void execute(Runnable command) {
            if (!shutdown) command.run();
        }

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public java.util.List<Runnable> shutdownNow() {
            shutdown = true;
            return new java.util.ArrayList<>();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, java.util.concurrent.TimeUnit unit) {
            return shutdown;
        }
    }
}
