package org.teavm.classlib.java.security;

public class TMessageDigest {
    public static TMessageDigest getInstance(String algorithm) {
        return new TMessageDigest();
    }

    public void update(byte[] input) {
    }

    public void update(byte[] input, int offset, int len) {
    }

    public byte[] digest() {
        return new byte[0];
    }

    public void reset() {
    }

    public int getDigestLength() {
        return 0;
    }
}
