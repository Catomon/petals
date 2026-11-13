package org.teavm.classlib.java.nio.channels.spi;

public abstract class TSelectorProvider {
    public static TSelectorProvider provider() {
        return null;
    }

    public abstract java.nio.channels.SocketChannel openSocketChannel();

    public abstract java.nio.channels.ServerSocketChannel openServerSocketChannel();

    public abstract java.nio.channels.Selector openSelector();
}

