package org.teavm.classlib.java.net;

public class TInetSocketAddress {
    private final TInetAddress address;
    private final String host;
    private final int port;

    public TInetSocketAddress(TInetAddress address, int port) {
        this.address = address;
        this.host = address.getHostName();
        this.port = port;
    }

    public TInetSocketAddress(String host, int port) {
        this.address = TInetAddress.getByName(host);
        this.host = host;
        this.port = port;
    }

    public TInetSocketAddress(int port) {
        this.address = TInetAddress.getLocalHost();
        this.host = address.getHostName();
        this.port = port;
    }

    public TInetAddress getAddress() {
        return address;
    }

    public String getHostName() {
        return host;
    }

    public String getHostString() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean isUnresolved() {
        return false;
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }
}
