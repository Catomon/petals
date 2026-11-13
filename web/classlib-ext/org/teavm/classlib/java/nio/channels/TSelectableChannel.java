package org.teavm.classlib.java.nio.channels;

public abstract class TSelectableChannel {
    public abstract boolean isOpen();

    public abstract boolean isBlocking();

    public abstract TSelectableChannel configureBlocking(boolean block);

    public abstract java.nio.channels.SelectionKey register(java.nio.channels.Selector selector, int ops);

    public abstract java.nio.channels.SelectionKey keyFor(java.nio.channels.Selector selector);

    public abstract int validOps();

    public abstract void close();
}

