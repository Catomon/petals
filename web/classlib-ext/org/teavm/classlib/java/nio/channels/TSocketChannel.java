package org.teavm.classlib.java.nio.channels;

public abstract class TSocketChannel extends TSelectableChannel {
    public static TSocketChannel open() {
        return null;
    }

    public abstract boolean connect(java.net.InetSocketAddress remote);

    public abstract boolean finishConnect();

    public abstract boolean isConnected();

    public abstract boolean isConnectionPending();

    public abstract int read(java.nio.ByteBuffer buffer);

    public abstract int write(java.nio.ByteBuffer buffer);

    public abstract java.net.Socket socket();
}

