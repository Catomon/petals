package org.teavm.classlib.java.nio.channels;

public abstract class TSelectionKey {
    public static final int OP_READ = 1;
    public static final int OP_WRITE = 4;
    public static final int OP_CONNECT = 8;
    public static final int OP_ACCEPT = 16;

    public abstract TSelectableChannel channel();

    public abstract TSelector selector();

    public abstract boolean isValid();

    public abstract void cancel();

    public abstract int interestOps();

    public abstract TSelectionKey interestOps(int ops);

    public abstract int readyOps();
}
