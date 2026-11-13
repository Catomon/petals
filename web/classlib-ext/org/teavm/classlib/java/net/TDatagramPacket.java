package org.teavm.classlib.java.net;

public final class TDatagramPacket {
    private final byte[] data;

    public TDatagramPacket(byte[] data, int length) {
        this.data = data;
    }

    public TDatagramPacket(byte[] data, int length, TInetAddress address, int port) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public int getLength() {
        return data.length;
    }

    public TInetAddress getAddress() {
        return null;
    }

    public int getPort() {
        return 0;
    }
}
