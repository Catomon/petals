package org.teavm.classlib.java.nio.channels;

public abstract class TSelector {
    public static TSelector open() {
        return null;
    }

    public abstract int select() throws java.io.IOException;

    public abstract int select(long timeout) throws java.io.IOException;

    public abstract int selectNow() throws java.io.IOException;

    public abstract java.util.Set<TSelectionKey> selectedKeys();

    public abstract java.util.Set<TSelectionKey> keys();

    public abstract TSelector wakeup();

    public abstract boolean isOpen();

    public abstract void close();
}
