package org.teavm.classlib.java.net;

public final class TNetworkInterface {
    private final String name;

    private TNetworkInterface(String name) {
        this.name = name;
    }

    public static TNetworkInterface getByName(String name) {
        return new TNetworkInterface(name);
    }

    public static java.util.Enumeration<TNetworkInterface> getNetworkInterfaces() {
        return null;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return name;
    }

    public java.util.Enumeration<TInetAddress> getInetAddresses() {
        return null;
    }

    public boolean isUp() {
        return false;
    }

    public boolean isLoopback() {
        return false;
    }

    public boolean supportsMulticast() {
        return false;
    }
}
