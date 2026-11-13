package org.teavm.classlib.java.nio.channels;

public abstract class TServerSocketChannel extends TSelectableChannel {
    public static TServerSocketChannel open() {
        return null;
    }

    public abstract TServerSocketChannel bind(java.net.InetSocketAddress local);

    public abstract TSocketChannel accept();

    public abstract java.net.ServerSocket socket();
}

