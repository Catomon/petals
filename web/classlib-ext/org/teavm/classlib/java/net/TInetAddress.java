package org.teavm.classlib.java.net;

public class TInetAddress {
    private final String host;
    private final byte[] address;

    TInetAddress(String host, byte[] address) {
        this.host = host;
        this.address = address;
    }

    public static TInetAddress getByName(String host) {
        return new TInetAddress(host, new byte[4]);
    }

    public static TInetAddress getByAddress(byte[] address) {
        return new TInetAddress("0.0.0.0", address);
    }

    public static TInetAddress getByAddress(String host, byte[] address) {
        return new TInetAddress(host, address);
    }

    public static TInetAddress getLocalHost() {
        return new TInetAddress("localhost", new byte[]{127, 0, 0, 1});
    }

    public static TInetAddress getLoopbackAddress() {
        return getLocalHost();
    }

    public String getHostName() {
        return host;
    }

    public String getCanonicalHostName() {
        return host;
    }

    public String getHostAddress() {
        return "0.0.0.0";
    }

    public byte[] getAddress() {
        return address;
    }

    public boolean isLoopbackAddress() {
        return false;
    }

    public boolean isAnyLocalAddress() {
        return false;
    }

    public boolean isMulticastAddress() {
        return false;
    }

    @Override
    public String toString() {
        return host;
    }
}
